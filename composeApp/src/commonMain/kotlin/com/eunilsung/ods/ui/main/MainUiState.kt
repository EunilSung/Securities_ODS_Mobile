package com.eunilsung.ods.ui.main

import com.eunilsung.ods.domain.model.ListDensity

/** 메인 화면의 상태 — 하단 탭 배지처럼 앱 전체가 보는 값을 둔다. */
data class MainUiState(
    /** 로그인했는지. 아니면 로그인 화면이 앱 전체를 덮는다. */
    val isLoggedIn: Boolean = false,
    /** 주문 탭 배지 — 미체결 주문 건수. */
    val openOrderCount: Int = 0,
    /** 사용자가 고른 목록 밀도. 화면 전체에 `LocalListDensity` 로 내려 준다. */
    val listDensity: ListDensity = ListDensity.COMFORTABLE,
)
