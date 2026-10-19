package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.OrderChangeResult
import com.eunilsung.ods.domain.model.OrderDraft
import com.eunilsung.ods.domain.model.OrderResult
import com.eunilsung.ods.domain.repository.AccountRepository
import com.eunilsung.ods.domain.repository.OrderRepository

class SubmitOrderUseCase(private val repository: OrderRepository) {
    suspend operator fun invoke(draft: OrderDraft): OrderResult = repository.submit(draft)
}

class ObserveOpenOrdersUseCase(private val repository: OrderRepository) {
    operator fun invoke() = repository.observeOpenOrders()
}

class AmendOrderUseCase(private val repository: OrderRepository) {
    suspend operator fun invoke(orderNo: String, newPrice: Long): OrderChangeResult = repository.amend(orderNo, newPrice)
}

class CancelOrderUseCase(private val repository: OrderRepository) {
    suspend operator fun invoke(orderNo: String): OrderChangeResult = repository.cancel(orderNo)
}

class ObserveAccountUseCase(private val repository: AccountRepository) {
    operator fun invoke() = repository.observeAccount()
}

class ObserveSellableQuantityUseCase(private val repository: AccountRepository) {
    operator fun invoke(code: String) = repository.observeSellableQuantity(code)
}

/** 주문 화면이 쓰는 유스케이스 묶음. */
class OrderUseCases(
    val observeStock: ObserveStockUseCase,
    val observeWatchlist: ObserveWatchlistUseCase,
    val observeOrderBook: ObserveOrderBookUseCase,
    val observeAccount: ObserveAccountUseCase,
    val observeSellableQuantity: ObserveSellableQuantityUseCase,
    val submitOrder: SubmitOrderUseCase,
    val getLastQuoteCode: GetLastQuoteCodeUseCase,
    val setLastQuoteCode: SetLastQuoteCodeUseCase,
)

/** 미체결 화면이 쓰는 유스케이스 묶음. */
class OpenOrderUseCases(
    val observeOpenOrders: ObserveOpenOrdersUseCase,
    val observeStock: ObserveStockUseCase,
    val observeWatchlist: ObserveWatchlistUseCase,
    val observeAccount: ObserveAccountUseCase,
    val amendOrder: AmendOrderUseCase,
    val cancelOrder: CancelOrderUseCase,
)

class ObserveHoldingsUseCase(private val repository: AccountRepository) {
    operator fun invoke() = repository.observeHoldings()
}
