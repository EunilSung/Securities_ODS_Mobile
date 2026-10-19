package com.eunilsung.ods.ui.uikit.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.click.clickable
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.chevron_left_icon
import ods.composeapp.generated.resources.chevron_right_icon
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
 * 한 주 달력 — 위에 연월과 이전·다음 주 버튼, 아래에 월~일 일곱 칸. 고른 날은 채워서, 오늘은 테두리로 보인다.
 *
 * @param counts 날짜별 건수. 있으면 날짜 아래에 점을 찍는다.
 * @param tagPrefix 화면 자동화 이름 앞에 붙인다. 태블릿에서 두 칸에 달력이 함께 떠도 가려 누를 수 있게 한다.
 */
@Composable
fun WeekStrip(
    days: List<LocalDate>,
    selected: LocalDate,
    today: LocalDate,
    counts: Map<LocalDate, Int>,
    onSelect: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.Space12, vertical = Dimens.Space8)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(Res.drawable.chevron_left_icon, stringResource(Res.string.week_previous), onPreviousWeek, "$tagPrefix${TestTags.WEEK_PREVIOUS}")
            Text(
                text = stringResource(Res.string.year_month, selected.year.toString(), selected.monthNumber.toString()),
                style = AppTypography.Section,
                color = AppColors.Text,
                modifier = Modifier.weight(1f).padding(horizontal = Dimens.Space4),
            )
            ArrowButton(Res.drawable.chevron_right_icon, stringResource(Res.string.week_next), onNextWeek, "$tagPrefix${TestTags.WEEK_NEXT}")
        }
        Spacer(Modifier.height(Dimens.Space4))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day ->
                DayCell(
                    day = day,
                    isSelected = day == selected,
                    isToday = day == today,
                    count = counts[day] ?: 0,
                    onClick = { onSelect(day) },
                    testTag = "$tagPrefix${TestTags.DAY_CELL_PREFIX}${day.dayOfMonth}",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, isSelected: Boolean, isToday: Boolean, count: Int, onClick: () -> Unit, testTag: String, modifier: Modifier) {
    val weekdayColor = when (day.dayOfWeek) {
        DayOfWeek.SUNDAY -> AppColors.Rise
        DayOfWeek.SATURDAY -> AppColors.Fall
        else -> AppColors.TextSub
    }
    Column(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = day.dayOfWeek.shortLabel(), style = AppTypography.Caption, color = weekdayColor)
        Spacer(Modifier.height(4.dp))
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
