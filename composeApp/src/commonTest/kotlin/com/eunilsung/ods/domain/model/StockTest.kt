package com.eunilsung.ods.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class StockTest {
    private fun stock(price: Long, previousClose: Long) =
        Stock(code = "000000", name = "T", market = Market.KOSPI, price = price, previousClose = previousClose, volume = 0)

    @Test
    fun directionFollowsChange() {
        assertEquals(PriceDirection.UP, stock(110, 100).direction)
        assertEquals(PriceDirection.DOWN, stock(90, 100).direction)
        assertEquals(PriceDirection.FLAT, stock(100, 100).direction)
    }

    @Test
    fun changeRateIsZeroWithoutPreviousClose() {
        assertEquals(0.0, stock(100, 0).changeRate)
        assertEquals(10.0, stock(110, 100).changeRate)
    }
}
