package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.testsupport.authGet
import com.eunilsung.ods.server.testsupport.authPost
import com.eunilsung.ods.server.testsupport.decode
import com.eunilsung.ods.server.testsupport.errorCode
import com.eunilsung.ods.server.testsupport.loginToken
import com.eunilsung.ods.server.testsupport.serverTest
import com.eunilsung.ods.shared.api.AccountDto
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.shared.api.CustomerBookResponse
import com.eunilsung.ods.shared.api.NewSubscriptionRequest
import com.eunilsung.ods.shared.api.OpenAccountRequest
import com.eunilsung.ods.shared.api.ProductDto
import com.eunilsung.ods.shared.api.SubscriptionDto
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 상품 목록과 가입 — 화면이 막은 조건을 서버가 한 번 더 본다.
 *
 * 시연용 C001 은 위험중립형(120일 전 진단)이고 계좌 7001-2231-01 이 있다. C002 는 400일 전에 진단해 만료됐다.
 */
class ProductRoutesTest {

    @Test
    fun `상품은 시연용 순서대로 핵심설명 항목과 함께 온다`() = serverTest {
        val products = client.authGet(client.loginToken("test1"), "/products").decode(ListSerializer(ProductDto.serializer()))

        assertEquals((1..8).map { "P00$it" }, products.map { it.id })
        val els = products.first { it.id == "P007" }
        assertEquals(4, els.keyPoints.size)
        assertEquals(true, els.isComplex)
        assertEquals(1_000_000, els.minAmount)
    }

    @Test
    fun `조건을 모두 지키면 가입되고 장부에 상품 이름과 함께 보인다`() = serverTest {
        val token = client.loginToken("test1")

        val response = client.subscribe(token, request(coolingOffUntilMillis = 1_800_000_000_000))

        assertEquals(HttpStatusCode.Created, response.status)
        val subscription = response.decode(SubscriptionDto.serializer())
        assertEquals("단기채권 증권투자신탁", subscription.productName)
        assertEquals(1_800_000_000_000, subscription.coolingOffUntilMillis)
        val book = client.authGet(token, "/customers").decode(CustomerBookResponse.serializer())
        assertEquals(listOf(subscription), book.subscriptions)
    }

    @Test
    fun `다른 고객의 계좌로는 가입할 수 없다`() = serverTest {
        val response = client.subscribe(client.loginToken("test1"), request(accountNo = "7001-3342-01"))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals(ApiErrorCode.ACCOUNT_MISMATCH, response.errorCode())
    }

    @Test
    fun `최소 금액보다 적거나 만 원 단위가 아니면 거절한다`() = serverTest {
        val token = client.loginToken("test1")

        for (amount in listOf(90_000L, 105_000L)) {
            val response = client.subscribe(token, request(amount = amount))
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals(ApiErrorCode.INVALID_AMOUNT, response.errorCode())
        }
    }

    @Test
    fun `투자성향이 없거나 1년이 지났으면 진단부터 하라고 한다`() = serverTest {
        val token = client.loginToken("test1")
        val expired = client.subscribe(token, request(customerId = "C002", accountNo = "7001-3342-01"))
        val never = client.authPost(
            token, "/customers/C005/accounts",
            OpenAccountRequest.serializer(), OpenAccountRequest("BROKERAGE"),
        ).decode(AccountDto.serializer())
        val none = client.subscribe(token, request(customerId = "C005", accountNo = never.accountNo))

        for (response in listOf(expired, none)) {
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertEquals(ApiErrorCode.NEEDS_ASSESSMENT, response.errorCode())
        }
    }

    @Test
    fun `성향보다 위험한 상품은 고객 확인이 있어야 가입된다`() = serverTest {
        val token = client.loginToken("test1")

        val unconfirmed = client.subscribe(token, request(productId = "P005"))
        val confirmed = client.subscribe(token, request(productId = "P005", unsuitableConfirmed = true))
        val atLimit = client.subscribe(token, request(productId = "P004", amount = 300_000))

        assertEquals(HttpStatusCode.Conflict, unconfirmed.status)
        assertEquals(ApiErrorCode.UNSUITABLE_NOT_CONFIRMED, unconfirmed.errorCode())
        assertEquals(HttpStatusCode.Created, confirmed.status)
        assertEquals(true, confirmed.decode(SubscriptionDto.serializer()).unsuitableConfirmed)
        assertEquals(HttpStatusCode.Created, atLimit.status)
    }

    @Test
    fun `없는 고객이나 상품은 404 다`() = serverTest {
        val token = client.loginToken("test1")

        assertEquals(HttpStatusCode.NotFound, client.subscribe(token, request(customerId = "C999")).status)
        assertEquals(HttpStatusCode.NotFound, client.subscribe(token, request(productId = "P999")).status)
    }

    private fun request(
        customerId: String = "C001",
        productId: String = "P003",
        accountNo: String = "7001-2231-01",
        amount: Long = 100_000,
        unsuitableConfirmed: Boolean = false,
        coolingOffUntilMillis: Long? = null,
    ) = NewSubscriptionRequest(customerId, productId, accountNo, amount, unsuitableConfirmed, coolingOffUntilMillis)

    private suspend fun HttpClient.subscribe(token: String, request: NewSubscriptionRequest): HttpResponse =
        authPost(token, "/subscriptions", NewSubscriptionRequest.serializer(), request)
}
