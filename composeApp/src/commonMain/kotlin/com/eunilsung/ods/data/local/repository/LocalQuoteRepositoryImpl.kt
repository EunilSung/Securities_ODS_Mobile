package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.data.local.seed.DemoMarketData
import com.eunilsung.ods.domain.model.Candle
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.model.OrderBook
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.domain.repository.QuoteRepository
import com.eunilsung.ods.domain.repository.StockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant
import kotlin.time.Clock

/**
 * 서버 없이 도는 현재가 저장소 — 기기 DB 시세를 바탕으로 시연용 호가·봉을 만든다.
 *
 * 호가를 보는 동안 [LocalTickSimulator] 가 시세를 움직이므로, 호가·봉·관심종목이 함께 실시간처럼 바뀐다.
 * 서버 모드에서는 [simulator] 없이 쓴다 — 서버 체결 알림이 기기 DB 시세를 바꾸고, 호가·봉은 그 값을 따라간다.
 */
class LocalQuoteRepositoryImpl(
    private val stockRepository: StockRepository,
    private val simulator: LocalTickSimulator?,
) : QuoteRepository {

    private val candleLock = Mutex()
    private val candleCache = mutableMapOf<Pair<String, ChartPeriod>, List<Candle>>()

    override fun observeOrderBook(code: String): Flow<OrderBook> = channelFlow {
        simulator?.let { launch { it.watch(code) } }
        stockRepository.observeStock(code).filterNotNull().collect { send(DemoMarketData.orderBook(it)) }
    }.flowOn(Dispatchers.Default)

    override fun observeCandles(code: String, period: ChartPeriod): Flow<List<Candle>> =
        stockRepository.observeStock(code)
            .filterNotNull()
            .map { stock ->
                val base = candleLock.withLock {
                    candleCache.getOrPut(code to period) { DemoMarketData.candles(stock, period, now()) }
                }
                base.followPrice(stock, period)
            }
            .flowOn(Dispatchers.Default)

    private fun now(): Instant = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())

    /** 마지막 봉을 현재가에 맞춘다 — 종가는 현재가, 고가·저가는 현재가를 넘으면 늘린다. */
    private fun List<Candle>.followPrice(stock: Stock, period: ChartPeriod): List<Candle> {
        val last = lastOrNull() ?: return this
        val updated = last.copy(
            close = stock.price,
            high = maxOf(last.high, stock.price),
            low = minOf(last.low, stock.price),
            volume = if (period == ChartPeriod.DAY) stock.volume else last.volume,
        )
        return dropLast(1) + updated
    }
}
