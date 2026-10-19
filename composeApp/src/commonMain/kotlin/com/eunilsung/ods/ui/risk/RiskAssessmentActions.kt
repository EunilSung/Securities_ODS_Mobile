package com.eunilsung.ods.ui.risk

import com.eunilsung.ods.domain.model.RiskQuestion
import com.eunilsung.ods.domain.model.Signature

sealed interface RiskAssessmentActions {
    /** 진단을 시작했다. 고객 나이로 나이 문항을 미리 고른다. */
    data class OnEnter(val customerId: String) : RiskAssessmentActions

    data class OnAnswer(val question: RiskQuestion, val option: Int) : RiskAssessmentActions

    /** 다음 문항(마지막 문항이면 결과). */
    data object OnNext : RiskAssessmentActions

    /** 이전 문항. */
    data object OnPrevious : RiskAssessmentActions

    /** 고객이 직접 답했고 결과를 확인했다는 확인란. */
    data class OnConfirmChange(val confirmed: Boolean) : RiskAssessmentActions

    data class OnSignatureChange(val signature: Signature) : RiskAssessmentActions

    /**
     * 결과를 저장한다.
     *
     * @param note 상담 이력에 남길 문구. 화면이 고객 언어로 만든다.
     */
    data class OnSave(val note: String) : RiskAssessmentActions
}

/** 진단 화면이 한 번씩 처리할 일. */
sealed interface RiskAssessmentEvent {
    /** 저장했다. 진단 화면을 닫는다. */
    data object Saved : RiskAssessmentEvent
}
