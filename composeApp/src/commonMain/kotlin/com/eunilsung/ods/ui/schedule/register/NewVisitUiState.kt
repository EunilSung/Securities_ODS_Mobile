package com.eunilsung.ods.ui.schedule.register

import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.VisitPurpose
import com.eunilsung.ods.domain.util.todayInSeoul
import com.eunilsung.ods.domain.util.weekDays
import kotlinx.datetime.LocalDate

/** 일정 등록 양식의 상태. */
data class NewVisitUiState(
    val today: LocalDate = todayInSeoul(),
    val date: LocalDate = today,
    val weekDays: List<LocalDate> = date.weekDays(),
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
) {
    val customerError: Boolean get() = customer == null
    val timeError: Boolean get() = time == null
    val placeError: Boolean get() = place.isBlank()
    val hasError: Boolean get() = customerError || timeError || placeError
}
