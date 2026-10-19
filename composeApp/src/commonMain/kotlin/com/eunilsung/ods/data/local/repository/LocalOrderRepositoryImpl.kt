package com.eunilsung.ods.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.eunilsung.ods.data.local.seed.AccountSeed
import com.eunilsung.ods.data.mapper.OrderMapper
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderChangeRejectReason
import com.eunilsung.ods.domain.model.OrderChangeResult
import com.eunilsung.ods.domain.model.OrderDraft
import com.eunilsung.ods.domain.model.OrderRejectReason
import com.eunilsung.ods.domain.model.OrderResult
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.repository.OrderRepository
import com.eunilsung.ods.domain.util.reservedAmount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * 서버 없이 도는 주문 접수·정정·취소 — 주문가능 금액·매도가능 수량을 다시 확인하고 미체결 주문으로 기기 DB 에 남긴다.
 *
 * 주문번호는 순번 테이블에서 받아 취소·정정 뒤에도 겹치지 않는다. 정정은 거래소처럼 새 주문번호를 주고 원주문번호를 남긴다.
 * 서버 모드 구현은 같은 확인을 서버가 하고, 응답을 받지 못하면 `Unreachable` 을 돌려준다.
 */
class LocalOrderRepositoryImpl(
    private val database: AppDatabase,
    private val mapper: OrderMapper,
    private val simulatedRoundTripMillis: Long = SIMULATED_ROUND_TRIP_MILLIS,
) : OrderRepository {

    private val queries get() = database.appDatabaseQueries

    override suspend fun submit(draft: OrderDraft): OrderResult {
        delay(simulatedRoundTripMillis)
        return withContext(Dispatchers.Default) {
            database.transactionWithResult {
                val stockRow = queries.selectStock(draft.code).executeAsOneOrNull()
                    ?: return@transactionWithResult OrderResult.Rejected(OrderRejectReason.NOT_ENOUGH_HOLDING)
                val stock = mapper.stockOf(stockRow)
                val reserved = draft.reservedAmount(stock)
                when (draft.side) {
                    OrderSide.BUY -> {
                        if (reserved > buyingPower()) return@transactionWithResult OrderResult.Rejected(OrderRejectReason.NOT_ENOUGH_BUYING_POWER)
                    }
                    OrderSide.SELL -> {
                        val holding = queries.selectHoldingQuantity(draft.code).executeAsOneOrNull() ?: 0
                        val sellable = holding - queries.sumOpenSellQuantity(draft.code).executeAsOne()
                        if (draft.quantity > sellable) return@transactionWithResult OrderResult.Rejected(OrderRejectReason.NOT_ENOUGH_HOLDING)
                    }
                }
                val now = nowEpochSeconds()
                val entity = mapper.toEntity(draft, issueOrderNo(now), stock.name, reserved, now)
                queries.insertOpenOrder(entity)
                OrderResult.Accepted(mapper.toModel(entity))
            }
        }
    }

    override fun observeOpenOrders(): Flow<List<Order>> =
        queries.selectOpenOrders().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map(mapper::toModel) }

    override suspend fun amend(orderNo: String, newPrice: Long): OrderChangeResult {
        delay(simulatedRoundTripMillis)
        return withContext(Dispatchers.Default) {
            database.transactionWithResult {
                val original = queries.selectOpenOrder(orderNo).executeAsOneOrNull()
                    ?: return@transactionWithResult OrderChangeResult.Rejected(OrderChangeRejectReason.NOT_OPEN)
                val order = mapper.toModel(original)
                val reserved = when (order.side) {
                    OrderSide.BUY -> newPrice * order.quantity
                    OrderSide.SELL -> 0
                }
                if (order.side == OrderSide.BUY && reserved > buyingPower() + original.reservedAmount) {
                    return@transactionWithResult OrderChangeResult.Rejected(OrderChangeRejectReason.NOT_ENOUGH_BUYING_POWER)
                }
                val draft = OrderDraft(order.code, order.side, OrderType.LIMIT, newPrice, order.quantity)
                val now = nowEpochSeconds()
                val amended = mapper.toEntity(draft, issueOrderNo(now), order.name, reserved, now, originalOrderNo = order.orderNo)
                queries.deleteOpenOrder(orderNo)
                queries.insertOpenOrder(amended)
                OrderChangeResult.Done(mapper.toModel(amended))
            }
        }
    }

    override suspend fun cancel(orderNo: String): OrderChangeResult {
        delay(simulatedRoundTripMillis)
        return withContext(Dispatchers.Default) {
            database.transactionWithResult {
                queries.selectOpenOrder(orderNo).executeAsOneOrNull()
                    ?: return@transactionWithResult OrderChangeResult.Rejected(OrderChangeRejectReason.NOT_OPEN)
                queries.deleteOpenOrder(orderNo)
                OrderChangeResult.Done(null)
            }
        }
    }

    /** 예수금에서 미체결 매수에 묶인 금액을 뺀 값. 트랜잭션 안에서 부른다. */
    private fun buyingPower(): Long = AccountSeed.CASH - queries.sumReservedBuyAmount().executeAsOne()

    /** 새 주문번호를 받는다. 트랜잭션 안에서 불러야 같은 연결의 마지막 순번을 읽는다. */
    private fun issueOrderNo(now: Long): String {
        queries.insertOrderNumber(now)
        return mapper.formatOrderNo(queries.lastOrderNumber().executeAsOne())
    }

    private fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

    private companion object {
        /** 서버에 다녀오는 시간을 흉내 낸다. 확인 화면의 "보내는 중" 상태가 보이게 한다. */
        const val SIMULATED_ROUND_TRIP_MILLIS = 400L
    }
}
