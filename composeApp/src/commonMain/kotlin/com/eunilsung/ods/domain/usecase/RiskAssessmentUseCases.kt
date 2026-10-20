package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.RiskAssessmentResult
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.repository.CustomerRepository
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.repository.SignatureRepository
import com.eunilsung.ods.domain.util.SignaturePolicy

/** 진단 결과 저장 결과. */
sealed interface SaveRiskResult {
    data object Saved : SaveRiskResult

    /** 서명이 모자라다(점이 너무 적거나 너무 작다). */
    data object SignatureRequired : SaveRiskResult

    /** 로그인이 풀려 기록할 직원이 없다. */
    data object NotSignedIn : SaveRiskResult

    /** 서버에 닿지 못했거나 서버가 받아 주지 않아 투자성향을 바꾸지 못했다. */
    data object Unreachable : SaveRiskResult
}

/**
 * 투자성향 진단을 마친다 — 고객 투자성향을 바꾸고, 고객 서명과 상담 이력에 진단 기록을 남긴다.
 *
 * 투자성향 저장이 본 작업이라 먼저 한다. 이것이 실패하면 아무것도 남기지 않고 [SaveRiskResult.Unreachable].
 * 투자성향이 바뀐 뒤 서명 기록·상담 이력이 실패해도 [SaveRiskResult.Saved] 다 — 이미 바뀐 성향을 실패라고 알리면
 * 직원이 다시 진단해 같은 저장이 겹친다. 그때 빠진 서명 기록·상담 이력은 남지 않는다.
 *
 * @param note 상담 이력에 남길 문구. 화면이 고객 언어로 만들어 넘긴다.
 */
class SaveRiskAssessmentUseCase(
    private val customers: CustomerRepository,
    private val signatures: SignatureRepository,
    private val login: LoginRepository,
) {
    suspend operator fun invoke(
        customerId: String,
        result: RiskAssessmentResult,
        signature: Signature,
        note: String,
        now: Long = nowMillis(),
    ): SaveRiskResult {
        if (!SignaturePolicy.isEnough(signature)) return SaveRiskResult.SignatureRequired
        val staff = login.currentStaff.value ?: return SaveRiskResult.NotSignedIn
        orNullOnFailure { customers.saveRiskProfile(customerId, result.profile, now) } ?: return SaveRiskResult.Unreachable
        orNullOnFailure { signatures.record(customerId, SignaturePurpose.RISK_PROFILE, signature, now) }
        val consultation = NewConsultation(customerId, ConsultChannel.VISIT, ConsultTopic.RISK_PROFILE, note)
        orNullOnFailure { customers.addConsultation(consultation, staff.name, now) }
        return SaveRiskResult.Saved
    }
}

/** 투자성향 진단 화면이 쓰는 유스케이스 묶음. */
class RiskAssessmentUseCases(
    val observeCustomer: ObserveCustomerUseCase,
    val save: SaveRiskAssessmentUseCase,
)
