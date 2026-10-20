package com.eunilsung.ods.server

import com.eunilsung.ods.shared.api.ApiError
import com.eunilsung.ods.shared.api.ApiErrorCode
import io.ktor.http.HttpStatusCode

/**
 * 요청을 거절한다. 저장소가 트랜잭션 안에서 던지면 쓰던 것이 되돌려지고, [module] 이 [error] 를 그대로 답한다.
 *
 * 규칙 확인을 저장과 한 트랜잭션에 두려고 쓴다 — 확인한 뒤 따로 저장하면 그 사이에 다른 요청이 끼어든다.
 */
class ApiException(val status: HttpStatusCode, val error: ApiError) : RuntimeException(error.code) {
    companion object {
        fun badRequest(message: String) = ApiException(HttpStatusCode.BadRequest, ApiError(ApiErrorCode.BAD_REQUEST, message))

        fun notFound(message: String) = ApiException(HttpStatusCode.NotFound, ApiError(ApiErrorCode.NOT_FOUND, message))

        fun conflict(code: String, message: String) = ApiException(HttpStatusCode.Conflict, ApiError(code, message))
    }
}
