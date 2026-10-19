package com.eunilsung.ods.ui.orderauth

/** 주문 인증 창의 상태. */
data class OrderAuthUiState(
    val step: OrderAuthStep = OrderAuthStep.Loading,
    val purpose: OrderAuthPurpose = OrderAuthPurpose.AUTHORIZE,
    /** 비밀번호 변경 중 현재 비밀번호를 확인했고 새 비밀번호를 정하는 중이다. 제목을 바꾸는 데 쓴다. */
    val isSettingNewPin: Boolean = false,
    /** 기기에 쓸 수 있는 생체가 있는지. */
    val biometricAvailable: Boolean = false,
    /** 생체 인증을 켜 두었는지. 켜져 있고 쓸 수 있으면 비밀번호 화면에 "생체 인증으로 확인" 을 보인다. */
    val biometricEnabled: Boolean = false,
    /** 등록할 때 생체 인증도 함께 쓸지. */
    val useBiometricOnRegister: Boolean = true,
    /** 틀릴 때마다 바뀌어 키패드 입력을 비운다. */
    val errorKey: Int = 0,
) {
    /**
     * 처음 비밀번호를 정할 때 "생체 인증도 함께 사용" 을 보일지 — 주문 중 첫 등록이고 생체를 쓸 수 있을 때만.
     * 비밀번호 변경·본인 확인에서는 생체 설정을 건드리지 않는다.
     */
    val offersBiometricOnRegister: Boolean
        get() = purpose == OrderAuthPurpose.AUTHORIZE && biometricAvailable && !isSettingNewPin
}

sealed interface OrderAuthStep {
    data object Loading : OrderAuthStep

    /** 생체 인증 프롬프트가 떠 있다. */
    data object Biometric : OrderAuthStep

    data class EnterPin(val notice: PinNotice?) : OrderAuthStep

    /** 처음 주문 — 비밀번호를 정한다. */
    data class RegisterPin(val notice: PinNotice?) : OrderAuthStep

    /** 정한 비밀번호를 한 번 더 넣는다. */
    data object ConfirmPin : OrderAuthStep

    data class Locked(val untilEpochSeconds: Long) : OrderAuthStep
}

/** 키패드 위에 띄우는 알림. */
sealed interface PinNotice {
    data class Wrong(val remainingAttempts: Int) : PinNotice

    /** 같은 숫자·연속 숫자라 등록하지 않았다. */
    data object Weak : PinNotice

    /** 확인 입력이 처음 입력과 달랐다. */
    data object Mismatch : PinNotice

    /** 새 비밀번호가 지금 비밀번호와 같다. */
    data object SameAsCurrent : PinNotice

    /** 생체 인증을 하지 못해 비밀번호로 넘어왔다. */
    data object BiometricFailed : PinNotice
}
