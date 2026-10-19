package com.eunilsung.ods.ui.customer.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.usecase.CustomerListUseCases
import com.eunilsung.ods.domain.util.matchesQuery
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 고객 목록 — 담당 고객을 이름·초성·전화번호로 찾고, 누르면 상세를 연다. */
class CustomerListViewModel(
    private val useCases: CustomerListUseCases,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<CustomerListUiState> = combine(
        useCases.observeCustomers(),
        query,
        useCases.observeRefreshing(),
        useCases.observeMaskPersonalInfo(),
        useCases.observeActiveCustomerId(),
    ) { customers, q, refreshing, mask, activeId ->
        CustomerListUiState(
            query = q,
            customers = customers.filter { it.matchesQuery(q) },
            totalCount = customers.size,
            isRefreshing = refreshing,
            maskPersonalInfo = mask,
            today = todayInSeoul(),
            activeCustomerId = activeId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerListUiState())

    private val _events = Channel<CustomerListEvent>(Channel.BUFFERED)
    val events: Flow<CustomerListEvent> = _events.receiveAsFlow()

    fun onAction(action: CustomerListActions) {
        when (action) {
            is CustomerListActions.OnQueryChange -> query.value = action.query
            CustomerListActions.OnRefresh -> viewModelScope.launch { useCases.refresh() }
            is CustomerListActions.OnCustomerClick -> viewModelScope.launch { _events.send(CustomerListEvent.OpenDetail(action.id)) }
            CustomerListActions.OnNewCustomerClick -> viewModelScope.launch { _events.send(CustomerListEvent.OpenNewCustomer) }
        }
    }
}
