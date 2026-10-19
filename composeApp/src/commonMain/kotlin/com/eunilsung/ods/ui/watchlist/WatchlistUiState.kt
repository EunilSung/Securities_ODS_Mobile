package com.eunilsung.ods.ui.watchlist

import com.eunilsung.ods.domain.model.Stock

/** 관심종목 화면의 상태. */
data class WatchlistUiState(
    val stocks: List<Stock> = emptyList(),
    /** 시세를 다시 받는 중. 목록이 있으면 상단 표시만, 없으면 본문 대기 표시를 그린다. */
    val isRefreshing: Boolean = false,
)
