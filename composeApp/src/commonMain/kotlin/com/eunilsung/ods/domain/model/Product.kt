package com.eunilsung.ods.domain.model

/**
 * 금융상품.
 *
 * 설명 문구([summary]·[keyPoints])는 상품 자료라 받은 그대로 보인다(시연용은 한국어).
 *
 * @param keyPoints 핵심설명서 항목. 가입 전에 고객에게 하나하나 설명하고 확인을 받는다.
 * @param minAmount 최소 가입금액(원).
 * @param isComplex 고난도 금융투자상품(ELS·파생결합증권 등). 고령투자자는 숙려기간을 거친다.
 */
data class Product(
    val id: String,
    val name: String,
    val type: ProductType,
    val riskGrade: ProductRiskGrade,
    val summary: String,
    val expectedReturn: String,
    val feeInfo: String,
    val minAmount: Long,
    val keyPoints: List<String>,
    val isComplex: Boolean,
)

enum class ProductType { FUND, ELS, BOND, RP, ISA }

/** 상품 가입 한 건. */
data class Subscription(
    val id: String,
    val customerId: String,
    val productId: String,
    val productName: String,
    val accountNo: String,
    val amount: Long,
    val subscribedAtMillis: Long,
    /** 성향에 맞지 않는 상품을 고객이 스스로 원해 가입했는지. */
    val unsuitableConfirmed: Boolean,
    /** 숙려기간이 끝나는 시각. 숙려 대상이 아니면 null. 이 시각 전에는 고객이 철회할 수 있다. */
    val coolingOffUntilMillis: Long?,
)

/**
 * 적합성 판정 — 이 고객에게 이 상품을 권유할 수 있는지.
 */
sealed interface Suitability {
    data object Suitable : Suitability

    /** 투자성향이 없거나 1년이 지났다. 진단부터 해야 한다. */
    data object NeedsAssessment : Suitability

    /** 성향보다 위험한 상품이다. 권유할 수 없고, 고객이 스스로 원할 때만 확인을 받고 진행한다. */
    data class Unsuitable(val profile: RiskProfile) : Suitability
}

/** 새 가입. 시각·숙려기간은 유스케이스가 정한다. */
data class NewSubscription(
    val customerId: String,
    val productId: String,
    val accountNo: String,
    val amount: Long,
    val unsuitableConfirmed: Boolean,
)
