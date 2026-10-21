package com.eunilsung.ods.ui.schedule

import com.eunilsung.ods.domain.model.CalendarMode
import kotlinx.datetime.LocalDate

sealed interface ScheduleActions {
    data class OnSelectDate(val date: LocalDate) : ScheduleActions

    /** 이전 주 또는 이전 달. 주는 같은 요일, 달은 같은 날(없으면 그 달 마지막 날)로 간다. */
    data object OnPrevious : ScheduleActions

    /** 다음 주 또는 다음 달. */
    data object OnNext : ScheduleActions

    /** 주·월 보기를 골랐다. 다음 실행에도 그대로 둔다. */
    data class OnModeChange(val mode: CalendarMode) : ScheduleActions

    /** 연월을 눌렀다. 주·월 보기를 맞바꾼다. */
    data object OnTitleClick : ScheduleActions

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
