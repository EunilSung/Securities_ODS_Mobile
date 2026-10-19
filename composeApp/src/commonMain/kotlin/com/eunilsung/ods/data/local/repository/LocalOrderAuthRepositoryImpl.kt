package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.domain.model.OrderAuthConfig
import com.eunilsung.ods.domain.model.PinCheckResult
import com.eunilsung.ods.domain.repository.OrderAuthRepository
import com.eunilsung.ods.domain.util.OrderPinPolicy
import com.eunilsung.ods.util.PinHash
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 서버 없이 도는 주문 인증 — 비밀번호를 salt + SHA-256 해시로 기기 설정에 저장하고, 틀린 횟수와 잠금을 센다.
 *
 * 서버 모드에서는 계좌 비밀번호를 서버가 확인하고 틀린 횟수도 서버가 센다. 이 클래스는 생체 인증 사용 여부만 남긴다.
 */
class LocalOrderAuthRepositoryImpl(
    private val settings: Settings,
) : OrderAuthRepository {

    private val _config = MutableStateFlow(load())
    override val config: StateFlow<OrderAuthConfig> = _config.asStateFlow()

    private fun load() = OrderAuthConfig(
        isPinSet = settings.getStringOrNull(KEY_HASH) != null,
        biometricEnabled = settings.getBoolean(KEY_BIOMETRIC, false),
        lockedUntilEpochSeconds = settings.getLongOrNull(KEY_LOCKED_UNTIL),
    )

    override fun setPin(pin: String) {
        val salt = PinHash.newSalt()
        settings.putString(KEY_SALT, salt)
        settings.putString(KEY_HASH, PinHash.hash(pin, salt))
        settings.remove(KEY_FAILURES)
        settings.remove(KEY_LOCKED_UNTIL)
        _config.value = load()
    }

    override fun verifyPin(pin: String, nowEpochSeconds: Long): PinCheckResult {
        val lockedUntil = settings.getLongOrNull(KEY_LOCKED_UNTIL)
        if (lockedUntil != null) {
            if (nowEpochSeconds < lockedUntil) return PinCheckResult.Locked(lockedUntil)
            settings.remove(KEY_LOCKED_UNTIL)
            settings.remove(KEY_FAILURES)
        }
        val salt = settings.getStringOrNull(KEY_SALT)
        val stored = settings.getStringOrNull(KEY_HASH)
        val result = if (salt != null && stored != null && PinHash.hash(pin, salt) == stored) {
            settings.remove(KEY_FAILURES)
            PinCheckResult.Matched
        } else {
            val failures = settings.getInt(KEY_FAILURES, 0) + 1
            if (failures >= OrderPinPolicy.MAX_ATTEMPTS) {
                val until = nowEpochSeconds + OrderPinPolicy.LOCK_SECONDS
                settings.remove(KEY_FAILURES)
                settings.putLong(KEY_LOCKED_UNTIL, until)
                PinCheckResult.Locked(until)
            } else {
                settings.putInt(KEY_FAILURES, failures)
                PinCheckResult.Wrong(OrderPinPolicy.MAX_ATTEMPTS - failures)
            }
        }
        _config.value = load()
        return result
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_BIOMETRIC, enabled)
        _config.value = load()
    }

    private companion object {
        const val KEY_SALT = "orderAuth.salt"
        const val KEY_HASH = "orderAuth.hash"
        const val KEY_FAILURES = "orderAuth.failures"
        const val KEY_LOCKED_UNTIL = "orderAuth.lockedUntil"
        const val KEY_BIOMETRIC = "orderAuth.biometric"
    }
}
