package com.eunilsung.ods.ui.login

sealed interface LoginActions {
    data class OnIdChange(val id: String) : LoginActions
    data class OnPasswordChange(val password: String) : LoginActions

    /** 비밀번호 보기 눈 버튼. */
    data object OnTogglePasswordVisible : LoginActions

    data class OnSaveIdChange(val save: Boolean) : LoginActions

    /** 로그인 버튼 또는 키보드의 완료. */
    data object OnLoginClick : LoginActions
}
