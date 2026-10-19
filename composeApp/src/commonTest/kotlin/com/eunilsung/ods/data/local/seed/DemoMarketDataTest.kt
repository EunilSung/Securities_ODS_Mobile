package com.eunilsung.ods.data.local.seed

import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.model.Market
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.domain.util.floorToTick
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoMarketDataTest {
    private val stock = Stock(
        code = "005930", name = "삼성전자", market = Market.KOSPI,
        price = 71_500, previousClose = 70_800, volume = 12_345_678,
    )
    private val now = Instant.parse("2026-10-19T05:30:00Z")

    @Test
    fun orderBookSurroundsCurrentPriceWithinLimits() {
        val book = DemoMarketData.orderBook(stock)
        assertEquals(10, book.asks.size)
        assertEquals(10, book.bids.size)
        assertEquals(71_600, book.asks.first().price)
        assertEquals(71_500, book.bids.first().price)
        assertTrue(book.asks.zipWithNext().all { (near, far) -> far.price > near.price })
        assertTrue(book.bids.zipWithNext().all { (near, far) -> far.price < near.price })
        assertTrue((book.asks + book.bids).all { it.quantity > 0 })
    }

    @Test
    fun orderBookStopsAtUpperLimit() {
        val nearLimit = stock.copy(price = 91_800)
        val book = DemoMarketData.orderBook(nearLimit)
        assertEquals(listOf(91_900L, 92_000L), book.asks.map { it.price })
    }

    @Test
    fun sameQuoteGivesSameOrderBook() {
        assertEquals(DemoMarketData.orderBook(stock), DemoMarketData.orderBook(stock))
    }

    @Test
    fun candlesEndAtCurrentPriceAndStayConsistent() {
        for (period in ChartPeriod.entries) {
            val candles = DemoMarketData.candles(stock, period, now)
            assertEquals(120, candles.size)
            assertEquals(stock.price, candles.last().close, "$period")
            assertTrue(candles.zipWithNext().all { (a, b) -> a.epochSeconds < b.epochSeconds }, "$period times")
            candles.forEach { candle ->
                assertTrue(candle.high >= maxOf(candle.open, candle.close), "$period high")
                assertTrue(candle.low <= minOf(candle.open, candle.close), "$period low")
                assertEquals(floorToTick(candle.open), candle.open, "$period open on tick")
            }
        }
    }

    @Test
    fun dailyCandlesMatchPreviousClose() {
        val candles = DemoMarketData.candles(stock, ChartPeriod.DAY, now)
        assertEquals(stock.previousClose, candles[candles.size - 2].close)
        assertEquals(stock.volume, candles.last().volume)
    }
}
