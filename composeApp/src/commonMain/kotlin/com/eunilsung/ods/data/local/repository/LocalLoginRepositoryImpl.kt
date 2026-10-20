package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.data.local.seed.DemoAccounts
import com.eunilsung.ods.domain.model.Staff
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.model.LogoutReason
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.util.LoginPolicy
import com.eunilsung.ods.util.Log
import com.eunilsung.ods.data.local.SecureSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 서버 없이 도는 로그인 — [DemoAccounts] 로만 인증하고, 사번별 틀린 횟수와 잠금을 기기에 센다.
 *
 * Talk 의 같은 클래스와 달리 **비밀번호를 저장하지 않는다**(자동 로그인 없음). 기억하는 것은 사번뿐이다.
 * 없는 사번도 틀린 횟수를 세고 같은 결과를 돌려준다 — 어느 사번이 있는지 드러내지 않는다.
 * 기억한 사번은 [secureSettings] 로 잠가 둔다.
 */
class LocalLoginRepositoryImpl(
    private val settings: Settings,
    private val secureSettings: SecureSettings,
    private val simulatedRoundTripMillis: Long = SIMULATED_ROUND_TRIP_MILLIS,
) : LoginRepository {

    private val _currentStaff = MutableStateFlow<Staff?>(null)
    override val currentStaff: StateFlow<Staff?> = _currentStaff.asStateFlow()

    override suspend fun login(request: LoginRequest, nowEpochSeconds: Long): LoginResult {
        delay(simulatedRoundTripMillis)
        val id = request.id.trim().lowercase()
        val lockedUntil = settings.getLongOrNull(lockKey(id))
        if (lockedUntil != null) {
            if (nowEpochSeconds < lockedUntil) return LoginResult.Locked(lockedUntil)
            settings.remove(lockKey(id))
            settings.remove(failureKey(id))
        }

        val staff = DemoAccounts.find(id)
        if (staff == null || request.password != DemoAccounts.PASSWORD) {
            val failures = settings.getInt(failureKey(id), 0) + 1
            return if (failures >= LoginPolicy.MAX_ATTEMPTS) {
                val until = nowEpochSeconds + LoginPolicy.LOCK_SECONDS
                settings.remove(failureKey(id))
                settings.putLong(lockKey(id), until)
                LoginResult.Locked(until)
            } else {
                settings.putInt(failureKey(id), failures)
                LoginResult.Wrong(LoginPolicy.MAX_ATTEMPTS - failures)
            }
        }

        settings.remove(failureKey(id))
        if (isSaveId()) secureSettings.putString(KEY_SAVED_ID, staff.id) else secureSettings.remove(KEY_SAVED_ID)
        _lastLogoutReason.value = null
        _currentStaff.value = staff
        Log.message("[Login/Local] success ${staff.id}")
        return LoginResult.Success(staff)
    }

    private val _lastLogoutReason = MutableStateFlow<LogoutReason?>(null)
    override val lastLogoutReason: StateFlow<LogoutReason?> = _lastLogoutReason.asStateFlow()

    override fun logout(reason: LogoutReason) {
        Log.message("[Login/Local] logout ${_currentStaff.value?.id} ($reason)")
        _lastLogoutReason.value = reason
        _currentStaff.value = null
    }

    override fun clearLogoutReason() {
        _lastLogoutReason.value = null
    }

    override fun savedId(): String? = if (isSaveId()) secureSettings.getStringOrNull(KEY_SAVED_ID) else null

    override fun isSaveId(): Boolean = settings.getBoolean(KEY_SAVE_ID, true)

    override fun setSaveId(save: Boolean) {
        settings.putBoolean(KEY_SAVE_ID, save)
        if (!save) secureSettings.remove(KEY_SAVED_ID)
    }

    private fun failureKey(id: String) = "login.failures.$id"
    private fun lockKey(id: String) = "login.lockedUntil.$id"

    private companion object {
        const val KEY_SAVE_ID = "login.saveId"
        const val KEY_SAVED_ID = "login.savedId"

        /** 서버에 다녀오는 시간을 흉내 낸다. 로그인 버튼의 대기 표시가 보이게 한다. */
        const val SIMULATED_ROUND_TRIP_MILLIS = 400L
    }
}
