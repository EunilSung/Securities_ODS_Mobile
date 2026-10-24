package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.RiskAssessmentResult
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.repository.CustomerRepository
import com.eunilsung.ods.domain.repository.SignatureRepository
import com.eunilsung.ods.domain.util.SignaturePolicy

/** 진단 결과 저장 결과. */
sealed interface SaveRiskResult {
    data object Saved : SaveRiskResult

    /** 서명이 모자라다(점이 너무 적거나 너무 작다). */
    data object SignatureRequired : SaveRiskResult

    /** 로그인이 풀려 기록할 직원이 없다. */
    data object NotSignedIn : SaveRiskResult
}

/**
 * 투자성향 진단을 마친다 — 고객 서명을 남기고, 고객 투자성향을 바꾸고, 상담 이력에 진단 기록을 남긴다.
 *
 * @param note 상담 이력에 남길 문구. 화면이 고객 언어로 만들어 넘긴다.
 */
class SaveRiskAssessmentUseCase(
    private val customers: CustomerRepository,
    private val signatures: SignatureRepository,
    private val addConsultation: AddConsultationUseCase,
) {
    suspend operator fun invoke(
        customerId: String,
        result: RiskAssessmentResult,
        signature: Signature,
        note: String,
        now: Long = nowMillis(),
    ): SaveRiskResult {
        if (!SignaturePolicy.isEnough(signature)) return SaveRiskResult.SignatureRequired
        val consultation = NewConsultation(customerId, ConsultChannel.VISIT, ConsultTopic.RISK_PROFILE, note)
        addConsultation(consultation, now) ?: return SaveRiskResult.NotSignedIn
        signatures.record(customerId, SignaturePurpose.RISK_PROFILE, signature, now)
        customers.saveRiskProfile(customerId, result.profile, now)
        return SaveRiskResult.Saved
    }
}

/** 투자성향 진단 화면이 쓰는 유스케이스 묶음. */
class RiskAssessmentUseCases(
    val observeCustomer: ObserveCustomerUseCase,
    val save: SaveRiskAssessmentUseCase,
)
