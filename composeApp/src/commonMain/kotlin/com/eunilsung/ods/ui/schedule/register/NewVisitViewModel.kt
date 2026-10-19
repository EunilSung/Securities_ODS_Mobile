package com.eunilsung.ods.ui.schedule.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.NewVisit
import com.eunilsung.ods.domain.usecase.AddVisitResult
import com.eunilsung.ods.domain.usecase.NewVisitUseCases
import com.eunilsung.ods.domain.util.atTimeMillis
import com.eunilsung.ods.domain.util.matchesQuery
import com.eunilsung.ods.domain.util.todayInSeoul
import com.eunilsung.ods.domain.util.weekDays
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 방문 일정 등록 — 고객, 날·시각(30분 단위), 장소, 목적, 메모를 받는다.
 *
 * 고객은 이름·초성·전화번호로 찾아 고른다. 지난 시각은 받지 않는다.
 */
class NewVisitViewModel(
    private val useCases: NewVisitUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewVisitUiState())
    val uiState: StateFlow<NewVisitUiState> = _uiState.asStateFlow()

    private val _events = Channel<NewVisitEvent>(Channel.BUFFERED)
    val events: Flow<NewVisitEvent> = _events.receiveAsFlow()

    private var customers: List<Customer> = emptyList()

    fun onAction(action: NewVisitActions) {
        when (action) {
            is NewVisitActions.OnEnter -> enter(action.date, action.customerId)
            is NewVisitActions.OnCustomerQueryChange -> _uiState.update {
                it.copy(customerQuery = action.query, customerMatches = match(action.query))
            }
            is NewVisitActions.OnCustomerSelect -> _uiState.update { state ->
                state.copy(customer = customers.firstOrNull { it.id == action.customerId }, customerQuery = "", customerMatches = emptyList())
            }
            NewVisitActions.OnCustomerClear -> _uiState.update { it.copy(customer = null, customerMatches = match("")) }
            is NewVisitActions.OnDateSelect -> setDate(action.date)
            NewVisitActions.OnPreviousWeek -> setDate(_uiState.value.date.minus(DatePeriod(days = 7)))
            NewVisitActions.OnNextWeek -> setDate(_uiState.value.date.plus(DatePeriod(days = 7)))
            is NewVisitActions.OnTimeSelect -> _uiState.update { it.copy(time = action.hour to action.minute, inPast = false) }
            is NewVisitActions.OnPlaceChange -> _uiState.update { it.copy(place = action.place.take(MAX_TEXT)) }
            is NewVisitActions.OnPurposeChange -> _uiState.update { it.copy(purpose = action.purpose) }
            is NewVisitActions.OnMemoChange -> _uiState.update { it.copy(memo = action.memo.take(MAX_TEXT)) }
            NewVisitActions.OnSubmit -> submit()
        }
    }

    private fun enter(date: LocalDate?, customerId: String?) {
        val start = date ?: todayInSeoul()
        _uiState.value = NewVisitUiState(date = start, weekDays = start.weekDays())
        viewModelScope.launch {
            customers = useCases.observeCustomers().first()
            _uiState.update { state ->
                val preset = customers.firstOrNull { it.id == customerId }
                state.copy(customer = preset, customerMatches = if (preset == null) match("") else emptyList())
            }
        }
    }

    private fun setDate(date: LocalDate) = _uiState.update { it.copy(date = date, weekDays = date.weekDays(), inPast = false) }

    private fun match(query: String): List<Customer> = customers.filter { it.matchesQuery(query) }.take(MAX_MATCHES)

    private fun submit() {
        val state = _uiState.value
        if (state.isSaving) return
        val customer = state.customer
        val time = state.time
        if (state.hasError || customer == null || time == null) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val visit = NewVisit(customer.id, state.date.atTimeMillis(time.first, time.second), state.place, state.purpose, state.memo)
            when (val result = useCases.addVisit(visit)) {
                is AddVisitResult.Added -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(NewVisitEvent.Added(result.visit.id))
                }
                AddVisitResult.InPast -> _uiState.update { it.copy(isSaving = false, inPast = true, showErrors = true) }
            }
        }
    }

    private companion object {
        const val MAX_MATCHES = 6
        const val MAX_TEXT = 100
    }
}
