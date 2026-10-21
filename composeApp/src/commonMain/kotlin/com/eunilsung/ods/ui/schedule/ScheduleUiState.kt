package com.eunilsung.ods.ui.schedule

import com.eunilsung.ods.domain.model.CalendarMode
import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.util.calendarDays
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.datetime.LocalDate

/** 일정 화면의 상태. */
data class ScheduleUiState(
    val today: LocalDate = todayInSeoul(),
    val selected: LocalDate = today,
    val calendarMode: CalendarMode = CalendarMode.WEEK,
    /** 달력에 보일 날 — 고른 날이 든 주(월~일) 또는 달(6주). */
    val days: List<LocalDate> = selected.calendarDays(calendarMode),
    /** 달력에 보이는 날짜별 방문 건수(취소 제외). */
    val counts: Map<LocalDate, Int> = emptyMap(),
    /** 고른 날의 방문(시각순). */
    val visits: List<Visit> = emptyList(),
)
