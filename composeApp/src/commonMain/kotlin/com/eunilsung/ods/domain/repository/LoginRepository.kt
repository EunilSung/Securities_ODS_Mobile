package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.Staff
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import kotlinx.coroutines.flow.StateFlow

/**
 * 로그인. 비밀번호는 어디에도 저장하지 않는다 — 앱을 켤 때마다 로그인하고, 아이디만 기억할 수 있다.
 */
interface LoginRepository {
    /** 로그인한 고객. 로그인 전·로그아웃 뒤에는 null. */
    val currentStaff: StateFlow<Staff?>

    /** [nowEpochSeconds] 는 잠금 계산에 쓴다. */
    suspend fun login(request: LoginRequest, nowEpochSeconds: Long): LoginResult

    fun logout()

    /** 기억해 둔 아이디. 없으면 null. */
    fun savedId(): String?

    fun isSaveId(): Boolean

    /** 아이디 저장을 끄면 기억해 둔 아이디도 지운다. */
    fun setSaveId(save: Boolean)
}
