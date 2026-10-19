package com.eunilsung.ods.ui.uikit.swipe

import androidx.compose.runtime.compositionLocalOf

/** 폰에서 상세 화면을 옆으로 밀어 닫을 때, 화면 안쪽(가로 스크롤 영역 등)이 진행 상황을 넘겨주는 통로. */
class SwipeDismissController(
    val onSwipeStart: () -> Unit = {},
    val onSwipeProgress: (totalDx: Float) -> Unit = {},
    val onSwipeEnd: (velocityX: Float) -> Unit = {},
)

val LocalSwipeDismiss = compositionLocalOf<SwipeDismissController?> { null }
