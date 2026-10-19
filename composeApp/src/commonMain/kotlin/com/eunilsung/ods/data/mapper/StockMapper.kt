package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.data.local.seed.StockSeedRow
import com.eunilsung.ods.db.WatchStockEntity
import com.eunilsung.ods.domain.model.Market
import com.eunilsung.ods.domain.model.Stock

/** 기기 DB 행·시연용 데이터를 도메인 [Stock] 으로 바꾼다. */
class StockMapper {
    fun toModel(entity: WatchStockEntity): Stock = Stock(
        code = entity.code,
        name = entity.name,
        market = entity.market.toMarket(),
        price = entity.price,
        previousClose = entity.previousClose,
        volume = entity.volume,
    )

    internal fun toEntity(row: StockSeedRow, sortOrder: Long): WatchStockEntity = WatchStockEntity(
        code = row.code,
        name = row.name,
        market = row.market,
        price = row.price,
        previousClose = row.previousClose,
        volume = row.volume,
        sortOrder = sortOrder,
    )

    private fun String.toMarket(): Market = Market.entries.firstOrNull { it.name == this } ?: Market.KOSPI
}
