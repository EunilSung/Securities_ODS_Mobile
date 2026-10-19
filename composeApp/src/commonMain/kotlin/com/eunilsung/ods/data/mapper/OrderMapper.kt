package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.db.OpenOrderEntity
import com.eunilsung.ods.db.WatchStockEntity
import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderDraft
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.model.Stock

/** 미체결 주문 행과 도메인 [Order] 를 잇는다. */
class OrderMapper(private val stockMapper: StockMapper) {
    fun toModel(entity: OpenOrderEntity): Order = Order(
        orderNo = entity.orderNo,
        code = entity.code,
        name = entity.name,
        side = OrderSide.entries.firstOrNull { it.name == entity.side } ?: OrderSide.BUY,
        type = OrderType.entries.firstOrNull { it.name == entity.type } ?: OrderType.LIMIT,
        price = entity.price,
        quantity = entity.quantity,
        acceptedAtEpochSeconds = entity.acceptedAt,
        originalOrderNo = entity.originalOrderNo,
    )

    fun toEntity(
        draft: OrderDraft,
        orderNo: String,
        name: String,
        reservedAmount: Long,
        acceptedAt: Long,
        originalOrderNo: String? = null,
    ): OpenOrderEntity =
        OpenOrderEntity(
            orderNo = orderNo,
            code = draft.code,
            name = name,
            side = draft.side.name,
            type = draft.type.name,
            price = if (draft.type == OrderType.MARKET) 0 else draft.price,
            quantity = draft.quantity,
            reservedAmount = reservedAmount,
            acceptedAt = acceptedAt,
            originalOrderNo = originalOrderNo,
        )

    fun stockOf(entity: WatchStockEntity): Stock = stockMapper.toModel(entity)

    /** 거래소 주문번호처럼 여섯 자리로 채운다. */
    fun formatOrderNo(number: Long): String = number.toString().padStart(6, '0')
}
