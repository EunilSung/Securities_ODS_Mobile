package com.eunilsung.ods.domain.model

/** 호가 한 단계 — 그 가격에 걸린 잔량. */
data class OrderBookLevel(
    val price: Long,
    val quantity: Long,
)

/**
 * 한 종목의 호가.
 *
 * [asks] 는 매도 호가, [bids] 는 매수 호가이고 둘 다 **현재가에 가까운 것부터** 담는다.
 */
data class OrderBook(
    val code: String,
    val asks: List<OrderBookLevel>,
    val bids: List<OrderBookLevel>,
) {
    val totalAskQuantity: Long get() = asks.sumOf { it.quantity }
    val totalBidQuantity: Long get() = bids.sumOf { it.quantity }
}

/** 봉 하나. [epochSeconds] 는 봉이 시작한 시각이다. */
data class Candle(
    val epochSeconds: Long,
    val open: Long,
    val high: Long,
    val low: Long,
    val close: Long,
    val volume: Long,
) {
    /** 시가보다 종가가 높거나 같은 양봉인지. */
    val isRising: Boolean get() = close >= open
}

/** 차트 봉 하나가 묶는 기간. */
enum class ChartPeriod { MINUTE, DAY, WEEK, MONTH }

/** 목록을 얼마나 촘촘하게 그릴지. 사용자가 관심종목 화면에서 고른다. */
enum class ListDensity { COMFORTABLE, COMPACT }
