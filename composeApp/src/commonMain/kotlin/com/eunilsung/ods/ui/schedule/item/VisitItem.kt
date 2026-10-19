package com.eunilsung.ods.ui.schedule.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.ui.schedule.color
import com.eunilsung.ods.ui.schedule.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.theme.LocalListDensity
import com.eunilsung.ods.ui.theme.tokens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.badge.Badge
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.util.formatTime

/**
 * 방문 한 건 — 왼쪽 시각, 가운데 고객·장소, 오른쪽 목적과 상태. 취소한 일정은 흐리게 긋는다.
 *
 * @param isSelected 태블릿에서 오른쪽에 열린 일정이면 배경을 칠한다.
 */
@Composable
fun VisitItem(visit: Visit, isSelected: Boolean, onClick: () -> Unit) {
    val tokens = LocalListDensity.current.tokens
    val canceled = visit.status == VisitStatus.CANCELED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.VISIT_ITEM)
            .background(if (isSelected) AppColors.PrimaryBg else AppColors.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = tokens.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatTime(visit.scheduledAtMillis),
            style = AppTypography.ListPrice,
            color = if (canceled) AppColors.TextDisabled else AppColors.Text,
            modifier = Modifier.width(64.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = visit.customerName,
                style = tokens.nameStyle,
                color = if (canceled) AppColors.TextDisabled else AppColors.Text,
                textDecoration = if (canceled) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = visit.place, style = tokens.subStyle, color = AppColors.TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Badge(text = visit.purpose.label(), color = if (canceled) AppColors.TextDisabled else AppColors.Text)
            Badge(text = visit.status.label(), color = visit.status.color)
        }
    }
}
