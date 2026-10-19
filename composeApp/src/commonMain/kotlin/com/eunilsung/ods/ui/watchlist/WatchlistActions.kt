package com.eunilsung.ods.ui.watchlist

import com.eunilsung.ods.domain.model.ListDensity

sealed interface WatchlistActions {
    /** 새로고침을 눌렀다. */
    data object OnRefresh : WatchlistActions

    /** 종목을 눌렀다. 상세 화면을 연다. */
    data class OnStockClick(val code: String) : WatchlistActions

    /** 목록 밀도(기본·촘촘)를 바꿨다. 앱 전체 목록에 적용된다. */
    data class OnListDensityChange(val density: ListDensity) : WatchlistActions
}

/** 관심종목 화면이 한 번씩 처리할 일. */
sealed interface WatchlistEvent {
    data class OpenStockDetail(val code: String) : WatchlistEvent
}
