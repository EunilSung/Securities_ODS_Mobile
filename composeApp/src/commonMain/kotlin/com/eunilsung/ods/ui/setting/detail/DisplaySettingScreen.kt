package com.eunilsung.ods.ui.setting.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.ui.setting.SettingActions
import com.eunilsung.ods.ui.setting.SettingUiState
import com.eunilsung.ods.ui.setting.SettingViewModel
import com.eunilsung.ods.ui.setting.component.SettingRadioRow
import com.eunilsung.ods.ui.setting.component.SettingSectionHeader
import com.eunilsung.ods.ui.setting.component.SettingToggleRow
import com.eunilsung.ods.ui.uikit.line.LineDivider
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.density_comfortable
import ods.composeapp.generated.resources.density_compact
import ods.composeapp.generated.resources.setting_density_comfortable_description
import ods.composeapp.generated.resources.setting_density_compact_description
import ods.composeapp.generated.resources.setting_density_section
import ods.composeapp.generated.resources.setting_display
import ods.composeapp.generated.resources.setting_hide_amounts_description
import ods.composeapp.generated.resources.setting_hide_amounts_section
import ods.composeapp.generated.resources.balance_hide_amounts
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 화면 설정 — 목록 밀도, 잔고 금액 가리기. 바꾸면 바로 앱 전체에 반영된다. */
class DisplaySettingScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: SettingViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        DisplaySettingContent(uiState, viewModel::onAction)
    }
}

@Composable
fun DisplaySettingContent(uiState: SettingUiState, onAction: (SettingActions) -> Unit) {
    SettingDetailScaffold(title = stringResource(Res.string.setting_display), uiState = uiState, onAction = onAction) {
        SettingSectionHeader(stringResource(Res.string.setting_density_section))
        SettingRadioRow(
            title = stringResource(Res.string.density_comfortable),
            description = stringResource(Res.string.setting_density_comfortable_description),
            selected = uiState.listDensity == ListDensity.COMFORTABLE,
            onClick = { onAction(SettingActions.OnListDensityChange(ListDensity.COMFORTABLE)) },
        )
        SettingRadioRow(
            title = stringResource(Res.string.density_compact),
            description = stringResource(Res.string.setting_density_compact_description),
            selected = uiState.listDensity == ListDensity.COMPACT,
            onClick = { onAction(SettingActions.OnListDensityChange(ListDensity.COMPACT)) },
        )
        LineDivider()

        SettingSectionHeader(stringResource(Res.string.setting_hide_amounts_section))
        SettingToggleRow(
            title = stringResource(Res.string.balance_hide_amounts),
            description = stringResource(Res.string.setting_hide_amounts_description),
            checked = uiState.hideAmounts,
            onCheckedChange = { onAction(SettingActions.OnHideAmountsChange(it)) },
        )
        LineDivider()
    }
}
