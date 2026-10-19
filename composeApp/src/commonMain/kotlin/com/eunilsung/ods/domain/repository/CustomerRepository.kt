package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.domain.model.RiskProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** 고객·상담 이력·계좌. 화면은 기기 DB 를 보고, [refresh] 가 서버(또는 시연용 데이터)에서 받아 채운다. */
interface CustomerRepository {
    /** 담당 고객 전체(이름순). */
    fun observeCustomers(): Flow<List<Customer>>

    /** 한 고객. 없으면 null. */
    fun observeCustomer(id: String): Flow<Customer?>

    /** 상담 이력(최근 것부터). */
    fun observeConsultations(customerId: String): Flow<List<Consultation>>

    fun observeAccounts(customerId: String): Flow<List<Account>>

    /** 담당 고객을 다시 받는다. 실패하면 가지고 있던 것을 그대로 둔다. */
    suspend fun refresh(): Boolean

    val isRefreshing: StateFlow<Boolean>

    /** 새 고객을 등록한다. */
    suspend fun register(customer: NewCustomer, nowMillis: Long): Customer

    /** 새 계좌를 연다. 계좌번호는 저장소가 정한다. */
    suspend fun openAccount(customerId: String, type: AccountType, nowMillis: Long): Account

    /** 투자성향을 바꾼다. 진단한 시각을 함께 남겨 유효기간을 따진다. */
    suspend fun saveRiskProfile(customerId: String, profile: RiskProfile, assessedAtMillis: Long)

    /** 상담 기록을 남긴다. [staffName] 은 기록한 직원이다. */
    suspend fun addConsultation(consultation: NewConsultation, staffName: String, nowMillis: Long): Consultation
}

/**
 * 지금 상담 중인 고객 — 고객 상세를 열면 그 고객이 된다. 넓은 태블릿의 상담 칸이 이 고객을 따라간다.
 *
 * 실행 중에만 들고 있다. 로그아웃하면 비운다.
 */
interface ConsultSessionRepository {
    val activeCustomerId: StateFlow<String?>

    fun start(customerId: String)

    fun end()
}
