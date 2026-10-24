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
    /** 앱 잠금 비밀번호를 너무 많이 틀려 업무 데이터를 지우고 로그아웃됐다. 다음 로그인까지 안내한다. */
    val dataWiped: Boolean = false,
) {
    val canSubmit: Boolean get() = id.isNotBlank() && password.isNotBlank() && !isLoading
}

sealed interface LoginError {
    data class Wrong(val remainingAttempts: Int) : LoginError
    data class Locked(val untilEpochSeconds: Long) : LoginError
    data object Failed : LoginError
}
