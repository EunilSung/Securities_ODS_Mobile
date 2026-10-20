package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.rules.BusinessRules
import com.eunilsung.ods.server.testsupport.TestDatabase
import com.eunilsung.ods.server.testsupport.authGet
import com.eunilsung.ods.server.testsupport.authPost
import com.eunilsung.ods.server.testsupport.authPut
import com.eunilsung.ods.server.testsupport.decode
import com.eunilsung.ods.server.testsupport.errorCode
import com.eunilsung.ods.server.testsupport.loginToken
import com.eunilsung.ods.server.testsupport.serverTest
import com.eunilsung.ods.shared.api.AccountDto
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.shared.api.ConsultationDto
import com.eunilsung.ods.shared.api.CustomerBookResponse
import com.eunilsung.ods.shared.api.CustomerDto
import com.eunilsung.ods.shared.api.NewConsultationRequest
import com.eunilsung.ods.shared.api.NewCustomerRequest
import com.eunilsung.ods.shared.api.OpenAccountRequest
import com.eunilsung.ods.shared.api.RiskProfileRequest
import com.eunilsung.ods.shared.api.SignaturePointDto
import com.eunilsung.ods.shared.api.SignatureRecordDto
import com.eunilsung.ods.shared.api.SignatureRequest
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 지점 고객 장부 — 등록, 계좌 개설 규칙, 투자성향, 상담 이력, 서명. */
class CustomerRoutesTest {

    @Test
    fun `장부에 시연용 고객과 딸린 기록이 모두 실리고 상담은 최근 것부터다`() = serverTest {
        val token = client.loginToken("test2")

        val book = client.authGet(token, "/customers").decode(CustomerBookResponse.serializer())

        assertEquals(12, book.customers.size)
        assertEquals(14, book.accounts.size)
        assertEquals(10, book.consultations.size)
        val c001 = book.customers.first { it.id == "C001" }
        assertEquals("강하늘", c001.name)
        assertEquals("1968-03-14", c001.birthDate)
        assertEquals("NEUTRAL", c001.riskProfile)
        assertEquals(book.consultations.sortedByDescending { it.consultedAtMillis }, book.consultations)
    }

    @Test
    fun `고객을 등록하면 서버가 아이디와 일반 등급을 정하고 장부에 보인다`() = serverTest {
        val token = client.loginToken("test1")

        val response = client.register(token, NewCustomerRequest("  신규고객 ", "1990-01-02", "FEMALE", "010-9876-5432", "서울 중구 세종대로 110"))

        assertEquals(HttpStatusCode.Created, response.status)
        val created = response.decode(CustomerDto.serializer())
        assertEquals("신규고객", created.name)
        assertEquals("GENERAL", created.grade)
        assertNull(created.riskProfile)
        assertTrue(created.id.isNotBlank())
        val book = client.authGet(token, "/customers").decode(CustomerBookResponse.serializer())
        assertTrue(book.customers.any { it.id == created.id })
    }

    @Test
    fun `이름이 비거나 날짜·성별이 틀리면 400 이다`() = serverTest {
        val token = client.loginToken("test1")
        val valid = NewCustomerRequest("홍길동", "1990-01-02", "MALE", "010-1111-2222", "서울")

        for (request in listOf(valid.copy(name = " "), valid.copy(birthDate = "1990-02-30"), valid.copy(birthDate = "19900102"), valid.copy(gender = "X"))) {
            val response = client.register(token, request)
            assertEquals(HttpStatusCode.BadRequest, response.status, request.toString())
            assertEquals(ApiErrorCode.BAD_REQUEST, response.errorCode())
        }
    }

    @Test
    fun `계좌번호는 7001-휴대폰 가운데 4자리-순번 이고 같은 앞부분이면 순번이 오른다`() = serverTest {
        val token = client.loginToken("test1")
        val customer = client.register(token, adult("010-1234-5678")).decode(CustomerDto.serializer())

        val first = client.openAccount(token, customer.id, "BROKERAGE").decode(AccountDto.serializer())
        val second = client.openAccount(token, customer.id, "CMA").decode(AccountDto.serializer())
        val existing = client.openAccount(token, "C001", "CMA").decode(AccountDto.serializer())

        assertEquals("7001-1234-01", first.accountNo)
        assertEquals("7001-1234-02", second.accountNo)
        assertEquals("7001-2231-03", existing.accountNo)
        assertEquals(customer.id, first.customerId)
    }

    @Test
    fun `ISA 는 만 19세가 되는 날부터 열 수 있다`() = serverTest {
        val token = client.loginToken("test1")
        val today = BusinessRules.todayInSeoul()
        val turnsTomorrow = client.register(token, adult("010-1000-0001").copy(birthDate = today.minusYears(19).plusDays(1).toString()))
            .decode(CustomerDto.serializer())
        val turnsToday = client.register(token, adult("010-1000-0002").copy(birthDate = today.minusYears(19).toString()))
            .decode(CustomerDto.serializer())

        val young = client.openAccount(token, turnsTomorrow.id, "ISA")
        val ofAge = client.openAccount(token, turnsToday.id, "ISA")

        assertEquals(HttpStatusCode.Conflict, young.status)
        assertEquals(ApiErrorCode.ISA_AGE, young.errorCode())
        assertEquals(HttpStatusCode.Created, ofAge.status)
    }

    @Test
    fun `ISA 와 연금저축은 하나씩만 열 수 있다`() = serverTest {
        val token = client.loginToken("test1")

        val isa = client.openAccount(token, "C002", "ISA")
        val pension = client.openAccount(token, "C001", "PENSION")

        assertEquals(HttpStatusCode.Conflict, isa.status)
        assertEquals(ApiErrorCode.ISA_DUPLICATE, isa.errorCode())
        assertEquals(HttpStatusCode.Conflict, pension.status)
        assertEquals(ApiErrorCode.PENSION_DUPLICATE, pension.errorCode())
    }

    @Test
    fun `없는 고객은 404, 모르는 계좌 종류는 400 이다`() = serverTest {
        val token = client.loginToken("test1")

        assertEquals(HttpStatusCode.NotFound, client.openAccount(token, "C999", "CMA").status)
        assertEquals(HttpStatusCode.BadRequest, client.openAccount(token, "C001", "SAVINGS").status)
    }

    @Test
    fun `투자성향을 저장하면 장부에 그대로 보인다`() = serverTest {
        val token = client.loginToken("test1")
        val assessedAt = System.currentTimeMillis()

        val saved = client.authPut(token, "/customers/C005/risk-profile", RiskProfileRequest.serializer(), RiskProfileRequest("ACTIVE", assessedAt))

        assertEquals(HttpStatusCode.NoContent, saved.status)
        val c005 = client.authGet(token, "/customers").decode(CustomerBookResponse.serializer()).customers.first { it.id == "C005" }
        assertEquals("ACTIVE", c005.riskProfile)
        assertEquals(assessedAt, c005.riskAssessedAtMillis)
        assertEquals(
            HttpStatusCode.BadRequest,
            client.authPut(token, "/customers/C005/risk-profile", RiskProfileRequest.serializer(), RiskProfileRequest("BRAVE", assessedAt)).status,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.authPut(token, "/customers/C999/risk-profile", RiskProfileRequest.serializer(), RiskProfileRequest("ACTIVE", assessedAt)).status,
        )
    }

    @Test
    fun `상담 이력의 직원 이름은 토큰의 직원이다`() = serverTest {
        val token = client.loginToken("test3")

        val response = client.authPost(
            token, "/customers/C001/consultations", NewConsultationRequest.serializer(),
            NewConsultationRequest("PHONE", "GENERAL", " 안부 전화 "),
        )

        assertEquals(HttpStatusCode.Created, response.status)
        val consultation = response.decode(ConsultationDto.serializer())
        assertEquals("박도윤", consultation.staffName)
        assertEquals("안부 전화", consultation.content)
        val newest = client.authGet(token, "/customers").decode(CustomerBookResponse.serializer()).consultations.first()
        assertEquals(consultation.id, newest.id)
    }

    @Test
    fun `서명은 그림을 서버에 남기고 앱과 같은 방식의 지문을 돌려준다`() = serverTest {
        val token = client.loginToken("test1")
        val strokes = listOf(
            listOf(SignaturePointDto(0.1f, 0.2f), SignaturePointDto(0.5f, 0.25f)),
            listOf(SignaturePointDto(0.3f, 0.9f)),
        )

        val response = client.authPost(
            token, "/customers/C001/signatures", SignatureRequest.serializer(),
            SignatureRequest("ACCOUNT_OPENING", 1_700_000_000_000, strokes),
        )

        assertEquals(HttpStatusCode.Created, response.status)
        val record = response.decode(SignatureRecordDto.serializer())
        assertEquals(sha256Hex("0.1,0.2;0.5,0.25|0.3,0.9"), record.digest)
        assertEquals(1_700_000_000_000, record.signedAtMillis)
        val storedPoints = TestDatabase.dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT jsonb_array_length(strokes -> 0) FROM signature WHERE id = ?").use { st ->
                st.setString(1, record.id)
                st.executeQuery().use { rs -> rs.next(); rs.getInt(1) }
            }
        }
        assertEquals(2, storedPoints)
        val book = client.authGet(token, "/customers").decode(CustomerBookResponse.serializer())
        assertEquals(listOf(record), book.signatures)
    }

    private fun adult(phone: String) = NewCustomerRequest("테스트고객", "1990-01-02", "MALE", phone, "서울")

    private suspend fun HttpClient.register(token: String, request: NewCustomerRequest) =
        authPost(token, "/customers", NewCustomerRequest.serializer(), request)

    private suspend fun HttpClient.openAccount(token: String, customerId: String, type: String) =
        authPost(token, "/customers/$customerId/accounts", OpenAccountRequest.serializer(), OpenAccountRequest(type))

    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
