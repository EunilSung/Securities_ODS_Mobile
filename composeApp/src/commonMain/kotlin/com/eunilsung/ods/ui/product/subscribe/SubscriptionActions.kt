package com.eunilsung.ods.ui.product.subscribe

import com.eunilsung.ods.domain.model.Signature

sealed interface SubscriptionActions {
    /**
     * 가입 절차를 시작했다.
     *
     * @param customerId 상담 중 고객. 있으면 고객 고르기를 건너뛴다.
     */
    data class OnEnter(val productId: String, val customerId: String?) : SubscriptionActions

    data class OnCustomerQueryChange(val query: String) : SubscriptionActions
    data class OnCustomerSelect(val customerId: String) : SubscriptionActions

    data object OnNext : SubscriptionActions
    data object OnPrevious : SubscriptionActions

    /** 투자성향 진단을 눌렀다(성향이 없거나 만료됐을 때). */
    data object OnAssessClick : SubscriptionActions

    /** 부적합 상품을 고객이 스스로 원한다는 확인. */
    data class OnUnsuitableConfirm(val confirmed: Boolean) : SubscriptionActions

    /** 핵심 설명 항목 하나를 설명했다(다시 누르면 푼다). */
    data class OnKeyPointToggle(val index: Int) : SubscriptionActions

    data class OnAccountSelect(val accountNo: String) : SubscriptionActions

    /** 가입금액. 숫자만 받는다. */
    data class OnAmountChange(val text: String) : SubscriptionActions

    /** 금액 −/+ 단추. 십만 원씩. */
    data class OnAmountStep(val up: Boolean) : SubscriptionActions

    data class OnSignatureChange(val signature: Signature) : SubscriptionActions

    /**
     * 가입한다.
     *
     * @param note 상담 이력에 남길 문구. 화면이 고객 언어로 만든다.
     */
    data class OnSubmit(val note: String) : SubscriptionActions
}

/** 가입 절차가 한 번씩 처리할 일. */
sealed interface SubscriptionEvent {
    data class OpenRiskAssessment(val customerId: String) : SubscriptionEvent
}
