package com.eunilsung.ods.ui.customer.detail

sealed interface CustomerDetailActions {
    /** 상세가 열렸다. 이 고객을 보고, 이 고객과 상담을 시작한다. */
    data class OnEnter(val customerId: String) : CustomerDetailActions

    /** 눈 버튼 — 개인정보 가리기를 켜고 끈다. 앱 전체 설정이 함께 바뀐다. */
    data object OnToggleMask : CustomerDetailActions

    /** 투자성향 진단을 눌렀다. */
    data object OnRiskAssessClick : CustomerDetailActions

    /** 상담 기록을 눌렀다. */
    data object OnAddConsultationClick : CustomerDetailActions

    /** 방문 일정 추가를 눌렀다. 이 고객으로 등록 화면을 연다. */
    data object OnAddVisitClick : CustomerDetailActions

    /** 방문 예정 한 건을 눌렀다. */
    data class OnVisitClick(val visitId: String) : CustomerDetailActions
}

/** 고객 상세가 한 번씩 처리할 일. */
sealed interface CustomerDetailEvent {
    data class OpenConsultationEditor(val customerId: String) : CustomerDetailEvent
    data class OpenRiskAssessment(val customerId: String) : CustomerDetailEvent
    data class OpenNewVisit(val customerId: String) : CustomerDetailEvent
    data class OpenVisit(val visitId: String) : CustomerDetailEvent
}
