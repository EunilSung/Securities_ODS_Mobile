package com.eunilsung.ods.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.usecase.SettingUseCases
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 더보기 설정 — 로그인한 직원·로그아웃, 화면(목록 밀도·개인정보 가리기), 앱 정보.
 *
 * 화면 설정은 앱 전체가 보는 값이라 바꾸는 즉시 고객 목록·상세에 반영된다.
 */
class SettingViewModel(
    private val useCases: SettingUseCases,
) : ViewModel() {

    val uiState: StateFlow<SettingUiState> = combine(
        useCases.observeListDensity(),
        useCases.observeMaskPersonalInfo(),
        useCases.observeStaff(),
    ) { density, mask, staff ->
        SettingUiState(
            staff = staff,
            listDensity = density,
            maskPersonalInfo = mask,
            appInfo = useCases.getAppInfo(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingUiState())

    fun onAction(action: SettingActions) {
        when (action) {
            is SettingActions.OnListDensityChange -> useCases.setListDensity(action.density)
            is SettingActions.OnMaskPersonalInfoChange -> useCases.setMaskPersonalInfo(action.mask)
            SettingActions.OnLogoutClick -> useCases.logout()
        }
    }
}
