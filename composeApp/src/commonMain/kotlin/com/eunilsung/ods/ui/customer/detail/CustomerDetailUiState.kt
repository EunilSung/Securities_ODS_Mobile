package com.eunilsung.ods.ui.customer.detail

import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.Visit
import kotlinx.datetime.LocalDate

/** 고객 상세의 상태. */
data class CustomerDetailUiState(
    /** 아직 읽지 못했거나 없는 고객이면 null. */
    val customer: Customer? = null,
    val accounts: List<Account> = emptyList(),
    /** 상담 이력(최근 것부터). */
    val consultations: List<Consultation> = emptyList(),
    /** 다가오는 방문(예정만). */
    val upcomingVisits: List<Visit> = emptyList(),
    val maskPersonalInfo: Boolean = true,
    val today: LocalDate? = null,
)
