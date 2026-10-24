package com.eunilsung.ods.ui.customer.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.domain.usecase.RegisterCustomerUseCase
import com.eunilsung.ods.domain.util.formatPhone
import com.eunilsung.ods.domain.util.parseBirthDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 신규 고객 등록 — 현장에서 처음 만난 고객의 이름·생년월일·성별·휴대폰·주소를 받는다.
 *
 * 등록하면 그 고객과 상담을 시작하고 [NewCustomerEvent.Registered] 로 상세를 연다. 등급은 일반으로 시작한다.
 */
class NewCustomerViewModel(
    private val registerCustomer: RegisterCustomerUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewCustomerUiState())
    val uiState: StateFlow<NewCustomerUiState> = _uiState.asStateFlow()

    private val _events = Channel<NewCustomerEvent>(Channel.BUFFERED)
    val events: Flow<NewCustomerEvent> = _events.receiveAsFlow()

    fun onAction(action: NewCustomerActions) {
        when (action) {
            is NewCustomerActions.OnNameChange -> _uiState.update { it.copy(name = action.name.take(MAX_NAME)) }
            is NewCustomerActions.OnBirthChange -> _uiState.update { it.copy(birthDigits = action.digits.filter(Char::isDigit).take(8)) }
            is NewCustomerActions.OnGenderChange -> _uiState.update { it.copy(gender = action.gender) }
            is NewCustomerActions.OnPhoneChange -> _uiState.update { it.copy(phoneDigits = action.digits.filter(Char::isDigit).take(11)) }
            is NewCustomerActions.OnAddressChange -> _uiState.update { it.copy(address = action.address.take(MAX_ADDRESS)) }
            NewCustomerActions.OnSubmit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isSaving) return
        val birth = parseBirthDate(state.birthDigits)
        val phone = formatPhone(state.phoneDigits)
        if (state.hasError || birth == null || phone == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val customer = registerCustomer(NewCustomer(state.name.trim(), birth, state.gender, phone, state.address.trim()))
            _uiState.value = NewCustomerUiState()
            _events.send(NewCustomerEvent.Registered(customer.id))
        }
    }

    private companion object {
        const val MAX_NAME = 30
        const val MAX_ADDRESS = 100
    }
}
