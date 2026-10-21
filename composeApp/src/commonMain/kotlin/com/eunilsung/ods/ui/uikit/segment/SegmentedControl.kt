package com.eunilsung.ods.ui.uikit.segment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.uikit.click.clickable

/**
 * 몇 개 중 하나를 고르는 버튼 묶음 — 차트 기간, 목록 밀도.
 *
 * @param label 항목마다 보일 이름.
 * @param optionTestTag 항목마다 붙일 화면 자동화 이름. null 이면 붙이지 않는다.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionTestTag: (T) -> String? = { null },
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, AppColors.Line, shape),
    ) {
        for (option in options) {
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .then(optionTestTag(option)?.let { Modifier.testTag(it) } ?: Modifier)
                    .background(if (isSelected) AppColors.PrimaryMain else AppColors.Transparent)
                    .clickable(onClick = { onSelect(option) })
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = AppTypography.Caption,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) AppColors.White else AppColors.TextSub,
                )
            }
        }
    }
}
