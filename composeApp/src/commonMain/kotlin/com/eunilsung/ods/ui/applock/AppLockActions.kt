package com.eunilsung.ods.ui.applock

sealed interface AppLockActions {
    /** 잠금 화면이 떴다. [purpose] 에 맞는 첫 단계로 간다. */
    data class OnStart(val purpose: AppLockPurpose, val prompt: BiometricPromptText) : AppLockActions

    /** 키패드로 비밀번호를 다 넣었다. */
    data class OnPinEntered(val pin: String) : AppLockActions

    /** "생체 인증으로 풀기" 를 눌렀다. */
    data class OnBiometricClick(val prompt: BiometricPromptText) : AppLockActions

    /** 비밀번호를 처음 정할 때 생체 인증도 쓸지. */
    data class OnUseBiometricChange(val use: Boolean) : AppLockActions

    /** 잠금 화면에서 "다른 사번으로 로그인" 을 눌렀다. */
    data object OnLogoutClick : AppLockActions
}

/** 잠금 화면이 한 번씩 처리할 일. */
sealed interface AppLockEvent {
    /** 비밀번호를 바꿨다. 변경 창을 닫는다. */
    data object PinChanged : AppLockEvent
}

/** 생체 인증 창의 문구. 화면이 리소스에서 읽어 넘긴다. */
data class BiometricPromptText(val title: String, val subtitle: String, val negative: String)
