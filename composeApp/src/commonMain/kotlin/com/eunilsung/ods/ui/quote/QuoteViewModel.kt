package com.eunilsung.ods.ui.quote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.usecase.QuoteUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * 현재가 화면 — 시세·오늘 일봉·차트 봉·호가를 한 종목에 묶어 내놓는다.
 *
 * 상담 중 고객에게 시장 상황을 보여 주는 용도라 주문은 하지 않는다.
 * 화면이 사라지고 5초가 지나면 구독을 끊어 실시간 시세도 멈춘다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuoteViewModel(
    private val quoteUseCases: QuoteUseCases,
) : ViewModel() {

    private val code = MutableStateFlow<String?>(null)
    private val period = MutableStateFlow(ChartPeriod.DAY)

    val uiState: StateFlow<QuoteUiState> = code.filterNotNull().flatMapLatest { selected ->
        combine(
            quoteUseCases.observeStock(selected),
            quoteUseCases.observeCandles(selected, ChartPeriod.DAY),
            period.flatMapLatest { p -> quoteUseCases.observeCandles(selected, p).map { p to it } },
            quoteUseCases.observeOrderBook(selected),
        ) { stock, dayCandles, (chartPeriod, candles), orderBook ->
            QuoteUiState(
                code = selected,
                stock = stock,
                today = dayCandles.lastOrNull(),
                period = chartPeriod,
                candles = candles,
                orderBook = orderBook,
            )
        }.onStart { emit(QuoteUiState(code = selected, period = period.value)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuoteUiState())

    fun onAction(action: QuoteActions) {
        when (action) {
            is QuoteActions.OnEnter -> code.value = action.code
            is QuoteActions.OnPeriodChange -> period.value = action.period
        }
    }
}
