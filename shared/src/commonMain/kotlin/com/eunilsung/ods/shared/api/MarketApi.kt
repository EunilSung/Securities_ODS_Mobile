package com.eunilsung.ods.shared.api

import kotlinx.serialization.Serializable

/** `GET /stocks` → 관심종목 시세. */
@Serializable
data class StockDto(
    val code: String,
    val name: String,
    val market: String,
    val price: Long,
    val previousClose: Long,
    val volume: Long,
)

/**
 * 실시간 알림 — `/ws?token=` 으로 받는다. 바꾸는 일은 모두 REST 로 하고, 알림은 "무엇이 바뀌었다" 만 알린다.
 *
 * - [TYPE_QUOTE]: [quote] 종목이 체결됐다. 앱은 기기 DB 시세를 바로 고친다.
 * - [TYPE_CHANGED]: [topic] 의 데이터가 바뀌었다. 앱은 그 목록을 다시 받는다.
 *
 * 다시 붙으면 놓친 알림이 있을 수 있으므로 앱은 목록을 다시 받는다.
 */
@Serializable
data class ServerEvent(
    val type: String,
    val quote: QuoteDto? = null,
    val topic: String? = null,
) {
    companion object {
        const val TYPE_QUOTE = "quote"
        const val TYPE_CHANGED = "changed"

        const val TOPIC_CUSTOMERS = "customers"
        const val TOPIC_VISITS = "visits"
    }
}

@Serializable
data class QuoteDto(val code: String, val price: Long, val volume: Long)
