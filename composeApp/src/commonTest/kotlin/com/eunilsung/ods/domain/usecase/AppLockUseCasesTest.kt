package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.data.local.LocalDataWiperImpl
import com.eunilsung.ods.data.local.LocalKeyGuard
import com.eunilsung.ods.data.local.repository.LocalAppLockRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalConsultSessionRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.remote.server.AuthTokenStore
import com.eunilsung.ods.data.repository.LoginRepositoryImpl
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LogoutReason
import com.eunilsung.ods.domain.model.UnlockResult
import com.eunilsung.ods.domain.repository.LocalDataWiper
import com.eunilsung.ods.domain.util.AppLockPolicy
import com.eunilsung.ods.testsupport.FakeOdsServer
import com.eunilsung.ods.testsupport.FakeServerEvents
import com.eunilsung.ods.testsupport.awaitUntil
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.testsupport.testLocalSecret
import com.eunilsung.ods.testsupport.testSecureSettings
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class AppLockUseCasesTest {
    private class CountingWiper : LocalDataWiper {
        var wipes = 0
        override suspend fun wipe() {
            wipes++
        }
    }

    private val settings = MapSettings()
    private val appLock = LocalAppLockRepositoryImpl(settings, testSecureSettings(settings))
    private val consultSession = LocalConsultSessionRepositoryImpl()
    private val login = LocalLoginRepositoryImpl(settings, testSecureSettings(settings), simulatedRoundTripMillis = 0)
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
    fun wipeRevokesServerTokenBeforeClearingIt() = runTest {
        val server = FakeOdsServer()
        val secure = testSecureSettings(settings)
        val tokenStore = AuthTokenStore(secure)
        val serverLogin = LoginRepositoryImpl(
            LocalLoginRepositoryImpl(settings, secure, simulatedRoundTripMillis = 0),
            server,
            tokenStore,
            FakeServerEvents(),
        )
        val database = createTestDatabase()
        val serverWiper = LocalDataWiperImpl(database, LocalKeyGuard(database, MapSettings(), testLocalSecret()), tokenStore)
        val serverUnlock = UnlockWithPinUseCase(appLock, serverWiper, LogoutUseCase(serverLogin, appLock, consultSession))
        serverLogin.login(LoginRequest("test1", FakeOdsServer.PASSWORD), nowEpochSeconds = 0)
        appLock.startSession("test1", 0)
        appLock.setPin("135790")
        appLock.lock()

        repeat(AppLockPolicy.MAX_ATTEMPTS - 1) { serverUnlock("000000") }
        assertEquals(UnlockResult.Wiped, serverUnlock("000000"))
        assertEquals("", tokenStore.token())
        awaitUntil { server.revoked == listOf("token-1") }
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
