package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.db.SelectHoldings
import com.eunilsung.ods.domain.model.Holding

/** 보유 종목 조회 행(보유 + 시세 + 미체결 매도)을 도메인 [Holding] 으로 바꾼다. */
class HoldingMapper {
    fun toModel(row: SelectHoldings): Holding = Holding(
        code = row.code,
        name = row.name,
        quantity = row.quantity,
        averagePrice = row.averagePrice,
        price = row.price,
        previousClose = row.previousClose,
        openSellQuantity = row.openSellQuantity,
    )
}
