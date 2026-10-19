package com.eunilsung.ods.ui.uikit.stock

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.uikit.rememberReduceMotion

private const val FLASH_FADE_MILLIS = 600

/**
 * 체결 깜빡임 — [value] 가 바뀌면 바탕을 잠깐 칠했다가 지운다. 오르면 상승색, 내리면 하락색.
 *
 * 처음 그릴 때는 칠하지 않는다. 기기에서 동작 줄이기를 켰으면 칠하지 않는다.
 * 색은 그리기 단계에서만 읽으므로 깜빡이는 동안 행이 다시 구성되지 않는다.
 */
@Composable
fun Modifier.tickFlash(value: Long): Modifier {
    val reduceMotion = rememberReduceMotion()
    val riseColor = AppColors.RiseBg
    val fallColor = AppColors.FallBg
    val flash = remember { Animatable(Color.Transparent) }
    var previous by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        val before = previous
        previous = value
        if (reduceMotion || before == value) return@LaunchedEffect
        val color = if (value > before) riseColor else fallColor
        flash.snapTo(color)
        flash.animateTo(color.copy(alpha = 0f), tween(FLASH_FADE_MILLIS))
    }
    return drawBehind { drawRect(flash.value) }
}
