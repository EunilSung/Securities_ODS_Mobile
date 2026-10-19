package com.eunilsung.ods.ui.setting

import com.eunilsung.ods.data.local.repository.LocalAppLockRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalUserPreferenceRepositoryImpl
import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.repository.AppVersionProvider
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import com.eunilsung.ods.domain.usecase.AuthenticateBiometricUseCase
import com.eunilsung.ods.domain.usecase.IsBiometricAvailableUseCase
import com.eunilsung.ods.domain.usecase.LockAppUseCase
import com.eunilsung.ods.domain.usecase.ObserveAppLockUseCase
import com.eunilsung.ods.domain.usecase.SetAppBiometricEnabledUseCase
import com.eunilsung.ods.ui.applock.BiometricPromptText
import com.eunilsung.ods.domain.usecase.GetAppInfoUseCase
import com.eunilsung.ods.domain.usecase.LogoutUseCase
import com.eunilsung.ods.domain.usecase.ObserveListDensityUseCase
import com.eunilsung.ods.domain.usecase.ObserveMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.ObserveStaffUseCase
import com.eunilsung.ods.domain.usecase.SetListDensityUseCase
import com.eunilsung.ods.domain.usecase.SetMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.SettingUseCases
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

    private object FakeVersion : AppVersionProvider {
        override fun versionCode() = 7L
        override fun versionName() = "1.2.3"
    }

    private class FakeBiometric(val available: Boolean, val result: BiometricResult = BiometricResult.SUCCESS) : BiometricAuthenticator {
        override fun isAvailable() = available
        override suspend fun authenticate(title: String, subtitle: String, negativeText: String) = result
    }

    private val preferences = LocalUserPreferenceRepositoryImpl(MapSettings())
    private val login = LocalLoginRepositoryImpl(MapSettings(), simulatedRoundTripMillis = 0)
    private val appLock = LocalAppLockRepositoryImpl(MapSettings())
    private val prompt = BiometricPromptText("t", "s", "n")

    private fun viewModel(biometric: BiometricAuthenticator = FakeBiometric(true)) = SettingViewModel(
        SettingUseCases(
            observeListDensity = ObserveListDensityUseCase(preferences),
            setListDensity = SetListDensityUseCase(preferences),
            observeMaskPersonalInfo = ObserveMaskPersonalInfoUseCase(preferences),
            setMaskPersonalInfo = SetMaskPersonalInfoUseCase(preferences),
            getAppInfo = GetAppInfoUseCase(FakeVersion),
            observeStaff = ObserveStaffUseCase(login),
            logout = LogoutUseCase(login, appLock),
            observeAppLock = ObserveAppLockUseCase(appLock),
            setAppBiometricEnabled = SetAppBiometricEnabledUseCase(appLock),
            isBiometricAvailable = IsBiometricAvailableUseCase(biometric),
            authenticateBiometric = AuthenticateBiometricUseCase(biometric),
            lockApp = LockAppUseCase(appLock),
        )
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun personalInfoIsMaskedByDefault() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.maskPersonalInfo)
    }

    @Test
    fun displaySettingsAndAppInfoFlowThrough() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(SettingActions.OnListDensityChange(ListDensity.COMPACT))
        vm.onAction(SettingActions.OnMaskPersonalInfoChange(false))
        advanceUntilIdle()
        val state = vm.uiState.first()
        assertEquals(ListDensity.COMPACT, state.listDensity)
        assertFalse(state.maskPersonalInfo)
        assertEquals("1.2.3", state.appInfo.versionName)
        assertTrue(state.appInfo.usesDemoData)
    }

    @Test
    fun enablingBiometricNeedsOneSuccessfulScan() = runTest(dispatcher) {
        appLock.startSession("test1", 0)
        appLock.setPin("135790")
        val vm = viewModel(FakeBiometric(available = true, result = BiometricResult.FAILED))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(SettingActions.OnBiometricToggle(true, prompt))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.biometricEnabled)
        assertEquals(SettingNotice.BIOMETRIC_FAILED, vm.uiState.value.notice)

        val ok = viewModel()
        backgroundScope.launch { ok.uiState.collect {} }
        ok.onAction(SettingActions.OnBiometricToggle(true, prompt))
        advanceUntilIdle()
        assertTrue(ok.uiState.value.biometricEnabled)
        ok.onAction(SettingActions.OnBiometricToggle(false, prompt))
        advanceUntilIdle()
        assertFalse(ok.uiState.value.biometricEnabled)
    }

    @Test
    fun lockNowLocksTheApp() = runTest(dispatcher) {
        appLock.startSession("test1", 0)
        appLock.setPin("135790")
        val vm = viewModel()
        vm.onAction(SettingActions.OnLockNowClick)
        assertTrue(appLock.state.value.isLocked)
    }

    @Test
    fun logoutClearsStaff() = runTest(dispatcher) {
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals("test1", vm.uiState.value.staff?.id)
        vm.onAction(SettingActions.OnLogoutClick)
        advanceUntilIdle()
        assertNull(vm.uiState.value.staff)
    }
}
