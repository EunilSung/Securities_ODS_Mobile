package com.eunilsung.ods.domain.model

/**
 * 투자자정보 확인 문항 — 금융투자협회 표준 투자자정보확인서를 줄여 옮긴 일곱 문항.
 *
 * 선택지마다 점수가 있고, 모든 문항은 하나만 고른다. 투자 경험은 가장 위험한 경험 하나를 고른다.
 */
enum class RiskQuestion(val scores: List<Int>) {
    /** 나이. 고객 생년월일로 미리 고른다. */
    AGE(listOf(5, 4, 3, 2, 1)),

    /** 투자할 수 있는 기간. */
    INVESTMENT_PERIOD(listOf(1, 2, 3, 4, 5)),

    /** 가장 위험했던 투자 경험. */
    EXPERIENCE(listOf(1, 2, 3, 4, 5)),

    /** 금융상품을 얼마나 이해하는지. */
    KNOWLEDGE(listOf(1, 2, 3, 4)),

    /** 감수할 수 있는 손실. */
    LOSS_TOLERANCE(listOf(1, 2, 3, 4)),

    /** 수입이 앞으로 어떨지. */
    INCOME(listOf(3, 2, 1)),

    /** 총자산 가운데 금융자산 비중. 낮을수록 손실을 견딜 여유가 있다. */
    ASSET_RATIO(listOf(4, 3, 2, 1)),
}

/** 진단 결과. */
data class RiskAssessmentResult(
    val profile: RiskProfile,
    /** 0~100 으로 바꾼 점수. */
    val score: Int,
    /** 고령투자자(65세 이상) — 투자권유 때 보호 절차(설명 녹취·숙려기간)를 거쳐야 한다. */
    val isElderly: Boolean,
)

/** 서명 한 획의 점. 서명 칸 크기에 대한 비율(0~1)로 둔다. */
data class SignaturePoint(val x: Float, val y: Float)

/** 고객이 서명 칸에 그린 서명. 획마다 점 목록이다. */
data class Signature(val strokes: List<List<SignaturePoint>>) {
    val pointCount: Int get() = strokes.sumOf { it.size }
}

/** 무엇에 대한 서명인지. */
enum class SignaturePurpose { RISK_PROFILE, ACCOUNT_OPENING, PRODUCT_SUBSCRIPTION }

/**
 * 남겨 둔 서명 기록. 서명 그림은 남기지 않고 지문(해시)만 남긴다 — 단말을 잃어버려도 서명이 새지 않게.
 * 서버 모드에서는 서명 그림을 서버로 보내고 이 기록만 기기에 둔다.
 */
data class SignatureRecord(
    val id: String,
    val customerId: String,
    val purpose: SignaturePurpose,
    val signedAtMillis: Long,
    val digest: String,
)
