package com.eunilsung.ods.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.CalendarMode
import com.eunilsung.ods.domain.usecase.ScheduleUseCases
import com.eunilsung.ods.domain.util.calendarDays
import com.eunilsung.ods.domain.util.movedBy
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** 점을 찍을 기간 — 보이는 주·달과 그 앞뒤 한 칸. 달력을 밀 때 넘어오는 주·달에도 점이 보인다. */
private fun LocalDate.countRange(mode: CalendarMode): Pair<LocalDate, LocalDate> =
    movedBy(mode, -1).calendarDays(mode).first() to movedBy(mode, 1).calendarDays(mode).last()

/**
 * 일정 — 주 또는 달 달력과 고른 날의 방문 목록. 처음에는 오늘을, 지난번에 고른 보기(주·월)로 연다.
 *
 * 주를 넘기면 같은 요일로, 달을 넘기면 같은 날(없으면 그 달 마지막 날)로 옮겨 간다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val useCases: ScheduleUseCases,
) : ViewModel() {

    private val selected = MutableStateFlow(todayInSeoul())
    private val mode = useCases.observeCalendarMode()
    private val days = combine(selected, mode) { date, calendarMode -> date.calendarDays(calendarMode) }.distinctUntilChanged()

    val uiState: StateFlow<ScheduleUiState> = combine(
        selected,
        mode,
        days,
        selected.flatMapLatest { useCases.observeDayVisits(it) },
        combine(selected, mode) { date, calendarMode -> date.countRange(calendarMode) }.distinctUntilChanged()
            .flatMapLatest { (from, until) -> useCases.observeVisitCounts(from, until) },
    ) { date, calendarMode, visibleDays, visits, counts ->
        ScheduleUiState(today = todayInSeoul(), selected = date, calendarMode = calendarMode, days = visibleDays, counts = counts, visits = visits)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState(calendarMode = mode.value))

    private val _events = Channel<ScheduleEvent>(Channel.BUFFERED)
    val events: Flow<ScheduleEvent> = _events.receiveAsFlow()

    fun onAction(action: ScheduleActions) {
        when (action) {
            is ScheduleActions.OnSelectDate -> selected.value = action.date
            ScheduleActions.OnPrevious -> selected.value = selected.value.movedBy(mode.value, -1)
            ScheduleActions.OnNext -> selected.value = selected.value.movedBy(mode.value, 1)
            is ScheduleActions.OnModeChange -> useCases.setCalendarMode(action.mode)
            ScheduleActions.OnTitleClick -> useCases.setCalendarMode(
                if (mode.value == CalendarMode.WEEK) CalendarMode.MONTH else CalendarMode.WEEK
            )
            ScheduleActions.OnToday -> selected.value = todayInSeoul()
            is ScheduleActions.OnVisitClick -> viewModelScope.launch { _events.send(ScheduleEvent.OpenVisit(action.id)) }
            ScheduleActions.OnAddClick -> viewModelScope.launch { _events.send(ScheduleEvent.OpenNewVisit(selected.value)) }
        }
    }
}
