package com.eunilsung.ods.ui.openorder

import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderChangeRejectReason
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.domain.util.OrderCheck

/** 미체결 화면의 상태. */
data class OpenOrdersUiState(
    /** 미체결 주문(최근 것부터). */
    val orders: List<Order> = emptyList(),
    /** 종목코드 → 지금 시세. 주문 가격 옆에 현재가를 보인다. */
    val stocks: Map<String, Stock> = emptyMap(),
    val buyingPower: Long = 0,
    /** 정정·취소 창. 닫혀 있으면 null. */
    val change: OrderChangeState? = null,
    /** 첫 목록을 받았는지. 받기 전에는 "없음" 대신 대기 표시를 그린다. */
    val isLoaded: Boolean = false,
)

/** 정정·취소 창의 단계 — 입력·확인 → 인증 → (보내는 중) → 완료·거부·확인 불가. */
sealed interface OrderChangeState {
    /**
     * 정정 입력.
     *
     * @param stock 종목 시세. 상·하한가와 호가 단위 검사에 쓴다. 아직 못 받았으면 null.
     */
    data class Amend(
        val order: Order,
        val stock: Stock?,
        val priceText: String,
        val check: OrderCheck,
        val extraAmount: Long,
        val awaitingAuth: Boolean,
        val isSubmitting: Boolean,
    ) : OrderChangeState

    data class Cancel(val order: Order, val awaitingAuth: Boolean, val isSubmitting: Boolean) : OrderChangeState

    /** 정정됐다. [order] 는 새 주문번호의 주문이다. */
    data class Amended(val order: Order) : OrderChangeState

    data class Canceled(val order: Order) : OrderChangeState
    data class Rejected(val reason: OrderChangeRejectReason) : OrderChangeState

    /** 응답을 받지 못했다. 정정·취소가 됐을 수 있으므로 목록을 보고 다시 하게 한다. */
    data object Unreachable : OrderChangeState
}
