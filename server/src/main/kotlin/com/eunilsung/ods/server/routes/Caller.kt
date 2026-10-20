package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.ApiException
import com.eunilsung.ods.server.repository.AuthTokenRepository
import com.eunilsung.ods.shared.api.ApiError
import com.eunilsung.ods.shared.api.ApiErrorCode
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 이 요청을 보낸 직원의 아이디.
 *
 * 본문이나 쿼리에 적힌 아이디는 보지 않는다. 그 값은 얼마든지 지어낼 수 있어서, 믿는 순간 남의
 * 일정을 읽고 남인 척 상담 이력을 남길 수 있게 된다. 신원은 오직 토큰이 정한다.
 *
 * 토큰이 없거나 모르는 값이면 401 로 답하고 null 을 돌려준다 — 호출한 쪽은 그대로 return 하면 된다.
 */
suspend fun ApplicationCall.callerStaffId(tokens: AuthTokenRepository): String? {
    val staffId = db { tokens.staffIdOf(bearerToken()) }
    if (staffId == null) {
        respond(HttpStatusCode.Unauthorized, ApiError(ApiErrorCode.UNAUTHORIZED, "다시 로그인해 주세요"))
    }
    return staffId
}

/** `Authorization: Bearer` 헤더의 토큰. 없으면 빈 문자열. */
fun ApplicationCall.bearerToken(): String {
    val header = request.headers[HttpHeaders.Authorization].orEmpty()
    return if (header.startsWith(BEARER)) header.removePrefix(BEARER).trim() else ""
}

/** 본문을 읽는다. 모양이 틀리면 400 으로 거절한다. */
suspend inline fun <reified T : Any> ApplicationCall.receiveBody(): T =
    runCatching { receive<T>() }.getOrElse { throw ApiException.badRequest("요청 형식이 올바르지 않습니다") }

/** 경로의 `{id}`. */
fun ApplicationCall.pathId(): String = parameters["id"].orEmpty()

/** 받을 수 있는 값인지 본다. 아니면 400. */
fun requireValid(isValid: Boolean, message: String) {
    if (!isValid) throw ApiException.badRequest(message)
}

/** JDBC 는 스레드를 붙잡는다. 요청을 처리하는 스레드가 막히지 않게 IO 쪽에서 돌린다. */
suspend fun <T> db(block: () -> T): T = withContext(Dispatchers.IO) { block() }

private const val BEARER = "Bearer "
