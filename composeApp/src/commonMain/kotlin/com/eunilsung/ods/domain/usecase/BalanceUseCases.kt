package com.eunilsung.ods.domain.usecase

/** 잔고 화면이 쓰는 유스케이스 묶음. */
class BalanceUseCases(
    val observeHoldings: ObserveHoldingsUseCase,
    val observeAccount: ObserveAccountUseCase,
    val observeHideAmounts: ObserveHideAmountsUseCase,
    val setHideAmounts: SetHideAmountsUseCase,
)
