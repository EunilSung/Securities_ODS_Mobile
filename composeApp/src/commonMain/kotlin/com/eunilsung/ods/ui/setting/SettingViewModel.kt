package com.eunilsung.ods.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.usecase.SettingUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 더보기 설정 — 로그인한 직원·로그아웃, 보안(잠금 비밀번호·생체·지금 잠그기), 화면(목록 밀도·개인정보 가리기), 앱 정보.
 *
 * 생체 인증은 켤 때 한 번 통과해야 켜진다 — 기기에 등록된 생체가 이 직원 것인지 확인하는 셈이다. 끄는 것은 바로 한다.
 * 화면 설정은 앱 전체가 보는 값이라 바꾸는 즉시 고객 목록·상세에 반영된다.
 */
class SettingViewModel(
    private val useCases: SettingUseCases,
) : ViewModel() {

    private val local = MutableStateFlow(LocalState(biometricAvailable = useCases.isBiometricAvailable()))

    val uiState: StateFlow<SettingUiState> = combine(
        useCases.observeListDensity(),
        useCases.observeMaskPersonalInfo(),
        useCases.observeStaff(),
        useCases.observeAppLock(),
        local,
    ) { density, mask, staff, lock, state ->
        SettingUiState(
            staff = staff,
            listDensity = density,
            maskPersonalInfo = mask,
            biometricEnabled = lock.biometricEnabled,
            biometricAvailable = state.biometricAvailable,
            isChangingPin = state.isChangingPin,
            appInfo = useCases.getAppInfo(),
            notice = state.notice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingUiState())

    fun onAction(action: SettingActions) {
        when (action) {
            SettingActions.OnEnter -> local.update { it.copy(biometricAvailable = useCases.isBiometricAvailable(), notice = null) }
            is SettingActions.OnListDensityChange -> useCases.setListDensity(action.density)
            is SettingActions.OnMaskPersonalInfoChange -> useCases.setMaskPersonalInfo(action.mask)
            is SettingActions.OnBiometricToggle ->
                if (action.enable) enableBiometric(action) else useCases.setAppBiometricEnabled(false)
            SettingActions.OnChangePinClick -> local.update { it.copy(isChangingPin = true, notice = null) }
            SettingActions.OnPinChanged -> local.update { it.copy(isChangingPin = false, notice = SettingNotice.PIN_CHANGED) }
            SettingActions.OnChangePinClose -> local.update { it.copy(isChangingPin = false) }
            SettingActions.OnLockNowClick -> useCases.lockApp()
            SettingActions.OnLogoutClick -> {
                local.update { it.copy(isChangingPin = false, notice = null) }
                useCases.logout()
            }
        }
    }

    private fun enableBiometric(action: SettingActions.OnBiometricToggle) {
        if (!local.value.biometricAvailable) return
        viewModelScope.launch {
            val result = useCases.authenticateBiometric(action.prompt.title, action.prompt.subtitle, action.prompt.negative)
            if (result == BiometricResult.SUCCESS) {
                useCases.setAppBiometricEnabled(true)
                local.update { it.copy(notice = SettingNotice.BIOMETRIC_ENABLED) }
            } else if (result != BiometricResult.CANCELED) {
                local.update { it.copy(notice = SettingNotice.BIOMETRIC_FAILED) }
            }
        }
    }

    /** 이 화면에만 있는 값. */
    private data class LocalState(
        val biometricAvailable: Boolean,
        val isChangingPin: Boolean = false,
        val notice: SettingNotice? = null,
    )
}
