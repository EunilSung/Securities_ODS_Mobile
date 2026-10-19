package com.eunilsung.ods.ui.uikit

import androidx.compose.ui.Modifier

/**
 * 화면 자동화(Maestro)가 요소를 찾는 이름. `.maestro` 의 흐름 파일이 같은 문자열을 `id` 로 쓴다.
 *
 * 화면 문구는 기기 언어에 따라 바뀌고 같은 문구가 여러 곳에 나오므로, 흐름이 누르고 확인하는 요소는 이 이름으로 찾는다.
 * 이름을 바꾸면 흐름 파일도 함께 바꾼다.
 */
object TestTags {
    const val LOGIN_ID = "login_id"
    const val LOGIN_PW = "login_pw"
    const val LOGIN_SUBMIT = "login_submit"

    /** 하단 탭은 `tab_<탭 key>` 이다(`tab_market`·`tab_more`). */

    /** 상단 제목줄의 뒤로 버튼. iOS 에는 시스템 뒤로 가기가 없어 흐름이 이것을 누른다. */
    const val TOP_BACK = "top_back"

    /** 보안 키패드의 숫자 키는 `pin_key_<숫자>` 이다. 배열이 섞여도 숫자로 누를 수 있다. */
    const val PIN_KEY_PREFIX = "pin_key_"
    const val APP_LOCK_SCREEN = "app_lock_screen"
    const val APP_LOCK_LOGOUT = "app_lock_logout"
    const val SETTING_SECURITY = "setting_security"
    const val SETTING_LOCK_NOW = "setting_lock_now"

    const val STOCK_ITEM = "stock_item"
    const val MORE_LOGOUT = "more_logout"
}

/**
 * 테스트 이름표를 화면 자동화 도구가 읽을 수 있게 내보낸다.
 *
 * Android 는 `testTagsAsResourceId` 를 켜야 resource-id 로 보인다. 다이얼로그는 창이 따로라 Surface 에 따로 붙인다.
 * iOS 는 testTag 가 그대로 접근성 식별자로 나가므로 아무것도 하지 않는다.
 */
expect fun Modifier.exposeTestTags(): Modifier
