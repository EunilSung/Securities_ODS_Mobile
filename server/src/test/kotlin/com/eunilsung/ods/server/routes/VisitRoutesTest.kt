package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.testsupport.authGet
import com.eunilsung.ods.server.testsupport.authPost
import com.eunilsung.ods.server.testsupport.authPut
import com.eunilsung.ods.server.testsupport.decode
import com.eunilsung.ods.server.testsupport.loginToken
import com.eunilsung.ods.server.testsupport.serverTest
import com.eunilsung.ods.shared.api.NewVisitRequest
import com.eunilsung.ods.shared.api.VisitDto
import com.eunilsung.ods.shared.api.VisitStatusRequest
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 방문 일정은 직원마다 따로다 — 남의 일정은 보이지도 바뀌지도 않는다. */
class VisitRoutesTest {

    @Test
    fun `직원마다 자기 일정만 시각 순으로 받는다`() = serverTest {
        val mine = client.visits(client.loginToken("test1"))
        val theirs = client.visits(client.loginToken("test2"))

        assertEquals(setOf("V001", "V002", "V003", "V004", "V005", "V006", "V007"), mine.map { it.id }.toSet())
        assertEquals(mine.sortedBy { it.scheduledAtMillis }, mine)
        assertEquals(setOf("V008", "V009"), theirs.map { it.id }.toSet())
        val past = mine.first { it.id == "V006" }
        assertEquals("COMPLETED", past.status)
        assertEquals("강하늘", past.customerName)
        assertEquals("SCHEDULED", mine.first { it.id == "V001" }.status)
    }

    @Test
    fun `새 일정은 예정 상태로 내 목록에만 생긴다`() = serverTest {
        val me = client.loginToken("test2")
        val other = client.loginToken("test3")

        val response = client.authPost(
            me, "/visits", NewVisitRequest.serializer(),
            NewVisitRequest("C003", 1_900_000_000_000, "판교", "PRODUCT", "자료 준비"),
        )

        assertEquals(HttpStatusCode.Created, response.status)
        val visit = response.decode(VisitDto.serializer())
        assertEquals("SCHEDULED", visit.status)
        assertEquals("박준호", visit.customerName)
        assertTrue(client.visits(me).any { it.id == visit.id })
        assertTrue(client.visits(other).none { it.id == visit.id })
    }

    @Test
    fun `없는 고객으로는 일정을 만들 수 없다`() = serverTest {
        val response = client.authPost(
            client.loginToken("test1"), "/visits", NewVisitRequest.serializer(),
            NewVisitRequest("C999", 1_900_000_000_000, "어딘가", "CONSULT", ""),
        )

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `내 일정의 상태는 바꾸고 남의 일정은 없는 일정과 같이 404 다`() = serverTest {
        val owner = client.loginToken("test1")
        val stranger = client.loginToken("test2")

        val byStranger = client.changeStatus(stranger, "V001", "CANCELED")
        val missing = client.changeStatus(stranger, "V999", "CANCELED")
        val byOwner = client.changeStatus(owner, "V001", "COMPLETED")

        assertEquals(HttpStatusCode.NotFound, byStranger.status)
        assertEquals(missing.status, byStranger.status)
        assertEquals(HttpStatusCode.NoContent, byOwner.status)
        assertEquals("COMPLETED", client.visits(owner).first { it.id == "V001" }.status)
        assertEquals(HttpStatusCode.BadRequest, client.changeStatus(owner, "V001", "DONE").status)
    }

    private suspend fun HttpClient.visits(token: String): List<VisitDto> =
        authGet(token, "/visits").decode(ListSerializer(VisitDto.serializer()))

    private suspend fun HttpClient.changeStatus(token: String, visitId: String, status: String) =
        authPut(token, "/visits/$visitId/status", VisitStatusRequest.serializer(), VisitStatusRequest(status))
}
