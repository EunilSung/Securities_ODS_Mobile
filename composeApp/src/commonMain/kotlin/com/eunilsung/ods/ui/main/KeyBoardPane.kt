package com.eunilsung.ods.ui.main

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 키보드 표시 여부와 무관하게 안정적인 하단 인셋(내비바/홈인디케이터). */
@Composable
expect fun keyboardBottomInset(): Dp

/**
 * 화면 맨 아래 키보드 자리.
 *
 * 루트는 하단 인셋을 소비하지 않으므로(edge-to-edge) 이 칸이 키보드 높이, 키보드가 없으면 내비바 높이만큼 채운다.
 *
 * @param fillIdleBottomInset 키보드가 없을 때 내비바·홈 인디케이터 높이를 채울지.
 */
@Composable
fun KeyBoardPane(
    modifier: Modifier = Modifier,
    isKeyboardVisible: Boolean,
    imeHeight: Dp,
    fillIdleBottomInset: Boolean = false,
) {
    val idleFloor = if (fillIdleBottomInset) keyboardBottomInset() else 0.dp
    val target = if (isKeyboardVisible) maxOf(imeHeight, idleFloor) else idleFloor
    val height by animateDpAsState(targetValue = target, animationSpec = tween(200), label = "keyboardPane")
    Spacer(modifier = modifier.fillMaxWidth().height(height))
}
