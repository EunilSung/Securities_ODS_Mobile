package com.eunilsung.ods.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.eunilsung.ods.data.local.seed.CustomerSeed
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.CustomerGrade
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.domain.model.RiskProfile
import com.eunilsung.ods.domain.repository.CustomerRepository
import com.eunilsung.ods.util.Log
import com.eunilsung.ods.util.newId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * 서버 없이 도는 고객 저장소 — 고객이 하나도 없을 때 시연용 고객·계좌·상담 이력을 기기 DB 에 넣는다.
 *
 * 업무 데이터를 지운 뒤(단말 분실 대비) 다시 로그인하면 시연용 데이터가 처음 상태로 돌아온다.
 * 서버 모드 구현이 생기면 이 클래스를 `by local` 로 위임받아 서버에서 받은 담당 고객만 기기 DB 에 반영한다.
 */
class LocalCustomerRepositoryImpl(
    private val database: AppDatabase,
    private val mapper: CustomerMapper,
) : CustomerRepository {

    private val queries get() = database.appDatabaseQueries

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    override fun observeCustomers(): Flow<List<Customer>> =
        queries.selectCustomers().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map(mapper::toModel) }

    override fun observeCustomer(id: String): Flow<Customer?> =
        queries.selectCustomer(id).asFlow().mapToOneOrNull(Dispatchers.Default).map { it?.let(mapper::toModel) }

    override fun observeConsultations(customerId: String): Flow<List<Consultation>> =
        queries.selectConsultations(customerId).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map(mapper::toModel) }

    override fun observeAccounts(customerId: String): Flow<List<Account>> =
        queries.selectAccounts(customerId).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map(mapper::toModel) }

    override suspend fun refresh(): Boolean {
        _isRefreshing.value = true
        try {
            withContext(Dispatchers.Default) {
                if (queries.countCustomers().executeAsOne() > 0L) return@withContext
                val now = Clock.System.now().toEpochMilliseconds()
                database.transaction {
                    CustomerSeed.customers.forEach { queries.insertCustomer(mapper.toEntity(it, now)) }
                    CustomerSeed.accounts.forEach { queries.insertAccount(mapper.toEntity(it, now)) }
                    CustomerSeed.consultations.forEachIndexed { index, row -> queries.insertConsultation(mapper.toEntity(row, index, now)) }
                }
                Log.message("[Customer/Local] seeded ${CustomerSeed.customers.size} customers")
            }
            return true
        } finally {
            _isRefreshing.value = false
        }
    }

    override suspend fun register(customer: NewCustomer, nowMillis: Long): Customer {
        val created = Customer(
            id = newId(),
            name = customer.name.trim(),
            birthDate = customer.birthDate,
            gender = customer.gender,
            phone = customer.phone,
            address = customer.address.trim(),
            grade = CustomerGrade.GENERAL,
            riskProfile = null,
            riskAssessedAtMillis = null,
            registeredAtMillis = nowMillis,
        )
        withContext(Dispatchers.Default) { queries.insertCustomer(mapper.toEntity(created)) }
        return created
    }

    /**
     * 계좌번호는 `7001-<휴대폰 가운데 4자리>-<순번 2자리>` 로 만든다. 시연용 계좌와 같은 모양이다.
     * 서버 모드에서는 서버가 발급한 번호를 받아 그대로 남긴다.
     */
    override suspend fun openAccount(customerId: String, type: AccountType, nowMillis: Long): Account = withContext(Dispatchers.Default) {
        val customer = queries.selectCustomer(customerId).executeAsOne()
        val middle = customer.phone.split("-").getOrNull(1)?.takeLast(4)?.padStart(4, '0') ?: "0000"
        val prefix = "$BRANCH_CODE-$middle-"
        val sequence = queries.countAccountsWithPrefix(prefix).executeAsOne() + 1
        val account = Account(
            accountNo = prefix + sequence.toString().padStart(2, '0'),
            customerId = customerId,
            type = type,
            openedAtMillis = nowMillis,
        )
        queries.insertAccount(mapper.toEntity(account))
        account
    }

    override suspend fun saveRiskProfile(customerId: String, profile: RiskProfile, assessedAtMillis: Long) {
        withContext(Dispatchers.Default) { queries.updateRiskProfile(profile.name, assessedAtMillis, customerId) }
    }

    override suspend fun addConsultation(consultation: NewConsultation, staffName: String, nowMillis: Long): Consultation {
        val created = Consultation(
            id = newId(),
            customerId = consultation.customerId,
            consultedAtMillis = nowMillis,
            channel = consultation.channel,
            topic = consultation.topic,
            content = consultation.content.trim(),
            staffName = staffName,
        )
        withContext(Dispatchers.Default) { queries.insertConsultation(mapper.toEntity(created)) }
        return created
    }

    private companion object {
        /** 시연용 지점 코드 — 계좌번호 앞 4자리. */
        const val BRANCH_CODE = "7001"
    }
}
