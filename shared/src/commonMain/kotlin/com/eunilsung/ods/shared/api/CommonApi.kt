package com.eunilsung.ods.shared.api

import kotlinx.serialization.Serializable

/**
 * 서버가 요청을 거절할 때의 본문. [code] 는 [ApiErrorCode] 가운데 하나다.
 *
 * @param remainingAttempts 로그인이 틀렸을 때 잠기기까지 남은 횟수.
 * @param lockedUntilEpochSeconds 로그인이 잠겼을 때 풀리는 시각.
 */
@Serializable
data class ApiError(
    val code: String,
    val message: String = "",
    val remainingAttempts: Int? = null,
    val lockedUntilEpochSeconds: Long? = null,
)

/** 거절 까닭. 앱은 이 값으로 화면 안내를 고른다. */
object ApiErrorCode {
    const val INVALID_CREDENTIALS = "invalid_credentials"
    const val TOO_MANY_ATTEMPTS = "too_many_attempts"
    const val UNAUTHORIZED = "unauthorized"
    const val BAD_REQUEST = "bad_request"
    const val NOT_FOUND = "not_found"
    const val INTERNAL_ERROR = "internal_error"

    /** 계좌 개설 — ISA 나이 미달, ISA·연금저축 중복. */
    const val ISA_AGE = "isa_age"
    const val ISA_DUPLICATE = "isa_duplicate"
    const val PENSION_DUPLICATE = "pension_duplicate"

    /** 상품 가입 — 투자성향 없음·만료, 부적합 확인 없음, 금액 미달·단위 어긋남, 고객의 계좌가 아님. */
    const val NEEDS_ASSESSMENT = "needs_assessment"
    const val UNSUITABLE_NOT_CONFIRMED = "unsuitable_not_confirmed"
    const val INVALID_AMOUNT = "invalid_amount"
    const val ACCOUNT_MISMATCH = "account_mismatch"
}

@Serializable
data class HealthResponse(val status: String)
