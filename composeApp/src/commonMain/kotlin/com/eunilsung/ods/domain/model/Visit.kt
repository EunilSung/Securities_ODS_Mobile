package com.eunilsung.ods.domain.model

/**
 * 고객 방문 일정.
 *
 * @param customerName 목록에 바로 보이도록 고객 이름을 함께 싣는다.
 * @param scheduledAtMillis 만나기로 한 시각.
 */
data class Visit(
    val id: String,
    val customerId: String,
    val customerName: String,
    val scheduledAtMillis: Long,
    val place: String,
    val purpose: VisitPurpose,
    val memo: String,
    val status: VisitStatus,
)

/** 무엇 하러 가는지. 방문을 마치고 상담을 기록할 때 주제로 이어진다. */
enum class VisitPurpose(val topic: ConsultTopic) {
    CONSULT(ConsultTopic.GENERAL),
    ACCOUNT_OPENING(ConsultTopic.ACCOUNT_OPENING),
    PRODUCT(ConsultTopic.PRODUCT),
    RISK_PROFILE(ConsultTopic.RISK_PROFILE),
}

enum class VisitStatus { SCHEDULED, COMPLETED, CANCELED }

/** 일정 달력을 한 주로 볼지, 한 달로 볼지. */
enum class CalendarMode { WEEK, MONTH }

/** 새 방문 일정. */
data class NewVisit(
    val customerId: String,
    val scheduledAtMillis: Long,
    val place: String,
    val purpose: VisitPurpose,
    val memo: String,
)
