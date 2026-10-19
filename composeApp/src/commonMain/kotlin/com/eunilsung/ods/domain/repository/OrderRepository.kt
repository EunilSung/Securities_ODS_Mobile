package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.Holding
import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderChangeResult
import com.eunilsung.ods.domain.model.OrderDraft
import com.eunilsung.ods.domain.model.OrderResult
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    /** 주문을 보낸다. 주문가능 금액·매도가능 수량은 받는 쪽(서버)이 다시 확인한다. */
    suspend fun submit(draft: OrderDraft): OrderResult

    /** 미체결 주문(최근 것부터). */
    fun observeOpenOrders(): Flow<List<Order>>

    /** 남은 수량 전체의 가격을 바꾼다. 받아들여지면 새 주문번호의 주문이 생기고 원래 주문은 없어진다. */
    suspend fun amend(orderNo: String, newPrice: Long): OrderChangeResult

    /** 남은 수량 전체를 취소한다. */
    suspend fun cancel(orderNo: String): OrderChangeResult
}

interface AccountRepository {
    fun observeAccount(): Flow<Account>

    /** [code] 매도가능 수량 — 보유 수량에서 미체결 매도 수량을 뺀 값. */
    fun observeSellableQuantity(code: String): Flow<Long>

    /** 보유 종목과 지금 시세. 시세·미체결 매도가 바뀌면 다시 흐른다. */
    fun observeHoldings(): Flow<List<Holding>>
}
