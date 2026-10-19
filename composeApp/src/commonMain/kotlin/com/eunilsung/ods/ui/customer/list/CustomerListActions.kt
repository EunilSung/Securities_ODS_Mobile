package com.eunilsung.ods.ui.customer.list

sealed interface CustomerListActions {
    /** 검색어를 바꿨다. 이름·초성·전화번호 일부로 찾는다. */
    data class OnQueryChange(val query: String) : CustomerListActions

    data object OnRefresh : CustomerListActions

    /** 고객을 눌렀다. 상세를 연다. */
    data class OnCustomerClick(val id: String) : CustomerListActions

    /** 신규 고객 등록을 눌렀다. */
    data object OnNewCustomerClick : CustomerListActions
}

/** 고객 목록이 한 번씩 처리할 일. */
sealed interface CustomerListEvent {
    data class OpenDetail(val id: String) : CustomerListEvent
    data object OpenNewCustomer : CustomerListEvent
}
