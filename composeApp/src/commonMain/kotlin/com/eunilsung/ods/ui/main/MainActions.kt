package com.eunilsung.ods.ui.main

sealed interface MainActions {
    /** 앱이 화면 앞으로 나왔다. 오래 비웠으면 잠그고, 로그인돼 있으면 비운 동안의 시세를 다시 받는다. */
    data object OnForeground : MainActions

    /** 앱이 뒤로 갔다. 돌아왔을 때 얼마나 비웠는지 잰다. */
    data object OnBackground : MainActions

    /** 화면을 만졌다. 자동 잠금 시계를 다시 맞춘다. */
    data object OnUserInteraction : MainActions
}

/** 메인 화면이 한 번씩 처리할 일. */
sealed interface MainScreenEvent {
    /** 로그아웃했다. 열려 있던 상세 화면을 닫는다. */
    data object LoggedOut : MainScreenEvent
}
