package com.eunilsung.ods.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.model.LogoutReason
import com.eunilsung.ods.domain.usecase.LoginUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 로그인 화면 — 사번·비밀번호 입력, 사번 저장, 결과(틀림·잠김·닿지 못함).
 *
 * 잠금 비밀번호를 너무 많이 틀려 데이터를 지우고 로그아웃됐다면 그 사실을 알린다.
 *
 * 로그인되면 화면을 덮던 로그인 창이 사라진다(메인이 직원 상태를 보고 가린다). 로그아웃되면 비밀번호를 비우고 처음 상태로 돌린다.
 */
class LoginViewModel(
    private val useCases: LoginUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            useCases.observeStaff()
                .drop(1)
                .filter { it == null }
                .collect { _uiState.value = initialState() }
        }
    }

    fun onAction(action: LoginActions) {
        when (action) {
            is LoginActions.OnIdChange -> _uiState.update { it.copy(id = action.id.trim(), error = null) }
            is LoginActions.OnPasswordChange -> _uiState.update { it.copy(password = action.password, error = null) }
            LoginActions.OnTogglePasswordVisible -> _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is LoginActions.OnSaveIdChange -> {
                useCases.setSaveId(action.save)
                _uiState.update { it.copy(saveId = action.save) }
            }
            LoginActions.OnLoginClick -> login()
        }
    }

    private fun login() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = useCases.login(LoginRequest(state.id, state.password))
            _uiState.update {
                when (result) {
                    is LoginResult.Success -> it.copy(isLoading = false, password = "")
                    is LoginResult.Wrong -> it.copy(isLoading = false, password = "", error = LoginError.Wrong(result.remainingAttempts))
                    is LoginResult.Locked -> it.copy(isLoading = false, password = "", error = LoginError.Locked(result.untilEpochSeconds))
                    LoginResult.Failed -> it.copy(isLoading = false, error = LoginError.Failed)
                }
            }
        }
    }

    private fun initialState() = LoginUiState(
        id = useCases.getSavedId().orEmpty(),
        saveId = useCases.isSaveId(),
        dataWiped = useCases.getLogoutReason() == LogoutReason.WIPED,
    )
}
