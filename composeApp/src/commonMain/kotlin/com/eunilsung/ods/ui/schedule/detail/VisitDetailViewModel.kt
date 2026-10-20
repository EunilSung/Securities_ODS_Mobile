package com.eunilsung.ods.ui.schedule.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.domain.usecase.VisitDetailUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 방문 상세 — 일정 내용과 완료·취소.
 *
 * 완료하면 그 고객과 상담 중으로 두고, 방문 목적을 주제로 상담 기록을 연다. 취소는 확인을 한 번 더 받는다.
 * 완료·취소를 남기지 못하면 [VisitDetailUiState.actionFailed] 를 켜고 상담 기록은 열지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VisitDetailViewModel(
    private val useCases: VisitDetailUseCases,
) : ViewModel() {

    private val visitId = MutableStateFlow<String?>(null)
    private val confirmingCancel = MutableStateFlow(false)
    private val actionFailed = MutableStateFlow(false)

    val uiState: StateFlow<VisitDetailUiState> = combine(
        visitId.filterNotNull().flatMapLatest { useCases.observeVisit(it) },
        confirmingCancel,
        actionFailed,
    ) { visit, confirming, failed ->
        VisitDetailUiState(visit = visit, isConfirmingCancel = confirming, actionFailed = failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VisitDetailUiState())

    private val _events = Channel<VisitDetailEvent>(Channel.BUFFERED)
    val events: Flow<VisitDetailEvent> = _events.receiveAsFlow()

    fun onAction(action: VisitDetailActions) {
        val visit = uiState.value.visit
        when (action) {
            is VisitDetailActions.OnEnter -> visitId.value = action.visitId
            VisitDetailActions.OnOpenCustomer -> if (visit != null) {
                viewModelScope.launch { _events.send(VisitDetailEvent.OpenCustomer(visit.customerId)) }
            }
            VisitDetailActions.OnComplete -> if (visit != null && visit.status == VisitStatus.SCHEDULED) {
                actionFailed.value = false
                viewModelScope.launch {
                    if (useCases.completeVisit(visit)) {
                        _events.send(VisitDetailEvent.OpenConsultation(visit.customerId, visit.purpose.topic))
                    } else {
                        actionFailed.value = true
                    }
                }
            }
            VisitDetailActions.OnCancelClick -> if (visit?.status == VisitStatus.SCHEDULED) confirmingCancel.value = true
            VisitDetailActions.OnCancelDismiss -> confirmingCancel.value = false
            VisitDetailActions.OnCancelConfirm -> {
                confirmingCancel.update { false }
                actionFailed.value = false
                if (visit != null) viewModelScope.launch { if (!useCases.cancelVisit(visit.id)) actionFailed.value = true }
            }
        }
    }
}
