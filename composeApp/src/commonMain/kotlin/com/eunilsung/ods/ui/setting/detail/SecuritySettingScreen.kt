package com.eunilsung.ods.ui.setting.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.domain.util.AppLockPolicy
import com.eunilsung.ods.ui.applock.AppLockPurpose
import com.eunilsung.ods.ui.applock.AppLockScreen
import com.eunilsung.ods.ui.applock.BiometricPromptText
import com.eunilsung.ods.ui.setting.SettingActions
import com.eunilsung.ods.ui.setting.SettingNotice
import com.eunilsung.ods.ui.setting.SettingUiState
import com.eunilsung.ods.ui.setting.SettingViewModel
import com.eunilsung.ods.ui.setting.component.SettingNavigateRow
import com.eunilsung.ods.ui.setting.component.SettingNoticeBanner
import com.eunilsung.ods.ui.setting.component.SettingSectionHeader
import com.eunilsung.ods.ui.setting.component.SettingToggleRow
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.exposeTestTags
import com.eunilsung.ods.ui.uikit.line.LineDivider
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.app_lock_biometric_negative
import ods.composeapp.generated.resources.setting_security
import ods.composeapp.generated.resources.setting_security_biometric
import ods.composeapp.generated.resources.setting_security_biometric_description
import ods.composeapp.generated.resources.setting_security_biometric_prompt_subtitle
import ods.composeapp.generated.resources.setting_security_biometric_prompt_title
import ods.composeapp.generated.resources.setting_security_biometric_unavailable
import ods.composeapp.generated.resources.setting_security_change_pin
import ods.composeapp.generated.resources.setting_security_lock_now
import ods.composeapp.generated.resources.setting_security_lock_section
import ods.composeapp.generated.resources.setting_security_notice_biometric_enabled
import ods.composeapp.generated.resources.setting_security_notice_biometric_failed
import ods.composeapp.generated.resources.setting_security_notice_pin_changed
import ods.composeapp.generated.resources.setting_security_policy
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 보안 설정 — 잠금 비밀번호 변경, 생체 인증, 지금 잠그기, 자동 잠금 규칙 안내. */
class SecuritySettingScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: SettingViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { viewModel.onAction(SettingActions.OnEnter) }
        SecuritySettingContent(uiState, viewModel::onAction)
    }
}

@Composable
fun SecuritySettingContent(uiState: SettingUiState, onAction: (SettingActions) -> Unit) {
    val prompt = BiometricPromptText(
        title = stringResource(Res.string.setting_security_biometric_prompt_title),
        subtitle = stringResource(Res.string.setting_security_biometric_prompt_subtitle),
        negative = stringResource(Res.string.app_lock_biometric_negative),
    )
    SettingDetailScaffold(title = stringResource(Res.string.setting_security)) {
        uiState.notice?.let { notice ->
            SettingNoticeBanner(
                stringResource(
                    when (notice) {
                        SettingNotice.PIN_CHANGED -> Res.string.setting_security_notice_pin_changed
                        SettingNotice.BIOMETRIC_ENABLED -> Res.string.setting_security_notice_biometric_enabled
                        SettingNotice.BIOMETRIC_FAILED -> Res.string.setting_security_notice_biometric_failed
                    }
                )
            )
        }
        SettingSectionHeader(stringResource(Res.string.setting_security_lock_section))
        SettingNavigateRow(
            title = stringResource(Res.string.setting_security_change_pin),
            onClick = { onAction(SettingActions.OnChangePinClick) },
        )
        LineDivider()
        SettingToggleRow(
            title = stringResource(Res.string.setting_security_biometric),
            description = stringResource(
                if (uiState.biometricAvailable) Res.string.setting_security_biometric_description
                else Res.string.setting_security_biometric_unavailable
            ),
            checked = uiState.biometricEnabled && uiState.biometricAvailable,
            enabled = uiState.biometricAvailable,
            onCheckedChange = { onAction(SettingActions.OnBiometricToggle(it, prompt)) },
        )
        LineDivider()
        SettingNavigateRow(
            title = stringResource(Res.string.setting_security_lock_now),
            modifier = Modifier.testTag(TestTags.SETTING_LOCK_NOW),
            onClick = { onAction(SettingActions.OnLockNowClick) },
        )
        LineDivider()
        Text(
            text = stringResource(
                Res.string.setting_security_policy,
                (AppLockPolicy.IDLE_LOCK_SECONDS / 60).toString(),
                AppLockPolicy.BACKGROUND_LOCK_SECONDS.toString(),
                AppLockPolicy.MAX_ATTEMPTS.toString(),
            ),
            style = AppTypography.Caption,
            color = AppColors.TextSub,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space16),
        )
    }

    if (uiState.isChangingPin) {
        Dialog(
            onDismissRequest = { onAction(SettingActions.OnChangePinClose) },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        ) {
            Box(modifier = Modifier.exposeTestTags()) {
                AppLockScreen(
                    purpose = AppLockPurpose.CHANGE,
                    onClose = { onAction(SettingActions.OnChangePinClose) },
                    onPinChanged = { onAction(SettingActions.OnPinChanged) },
                )
            }
        }
    }
}
