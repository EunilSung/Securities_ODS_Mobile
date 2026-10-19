package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.ProductRiskGrade
import com.eunilsung.ods.domain.model.Suitability
import kotlinx.datetime.LocalDate

/** 상품 가입 규칙. */
object SubscriptionPolicy {
    /** 가입금액 단위(원). */
    const val AMOUNT_UNIT = 10_000L

    /** 고령투자자가 고난도·고위험 상품에 가입하면 이 기간(일) 동안 숙려한다. */
    const val COOLING_OFF_DAYS = 2

    /** 이 등급보다 위험하면(1·2등급) 고위험으로 본다. */
    val HIGH_RISK_FLOOR = ProductRiskGrade.HIGH

    fun isValidAmount(amount: Long, product: Product): Boolean = amount >= product.minAmount && amount % AMOUNT_UNIT == 0L

    /** 숙려기간을 거쳐야 하는지 — 고령투자자가 고난도 상품이나 1·2등급 상품에 가입할 때. */
    fun needsCoolingOff(customer: Customer, product: Product, today: LocalDate): Boolean =
        customer.ageOn(today) >= RiskAssessmentPolicy.ELDERLY_AGE &&
            (product.isComplex || product.riskGrade.level <= HIGH_RISK_FLOOR.level)
}

/** 적합성 판정. 성향이 없거나 만료됐으면 진단부터, 성향보다 위험하면 부적합. */
fun Customer.suitabilityFor(product: Product, today: LocalDate): Suitability {
    val profile = riskProfile
    if (profile == null || !hasValidRiskProfile(today)) return Suitability.NeedsAssessment
    return if (profile.allows(product.riskGrade)) Suitability.Suitable else Suitability.Unsuitable(profile)
}
