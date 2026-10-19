package com.eunilsung.ods.ui.setting

import com.eunilsung.ods.domain.model.ListDensity

sealed interface SettingActions {
    /** 설정 화면이 열렸다. 기기의 생체 사용 가능 여부처럼 밖에서 바뀔 수 있는 값을 다시 읽는다. */
    data object OnEnter : SettingActions

    data class OnListDensityChange(val density: ListDensity) : SettingActions
    data class OnHideAmountsChange(val hide: Boolean) : SettingActions

    /** 생체 인증 스위치. 켜려면 비밀번호로 본인 확인을 먼저 받고, 끄는 것은 바로 한다. */
    data class OnBiometricToggle(val enable: Boolean) : SettingActions

    /** 비밀번호 변경(없으면 등록)을 눌렀다. */
    data object OnChangePinClick : SettingActions

    /** 인증 창이 끝났다(본인 확인됐거나 비밀번호를 바꿨다). */
    data object OnAuthDone : SettingActions

    /** 인증 창을 닫았다. */
    data object OnAuthCanceled : SettingActions

    /** 더보기 맨 위의 로그아웃. */
    data object OnLogoutClick : SettingActions
}
