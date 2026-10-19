package com.eunilsung.ods.domain.util

/**
 * 가격대별 호가 단위 — 한국거래소 유가증권·코스닥 공통(2023년 1월 개편 기준).
 *
 * | 가격 | 단위 |
 * |---|---|
 * | 2,000원 미만 | 1 |
 * | 5,000원 미만 | 5 |
 * | 20,000원 미만 | 10 |
 * | 50,000원 미만 | 50 |
 * | 200,000원 미만 | 100 |
 * | 500,000원 미만 | 500 |
 * | 그 이상 | 1,000 |
 */
fun tickSize(price: Long): Long = when {
    price < 2_000 -> 1
    price < 5_000 -> 5
    price < 20_000 -> 10
    price < 50_000 -> 50
    price < 200_000 -> 100
    price < 500_000 -> 500
    else -> 1_000
}

/** [price] 이하에서 가장 가까운 호가. */
fun floorToTick(price: Long): Long = price - price % tickSize(price)

/** [price] 이상에서 가장 가까운 호가. */
fun ceilToTick(price: Long): Long {
    val floor = floorToTick(price)
    return if (floor == price) price else floor + tickSize(floor)
}

/** 한 호가 위. 가격대가 바뀌는 자리(예: 49,950 → 50,000)도 맞게 넘어간다. */
fun tickUp(price: Long): Long {
    val floor = floorToTick(price)
    return floor + tickSize(floor)
}

/** 한 호가 아래. 가격대가 바뀌는 자리(예: 50,000 → 49,950)도 맞게 넘어간다. */
fun tickDown(price: Long): Long = floorToTick(price - 1).coerceAtLeast(1)

/** 상한가 — 기준가격 +30% 이내의 가장 높은 호가. */
fun upperLimitPrice(basePrice: Long): Long = floorToTick(basePrice * 13 / 10)

/** 하한가 — 기준가격 −30% 이상의 가장 낮은 호가. */
fun lowerLimitPrice(basePrice: Long): Long = ceilToTick((basePrice * 7 + 9) / 10)
