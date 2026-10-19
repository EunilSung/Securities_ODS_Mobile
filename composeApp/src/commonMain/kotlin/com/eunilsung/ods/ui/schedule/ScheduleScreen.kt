package com.eunilsung.ods.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.ui.main.LocalIsTablet
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.schedule.detail.VisitDetailScreen
import com.eunilsung.ods.ui.schedule.item.VisitItem
import com.eunilsung.ods.ui.schedule.register.NewVisitScreen
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import com.eunilsung.ods.ui.uikit.calendar.WeekStrip
import com.eunilsung.ods.ui.uikit.calendar.shortLabel
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import com.eunilsung.ods.ui.util.formatDate
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.add_icon
import ods.composeapp.generated.resources.schedule_add
import ods.composeapp.generated.resources.schedule_day_count
import ods.composeapp.generated.resources.schedule_empty
import ods.composeapp.generated.resources.schedule_today
import ods.composeapp.generated.resources.tab_schedule
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 일정 탭 — 한 주 달력과 고른 날의 방문. 방문을 누르면 오른쪽 스택에 상세를 연다. */
object ScheduleScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: ScheduleViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current
        val openedId = (rightNavigator.lastItem as? VisitDetailScreen)?.visitId

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is ScheduleEvent.OpenVisit -> {
                        rightNavigator.popToEmptyRoot()
                        rightNavigator.push(VisitDetailScreen(event.id))
                    }
                    is ScheduleEvent.OpenNewVisit -> {
                        rightNavigator.popToEmptyRoot()
                        rightNavigator.push(NewVisitScreen(date = event.date))
                    }
                }
            }
        }

        ScheduleContent(uiState = uiState, selectedVisitId = if (LocalIsTablet.current) openedId else null, onAction = viewModel::onAction)
    }
}

/**
 * 일정 화면 전체 — 상태 없이 값과 콜백만 받는다.
 *
 * @param selectedVisitId 태블릿에서 오른쪽에 열린 방문. 폰에서는 null.
 */
@Composable
fun ScheduleContent(uiState: ScheduleUiState, selectedVisitId: String?, onAction: (ScheduleActions) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(Res.string.tab_schedule),
            actions = {
                if (uiState.selected != uiState.today) {
                    OutlineButton(text = stringResource(Res.string.schedule_today), onClick = { onAction(ScheduleActions.OnToday) })
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .testTag(TestTags.SCHEDULE_ADD)
                        .clickable(cornerRadius = 20.dp, onClick = { onAction(ScheduleActions.OnAddClick) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.add_icon),
                        contentDescription = stringResource(Res.string.schedule_add),
                        tint = AppColors.Text,
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        )
        WeekStrip(
            days = uiState.weekDays,
            selected = uiState.selected,
            today = uiState.today,
            counts = uiState.counts,
            onSelect = { onAction(ScheduleActions.OnSelectDate(it)) },
            onPreviousWeek = { onAction(ScheduleActions.OnPreviousWeek) },
            onNextWeek = { onAction(ScheduleActions.OnNextWeek) },
            tagPrefix = TestTags.SCHEDULE_PREFIX,
        )
        LineDivider()
        val active = uiState.visits.count { it.status != VisitStatus.CANCELED }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8)) {
            Text(
                text = stringResource(
                    Res.string.schedule_day_count,
                    "${formatDate(uiState.selected)} (${uiState.selected.dayOfWeek.shortLabel()})",
                    active,
                ),
                style = AppTypography.Caption,
                color = AppColors.TextSub,
            )
        }
        LineDivider()
        if (uiState.visits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(Res.string.schedule_empty), color = AppColors.TextSub)
            }
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.visits, key = { it.id }) { visit ->
                VisitItem(visit = visit, isSelected = visit.id == selectedVisitId, onClick = { onAction(ScheduleActions.OnVisitClick(visit.id)) })
                LineDivider(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))
            }
        }
    }
}
