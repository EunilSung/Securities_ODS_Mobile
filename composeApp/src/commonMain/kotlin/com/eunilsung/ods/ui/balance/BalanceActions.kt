package com.eunilsung.ods.ui.balance

sealed interface BalanceActions {
    /** 눈 버튼 — 금액을 가리거나 다시 보인다. */
    data object OnToggleHideAmounts : BalanceActions

    /** 보유 종목을 눌렀다. 현재가 상세를 연다. */
    data class OnHoldingClick(val code: String) : BalanceActions
}

/** 잔고 화면이 한 번씩 처리할 일. */
sealed interface BalanceEvent {
    data class OpenStockDetail(val code: String) : BalanceEvent
}
