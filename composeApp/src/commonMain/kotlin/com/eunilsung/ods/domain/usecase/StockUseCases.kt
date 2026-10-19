package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.repository.StockRepository

class ObserveWatchlistUseCase(private val repository: StockRepository) {
    operator fun invoke() = repository.observeWatchlist()
}

class FetchWatchlistUseCase(private val repository: StockRepository) {
    suspend operator fun invoke(): Boolean = repository.fetchWatchlist()
}

class ObserveWatchlistFetchingUseCase(private val repository: StockRepository) {
    operator fun invoke() = repository.isFetching
}

class ObserveStockUseCase(private val repository: StockRepository) {
    operator fun invoke(code: String) = repository.observeStock(code)
}

/** 관심종목·현재가 화면이 쓰는 유스케이스 묶음. */
class StockUseCases(
    val observeWatchlist: ObserveWatchlistUseCase,
    val fetchWatchlist: FetchWatchlistUseCase,
    val observeWatchlistFetching: ObserveWatchlistFetchingUseCase,
    val observeStock: ObserveStockUseCase,
)
