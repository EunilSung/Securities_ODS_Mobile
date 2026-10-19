package com.eunilsung.ods.ui.order.panel

import androidx.compose.runtime.Composable
import com.eunilsung.ods.domain.model.OrderRejectReason
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.util.OrderIssue
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.order_issue_above_upper
import ods.composeapp.generated.resources.order_issue_below_lower
import ods.composeapp.generated.resources.order_issue_buying_power
import ods.composeapp.generated.resources.order_issue_far
import ods.composeapp.generated.resources.order_issue_holding
import ods.composeapp.generated.resources.order_issue_market_not_amendable
import ods.composeapp.generated.resources.order_issue_off_tick
import ods.composeapp.generated.resources.order_issue_price_required
import ods.composeapp.generated.resources.order_issue_quantity_required
import ods.composeapp.generated.resources.order_issue_same_price
import ods.composeapp.generated.resources.order_quantity_unit
import ods.composeapp.generated.resources.order_reject_buying_power
import ods.composeapp.generated.resources.order_reject_holding
import ods.composeapp.generated.resources.order_side_buy
import ods.composeapp.generated.resources.order_side_sell
import ods.composeapp.generated.resources.order_type_limit
import ods.composeapp.generated.resources.order_type_market
import ods.composeapp.generated.resources.order_won
import org.jetbrains.compose.resources.stringResource

/** 주문 화면 문구 — 패널과 확인 화면이 같이 쓴다. */

@Composable
fun OrderSide.label(): String = stringResource(if (this == OrderSide.BUY) Res.string.order_side_buy else Res.string.order_side_sell)

@Composable
fun OrderType.label(): String = stringResource(if (this == OrderType.LIMIT) Res.string.order_type_limit else Res.string.order_type_market)

@Composable
fun won(amount: Long): String = stringResource(Res.string.order_won, amount.withComma())

@Composable
fun shares(quantity: Long): String = stringResource(Res.string.order_quantity_unit, quantity.withComma())

@Composable
fun OrderIssue.message(): String = when (this) {
    OrderIssue.QuantityRequired -> stringResource(Res.string.order_issue_quantity_required)
    OrderIssue.PriceRequired -> stringResource(Res.string.order_issue_price_required)
    is OrderIssue.PriceOffTick -> stringResource(Res.string.order_issue_off_tick, tick.withComma())
    is OrderIssue.AboveUpperLimit -> stringResource(Res.string.order_issue_above_upper, limit.withComma())
    is OrderIssue.BelowLowerLimit -> stringResource(Res.string.order_issue_below_lower, limit.withComma())
    is OrderIssue.NotEnoughBuyingPower -> stringResource(Res.string.order_issue_buying_power, shortage.withComma())
    is OrderIssue.NotEnoughHolding -> stringResource(Res.string.order_issue_holding, sellable.withComma())
    is OrderIssue.FarFromCurrentPrice -> stringResource(Res.string.order_issue_far, percent.toString())
    OrderIssue.SameAsOrderPrice -> stringResource(Res.string.order_issue_same_price)
    OrderIssue.MarketOrderNotAmendable -> stringResource(Res.string.order_issue_market_not_amendable)
}

@Composable
fun OrderRejectReason.message(): String = stringResource(
    when (this) {
        OrderRejectReason.NOT_ENOUGH_BUYING_POWER -> Res.string.order_reject_buying_power
        OrderRejectReason.NOT_ENOUGH_HOLDING -> Res.string.order_reject_holding
    }
)
