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

    const val CUSTOMER_SEARCH = "customer_search"
    const val CUSTOMER_ITEM = "customer_item"
    const val CUSTOMER_NEW = "customer_new"
    const val NEW_CUSTOMER_NAME = "new_customer_name"
    const val NEW_CUSTOMER_BIRTH = "new_customer_birth"
    const val NEW_CUSTOMER_PHONE = "new_customer_phone"
    const val NEW_CUSTOMER_ADDRESS = "new_customer_address"
    const val NEW_CUSTOMER_SUBMIT = "new_customer_submit"
    const val CUSTOMER_DETAIL_NAME = "customer_detail_name"
    const val CONSULT_ADD = "consult_add"
    const val CONSULT_CONTENT = "consult_content"
    const val CONSULT_SAVE = "consult_save"
    const val CONSULT_ITEM = "consult_item"
    const val CONSULT_PANE = "consult_pane"

    /** 주간 달력은 화면마다 접두어가 붙는다(`schedule_week_next`·`new_visit_week_next`). 날짜 칸은 `<접두어>day_cell_<일>`. */
    const val SCHEDULE_PREFIX = "schedule_"
    const val NEW_VISIT_PREFIX = "new_visit_"
    const val WEEK_PREVIOUS = "week_previous"
    const val WEEK_NEXT = "week_next"
    const val DAY_CELL_PREFIX = "day_cell_"
    const val SCHEDULE_ADD = "schedule_add"
    const val VISIT_ITEM = "visit_item"
    const val VISIT_COMPLETE = "visit_complete"
    const val VISIT_CANCEL = "visit_cancel"
    const val VISIT_CONFIRM_CANCEL = "visit_confirm_cancel"
    const val VISIT_OPEN_CUSTOMER = "visit_open_customer"
    const val NEW_VISIT_CUSTOMER_SEARCH = "new_visit_customer_search"
    const val NEW_VISIT_CUSTOMER = "new_visit_customer"
    /** 시각 칸은 `time_slot_<시><분>` 이다(예: time_slot_1430). */
    const val TIME_SLOT_PREFIX = "time_slot_"
    const val NEW_VISIT_PLACE = "new_visit_place"
    const val NEW_VISIT_MEMO = "new_visit_memo"
    const val NEW_VISIT_SUBMIT = "new_visit_submit"
    const val CUSTOMER_ADD_VISIT = "customer_add_visit"

    const val SIGNATURE_PAD = "signature_pad"
    const val RISK_START = "risk_start"
    /** 진단 선택지는 `risk_option_<번호>` 이다(0부터). */
    const val RISK_OPTION_PREFIX = "risk_option_"
    const val RISK_NEXT = "risk_next"
    const val RISK_RESULT = "risk_result"
    const val RISK_CONFIRM = "risk_confirm"
    const val RISK_SAVE = "risk_save"

    const val PRODUCT_ITEM = "product_item"
    const val PRODUCT_SUBSCRIBE = "product_subscribe"
    const val SUITABILITY_CARD = "suitability_card"
    const val SUBSCRIPTION_CUSTOMER_SEARCH = "subscription_customer_search"
    const val SUBSCRIPTION_CUSTOMER = "subscription_customer"
    const val SUBSCRIPTION_ASSESS = "subscription_assess"
    const val SUBSCRIPTION_UNSUITABLE_CONFIRM = "subscription_unsuitable_confirm"
    /** 핵심 설명 항목은 `key_point_<번호>` 이다(0부터). */
    const val KEY_POINT_PREFIX = "key_point_"
    const val SUBSCRIPTION_ACCOUNT = "subscription_account"
    const val SUBSCRIPTION_AMOUNT = "subscription_amount"
    const val SUBSCRIPTION_NEXT = "subscription_next"
    const val SUBSCRIPTION_SUBMIT = "subscription_submit"
    const val SUBSCRIPTION_DONE = "subscription_done"
    const val SUBSCRIPTION_CLOSE = "subscription_close"

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
