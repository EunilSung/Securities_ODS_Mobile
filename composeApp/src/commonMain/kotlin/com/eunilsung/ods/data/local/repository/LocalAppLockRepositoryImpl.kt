package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.data.local.SecureSettings
import com.eunilsung.ods.domain.model.AppLockState
import com.eunilsung.ods.domain.repository.AppLockRepository
import com.eunilsung.ods.util.PinHash
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 앱 잠금을 기기에 둔다 — 직원마다 salt + SHA-256 해시로 비밀번호를 남기고, 틀린 횟수를 센다.
 *
 * 서버 모드에서도 앱 잠금은 기기 안의 일이라 이 구현을 그대로 쓴다. 저장 키에는 사번이 들어가므로
 * 한 기기를 여러 직원이 써도 서로의 비밀번호를 쓰지 않는다.
 * salt·해시는 [secureSettings] 로 잠가 둔다 — 설정 파일만 빼내서는 6자리 비밀번호를 대입해 볼 수 없다.
 */
class LocalAppLockRepositoryImpl(
    private val settings: Settings,
    private val secureSettings: SecureSettings,
) : AppLockRepository {

    private var staffId: String? = null
    private val _state = MutableStateFlow(AppLockState())
    override val state: StateFlow<AppLockState> = _state.asStateFlow()

    override var lastActivityMillis: Long = 0L
        private set

    private var backgroundSince: Long? = null

    override fun startSession(staffId: String, nowMillis: Long) {
        this.staffId = staffId
        lastActivityMillis = nowMillis
        backgroundSince = null
        _state.value = load(staffId).copy(isLocked = false)
    }

    override fun endSession() {
        staffId = null
        backgroundSince = null
        _state.value = AppLockState()
    }

    override fun setPin(pin: String) {
        val id = staffId ?: return
        val salt = PinHash.newSalt()
        secureSettings.putString(saltKey(id), salt)
        secureSettings.putString(hashKey(id), PinHash.hash(pin, salt))
        settings.remove(failureKey(id))
        settings.putString(KEY_STAFF_IDS, (staffIds() + id).joinToString(","))
        _state.value = load(id).copy(isLocked = false)
    }

    override fun checkPin(pin: String): AppLockRepository.PinCheck {
        val id = staffId ?: return AppLockRepository.PinCheck.Wrong(Int.MAX_VALUE)
        val salt = secureSettings.getStringOrNull(saltKey(id))
        val stored = secureSettings.getStringOrNull(hashKey(id))
        return if (salt != null && stored != null && PinHash.hash(pin, salt) == stored) {
            settings.remove(failureKey(id))
            AppLockRepository.PinCheck.Matched
        } else {
            val failures = settings.getInt(failureKey(id), 0) + 1
            settings.putInt(failureKey(id), failures)
            AppLockRepository.PinCheck.Wrong(failures)
        }
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        val id = staffId ?: return
        settings.putBoolean(biometricKey(id), enabled)
        _state.update { it.copy(biometricEnabled = enabled) }
    }

    override fun lock() {
        if (staffId == null) return
        _state.update { it.copy(isLocked = true) }
    }

    override fun unlock() {
        _state.update { it.copy(isLocked = false) }
    }

    override fun recordActivity(nowMillis: Long) {
        lastActivityMillis = nowMillis
    }

    override fun recordBackground(nowMillis: Long) {
        backgroundSince = nowMillis
    }

    override fun takeBackgroundSince(): Long? = backgroundSince.also { backgroundSince = null }

    override fun clearAll() {
        staffIds().forEach { id ->
            listOf(saltKey(id), hashKey(id), failureKey(id), biometricKey(id)).forEach(settings::remove)
        }
        settings.remove(KEY_STAFF_IDS)
        _state.update { it.copy(isPinSet = false, biometricEnabled = false) }
    }

    private fun load(id: String) = AppLockState(
        isPinSet = secureSettings.getStringOrNull(hashKey(id)) != null,
        biometricEnabled = settings.getBoolean(biometricKey(id), false),
    )

    private fun staffIds(): Set<String> =
        settings.getStringOrNull(KEY_STAFF_IDS)?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    private fun saltKey(id: String) = "appLock.$id.salt"
    private fun hashKey(id: String) = "appLock.$id.hash"
    private fun failureKey(id: String) = "appLock.$id.failures"
    private fun biometricKey(id: String) = "appLock.$id.biometric"

    private companion object {
        /** 비밀번호를 정해 둔 사번들. 모두 지울 때 쓴다. */
        const val KEY_STAFF_IDS = "appLock.staffIds"
    }
}
