package com.eunilsung.ods.server.testsupport

import com.eunilsung.ods.server.ServerJson
import com.eunilsung.ods.server.realtime.EventHub
import com.eunilsung.ods.server.realtime.QuoteTicker
import com.eunilsung.ods.server.repository.StockRepository
import com.eunilsung.ods.server.module
import com.eunilsung.ods.server.seed.SeedAccounts
import com.eunilsung.ods.shared.api.ApiError
import com.eunilsung.ods.shared.api.LoginRequestDto
import com.eunilsung.ods.shared.api.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.KSerializer
import kotlin.random.Random

/** 테스트 서버 한 대 분의 알림 통로와 시세 흉내. 시세는 테스트가 [QuoteTicker.tick] 을 부를 때만 움직인다. */
class TestServer {
    val hub = EventHub()
    val ticker = QuoteTicker(StockRepository(TestDatabase.dataSource), hub, Random(SEED))

    private companion object {
        const val SEED = 7
    }
}

/** 시연용 데이터가 처음 상태인 DB 로 서버를 띄워 [block] 을 돌린다. */
fun serverTest(block: suspend ApplicationTestBuilder.(TestServer) -> Unit) {
    TestDatabase.clean()
    val server = TestServer()
    testApplication {
        application { module(TestDatabase.dataSource, server.hub, server.ticker, startTicker = false) }
        block(server)
    }
}

suspend fun HttpClient.login(id: String, password: String): HttpResponse =
    post("/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(ServerJson.encodeToString(LoginRequestDto.serializer(), LoginRequestDto(id, password)))
    }

suspend fun HttpClient.loginToken(id: String): String =
    login(id, SeedAccounts.PASSWORD).decode(LoginResponse.serializer()).token

suspend fun HttpClient.authGet(token: String, path: String): HttpResponse =
    get(path) { header(HttpHeaders.Authorization, "Bearer $token") }

suspend fun <T> HttpClient.authPost(token: String, path: String, serializer: KSerializer<T>, body: T): HttpResponse =
    post(path) {
        header(HttpHeaders.Authorization, "Bearer $token")
        contentType(ContentType.Application.Json)
        setBody(ServerJson.encodeToString(serializer, body))
    }

suspend fun <T> HttpClient.authPut(token: String, path: String, serializer: KSerializer<T>, body: T): HttpResponse =
    put(path) {
        header(HttpHeaders.Authorization, "Bearer $token")
        contentType(ContentType.Application.Json)
        setBody(ServerJson.encodeToString(serializer, body))
    }

suspend fun <T> HttpResponse.decode(serializer: KSerializer<T>): T = ServerJson.decodeFromString(serializer, bodyAsText())

suspend fun HttpResponse.errorCode(): String = decode(ApiError.serializer()).code
