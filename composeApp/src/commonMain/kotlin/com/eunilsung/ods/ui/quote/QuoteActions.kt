package com.eunilsung.ods.ui.quote

import com.eunilsung.ods.domain.model.ChartPeriod

sealed interface QuoteActions {
    /** 종목 상세로 열렸다. 이 종목을 본다. */
    data class OnEnter(val code: String) : QuoteActions

    /** 차트 기간을 바꿨다. */
    data class OnPeriodChange(val period: ChartPeriod) : QuoteActions

    /** 고객에게 보여 주기를 눌렀다. 화면 전체에 크게 띄운다. */
    data object OnPresentClick : QuoteActions

    data object OnPresentDismiss : QuoteActions

    /** 보여 주는 중에 관심종목의 이전·다음 종목으로 넘긴다. */
    data class OnPresentStep(val forward: Boolean) : QuoteActions
}
