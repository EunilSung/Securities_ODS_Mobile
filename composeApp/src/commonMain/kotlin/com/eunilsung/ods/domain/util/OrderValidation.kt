package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderDraft
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.model.Stock
import kotlin.math.abs

/** 현재가와 이만큼(%) 넘게 벌어진 지정가는 확인 화면에서 한 번 더 알린다. */
const val FAR_FROM_CURRENT_PRICE_PERCENT = 5

/** 주문을 보내기 전에 걸리는 것. 오류는 보낼 수 없고, 경고는 확인 화면에서 알린다. */
sealed interface OrderIssue {
    data object QuantityRequired : OrderIssue
    data object PriceRequired : OrderIssue
    data class PriceOffTick(val tick: Long) : OrderIssue
    data class AboveUpperLimit(val limit: Long) : OrderIssue
    data class BelowLowerLimit(val limit: Long) : OrderIssue
    data class NotEnoughBuyingPower(val shortage: Long) : OrderIssue
    data class NotEnoughHolding(val sellable: Long) : OrderIssue
    data class FarFromCurrentPrice(val percent: Int) : OrderIssue

    /** 정정 가격이 원래 주문 가격과 같다. */
    data object SameAsOrderPrice : OrderIssue

    /** 시장가 주문은 가격이 없어 정정할 수 없다(취소만 된다). */
    data object MarketOrderNotAmendable : OrderIssue
}

data class OrderCheck(
    val errors: List<OrderIssue>,
    val warnings: List<OrderIssue>,
) {
    val canSubmit: Boolean get() = errors.isEmpty()

    companion object {
        val EMPTY = OrderCheck(emptyList(), emptyList())
    }
}

/**
 * 주문에 묶이는 금액. 시장가 매수는 체결 가격을 모르므로 상한가로 잡는다(증권사 증거금 계산과 같은 방식).
 */
fun OrderDraft.reservedAmount(stock: Stock): Long = when {
    type == OrderType.MARKET && side == OrderSide.BUY -> stock.upperLimit * quantity
    type == OrderType.MARKET -> stock.price * quantity
    else -> price * quantity
}

/**
 * 주문을 검사한다 — 수량·가격 입력, 호가 단위, 상·하한가, 주문가능 금액·매도가능 수량, 현재가와의 거리.
 *
 * @param buyingPower 매수 주문가능 금액.
 * @param sellableQuantity 매도가능 수량(보유 − 미체결 매도).
 */
fun checkOrder(draft: OrderDraft, stock: Stock, buyingPower: Long, sellableQuantity: Long): OrderCheck {
    val errors = mutableListOf<OrderIssue>()
    val warnings = mutableListOf<OrderIssue>()

    if (draft.quantity <= 0) errors += OrderIssue.QuantityRequired

    if (draft.type == OrderType.LIMIT) {
        when {
            draft.price <= 0 -> errors += OrderIssue.PriceRequired
            draft.price > stock.upperLimit -> errors += OrderIssue.AboveUpperLimit(stock.upperLimit)
            draft.price < stock.lowerLimit -> errors += OrderIssue.BelowLowerLimit(stock.lowerLimit)
            floorToTick(draft.price) != draft.price -> errors += OrderIssue.PriceOffTick(tickSize(draft.price))
        }
        if (draft.price > 0 && stock.price > 0) {
            val percent = (abs(draft.price - stock.price) * 100 / stock.price).toInt()
            if (percent >= FAR_FROM_CURRENT_PRICE_PERCENT) warnings += OrderIssue.FarFromCurrentPrice(percent)
        }
    }

    if (draft.quantity > 0) {
        when (draft.side) {
            OrderSide.BUY -> {
                val required = draft.reservedAmount(stock)
                if (draft.type == OrderType.MARKET || draft.price > 0) {
                    if (required > buyingPower) errors += OrderIssue.NotEnoughBuyingPower(required - buyingPower)
                }
            }
            OrderSide.SELL ->
                if (draft.quantity > sellableQuantity) errors += OrderIssue.NotEnoughHolding(sellableQuantity)
        }
    }
    return OrderCheck(errors, warnings)
}

/** 최대 주문 수량 — 매수는 주문가능 금액으로 살 수 있는 만큼, 매도는 매도가능 수량. */
fun maxOrderQuantity(
    side: OrderSide,
    type: OrderType,
    price: Long,
    stock: Stock,
    buyingPower: Long,
    sellableQuantity: Long,
): Long = when (side) {
    OrderSide.SELL -> sellableQuantity
    OrderSide.BUY -> {
        val unit = if (type == OrderType.MARKET) stock.upperLimit else price
        if (unit <= 0) 0 else buyingPower / unit
    }
}

/**
 * 정정을 검사한다 — 남은 수량 전체의 가격을 [newPrice] 로 바꾼다.
 *
 * 매수 정정은 가격을 올린 만큼만 돈이 더 묶이므로, 늘어난 금액을 [buyingPower] 와 비교한다.
 */
fun checkAmend(order: Order, newPrice: Long, stock: Stock, buyingPower: Long): OrderCheck {
    if (order.type == OrderType.MARKET) return OrderCheck(listOf(OrderIssue.MarketOrderNotAmendable), emptyList())
    val draft = OrderDraft(order.code, order.side, OrderType.LIMIT, newPrice, order.quantity)
    val base = checkOrder(draft, stock, buyingPower = Long.MAX_VALUE, sellableQuantity = Long.MAX_VALUE)
    val errors = base.errors.toMutableList()
    if (newPrice > 0 && newPrice == order.price) errors += OrderIssue.SameAsOrderPrice
    if (order.side == OrderSide.BUY && newPrice > 0) {
        val extra = amendExtraAmount(order, newPrice)
        if (extra > buyingPower) errors += OrderIssue.NotEnoughBuyingPower(extra - buyingPower)
    }
    return OrderCheck(errors, base.warnings)
}

/** 매수 정정으로 더 묶이는 금액. 가격을 내리면 0 이다. */
fun amendExtraAmount(order: Order, newPrice: Long): Long =
    if (order.side == OrderSide.BUY) ((newPrice - order.price) * order.quantity).coerceAtLeast(0) else 0
