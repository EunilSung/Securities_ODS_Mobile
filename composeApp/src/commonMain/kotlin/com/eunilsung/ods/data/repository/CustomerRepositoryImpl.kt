package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.data.mapper.ProductMapper
import com.eunilsung.ods.data.mapper.ServerCustomerMapper
import com.eunilsung.ods.data.mapper.ServerProductMapper
import com.eunilsung.ods.data.remote.server.OdsServer
import com.eunilsung.ods.data.remote.server.ServerEvents
import com.eunilsung.ods.data.remote.server.ServerResult
import com.eunilsung.ods.data.remote.server.getOrThrow
import com.eunilsung.ods.data.remote.server.valueOrNull
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.AccountTypeBlock
import com.eunilsung.ods.domain.model.AccountTypeBlockedException
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.domain.model.RiskProfile
import com.eunilsung.ods.domain.repository.CustomerRepository
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.shared.api.OpenAccountRequest
import com.eunilsung.ods.shared.api.RiskProfileRequest
import com.eunilsung.ods.shared.api.ServerEvent
import com.eunilsung.ods.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 서버 모드 고객 저장소. 화면은 [local] 처럼 기기 DB 를 보고, 이 저장소가 서버에서 받은 담당 고객으로 DB 를 맞춘다.
 *
 * [refresh] 는 고객·계좌·상담·가입·서명 기록을 서버가 준 그대로 한 번에 바꾼다. 등록·개설·기록은 서버가 받아 준 뒤에만
 * 돌려받은 행을 DB 에 넣는다. 서버가 거절하거나 닿지 못하면 던진다 — ISA·연금 제한은 [AccountTypeBlockedException] 이다.
 * 다시 연결되거나 고객 데이터가 바뀌었다는 알림이 오면 다시 받는다.
 */
class CustomerRepositoryImpl(
    private val local: LocalCustomerRepositoryImpl,
    private val database: AppDatabase,
    private val server: OdsServer,
    serverEvents: ServerEvents,
    private val mapper: CustomerMapper,
    private val productMapper: ProductMapper,
    private val serverMapper: ServerCustomerMapper,
    private val serverProductMapper: ServerProductMapper,
) : CustomerRepository by local {

    private val queries get() = database.appDatabaseQueries
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 다시 받기를 한 번에 하나씩만 한다. 겹치면 늦게 끝난 옛 응답이 새 목록을 덮는다. */
    private val refreshLock = Mutex()

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        scope.launch { serverEvents.connected.collect { refresh() } }
        scope.launch {
            serverEvents.events.collect {
                if (it.type == ServerEvent.TYPE_CHANGED && it.topic == ServerEvent.TOPIC_CUSTOMERS) refresh()
            }
        }
    }

    override suspend fun refresh(): Boolean = refreshLock.withLock {
        _isRefreshing.value = true
        try {
            val book = server.customers().valueOrNull() ?: return@withLock false
            withContext(Dispatchers.Default) {
                database.transaction {
                    queries.clearCustomerBook()
                    book.customers.forEach { queries.upsertCustomer(mapper.toEntity(serverMapper.toModel(it))) }
                    book.accounts.forEach { queries.upsertAccount(mapper.toEntity(serverMapper.toModel(it))) }
                    book.consultations.forEach { queries.upsertConsultation(mapper.toEntity(serverMapper.toModel(it))) }
                    book.subscriptions.forEach { queries.upsertSubscription(productMapper.toEntity(serverProductMapper.toModel(it))) }
                    book.signatures.forEach { queries.upsertSignature(serverMapper.toEntity(serverMapper.toModel(it))) }
                }
            }
            Log.message("[Customer] refreshed ${book.customers.size} customers")
            true
        } finally {
            _isRefreshing.value = false
        }
    }

    override suspend fun register(customer: NewCustomer, nowMillis: Long): Customer {
        val created = serverMapper.toModel(server.registerCustomer(serverMapper.toRequest(customer)).getOrThrow())
        withContext(Dispatchers.Default) { queries.upsertCustomer(mapper.toEntity(created)) }
        return created
    }

    override suspend fun openAccount(customerId: String, type: AccountType, nowMillis: Long): Account {
        val result = server.openAccount(customerId, OpenAccountRequest(type.name))
        if (result is ServerResult.Rejected) blockOf(result.error?.code)?.let { throw AccountTypeBlockedException(it) }
        val account = serverMapper.toModel(result.getOrThrow())
        withContext(Dispatchers.Default) { queries.upsertAccount(mapper.toEntity(account)) }
        return account
    }

    override suspend fun saveRiskProfile(customerId: String, profile: RiskProfile, assessedAtMillis: Long) {
        server.saveRiskProfile(customerId, RiskProfileRequest(profile.name, assessedAtMillis)).getOrThrow()
        local.saveRiskProfile(customerId, profile, assessedAtMillis)
    }

    override suspend fun addConsultation(consultation: NewConsultation, staffName: String, nowMillis: Long): Consultation {
        val created = serverMapper.toModel(
            server.addConsultation(consultation.customerId, serverMapper.toRequest(consultation)).getOrThrow()
        )
        withContext(Dispatchers.Default) { queries.upsertConsultation(mapper.toEntity(created)) }
        return created
    }

    private fun blockOf(code: String?): AccountTypeBlock? = when (code) {
        ApiErrorCode.ISA_AGE -> AccountTypeBlock.ISA_AGE
        ApiErrorCode.ISA_DUPLICATE -> AccountTypeBlock.ISA_DUPLICATE
        ApiErrorCode.PENSION_DUPLICATE -> AccountTypeBlock.PENSION_DUPLICATE
        else -> null
    }
}
