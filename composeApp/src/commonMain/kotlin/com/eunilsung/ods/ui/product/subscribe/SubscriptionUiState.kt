package com.eunilsung.ods.ui.product.subscribe

import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.Subscription
import com.eunilsung.ods.domain.model.Suitability
import com.eunilsung.ods.domain.util.SignaturePolicy
import com.eunilsung.ods.domain.util.SubscriptionPolicy

/** 가입 절차의 단계. */
enum class SubscriptionStep { CUSTOMER, SUITABILITY, EXPLANATION, DETAILS, SIGN, DONE }

/** 가입 절차의 상태. */
data class SubscriptionUiState(
    val step: SubscriptionStep = SubscriptionStep.CUSTOMER,
    val product: Product? = null,
    val customer: Customer? = null,
    val customerQuery: String = "",
    val customerMatches: List<Customer> = emptyList(),
    val suitability: Suitability? = null,
    val unsuitableConfirmed: Boolean = false,
    /** 설명을 마친 핵심 설명 항목 번호. */
    val explainedPoints: Set<Int> = emptySet(),
    val accounts: List<Account> = emptyList(),
    val accountNo: String? = null,
    /** 가입금액(숫자만). */
    val amountText: String = "",
    val signature: Signature = Signature(emptyList()),
    /** 숙려기간 대상인지(고령투자자 · 고난도·고위험 상품). */
    val needsCoolingOff: Boolean = false,
    val isSaving: Boolean = false,
    val error: SubscribeError? = null,
    /** 가입을 마쳤다. */
    val result: Subscription? = null,
) {
    val amount: Long get() = amountText.toLongOrNull() ?: 0L

    val amountValid: Boolean get() = product?.let { SubscriptionPolicy.isValidAmount(amount, it) } ?: false

    val canProceed: Boolean
        get() = when (step) {
            SubscriptionStep.CUSTOMER -> customer != null
            SubscriptionStep.SUITABILITY -> when (suitability) {
                Suitability.Suitable -> true
                is Suitability.Unsuitable -> unsuitableConfirmed
                else -> false
            }
            SubscriptionStep.EXPLANATION -> product != null && explainedPoints.size == product.keyPoints.size
            SubscriptionStep.DETAILS -> accountNo != null && amountValid
            SubscriptionStep.SIGN -> SignaturePolicy.isEnough(signature) && !isSaving
            SubscriptionStep.DONE -> false
        }
}

enum class SubscribeError { NEEDS_ASSESSMENT, UNSUITABLE, INVALID_AMOUNT, INVALID_ACCOUNT, SIGNATURE, NOT_SIGNED_IN, NOT_FOUND, UNREACHABLE }
