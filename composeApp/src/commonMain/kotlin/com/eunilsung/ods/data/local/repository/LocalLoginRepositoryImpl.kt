package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.data.local.seed.DemoAccounts
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.util.LoginPolicy
import com.eunilsung.ods.util.Log
import com.russhwolf.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 서버 없이 도는 로그인 — [DemoAccounts] 로만 인증하고, 아이디별 틀린 횟수와 잠금을 기기에 센다.
 *
 * Talk 의 같은 클래스와 달리 **비밀번호를 저장하지 않는다**(자동 로그인 없음). 기억하는 것은 아이디뿐이다.
 * 없는 아이디도 틀린 횟수를 세고 같은 결과를 돌려준다 — 어느 아이디가 있는지 드러내지 않는다.
 */
class LocalLoginRepositoryImpl(
    private val settings: Settings,
    private val simulatedRoundTripMillis: Long = SIMULATED_ROUND_TRIP_MILLIS,
) : LoginRepository {

    private val _currentCustomer = MutableStateFlow<Customer?>(null)
    override val currentCustomer: StateFlow<Customer?> = _currentCustomer.asStateFlow()

    override suspend fun login(request: LoginRequest, nowEpochSeconds: Long): LoginResult {
        delay(simulatedRoundTripMillis)
        val id = request.id.trim().lowercase()
        val lockedUntil = settings.getLongOrNull(lockKey(id))
        if (lockedUntil != null) {
            if (nowEpochSeconds < lockedUntil) return LoginResult.Locked(lockedUntil)
            settings.remove(lockKey(id))
            settings.remove(failureKey(id))
        }

        val customer = DemoAccounts.find(id)
        if (customer == null || request.password != DemoAccounts.PASSWORD) {
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
        if (isSaveId()) settings.putString(KEY_SAVED_ID, customer.id) else settings.remove(KEY_SAVED_ID)
        _currentCustomer.value = customer
        Log.message("[Login/Local] success ${customer.id}")
        return LoginResult.Success(customer)
    }

    override fun logout() {
        Log.message("[Login/Local] logout ${_currentCustomer.value?.id}")
        _currentCustomer.value = null
    }

    override fun savedId(): String? = if (isSaveId()) settings.getStringOrNull(KEY_SAVED_ID) else null

    override fun isSaveId(): Boolean = settings.getBoolean(KEY_SAVE_ID, true)

    override fun setSaveId(save: Boolean) {
        settings.putBoolean(KEY_SAVE_ID, save)
        if (!save) settings.remove(KEY_SAVED_ID)
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
