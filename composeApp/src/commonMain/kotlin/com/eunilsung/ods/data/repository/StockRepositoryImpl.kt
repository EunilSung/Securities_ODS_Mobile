package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalStockRepositoryImpl
import com.eunilsung.ods.data.mapper.ServerMarketMapper
import com.eunilsung.ods.data.mapper.StockMapper
import com.eunilsung.ods.data.remote.server.OdsServer
import com.eunilsung.ods.data.remote.server.ServerEvents
import com.eunilsung.ods.data.remote.server.valueOrNull
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.repository.StockRepository
import com.eunilsung.ods.shared.api.ServerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 서버 모드 관심종목. [fetchWatchlist] 가 서버 시세를 기기 DB 에 덮어쓰고, 체결 알림은 그 종목의 DB 시세를 바로 고친다.
 *
 * 화면과 호가·봉([com.eunilsung.ods.data.local.repository.LocalQuoteRepositoryImpl])은 기기 DB 시세를 따라간다.
 * 다시 연결되면 끊긴 동안 놓친 체결을 메우려고 다시 받는다.
 */
class StockRepositoryImpl(
    private val local: LocalStockRepositoryImpl,
    private val database: AppDatabase,
    private val server: OdsServer,
    serverEvents: ServerEvents,
    private val mapper: StockMapper,
    private val serverMapper: ServerMarketMapper,
) : StockRepository by local {

    private val queries get() = database.appDatabaseQueries
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val activeFetches = MutableStateFlow(0)
    private val _isFetching = MutableStateFlow(false)
    override val isFetching: StateFlow<Boolean> = _isFetching

    init {
        scope.launch { serverEvents.connected.collect { fetchWatchlist() } }
        scope.launch {
            serverEvents.events.collect { event ->
                val quote = event.quote
                if (event.type == ServerEvent.TYPE_QUOTE && quote != null) {
                    queries.updateQuote(price = quote.price, volume = quote.volume, code = quote.code)
                }
            }
        }
    }

    override suspend fun fetchWatchlist(): Boolean {
        _isFetching.value = activeFetches.updateAndGet { it + 1 } > 0
        try {
            val stocks = server.stocks().valueOrNull() ?: return false
            withContext(Dispatchers.Default) {
                database.transaction {
                    stocks.forEachIndexed { index, dto -> queries.upsertStock(mapper.toEntity(serverMapper.toModel(dto), index.toLong())) }
                }
            }
            return true
        } finally {
            _isFetching.value = activeFetches.updateAndGet { it - 1 } > 0
        }
    }
}
