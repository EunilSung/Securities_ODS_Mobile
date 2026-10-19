package com.eunilsung.ods.ui.balance

import com.eunilsung.ods.domain.model.BalanceSummary
import com.eunilsung.ods.domain.model.Holding

/** 잔고 화면의 상태. */
data class BalanceUiState(
    val summary: BalanceSummary = BalanceSummary(cash = 0, buyingPower = 0, purchaseAmount = 0, evaluationAmount = 0),
    /** 보유 종목(관심종목 순서). */
    val holdings: List<Holding> = emptyList(),
    /** 금액·수량을 가리는지. 수익률·현재가는 공개 시세라 가리지 않는다. */
    val hideAmounts: Boolean = false,
    /** 첫 값을 받았는지. 받기 전에는 0원 대신 대기 표시를 그린다. */
    val isLoaded: Boolean = false,
)
