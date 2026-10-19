package com.eunilsung.ods.ui.orderauth

/** 생체 인증 프롬프트 문구. ViewModel 은 문자열 리소스를 모르므로 화면이 넘긴다. */
data class BiometricPromptText(
    val title: String,
    val subtitle: String,
    val negative: String,
)

/** 인증 창을 왜 여는지. */
enum class OrderAuthPurpose {
    /** 주문·정정·취소 직전. 생체 인증을 켜 두었으면 생체부터 묻는다. */
    AUTHORIZE,

    /** 설정을 바꾸기 전 본인 확인(생체 인증 켜기). 비밀번호만 받는다. */
    VERIFY_PIN,

    /** 비밀번호 변경 — 현재 비밀번호 → 새 비밀번호 → 한 번 더. */
    CHANGE_PIN,
}

sealed interface OrderAuthActions {
    /** 인증 창이 열렸다. 잠금 → 등록 → (주문이면) 생체 → 비밀번호 순으로 시작 단계를 고른다. */
    data class OnStart(val prompt: BiometricPromptText, val purpose: OrderAuthPurpose) : OrderAuthActions

    /** 키패드로 4자리를 다 넣었다. */
    data class OnPinEntered(val pin: String) : OrderAuthActions

    /** 비밀번호 화면에서 생체 인증으로 다시 해 본다. */
    data class OnRetryBiometric(val prompt: BiometricPromptText) : OrderAuthActions

    /** 비밀번호를 등록할 때 생체 인증도 쓸지 바꿨다. */
    data class OnUseBiometricChange(val use: Boolean) : OrderAuthActions

    /** 창을 닫았다. 진행 중인 생체 인증도 그만둔다. */
    data object OnCancel : OrderAuthActions
}

/** 인증 창이 한 번 처리할 일. */
sealed interface OrderAuthEvent {
    /** 끝났다 — 인증됐거나(주문·본인 확인) 비밀번호를 바꿨다. 부른 쪽이 다음 일을 한다. */
    data object Authorized : OrderAuthEvent
}
