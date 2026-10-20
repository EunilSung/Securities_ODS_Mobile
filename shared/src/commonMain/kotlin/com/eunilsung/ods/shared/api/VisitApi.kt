package com.eunilsung.ods.shared.api

import kotlinx.serialization.Serializable

/** `GET /visits` → 로그인한 직원의 방문 일정 전부. 다른 직원의 일정은 없는 것과 같다. */
@Serializable
data class VisitDto(
    val id: String,
    val customerId: String,
    val customerName: String,
    val scheduledAtMillis: Long,
    val place: String,
    val purpose: String,
    val memo: String,
    val status: String,
)

/** `POST /visits` → [VisitDto]. */
@Serializable
data class NewVisitRequest(
    val customerId: String,
    val scheduledAtMillis: Long,
    val place: String,
    val purpose: String,
    val memo: String,
)

/** `PUT /visits/{id}/status` → 204. */
@Serializable
data class VisitStatusRequest(val status: String)
