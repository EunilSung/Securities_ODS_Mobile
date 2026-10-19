package com.eunilsung.ods.ui.consult

import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer

/** 상담 칸의 상태. */
data class ConsultPaneUiState(
    /** 상담 중인 고객. 없으면 안내만 보인다. */
    val customer: Customer? = null,
    /** 최근 상담 몇 건. */
    val recent: List<Consultation> = emptyList(),
)
