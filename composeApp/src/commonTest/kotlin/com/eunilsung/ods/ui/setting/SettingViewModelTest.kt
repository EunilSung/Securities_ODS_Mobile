package com.eunilsung.ods.ui.setting

import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalOrderAuthRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalUserPreferenceRepositoryImpl
import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.domain.repository.AppVersionProvider
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import com.eunilsung.ods.domain.usecase.GetAppInfoUseCase
import com.eunilsung.ods.domain.usecase.GetOrderAuthLockUseCase
import com.eunilsung.ods.domain.usecase.IsBiometricAvailableUseCase
import com.eunilsung.ods.domain.usecase.LogoutUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomerUseCase
import com.eunilsung.ods.domain.usecase.ObserveHideAmountsUseCase
import com.eunilsung.ods.domain.usecase.ObserveListDensityUseCase
import com.eunilsung.ods.domain.usecase.ObserveOrderAuthConfigUseCase
import com.eunilsung.ods.domain.usecase.SetHideAmountsUseCase
import com.eunilsung.ods.domain.usecase.SetListDensityUseCase
import com.eunilsung.ods.domain.usecase.SetOrderBiometricEnabledUseCase
import com.eunilsung.ods.domain.usecase.SettingUseCases
import com.eunilsung.ods.ui.orderauth.OrderAuthPurpose
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeBiometric(val available: Boolean) : BiometricAuthenticator {
        override fun isAvailable() = available
        override suspend fun authenticate(title: String, subtitle: String, negativeText: String) = BiometricResult.SUCCESS
    }

    private object FakeVersion : AppVersionProvider {
        override fun versionCode() = 7L
        override fun versionName() = "1.2.3"
    }

    private val preferences = LocalUserPreferenceRepositoryImpl(MapSettings())
    private val auth = LocalOrderAuthRepositoryImpl(MapSettings())
    private val login = LocalLoginRepositoryImpl(MapSettings(), simulatedRoundTripMillis = 0)

    private fun viewModel(biometricAvailable: Boolean = true) = SettingViewModel(
        SettingUseCases(
            observeListDensity = ObserveListDensityUseCase(preferences),
            setListDensity = SetListDensityUseCase(preferences),
            observeHideAmounts = ObserveHideAmountsUseCase(preferences),
            setHideAmounts = SetHideAmountsUseCase(preferences),
            observeOrderAuthConfig = ObserveOrderAuthConfigUseCase(auth),
            getOrderAuthLock = GetOrderAuthLockUseCase(auth),
            setOrderBiometricEnabled = SetOrderBiometricEnabledUseCase(auth),
            isBiometricAvailable = IsBiometricAvailableUseCase(FakeBiometric(biometricAvailable)),
            getAppInfo = GetAppInfoUseCase(FakeVersion),
            observeCustomer = ObserveCustomerUseCase(login),
            logout = LogoutUseCase(login),
        )
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun enablingBiometricWaitsForPinVerification() = runTest(dispatcher) {
        auth.setPin("2580")
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }

        vm.onAction(SettingActions.OnBiometricToggle(true))
        advanceUntilIdle()
        assertEquals(OrderAuthPurpose.VERIFY_PIN, vm.uiState.value.authPurpose)
        assertFalse(auth.config.value.biometricEnabled)

        vm.onAction(SettingActions.OnAuthDone)
        advanceUntilIdle()
        assertTrue(auth.config.value.biometricEnabled)
        assertNull(vm.uiState.value.authPurpose)
        assertEquals(SettingNotice.BIOMETRIC_ENABLED, vm.uiState.value.notice)
    }

    @Test
    fun cancelingVerificationLeavesBiometricOff() = runTest(dispatcher) {
        auth.setPin("2580")
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(SettingActions.OnBiometricToggle(true))
        vm.onAction(SettingActions.OnAuthCanceled)
        advanceUntilIdle()
        assertFalse(auth.config.value.biometricEnabled)
        assertNull(vm.uiState.value.authPurpose)
    }

    @Test
    fun disablingBiometricNeedsNoVerification() = runTest(dispatcher) {
        auth.setPin("2580")
        auth.setBiometricEnabled(true)
        val vm = viewModel()
        vm.onAction(SettingActions.OnBiometricToggle(false))
        assertFalse(auth.config.value.biometricEnabled)
    }

    @Test
    fun biometricCannotBeEnabledWithoutHardware() = runTest(dispatcher) {
        auth.setPin("2580")
        val vm = viewModel(biometricAvailable = false)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(SettingActions.OnBiometricToggle(true))
        advanceUntilIdle()
        assertNull(vm.uiState.value.authPurpose)
    }

    @Test
    fun changePinNoticeDependsOnWhetherPinExisted() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.onAction(SettingActions.OnChangePinClick)
        auth.setPin("2580")
        vm.onAction(SettingActions.OnAuthDone)
        advanceUntilIdle()
        assertEquals(SettingNotice.PIN_REGISTERED, vm.uiState.value.notice)

        vm.onAction(SettingActions.OnChangePinClick)
        advanceUntilIdle()
        vm.onAction(SettingActions.OnAuthDone)
        advanceUntilIdle()
        assertEquals(SettingNotice.PIN_CHANGED, vm.uiState.value.notice)
    }

    @Test
    fun displaySettingsAndAppInfoFlowThrough() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(SettingActions.OnListDensityChange(ListDensity.COMPACT))
        vm.onAction(SettingActions.OnHideAmountsChange(true))
        advanceUntilIdle()
        val state = vm.uiState.first()
        assertEquals(ListDensity.COMPACT, state.listDensity)
        assertTrue(state.hideAmounts)
        assertEquals("1.2.3", state.appInfo.versionName)
        assertTrue(state.appInfo.usesDemoData)
    }
}
