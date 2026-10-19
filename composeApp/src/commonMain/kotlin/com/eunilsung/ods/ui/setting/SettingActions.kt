package com.eunilsung.ods.ui.setting

import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.ui.applock.BiometricPromptText

sealed interface SettingActions {
    /** 설정 상세가 열렸다. 기기의 생체 사용 가능 여부처럼 밖에서 바뀔 수 있는 값을 다시 읽는다. */
    data object OnEnter : SettingActions

    data class OnListDensityChange(val density: ListDensity) : SettingActions
    data class OnMaskPersonalInfoChange(val mask: Boolean) : SettingActions

    /** 생체 인증 스위치. 켤 때는 생체 인증을 한 번 통과해야 켜지고, 끄는 것은 바로 한다. */
    data class OnBiometricToggle(val enable: Boolean, val prompt: BiometricPromptText) : SettingActions

    /** 잠금 비밀번호 변경을 눌렀다. 변경 창을 연다. */
    data object OnChangePinClick : SettingActions

    /** 변경 창에서 비밀번호를 바꿨다. */
    data object OnPinChanged : SettingActions

    /** 변경 창을 닫았다. */
    data object OnChangePinClose : SettingActions

    /** 지금 잠그기. 고객 앞에서 자리를 비울 때 쓴다. */
    data object OnLockNowClick : SettingActions

    /** 더보기 맨 위의 로그아웃. */
    data object OnLogoutClick : SettingActions
}
