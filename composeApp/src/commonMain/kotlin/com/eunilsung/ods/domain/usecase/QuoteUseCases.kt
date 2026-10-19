package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.repository.QuoteRepository
import com.eunilsung.ods.domain.repository.UserPreferenceRepository

class ObserveOrderBookUseCase(private val repository: QuoteRepository) {
    operator fun invoke(code: String) = repository.observeOrderBook(code)
}

class ObserveCandlesUseCase(private val repository: QuoteRepository) {
    operator fun invoke(code: String, period: ChartPeriod) = repository.observeCandles(code, period)
}

class GetLastQuoteCodeUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke(): String? = repository.lastQuoteCode()
}

class SetLastQuoteCodeUseCase(private val repository: UserPreferenceRepository) {
    operator fun invoke(code: String) = repository.setLastQuoteCode(code)
}

/** 현재가 화면이 쓰는 유스케이스 묶음. */
class QuoteUseCases(
    val observeStock: ObserveStockUseCase,
    val observeWatchlist: ObserveWatchlistUseCase,
    val observeOrderBook: ObserveOrderBookUseCase,
    val observeCandles: ObserveCandlesUseCase,
    val getLastQuoteCode: GetLastQuoteCodeUseCase,
    val setLastQuoteCode: SetLastQuoteCodeUseCase,
)
