package com.eunilsung.ods.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.AppLockState
import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.domain.model.Staff
import com.eunilsung.ods.domain.usecase.MainUseCases
import com.eunilsung.ods.ui.applock.AppLockPurpose
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 메인 — 로그인 여부, 잠금 화면, 목록 밀도처럼 앱 전체가 보는 값을 낸다.
 *
 * 로그인돼 있는 동안 [IDLE_CHECK_MILLIS] 마다 자동 잠금 시각이 지났는지 본다. 앱이 앞으로 나올 때도 본다.
 * 로그인되면 탭 화면이 열리기 전에 첫 시세를 받아 두고, 로그아웃되면 [MainScreenEvent.LoggedOut] 을 보내 열린 상세를 닫게 한다.
 */
class MainViewModel(
    private val useCases: MainUseCases,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> =
        combine(useCases.observeStaff(), useCases.observeAppLock(), useCases.observeListDensity()) { staff, lock, density ->
            mainState(staff, lock, density)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            mainState(useCases.observeStaff().value, useCases.observeAppLock().value, useCases.observeListDensity().value),
        )

    private val _events = Channel<MainScreenEvent>(Channel.BUFFERED)
    val events: Flow<MainScreenEvent> = _events.receiveAsFlow()

    private var idleWatch: Job? = null

    init {
        viewModelScope.launch {
            useCases.observeStaff()
                .map { it != null }
                .distinctUntilChanged()
                .drop(1)
                .collect { loggedIn ->
                    if (loggedIn) {
                        prefetch()
                        watchIdle()
                    } else {
                        idleWatch?.cancel()
                        _events.send(MainScreenEvent.LoggedOut)
                    }
                }
        }
        if (useCases.observeStaff().value != null) watchIdle()
    }

    fun onAction(action: MainActions) {
        when (action) {
            MainActions.OnForeground -> if (useCases.observeStaff().value != null) {
                useCases.lockIfInactive()
                prefetch()
            }
            MainActions.OnBackground -> if (useCases.observeStaff().value != null) useCases.recordBackground()
            MainActions.OnUserInteraction -> useCases.recordActivity()
        }
    }

    private fun watchIdle() {
        idleWatch?.cancel()
        idleWatch = viewModelScope.launch {
            while (isActive) {
                delay(IDLE_CHECK_MILLIS)
                useCases.lockIfInactive()
            }
        }
    }

    /** 탭 화면이 열리기 전에 담당 고객과 첫 시세를 받아 둔다. */
    private fun prefetch() {
        viewModelScope.launch { runCatching { useCases.fetchWatchlist() } }
        viewModelScope.launch { useCases.refreshCustomers() }
    }

    private fun mainState(staff: Staff?, lock: AppLockState, density: ListDensity) = MainUiState(
        isLoggedIn = staff != null,
        lockOverlay = when {
            staff == null -> null
            !lock.isPinSet -> AppLockPurpose.REGISTER
            lock.isLocked -> AppLockPurpose.UNLOCK
            else -> null
        },
        listDensity = density,
    )

    private companion object {
        const val IDLE_CHECK_MILLIS = 15_000L
    }
}
