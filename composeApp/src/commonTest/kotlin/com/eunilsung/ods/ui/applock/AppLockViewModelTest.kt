package com.eunilsung.ods.ui.applock

import com.eunilsung.ods.data.local.repository.LocalAppLockRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import com.eunilsung.ods.domain.repository.LocalDataWiper
import com.eunilsung.ods.domain.usecase.AppLockUseCases
import com.eunilsung.ods.domain.usecase.IsBiometricAvailableUseCase
import com.eunilsung.ods.domain.usecase.LogoutUseCase
import com.eunilsung.ods.domain.usecase.ObserveAppLockUseCase
import com.eunilsung.ods.domain.usecase.ObserveStaffUseCase
import com.eunilsung.ods.domain.usecase.RegisterAppPinUseCase
import com.eunilsung.ods.domain.usecase.SetAppBiometricEnabledUseCase
import com.eunilsung.ods.domain.usecase.UnlockWithBiometricUseCase
import com.eunilsung.ods.domain.usecase.UnlockWithPinUseCase
import com.eunilsung.ods.domain.util.AppLockPolicy
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class AppLockViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeBiometric(val available: Boolean, val result: BiometricResult) : BiometricAuthenticator {
        override fun isAvailable() = available
        override suspend fun authenticate(title: String, subtitle: String, negativeText: String) = result
    }

    private val settings = MapSettings()
    private val appLock = LocalAppLockRepositoryImpl(settings)
    private val login = LocalLoginRepositoryImpl(settings, simulatedRoundTripMillis = 0)
    private val prompt = BiometricPromptText("t", "s", "n")
    private val wiper = object : LocalDataWiper {
        override suspend fun wipe() = Unit
    }

    private fun viewModel(biometric: BiometricAuthenticator = FakeBiometric(false, BiometricResult.UNAVAILABLE)) = AppLockViewModel(
        AppLockUseCases(
            observeAppLock = ObserveAppLockUseCase(appLock),
            observeStaff = ObserveStaffUseCase(login),
            registerPin = RegisterAppPinUseCase(appLock),
            unlockWithPin = UnlockWithPinUseCase(appLock, wiper, login),
            unlockWithBiometric = UnlockWithBiometricUseCase(appLock, biometric),
            setBiometricEnabled = SetAppBiometricEnabledUseCase(appLock),
            isBiometricAvailable = IsBiometricAvailableUseCase(biometric),
            logout = LogoutUseCase(login, appLock),
        )
    )

    private fun AppLockViewModel.enter(pin: String) = onAction(AppLockActions.OnPinEntered(pin))

    @BeforeTest
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        appLock.startSession("test1", 0)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun registerRejectsWeakAndMismatchedPins() = runTest(dispatcher) {
        val vm = viewModel(FakeBiometric(true, BiometricResult.SUCCESS))
        vm.onAction(AppLockActions.OnStart(AppLockPurpose.REGISTER, prompt))
        assertEquals("김민준", vm.uiState.value.staffName)

        vm.enter("123456")
        assertEquals(AppLockStep.NewPin(PinNotice.Weak), vm.uiState.value.step)

        vm.enter("135790")
        assertEquals(AppLockStep.ConfirmPin, vm.uiState.value.step)
        vm.enter("135791")
        assertEquals(AppLockStep.NewPin(PinNotice.Mismatch), vm.uiState.value.step)

        vm.enter("135790")
        vm.enter("135790")
        assertTrue(appLock.state.value.isPinSet)
        assertTrue(appLock.state.value.biometricEnabled)
    }

    @Test
    fun unlockCountsDownAndThenOpens() = runTest(dispatcher) {
        appLock.setPin("135790")
        appLock.lock()
        val vm = viewModel()
        vm.onAction(AppLockActions.OnStart(AppLockPurpose.UNLOCK, prompt))
        assertEquals(AppLockStep.EnterPin(null), vm.uiState.value.step)

        vm.enter("000000")
        advanceUntilIdle()
        assertEquals(AppLockStep.EnterPin(PinNotice.Wrong(AppLockPolicy.MAX_ATTEMPTS - 1)), vm.uiState.value.step)

        vm.enter("135790")
        advanceUntilIdle()
        assertFalse(appLock.state.value.isLocked)
    }

    @Test
    fun unlockStartsWithBiometricWhenEnabled() = runTest(dispatcher) {
        appLock.setPin("135790")
        appLock.setBiometricEnabled(true)
        appLock.lock()
        val vm = viewModel(FakeBiometric(true, BiometricResult.SUCCESS))
        vm.onAction(AppLockActions.OnStart(AppLockPurpose.UNLOCK, prompt))
        advanceUntilIdle()
        assertFalse(appLock.state.value.isLocked)
    }

    @Test
    fun canceledBiometricFallsBackToPin() = runTest(dispatcher) {
        appLock.setPin("135790")
        appLock.setBiometricEnabled(true)
        appLock.lock()
        val vm = viewModel(FakeBiometric(true, BiometricResult.CANCELED))
        vm.onAction(AppLockActions.OnStart(AppLockPurpose.UNLOCK, prompt))
        advanceUntilIdle()
        assertTrue(appLock.state.value.isLocked)
        assertEquals(AppLockStep.EnterPin(null), vm.uiState.value.step)
        assertTrue(vm.uiState.value.showsBiometricButton)
    }

    @Test
    fun changeNeedsCurrentPinAndADifferentNewOne() = runTest(dispatcher) {
        appLock.setPin("135790")
        val vm = viewModel()
        vm.onAction(AppLockActions.OnStart(AppLockPurpose.CHANGE, prompt))
        vm.enter("135790")
        advanceUntilIdle()
        assertEquals(AppLockStep.NewPin(null), vm.uiState.value.step)

        vm.enter("135790")
        assertEquals(AppLockStep.NewPin(PinNotice.SameAsCurrent), vm.uiState.value.step)

        vm.enter("246801")
        vm.enter("246801")
        assertEquals(AppLockEvent.PinChanged, vm.events.first())

        appLock.lock()
        val unlock = viewModel()
        unlock.onAction(AppLockActions.OnStart(AppLockPurpose.UNLOCK, prompt))
        unlock.enter("246801")
        advanceUntilIdle()
        assertFalse(appLock.state.value.isLocked)
    }

    @Test
    fun logoutFromLockScreenEndsSession() = runTest(dispatcher) {
        appLock.setPin("135790")
        appLock.lock()
        val vm = viewModel()
        vm.onAction(AppLockActions.OnLogoutClick)
        assertNull(login.currentStaff.value)
        assertFalse(appLock.state.value.isLocked)
    }
}
