package com.eunilsung.ods.ui.quote

import com.eunilsung.ods.domain.model.Candle
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.model.OrderBook
import com.eunilsung.ods.domain.model.Stock

/** 현재가 화면의 상태. */
data class QuoteUiState(
    val code: String = "",
    /** 아직 기기 DB 에 없으면 null. */
    val stock: Stock? = null,
    /** 오늘 일봉 — 머리의 시가·고가·저가에 쓴다. */
    val today: Candle? = null,
    val period: ChartPeriod = ChartPeriod.DAY,
    /** [period] 의 봉(오래된 것부터). */
    val candles: List<Candle> = emptyList(),
    val orderBook: OrderBook? = null,
)
