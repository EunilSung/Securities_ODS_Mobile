package com.eunilsung.ods.ui.setting

import com.eunilsung.ods.domain.model.AppInfo
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.ui.orderauth.OrderAuthPurpose

/** 더보기 설정 화면들의 상태. */
data class SettingUiState(
    /** 로그인한 고객. 더보기 맨 위에 이름·계좌를 보인다. */
    val customer: Customer? = null,
    val listDensity: ListDensity = ListDensity.COMFORTABLE,
    val hideAmounts: Boolean = false,
    val isPinSet: Boolean = false,
    val biometricEnabled: Boolean = false,
    /** 기기에 쓸 수 있는 생체가 있는지. 없으면 생체 스위치를 막는다. */
    val biometricAvailable: Boolean = false,
    /** 주문 인증이 잠겨 있으면 풀리는 시각. */
    val lockedUntilEpochSeconds: Long? = null,
    val appInfo: AppInfo = AppInfo(versionName = "", versionCode = 0, usesDemoData = true),
    /** 떠 있는 인증 창. 없으면 null. */
    val authPurpose: OrderAuthPurpose? = null,
    /** 방금 끝난 일 알림(비밀번호를 바꿨다 등). 화면을 다시 열면 지운다. */
    val notice: SettingNotice? = null,
)

enum class SettingNotice { PIN_CHANGED, PIN_REGISTERED, BIOMETRIC_ENABLED }
