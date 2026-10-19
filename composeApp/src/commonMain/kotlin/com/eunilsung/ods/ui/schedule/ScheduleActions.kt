package com.eunilsung.ods.ui.schedule

import kotlinx.datetime.LocalDate

sealed interface ScheduleActions {
    data class OnSelectDate(val date: LocalDate) : ScheduleActions
    data object OnPreviousWeek : ScheduleActions
    data object OnNextWeek : ScheduleActions
    data object OnToday : ScheduleActions

    /** 방문을 눌렀다. 상세를 연다. */
    data class OnVisitClick(val id: String) : ScheduleActions

    /** 일정 추가를 눌렀다. 고른 날로 등록 화면을 연다. */
    data object OnAddClick : ScheduleActions
}

/** 일정 화면이 한 번씩 처리할 일. */
sealed interface ScheduleEvent {
    data class OpenVisit(val id: String) : ScheduleEvent
    data class OpenNewVisit(val date: LocalDate) : ScheduleEvent
}
