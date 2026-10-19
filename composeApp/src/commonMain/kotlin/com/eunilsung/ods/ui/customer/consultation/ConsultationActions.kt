package com.eunilsung.ods.ui.customer.consultation

import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic

sealed interface ConsultationActions {
    /** 이 고객의 상담을 적는다. 고객이 바뀌면 적던 내용을 비운다. */
    data class OnEnter(val customerId: String) : ConsultationActions

    data class OnChannelChange(val channel: ConsultChannel) : ConsultationActions
    data class OnTopicChange(val topic: ConsultTopic) : ConsultationActions
    data class OnContentChange(val content: String) : ConsultationActions
    data object OnSave : ConsultationActions
}

/** 상담 기록이 한 번씩 처리할 일. */
sealed interface ConsultationEvent {
    /** 저장했다. 따로 연 기록 화면이면 닫는다. */
    data object Saved : ConsultationEvent
}
