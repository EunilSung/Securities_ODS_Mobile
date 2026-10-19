package com.eunilsung.ods.domain.model

/**
 * 앱 정보.
 *
 * @param usesDemoData 서버에 붙지 않고 기기 안 시연용 시세·계좌로 도는지. 실제 시세가 아니라는 것을 화면에 알린다.
 */
data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val usesDemoData: Boolean,
)
