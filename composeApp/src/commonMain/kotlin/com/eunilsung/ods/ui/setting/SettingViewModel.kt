package com.eunilsung.ods.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eunilsung.ods.domain.usecase.SettingUseCases
import com.eunilsung.ods.ui.orderauth.OrderAuthPurpose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * 더보기 설정 — 로그인한 고객·로그아웃, 주문 인증(생체 켜기·끄기, 비밀번호 변경), 화면(목록 밀도·금액 가리기), 앱 정보.
 *
 * 생체 인증을 켤 때는 비밀번호로 본인 확인을 먼저 받는다(비밀번호가 없으면 그 자리에서 등록). 끄는 것은 확인 없이 바로 한다.
 * 화면 설정은 앱 전체가 보는 값이라 바꾸는 즉시 관심종목·잔고에 반영된다.
 */
class SettingViewModel(
    private val useCases: SettingUseCases,
) : ViewModel() {

    private val local = MutableStateFlow(LocalState(biometricAvailable = useCases.isBiometricAvailable()))

    val uiState: StateFlow<SettingUiState> = combine(
        useCases.observeListDensity(),
        useCases.observeHideAmounts(),
        useCases.observeOrderAuthConfig(),
        useCases.observeCustomer(),
        local,
    ) { density, hide, auth, customer, state ->
        SettingUiState(
            customer = customer,
            listDensity = density,
            hideAmounts = hide,
            isPinSet = auth.isPinSet,
            biometricEnabled = auth.biometricEnabled,
            biometricAvailable = state.biometricAvailable,
            lockedUntilEpochSeconds = useCases.getOrderAuthLock(),
            appInfo = useCases.getAppInfo(),
            authPurpose = state.authPurpose,
            notice = state.notice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingUiState())

    fun onAction(action: SettingActions) {
        when (action) {
            SettingActions.OnEnter -> local.update { it.copy(biometricAvailable = useCases.isBiometricAvailable(), notice = null) }
            is SettingActions.OnListDensityChange -> useCases.setListDensity(action.density)
            is SettingActions.OnHideAmountsChange -> useCases.setHideAmounts(action.hide)
            is SettingActions.OnBiometricToggle ->
                if (action.enable) {
                    if (local.value.biometricAvailable) local.update { it.copy(authPurpose = OrderAuthPurpose.VERIFY_PIN, notice = null) }
                } else {
                    useCases.setOrderBiometricEnabled(false)
                }
            SettingActions.OnChangePinClick -> local.update {
                it.copy(authPurpose = OrderAuthPurpose.CHANGE_PIN, pinSetWhenOpened = uiState.value.isPinSet, notice = null)
            }
            SettingActions.OnAuthDone -> authDone()
            SettingActions.OnAuthCanceled -> local.update { it.copy(authPurpose = null) }
            SettingActions.OnLogoutClick -> {
                local.update { it.copy(authPurpose = null, notice = null) }
                useCases.logout()
            }
        }
    }

    private fun authDone() {
        val purpose = local.value.authPurpose ?: return
        val notice = when (purpose) {
            OrderAuthPurpose.VERIFY_PIN -> {
                useCases.setOrderBiometricEnabled(true)
                SettingNotice.BIOMETRIC_ENABLED
            }
            OrderAuthPurpose.CHANGE_PIN -> if (local.value.pinSetWhenOpened) SettingNotice.PIN_CHANGED else SettingNotice.PIN_REGISTERED
            OrderAuthPurpose.AUTHORIZE -> null
        }
        local.update { it.copy(authPurpose = null, notice = notice) }
    }

    /** 이 화면에만 있는 값. */
    private data class LocalState(
        val biometricAvailable: Boolean,
        val authPurpose: OrderAuthPurpose? = null,
        /** 비밀번호 창을 열 때 이미 등록돼 있었는지 — 끝난 뒤 "바꿨다"·"등록했다" 를 가른다. */
        val pinSetWhenOpened: Boolean = false,
        val notice: SettingNotice? = null,
    )
}
