package com.eunilsung.ods.server.realtime

import com.eunilsung.ods.server.ServerJson
import com.eunilsung.ods.shared.api.ServerEvent
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 지금 붙어 있는 WebSocket 연결들.
 *
 * 한 직원이 여러 기기로 붙을 수 있어 직원마다 연결을 여러 개 둔다. 보내다 실패한 연결은 조용히
 * 넘어간다 — 끊긴 쪽은 다시 붙을 때 REST 로 목록을 다시 받으므로, 여기서 재시도할 이유가 없다.
 */
class EventHub {

    private val sessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    fun join(staffId: String, session: WebSocketSession) {
        sessions.computeIfAbsent(staffId) { CopyOnWriteArraySet() }.add(session)
    }

    fun leave(staffId: String, session: WebSocketSession) {
        sessions.computeIfPresent(staffId) { _, set ->
            set.remove(session)
            set.takeIf { it.isNotEmpty() }
        }
    }

    /** 붙어 있는 연결 수. 시세 흉내는 0 이면 쉰다. */
    fun connectionCount(): Int = sessions.values.sumOf { it.size }

    /** 붙어 있는 모두에게 — 시세와 지점 고객 장부는 직원 모두의 것이다. */
    suspend fun broadcast(event: ServerEvent) {
        sendTo(sessions.values.flatten(), event)
    }

    /** 이 직원의 연결에만 — 방문 일정은 직원마다 따로다. */
    suspend fun send(staffId: String, event: ServerEvent) {
        sendTo(sessions[staffId].orEmpty(), event)
    }

    private suspend fun sendTo(targets: Collection<WebSocketSession>, event: ServerEvent) {
        if (targets.isEmpty()) return
        val frameText = ServerJson.encodeToString(ServerEvent.serializer(), event)
        targets.forEach { session -> runCatching { session.send(Frame.Text(frameText)) } }
    }
}
