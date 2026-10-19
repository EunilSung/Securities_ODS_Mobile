package com.eunilsung.ods.ui.setting

import com.eunilsung.ods.domain.model.ListDensity

sealed interface SettingActions {
    data class OnListDensityChange(val density: ListDensity) : SettingActions
    data class OnMaskPersonalInfoChange(val mask: Boolean) : SettingActions

    /** 더보기 맨 위의 로그아웃. */
    data object OnLogoutClick : SettingActions
}
