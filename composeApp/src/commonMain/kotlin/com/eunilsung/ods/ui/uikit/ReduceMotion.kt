package com.eunilsung.ods.ui.uikit

import androidx.compose.runtime.Composable

/** 기기에서 동작 줄이기(애니메이션 끄기)를 켰는지. 켜져 있으면 깜빡임 같은 장식 동작을 하지 않는다. */
@Composable
expect fun rememberReduceMotion(): Boolean
