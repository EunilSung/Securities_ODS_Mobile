package com.eunilsung.ods.ui.uikit

import androidx.compose.runtime.Composable

/** iOS 는 시스템 뒤로 가기 버튼이 없다. 폰 상세 화면은 왼쪽 가장자리에서 밀어 닫는다. */
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
}
