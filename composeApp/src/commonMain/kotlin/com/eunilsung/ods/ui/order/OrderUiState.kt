package com.eunilsung.ods.ui.order

import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderBook
import com.eunilsung.ods.domain.model.OrderDraft
import com.eunilsung.ods.domain.model.OrderRejectReason
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.domain.util.OrderCheck
import com.eunilsung.ods.domain.util.OrderIssue

/** 주문 화면의 상태. */
data class OrderUiState(
    val code: String = "",
    /** 아직 고른 종목이 없거나 기기 DB 에 없으면 null. */
    val stock: Stock? = null,
    /** 주문 화면 왼쪽 호가. 넓은 태블릿 주문 칸에서는 가운데 칸에 호가가 있어 null 이다. */
    val orderBook: OrderBook? = null,
    /** 주문 탭 위쪽 종목 줄. */
    val watchlist: List<Stock> = emptyList(),
    val side: OrderSide = OrderSide.BUY,
    val type: OrderType = OrderType.LIMIT,
    /** 입력 중인 가격·수량(숫자만). */
    val priceText: String = "",
    val quantityText: String = "",
    val buyingPower: Long = 0,
    val sellableQuantity: Long = 0,
    /** 지금 가격으로 넣을 수 있는 최대 수량. */
    val maxQuantity: Long = 0,
    /** 주문에 묶이는 금액. 시장가 매수는 상한가 기준이다. */
    val orderAmount: Long = 0,
    val check: OrderCheck = OrderCheck.EMPTY,
    /** 확인·결과 화면. 닫혀 있으면 null. */
    val confirm: OrderConfirmState? = null,
) {
    val quantity: Long get() = quantityText.toLongOrNull() ?: 0
}

/** 주문 버튼을 누른 뒤의 단계 — 확인 → 인증 → (보내는 중) → 접수·거부·확인 불가. */
sealed interface OrderConfirmState {
    data class Review(
        val draft: OrderDraft,
        val stockName: String,
        val amount: Long,
        val warnings: List<OrderIssue>,
        /** 인증 창이 떠 있다. 인증되면 보낸다. */
        val awaitingAuth: Boolean = false,
        val isSubmitting: Boolean = false,
    ) : OrderConfirmState

    data class Accepted(val order: Order) : OrderConfirmState
    data class Rejected(val reason: OrderRejectReason) : OrderConfirmState

    /** 응답을 받지 못했다. 접수됐을 수 있으므로 다시 보내기 전에 주문 내역을 확인하게 한다. */
    data object Unreachable : OrderConfirmState
}
