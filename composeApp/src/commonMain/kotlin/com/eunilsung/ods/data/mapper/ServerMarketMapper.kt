package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.domain.model.Market
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.shared.api.StockDto

/** 서버 시세 응답을 도메인 [Stock] 으로 바꾼다. */
class ServerMarketMapper {
    fun toModel(dto: StockDto): Stock = Stock(
        code = dto.code,
        name = dto.name,
        market = dto.market.toEnumOr(Market.KOSPI),
        price = dto.price,
        previousClose = dto.previousClose,
        volume = dto.volume,
    )
}
