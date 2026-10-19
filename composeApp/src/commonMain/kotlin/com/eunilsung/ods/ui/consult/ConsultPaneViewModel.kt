package com.eunilsung.ods.ui.consult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.usecase.ConsultUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * 상담 칸 — 넓은 태블릿 오른쪽 끝. 상담 중인 고객을 따라가며 최근 상담과 바로 적는 칸을 보인다.
 *
 * 가운데에서 시세를 보여 주거나 상품을 설명하는 동안에도 고객과 메모가 그대로 남아 있게 하려는 칸이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConsultPaneViewModel(
    private val useCases: ConsultUseCases,
) : ViewModel() {

    val uiState: StateFlow<ConsultPaneUiState> = useCases.observeActiveCustomerId().flatMapLatest { id ->
        if (id == null) {
            flowOf(ConsultPaneUiState())
        } else {
            combine(useCases.observeCustomer(id), useCases.observeConsultations(id)) { customer, consultations ->
                ConsultPaneUiState(customer = customer, recent = consultations.take(RECENT_COUNT))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConsultPaneUiState())

    fun onAction(action: ConsultPaneActions) {
        when (action) {
            ConsultPaneActions.OnEndConsult -> useCases.endConsult()
        }
    }

    private companion object {
        const val RECENT_COUNT = 3
    }
}
