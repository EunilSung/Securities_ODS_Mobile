package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.repository.QuoteRepository

class ObserveOrderBookUseCase(private val repository: QuoteRepository) {
    operator fun invoke(code: String) = repository.observeOrderBook(code)
}

class ObserveCandlesUseCase(private val repository: QuoteRepository) {
    operator fun invoke(code: String, period: ChartPeriod) = repository.observeCandles(code, period)
}

/** 현재가 화면이 쓰는 유스케이스 묶음. */
class QuoteUseCases(
    val observeStock: ObserveStockUseCase,
    val observeOrderBook: ObserveOrderBookUseCase,
    val observeCandles: ObserveCandlesUseCase,
)
