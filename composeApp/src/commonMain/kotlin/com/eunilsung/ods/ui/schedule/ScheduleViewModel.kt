package com.eunilsung.ods.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.usecase.ScheduleUseCases
import com.eunilsung.ods.domain.util.startOfWeek
import com.eunilsung.ods.domain.util.todayInSeoul
import com.eunilsung.ods.domain.util.weekDays
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 일정 — 한 주 달력과 고른 날의 방문 목록. 처음에는 오늘을 연다.
 *
 * 주를 넘기면 같은 요일로 옮겨 간다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val useCases: ScheduleUseCases,
) : ViewModel() {

    private val selected = MutableStateFlow(todayInSeoul())

    val uiState: StateFlow<ScheduleUiState> = combine(
        selected,
        selected.flatMapLatest { useCases.observeDayVisits(it) },
        selected.map { it.startOfWeek() }.distinctUntilChanged().flatMapLatest { useCases.observeWeekVisitCounts(it) },
    ) { date, visits, counts ->
        ScheduleUiState(today = todayInSeoul(), selected = date, weekDays = date.weekDays(), counts = counts, visits = visits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    private val _events = Channel<ScheduleEvent>(Channel.BUFFERED)
    val events: Flow<ScheduleEvent> = _events.receiveAsFlow()

    fun onAction(action: ScheduleActions) {
        when (action) {
            is ScheduleActions.OnSelectDate -> selected.value = action.date
            ScheduleActions.OnPreviousWeek -> selected.value = selected.value.minus(DatePeriod(days = 7))
            ScheduleActions.OnNextWeek -> selected.value = selected.value.plus(DatePeriod(days = 7))
            ScheduleActions.OnToday -> selected.value = todayInSeoul()
            is ScheduleActions.OnVisitClick -> viewModelScope.launch { _events.send(ScheduleEvent.OpenVisit(action.id)) }
            ScheduleActions.OnAddClick -> viewModelScope.launch { _events.send(ScheduleEvent.OpenNewVisit(selected.value)) }
        }
    }
}
