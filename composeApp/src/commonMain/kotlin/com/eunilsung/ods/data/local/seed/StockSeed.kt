package com.eunilsung.ods.data.local.seed

/** 서버 없이 화면을 띄우기 위한 시연용 관심종목. 값은 예시이며 실제 시세가 아니다. */
internal data class StockSeedRow(
    val code: String,
    val name: String,
    val market: String,
    val price: Long,
    val previousClose: Long,
    val volume: Long,
)

internal object StockSeed {
    val watchlist: List<StockSeedRow> = listOf(
        StockSeedRow("005930", "삼성전자", "KOSPI", 71_500, 70_800, 12_345_678),
        StockSeedRow("000660", "SK하이닉스", "KOSPI", 182_300, 185_000, 3_210_456),
        StockSeedRow("035420", "NAVER", "KOSPI", 201_000, 201_000, 812_334),
        StockSeedRow("035720", "카카오", "KOSPI", 43_250, 42_100, 2_456_789),
        StockSeedRow("373220", "LG에너지솔루션", "KOSPI", 389_500, 395_000, 254_112),
        StockSeedRow("247540", "에코프로비엠", "KOSDAQ", 168_900, 161_200, 1_102_345),
        StockSeedRow("091990", "셀트리온헬스케어", "KOSDAQ", 61_800, 62_400, 678_901),
    )
}
