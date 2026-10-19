package com.eunilsung.ods.ui.schedule.detail

import com.eunilsung.ods.domain.model.ConsultTopic

sealed interface VisitDetailActions {
    data class OnEnter(val visitId: String) : VisitDetailActions

    /** 고객 정보를 눌렀다. 고객 상세를 연다. */
    data object OnOpenCustomer : VisitDetailActions

    /** 방문 완료. 완료로 바꾸고 상담 기록을 연다. */
    data object OnComplete : VisitDetailActions

    /** 일정 취소를 눌렀다. 확인을 받는다. */
    data object OnCancelClick : VisitDetailActions
    data object OnCancelConfirm : VisitDetailActions
    data object OnCancelDismiss : VisitDetailActions
}

/** 방문 상세가 한 번씩 처리할 일. */
sealed interface VisitDetailEvent {
    data class OpenCustomer(val customerId: String) : VisitDetailEvent

    /** 방문을 마쳤다. 방문 목적을 주제로 상담 기록을 연다. */
    data class OpenConsultation(val customerId: String, val topic: ConsultTopic) : VisitDetailEvent
}
