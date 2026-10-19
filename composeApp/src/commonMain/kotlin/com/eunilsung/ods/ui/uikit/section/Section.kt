package com.eunilsung.ods.ui.uikit.section

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider

/** 상세 화면 구역 제목 — 왼쪽 제목, 오른쪽 끝에 붙는 버튼. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = AppTypography.Section, color = AppColors.Text, modifier = Modifier.weight(1f))
        action()
    }
}

/** 이름 · 값 한 줄. 이름은 고정 폭이라 값이 세로로 줄 맞는다. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = AppColors.Text) {
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = 6.dp)) {
        Text(text = label, style = AppTypography.Body, color = AppColors.TextSub, modifier = Modifier.width(96.dp))
        Text(text = value, style = AppTypography.Body, color = valueColor, modifier = Modifier.weight(1f))
    }
}

/** 구역 사이의 두꺼운 띠. */
@Composable
fun SectionGap() {
    LineDivider(thickness = Dimens.SectionGap, color = AppColors.Gray100Bg)
}
