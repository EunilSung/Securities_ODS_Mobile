package com.eunilsung.ods.ui.applock

/** 잠금 화면을 무엇 때문에 띄웠는지. */
enum class AppLockPurpose {
    /** 잠겨 있다 — 비밀번호나 생체로 푼다. */
    UNLOCK,

    /** 로그인했는데 이 직원의 잠금 비밀번호가 없다 — 정해야 업무 화면이 열린다. */
    REGISTER,

    /** 설정에서 비밀번호를 바꾼다 — 지금 비밀번호를 맞힌 뒤 새 비밀번호를 두 번 받는다. */
    CHANGE,
}

/** 잠금 화면의 상태. */
data class AppLockUiState(
    val purpose: AppLockPurpose = AppLockPurpose.UNLOCK,
    val step: AppLockStep = AppLockStep.Loading,
    /** 잠금 화면 위에 보일 직원 이름. */
    val staffName: String = "",
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
    /** 처음 정할 때 생체 인증도 함께 쓸지. */
    val useBiometricOnRegister: Boolean = true,
    /** 틀릴 때마다 바뀌어 키패드 입력을 비우고 배열을 다시 섞는다. */
    val errorKey: Int = 0,
) {
    /** 잠금을 풀 때 생체 버튼을 보일지. */
    val showsBiometricButton: Boolean
        get() = purpose == AppLockPurpose.UNLOCK && biometricEnabled && biometricAvailable

    /** 처음 정할 때 "생체 인증도 사용" 을 보일지. 변경 중에는 생체 설정을 건드리지 않는다. */
    val offersBiometricOnRegister: Boolean
        get() = purpose == AppLockPurpose.REGISTER && biometricAvailable
}

sealed interface AppLockStep {
    data object Loading : AppLockStep

    /** 생체 인증 창이 떠 있다. */
    data object Biometric : AppLockStep

    /** 지금 비밀번호를 넣는다(풀기·변경 전 확인). */
    data class EnterPin(val notice: PinNotice?) : AppLockStep

    /** 새 비밀번호를 정한다. */
    data class NewPin(val notice: PinNotice?) : AppLockStep

    /** 새 비밀번호를 한 번 더 넣는다. */
    data object ConfirmPin : AppLockStep
}

/** 키패드 위에 띄우는 알림. */
sealed interface PinNotice {
    data class Wrong(val remainingAttempts: Int) : PinNotice

    /** 같은 숫자·연속 숫자라 정하지 않았다. */
    data object Weak : PinNotice

    /** 확인 입력이 처음 입력과 달랐다. */
    data object Mismatch : PinNotice

    /** 새 비밀번호가 지금 비밀번호와 같다. */
    data object SameAsCurrent : PinNotice

    /** 생체 인증을 하지 못해 비밀번호로 넘어왔다. */
    data object BiometricFailed : PinNotice
}
