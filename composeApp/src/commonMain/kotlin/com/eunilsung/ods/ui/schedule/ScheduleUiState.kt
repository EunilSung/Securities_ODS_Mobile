package com.eunilsung.ods.ui.schedule

import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.util.todayInSeoul
import com.eunilsung.ods.domain.util.weekDays
import kotlinx.datetime.LocalDate

/** 일정 화면의 상태. */
data class ScheduleUiState(
    val today: LocalDate = todayInSeoul(),
    val selected: LocalDate = today,
    /** 고른 날이 들어 있는 주(월~일). */
    val weekDays: List<LocalDate> = selected.weekDays(),
    /** 그 주 날짜별 방문 건수(취소 제외). */
    val counts: Map<LocalDate, Int> = emptyMap(),
    /** 고른 날의 방문(시각순). */
    val visits: List<Visit> = emptyList(),
)
