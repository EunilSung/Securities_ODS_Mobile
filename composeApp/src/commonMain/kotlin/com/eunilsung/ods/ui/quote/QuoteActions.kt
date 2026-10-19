package com.eunilsung.ods.ui.quote

import com.eunilsung.ods.domain.model.ChartPeriod

sealed interface QuoteActions {
    /** 종목 상세로 열렸다. 이 종목을 보고, 현재가 탭도 다음에는 이 종목을 연다. */
    data class OnEnter(val code: String) : QuoteActions

    /** 현재가 탭으로 열렸다. 마지막으로 본 종목(없으면 관심종목 첫 종목)을 연다. */
    data object OnEnterTab : QuoteActions

    /** 현재가 탭 위쪽 종목 줄에서 종목을 골랐다. */
    data class OnSelectStock(val code: String) : QuoteActions

    /** 차트 기간을 바꿨다. */
    data class OnPeriodChange(val period: ChartPeriod) : QuoteActions
}
