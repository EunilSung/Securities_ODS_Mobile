package com.eunilsung.ods.ui.quote

import com.eunilsung.ods.domain.model.ChartPeriod

sealed interface QuoteActions {
    /** 종목 상세로 열렸다. 이 종목을 본다. */
    data class OnEnter(val code: String) : QuoteActions

    /** 차트 기간을 바꿨다. */
    data class OnPeriodChange(val period: ChartPeriod) : QuoteActions
}
