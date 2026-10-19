package com.eunilsung.ods.ui.uikit.line

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppColors

@Composable
fun LineDivider(
    modifier: Modifier = Modifier,
    color: Color = AppColors.Line,
    thickness: Dp = 1.dp,
    isWidth: Boolean = false,
) {
    Box(modifier = (if (isWidth) modifier.width(thickness) else modifier.height(thickness)).background(color))
}
