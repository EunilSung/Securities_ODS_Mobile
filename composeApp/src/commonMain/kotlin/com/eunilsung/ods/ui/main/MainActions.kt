package com.eunilsung.ods.ui.main

sealed interface MainActions {
    /** 앱이 화면 앞으로 나왔다. 로그인돼 있으면 백그라운드에 있던 동안의 시세를 다시 받는다. */
    data object OnForeground : MainActions
}

/** 메인 화면이 한 번씩 처리할 일. */
sealed interface MainScreenEvent {
    /** 로그아웃했다. 열려 있던 상세 화면을 닫는다. */
    data object LoggedOut : MainScreenEvent
}
