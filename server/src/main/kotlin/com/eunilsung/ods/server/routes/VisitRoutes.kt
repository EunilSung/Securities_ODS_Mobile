package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.ApiException
import com.eunilsung.ods.server.realtime.EventHub
import com.eunilsung.ods.server.repository.AuthTokenRepository
import com.eunilsung.ods.server.repository.VisitRepository
import com.eunilsung.ods.server.rules.Enums
import com.eunilsung.ods.shared.api.NewVisitRequest
import com.eunilsung.ods.shared.api.ServerEvent
import com.eunilsung.ods.shared.api.VisitStatusRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

/** 방문 일정 — 로그인한 직원의 것만. 바뀌면 그 직원의 연결에만 알린다. */
fun Route.visitRoutes(visits: VisitRepository, tokens: AuthTokenRepository, hub: EventHub) {

    suspend fun announce(staffId: String) =
        hub.send(staffId, ServerEvent(ServerEvent.TYPE_CHANGED, topic = ServerEvent.TOPIC_VISITS))

    get("/visits") {
        val staffId = call.callerStaffId(tokens) ?: return@get
        call.respond(db { visits.visitsOf(staffId) })
    }

    post("/visits") {
        val staffId = call.callerStaffId(tokens) ?: return@post
        val request = call.receiveBody<NewVisitRequest>()
        requireValid(request.purpose in Enums.VISIT_PURPOSE, "방문 목적이 올바르지 않습니다")

        val visit = db {
            visits.add(
                staffId = staffId,
                customerId = request.customerId,
                scheduledAtMillis = request.scheduledAtMillis,
                place = request.place.trim(),
                purpose = request.purpose,
                memo = request.memo.trim(),
            )
        } ?: throw ApiException.notFound("고객을 찾을 수 없습니다")
        announce(staffId)
        call.respond(HttpStatusCode.Created, visit)
    }

    /** 남의 일정은 없는 일정과 같은 404 로 답한다. */
    put("/visits/{id}/status") {
        val staffId = call.callerStaffId(tokens) ?: return@put
        val request = call.receiveBody<VisitStatusRequest>()
        requireValid(request.status in Enums.VISIT_STATUS, "일정 상태가 올바르지 않습니다")

        val updated = db { visits.updateStatus(staffId, call.pathId(), request.status) }
        if (!updated) throw ApiException.notFound("일정을 찾을 수 없습니다")
        announce(staffId)
        call.respond(HttpStatusCode.NoContent)
    }
}
