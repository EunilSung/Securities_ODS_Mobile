package com.eunilsung.ods.shared.api

import kotlinx.serialization.Serializable

/**
 * `GET /customers` — 고객과 그 고객에 딸린 기록 전부. 앱은 받은 그대로 기기 DB 를 맞춘다.
 *
 * 열거형은 앱 도메인 이름(`Gender.MALE` → `"MALE"`)을 글자로 싣는다. 시각은 epoch 밀리초, 날짜는 `yyyy-MM-dd`.
 */
@Serializable
data class CustomerBookResponse(
    val customers: List<CustomerDto>,
    val accounts: List<AccountDto>,
    val consultations: List<ConsultationDto>,
    val subscriptions: List<SubscriptionDto>,
    val signatures: List<SignatureRecordDto>,
)

@Serializable
data class CustomerDto(
    val id: String,
    val name: String,
    val birthDate: String,
    val gender: String,
    val phone: String,
    val address: String,
    val grade: String,
    val riskProfile: String? = null,
    val riskAssessedAtMillis: Long? = null,
    val registeredAtMillis: Long,
)

/** `POST /customers` → [CustomerDto]. 등급은 서버가 일반으로 정한다. */
@Serializable
data class NewCustomerRequest(
    val name: String,
    val birthDate: String,
    val gender: String,
    val phone: String,
    val address: String,
)

@Serializable
data class AccountDto(
    val accountNo: String,
    val customerId: String,
    val type: String,
    val openedAtMillis: Long,
)

/**
 * `POST /customers/{id}/accounts` → [AccountDto]. 계좌번호는 서버가 발급한다.
 * 열 수 없으면 409 [ApiErrorCode.ISA_AGE]·[ApiErrorCode.ISA_DUPLICATE]·[ApiErrorCode.PENSION_DUPLICATE].
 */
@Serializable
data class OpenAccountRequest(val type: String)

/** `PUT /customers/{id}/risk-profile` → 204. */
@Serializable
data class RiskProfileRequest(val profile: String, val assessedAtMillis: Long)

@Serializable
data class ConsultationDto(
    val id: String,
    val customerId: String,
    val consultedAtMillis: Long,
    val channel: String,
    val topic: String,
    val content: String,
    val staffName: String,
)

/** `POST /customers/{id}/consultations` → [ConsultationDto]. 시각과 직원 이름은 서버가 토큰으로 채운다. */
@Serializable
data class NewConsultationRequest(
    val channel: String,
    val topic: String,
    val content: String,
)

/** 서명 한 점. 서명 칸 크기에 대한 비율(0~1). */
@Serializable
data class SignaturePointDto(val x: Float, val y: Float)

/**
 * `POST /customers/{id}/signatures` → [SignatureRecordDto].
 * 서명 그림은 서버에만 남는다. 앱은 돌려받은 기록(지문)만 기기에 둔다.
 */
@Serializable
data class SignatureRequest(
    val purpose: String,
    val signedAtMillis: Long,
    val strokes: List<List<SignaturePointDto>>,
)

@Serializable
data class SignatureRecordDto(
    val id: String,
    val customerId: String,
    val purpose: String,
    val signedAtMillis: Long,
    val digest: String,
)

/**
 * `POST /id-verifications` → [IdVerificationResponse]. 서버가 발급기관에 진위를 묻는다(로컬 서버는 흉내).
 * 주민등록번호는 앞 6자리와 뒷자리 첫 숫자만 보낸다. 서버도 남기지 않는다.
 */
@Serializable
data class IdVerificationRequest(
    val type: String,
    val name: String,
    val rrnFront: String,
    val rrnBackFirst: String,
)

/** @param verified 거짓이면 발급기관 기록과 맞지 않는다(분실 신고·재발급 등). */
@Serializable
data class IdVerificationResponse(val verified: Boolean, val verifiedAtMillis: Long)
