package com.eunilsung.ods.ui.orderauth

import com.eunilsung.ods.data.local.repository.LocalOrderAuthRepositoryImpl
import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.model.PinCheckResult
import com.eunilsung.ods.domain.repository.BiometricAuthenticator
import com.eunilsung.ods.domain.usecase.AuthenticateBiometricUseCase
import com.eunilsung.ods.domain.usecase.GetOrderAuthLockUseCase
import com.eunilsung.ods.domain.usecase.IsBiometricAvailableUseCase
import com.eunilsung.ods.domain.usecase.ObserveOrderAuthConfigUseCase
import com.eunilsung.ods.domain.usecase.OrderAuthUseCases
import com.eunilsung.ods.domain.usecase.RegisterOrderPinUseCase
import com.eunilsung.ods.domain.usecase.SetOrderBiometricEnabledUseCase
import com.eunilsung.ods.domain.usecase.VerifyOrderPinUseCase
import com.eunilsung.ods.domain.util.OrderPinPolicy
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OrderAuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val prompt = BiometricPromptText("t", "s", "n")

    private class FakeBiometric(var available: Boolean, var result: BiometricResult) : BiometricAuthenticator {
        var calls = 0
        override fun isAvailable() = available
        override suspend fun authenticate(title: String, subtitle: String, negativeText: String): BiometricResult {
            calls++
            return result
        }
    }

    private fun viewModel(repository: LocalOrderAuthRepositoryImpl, biometric: FakeBiometric) = OrderAuthViewModel(
        OrderAuthUseCases(
            observeConfig = ObserveOrderAuthConfigUseCase(repository),
            getLock = GetOrderAuthLockUseCase(repository),
            registerPin = RegisterOrderPinUseCase(repository),
            verifyPin = VerifyOrderPinUseCase(repository),
            setBiometricEnabled = SetOrderBiometricEnabledUseCase(repository),
            isBiometricAvailable = IsBiometricAvailableUseCase(biometric),
            authenticateBiometric = AuthenticateBiometricUseCase(biometric),
        )
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstOrderRegistersPinTwiceThenAuthorizes() = runTest(dispatcher) {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings())
        val vm = viewModel(repository, FakeBiometric(available = true, result = BiometricResult.SUCCESS))
        vm.onAction(OrderAuthActions.OnStart(prompt, OrderAuthPurpose.AUTHORIZE))
        assertIs<OrderAuthStep.RegisterPin>(vm.uiState.value.step)

        vm.onAction(OrderAuthActions.OnPinEntered("1234"))
        assertEquals(OrderAuthStep.RegisterPin(PinNotice.Weak), vm.uiState.value.step)

        vm.onAction(OrderAuthActions.OnPinEntered("2580"))
        assertEquals(OrderAuthStep.ConfirmPin, vm.uiState.value.step)
        vm.onAction(OrderAuthActions.OnPinEntered("2580"))
        assertEquals(OrderAuthEvent.Authorized, vm.events.first())
        assertTrue(repository.config.value.isPinSet)
        assertTrue(repository.config.value.biometricEnabled)
    }

    @Test
    fun mismatchedConfirmationStartsOver() = runTest(dispatcher) {
        val vm = viewModel(LocalOrderAuthRepositoryImpl(MapSettings()), FakeBiometric(false, BiometricResult.UNAVAILABLE))
        vm.onAction(OrderAuthActions.OnStart(prompt, OrderAuthPurpose.AUTHORIZE))
        vm.onAction(OrderAuthActions.OnPinEntered("2580"))
        vm.onAction(OrderAuthActions.OnPinEntered("1357"))
        assertEquals(OrderAuthStep.RegisterPin(PinNotice.Mismatch), vm.uiState.value.step)
    }

    @Test
    fun biometricSuccessAuthorizesWithoutPin() = runTest(dispatcher) {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings()).apply { setPin("2580"); setBiometricEnabled(true) }
        val biometric = FakeBiometric(available = true, result = BiometricResult.SUCCESS)
        val vm = viewModel(repository, biometric)
        vm.onAction(OrderAuthActions.OnStart(prompt, OrderAuthPurpose.AUTHORIZE))
        advanceUntilIdle()
        assertEquals(OrderAuthEvent.Authorized, vm.events.first())
        assertEquals(1, biometric.calls)
    }

    @Test
    fun biometricCancelFallsBackToPin() = runTest(dispatcher) {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings()).apply { setPin("2580"); setBiometricEnabled(true) }
        val vm = viewModel(repository, FakeBiometric(available = true, result = BiometricResult.CANCELED))
        vm.onAction(OrderAuthActions.OnStart(prompt, OrderAuthPurpose.AUTHORIZE))
        advanceUntilIdle()
        assertEquals(OrderAuthStep.EnterPin(null), vm.uiState.value.step)

        vm.onAction(OrderAuthActions.OnPinEntered("2580"))
        assertEquals(OrderAuthEvent.Authorized, vm.events.first())
    }

    @Test
    fun biometricOffGoesStraightToPin() = runTest(dispatcher) {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings()).apply { setPin("2580") }
        val biometric = FakeBiometric(available = true, result = BiometricResult.SUCCESS)
        val vm = viewModel(repository, biometric)
        vm.onAction(OrderAuthActions.OnStart(prompt, OrderAuthPurpose.AUTHORIZE))
        advanceUntilIdle()
        assertEquals(OrderAuthStep.EnterPin(null), vm.uiState.value.step)
        assertEquals(0, biometric.calls)
    }

    @Test
    fun repeatedWrongPinsLockAndStayLocked() = runTest(dispatcher) {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings()).apply { setPin("2580") }
        val vm = viewModel(repository, FakeBiometric(false, BiometricResult.UNAVAILABLE))
        vm.onAction(OrderAuthActions.OnStart(prompt, OrderAuthPurpose.AUTHORIZE))
        vm.onAction(OrderAuthActions.OnPinEntered("0000"))
        assertEquals(OrderAuthStep.EnterPin(PinNotice.Wrong(OrderPinPolicy.MAX_ATTEMPTS - 1)), vm.uiState.value.step)
        repeat(OrderPinPolicy.MAX_ATTEMPTS - 1) { vm.onAction(OrderAuthActions.OnPinEntered("0000")) }
        assertIs<OrderAuthStep.Locked>(vm.uiState.value.step)

        vm.onAction(OrderAuthActions.OnStart(prompt, OrderAuthPurpose.AUTHORIZE))
        assertIs<OrderAuthStep.Locked>(vm.uiState.value.step)
    }

    @Test
    fun changePinNeedsCurrentThenNewTwice() = runTest(dispatcher) {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings()).apply { setPin("2580"); setBiometricEnabled(true) }
        val biometric = FakeBiometric(available = true, result = BiometricResult.SUCCESS)
        val vm = viewModel(repository, biometric)
        vm.onAction(OrderAuthActions.OnStart(prompt, OrderAuthPurpose.CHANGE_PIN))
        advanceUntilIdle()
        assertEquals(OrderAuthStep.EnterPin(null), vm.uiState.value.step)
        assertEquals(0, biometric.calls)

        vm.onAction(OrderAuthActions.OnPinEntered("1357"))
        assertEquals(OrderAuthStep.EnterPin(PinNotice.Wrong(OrderPinPolicy.MAX_ATTEMPTS - 1)), vm.uiState.value.step)

        vm.onAction(OrderAuthActions.OnPinEntered("2580"))
        assertEquals(OrderAuthStep.RegisterPin(null), vm.uiState.value.step)
        assertTrue(vm.uiState.value.isSettingNewPin)
        assertEquals(false, vm.uiState.value.offersBiometricOnRegister)

        vm.onAction(OrderAuthActions.OnPinEntered("2580"))
        assertEquals(OrderAuthStep.RegisterPin(PinNotice.SameAsCurrent), vm.uiState.value.step)

        vm.onAction(OrderAuthActions.OnPinEntered("1357"))
        vm.onAction(OrderAuthActions.OnPinEntered("1357"))
        assertEquals(OrderAuthEvent.Authorized, vm.events.first())
        assertEquals(PinCheckResult.Matched, repository.verifyPin("1357", 0))
        assertTrue(repository.config.value.biometricEnabled)
    }

    @Test
    fun verifyPurposeSkipsBiometric() = runTest(dispatcher) {
        val repository = LocalOrderAuthRepositoryImpl(MapSettings()).apply { setPin("2580"); setBiometricEnabled(true) }
        val biometric = FakeBiometric(available = true, result = BiometricResult.SUCCESS)
        val vm = viewModel(repository, biometric)
        vm.onAction(OrderAuthActions.OnStart(prompt, OrderAuthPurpose.VERIFY_PIN))
        advanceUntilIdle()
        assertEquals(OrderAuthStep.EnterPin(null), vm.uiState.value.step)
        assertEquals(0, biometric.calls)
    }
}
