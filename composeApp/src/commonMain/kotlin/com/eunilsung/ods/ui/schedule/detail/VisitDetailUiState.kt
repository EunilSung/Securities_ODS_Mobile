package com.eunilsung.ods.ui.schedule.detail

import com.eunilsung.ods.domain.model.Visit

/** 방문 상세의 상태. */
data class VisitDetailUiState(
    /** 아직 읽지 못했거나 없는 일정이면 null. */
    val visit: Visit? = null,
    /** 취소 확인 창이 떠 있는지. */
    val isConfirmingCancel: Boolean = false,
)
