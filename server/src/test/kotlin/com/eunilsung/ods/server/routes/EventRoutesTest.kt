package com.eunilsung.ods.server.routes

import com.eunilsung.ods.domain.util.lowerLimitPrice
import com.eunilsung.ods.domain.util.upperLimitPrice
import com.eunilsung.ods.server.ServerJson
import com.eunilsung.ods.server.realtime.EventHub
import com.eunilsung.ods.server.testsupport.authGet
import com.eunilsung.ods.server.testsupport.authPost
import com.eunilsung.ods.server.testsupport.authPut
import com.eunilsung.ods.server.testsupport.decode
import com.eunilsung.ods.server.testsupport.loginToken
import com.eunilsung.ods.server.testsupport.serverTest
import com.eunilsung.ods.shared.api.NewConsultationRequest
import com.eunilsung.ods.shared.api.QuoteDto
import com.eunilsung.ods.shared.api.ServerEvent
import com.eunilsung.ods.shared.api.StockDto
import com.eunilsung.ods.shared.api.VisitStatusRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 실시간 알림 — 시세 체결과 "바뀌었다" 알림이 붙어 있는 연결에 실제로 닿는지 본다. */
class EventRoutesTest {

    @Test
    fun `시세가 움직이면 붙어 있는 연결이 체결 알림을 받고 DB 시세도 바뀐다`() = serverTest { server ->
        val token = client.loginToken("test1")
        val socketClient = createClient { install(WebSockets) }
        var moved: QuoteDto? = null

        socketClient.webSocket("/ws?token=$token") {
            server.hub.awaitConnections(1)
            moved = assertNotNull(server.ticker.tick("005930"))

            val event = nextEvent()
            assertEquals(ServerEvent.TYPE_QUOTE, event.type)
            assertEquals(moved, event.quote)
        }
        val stock = client.authGet(token, "/stocks").decode(ListSerializer(StockDto.serializer())).first { it.code == "005930" }
        assertEquals(moved, QuoteDto(stock.code, stock.price, stock.volume))
        assertTrue(stock.volume in 12_345_678L + 100..12_345_678L + 5_000)
    }

    @Test
    fun `여러 번 움직여도 상한가와 하한가를 넘지 않는다`() = serverTest { server ->
        repeat(300) { server.ticker.tick("035420") }

        val naver = client.authGet(client.loginToken("test1"), "/stocks")
            .decode(ListSerializer(StockDto.serializer())).first { it.code == "035420" }
        assertTrue(naver.price in lowerLimitPrice(201_000)..upperLimitPrice(201_000), "${naver.price}")
    }

    @Test
    fun `관심종목은 시연용 순서대로 온다`() = serverTest {
        val codes = client.authGet(client.loginToken("test1"), "/stocks").decode(ListSerializer(StockDto.serializer())).map { it.code }

        assertEquals(listOf("005930", "000660", "035420", "035720", "373220", "247540", "091990"), codes)
    }

    @Test
    fun `고객 장부를 고치면 다른 직원도 바뀌었다는 알림을 받는다`() = serverTest { server ->
        val writer = client.loginToken("test1")
        val listener = client.loginToken("test2")
        val socketClient = createClient { install(WebSockets) }

        socketClient.webSocket("/ws?token=$listener") {
            server.hub.awaitConnections(1)
            client.authPost(writer, "/customers/C001/consultations", NewConsultationRequest.serializer(), NewConsultationRequest("PHONE", "GENERAL", "알림 확인"))

            val event = nextEvent()
            assertEquals(ServerEvent.TYPE_CHANGED, event.type)
            assertEquals(ServerEvent.TOPIC_CUSTOMERS, event.topic)
        }
    }

    @Test
    fun `방문 일정 알림은 일정 주인에게만 간다`() = serverTest { server ->
        val owner = client.loginToken("test1")
        val other = client.loginToken("test2")
        val socketClient = createClient { install(WebSockets) }

        socketClient.webSocket("/ws?token=$other") {
            server.hub.awaitConnections(1)
            socketClient.webSocket("/ws?token=$owner") {
                server.hub.awaitConnections(2)
                client.authPut(owner, "/visits/V001/status", VisitStatusRequest.serializer(), VisitStatusRequest("CANCELED"))

                val event = nextEvent()
                assertEquals(ServerEvent.TOPIC_VISITS, event.topic)
            }
            assertNull(runCatching { withTimeout(SILENCE_WAIT_MS) { nextEvent() } }.getOrNull())
        }
    }

    @Test
    fun `지어낸 토큰으로는 붙지 못한다`() = serverTest {
        val socketClient = createClient { install(WebSockets) }

        socketClient.webSocket("/ws?token=made-up") {
            val reason = withTimeout(EVENT_WAIT_MS) { closeReason.await() }
            assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, reason?.code)
        }
    }

    private suspend fun EventHub.awaitConnections(count: Int) {
        repeat(CONNECT_RETRIES) {
            if (connectionCount() >= count) return
            delay(CONNECT_RETRY_DELAY_MS)
        }
        error("연결이 $count 개가 되지 않았다")
    }

    private suspend fun DefaultClientWebSocketSession.nextEvent(): ServerEvent = withTimeout(EVENT_WAIT_MS) {
        val frame = incoming.receive() as Frame.Text
        ServerJson.decodeFromString(ServerEvent.serializer(), frame.readText())
    }

    private companion object {
        const val EVENT_WAIT_MS = 5_000L
        const val SILENCE_WAIT_MS = 500L
        const val CONNECT_RETRIES = 100
        const val CONNECT_RETRY_DELAY_MS = 20L
    }
}
