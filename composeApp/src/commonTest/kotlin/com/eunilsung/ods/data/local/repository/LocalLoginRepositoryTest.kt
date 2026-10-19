package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.data.local.seed.DemoAccounts
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.util.LoginPolicy
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalLoginRepositoryTest {
    private val now = 1_760_000_000L

    private fun repository(settings: MapSettings = MapSettings()) =
        LocalLoginRepositoryImpl(settings, simulatedRoundTripMillis = 0)

    @Test
    fun successSetsStaffAndRemembersIdOnly() = runTest {
        val settings = MapSettings()
        val repository = repository(settings)

        val result = repository.login(LoginRequest(" TEST1 ", DemoAccounts.PASSWORD), now)

        assertTrue(result is LoginResult.Success)
        assertEquals("test1", repository.currentStaff.value?.id)
        assertEquals("test1", repository.savedId())
        assertFalse(settings.keys.any { settings.getStringOrNull(it) == DemoAccounts.PASSWORD })
    }

    @Test
    fun unknownIdAndWrongPasswordLookTheSame() = runTest {
        val repository = repository()
        val expected = LoginResult.Wrong(LoginPolicy.MAX_ATTEMPTS - 1)
        assertEquals(expected, repository.login(LoginRequest("test1", "0000"), now))
        assertEquals(expected, repository.login(LoginRequest("nobody", DemoAccounts.PASSWORD), now))
        assertNull(repository.currentStaff.value)
    }

    @Test
    fun wrongTriesCountDownThenLock() = runTest {
        val repository = repository()
        for (remaining in LoginPolicy.MAX_ATTEMPTS - 1 downTo 1) {
            assertEquals(LoginResult.Wrong(remaining), repository.login(LoginRequest("test1", "0000"), now))
        }
        val lockedUntil = now + LoginPolicy.LOCK_SECONDS
        assertEquals(LoginResult.Locked(lockedUntil), repository.login(LoginRequest("test1", "0000"), now))
        assertEquals(LoginResult.Locked(lockedUntil), repository.login(LoginRequest("test1", DemoAccounts.PASSWORD), now + 60))
        assertTrue(repository.login(LoginRequest("test2", DemoAccounts.PASSWORD), now + 60) is LoginResult.Success)
    }

    @Test
    fun lockExpiresAndCountStartsOver() = runTest {
        val repository = repository()
        repeat(LoginPolicy.MAX_ATTEMPTS) { repository.login(LoginRequest("test1", "0000"), now) }
        val later = now + LoginPolicy.LOCK_SECONDS
        assertEquals(LoginResult.Wrong(LoginPolicy.MAX_ATTEMPTS - 1), repository.login(LoginRequest("test1", "0000"), later))
        assertTrue(repository.login(LoginRequest("test1", DemoAccounts.PASSWORD), later) is LoginResult.Success)
    }

    @Test
    fun successResetsFailures() = runTest {
        val repository = repository()
        repeat(3) { repository.login(LoginRequest("test1", "0000"), now) }
        repository.login(LoginRequest("test1", DemoAccounts.PASSWORD), now)
        assertEquals(LoginResult.Wrong(LoginPolicy.MAX_ATTEMPTS - 1), repository.login(LoginRequest("test1", "0000"), now))
    }

    @Test
    fun turningOffSaveIdForgetsIt() = runTest {
        val settings = MapSettings()
        val repository = repository(settings)
        repository.login(LoginRequest("test2", DemoAccounts.PASSWORD), now)
        assertEquals("test2", repository(settings).savedId())

        repository.setSaveId(false)
        assertNull(repository.savedId())
        repository.login(LoginRequest("test2", DemoAccounts.PASSWORD), now)
        assertNull(repository(settings).savedId())
        assertFalse(repository(settings).isSaveId())
    }

    @Test
    fun logoutClearsStaffButKeepsSavedId() = runTest {
        val repository = repository()
        repository.login(LoginRequest("test3", DemoAccounts.PASSWORD), now)
        repository.logout()
        assertNull(repository.currentStaff.value)
        assertEquals("test3", repository.savedId())
    }
}
