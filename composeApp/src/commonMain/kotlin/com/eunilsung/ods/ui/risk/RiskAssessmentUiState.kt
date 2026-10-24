package com.eunilsung.ods.ui.risk

import com.eunilsung.ods.domain.model.RiskAssessmentResult
import com.eunilsung.ods.domain.model.RiskQuestion
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.util.SignaturePolicy

/** 투자성향 진단 화면의 상태. */
data class RiskAssessmentUiState(
    val customerId: String? = null,
    val customerName: String = "",
    /** 지금 보이는 문항 번호. [RiskQuestion.entries] 크기와 같으면 결과 화면이다. */
    val stepIndex: Int = 0,
    /** 문항별 고른 선택지. */
    val answers: Map<RiskQuestion, Int> = emptyMap(),
    /** 결과 화면에서만 있다. */
    val result: RiskAssessmentResult? = null,
    val confirmed: Boolean = false,
    val signature: Signature = Signature(emptyList()),
    val isSaving: Boolean = false,
    val error: RiskSaveError? = null,
) {
    val isResult: Boolean get() = stepIndex >= RiskQuestion.entries.size
    val question: RiskQuestion? get() = RiskQuestion.entries.getOrNull(stepIndex)
    val canGoNext: Boolean get() = question?.let { it in answers } ?: false
    val canSave: Boolean get() = isResult && confirmed && SignaturePolicy.isEnough(signature) && !isSaving
}

enum class RiskSaveError { SIGNATURE_REQUIRED, NOT_SIGNED_IN }
