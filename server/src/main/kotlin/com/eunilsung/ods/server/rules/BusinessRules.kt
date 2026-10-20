package com.eunilsung.ods.server.rules

import com.eunilsung.ods.shared.api.SignaturePointDto
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

/**
 * 앱이 화면에서 막은 업무 규칙을 서버가 저장 직전에 한 번 더 본다.
 *
 * 앱 `domain/util` 의 `CustomerRules`·`ProductRules`·`AccountOpeningRules`·`RiskRules` 와 같은 답을 내야 한다.
 * 한쪽만 고치면 화면은 통과시키고 서버는 거절하는 일이 생긴다.
 */
object BusinessRules {

    /** 업무 날짜는 한국 시간으로 따진다. */
    val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")

    const val ISA_MIN_AGE = 19
    const val AMOUNT_UNIT = 10_000L
    const val RISK_PROFILE_VALID_YEARS = 1

    /** 시연용 지점 코드 — 계좌번호 앞 4자리. */
    const val BRANCH_CODE = "7001"

    fun todayInSeoul(nowMillis: Long = System.currentTimeMillis()): LocalDate =
        Instant.ofEpochMilli(nowMillis).atZone(SEOUL).toLocalDate()

    /** 만 나이. */
    fun ageOn(birthDate: LocalDate, today: LocalDate): Int = Period.between(birthDate, today).years

    /** 투자성향이 지금 유효한지 — 진단한 날(한국 날짜)부터 1년이 지나지 않았다. */
    fun hasValidRiskProfile(profile: String?, assessedAtMillis: Long?, today: LocalDate): Boolean {
        if (profile == null || assessedAtMillis == null) return false
        return today < todayInSeoul(assessedAtMillis).plusYears(RISK_PROFILE_VALID_YEARS.toLong())
    }

    /** 상품 위험등급의 숫자. 작을수록 위험하다(1등급 매우 높은 위험 ~ 6등급 매우 낮은 위험). */
    private val RISK_GRADE_LEVEL = mapOf(
        "VERY_HIGH" to 1, "HIGH" to 2, "MODERATE" to 3, "LOW" to 4, "VERY_LOW" to 5, "MINIMAL" to 6,
    )

    /** 투자성향마다 권유할 수 있는 가장 위험한 등급의 숫자. */
    private val PROFILE_MAX_LEVEL = mapOf(
        "STABLE" to 5, "STABLE_SEEKING" to 4, "NEUTRAL" to 3, "ACTIVE" to 2, "AGGRESSIVE" to 1,
    )

    /** 이 성향에 이 등급의 상품을 권유할 수 있는지. */
    fun allows(profile: String, riskGrade: String): Boolean {
        val level = RISK_GRADE_LEVEL.getValue(riskGrade)
        return level >= PROFILE_MAX_LEVEL.getValue(profile)
    }

    fun isValidAmount(amount: Long, minAmount: Long): Boolean = amount >= minAmount && amount % AMOUNT_UNIT == 0L

    /** 계좌번호 앞부분 `7001-<휴대폰 가운데 자리 끝 4자리>-`. 가운데 자리가 없으면 `0000`. */
    fun accountPrefix(phone: String): String {
        val middle = phone.split("-").getOrNull(1)?.takeLast(4)?.padStart(4, '0') ?: "0000"
        return "$BRANCH_CODE-$middle-"
    }

    /** 서명 지문 — 앱 `LocalSignatureRepositoryImpl` 과 같이 점을 글자로 늘어놓아 SHA-256 한 소문자 hex. */
    fun signatureDigest(strokes: List<List<SignaturePointDto>>): String {
        val text = strokes.joinToString("|") { stroke -> stroke.joinToString(";") { "${it.x},${it.y}" } }
        return MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

/** 요청으로 받는 열거형 값. 앱 도메인 enum 이름과 같다. */
object Enums {
    val GENDER = setOf("MALE", "FEMALE")
    val ACCOUNT_TYPE = setOf("BROKERAGE", "CMA", "ISA", "PENSION")
    val RISK_PROFILE = setOf("STABLE", "STABLE_SEEKING", "NEUTRAL", "ACTIVE", "AGGRESSIVE")
    val CONSULT_CHANNEL = setOf("VISIT", "PHONE", "BRANCH")
    val CONSULT_TOPIC = setOf("GENERAL", "MARKET", "ACCOUNT_OPENING", "RISK_PROFILE", "PRODUCT")
    val SIGNATURE_PURPOSE = setOf("RISK_PROFILE", "ACCOUNT_OPENING", "PRODUCT_SUBSCRIPTION")
    val VISIT_PURPOSE = setOf("CONSULT", "ACCOUNT_OPENING", "PRODUCT", "RISK_PROFILE")
    val VISIT_STATUS = setOf("SCHEDULED", "COMPLETED", "CANCELED")
    val ID_CARD_TYPE = setOf("RESIDENT", "DRIVER")
}
