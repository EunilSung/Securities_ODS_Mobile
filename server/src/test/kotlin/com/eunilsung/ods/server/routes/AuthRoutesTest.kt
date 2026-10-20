package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.seed.SeedAccounts
import com.eunilsung.ods.server.testsupport.TestDatabase
import com.eunilsung.ods.server.testsupport.authGet
import com.eunilsung.ods.server.testsupport.decode
import com.eunilsung.ods.server.testsupport.login
import com.eunilsung.ods.server.testsupport.loginToken
import com.eunilsung.ods.server.testsupport.serverTest
import com.eunilsung.ods.shared.api.ApiError
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.shared.api.LoginResponse
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthRoutesTest {

    @Test
    fun `맞는 아이디와 비밀번호면 토큰과 직원 정보를 준다`() = serverTest {
        val response = client.login("  TEST1 ", SeedAccounts.PASSWORD)

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.decode(LoginResponse.serializer())
        assertTrue(body.token.isNotBlank())
        assertEquals("test1", body.staff.id)
        assertEquals("김민준", body.staff.name)
        assertEquals("강남금융센터", body.staff.branch)
    }

    @Test
    fun `비밀번호가 틀리면 401 과 남은 횟수를 주고 토큰은 주지 않는다`() = serverTest {
        val response = client.login("test1", "wrong")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val text = response.bodyAsText()
        val error = response.decode(ApiError.serializer())
        assertEquals(ApiErrorCode.INVALID_CREDENTIALS, error.code)
        assertEquals(4, error.remainingAttempts)
        assertFalse("token" in text)
    }

    @Test
    fun `없는 아이디도 틀린 비밀번호와 같은 응답을 받고 똑같이 잠긴다`() = serverTest {
        val unknown = client.login("nobody", SeedAccounts.PASSWORD)
        val wrong = client.login("test2", "wrong")

        assertEquals(wrong.status, unknown.status)
        assertEquals(wrong.bodyAsText(), unknown.bodyAsText())

        repeat(4) { client.login("nobody", "x") }
        assertEquals(HttpStatusCode.TooManyRequests, client.login("nobody", "x").status)
    }

    @Test
    fun `다섯 번 틀리면 429 로 잠기고 맞는 비밀번호도 받지 않는다`() = serverTest {
        repeat(4) { assertEquals(HttpStatusCode.Unauthorized, client.login("test3", "wrong-$it").status) }

        val fifth = client.login("test3", "wrong")
        val locked = client.login("test3", SeedAccounts.PASSWORD)

        for (response in listOf(fifth, locked)) {
            assertEquals(HttpStatusCode.TooManyRequests, response.status)
            val error = response.decode(ApiError.serializer())
            assertEquals(ApiErrorCode.TOO_MANY_ATTEMPTS, error.code)
            val until = assertNotNull(error.lockedUntilEpochSeconds)
            assertTrue(until > System.currentTimeMillis() / 1000)
            assertTrue((response.headers[HttpHeaders.RetryAfter]?.toLongOrNull() ?: 0) in 1..300)
        }
        assertEquals(HttpStatusCode.OK, client.login("test1", SeedAccounts.PASSWORD).status)
    }

    @Test
    fun `토큰이 없거나 지어낸 값이면 401 이다`() = serverTest {
        for (path in listOf("/customers", "/products", "/visits", "/stocks")) {
            val missing = client.get(path)
            assertEquals(HttpStatusCode.Unauthorized, missing.status, path)
            assertEquals(ApiErrorCode.UNAUTHORIZED, missing.decode(ApiError.serializer()).code)
            assertEquals(HttpStatusCode.Unauthorized, client.authGet("made-up-token", path).status, path)
        }
        assertEquals(HttpStatusCode.Unauthorized, client.post("/auth/logout").status)
    }

    @Test
    fun `로그아웃하면 그 토큰만 못 쓰게 된다`() = serverTest {
        val phone = client.loginToken("test1")
        val tablet = client.loginToken("test1")

        val logout = client.post("/auth/logout") { header(HttpHeaders.Authorization, "Bearer $phone") }

        assertEquals(HttpStatusCode.NoContent, logout.status)
        assertEquals(HttpStatusCode.Unauthorized, client.authGet(phone, "/visits").status)
        assertEquals(HttpStatusCode.OK, client.authGet(tablet, "/visits").status)
    }

    @Test
    fun `DB 에는 비밀번호도 토큰도 평문으로 남지 않는다`() = serverTest {
        val token = client.loginToken("test1")

        val stored = TestDatabase.dataSource.connection.use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT token_hash FROM auth_token").use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } }
            }
        }

        assertEquals(1, stored.size)
        assertFalse(token in stored)
    }
}
