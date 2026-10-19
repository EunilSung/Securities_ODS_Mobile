package com.eunilsung.ods.ui.applock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.model.UnlockResult
import com.eunilsung.ods.domain.usecase.AppLockUseCases
import com.eunilsung.ods.domain.util.AppLockPolicy
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
 * 잠금 화면 — 앱 잠금 비밀번호 등록·풀기·변경.
 *
 * - 풀기: 생체 인증을 켜 두었고 쓸 수 있으면 생체부터, 아니면 비밀번호. 생체가 안 되면 비밀번호로 넘어간다.
 *   [AppLockPolicy.MAX_ATTEMPTS] 번 틀리면 유스케이스가 업무 데이터를 지우고 로그아웃한다 — 그러면 로그인 화면이 덮는다.
 * - 등록: 새 비밀번호를 두 번 받는다. 약한 비밀번호는 받지 않는다.
 * - 변경: 지금 비밀번호를 맞힌 뒤 등록과 같다. 지금과 같은 비밀번호는 받지 않는다. 끝나면 [AppLockEvent.PinChanged].
 *
 * 풀기·등록이 끝나면 잠금 상태가 바뀌어 메인이 이 화면을 걷는다. 이 화면이 따로 알릴 것은 없다.
 */
class AppLockViewModel(
    private val useCases: AppLockUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    private val _events = Channel<AppLockEvent>(Channel.BUFFERED)
    val events: Flow<AppLockEvent> = _events.receiveAsFlow()

    private var firstPin: String? = null
    private var currentPin: String? = null
    private var biometricJob: Job? = null

    fun onAction(action: AppLockActions) {
        when (action) {
            is AppLockActions.OnStart -> start(action.purpose, action.prompt)
            is AppLockActions.OnPinEntered -> pinEntered(action.pin)
            is AppLockActions.OnBiometricClick -> runBiometric(action.prompt)
            is AppLockActions.OnUseBiometricChange -> _uiState.update { it.copy(useBiometricOnRegister = action.use) }
            AppLockActions.OnLogoutClick -> useCases.logout()
        }
    }

    private fun start(purpose: AppLockPurpose, prompt: BiometricPromptText) {
        biometricJob?.cancel()
        firstPin = null
        currentPin = null
        val lock = useCases.observeAppLock().value
        val available = useCases.isBiometricAvailable()
        _uiState.value = AppLockUiState(
            purpose = purpose,
            staffName = useCases.observeStaff().value?.name.orEmpty(),
            biometricAvailable = available,
            biometricEnabled = lock.biometricEnabled,
            useBiometricOnRegister = available,
        )
        when {
            purpose == AppLockPurpose.REGISTER -> setStep(AppLockStep.NewPin(null))
            purpose == AppLockPurpose.UNLOCK && lock.biometricEnabled && available -> runBiometric(prompt)
            else -> setStep(AppLockStep.EnterPin(null))
        }
    }

    private fun runBiometric(prompt: BiometricPromptText) {
        biometricJob?.cancel()
        setStep(AppLockStep.Biometric)
        biometricJob = viewModelScope.launch {
            when (useCases.unlockWithBiometric(prompt.title, prompt.subtitle, prompt.negative)) {
                BiometricResult.SUCCESS -> setStep(AppLockStep.Loading)
                BiometricResult.CANCELED -> setStep(AppLockStep.EnterPin(null))
                BiometricResult.FAILED, BiometricResult.UNAVAILABLE -> setStep(AppLockStep.EnterPin(PinNotice.BiometricFailed))
            }
        }
    }

    private fun pinEntered(pin: String) {
        when (_uiState.value.step) {
            is AppLockStep.EnterPin -> viewModelScope.launch { checkCurrent(pin) }
            is AppLockStep.NewPin -> when {
                AppLockPolicy.isWeak(pin) -> fail(AppLockStep.NewPin(PinNotice.Weak))
                pin == currentPin -> fail(AppLockStep.NewPin(PinNotice.SameAsCurrent))
                else -> {
                    firstPin = pin
                    setStep(AppLockStep.ConfirmPin)
                }
            }
            AppLockStep.ConfirmPin ->
                if (pin == firstPin) {
                    register(pin)
                } else {
                    firstPin = null
                    fail(AppLockStep.NewPin(PinNotice.Mismatch))
                }
            else -> Unit
        }
    }

    private suspend fun checkCurrent(pin: String) {
        when (val result = useCases.unlockWithPin(pin)) {
            UnlockResult.Unlocked ->
                if (_uiState.value.purpose == AppLockPurpose.CHANGE) {
                    currentPin = pin
                    setStep(AppLockStep.NewPin(null))
                } else {
                    setStep(AppLockStep.Loading)
                }
            is UnlockResult.Wrong -> fail(AppLockStep.EnterPin(PinNotice.Wrong(result.remainingAttempts)))
            UnlockResult.Wiped -> setStep(AppLockStep.Loading)
        }
    }

    private fun register(pin: String) {
        val state = _uiState.value
        useCases.registerPin(pin)
        if (state.offersBiometricOnRegister) useCases.setBiometricEnabled(state.useBiometricOnRegister)
        firstPin = null
        currentPin = null
        setStep(AppLockStep.Loading)
        if (state.purpose == AppLockPurpose.CHANGE) viewModelScope.launch { _events.send(AppLockEvent.PinChanged) }
    }

    private fun setStep(step: AppLockStep) = _uiState.update { it.copy(step = step) }

    private fun fail(step: AppLockStep) = _uiState.update { it.copy(step = step, errorKey = it.errorKey + 1) }
}
