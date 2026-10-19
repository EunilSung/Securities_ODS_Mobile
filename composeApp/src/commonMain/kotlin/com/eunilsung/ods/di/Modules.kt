package com.eunilsung.ods.di

import com.eunilsung.ods.data.local.DatabaseDriverFactory
import com.eunilsung.ods.data.local.LocalDataWiperImpl
import com.eunilsung.ods.data.local.repository.LocalAppLockRepositoryImpl
import com.eunilsung.ods.domain.repository.AppLockRepository
import com.eunilsung.ods.domain.repository.LocalDataWiper
import com.eunilsung.ods.domain.usecase.AppLockUseCases
import com.eunilsung.ods.domain.usecase.AuthenticateBiometricUseCase
import com.eunilsung.ods.domain.usecase.ClearLogoutReasonUseCase
import com.eunilsung.ods.domain.usecase.GetLogoutReasonUseCase
import com.eunilsung.ods.domain.usecase.IsBiometricAvailableUseCase
import com.eunilsung.ods.domain.usecase.LockAppUseCase
import com.eunilsung.ods.domain.usecase.LockIfInactiveUseCase
import com.eunilsung.ods.domain.usecase.MainUseCases
import com.eunilsung.ods.domain.usecase.ObserveAppLockUseCase
import com.eunilsung.ods.domain.usecase.RecordActivityUseCase
import com.eunilsung.ods.domain.usecase.RecordBackgroundUseCase
import com.eunilsung.ods.domain.usecase.RegisterAppPinUseCase
import com.eunilsung.ods.domain.usecase.SetAppBiometricEnabledUseCase
import com.eunilsung.ods.domain.usecase.UnlockWithBiometricUseCase
import com.eunilsung.ods.domain.usecase.UnlockWithPinUseCase
import com.eunilsung.ods.ui.applock.AppLockViewModel
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalQuoteRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalStockRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalTickSimulator
import com.eunilsung.ods.data.local.repository.LocalUserPreferenceRepositoryImpl
import com.eunilsung.ods.data.mapper.StockMapper
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.repository.QuoteRepository
import com.eunilsung.ods.domain.repository.StockRepository
import com.eunilsung.ods.domain.repository.UserPreferenceRepository
import com.eunilsung.ods.domain.usecase.FetchWatchlistUseCase
import com.eunilsung.ods.domain.usecase.GetAppInfoUseCase
import com.eunilsung.ods.domain.usecase.GetSavedIdUseCase
import com.eunilsung.ods.domain.usecase.IsSaveIdUseCase
import com.eunilsung.ods.domain.usecase.LoginUseCase
import com.eunilsung.ods.domain.usecase.LoginUseCases
import com.eunilsung.ods.domain.usecase.LogoutUseCase
import com.eunilsung.ods.domain.usecase.ObserveCandlesUseCase
import com.eunilsung.ods.domain.usecase.ObserveListDensityUseCase
import com.eunilsung.ods.domain.usecase.ObserveMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.ObserveOrderBookUseCase
import com.eunilsung.ods.domain.usecase.ObserveStaffUseCase
import com.eunilsung.ods.domain.usecase.ObserveStockUseCase
import com.eunilsung.ods.domain.usecase.ObserveWatchlistFetchingUseCase
import com.eunilsung.ods.domain.usecase.ObserveWatchlistUseCase
import com.eunilsung.ods.domain.usecase.QuoteUseCases
import com.eunilsung.ods.domain.usecase.SetListDensityUseCase
import com.eunilsung.ods.domain.usecase.SetMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.SetSaveIdUseCase
import com.eunilsung.ods.domain.usecase.SettingUseCases
import com.eunilsung.ods.domain.usecase.StockUseCases
import com.eunilsung.ods.ui.login.LoginViewModel
import com.eunilsung.ods.ui.main.MainViewModel
import com.eunilsung.ods.ui.quote.QuoteViewModel
import com.eunilsung.ods.ui.setting.SettingViewModel
import com.eunilsung.ods.ui.watchlist.WatchlistViewModel
import com.russhwolf.settings.Settings
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    single { Settings() }

    single<AppDatabase> {
        val factory: DatabaseDriverFactory = get()
        AppDatabase(factory.createDriver())
    }

    single<UserPreferenceRepository> { LocalUserPreferenceRepositoryImpl(get()) }
    singleOf(::ObserveListDensityUseCase)
    singleOf(::SetListDensityUseCase)
    singleOf(::ObserveMaskPersonalInfoUseCase)
    singleOf(::SetMaskPersonalInfoUseCase)

    singleOf(::StockMapper)
    /** 서버가 붙으면 `Config.Server.IS_ENABLED` 로 서버 구현과 갈라 끼운다. 지금은 시연용 데이터만 쓴다. */
    single<StockRepository> { LocalStockRepositoryImpl(get(), get()) }
    singleOf(::ObserveWatchlistUseCase)
    singleOf(::FetchWatchlistUseCase)
    singleOf(::ObserveWatchlistFetchingUseCase)
    singleOf(::ObserveStockUseCase)
    singleOf(::StockUseCases)

    /** 서버 모드에서는 실시간 시세를 서버 알림이 채우므로 시세 흉내([LocalTickSimulator])를 쓰지 않는다. */
    singleOf(::LocalTickSimulator)
    single<QuoteRepository> { LocalQuoteRepositoryImpl(get(), get()) }
    singleOf(::ObserveOrderBookUseCase)
    singleOf(::ObserveCandlesUseCase)
    singleOf(::QuoteUseCases)

    singleOf(::GetAppInfoUseCase)
    singleOf(::SettingUseCases)

    /** 앱 잠금은 서버 모드에서도 기기 안의 일이라 이 구현을 그대로 쓴다. */
    single<AppLockRepository> { LocalAppLockRepositoryImpl(get()) }
    single<LocalDataWiper> { LocalDataWiperImpl(get()) }
    singleOf(::ObserveAppLockUseCase)
    singleOf(::RegisterAppPinUseCase)
    singleOf(::UnlockWithPinUseCase)
    singleOf(::UnlockWithBiometricUseCase)
    singleOf(::SetAppBiometricEnabledUseCase)
    singleOf(::IsBiometricAvailableUseCase)
    singleOf(::AuthenticateBiometricUseCase)
    singleOf(::LockAppUseCase)
    singleOf(::RecordActivityUseCase)
    singleOf(::RecordBackgroundUseCase)
    singleOf(::LockIfInactiveUseCase)
    singleOf(::GetLogoutReasonUseCase)
    singleOf(::ClearLogoutReasonUseCase)
    singleOf(::AppLockUseCases)
    singleOf(::MainUseCases)

    /** 서버가 붙으면 `Config.Server.IS_ENABLED` 로 서버 로그인과 갈라 끼운다. */
    single<LoginRepository> { LocalLoginRepositoryImpl(get()) }
    singleOf(::LoginUseCase)
    singleOf(::LogoutUseCase)
    singleOf(::ObserveStaffUseCase)
    singleOf(::GetSavedIdUseCase)
    singleOf(::IsSaveIdUseCase)
    singleOf(::SetSaveIdUseCase)
    singleOf(::LoginUseCases)

    viewModelOf(::MainViewModel)
    viewModelOf(::WatchlistViewModel)
    viewModelOf(::QuoteViewModel)
    viewModelOf(::SettingViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::AppLockViewModel)
}
