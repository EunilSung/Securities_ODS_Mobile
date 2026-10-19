package com.eunilsung.ods.ui.customer.detail

sealed interface CustomerDetailActions {
    /** 상세가 열렸다. 이 고객을 보고, 이 고객과 상담을 시작한다. */
    data class OnEnter(val customerId: String) : CustomerDetailActions

    /** 눈 버튼 — 개인정보 가리기를 켜고 끈다. 앱 전체 설정이 함께 바뀐다. */
    data object OnToggleMask : CustomerDetailActions

    /** 상담 기록을 눌렀다. */
    data object OnAddConsultationClick : CustomerDetailActions
}

/** 고객 상세가 한 번씩 처리할 일. */
sealed interface CustomerDetailEvent {
    data class OpenConsultationEditor(val customerId: String) : CustomerDetailEvent
}
