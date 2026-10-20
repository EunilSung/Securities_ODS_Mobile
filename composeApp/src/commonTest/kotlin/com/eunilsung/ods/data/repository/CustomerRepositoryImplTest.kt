package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalProductRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalSignatureRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.data.mapper.ProductMapper
import com.eunilsung.ods.data.mapper.ServerCustomerMapper
import com.eunilsung.ods.data.mapper.ServerProductMapper
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.AccountTypeBlock
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.IdCardType
import com.eunilsung.ods.domain.model.IdVerification
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePoint
import com.eunilsung.ods.domain.model.Terms
import com.eunilsung.ods.domain.usecase.OpenAccountResult
import com.eunilsung.ods.domain.usecase.OpenAccountUseCase
import com.eunilsung.ods.shared.api.AccountDto
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.shared.api.ConsultationDto
import com.eunilsung.ods.shared.api.CustomerBookResponse
import com.eunilsung.ods.shared.api.CustomerDto
import com.eunilsung.ods.shared.api.ServerEvent
import com.eunilsung.ods.shared.api.SignatureRecordDto
import com.eunilsung.ods.shared.api.SubscriptionDto
import com.eunilsung.ods.testsupport.FakeOdsServer
import com.eunilsung.ods.testsupport.FakeServerEvents
import com.eunilsung.ods.testsupport.awaitUntil
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.testsupport.testLocalSecret
import com.eunilsung.ods.testsupport.testSecureSettings
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 서버 모드 고객 저장소 — 서버 장부로 기기 DB 를 맞추고, 서버의 거절을 도메인 결과로 옮기는지 본다. */
class CustomerRepositoryImplTest {
    private val database = createTestDatabase()
    private val secret = testLocalSecret()
    private val server = FakeOdsServer()
    private val events = FakeServerEvents()
    private val local = LocalCustomerRepositoryImpl(database, CustomerMapper(secret))
    private val repository = CustomerRepositoryImpl(
        local, database, server, events, CustomerMapper(secret), ProductMapper(secret), ServerCustomerMapper(), ServerProductMapper(),
    )
    private val products = LocalProductRepositoryImpl(database, ProductMapper(secret))
    private val signatures = LocalSignatureRepositoryImpl(database)

    private val customer = CustomerDto("C1", "한지민", "1985-03-12", "FEMALE", "010-1234-5678", "서울 강남구", "VIP", registeredAtMillis = 10)

    @Test
    fun refreshWritesServerBookWithoutDroppingRowsSavedMeanwhile() = runTest {
        local.register(NewCustomer("옛 고객", LocalDate(1970, 1, 1), Gender.MALE, "010-0000-0000", "부산"), nowMillis = 1)
        server.book = CustomerBookResponse(
            customers = listOf(customer),
            accounts = listOf(AccountDto("9001-1234-01", "C1", "ISA", 20)),
            consultations = listOf(ConsultationDto("K1", "C1", 30, "PHONE", "MARKET", "시황 안내", "김민준")),
            subscriptions = listOf(SubscriptionDto("B1", "C1", "P1", "채권형 펀드", "9001-1234-01", 1_000_000, 40, false)),
            signatures = listOf(SignatureRecordDto("G1", "C1", "ACCOUNT_OPENING", 50, "abc123")),
        )

        assertTrue(repository.refresh())

        val customers = repository.observeCustomers().first()
        assertEquals(listOf("옛 고객", "한지민"), customers.map { it.name }.sorted())
        assertEquals(LocalDate(1985, 3, 12), customers.single { it.id == "C1" }.birthDate)
        assertEquals(listOf(AccountType.ISA), repository.observeAccounts("C1").first().map { it.type })
        assertEquals("시황 안내", repository.observeConsultations("C1").first().single().content)
        assertEquals(1_000_000, products.observeSubscriptions("C1").first().single().amount)
        assertEquals("abc123", signatures.observeRecords("C1").first().single().digest)
        assertFalse(repository.isRefreshing.value)
    }

    @Test
    fun unreachableRefreshKeepsWhatIsThere() = runTest {
        server.book = server.book.copy(customers = listOf(customer))
        repository.refresh()
        server.isReachable = false

        assertFalse(repository.refresh())
        assertEquals(1, repository.observeCustomers().first().size)
    }

    @Test
    fun serverAccountBlockBecomesDomainBlock() = runTest {
        server.book = server.book.copy(customers = listOf(customer))
        repository.refresh()
        server.openAccountRejection = ApiErrorCode.ISA_DUPLICATE
        val login = MapSettings().let { LocalLoginRepositoryImpl(it, testSecureSettings(it), simulatedRoundTripMillis = 0) }
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        val openAccount = OpenAccountUseCase(repository, SignatureRepositoryImpl(signatures, database, server, ServerCustomerMapper()), login)

        val result = openAccount(
            customerId = "C1",
            type = AccountType.ISA,
            verification = IdVerification.Verified(IdCardType.RESIDENT, 0),
            agreedTerms = Terms.entries.toSet(),
            pinSet = true,
            signature = Signature(listOf(List(20) { SignaturePoint(0.1f + it * 0.03f, 0.5f) })),
            note = "note",
        )

        assertEquals(OpenAccountResult.TypeBlocked(AccountTypeBlock.ISA_DUPLICATE), result)
        assertTrue(repository.observeAccounts("C1").first().isEmpty())
        assertTrue(server.signatureRequests.isEmpty())
    }

    @Test
    fun customersChangedEventRefetches() = runTest {
        repository.refresh()
        server.book = server.book.copy(customers = listOf(customer))

        events.push(ServerEvent(ServerEvent.TYPE_CHANGED, topic = ServerEvent.TOPIC_CUSTOMERS))

        awaitUntil { repository.observeCustomers().first().size == 1 }
    }

    @Test
    fun reconnectRefetches() = runTest {
        server.book = server.book.copy(customers = listOf(customer))

        events.connect()

        awaitUntil { repository.observeCustomers().first().size == 1 }
    }
}
