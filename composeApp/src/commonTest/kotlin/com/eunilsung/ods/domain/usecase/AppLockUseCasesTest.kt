package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.data.local.repository.LocalAppLockRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalConsultSessionRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LogoutReason
import com.eunilsung.ods.domain.model.UnlockResult
import com.eunilsung.ods.domain.repository.LocalDataWiper
import com.eunilsung.ods.domain.util.AppLockPolicy
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppLockUseCasesTest {
    private class CountingWiper : LocalDataWiper {
        var wipes = 0
        override suspend fun wipe() {
            wipes++
        }
    }

    private val settings = MapSettings()
    private val appLock = LocalAppLockRepositoryImpl(settings)
    private val consultSession = LocalConsultSessionRepositoryImpl()
    private val login = LocalLoginRepositoryImpl(settings, simulatedRoundTripMillis = 0)
    private val wiper = CountingWiper()
    private val unlock = UnlockWithPinUseCase(appLock, wiper, LogoutUseCase(login, appLock, consultSession))
    private val lockIfInactive = LockIfInactiveUseCase(appLock)

    private suspend fun signedInWithPin(now: Long = 0) {
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        consultSession.start("C001")
        appLock.startSession("test1", now)
        appLock.setPin("135790")
    }

    @Test
    fun rightPinUnlocksAndResetsFailures() = runTest {
        signedInWithPin()
        appLock.lock()
        assertEquals(UnlockResult.Wrong(AppLockPolicy.MAX_ATTEMPTS - 1), unlock("000000"))
        assertEquals(UnlockResult.Unlocked, unlock("135790"))
        assertFalse(appLock.state.value.isLocked)
        assertEquals(UnlockResult.Wrong(AppLockPolicy.MAX_ATTEMPTS - 1), unlock("000000"))
    }

    @Test
    fun tooManyWrongPinsWipeDataAndSignOut() = runTest {
        signedInWithPin()
        appLock.lock()
        repeat(AppLockPolicy.MAX_ATTEMPTS - 1) { unlock("000000") }
        assertEquals(0, wiper.wipes)

        assertEquals(UnlockResult.Wiped, unlock("000000"))
        assertEquals(1, wiper.wipes)
        assertNull(login.currentStaff.value)
        assertNull(consultSession.activeCustomerId.value)
        assertEquals(LogoutReason.WIPED, login.lastLogoutReason.value)

        appLock.startSession("test1", 0)
        assertFalse(appLock.state.value.isPinSet)
    }

    @Test
    fun pinsAreKeptPerStaff() = runTest {
        signedInWithPin()
        appLock.startSession("test2", 0)
        assertFalse(appLock.state.value.isPinSet)
        appLock.startSession("test1", 0)
        assertTrue(appLock.state.value.isPinSet)
    }

    @Test
    fun idleTooLongLocks() = runTest {
        signedInWithPin(now = 0)
        val idle = AppLockPolicy.IDLE_LOCK_SECONDS * 1000
        assertFalse(lockIfInactive(now = idle - 1))
        appLock.recordActivity(idle - 1)
        assertFalse(lockIfInactive(now = idle * 2 - 2))
        assertTrue(lockIfInactive(now = idle * 2))
        assertTrue(appLock.state.value.isLocked)
    }

    @Test
    fun shortTripToBackgroundDoesNotLockButLongOneDoes() = runTest {
        signedInWithPin(now = 0)
        val away = AppLockPolicy.BACKGROUND_LOCK_SECONDS * 1000
        appLock.recordActivity(1_000)
        appLock.recordBackground(1_000)
        assertFalse(lockIfInactive(now = 1_000 + away - 1))

        appLock.recordActivity(5_000)
        appLock.recordBackground(5_000)
        assertTrue(lockIfInactive(now = 5_000 + away))
    }

    @Test
    fun withoutPinNothingLocks() = runTest {
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        appLock.startSession("test1", 0)
        assertFalse(lockIfInactive(now = Long.MAX_VALUE / 2))
        assertFalse(appLock.state.value.isLocked)
    }
}
