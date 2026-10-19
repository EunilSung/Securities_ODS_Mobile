package com.eunilsung.ods.ui.risk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.RiskQuestion
import com.eunilsung.ods.domain.usecase.RiskAssessmentUseCases
import com.eunilsung.ods.domain.usecase.SaveRiskResult
import com.eunilsung.ods.domain.util.RiskAssessmentPolicy
import com.eunilsung.ods.domain.util.ageOn
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 투자성향 진단 — 일곱 문항을 하나씩 묻고, 결과를 보여 주고, 고객 확인과 서명을 받아 저장한다.
 *
 * 나이 문항은 고객 생년월일로 미리 고른다(고객이 바꿀 수 있다). 마지막 문항 다음에 점수로 성향을 정한다.
 */
class RiskAssessmentViewModel(
    private val useCases: RiskAssessmentUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RiskAssessmentUiState())
    val uiState: StateFlow<RiskAssessmentUiState> = _uiState.asStateFlow()

    private val _events = Channel<RiskAssessmentEvent>(Channel.BUFFERED)
    val events: Flow<RiskAssessmentEvent> = _events.receiveAsFlow()

    private var age: Int = 0

    fun onAction(action: RiskAssessmentActions) {
        when (action) {
            is RiskAssessmentActions.OnEnter -> enter(action.customerId)
            is RiskAssessmentActions.OnAnswer -> _uiState.update { it.copy(answers = it.answers + (action.question to action.option)) }
            RiskAssessmentActions.OnNext -> next()
            RiskAssessmentActions.OnPrevious -> _uiState.update {
                it.copy(stepIndex = (it.stepIndex - 1).coerceAtLeast(0), result = null, confirmed = false, error = null)
            }
            is RiskAssessmentActions.OnConfirmChange -> _uiState.update { it.copy(confirmed = action.confirmed) }
            is RiskAssessmentActions.OnSignatureChange -> _uiState.update { it.copy(signature = action.signature, error = null) }
            is RiskAssessmentActions.OnSave -> save(action.note)
        }
    }

    private fun enter(customerId: String) {
        if (_uiState.value.customerId == customerId) return
        _uiState.value = RiskAssessmentUiState(customerId = customerId)
        viewModelScope.launch {
            val customer = useCases.observeCustomer(customerId).first() ?: return@launch
            age = customer.ageOn(todayInSeoul())
            _uiState.update {
                it.copy(customerName = customer.name, answers = it.answers + (RiskQuestion.AGE to RiskAssessmentPolicy.ageOption(age)))
            }
        }
    }

    private fun next() {
        val state = _uiState.value
        if (!state.canGoNext) return
        val nextIndex = state.stepIndex + 1
        _uiState.update {
            if (nextIndex >= RiskQuestion.entries.size) {
                it.copy(stepIndex = nextIndex, result = RiskAssessmentPolicy.assess(it.answers, age))
            } else {
                it.copy(stepIndex = nextIndex)
            }
        }
    }

    private fun save(note: String) {
        val state = _uiState.value
        val customerId = state.customerId ?: return
        val result = state.result ?: return
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (useCases.save(customerId, result, state.signature, note)) {
                SaveRiskResult.Saved -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(RiskAssessmentEvent.Saved)
                }
                SaveRiskResult.SignatureRequired -> _uiState.update { it.copy(isSaving = false, error = RiskSaveError.SIGNATURE_REQUIRED) }
                SaveRiskResult.NotSignedIn -> _uiState.update { it.copy(isSaving = false, error = RiskSaveError.NOT_SIGNED_IN) }
            }
        }
    }
}
