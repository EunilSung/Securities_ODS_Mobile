package com.eunilsung.ods.domain.model

import kotlinx.datetime.LocalDate

/**
 * 고객.
 *
 * @param phone 휴대폰 번호(하이픈 포함). 화면에는 개인정보 가리기 설정에 따라 가려서 보인다.
 * @param riskProfile 투자성향. 진단한 적이 없으면 null.
 * @param riskAssessedAtMillis 투자성향을 진단한 시각. 유효기간을 따지는 데 쓴다.
 */
data class Customer(
    val id: String,
    val name: String,
    val birthDate: LocalDate,
    val gender: Gender,
    val phone: String,
    val address: String,
    val grade: CustomerGrade,
    val riskProfile: RiskProfile?,
    val riskAssessedAtMillis: Long?,
    val registeredAtMillis: Long,
)

enum class Gender { MALE, FEMALE }

/** 고객 등급. 자산 규모로 나눈다. */
enum class CustomerGrade { VIP, GOLD, GENERAL }

/**
 * 투자성향 — 금융투자협회 표준 투자자정보확인서의 다섯 단계. 위험을 덜 감수하는 것부터 놓는다.
 *
 * [maxProductRisk] 는 이 성향에 권유할 수 있는 가장 위험한 상품 등급이다(1등급이 가장 위험하다).
 */
enum class RiskProfile(val maxProductRisk: ProductRiskGrade) {
    /** 안정형 — 원금 손실을 원하지 않는다. */
    STABLE(ProductRiskGrade.VERY_LOW),

    /** 안정추구형 — 원금 손실을 최소화한다. */
    STABLE_SEEKING(ProductRiskGrade.LOW),

    /** 위험중립형 — 손실 위험을 알고 시장 수익률 수준을 기대한다. */
    NEUTRAL(ProductRiskGrade.MODERATE),

    /** 적극투자형 — 손실 위험을 감수하고 높은 수익을 기대한다. */
    ACTIVE(ProductRiskGrade.HIGH),

    /** 공격투자형 — 큰 손실도 감수하고 시장보다 훨씬 높은 수익을 노린다. */
    AGGRESSIVE(ProductRiskGrade.VERY_HIGH),
}

/** 상품 위험등급. 숫자가 작을수록 위험하다(1등급 매우 높은 위험 ~ 6등급 매우 낮은 위험). */
enum class ProductRiskGrade(val level: Int) {
    VERY_HIGH(1),
    HIGH(2),
    MODERATE(3),
    LOW(4),
    VERY_LOW(5),
    MINIMAL(6),
}

/** 상담 한 건. */
data class Consultation(
    val id: String,
    val customerId: String,
    val consultedAtMillis: Long,
    val channel: ConsultChannel,
    val topic: ConsultTopic,
    val content: String,
    val staffName: String,
)

/** 어디서 만났는지. */
enum class ConsultChannel { VISIT, PHONE, BRANCH }

/** 무엇을 상담했는지. 업무가 끝나면 앱이 자동으로 남기는 기록도 같은 갈래를 쓴다. */
enum class ConsultTopic { GENERAL, MARKET, ACCOUNT_OPENING, RISK_PROFILE, PRODUCT }

/** 고객 계좌. */
data class Account(
    val accountNo: String,
    val customerId: String,
    val type: AccountType,
    val openedAtMillis: Long,
)

/** 계좌 종류. */
enum class AccountType { BROKERAGE, CMA, ISA, PENSION }

/** 새 고객을 등록할 때 받는 값. */
data class NewCustomer(
    val name: String,
    val birthDate: LocalDate,
    val gender: Gender,
    val phone: String,
    val address: String,
)

/** 새 상담 기록. 시각과 직원은 저장소가 채운다. */
data class NewConsultation(
    val customerId: String,
    val channel: ConsultChannel,
    val topic: ConsultTopic,
    val content: String,
)
