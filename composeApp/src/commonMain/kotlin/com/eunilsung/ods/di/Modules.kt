package com.eunilsung.ods.di

import com.eunilsung.ods.data.local.DatabaseDriverFactory
import com.eunilsung.ods.data.local.repository.LocalAccountRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalOrderAuthRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalOrderRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalQuoteRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalStockRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalTickSimulator
import com.eunilsung.ods.data.local.repository.LocalUserPreferenceRepositoryImpl
import com.eunilsung.ods.data.mapper.HoldingMapper
import com.eunilsung.ods.data.mapper.OrderMapper
import com.eunilsung.ods.data.mapper.StockMapper
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.repository.AccountRepository
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.repository.OrderAuthRepository
import com.eunilsung.ods.domain.repository.OrderRepository
import com.eunilsung.ods.domain.repository.QuoteRepository
import com.eunilsung.ods.domain.repository.StockRepository
import com.eunilsung.ods.domain.repository.UserPreferenceRepository
import com.eunilsung.ods.domain.usecase.AmendOrderUseCase
import com.eunilsung.ods.domain.usecase.AuthenticateBiometricUseCase
import com.eunilsung.ods.domain.usecase.BalanceUseCases
import com.eunilsung.ods.domain.usecase.CancelOrderUseCase
import com.eunilsung.ods.domain.usecase.FetchWatchlistUseCase
import com.eunilsung.ods.domain.usecase.GetAppInfoUseCase
import com.eunilsung.ods.domain.usecase.GetLastQuoteCodeUseCase
import com.eunilsung.ods.domain.usecase.GetOrderAuthLockUseCase
import com.eunilsung.ods.domain.usecase.GetSavedIdUseCase
import com.eunilsung.ods.domain.usecase.IsSaveIdUseCase
import com.eunilsung.ods.domain.usecase.LoginUseCase
import com.eunilsung.ods.domain.usecase.LoginUseCases
import com.eunilsung.ods.domain.usecase.LogoutUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomerUseCase
import com.eunilsung.ods.domain.usecase.IsBiometricAvailableUseCase
import com.eunilsung.ods.domain.usecase.ObserveAccountUseCase
import com.eunilsung.ods.domain.usecase.ObserveCandlesUseCase
import com.eunilsung.ods.domain.usecase.ObserveHideAmountsUseCase
import com.eunilsung.ods.domain.usecase.ObserveHoldingsUseCase
import com.eunilsung.ods.domain.usecase.ObserveListDensityUseCase
import com.eunilsung.ods.domain.usecase.ObserveOpenOrdersUseCase
import com.eunilsung.ods.domain.usecase.ObserveOrderAuthConfigUseCase
import com.eunilsung.ods.domain.usecase.ObserveOrderBookUseCase
import com.eunilsung.ods.domain.usecase.OpenOrderUseCases
import com.eunilsung.ods.domain.usecase.ObserveSellableQuantityUseCase
import com.eunilsung.ods.domain.usecase.ObserveStockUseCase
import com.eunilsung.ods.domain.usecase.ObserveWatchlistFetchingUseCase
import com.eunilsung.ods.domain.usecase.ObserveWatchlistUseCase
import com.eunilsung.ods.domain.usecase.QuoteUseCases
import com.eunilsung.ods.domain.usecase.SetHideAmountsUseCase
import com.eunilsung.ods.domain.usecase.SetLastQuoteCodeUseCase
import com.eunilsung.ods.domain.usecase.SetOrderBiometricEnabledUseCase
import com.eunilsung.ods.domain.usecase.SetSaveIdUseCase
import com.eunilsung.ods.domain.usecase.SettingUseCases
import com.eunilsung.ods.domain.usecase.SetListDensityUseCase
import com.eunilsung.ods.domain.usecase.OrderAuthUseCases
import com.eunilsung.ods.domain.usecase.OrderUseCases
import com.eunilsung.ods.domain.usecase.RegisterOrderPinUseCase
import com.eunilsung.ods.domain.usecase.StockUseCases
import com.eunilsung.ods.domain.usecase.SubmitOrderUseCase
import com.eunilsung.ods.domain.usecase.VerifyOrderPinUseCase
import com.eunilsung.ods.ui.balance.BalanceViewModel
import com.eunilsung.ods.ui.login.LoginViewModel
import com.eunilsung.ods.ui.main.MainViewModel
import com.eunilsung.ods.ui.openorder.OpenOrdersViewModel
import com.eunilsung.ods.ui.order.OrderViewModel
import com.eunilsung.ods.ui.orderauth.OrderAuthViewModel
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
    singleOf(::GetLastQuoteCodeUseCase)
    singleOf(::SetLastQuoteCodeUseCase)

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

    singleOf(::OrderMapper)
    /** 서버가 붙으면 주문·계좌도 `Config.Server.IS_ENABLED` 로 서버 구현과 갈라 끼운다. */
    single<OrderRepository> { LocalOrderRepositoryImpl(get(), get()) }
    singleOf(::HoldingMapper)
    single<AccountRepository> { LocalAccountRepositoryImpl(get(), get()) }
    singleOf(::SubmitOrderUseCase)
    singleOf(::ObserveAccountUseCase)
    singleOf(::ObserveSellableQuantityUseCase)
    singleOf(::OrderUseCases)
    singleOf(::ObserveOpenOrdersUseCase)
    singleOf(::AmendOrderUseCase)
    singleOf(::CancelOrderUseCase)
    singleOf(::OpenOrderUseCases)
    singleOf(::ObserveHoldingsUseCase)
    singleOf(::ObserveHideAmountsUseCase)
    singleOf(::SetHideAmountsUseCase)
    singleOf(::BalanceUseCases)

    /** 서버가 붙으면 비밀번호 확인은 서버 구현으로 바꾸고, 생체 인증 사용 여부만 기기에 남긴다. */
    single<OrderAuthRepository> { LocalOrderAuthRepositoryImpl(get()) }
    singleOf(::ObserveOrderAuthConfigUseCase)
    singleOf(::GetOrderAuthLockUseCase)
    singleOf(::RegisterOrderPinUseCase)
    singleOf(::VerifyOrderPinUseCase)
    singleOf(::SetOrderBiometricEnabledUseCase)
    singleOf(::IsBiometricAvailableUseCase)
    singleOf(::AuthenticateBiometricUseCase)
    singleOf(::OrderAuthUseCases)

    singleOf(::GetAppInfoUseCase)
    singleOf(::SettingUseCases)

    /** 서버가 붙으면 `Config.Server.IS_ENABLED` 로 서버 로그인과 갈라 끼운다. */
    single<LoginRepository> { LocalLoginRepositoryImpl(get()) }
    singleOf(::LoginUseCase)
    singleOf(::LogoutUseCase)
    singleOf(::ObserveCustomerUseCase)
    singleOf(::GetSavedIdUseCase)
    singleOf(::IsSaveIdUseCase)
    singleOf(::SetSaveIdUseCase)
    singleOf(::LoginUseCases)

    viewModelOf(::MainViewModel)
    viewModelOf(::WatchlistViewModel)
    viewModelOf(::QuoteViewModel)
    viewModelOf(::OrderViewModel)
    viewModelOf(::OpenOrdersViewModel)
    viewModelOf(::BalanceViewModel)
    viewModelOf(::OrderAuthViewModel)
    viewModelOf(::SettingViewModel)
    viewModelOf(::LoginViewModel)
}
