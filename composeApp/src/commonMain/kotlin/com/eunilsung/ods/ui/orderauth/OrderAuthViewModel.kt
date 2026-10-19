package com.eunilsung.ods.ui.orderauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.model.PinCheckResult
import com.eunilsung.ods.domain.usecase.OrderAuthUseCases
import com.eunilsung.ods.domain.util.OrderPinPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 주문 인증 — 주문·정정·취소를 보내기 직전에 본인임을 확인한다.
 *
 * 시작 단계: 잠겨 있으면 잠금 안내, 비밀번호가 없으면 등록, 주문이고 생체 인증을 켜 두었고 쓸 수 있으면 생체, 아니면 비밀번호.
 * 생체 인증이 안 되면(취소·실패·불가) 비밀번호로 넘어간다. 끝나면 [OrderAuthEvent.Authorized] 를 한 번 보낸다.
 *
 * 비밀번호 변경([OrderAuthPurpose.CHANGE_PIN])은 현재 비밀번호를 맞힌 뒤 새 비밀번호를 두 번 받는다. 지금과 같은 비밀번호는 받지 않는다.
 * 설정을 바꾸기 전 본인 확인([OrderAuthPurpose.VERIFY_PIN])은 생체를 쓰지 않고 비밀번호만 받는다.
 * 주문 화면과 미체결 화면이 같은 인스턴스를 쓰므로 창을 열 때마다 [OrderAuthActions.OnStart] 로 처음부터 시작한다.
 */
class OrderAuthViewModel(
    private val useCases: OrderAuthUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderAuthUiState())
    val uiState: StateFlow<OrderAuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<OrderAuthEvent>(Channel.BUFFERED)
    val events: Flow<OrderAuthEvent> = _events.receiveAsFlow()

    private var firstPin: String? = null
    private var verifiedCurrentPin: String? = null
    private var biometricJob: Job? = null

    fun onAction(action: OrderAuthActions) {
        when (action) {
            is OrderAuthActions.OnStart -> start(action.prompt, action.purpose)
            is OrderAuthActions.OnPinEntered -> pinEntered(action.pin)
            is OrderAuthActions.OnRetryBiometric -> runBiometric(action.prompt)
            is OrderAuthActions.OnUseBiometricChange -> _uiState.update { it.copy(useBiometricOnRegister = action.use) }
            OrderAuthActions.OnCancel -> reset()
        }
    }

    private fun start(prompt: BiometricPromptText, purpose: OrderAuthPurpose) {
        reset()
        val config = useCases.observeConfig().value
        val available = useCases.isBiometricAvailable()
        _uiState.value = OrderAuthUiState(
            purpose = purpose,
            biometricAvailable = available,
            biometricEnabled = config.biometricEnabled,
            useBiometricOnRegister = available,
        )
        val lockedUntil = useCases.getLock()
        when {
            lockedUntil != null -> setStep(OrderAuthStep.Locked(lockedUntil))
            !config.isPinSet -> setStep(OrderAuthStep.RegisterPin(null))
            purpose == OrderAuthPurpose.AUTHORIZE && config.biometricEnabled && available -> runBiometric(prompt)
            else -> setStep(OrderAuthStep.EnterPin(null))
        }
    }

    private fun runBiometric(prompt: BiometricPromptText) {
        biometricJob?.cancel()
        setStep(OrderAuthStep.Biometric)
        biometricJob = viewModelScope.launch {
            when (useCases.authenticateBiometric(prompt.title, prompt.subtitle, prompt.negative)) {
                BiometricResult.SUCCESS -> authorized()
                BiometricResult.CANCELED -> setStep(OrderAuthStep.EnterPin(null))
                BiometricResult.FAILED, BiometricResult.UNAVAILABLE -> setStep(OrderAuthStep.EnterPin(PinNotice.BiometricFailed))
            }
        }
    }

    private fun pinEntered(pin: String) {
        when (_uiState.value.step) {
            is OrderAuthStep.EnterPin -> when (val result = useCases.verifyPin(pin)) {
                PinCheckResult.Matched ->
                    if (_uiState.value.purpose == OrderAuthPurpose.CHANGE_PIN) {
                        verifiedCurrentPin = pin
                        _uiState.update { it.copy(step = OrderAuthStep.RegisterPin(null), isSettingNewPin = true) }
                    } else {
                        authorized()
                    }
                is PinCheckResult.Wrong -> fail(OrderAuthStep.EnterPin(PinNotice.Wrong(result.remainingAttempts)))
                is PinCheckResult.Locked -> setStep(OrderAuthStep.Locked(result.untilEpochSeconds))
            }
            is OrderAuthStep.RegisterPin ->
                if (OrderPinPolicy.isWeak(pin)) {
                    fail(OrderAuthStep.RegisterPin(PinNotice.Weak))
                } else if (pin == verifiedCurrentPin) {
                    fail(OrderAuthStep.RegisterPin(PinNotice.SameAsCurrent))
                } else {
                    firstPin = pin
                    setStep(OrderAuthStep.ConfirmPin)
                }
            OrderAuthStep.ConfirmPin ->
                if (pin == firstPin) {
                    useCases.registerPin(pin)
                    val state = _uiState.value
                    if (state.offersBiometricOnRegister) {
                        useCases.setBiometricEnabled(state.useBiometricOnRegister)
                    }
                    authorized()
                } else {
                    firstPin = null
                    fail(OrderAuthStep.RegisterPin(PinNotice.Mismatch))
                }
            else -> Unit
        }
    }

    private fun setStep(step: OrderAuthStep) = _uiState.update { it.copy(step = step) }

    private fun fail(step: OrderAuthStep) = _uiState.update { it.copy(step = step, errorKey = it.errorKey + 1) }

    private fun authorized() {
        firstPin = null
        verifiedCurrentPin = null
        setStep(OrderAuthStep.Loading)
        viewModelScope.launch { _events.send(OrderAuthEvent.Authorized) }
    }

    private fun reset() {
        biometricJob?.cancel()
        biometricJob = null
        firstPin = null
        verifiedCurrentPin = null
        _uiState.update { it.copy(step = OrderAuthStep.Loading, isSettingNewPin = false) }
    }
}
