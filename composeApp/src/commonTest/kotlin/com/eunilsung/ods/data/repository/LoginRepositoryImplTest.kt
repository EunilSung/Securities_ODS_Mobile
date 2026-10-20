package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.remote.server.AuthTokenStore
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.model.LogoutReason
import com.eunilsung.ods.testsupport.FakeOdsServer
import com.eunilsung.ods.testsupport.FakeServerEvents
import com.eunilsung.ods.testsupport.awaitUntil
import com.eunilsung.ods.testsupport.testSecureSettings
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/** 서버 로그인 — 서버 답을 도메인 결과로 옮기고, 토큰·사번·실시간 알림을 어떻게 다루는지 본다. */
class LoginRepositoryImplTest {
    private val settings = MapSettings()
    private val secureSettings = testSecureSettings(settings)
    private val server = FakeOdsServer()
    private val events = FakeServerEvents()
    private val tokenStore = AuthTokenStore(secureSettings)
    private val repository = LoginRepositoryImpl(
        LocalLoginRepositoryImpl(settings, secureSettings, simulatedRoundTripMillis = 0),
        server,
        tokenStore,
        events,
    )

    @Test
    fun successKeepsTokenSealedAndStartsListening() = runTest {
        val result = repository.login(LoginRequest(" TEST1 ", FakeOdsServer.PASSWORD), nowEpochSeconds = 0)

        assertIs<LoginResult.Success>(result)
        assertEquals("김민준", repository.currentStaff.value?.name)
        assertEquals("token-1", tokenStore.token())
        assertEquals("token-1", AuthTokenStore(secureSettings).token())
        assertFalse(settings.keys.any { settings.getStringOrNull(it)?.contains("token-1") == true })
        assertEquals("test1", repository.savedId())
        awaitUntil { events.listening == 1 }
    }

    @Test
    fun wrongPasswordCarriesRemainingAttempts() = runTest {
        server.remainingAttempts = 2

        assertEquals(LoginResult.Wrong(2), repository.login(LoginRequest("test1", "0000"), nowEpochSeconds = 0))
        assertNull(repository.currentStaff.value)
        assertEquals("", tokenStore.token())
    }

    @Test
    fun lockedCarriesUnlockTime() = runTest {
        server.lockedUntilEpochSeconds = 9_999

        assertEquals(LoginResult.Locked(9_999), repository.login(LoginRequest("test1", FakeOdsServer.PASSWORD), nowEpochSeconds = 0))
    }

    @Test
    fun unreachableServerIsUnreachable() = runTest {
        server.isReachable = false

        assertEquals(LoginResult.Unreachable, repository.login(LoginRequest("test1", FakeOdsServer.PASSWORD), nowEpochSeconds = 0))
        assertNull(repository.currentStaff.value)
    }

    @Test
    fun logoutRevokesTokenAndStopsListening() = runTest {
        repository.login(LoginRequest("test1", FakeOdsServer.PASSWORD), nowEpochSeconds = 0)
        awaitUntil { events.listening == 1 }

        repository.logout(LogoutReason.USER)

        assertNull(repository.currentStaff.value)
        assertEquals(LogoutReason.USER, repository.lastLogoutReason.value)
        assertEquals("", tokenStore.token())
        awaitUntil { events.listening == 0 && server.revoked == listOf("token-1") }
    }
}
