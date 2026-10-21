package com.eunilsung.ods.ui.schedule.register

import com.eunilsung.ods.domain.model.CalendarMode
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.VisitPurpose
import com.eunilsung.ods.domain.util.calendarDays
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.datetime.LocalDate

/** 일정 등록 양식의 상태. */
data class NewVisitUiState(
    val today: LocalDate = todayInSeoul(),
    val date: LocalDate = today,
    /** 달력 보기. 늘 주 보기로 연다. */
    val calendarMode: CalendarMode = CalendarMode.WEEK,
    /** 달력에 보일 날 — 고른 날이 든 주(월~일) 또는 달(6주). */
    val days: List<LocalDate> = date.calendarDays(calendarMode),
    /** 고른 고객. 없으면 찾기 칸과 목록을 보인다. */
    val customer: Customer? = null,
    val customerQuery: String = "",
    /** 찾기 칸에 맞는 고객(최대 몇 명). */
    val customerMatches: List<Customer> = emptyList(),
    /** 고른 시각(시, 분). */
    val time: Pair<Int, Int>? = null,
    val place: String = "",
    val purpose: VisitPurpose = VisitPurpose.CONSULT,
    val memo: String = "",
    val showErrors: Boolean = false,
    /** 고른 날·시각이 이미 지났다. */
    val inPast: Boolean = false,
    val isSaving: Boolean = false,
    /** 서버에 닿지 못했거나 서버가 받아 주지 않아 등록하지 못했다. 적은 내용은 그대로 두어 다시 누를 수 있다. */
    val saveFailed: Boolean = false,
) {
    val customerError: Boolean get() = customer == null
    val timeError: Boolean get() = time == null
    val placeError: Boolean get() = place.isBlank()
    val hasError: Boolean get() = customerError || timeError || placeError

    /** 고를 수 있는 날 — 오늘부터(한국 시간). 지난 날은 달력에서 흐리게 보이고 눌리지 않는다. */
    fun isSelectable(day: LocalDate): Boolean = day >= today
}
