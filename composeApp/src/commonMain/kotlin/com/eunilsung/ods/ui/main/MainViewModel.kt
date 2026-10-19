package com.eunilsung.ods.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.usecase.ObserveStaffUseCase
import com.eunilsung.ods.domain.usecase.ObserveListDensityUseCase
import com.eunilsung.ods.domain.usecase.StockUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 메인 — 로그인 여부, 목록 밀도처럼 앱 전체가 보는 값을 낸다.
 *
 * 로그인되면 탭 화면이 열리기 전에 첫 시세를 받아 두고, 로그아웃되면 [MainScreenEvent.LoggedOut] 을 보내 열린 상세를 닫게 한다.
 */
class MainViewModel(
    private val stockUseCases: StockUseCases,
    observeListDensity: ObserveListDensityUseCase,
    private val observeStaff: ObserveStaffUseCase,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> =
        combine(observeStaff(), observeListDensity()) { staff, density ->
            MainUiState(isLoggedIn = staff != null, listDensity = density)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            MainUiState(isLoggedIn = observeStaff().value != null, listDensity = observeListDensity().value),
        )

    private val _events = Channel<MainScreenEvent>(Channel.BUFFERED)
    val events: Flow<MainScreenEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeStaff()
                .map { it != null }
                .distinctUntilChanged()
                .drop(1)
                .collect { loggedIn -> if (loggedIn) prefetch() else _events.send(MainScreenEvent.LoggedOut) }
        }
    }

    fun onAction(action: MainActions) {
        when (action) {
            MainActions.OnForeground -> if (observeStaff().value != null) prefetch()
        }
    }

    /** 탭 화면이 열리기 전에 첫 데이터를 받아 둔다. */
    private fun prefetch() {
        viewModelScope.launch { runCatching { stockUseCases.fetchWatchlist() } }
    }
}
