package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.Config
import com.eunilsung.ods.domain.model.AppInfo
import com.eunilsung.ods.domain.repository.AppVersionProvider

/** @param usesDemoData 시연용 데이터로 도는가. 서버 주소가 없는 빌드(로컬 모드)면 참이다. */
class GetAppInfoUseCase(
    private val provider: AppVersionProvider,
    private val usesDemoData: Boolean = !Config.Server.IS_ENABLED,
) {
    operator fun invoke(): AppInfo = AppInfo(
        versionName = provider.versionName(),
        versionCode = provider.versionCode(),
        usesDemoData = usesDemoData,
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
    val observeAppLock: ObserveAppLockUseCase,
    val setAppBiometricEnabled: SetAppBiometricEnabledUseCase,
    val isBiometricAvailable: IsBiometricAvailableUseCase,
    val authenticateBiometric: AuthenticateBiometricUseCase,
    val lockApp: LockAppUseCase,
)
