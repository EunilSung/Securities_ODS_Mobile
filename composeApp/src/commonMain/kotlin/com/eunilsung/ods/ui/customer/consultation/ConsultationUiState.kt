package com.eunilsung.ods.ui.customer.consultation

import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic

/** 상담 기록 입력의 상태. */
data class ConsultationUiState(
    val customerId: String? = null,
    val channel: ConsultChannel = ConsultChannel.VISIT,
    val topic: ConsultTopic = ConsultTopic.GENERAL,
    val content: String = "",
    val isSaving: Boolean = false,
    /** 저장하지 못한 까닭. 저장하지 못해도 적은 내용은 그대로 둔다. */
    val saveError: ConsultSaveError? = null,
    /** 방금 저장했다. 다음 글자를 넣으면 지운다. */
    val justSaved: Boolean = false,
) {
    val canSave: Boolean get() = customerId != null && content.isNotBlank() && !isSaving

    companion object {
        /** 상담 내용 최대 글자 수. */
        const val MAX_LENGTH = 1000
    }
}

/** 상담 기록을 저장하지 못한 까닭. */
enum class ConsultSaveError { NOT_SIGNED_IN, UNREACHABLE }
