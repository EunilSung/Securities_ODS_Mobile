package com.eunilsung.ods.di

import com.eunilsung.ods.data.local.DatabaseDriverFactory
import com.eunilsung.ods.data.local.LocalKeyGuard
import com.eunilsung.ods.data.local.LocalSecret
import com.eunilsung.ods.data.local.SecureSettings
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
import com.eunilsung.ods.domain.usecase.ObserveVisitCountsUseCase
import com.eunilsung.ods.domain.usecase.ObserveVisitUseCase
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
import com.eunilsung.ods.domain.usecase.ObserveScheduleCalendarModeUseCase
import com.eunilsung.ods.domain.usecase.ObserveMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.ObserveOrderBookUseCase
import com.eunilsung.ods.domain.usecase.ObserveStaffUseCase
import com.eunilsung.ods.domain.usecase.ObserveStockUseCase
import com.eunilsung.ods.domain.usecase.ObserveWatchlistFetchingUseCase
import com.eunilsung.ods.domain.usecase.ObserveWatchlistUseCase
import com.eunilsung.ods.domain.usecase.QuoteUseCases
import com.eunilsung.ods.domain.usecase.SetListDensityUseCase
import com.eunilsung.ods.domain.usecase.SetScheduleCalendarModeUseCase
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
    /** 개인정보 칸·설정을 잠그는 키는 플랫폼 보안 저장소에 있다(`platformModule` 의 `LocalSecretCipher`). */
    single { LocalSecret(get()) }
    single { SecureSettings(get(), get()) }

    /** DB 를 열자마자 그 DB 를 잠근 키와 짝이 맞는지 본다. 맞지 않으면 풀 수 없는 업무 데이터를 지운다. */
    single<AppDatabase> {
        val factory: DatabaseDriverFactory = get()
        AppDatabase(factory.createDriver()).also { LocalKeyGuard(it, get(), get()).ensureReadable() }
    }
    single { LocalKeyGuard(get(), get(), get()) }

    single<UserPreferenceRepository> { LocalUserPreferenceRepositoryImpl(get()) }
    singleOf(::ObserveListDensityUseCase)
    singleOf(::SetListDensityUseCase)
    singleOf(::ObserveScheduleCalendarModeUseCase)
    singleOf(::SetScheduleCalendarModeUseCase)
    singleOf(::ObserveMaskPersonalInfoUseCase)
    singleOf(::SetMaskPersonalInfoUseCase)

    singleOf(::StockMapper)
    single<StockRepository> { LocalStockRepositoryImpl(get(), get()) }
    singleOf(::ObserveWatchlistUseCase)
    singleOf(::FetchWatchlistUseCase)
    singleOf(::ObserveWatchlistFetchingUseCase)
    singleOf(::ObserveStockUseCase)
    singleOf(::StockUseCases)

    singleOf(::LocalTickSimulator)
    single<QuoteRepository> { LocalQuoteRepositoryImpl(get(), get()) }
    singleOf(::ObserveOrderBookUseCase)
    singleOf(::ObserveCandlesUseCase)
    singleOf(::QuoteUseCases)

    singleOf(::GetAppInfoUseCase)
    singleOf(::SettingUseCases)

    singleOf(::CustomerMapper)
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
    single<VisitRepository> { LocalVisitRepositoryImpl(get(), get()) }
    singleOf(::ObserveDayVisitsUseCase)
    singleOf(::ObserveVisitCountsUseCase)
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
    single<ProductRepository> { LocalProductRepositoryImpl(get(), get()) }
    singleOf(::ObserveProductsUseCase)
    singleOf(::ObserveProductUseCase)
    singleOf(::RefreshProductsUseCase)
    singleOf(::ObserveSubscriptionsUseCase)
    singleOf(::SubscribeProductUseCase)
    singleOf(::ProductListUseCases)
    singleOf(::ProductDetailUseCases)
    singleOf(::SubscriptionUseCases)

    single<IdVerificationRepository> { LocalIdVerificationRepositoryImpl() }
    singleOf(::VerifyIdCardUseCase)
    singleOf(::OpenAccountUseCase)
    singleOf(::AccountOpeningUseCases)

    /** 서명 그림은 남기지 않고 지문만 남긴다. */
    single<SignatureRepository> { LocalSignatureRepositoryImpl(get()) }
    singleOf(::SaveRiskAssessmentUseCase)
    singleOf(::RiskAssessmentUseCases)

    single<AppLockRepository> { LocalAppLockRepositoryImpl(get(), get()) }
    single<LocalDataWiper> { LocalDataWiperImpl(get(), get()) }
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

    single<LoginRepository> { LocalLoginRepositoryImpl(get(), get()) }
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
