package com.eunilsung.ods.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.eunilsung.ods.data.local.seed.StockSeed
import com.eunilsung.ods.data.mapper.StockMapper
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.domain.repository.StockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext

/**
 * 서버 없이 도는 관심종목 저장소 — 시연용 데이터를 기기 DB 에 채워 쓴다.
 *
 * 시연용 관심종목과 보유 종목은 없을 때만 넣는다. 이미 있는 종목의 시세는 [LocalTickSimulator] 가 움직인 값을 그대로 둔다.
 *
 * 서버 모드 구현(`data/repository/StockRepositoryImpl`)이 생기면 이 클래스를 `by local` 로 위임받아
 * 서버에서 받은 시세만 기기 DB 에 반영하게 한다. 화면은 어느 쪽이든 기기 DB 만 본다.
 */
class LocalStockRepositoryImpl(
    private val database: AppDatabase,
    private val mapper: StockMapper,
) : StockRepository {

    private val queries get() = database.appDatabaseQueries

    private val activeFetches = MutableStateFlow(0)
    private val _isFetching = MutableStateFlow(false)
    override val isFetching: StateFlow<Boolean> = _isFetching

    override fun observeWatchlist(): Flow<List<Stock>> =
        queries.selectWatchlist().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map(mapper::toModel) }

    override fun observeStock(code: String): Flow<Stock?> =
        queries.selectStock(code).asFlow().mapToOneOrNull(Dispatchers.Default).map { it?.let(mapper::toModel) }

    override suspend fun fetchWatchlist(): Boolean {
        _isFetching.value = activeFetches.updateAndGet { it + 1 } > 0
        try {
            withContext(Dispatchers.Default) {
                database.transaction {
                    StockSeed.watchlist.forEachIndexed { index, row ->
                        queries.insertStockIfAbsent(mapper.toEntity(row, index.toLong()))
                    }
                }
            }
            return true
        } finally {
            _isFetching.value = activeFetches.updateAndGet { it - 1 } > 0
        }
    }
}
