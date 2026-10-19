package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.Stock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface StockRepository {
    /** 관심종목. 기기 DB 를 보므로 시세가 바뀌면 다시 흐른다. */
    fun observeWatchlist(): Flow<List<Stock>>

    /** 한 종목. 없으면 null. */
    fun observeStock(code: String): Flow<Stock?>

    /** 서버(또는 시연용 데이터)에서 관심종목 시세를 다시 받는다. 실패하면 가지고 있던 값을 그대로 둔다. */
    suspend fun fetchWatchlist(): Boolean

    /** 조회가 진행 중인지 — 상단 새로고침 표시에 쓴다. */
    val isFetching: StateFlow<Boolean>
}
