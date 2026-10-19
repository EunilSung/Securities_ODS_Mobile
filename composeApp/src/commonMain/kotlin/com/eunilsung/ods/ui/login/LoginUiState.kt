package com.eunilsung.ods.ui.login

/** 로그인 화면의 상태. */
data class LoginUiState(
    val id: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val saveId: Boolean = true,
    val isLoading: Boolean = false,
    /** 마지막 시도가 실패한 이유. 입력을 고치면 지운다. */
    val error: LoginError? = null,
    /** 시연용 데이터로 도는지 — 그렇다면 시연용 계정을 안내한다. */
    val usesDemoData: Boolean = true,
) {
    val canSubmit: Boolean get() = id.isNotBlank() && password.isNotBlank() && !isLoading
}

sealed interface LoginError {
    data class Wrong(val remainingAttempts: Int) : LoginError
    data class Locked(val untilEpochSeconds: Long) : LoginError
    data object Unreachable : LoginError
}
