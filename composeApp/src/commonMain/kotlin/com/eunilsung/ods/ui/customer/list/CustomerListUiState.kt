package com.eunilsung.ods.ui.customer.list

import com.eunilsung.ods.domain.model.Customer
import kotlinx.datetime.LocalDate

/** 고객 목록의 상태. */
data class CustomerListUiState(
    val query: String = "",
    /** 검색어에 맞는 고객(이름순). */
    val customers: List<Customer> = emptyList(),
    /** 담당 고객 전체 수. 검색 중에도 보인다. */
    val totalCount: Int = 0,
    val isRefreshing: Boolean = false,
    val maskPersonalInfo: Boolean = true,
    /** 나이·투자성향 유효기간을 따질 오늘(한국 시간). */
    val today: LocalDate? = null,
    /** 지금 상담 중인 고객. 목록에 표시한다. */
    val activeCustomerId: String? = null,
)
