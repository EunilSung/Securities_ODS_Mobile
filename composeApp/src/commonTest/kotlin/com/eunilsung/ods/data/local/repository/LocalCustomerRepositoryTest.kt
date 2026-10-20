package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.data.local.LocalDataWiperImpl
import com.eunilsung.ods.data.local.LocalKeyGuard
import com.eunilsung.ods.data.local.seed.CustomerSeed
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.CustomerGrade
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.testsupport.testLocalSecret
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class LocalCustomerRepositoryTest {
    private val database = createTestDatabase()
    private val secret = testLocalSecret()
    private val repository = LocalCustomerRepositoryImpl(database, CustomerMapper(secret))

    @Test
    fun seedsOnlyWhenEmpty() = runTest {
        repository.refresh()
        repository.refresh()
        assertEquals(CustomerSeed.customers.size, repository.observeCustomers().first().size)
        assertEquals(2, repository.observeAccounts("C001").first().size)
        assertEquals(3, repository.observeConsultations("C001").first().size)
    }

    @Test
    fun consultationsComeNewestFirst() = runTest {
        repository.refresh()
        val added = repository.addConsultation(
            NewConsultation("C001", ConsultChannel.PHONE, ConsultTopic.GENERAL, "  전화 상담  "),
            staffName = "김민준",
            nowMillis = Long.MAX_VALUE / 2,
        )
        val history = repository.observeConsultations("C001").first()
        assertEquals(added, history.first())
        assertEquals("전화 상담", history.first().content)
        assertEquals("김민준", history.first().staffName)
    }

    @Test
    fun registeredCustomerStartsAsGeneralWithoutRiskProfile() = runTest {
        val created = repository.register(
            NewCustomer("  홍길동 ", LocalDate(1990, 1, 2), Gender.MALE, "010-1111-2222", "서울 중구"),
            nowMillis = 1_000,
        )
        val saved = repository.observeCustomer(created.id).first()
        assertEquals("홍길동", saved?.name)
        assertEquals(CustomerGrade.GENERAL, saved?.grade)
        assertNull(saved?.riskProfile)
    }

    @Test
    fun personalColumnsAreSealedOnDisk() = runTest {
        repository.refresh()
        val row = database.appDatabaseQueries.selectCustomer("C001").executeAsOne()
        val customer = repository.observeCustomer("C001").first()!!
        listOf(row.name, row.birthDate, row.phone, row.address).forEach { stored ->
            assertTrue(secret.isSealed(stored))
            assertFalse(stored.contains(customer.name))
        }
        val consultation = database.appDatabaseQueries.selectConsultations("C001").executeAsList().first()
        assertTrue(secret.isSealed(consultation.content))
        assertTrue(secret.isSealed(database.appDatabaseQueries.selectAccountNos().executeAsList().first()))
    }

    @Test
    fun customersAreSortedByNameAfterOpening() = runTest {
        repository.refresh()
        val names = repository.observeCustomers().first().map { it.name }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun accountSequenceCountsSealedNumbers() = runTest {
        val created = repository.register(
            NewCustomer("이순신", LocalDate(1985, 4, 28), Gender.MALE, "010-9999-1234", "서울 종로구"),
            nowMillis = 1_000,
        )
        val first = repository.openAccount(created.id, AccountType.BROKERAGE, nowMillis = 2_000)
        val second = repository.openAccount(created.id, AccountType.PENSION, nowMillis = 3_000)
        assertEquals("7001-9999-01", first.accountNo)
        assertEquals("7001-9999-02", second.accountNo)
    }

    @Test
    fun wipeRemovesBusinessDataAndShredsKey() = runTest {
        repository.refresh()
        val storedName = database.appDatabaseQueries.selectCustomer("C001").executeAsOne().name
        LocalDataWiperImpl(database, LocalKeyGuard(database, MapSettings(), secret)).wipe()
        assertEquals(0, repository.observeCustomers().first().size)
        assertEquals(0, repository.observeConsultations("C001").first().size)
        assertNull(secret.decryptOrNull(storedName))
    }
}
