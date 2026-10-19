package com.eunilsung.ods.ui.customer.consultation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.usecase.ConsultUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 상담 기록 — 만난 곳·주제·내용을 받아 고객의 상담 이력에 남긴다.
 *
 * 따로 연 기록 화면과 넓은 태블릿의 상담 칸이 각각 하나씩 쓴다. 저장하면 내용을 비우고 [ConsultationEvent.Saved] 를 보낸다.
 */
class ConsultationViewModel(
    private val useCases: ConsultUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsultationUiState())
    val uiState: StateFlow<ConsultationUiState> = _uiState.asStateFlow()

    private val _events = Channel<ConsultationEvent>(Channel.BUFFERED)
    val events: Flow<ConsultationEvent> = _events.receiveAsFlow()

    fun onAction(action: ConsultationActions) {
        when (action) {
            is ConsultationActions.OnEnter -> if (_uiState.value.customerId != action.customerId) {
                _uiState.value = ConsultationUiState(customerId = action.customerId, topic = action.topic ?: ConsultTopic.GENERAL)
            }
            is ConsultationActions.OnChannelChange -> _uiState.update { it.copy(channel = action.channel) }
            is ConsultationActions.OnTopicChange -> _uiState.update { it.copy(topic = action.topic) }
            is ConsultationActions.OnContentChange -> _uiState.update {
                it.copy(content = action.content.take(ConsultationUiState.MAX_LENGTH), justSaved = false, saveFailed = false)
            }
            ConsultationActions.OnSave -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        val customerId = state.customerId ?: return
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val saved = useCases.addConsultation(NewConsultation(customerId, state.channel, state.topic, state.content))
            if (saved == null) {
                _uiState.update { it.copy(isSaving = false, saveFailed = true) }
            } else {
                _uiState.value = ConsultationUiState(customerId = customerId, channel = state.channel, justSaved = true)
                _events.send(ConsultationEvent.Saved)
            }
        }
    }
}
