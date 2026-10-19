package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.data.local.LocalDataWiperImpl
import com.eunilsung.ods.data.local.seed.CustomerSeed
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.CustomerGrade
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.testsupport.createTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalCustomerRepositoryTest {
    private val database = createTestDatabase()
    private val repository = LocalCustomerRepositoryImpl(database, CustomerMapper())

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
    fun wipeRemovesBusinessData() = runTest {
        repository.refresh()
        LocalDataWiperImpl(database).wipe()
        assertEquals(0, repository.observeCustomers().first().size)
        assertEquals(0, repository.observeConsultations("C001").first().size)
    }
}
