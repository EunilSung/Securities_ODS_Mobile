package com.eunilsung.ods.ui.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.BalanceSummary
import com.eunilsung.ods.domain.usecase.BalanceUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 잔고 화면 — 보유 종목과 계좌를 묶어 요약을 내고, 금액 가리기를 저장한다. */
class BalanceViewModel(
    private val useCases: BalanceUseCases,
) : ViewModel() {

    val uiState: StateFlow<BalanceUiState> = combine(
        useCases.observeHoldings(),
        useCases.observeAccount(),
        useCases.observeHideAmounts(),
    ) { holdings, account, hide ->
        BalanceUiState(
            summary = BalanceSummary.of(account, holdings),
            holdings = holdings,
            hideAmounts = hide,
            isLoaded = true,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BalanceUiState(hideAmounts = useCases.observeHideAmounts().value),
    )

    private val _events = Channel<BalanceEvent>(Channel.BUFFERED)
    val events: Flow<BalanceEvent> = _events.receiveAsFlow()

    fun onAction(action: BalanceActions) {
        when (action) {
            BalanceActions.OnToggleHideAmounts -> useCases.setHideAmounts(!useCases.observeHideAmounts().value)
            is BalanceActions.OnHoldingClick -> viewModelScope.launch { _events.send(BalanceEvent.OpenStockDetail(action.code)) }
        }
    }
}
