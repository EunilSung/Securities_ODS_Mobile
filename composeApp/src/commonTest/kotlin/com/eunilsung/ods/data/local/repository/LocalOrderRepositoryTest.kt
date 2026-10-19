package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.data.local.seed.AccountSeed
import com.eunilsung.ods.data.mapper.OrderMapper
import com.eunilsung.ods.data.mapper.StockMapper
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.db.HoldingEntity
import com.eunilsung.ods.db.WatchStockEntity
import com.eunilsung.ods.domain.model.OrderChangeRejectReason
import com.eunilsung.ods.domain.model.OrderChangeResult
import com.eunilsung.ods.domain.model.OrderDraft
import com.eunilsung.ods.domain.model.OrderRejectReason
import com.eunilsung.ods.domain.model.OrderResult
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.testsupport.createTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class LocalOrderRepositoryTest {
    private fun setUp(): Pair<AppDatabase, LocalOrderRepositoryImpl> {
        val database = createTestDatabase()
        database.appDatabaseQueries.insertStockIfAbsent(
            WatchStockEntity("005930", "삼성전자", "KOSPI", 71_500, 70_800, 1_000, 0)
        )
        database.appDatabaseQueries.insertHoldingIfAbsent(HoldingEntity("005930", 50, 68_200))
        val repository = LocalOrderRepositoryImpl(database, OrderMapper(StockMapper()), simulatedRoundTripMillis = 0)
        return database to repository
    }

    private fun buy(price: Long, quantity: Long) = OrderDraft("005930", OrderSide.BUY, OrderType.LIMIT, price, quantity)

    @Test
    fun acceptedOrdersGetIncreasingNumbers() = runTest {
        val (_, repository) = setUp()
        val first = assertIs<OrderResult.Accepted>(repository.submit(buy(71_500, 10))).order
        val second = assertIs<OrderResult.Accepted>(repository.submit(buy(71_400, 5))).order
        assertEquals("000001", first.orderNo)
        assertEquals("000002", second.orderNo)
        assertEquals(listOf("000002", "000001"), repository.observeOpenOrders().first().map { it.orderNo })
    }

    @Test
    fun buyBeyondCashIsRejected() = runTest {
        val (_, repository) = setUp()
        val result = repository.submit(buy(71_500, AccountSeed.CASH / 71_500 + 1))
        assertEquals(OrderResult.Rejected(OrderRejectReason.NOT_ENOUGH_BUYING_POWER), result)
    }

    @Test
    fun sellBeyondHoldingCountsOpenSells() = runTest {
        val (_, repository) = setUp()
        val sell = OrderDraft("005930", OrderSide.SELL, OrderType.LIMIT, 72_000, 30)
        assertIs<OrderResult.Accepted>(repository.submit(sell))
        assertEquals(OrderResult.Rejected(OrderRejectReason.NOT_ENOUGH_HOLDING), repository.submit(sell))
    }

    @Test
    fun amendIssuesNewNumberAndKeepsOriginal() = runTest {
        val (database, repository) = setUp()
        val original = assertIs<OrderResult.Accepted>(repository.submit(buy(71_500, 10))).order
        val amended = assertIs<OrderChangeResult.Done>(repository.amend(original.orderNo, 71_000)).order!!
        assertEquals("000002", amended.orderNo)
        assertEquals(original.orderNo, amended.originalOrderNo)
        assertEquals(71_000, amended.price)
        assertEquals(listOf(amended.orderNo), repository.observeOpenOrders().first().map { it.orderNo })
        assertEquals(710_000, database.appDatabaseQueries.sumReservedBuyAmount().executeAsOne())
    }

    @Test
    fun amendUpCountsOnlyTheExtraAmount() = runTest {
        val (_, repository) = setUp()
        val quantity = AccountSeed.CASH / 71_500
        val order = assertIs<OrderResult.Accepted>(repository.submit(buy(71_500, quantity))).order
        val leftover = AccountSeed.CASH - 71_500 * quantity
        val affordable = 71_500 + leftover / quantity / 100 * 100
        assertIs<OrderChangeResult.Done>(repository.amend(order.orderNo, affordable))
        val latest = repository.observeOpenOrders().first().single()
        assertEquals(
            OrderChangeResult.Rejected(OrderChangeRejectReason.NOT_ENOUGH_BUYING_POWER),
            repository.amend(latest.orderNo, affordable + 100),
        )
    }

    @Test
    fun cancelRemovesOrderAndFreesCash() = runTest {
        val (database, repository) = setUp()
        val order = assertIs<OrderResult.Accepted>(repository.submit(buy(71_500, 10))).order
        val result = assertIs<OrderChangeResult.Done>(repository.cancel(order.orderNo))
        assertNull(result.order)
        assertEquals(emptyList(), repository.observeOpenOrders().first())
        assertEquals(0, database.appDatabaseQueries.sumReservedBuyAmount().executeAsOne())
    }

    @Test
    fun changingAGoneOrderIsRejected() = runTest {
        val (_, repository) = setUp()
        val order = assertIs<OrderResult.Accepted>(repository.submit(buy(71_500, 10))).order
        repository.cancel(order.orderNo)
        assertEquals(OrderChangeResult.Rejected(OrderChangeRejectReason.NOT_OPEN), repository.cancel(order.orderNo))
        assertEquals(OrderChangeResult.Rejected(OrderChangeRejectReason.NOT_OPEN), repository.amend(order.orderNo, 71_000))
    }

    @Test
    fun cancelledNumbersAreNotReused() = runTest {
        val (_, repository) = setUp()
        val first = assertIs<OrderResult.Accepted>(repository.submit(buy(71_500, 1))).order
        repository.cancel(first.orderNo)
        val next = assertIs<OrderResult.Accepted>(repository.submit(buy(71_500, 1))).order
        assertEquals("000002", next.orderNo)
    }
}
