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
    val biometricEnabled: Boolean = false,
    /** 기기에 쓸 수 있는 생체가 있는지. 없으면 생체 스위치를 막는다. */
    val biometricAvailable: Boolean = false,
    /** 잠금 비밀번호 변경 창이 떠 있는지. */
    val isChangingPin: Boolean = false,
    val appInfo: AppInfo = AppInfo(versionName = "", versionCode = 0, usesDemoData = true),
    /** 방금 끝난 일 알림. 화면을 다시 열면 지운다. */
    val notice: SettingNotice? = null,
)

enum class SettingNotice { PIN_CHANGED, BIOMETRIC_ENABLED, BIOMETRIC_FAILED }
