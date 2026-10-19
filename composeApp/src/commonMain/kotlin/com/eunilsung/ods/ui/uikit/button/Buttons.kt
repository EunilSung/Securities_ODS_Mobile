package com.eunilsung.ods.ui.uikit.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable

/** 화면 아래의 큰 확인 버튼. 막혀 있으면 회색이고 눌리지 않는다. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled || loading) AppColors.PrimaryMain else AppColors.Gray100Bg)
            .then(if (enabled && !loading) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = AppColors.White)
        } else {
            Text(
                text = text,
                style = AppTypography.Section,
                fontWeight = FontWeight.Bold,
                color = if (enabled) AppColors.White else AppColors.TextDisabled,
            )
        }
    }
}

/** 테두리만 있는 작은 버튼 — 화면 안의 보조 동작(상담 기록·진단 시작). */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Text(
        text = text,
        style = AppTypography.Body,
        fontWeight = FontWeight.SemiBold,
        color = if (enabled) AppColors.PrimaryMain else AppColors.TextDisabled,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (enabled) AppColors.PrimaryMain else AppColors.Line, RoundedCornerShape(8.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
    )
}
