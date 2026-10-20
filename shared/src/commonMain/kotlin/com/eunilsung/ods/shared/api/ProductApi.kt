package com.eunilsung.ods.shared.api

import kotlinx.serialization.Serializable

/** `GET /products` → 목록. */
@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val type: String,
    val riskGrade: String,
    val summary: String,
    val expectedReturn: String,
    val feeInfo: String,
    val minAmount: Long,
    val keyPoints: List<String>,
    val isComplex: Boolean,
)

@Serializable
data class SubscriptionDto(
    val id: String,
    val customerId: String,
    val productId: String,
    val productName: String,
    val accountNo: String,
    val amount: Long,
    val subscribedAtMillis: Long,
    val unsuitableConfirmed: Boolean,
    val coolingOffUntilMillis: Long? = null,
)

/**
 * `POST /subscriptions` → [SubscriptionDto]. 화면이 막은 조건을 서버가 한 번 더 본다 —
 * 409 [ApiErrorCode.NEEDS_ASSESSMENT]·[ApiErrorCode.UNSUITABLE_NOT_CONFIRMED]·[ApiErrorCode.INVALID_AMOUNT]·[ApiErrorCode.ACCOUNT_MISMATCH].
 */
@Serializable
data class NewSubscriptionRequest(
    val customerId: String,
    val productId: String,
    val accountNo: String,
    val amount: Long,
    val unsuitableConfirmed: Boolean,
    val coolingOffUntilMillis: Long? = null,
)
