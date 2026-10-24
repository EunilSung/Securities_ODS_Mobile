package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.Stock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface StockRepository {
    /** 관심종목. 기기 DB 를 보므로 시세가 바뀌면 다시 흐른다. */
    fun observeWatchlist(): Flow<List<Stock>>

    /** 한 종목. 없으면 null. */
    fun observeStock(code: String): Flow<Stock?>

    /** 관심종목을 채운다 — 시연용 관심종목 중 기기에 없는 것만 넣고, 있는 종목의 시세는 그대로 둔다. 채웠으면 true. */
    suspend fun fetchWatchlist(): Boolean

    /** 조회가 진행 중인지 — 상단 새로고침 표시에 쓴다. */
    val isFetching: StateFlow<Boolean>
}
