package com.eunilsung.ods.domain.model

import com.eunilsung.ods.domain.util.lowerLimitPrice
import com.eunilsung.ods.domain.util.upperLimitPrice

/**
 * 종목 시세 한 건.
 *
 * 가격은 원 단위 정수로 둔다. 소수 가격(해외주식)은 [priceScale] 로 자릿수를 함께 들고 다닌다.
 */
data class Stock(
    val code: String,
    val name: String,
    val market: Market,
    val price: Long,
    val previousClose: Long,
    val volume: Long,
    val priceScale: Int = 0,
) {
    /** 전일 대비 변화량. */
    val change: Long get() = price - previousClose

    /** 전일 대비 등락률(%). 전일 종가가 없으면 0. */
    val changeRate: Double get() = if (previousClose == 0L) 0.0 else change * 100.0 / previousClose

    val direction: PriceDirection get() = PriceDirection.of(price, previousClose)

    /** 오늘 상한가 — 전일 종가 +30% 이내의 가장 높은 호가. */
    val upperLimit: Long get() = upperLimitPrice(previousClose)

    /** 오늘 하한가 — 전일 종가 −30% 이상의 가장 낮은 호가. */
    val lowerLimit: Long get() = lowerLimitPrice(previousClose)
}

enum class Market { KOSPI, KOSDAQ }

/** 등락 방향. 국내 관례대로 화면에서 상승은 빨강, 하락은 파랑으로 그린다. */
enum class PriceDirection {
    UP, DOWN, FLAT;

    companion object {
        /** [price] 가 기준가격 [basePrice](보통 전일 종가)보다 높은지 낮은지. */
        fun of(price: Long, basePrice: Long): PriceDirection = when {
            price > basePrice -> UP
            price < basePrice -> DOWN
            else -> FLAT
        }
    }
}
