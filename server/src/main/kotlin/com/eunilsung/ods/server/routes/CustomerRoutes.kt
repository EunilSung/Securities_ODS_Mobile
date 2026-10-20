package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.ApiException
import com.eunilsung.ods.server.realtime.EventHub
import com.eunilsung.ods.server.repository.AuthTokenRepository
import com.eunilsung.ods.server.repository.CustomerRepository
import com.eunilsung.ods.server.repository.StaffRepository
import com.eunilsung.ods.server.rules.Enums
import com.eunilsung.ods.shared.api.NewConsultationRequest
import com.eunilsung.ods.shared.api.NewCustomerRequest
import com.eunilsung.ods.shared.api.OpenAccountRequest
import com.eunilsung.ods.shared.api.RiskProfileRequest
import com.eunilsung.ods.shared.api.ServerEvent
import com.eunilsung.ods.shared.api.SignatureRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import java.time.LocalDate

/**
 * 지점 고객 장부 — 고객 등록, 계좌 개설, 투자성향, 상담 이력, 서명.
 *
 * 쓰고 나면 붙어 있는 모두에게 고객 장부가 바뀌었다고 알린다. 앱은 알림을 받고 장부를 다시 받는다.
 */
fun Route.customerRoutes(
    customers: CustomerRepository,
    staff: StaffRepository,
    tokens: AuthTokenRepository,
    hub: EventHub,
) {
    suspend fun announce() = hub.broadcast(ServerEvent(ServerEvent.TYPE_CHANGED, topic = ServerEvent.TOPIC_CUSTOMERS))

    get("/customers") {
        call.callerStaffId(tokens) ?: return@get
        call.respond(db { customers.book() })
    }

    post("/customers") {
        call.callerStaffId(tokens) ?: return@post
        val request = call.receiveBody<NewCustomerRequest>()
        val birthDate = runCatching { LocalDate.parse(request.birthDate) }.getOrNull()
        requireValid(request.name.isNotBlank(), "이름이 필요합니다")
        requireValid(birthDate != null, "생년월일 형식이 올바르지 않습니다")
        requireValid(request.gender in Enums.GENDER, "성별이 올바르지 않습니다")

        val created = db {
            customers.register(
                name = request.name.trim(),
                birthDate = birthDate!!,
                gender = request.gender,
                phone = request.phone,
                address = request.address.trim(),
                nowMillis = System.currentTimeMillis(),
            )
        }
        announce()
        call.respond(HttpStatusCode.Created, created)
    }

    post("/customers/{id}/accounts") {
        call.callerStaffId(tokens) ?: return@post
        val request = call.receiveBody<OpenAccountRequest>()
        requireValid(request.type in Enums.ACCOUNT_TYPE, "계좌 종류가 올바르지 않습니다")

        val account = db { customers.openAccount(call.pathId(), request.type, System.currentTimeMillis()) }
        announce()
        call.respond(HttpStatusCode.Created, account)
    }

    put("/customers/{id}/risk-profile") {
        call.callerStaffId(tokens) ?: return@put
        val request = call.receiveBody<RiskProfileRequest>()
        requireValid(request.profile in Enums.RISK_PROFILE, "투자성향이 올바르지 않습니다")

        val saved = db { customers.saveRiskProfile(call.pathId(), request.profile, request.assessedAtMillis) }
        if (!saved) throw customerNotFound()
        announce()
        call.respond(HttpStatusCode.NoContent)
    }

    /** 남긴 직원은 토큰의 직원이다. 본문에 직원 이름을 받지 않는다. */
    post("/customers/{id}/consultations") {
        val staffId = call.callerStaffId(tokens) ?: return@post
        val request = call.receiveBody<NewConsultationRequest>()
        requireValid(request.channel in Enums.CONSULT_CHANNEL, "상담 채널이 올바르지 않습니다")
        requireValid(request.topic in Enums.CONSULT_TOPIC, "상담 주제가 올바르지 않습니다")

        val consultation = db {
            val staffName = staff.find(staffId)?.name.orEmpty()
            customers.addConsultation(
                customerId = call.pathId(),
                channel = request.channel,
                topic = request.topic,
                content = request.content.trim(),
                staffName = staffName,
                nowMillis = System.currentTimeMillis(),
            )
        } ?: throw customerNotFound()
        announce()
        call.respond(HttpStatusCode.Created, consultation)
    }

    post("/customers/{id}/signatures") {
        call.callerStaffId(tokens) ?: return@post
        val request = call.receiveBody<SignatureRequest>()
        requireValid(request.purpose in Enums.SIGNATURE_PURPOSE, "서명 목적이 올바르지 않습니다")
        requireValid(request.strokes.any { it.isNotEmpty() }, "서명이 비어 있습니다")

        val record = db {
            customers.addSignature(call.pathId(), request.purpose, request.signedAtMillis, request.strokes)
        } ?: throw customerNotFound()
        announce()
        call.respond(HttpStatusCode.Created, record)
    }
}

private fun customerNotFound() = ApiException.notFound("고객을 찾을 수 없습니다")
