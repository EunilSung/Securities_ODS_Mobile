package com.eunilsung.ods.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.domain.model.Staff
import com.eunilsung.ods.ui.main.LocalIsTablet
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.setting.SettingActions
import com.eunilsung.ods.ui.setting.SettingUiState
import com.eunilsung.ods.ui.setting.SettingViewModel
import com.eunilsung.ods.ui.setting.component.SettingNavigateRow
import com.eunilsung.ods.ui.setting.component.SettingSectionHeader
import com.eunilsung.ods.ui.setting.detail.AppInfoScreen
import com.eunilsung.ods.ui.setting.detail.DisplaySettingScreen
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.density_comfortable
import ods.composeapp.generated.resources.density_compact
import ods.composeapp.generated.resources.more_info_section
import ods.composeapp.generated.resources.more_logout
import ods.composeapp.generated.resources.more_setting_section
import ods.composeapp.generated.resources.more_staff_branch
import ods.composeapp.generated.resources.more_staff_name
import ods.composeapp.generated.resources.setting_app_info
import ods.composeapp.generated.resources.setting_display
import ods.composeapp.generated.resources.tab_more
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 더보기 탭 — 맨 위에 로그인한 직원과 로그아웃, 그 아래 설정(화면)과 정보(앱 정보) 목록. 줄을 누르면 상세를 오른쪽 스택에 연다.
 *
 * 줄 오른쪽에 지금 값을 보여 상세에 들어가지 않고도 상태를 알 수 있게 한다.
 */
object MoreScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: SettingViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current
        val opened = if (LocalIsTablet.current) rightNavigator.lastItem else null

        MoreContent(
            uiState = uiState,
            openedItem = when (opened) {
                is DisplaySettingScreen -> MoreItem.DISPLAY
                is AppInfoScreen -> MoreItem.APP_INFO
                else -> null
            },
            onItemClick = { openDetail(rightNavigator, it) },
            onLogout = { viewModel.onAction(SettingActions.OnLogoutClick) },
        )
    }
}

enum class MoreItem { DISPLAY, APP_INFO }

private fun openDetail(rightNavigator: Navigator, item: MoreItem) {
    val screen: Screen = when (item) {
        MoreItem.DISPLAY -> DisplaySettingScreen()
        MoreItem.APP_INFO -> AppInfoScreen()
    }
    rightNavigator.popToEmptyRoot()
    rightNavigator.push(screen)
}

/**
 * 더보기 목록 — 상태 없이 값과 콜백만 받는다.
 *
 * @param openedItem 태블릿에서 오른쪽에 열린 상세. 폰에서는 null.
 */
@Composable
fun MoreContent(
    uiState: SettingUiState,
    openedItem: MoreItem?,
    onItemClick: (MoreItem) -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg)) {
        TopBar(title = stringResource(Res.string.tab_more))
        LineDivider()
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            uiState.staff?.let { staff ->
                StaffCard(staff = staff, onLogout = onLogout)
                LineDivider(thickness = Dimens.SectionGap, color = AppColors.Gray100Bg)
            }
            SettingSectionHeader(stringResource(Res.string.more_setting_section))
            SettingNavigateRow(
                title = stringResource(Res.string.setting_display),
                value = stringResource(if (uiState.listDensity == ListDensity.COMPACT) Res.string.density_compact else Res.string.density_comfortable),
                isSelected = openedItem == MoreItem.DISPLAY,
                onClick = { onItemClick(MoreItem.DISPLAY) },
            )
            LineDivider()
            SettingSectionHeader(stringResource(Res.string.more_info_section))
            SettingNavigateRow(
                title = stringResource(Res.string.setting_app_info),
                value = uiState.appInfo.versionName.ifBlank { null },
                isSelected = openedItem == MoreItem.APP_INFO,
                onClick = { onItemClick(MoreItem.APP_INFO) },
            )
            LineDivider()
        }
    }
}

/** 로그인한 직원 — 이름·직급과 소속 지점, 로그아웃. */
@Composable
private fun StaffCard(staff: Staff, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(Res.string.more_staff_name, staff.name, staff.position), style = AppTypography.Title, color = AppColors.Text)
            Text(
                text = stringResource(Res.string.more_staff_branch, staff.branch, staff.id),
                style = AppTypography.CaptionNumber,
                color = AppColors.TextSub,
            )
        }
        Text(
            text = stringResource(Res.string.more_logout),
            style = AppTypography.Caption,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Text,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, AppColors.Line, RoundedCornerShape(6.dp))
                .testTag(TestTags.MORE_LOGOUT)
                .clickable(onClick = onLogout)
                .padding(horizontal = Dimens.Space12, vertical = 6.dp),
        )
    }
}
