package com.eunilsung.ods.data.local.seed

/** 서버 없이 주문을 시연하기 위한 계좌. 예수금과 보유 종목은 지어낸 값이다. */
internal object AccountSeed {
    const val CASH: Long = 10_000_000

    /** (종목코드, 수량, 평균단가) */
    val holdings: List<Triple<String, Long, Long>> = listOf(
        Triple("005930", 50, 68_200),
        Triple("035720", 120, 47_800),
        Triple("035420", 10, 195_000),
    )
}
