package com.eunilsung.ods.di

import com.eunilsung.ods.data.local.DatabaseDriverFactory
import com.eunilsung.ods.data.local.repository.LocalIdVerificationRepositoryImpl
import com.eunilsung.ods.domain.repository.IdVerificationRepository
import com.eunilsung.ods.domain.usecase.AccountOpeningUseCases
import com.eunilsung.ods.domain.usecase.OpenAccountUseCase
import com.eunilsung.ods.domain.usecase.VerifyIdCardUseCase
import com.eunilsung.ods.ui.account.AccountOpeningViewModel
import com.eunilsung.ods.data.local.repository.LocalProductRepositoryImpl
import com.eunilsung.ods.data.mapper.ProductMapper
import com.eunilsung.ods.domain.repository.ProductRepository
import com.eunilsung.ods.domain.usecase.ObserveProductUseCase
import com.eunilsung.ods.domain.usecase.ObserveProductsUseCase
import com.eunilsung.ods.domain.usecase.ObserveSubscriptionsUseCase
import com.eunilsung.ods.domain.usecase.ProductDetailUseCases
import com.eunilsung.ods.domain.usecase.ProductListUseCases
import com.eunilsung.ods.domain.usecase.RefreshProductsUseCase
import com.eunilsung.ods.domain.usecase.SubscribeProductUseCase
import com.eunilsung.ods.domain.usecase.SubscriptionUseCases
import com.eunilsung.ods.ui.product.detail.ProductDetailViewModel
import com.eunilsung.ods.ui.product.list.ProductListViewModel
import com.eunilsung.ods.ui.product.subscribe.SubscriptionViewModel
import com.eunilsung.ods.data.local.repository.LocalSignatureRepositoryImpl
import com.eunilsung.ods.domain.repository.SignatureRepository
import com.eunilsung.ods.domain.usecase.RiskAssessmentUseCases
import com.eunilsung.ods.domain.usecase.SaveRiskAssessmentUseCase
import com.eunilsung.ods.ui.risk.RiskAssessmentViewModel
import com.eunilsung.ods.data.local.repository.LocalVisitRepositoryImpl
import com.eunilsung.ods.data.mapper.VisitMapper
import com.eunilsung.ods.domain.repository.VisitRepository
import com.eunilsung.ods.domain.usecase.AddVisitUseCase
import com.eunilsung.ods.domain.usecase.CancelVisitUseCase
import com.eunilsung.ods.domain.usecase.CompleteVisitUseCase
import com.eunilsung.ods.domain.usecase.NewVisitUseCases
import com.eunilsung.ods.domain.usecase.ObserveDayVisitsUseCase
import com.eunilsung.ods.domain.usecase.ObserveUpcomingVisitsUseCase
import com.eunilsung.ods.domain.usecase.ObserveVisitUseCase
import com.eunilsung.ods.domain.usecase.ObserveWeekVisitCountsUseCase
import com.eunilsung.ods.domain.usecase.RefreshVisitsUseCase
import com.eunilsung.ods.domain.usecase.ScheduleUseCases
import com.eunilsung.ods.domain.usecase.VisitDetailUseCases
import com.eunilsung.ods.ui.schedule.ScheduleViewModel
import com.eunilsung.ods.ui.schedule.detail.VisitDetailViewModel
import com.eunilsung.ods.ui.schedule.register.NewVisitViewModel
import com.eunilsung.ods.data.local.repository.LocalConsultSessionRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.domain.repository.ConsultSessionRepository
import com.eunilsung.ods.domain.repository.CustomerRepository
import com.eunilsung.ods.domain.usecase.AddConsultationUseCase
import com.eunilsung.ods.domain.usecase.ConsultUseCases
import com.eunilsung.ods.domain.usecase.CustomerDetailUseCases
import com.eunilsung.ods.domain.usecase.CustomerListUseCases
import com.eunilsung.ods.domain.usecase.EndConsultUseCase
import com.eunilsung.ods.domain.usecase.ObserveAccountsUseCase
import com.eunilsung.ods.domain.usecase.ObserveActiveCustomerIdUseCase
import com.eunilsung.ods.domain.usecase.ObserveConsultationsUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomerUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomersRefreshingUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomersUseCase
import com.eunilsung.ods.domain.usecase.RefreshCustomersUseCase
import com.eunilsung.ods.domain.usecase.RegisterCustomerUseCase
import com.eunilsung.ods.domain.usecase.StartConsultUseCase
import com.eunilsung.ods.ui.consult.ConsultPaneViewModel
import com.eunilsung.ods.ui.customer.consultation.ConsultationViewModel
import com.eunilsung.ods.ui.customer.detail.CustomerDetailViewModel
import com.eunilsung.ods.ui.customer.list.CustomerListViewModel
import com.eunilsung.ods.ui.customer.register.NewCustomerViewModel
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

    singleOf(::CustomerMapper)
    /** 서버가 붙으면 `Config.Server.IS_ENABLED` 로 서버 구현과 갈라 끼운다. 지금은 시연용 고객만 쓴다. */
    single<CustomerRepository> { LocalCustomerRepositoryImpl(get(), get()) }
    single<ConsultSessionRepository> { LocalConsultSessionRepositoryImpl() }
    singleOf(::ObserveCustomersUseCase)
    singleOf(::RefreshCustomersUseCase)
    singleOf(::ObserveCustomersRefreshingUseCase)
    singleOf(::ObserveCustomerUseCase)
    singleOf(::ObserveConsultationsUseCase)
    singleOf(::ObserveAccountsUseCase)
    singleOf(::RegisterCustomerUseCase)
    singleOf(::AddConsultationUseCase)
    singleOf(::ObserveActiveCustomerIdUseCase)
    singleOf(::StartConsultUseCase)
    singleOf(::EndConsultUseCase)
    singleOf(::CustomerListUseCases)
    singleOf(::CustomerDetailUseCases)
    singleOf(::ConsultUseCases)

    singleOf(::VisitMapper)
    /** 서버가 붙으면 `Config.Server.IS_ENABLED` 로 서버 구현과 갈라 끼운다. 지금은 시연용 일정만 쓴다. */
    single<VisitRepository> { LocalVisitRepositoryImpl(get(), get()) }
    singleOf(::ObserveDayVisitsUseCase)
    singleOf(::ObserveWeekVisitCountsUseCase)
    singleOf(::ObserveVisitUseCase)
    singleOf(::ObserveUpcomingVisitsUseCase)
    singleOf(::RefreshVisitsUseCase)
    singleOf(::AddVisitUseCase)
    singleOf(::CompleteVisitUseCase)
    singleOf(::CancelVisitUseCase)
    singleOf(::ScheduleUseCases)
    singleOf(::VisitDetailUseCases)
    singleOf(::NewVisitUseCases)

    singleOf(::ProductMapper)
    /** 서버가 붙으면 `Config.Server.IS_ENABLED` 로 서버 구현과 갈라 끼운다. 지금은 시연용 상품만 쓴다. */
    single<ProductRepository> { LocalProductRepositoryImpl(get(), get()) }
    singleOf(::ObserveProductsUseCase)
    singleOf(::ObserveProductUseCase)
    singleOf(::RefreshProductsUseCase)
    singleOf(::ObserveSubscriptionsUseCase)
    singleOf(::SubscribeProductUseCase)
    singleOf(::ProductListUseCases)
    singleOf(::ProductDetailUseCases)
    singleOf(::SubscriptionUseCases)

    /** 서버 모드에서는 서버가 발급기관에 진위를 묻는 구현으로 바꾼다. */
    single<IdVerificationRepository> { LocalIdVerificationRepositoryImpl() }
    singleOf(::VerifyIdCardUseCase)
    singleOf(::OpenAccountUseCase)
    singleOf(::AccountOpeningUseCases)

    /** 서버 모드에서는 서명 그림을 서버로 보내는 구현으로 바꾼다. 기기에는 지문만 남긴다. */
    single<SignatureRepository> { LocalSignatureRepositoryImpl(get()) }
    singleOf(::SaveRiskAssessmentUseCase)
    singleOf(::RiskAssessmentUseCases)

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
    viewModelOf(::CustomerListViewModel)
    viewModelOf(::CustomerDetailViewModel)
    viewModelOf(::ConsultationViewModel)
    viewModelOf(::NewCustomerViewModel)
    viewModelOf(::ConsultPaneViewModel)
    viewModelOf(::ScheduleViewModel)
    viewModelOf(::VisitDetailViewModel)
    viewModelOf(::NewVisitViewModel)
    viewModelOf(::RiskAssessmentViewModel)
    viewModelOf(::ProductListViewModel)
    viewModelOf(::ProductDetailViewModel)
    viewModelOf(::SubscriptionViewModel)
    viewModelOf(::AccountOpeningViewModel)
}
