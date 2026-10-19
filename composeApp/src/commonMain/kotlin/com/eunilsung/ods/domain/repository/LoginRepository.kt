package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.Staff
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.model.LogoutReason
import kotlinx.coroutines.flow.StateFlow

/**
 * 직원 로그인. 비밀번호는 어디에도 저장하지 않는다 — 앱을 켤 때마다 로그인하고, 사번만 기억할 수 있다.
 */
interface LoginRepository {
    /** 로그인한 직원. 로그인 전·로그아웃 뒤에는 null. */
    val currentStaff: StateFlow<Staff?>

    /** [nowEpochSeconds] 는 잠금 계산에 쓴다. */
    suspend fun login(request: LoginRequest, nowEpochSeconds: Long): LoginResult

    fun logout(reason: LogoutReason = LogoutReason.USER)

    /** 마지막 로그아웃 까닭. 로그인 화면이 한 번 안내한 뒤 [clearLogoutReason] 으로 지운다. 아직 없으면 null. */
    val lastLogoutReason: StateFlow<LogoutReason?>

    fun clearLogoutReason()

    /** 기억해 둔 사번. 없으면 null. */
    fun savedId(): String?

    fun isSaveId(): Boolean

    /** 사번 저장을 끄면 기억해 둔 사번도 지운다. */
    fun setSaveId(save: Boolean)
}
