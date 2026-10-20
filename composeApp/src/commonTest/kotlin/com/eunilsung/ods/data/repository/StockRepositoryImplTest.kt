package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalQuoteRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalStockRepositoryImpl
import com.eunilsung.ods.data.mapper.ServerMarketMapper
import com.eunilsung.ods.data.mapper.StockMapper
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.shared.api.QuoteDto
import com.eunilsung.ods.shared.api.ServerEvent
import com.eunilsung.ods.shared.api.StockDto
import com.eunilsung.ods.testsupport.FakeOdsServer
import com.eunilsung.ods.testsupport.FakeServerEvents
import com.eunilsung.ods.testsupport.awaitUntil
import com.eunilsung.ods.testsupport.createTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 서버 모드 시세 — 받은 시세를 기기 DB 에 쓰고, 체결 알림이 DB 시세와 그 위의 봉을 바꾸는지 본다. */
class StockRepositoryImplTest {
    private val database = createTestDatabase()
    private val server = FakeOdsServer()
    private val events = FakeServerEvents()
    private val repository = StockRepositoryImpl(
        LocalStockRepositoryImpl(database, StockMapper()), database, server, events, StockMapper(), ServerMarketMapper(),
    )

    @Test
    fun quoteEventUpdatesDevicePrice() = runTest {
        server.stocks = listOf(StockDto("005930", "삼성전자", "KOSPI", 70_000, 69_000, 1_000))
        assertTrue(repository.fetchWatchlist())
        assertEquals(70_000, repository.observeStock("005930").first()?.price)

        events.push(ServerEvent(ServerEvent.TYPE_QUOTE, quote = QuoteDto("005930", 70_100, 1_500)))

        awaitUntil { repository.observeStock("005930").first()?.price == 70_100L }
        assertEquals(1_500, repository.observeStock("005930").first()?.volume)
        val quotes = LocalQuoteRepositoryImpl(repository, simulator = null)
        assertEquals(70_100, quotes.observeCandles("005930", ChartPeriod.DAY).first().last().close)
    }
}
