package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.Candle
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.model.OrderBook
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    /** 호가. 보고 있는 동안 실시간으로 바뀐다. */
    fun observeOrderBook(code: String): Flow<OrderBook>

    /** 봉 목록(오래된 것부터). 마지막 봉은 현재가를 따라 바뀐다. */
    fun observeCandles(code: String, period: ChartPeriod): Flow<List<Candle>>
}
