package com.eunilsung.ods.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.eunilsung.ods.data.local.seed.AccountSeed
import com.eunilsung.ods.data.mapper.HoldingMapper
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.Holding
import com.eunilsung.ods.domain.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 서버 없이 도는 계좌 — 시연용 예수금과 보유 종목에서 미체결 주문에 묶인 만큼을 뺀다.
 *
 * 보유 종목은 [LocalStockRepositoryImpl] 이 관심종목과 함께 채운다. 체결은 흉내 내지 않는다. 접수한 주문은 미체결로 남아 주문가능 금액·매도가능 수량을 계속 묶는다.
 */
class LocalAccountRepositoryImpl(
    private val database: AppDatabase,
    private val mapper: HoldingMapper,
) : AccountRepository {

    private val queries get() = database.appDatabaseQueries

    override fun observeAccount(): Flow<Account> =
        queries.sumReservedBuyAmount().asFlow().mapToOne(Dispatchers.Default)
            .map { reserved -> Account(cash = AccountSeed.CASH, buyingPower = (AccountSeed.CASH - reserved).coerceAtLeast(0)) }

    override fun observeSellableQuantity(code: String): Flow<Long> =
        combine(
            queries.selectHoldingQuantity(code).asFlow().mapToOneOrNull(Dispatchers.Default),
            queries.sumOpenSellQuantity(code).asFlow().mapToOne(Dispatchers.Default),
        ) { holding, openSell -> ((holding ?: 0) - openSell).coerceAtLeast(0) }

    /** 관심종목에 없는 보유 종목은 이름 대신 코드, 시세 대신 평균단가로 보인다. 서버 모드에서는 서버가 이름·시세를 함께 준다. */
    override fun observeHoldings(): Flow<List<Holding>> =
        queries.selectHoldings().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map(mapper::toModel) }
}
