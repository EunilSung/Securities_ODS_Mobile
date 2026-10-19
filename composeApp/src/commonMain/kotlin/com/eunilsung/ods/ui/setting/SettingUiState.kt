package com.eunilsung.ods.ui.setting

import com.eunilsung.ods.domain.model.AppInfo
import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.domain.model.Staff

/** 더보기 설정 화면들의 상태. */
data class SettingUiState(
    /** 로그인한 직원. 더보기 맨 위에 이름·지점을 보인다. */
    val staff: Staff? = null,
    val listDensity: ListDensity = ListDensity.COMFORTABLE,
    val maskPersonalInfo: Boolean = true,
    val appInfo: AppInfo = AppInfo(versionName = "", versionCode = 0, usesDemoData = true),
)
