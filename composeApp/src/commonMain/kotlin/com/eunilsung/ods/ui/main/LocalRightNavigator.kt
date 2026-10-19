package com.eunilsung.ods.ui.main

import androidx.compose.runtime.compositionLocalOf
import cafe.adriel.voyager.navigator.Navigator

/** 오른쪽(상세) 화면 스택. 폰에서는 목록 위로 밀려 들어오는 화면, 태블릿에서는 오른쪽 칸이다. */
val LocalRightNavigator = compositionLocalOf<Navigator> {
    error("LocalRightNavigator is not provided. Wrap usage with MainContent.")
}

/** 오른쪽에 상세 화면이 떠 있는지. 태블릿에서 목록의 선택 표시를 가를 때 쓴다. */
val LocalRightPaneOpen = compositionLocalOf { false }

/** 지금 그리는 레이아웃이 태블릿(좌우 2-pane)인지. */
val LocalIsTablet = compositionLocalOf { false }

/** 오른쪽 끝에 주문 칸이 떠 있는지(태블릿 가로 3칸). 떠 있으면 현재가의 매수·매도 버튼을 숨기고 호가를 누르면 주문 칸에 넣는다. */
val LocalOrderPaneVisible = compositionLocalOf { false }
