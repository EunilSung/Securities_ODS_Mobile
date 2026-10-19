package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.domain.model.PinCheckResult
import com.eunilsung.ods.domain.util.OrderPinPolicy
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalOrderAuthRepositoryTest {
    private val now = 1_760_000_000L

    @Test
    fun pinIsStoredOnlyAsSaltedHash() {
        val settings = MapSettings()
        val repository = LocalOrderAuthRepositoryImpl(settings)
        assertFalse(repository.config.value.isPinSet)

        repository.setPin("2580")

        assertTrue(repository.config.value.isPinSet)
        assertFalse(settings.keys.any { settings.getStringOrNull(it) == "2580" })
        assertEquals(PinCheckResult.Matched, repository.verifyPin("2580", now))
    }

    @Test
    fun wrongPinsCountDownThenLock() {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings()).apply { setPin("2580") }
        for (remaining in OrderPinPolicy.MAX_ATTEMPTS - 1 downTo 1) {
            assertEquals(PinCheckResult.Wrong(remaining), repository.verifyPin("0000", now))
        }
        val lockedUntil = now + OrderPinPolicy.LOCK_SECONDS
        assertEquals(PinCheckResult.Locked(lockedUntil), repository.verifyPin("0000", now))
        assertEquals(PinCheckResult.Locked(lockedUntil), repository.verifyPin("2580", now + 60))
        assertEquals(lockedUntil, repository.config.value.lockedUntilEpochSeconds)
    }

    @Test
    fun lockExpiresAndCountStartsOver() {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings()).apply { setPin("2580") }
        repeat(OrderPinPolicy.MAX_ATTEMPTS) { repository.verifyPin("0000", now) }
        val later = now + OrderPinPolicy.LOCK_SECONDS
        assertEquals(PinCheckResult.Wrong(OrderPinPolicy.MAX_ATTEMPTS - 1), repository.verifyPin("0000", later))
        assertEquals(PinCheckResult.Matched, repository.verifyPin("2580", later))
        assertNull(repository.config.value.lockedUntilEpochSeconds)
    }

    @Test
    fun matchResetsFailures() {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings()).apply { setPin("2580") }
        repeat(3) { repository.verifyPin("0000", now) }
        repository.verifyPin("2580", now)
        assertEquals(PinCheckResult.Wrong(OrderPinPolicy.MAX_ATTEMPTS - 1), repository.verifyPin("0000", now))
    }

    @Test
    fun settingsSurviveANewInstance() {
        val settings = MapSettings()
        LocalOrderAuthRepositoryImpl(settings).apply {
            setPin("2580")
            setBiometricEnabled(true)
        }
        val reopened = LocalOrderAuthRepositoryImpl(settings)
        assertTrue(reopened.config.value.isPinSet)
        assertTrue(reopened.config.value.biometricEnabled)
    }
}
