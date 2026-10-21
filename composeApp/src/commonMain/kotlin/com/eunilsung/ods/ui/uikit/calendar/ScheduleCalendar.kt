package com.eunilsung.ods.ui.uikit.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.CalendarMode
import com.eunilsung.ods.domain.util.calendarDays
import com.eunilsung.ods.domain.util.movedBy
import com.eunilsung.ods.domain.util.periodsFrom
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.calendar_mode_month
import ods.composeapp.generated.resources.calendar_mode_week
import ods.composeapp.generated.resources.chevron_left_icon
import ods.composeapp.generated.resources.chevron_right_icon
import ods.composeapp.generated.resources.month_next
import ods.composeapp.generated.resources.month_previous
import ods.composeapp.generated.resources.week_next
import ods.composeapp.generated.resources.week_previous
import ods.composeapp.generated.resources.weekday_fri
import ods.composeapp.generated.resources.weekday_mon
import ods.composeapp.generated.resources.weekday_sat
import ods.composeapp.generated.resources.weekday_sun
import ods.composeapp.generated.resources.weekday_thu
import ods.composeapp.generated.resources.weekday_tue
import ods.composeapp.generated.resources.weekday_wed
import ods.composeapp.generated.resources.year_month
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 일정 달력 — 위에 이전·다음 버튼, 연월, 주·월 전환. 아래에 한 주(월~일 일곱 칸) 또는 한 달(6주 42칸).
 * 고른 날은 채워서, 오늘은 테두리로 보인다. 달력을 좌우로 밀면 앞뒤 주·달이 손가락을 따라 넘어오고, 연월을 누르면 주·월이 바뀐다.
 *
 * 페이지마다 오늘이 든 주·달에서 몇 칸 떨어진 주·달을 그린다. 손으로 넘기면 [onPrevious]·[onNext] 로 알리고,
 * 화살표·오늘·날짜 고르기로 [selected] 가 바뀌면 그 페이지로 옮긴다(한 칸이면 넘어가는 모습을 보인다).
 *
 * @param counts 날짜별 건수. 있으면 날짜 아래에 점을 찍는다.
 * @param isSelectable 고를 수 있는 날. 아니면 흐리게 보이고 눌리지 않는다.
 * @param tagPrefix 화면 자동화 이름 앞에 붙인다. 태블릿에서 두 칸에 달력이 함께 떠도 가려 누를 수 있게 한다.
 */
@Composable
fun ScheduleCalendar(
    mode: CalendarMode,
    selected: LocalDate,
    today: LocalDate,
    counts: Map<LocalDate, Int>,
    onSelect: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onModeChange: (CalendarMode) -> Unit,
    onTitleClick: () -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
    isSelectable: (LocalDate) -> Boolean = { true },
) {
    val isMonth = mode == CalendarMode.MONTH
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.Space12, vertical = Dimens.Space8)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(
                icon = Res.drawable.chevron_left_icon,
                label = stringResource(if (isMonth) Res.string.month_previous else Res.string.week_previous),
                onClick = onPrevious,
                testTag = "$tagPrefix${TestTags.WEEK_PREVIOUS}",
            )
            Box(modifier = Modifier.weight(1f).padding(horizontal = Dimens.Space4)) {
                Text(
                    text = stringResource(Res.string.year_month, selected.year.toString(), selected.monthNumber.toString()),
                    style = AppTypography.Section,
                    color = AppColors.Text,
                    modifier = Modifier
                        .testTag("$tagPrefix${TestTags.CALENDAR_TITLE}")
                        .clickable(cornerRadius = 6.dp, onClick = onTitleClick)
                        .padding(horizontal = Dimens.Space4, vertical = 2.dp),
                )
            }
            SegmentedControl(
                options = CalendarMode.entries,
                selected = mode,
                label = { stringResource(if (it == CalendarMode.MONTH) Res.string.calendar_mode_month else Res.string.calendar_mode_week) },
                onSelect = onModeChange,
                optionTestTag = {
                    "$tagPrefix${if (it == CalendarMode.MONTH) TestTags.CALENDAR_MODE_MONTH else TestTags.CALENDAR_MODE_WEEK}"
                },
            )
            ArrowButton(
                icon = Res.drawable.chevron_right_icon,
                label = stringResource(if (isMonth) Res.string.month_next else Res.string.week_next),
                onClick = onNext,
                testTag = "$tagPrefix${TestTags.WEEK_NEXT}",
            )
        }
        Spacer(Modifier.height(Dimens.Space4))
        if (isMonth) {
            Row(modifier = Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { weekday ->
                    Text(
                        text = weekday.shortLabel(),
                        style = AppTypography.Caption,
                        color = weekday.labelColor(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    )
                }
            }
        }
        val previous by rememberUpdatedState(onPrevious)
        val next by rememberUpdatedState(onNext)
        val target = CENTER_PAGE + selected.periodsFrom(today, mode)
        val pagerState = rememberPagerState(initialPage = target) { PAGE_COUNT }
        var lastTarget by remember { mutableIntStateOf(target) }
        LaunchedEffect(target, pagerState.settledPage) {
            if (target != lastTarget) {
                lastTarget = target
                if (abs(pagerState.currentPage - target) == 1) pagerState.animateScrollToPage(target) else pagerState.scrollToPage(target)
            } else if (pagerState.settledPage != target) {
                val steps = pagerState.settledPage - target
                repeat(abs(steps)) { if (steps > 0) next() else previous() }
            }
        }
        HorizontalPager(state = pagerState, verticalAlignment = Alignment.Top) { page ->
            val period = today.movedBy(mode, page - CENTER_PAGE)
            Column {
                period.calendarDays(mode).chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val outside = isMonth && day.month != period.month
                            val selectable = isSelectable(day)
                            DayCell(
                                day = day,
                                showWeekday = !isMonth,
                                isSelected = day == selected,
                                isToday = day == today,
                                isDimmed = outside || !selectable,
                                count = counts[day] ?: 0,
                                onClick = if (selectable) ({ onSelect(day) }) else null,
                                testTag = "$tagPrefix${if (outside) TestTags.DAY_CELL_OUTSIDE_PREFIX else TestTags.DAY_CELL_PREFIX}${day.dayOfMonth}",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 달력 페이지 수와 가운데(오늘이 든 주·달) 페이지. 앞뒤로 몇십 년을 넘길 수 있을 만큼 둔다. */
private const val PAGE_COUNT = 20_000
private const val CENTER_PAGE = PAGE_COUNT / 2

@Composable
private fun DayCell(
    day: LocalDate,
    showWeekday: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    isDimmed: Boolean,
    count: Int,
    onClick: (() -> Unit)?,
    testTag: String,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (isDimmed && !isSelected) 0.35f else 1f)
            .padding(vertical = if (showWeekday) 6.dp else 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showWeekday) {
            Text(text = day.dayOfWeek.shortLabel(), style = AppTypography.Caption, color = day.dayOfWeek.labelColor())
            Spacer(Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isSelected) AppColors.PrimaryMain else AppColors.Transparent)
                .then(if (isToday && !isSelected) Modifier.border(1.dp, AppColors.PrimaryMain, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = AppTypography.BodyNumber,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) AppColors.White else AppColors.Text,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (count > 0) AppColors.PrimaryMain else AppColors.Transparent),
        )
    }
}

@Composable
private fun DayOfWeek.labelColor() = when (this) {
    DayOfWeek.SUNDAY -> AppColors.Rise
    DayOfWeek.SATURDAY -> AppColors.Fall
    else -> AppColors.TextSub
}

@Composable
private fun ArrowButton(icon: DrawableResource, label: String, onClick: () -> Unit, testTag: String) {
    Box(
        modifier = Modifier.size(36.dp).testTag(testTag).clickable(cornerRadius = 18.dp, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(icon), contentDescription = label, tint = AppColors.Text, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun DayOfWeek.shortLabel(): String = stringResource(
    when (this) {
        DayOfWeek.MONDAY -> Res.string.weekday_mon
        DayOfWeek.TUESDAY -> Res.string.weekday_tue
        DayOfWeek.WEDNESDAY -> Res.string.weekday_wed
        DayOfWeek.THURSDAY -> Res.string.weekday_thu
        DayOfWeek.FRIDAY -> Res.string.weekday_fri
        DayOfWeek.SATURDAY -> Res.string.weekday_sat
        else -> Res.string.weekday_sun
    }
)
