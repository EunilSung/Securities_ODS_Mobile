package com.eunilsung.ods.ui.uikit.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppTypography

/** 작은 이름표 — 고객 등급, 투자성향, 상품 위험등급. 바탕은 글자색을 옅게 깐다. */
@Composable
fun Badge(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypography.Caption,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
