package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.testsupport.TestDatabase
import com.eunilsung.ods.server.testsupport.decode
import com.eunilsung.ods.server.testsupport.serverTest
import com.eunilsung.ods.shared.api.HealthResponse
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthRoutesTest {

    @Test
    fun `DB 가 살아 있으면 토큰 없이도 ok 로 답한다`() = serverTest {
        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.decode(HealthResponse.serializer()).status)
    }

    @Test
    fun `마이그레이션이 업무 표를 모두 만든다`() {
        val tables = TestDatabase.dataSource.connection.use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'").use { rs ->
                    buildSet { while (rs.next()) add(rs.getString(1)) }
                }
            }
        }

        val expected = setOf(
            "staff", "auth_token", "customer", "account", "consultation", "subscription", "signature", "product", "visit", "stock",
        )
        assertTrue(tables.containsAll(expected), "빠진 표: ${expected - tables}")
    }
}
