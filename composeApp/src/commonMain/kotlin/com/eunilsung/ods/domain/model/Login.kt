package com.eunilsung.ods.domain.model

/** 로그인한 고객. */
data class Customer(
    val id: String,
    val name: String,
    /** 종합 계좌번호(하이픈 포함). 화면에는 가려서 보인다. */
    val accountNo: String,
)

data class LoginRequest(
    val id: String,
    val password: String,
)

/**
 * 로그인 결과. 아이디가 없는 것과 비밀번호가 틀린 것을 가르지 않는다 — 어느 아이디가 있는지 드러내지 않기 위해서다.
 */
sealed interface LoginResult {
    data class Success(val customer: Customer) : LoginResult

    /** 아이디 또는 비밀번호가 맞지 않다. [remainingAttempts] 번 더 틀리면 잠긴다. */
    data class Wrong(val remainingAttempts: Int) : LoginResult

    data class Locked(val untilEpochSeconds: Long) : LoginResult

    /** 서버에 닿지 못했다. 다시 시도할 수 있다. */
    data object Unreachable : LoginResult
}
