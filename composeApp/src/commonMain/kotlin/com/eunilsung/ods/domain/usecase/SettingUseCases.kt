package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.Config
import com.eunilsung.ods.domain.model.AppInfo
import com.eunilsung.ods.domain.repository.AppVersionProvider

class GetAppInfoUseCase(private val provider: AppVersionProvider) {
    operator fun invoke(): AppInfo = AppInfo(
        versionName = provider.versionName(),
        versionCode = provider.versionCode(),
        usesDemoData = !Config.Server.IS_ENABLED,
    )
}

/** 더보기 설정 화면이 쓰는 유스케이스 묶음. */
class SettingUseCases(
    val observeListDensity: ObserveListDensityUseCase,
    val setListDensity: SetListDensityUseCase,
    val observeMaskPersonalInfo: ObserveMaskPersonalInfoUseCase,
    val setMaskPersonalInfo: SetMaskPersonalInfoUseCase,
    val getAppInfo: GetAppInfoUseCase,
    val observeStaff: ObserveStaffUseCase,
    val logout: LogoutUseCase,
)
