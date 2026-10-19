package com.eunilsung.ods.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceTest {
    private val samsung = Holding(
        code = "005930", name = "삼성전자", quantity = 50, averagePrice = 68_200,
        price = 71_700, previousClose = 70_800, openSellQuantity = 20,
    )
    private val kakao = Holding(
        code = "035720", name = "카카오", quantity = 120, averagePrice = 47_800,
        price = 43_250, previousClose = 42_100,
    )

    @Test
    fun holdingProfitAndRate() {
        assertEquals(3_410_000, samsung.purchaseAmount)
        assertEquals(3_585_000, samsung.evaluationAmount)
        assertEquals(175_000, samsung.profit)
        assertEquals(5.13, (samsung.profitRate * 100).toLong() / 100.0)
        assertEquals(-546_000, kakao.profit)
        assertEquals(30, samsung.sellableQuantity)
    }

    @Test
    fun summaryAddsCashToEvaluation() {
        val summary = BalanceSummary.of(Account(cash = 10_000_000, buyingPower = 9_000_000), listOf(samsung, kakao))
        assertEquals(3_410_000 + 5_736_000, summary.purchaseAmount)
        assertEquals(3_585_000 + 5_190_000, summary.evaluationAmount)
        assertEquals(-371_000, summary.profit)
        assertEquals(10_000_000 + 8_775_000, summary.totalAsset)
        assertEquals(9_000_000, summary.buyingPower)
    }

    @Test
    fun emptyHoldingsHaveZeroRate() {
        val summary = BalanceSummary.of(Account(cash = 1_000, buyingPower = 1_000), emptyList())
        assertEquals(0.0, summary.profitRate)
        assertEquals(1_000, summary.totalAsset)
    }
}
