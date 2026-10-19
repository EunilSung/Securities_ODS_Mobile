package com.eunilsung.ods.ui.setting.detail

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.domain.util.OrderPinPolicy
import com.eunilsung.ods.ui.setting.SettingActions
import com.eunilsung.ods.ui.setting.SettingNotice
import com.eunilsung.ods.ui.setting.SettingUiState
import com.eunilsung.ods.ui.setting.SettingViewModel
import com.eunilsung.ods.ui.setting.component.SettingNavigateRow
import com.eunilsung.ods.ui.setting.component.SettingNoticeBanner
import com.eunilsung.ods.ui.setting.component.SettingSectionHeader
import com.eunilsung.ods.ui.setting.component.SettingToggleRow
import com.eunilsung.ods.ui.setting.component.SettingValueRow
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.util.formatOrderTime
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.setting_auth_biometric_section
import ods.composeapp.generated.resources.setting_auth_biometric_title
import ods.composeapp.generated.resources.setting_auth_biometric_description
import ods.composeapp.generated.resources.setting_auth_biometric_unavailable
import ods.composeapp.generated.resources.setting_auth_change_pin
import ods.composeapp.generated.resources.setting_auth_lock_note
import ods.composeapp.generated.resources.setting_auth_locked_until
import ods.composeapp.generated.resources.setting_auth_pin_section
import ods.composeapp.generated.resources.setting_auth_pin_set
import ods.composeapp.generated.resources.setting_auth_pin_status
import ods.composeapp.generated.resources.setting_auth_pin_unset
import ods.composeapp.generated.resources.setting_auth_register_pin
import ods.composeapp.generated.resources.setting_notice_biometric_enabled
import ods.composeapp.generated.resources.setting_notice_pin_changed
import ods.composeapp.generated.resources.setting_notice_pin_registered
import ods.composeapp.generated.resources.setting_order_auth
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 주문 인증 설정 — 비밀번호 상태·변경, 생체 인증 켜기·끄기. */
class OrderAuthSettingScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: SettingViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        OrderAuthSettingContent(uiState, viewModel::onAction)
    }
}

@Composable
fun OrderAuthSettingContent(uiState: SettingUiState, onAction: (SettingActions) -> Unit) {
    SettingDetailScaffold(title = stringResource(Res.string.setting_order_auth), uiState = uiState, onAction = onAction) {
        uiState.notice?.let { notice ->
            SettingNoticeBanner(
                stringResource(
                    when (notice) {
                        SettingNotice.PIN_CHANGED -> Res.string.setting_notice_pin_changed
                        SettingNotice.PIN_REGISTERED -> Res.string.setting_notice_pin_registered
                        SettingNotice.BIOMETRIC_ENABLED -> Res.string.setting_notice_biometric_enabled
                    }
                )
            )
        }

        SettingSectionHeader(stringResource(Res.string.setting_auth_pin_section))
        val locked = uiState.lockedUntilEpochSeconds
        SettingValueRow(
            label = stringResource(Res.string.setting_auth_pin_status),
            value = when {
                locked != null -> stringResource(Res.string.setting_auth_locked_until, formatOrderTime(locked))
                uiState.isPinSet -> stringResource(Res.string.setting_auth_pin_set)
                else -> stringResource(Res.string.setting_auth_pin_unset)
            },
        )
        LineDivider()
        SettingNavigateRow(
            title = stringResource(if (uiState.isPinSet) Res.string.setting_auth_change_pin else Res.string.setting_auth_register_pin),
            onClick = { onAction(SettingActions.OnChangePinClick) },
        )
        LineDivider()

        SettingSectionHeader(stringResource(Res.string.setting_auth_biometric_section))
        SettingToggleRow(
            title = stringResource(Res.string.setting_auth_biometric_title),
            description = stringResource(
                if (uiState.biometricAvailable) Res.string.setting_auth_biometric_description
                else Res.string.setting_auth_biometric_unavailable
            ),
            checked = uiState.biometricEnabled && uiState.biometricAvailable,
            enabled = uiState.biometricAvailable && locked == null,
            onCheckedChange = { onAction(SettingActions.OnBiometricToggle(it)) },
        )
        LineDivider()

        Text(
            text = stringResource(
                Res.string.setting_auth_lock_note,
                OrderPinPolicy.MAX_ATTEMPTS.toString(),
                (OrderPinPolicy.LOCK_SECONDS / 60).toString(),
            ),
            style = AppTypography.Caption,
            color = AppColors.TextSub,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space16),
        )
    }
}
