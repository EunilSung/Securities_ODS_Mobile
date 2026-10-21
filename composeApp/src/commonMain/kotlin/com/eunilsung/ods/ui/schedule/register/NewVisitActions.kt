package com.eunilsung.ods.ui.schedule.register

import com.eunilsung.ods.domain.model.CalendarMode
import com.eunilsung.ods.domain.model.VisitPurpose
import kotlinx.datetime.LocalDate

sealed interface NewVisitActions {
    /**
     * 등록 화면이 열렸다.
     *
     * @param date 처음 고를 날. 없으면 오늘.
     * @param customerId 고객 상세에서 열었으면 그 고객으로 정해 둔다.
     */
    data class OnEnter(val date: LocalDate?, val customerId: String?) : NewVisitActions

    data class OnCustomerQueryChange(val query: String) : NewVisitActions
    data class OnCustomerSelect(val customerId: String) : NewVisitActions

    /** 고른 고객을 풀고 다시 찾는다. */
    data object OnCustomerClear : NewVisitActions

    /** 날을 골랐다. 오늘보다 앞선 날은 받지 않는다. */
    data class OnDateSelect(val date: LocalDate) : NewVisitActions

    /** 이전 주 또는 이전 달. 오늘보다 앞서면 오늘로 간다. */
    data object OnPrevious : NewVisitActions

    /** 다음 주 또는 다음 달. */
    data object OnNext : NewVisitActions

    /** 주·월 보기를 골랐다. 이 화면에서만 쓰고 남기지 않는다. */
    data class OnModeChange(val mode: CalendarMode) : NewVisitActions

    /** 연월을 눌렀다. 주·월 보기를 맞바꾼다. */
    data object OnTitleClick : NewVisitActions

    data class OnTimeSelect(val hour: Int, val minute: Int) : NewVisitActions
    data class OnPlaceChange(val place: String) : NewVisitActions
    data class OnPurposeChange(val purpose: VisitPurpose) : NewVisitActions
    data class OnMemoChange(val memo: String) : NewVisitActions
    data object OnSubmit : NewVisitActions
}

/** 일정 등록이 한 번씩 처리할 일. */
sealed interface NewVisitEvent {
    /** 등록했다. 그 일정의 상세를 연다. */
    data class Added(val visitId: String) : NewVisitEvent
}
