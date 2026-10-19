package com.eunilsung.ods.ui.schedule.register

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

    data class OnDateSelect(val date: LocalDate) : NewVisitActions
    data object OnPreviousWeek : NewVisitActions
    data object OnNextWeek : NewVisitActions
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
