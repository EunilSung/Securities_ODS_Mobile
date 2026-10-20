package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.realtime.EventHub
import com.eunilsung.ods.server.repository.AuthTokenRepository
import com.eunilsung.ods.shared.api.ApiErrorCode
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close

/**
 * 실시간 알림 — `/ws?token=`.
 *
 * 바꾸는 요청은 전부 REST 로 받는다. WebSocket 은 시세 체결과 "무엇이 바뀌었다" 를 알리는 데만 쓴다 —
 * 연결이 끊겨도 요청이 사라지지 않고, 다시 붙은 쪽은 목록만 다시 받으면 같은 상태가 된다.
 */
fun Route.eventRoutes(tokens: AuthTokenRepository, hub: EventHub) {
    webSocket("/ws") {
        val token = call.request.queryParameters["token"].orEmpty()
        val staffId = db { tokens.staffIdOf(token) }
        if (staffId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, ApiErrorCode.UNAUTHORIZED))
            return@webSocket
        }
        hub.join(staffId, this)
        try {
            for (frame in incoming) Unit
        } finally {
            hub.leave(staffId, this)
        }
    }
}
