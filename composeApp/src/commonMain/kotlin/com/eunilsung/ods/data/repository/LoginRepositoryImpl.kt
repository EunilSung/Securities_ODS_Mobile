package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.remote.server.AuthTokenStore
import com.eunilsung.ods.data.remote.server.OdsServer
import com.eunilsung.ods.data.remote.server.ServerEvents
import com.eunilsung.ods.data.remote.server.ServerResult
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.model.LogoutReason
import com.eunilsung.ods.domain.model.Staff
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.util.LoginPolicy
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 서버에 로그인하는 저장소. 틀린 횟수와 잠금은 서버가 센다. 사번 기억하기는 [local] 과 같은 자리를 쓴다.
 *
 * 비밀번호는 로그인 요청에 한 번만 실리고, 그 뒤로는 서버가 내준 토큰이 신원을 대신한다.
 * 로그인해 있는 동안 실시간 알림([ServerEvents.listen])을 듣고, 로그아웃하면 끊는다.
 */
class LoginRepositoryImpl(
    private val local: LocalLoginRepositoryImpl,
    private val server: OdsServer,
    private val tokenStore: AuthTokenStore,
    private val serverEvents: ServerEvents,
) : LoginRepository by local {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listenJob: Job? = null

    private val _currentStaff = MutableStateFlow<Staff?>(null)
    override val currentStaff: StateFlow<Staff?> = _currentStaff.asStateFlow()

    private val _lastLogoutReason = MutableStateFlow<LogoutReason?>(null)
    override val lastLogoutReason: StateFlow<LogoutReason?> = _lastLogoutReason.asStateFlow()

    override suspend fun login(request: LoginRequest, nowEpochSeconds: Long): LoginResult {
        val previousToken = tokenStore.token()
        return when (val result = server.login(request.id.trim().lowercase(), request.password)) {
            is ServerResult.Success -> {
                val staff = result.value.staff.let { Staff(it.id, it.name, it.branch, it.position) }
                tokenStore.save(result.value.token)
                revokeLater(previousToken)
                local.rememberId(staff.id)
                _lastLogoutReason.value = null
                _currentStaff.value = staff
                listenJob?.cancel()
                listenJob = scope.launch { serverEvents.listen() }
                Log.message("[Login] success ${staff.id}")
                LoginResult.Success(staff)
            }
            is ServerResult.Rejected -> when {
                result.status == STATUS_TOO_MANY_REQUESTS || result.error?.code == ApiErrorCode.TOO_MANY_ATTEMPTS ->
                    LoginResult.Locked(result.error?.lockedUntilEpochSeconds ?: (nowEpochSeconds + LoginPolicy.LOCK_SECONDS))
                result.status == STATUS_UNAUTHORIZED || result.error?.code == ApiErrorCode.INVALID_CREDENTIALS ->
                    LoginResult.Wrong(result.error?.remainingAttempts ?: 0)
                else -> LoginResult.Unreachable
            }
            ServerResult.Unreachable -> LoginResult.Unreachable
        }
    }

    override fun logout(reason: LogoutReason) {
        Log.message("[Login] logout ${_currentStaff.value?.id} ($reason)")
        listenJob?.cancel()
        listenJob = null
        revokeLater(tokenStore.token())
        tokenStore.clear()
        _lastLogoutReason.value = reason
        _currentStaff.value = null
    }

    override fun clearLogoutReason() {
        _lastLogoutReason.value = null
    }

    /** 더 쓰지 않는 토큰을 서버에서 지운다. 실패해도 앱 동작에는 영향이 없어 기다리지 않는다. */
    private fun revokeLater(token: String) {
        if (token.isBlank()) return
        scope.launch { server.logout(token) }
    }

    private companion object {
        const val STATUS_UNAUTHORIZED = 401
        const val STATUS_TOO_MANY_REQUESTS = 429
    }
}
