package com.eunilsung.ods.domain.model

enum class OrderSide { BUY, SELL }

/** 지정가는 정한 가격에, 시장가는 그때 시장 가격에 체결된다. */
enum class OrderType { LIMIT, MARKET }

/** 보내기 전의 주문. 시장가면 [price] 는 쓰지 않는다. */
data class OrderDraft(
    val code: String,
    val side: OrderSide,
    val type: OrderType,
    val price: Long,
    val quantity: Long,
)

/**
 * 접수된 주문(미체결).
 *
 * @param originalOrderNo 정정으로 생긴 주문이면 원래 주문번호. 거래소는 정정하면 새 주문번호를 준다.
 */
data class Order(
    val orderNo: String,
    val code: String,
    val name: String,
    val side: OrderSide,
    val type: OrderType,
    val price: Long,
    val quantity: Long,
    val acceptedAtEpochSeconds: Long,
    val originalOrderNo: String? = null,
)

/** 주문을 보낸 결과. 거부와 닿지 못함을 섞지 않는다 — 닿지 못한 주문은 접수됐을 수도 있어 그대로 다시 보내면 안 된다. */
sealed interface OrderResult {
    data class Accepted(val order: Order) : OrderResult
    data class Rejected(val reason: OrderRejectReason) : OrderResult
    data object Unreachable : OrderResult
}

enum class OrderRejectReason { NOT_ENOUGH_BUYING_POWER, NOT_ENOUGH_HOLDING }

/**
 * 정정·취소를 보낸 결과. 받는 쪽에 이미 없는 주문(체결·취소됨)은 [OrderChangeRejectReason.NOT_OPEN] 으로 거부된다.
 */
sealed interface OrderChangeResult {
    /** 정정이면 새로 생긴 주문, 취소면 null. */
    data class Done(val order: Order?) : OrderChangeResult
    data class Rejected(val reason: OrderChangeRejectReason) : OrderChangeResult
    data object Unreachable : OrderChangeResult
}

enum class OrderChangeRejectReason { NOT_OPEN, NOT_ENOUGH_BUYING_POWER }

/** 계좌 요약. [buyingPower] 는 예수금에서 미체결 매수 주문에 묶인 금액을 뺀 값이다. */
data class Account(
    val cash: Long,
    val buyingPower: Long,
)
