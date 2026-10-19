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
import com.eunilsung.ods.ui.setting.SettingActions
import com.eunilsung.ods.ui.setting.SettingUiState
import com.eunilsung.ods.ui.setting.SettingViewModel
import com.eunilsung.ods.ui.setting.component.SettingValueRow
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.setting_app_info
import ods.composeapp.generated.resources.setting_app_version
import ods.composeapp.generated.resources.setting_data_demo
import ods.composeapp.generated.resources.setting_data_demo_description
import ods.composeapp.generated.resources.setting_data_live
import ods.composeapp.generated.resources.setting_data_source
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 앱 정보 — 버전, 시세·계좌 데이터가 시연용인지 실제인지. */
class AppInfoScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: SettingViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        AppInfoContent(uiState, viewModel::onAction)
    }
}

@Composable
fun AppInfoContent(uiState: SettingUiState, onAction: (SettingActions) -> Unit) {
    val info = uiState.appInfo
    SettingDetailScaffold(title = stringResource(Res.string.setting_app_info), uiState = uiState, onAction = onAction) {
        SettingValueRow(
            label = stringResource(Res.string.setting_app_version),
            value = if (info.versionName.isBlank()) "-" else "${info.versionName} (${info.versionCode})",
        )
        LineDivider()
        SettingValueRow(
            label = stringResource(Res.string.setting_data_source),
            value = stringResource(if (info.usesDemoData) Res.string.setting_data_demo else Res.string.setting_data_live),
        )
        LineDivider()
        if (info.usesDemoData) {
            Text(
                text = stringResource(Res.string.setting_data_demo_description),
                style = AppTypography.Caption,
                color = AppColors.TextSub,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space16),
            )
        }
    }
}
