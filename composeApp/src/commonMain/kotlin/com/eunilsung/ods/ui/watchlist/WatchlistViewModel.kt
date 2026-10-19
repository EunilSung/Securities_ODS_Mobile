package com.eunilsung.ods.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.usecase.SetListDensityUseCase
import com.eunilsung.ods.domain.usecase.StockUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WatchlistViewModel(
    private val stockUseCases: StockUseCases,
    private val setListDensity: SetListDensityUseCase,
) : ViewModel() {

    val uiState: StateFlow<WatchlistUiState> =
        combine(
            stockUseCases.observeWatchlist(),
            stockUseCases.observeWatchlistFetching(),
        ) { stocks, isFetching ->
            WatchlistUiState(stocks = stocks, isRefreshing = isFetching)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchlistUiState())

    private val _events = Channel<WatchlistEvent>(Channel.BUFFERED)
    val events: Flow<WatchlistEvent> = _events.receiveAsFlow()

    fun onAction(action: WatchlistActions) {
        when (action) {
            WatchlistActions.OnRefresh -> viewModelScope.launch { stockUseCases.fetchWatchlist() }
            is WatchlistActions.OnStockClick ->
                viewModelScope.launch { _events.send(WatchlistEvent.OpenStockDetail(action.code)) }
            is WatchlistActions.OnListDensityChange -> setListDensity(action.density)
        }
    }
}
