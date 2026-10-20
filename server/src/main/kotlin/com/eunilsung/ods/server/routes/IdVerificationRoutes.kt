package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.repository.AuthTokenRepository
import com.eunilsung.ods.server.rules.Enums
import com.eunilsung.ods.shared.api.IdVerificationRequest
import com.eunilsung.ods.shared.api.IdVerificationResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/**
 * 신분증 진위 확인 — 발급기관에 묻는 대신 흉내 낸다. 이름에 "분실" 이 들어 있으면 분실 신고된 신분증으로 본다.
 *
 * 주민등록번호 앞자리·뒷자리 첫 숫자는 형식만 보고 버린다. 저장하지도 로그에 남기지도 않는다.
 */
fun Route.idVerificationRoutes(tokens: AuthTokenRepository) {
    post("/id-verifications") {
        call.callerStaffId(tokens) ?: return@post
        val request = call.receiveBody<IdVerificationRequest>()
        requireValid(request.type in Enums.ID_CARD_TYPE, "신분증 종류가 올바르지 않습니다")
        requireValid(request.rrnFront.length == RRN_FRONT_LENGTH && request.rrnFront.all(Char::isDigit), "주민등록번호 앞자리가 올바르지 않습니다")
        requireValid(request.rrnBackFirst.length == 1 && request.rrnBackFirst[0].isDigit(), "주민등록번호 뒷자리 첫 숫자가 올바르지 않습니다")

        call.respond(IdVerificationResponse(verified = LOST_CARD_MARK !in request.name, verifiedAtMillis = System.currentTimeMillis()))
    }
}

private const val RRN_FRONT_LENGTH = 6
private const val LOST_CARD_MARK = "분실"
