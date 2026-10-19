package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.Market
import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderDraft
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.model.Stock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderValidationTest {
    private val stock = Stock(
        code = "005930", name = "삼성전자", market = Market.KOSPI,
        price = 71_500, previousClose = 70_800, volume = 0,
    )

    private fun draft(
        side: OrderSide = OrderSide.BUY,
        type: OrderType = OrderType.LIMIT,
        price: Long = 71_500,
        quantity: Long = 10,
    ) = OrderDraft(stock.code, side, type, price, quantity)

    @Test
    fun validLimitBuyPasses() {
        val check = checkOrder(draft(), stock, buyingPower = 10_000_000, sellableQuantity = 0)
        assertTrue(check.canSubmit)
        assertEquals(emptyList(), check.warnings)
    }

    @Test
    fun missingInputBlocksSubmit() {
        val check = checkOrder(draft(price = 0, quantity = 0), stock, 10_000_000, 0)
        assertEquals(listOf(OrderIssue.QuantityRequired, OrderIssue.PriceRequired), check.errors)
    }

    @Test
    fun priceMustSitOnTickAndWithinLimits() {
        assertEquals(listOf(OrderIssue.PriceOffTick(100)), checkOrder(draft(price = 71_550), stock, 10_000_000, 0).errors)
        assertEquals(listOf(OrderIssue.AboveUpperLimit(92_000)), checkOrder(draft(price = 92_100), stock, 100_000_000, 0).errors)
        assertEquals(listOf(OrderIssue.BelowLowerLimit(49_600)), checkOrder(draft(price = 49_550), stock, 10_000_000, 0).errors)
    }

    @Test
    fun buyNeedsBuyingPower() {
        val check = checkOrder(draft(quantity = 100), stock, buyingPower = 7_000_000, sellableQuantity = 0)
        assertEquals(listOf(OrderIssue.NotEnoughBuyingPower(150_000)), check.errors)
    }

    @Test
    fun marketBuyReservesAtUpperLimit() {
        val market = draft(type = OrderType.MARKET, price = 0, quantity = 10)
        assertEquals(920_000, market.reservedAmount(stock))
        assertEquals(listOf(OrderIssue.NotEnoughBuyingPower(20_000)), checkOrder(market, stock, 900_000, 0).errors)
    }

    @Test
    fun sellNeedsHolding() {
        val check = checkOrder(draft(side = OrderSide.SELL, quantity = 60), stock, 0, sellableQuantity = 50)
        assertEquals(listOf(OrderIssue.NotEnoughHolding(50)), check.errors)
    }

    @Test
    fun farPriceIsOnlyAWarning() {
        val check = checkOrder(draft(price = 66_000), stock, 10_000_000, 0)
        assertTrue(check.canSubmit)
        assertEquals(listOf(OrderIssue.FarFromCurrentPrice(7)), check.warnings)
    }

    @Test
    fun maxQuantityFollowsSideAndType() {
        assertEquals(139, maxOrderQuantity(OrderSide.BUY, OrderType.LIMIT, 71_500, stock, 10_000_000, 0))
        assertEquals(108, maxOrderQuantity(OrderSide.BUY, OrderType.MARKET, 0, stock, 10_000_000, 0))
        assertEquals(50, maxOrderQuantity(OrderSide.SELL, OrderType.LIMIT, 71_500, stock, 10_000_000, 50))
    }

    private val openBuy = Order(
        orderNo = "000001", code = stock.code, name = stock.name, side = OrderSide.BUY,
        type = OrderType.LIMIT, price = 71_000, quantity = 10, acceptedAtEpochSeconds = 0,
    )

    @Test
    fun amendLowerNeedsNoExtraCash() {
        val check = checkAmend(openBuy, newPrice = 70_500, stock = stock, buyingPower = 0)
        assertTrue(check.canSubmit)
        assertEquals(0, amendExtraAmount(openBuy, 70_500))
    }

    @Test
    fun amendHigherNeedsOnlyTheDifference() {
        assertEquals(10_000, amendExtraAmount(openBuy, 72_000))
        assertTrue(checkAmend(openBuy, 72_000, stock, buyingPower = 10_000).canSubmit)
        assertEquals(listOf(OrderIssue.NotEnoughBuyingPower(1)), checkAmend(openBuy, 72_000, stock, buyingPower = 9_999).errors)
    }

    @Test
    fun amendRejectsSamePriceOffTickAndMarket() {
        assertEquals(listOf(OrderIssue.SameAsOrderPrice), checkAmend(openBuy, 71_000, stock, 0).errors)
        assertEquals(listOf(OrderIssue.PriceOffTick(100)), checkAmend(openBuy, 70_950, stock, 0).errors)
        val market = openBuy.copy(type = OrderType.MARKET, price = 0)
        assertEquals(listOf(OrderIssue.MarketOrderNotAmendable), checkAmend(market, 71_000, stock, 0).errors)
    }
}
