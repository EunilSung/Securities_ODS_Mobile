package com.eunilsung.ods.ui.quote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.domain.usecase.QuoteUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 현재가 화면 — 시세·오늘 일봉·차트 봉·호가를 한 종목에 묶어 내놓는다.
 *
 * 현재가 탭과 종목 상세(오른쪽 칸)가 같은 클래스를 따로 하나씩 쓴다.
 * 화면이 사라지고 5초가 지나면 구독을 끊어 실시간 시세도 멈춘다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuoteViewModel(
    private val quoteUseCases: QuoteUseCases,
) : ViewModel() {

    private val code = MutableStateFlow<String?>(null)
    private val period = MutableStateFlow(ChartPeriod.DAY)
    private val showsWatchlist = MutableStateFlow(false)

    val uiState: StateFlow<QuoteUiState> = code.filterNotNull().flatMapLatest { selected ->
        combine(
            quoteUseCases.observeStock(selected),
            quoteUseCases.observeCandles(selected, ChartPeriod.DAY),
            period.flatMapLatest { p -> quoteUseCases.observeCandles(selected, p).map { p to it } },
            quoteUseCases.observeOrderBook(selected),
            showsWatchlist.flatMapLatest { shows -> if (shows) quoteUseCases.observeWatchlist() else flowOf(emptyList<Stock>()) },
        ) { stock, dayCandles, (chartPeriod, candles), orderBook, watchlist ->
            QuoteUiState(
                code = selected,
                stock = stock,
                today = dayCandles.lastOrNull(),
                period = chartPeriod,
                candles = candles,
                orderBook = orderBook,
                watchlist = watchlist,
            )
        }.onStart { emit(QuoteUiState(code = selected, period = period.value)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuoteUiState())

    fun onAction(action: QuoteActions) {
        when (action) {
            is QuoteActions.OnEnter -> select(action.code)
            QuoteActions.OnEnterTab -> enterTab()
            is QuoteActions.OnSelectStock -> select(action.code)
            is QuoteActions.OnPeriodChange -> period.value = action.period
        }
    }

    private fun select(selected: String) {
        quoteUseCases.setLastQuoteCode(selected)
        code.value = selected
    }

    private fun enterTab() {
        showsWatchlist.value = true
        viewModelScope.launch {
            code.value = quoteUseCases.getLastQuoteCode()
                ?: quoteUseCases.observeWatchlist().first { it.isNotEmpty() }.first().code
        }
    }
}
