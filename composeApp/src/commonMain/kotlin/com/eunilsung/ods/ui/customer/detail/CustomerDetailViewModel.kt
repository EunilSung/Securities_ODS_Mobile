package com.eunilsung.ods.ui.customer.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.usecase.CustomerDetailUseCases
import com.eunilsung.ods.domain.util.todayInSeoul
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
import kotlinx.coroutines.launch

/**
 * 고객 상세 — 기본 정보, 투자성향, 계좌, 방문 예정, 상담 이력을 한 고객에 묶어 내놓는다.
 *
 * 상세를 열면 그 고객과 상담을 시작한 것으로 본다(넓은 태블릿의 상담 칸이 따라온다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomerDetailViewModel(
    private val useCases: CustomerDetailUseCases,
) : ViewModel() {

    private val customerId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CustomerDetailUiState> = customerId.filterNotNull().flatMapLatest { id ->
        combine(
            useCases.observeCustomer(id),
            useCases.observeAccounts(id),
            useCases.observeConsultations(id),
            useCases.observeMaskPersonalInfo(),
            useCases.observeUpcomingVisits(id),
        ) { customer, accounts, consultations, mask, visits ->
            CustomerDetailUiState(
                customer = customer,
                accounts = accounts,
                consultations = consultations,
                upcomingVisits = visits,
                maskPersonalInfo = mask,
                today = todayInSeoul(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerDetailUiState())

    private val _events = Channel<CustomerDetailEvent>(Channel.BUFFERED)
    val events: Flow<CustomerDetailEvent> = _events.receiveAsFlow()

    fun onAction(action: CustomerDetailActions) {
        when (action) {
            is CustomerDetailActions.OnEnter -> {
                customerId.value = action.customerId
                useCases.startConsult(action.customerId)
            }
            CustomerDetailActions.OnToggleMask -> useCases.setMaskPersonalInfo(!useCases.observeMaskPersonalInfo().value)
            CustomerDetailActions.OnRiskAssessClick -> send { CustomerDetailEvent.OpenRiskAssessment(it) }
            CustomerDetailActions.OnAddConsultationClick -> send { CustomerDetailEvent.OpenConsultationEditor(it) }
            CustomerDetailActions.OnAddVisitClick -> send { CustomerDetailEvent.OpenNewVisit(it) }
            is CustomerDetailActions.OnVisitClick -> viewModelScope.launch { _events.send(CustomerDetailEvent.OpenVisit(action.visitId)) }
        }
    }

    /** 지금 보는 고객 id 로 한 번만 처리할 일을 보낸다. */
    private fun send(event: (String) -> CustomerDetailEvent) {
        val id = customerId.value ?: return
        viewModelScope.launch { _events.send(event(id)) }
    }
}
