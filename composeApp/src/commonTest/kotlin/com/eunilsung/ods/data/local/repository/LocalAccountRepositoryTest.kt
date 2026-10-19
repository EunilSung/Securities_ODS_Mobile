package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.data.local.seed.AccountSeed
import com.eunilsung.ods.data.mapper.HoldingMapper
import com.eunilsung.ods.db.HoldingEntity
import com.eunilsung.ods.db.OpenOrderEntity
import com.eunilsung.ods.db.WatchStockEntity
import com.eunilsung.ods.testsupport.createTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalAccountRepositoryTest {
    @Test
    fun holdingsJoinPricesAndOpenSells() = runTest {
        val database = createTestDatabase()
        val queries = database.appDatabaseQueries
        queries.insertStockIfAbsent(WatchStockEntity("005930", "삼성전자", "KOSPI", 71_500, 70_800, 0, 0))
        queries.insertHoldingIfAbsent(HoldingEntity("005930", 50, 68_200))
        queries.insertHoldingIfAbsent(HoldingEntity("999999", 3, 10_000))
        queries.insertOpenOrder(OpenOrderEntity("000001", "005930", "삼성전자", "SELL", "LIMIT", 72_000, 20, 0, 0, null))
        queries.insertOpenOrder(OpenOrderEntity("000002", "005930", "삼성전자", "BUY", "LIMIT", 71_000, 10, 710_000, 0, null))
        val repository = LocalAccountRepositoryImpl(database, HoldingMapper())

        val holdings = repository.observeHoldings().first()

        assertEquals(listOf("005930", "999999"), holdings.map { it.code })
        val samsung = holdings[0]
        assertEquals("삼성전자", samsung.name)
        assertEquals(71_500, samsung.price)
        assertEquals(20, samsung.openSellQuantity)
        assertEquals(30, samsung.sellableQuantity)
        val unknown = holdings[1]
        assertEquals("999999", unknown.name)
        assertEquals(10_000, unknown.price)
        assertEquals(0, unknown.profit)

        val account = repository.observeAccount().first()
        assertEquals(AccountSeed.CASH, account.cash)
        assertEquals(AccountSeed.CASH - 710_000, account.buyingPower)
    }
}
