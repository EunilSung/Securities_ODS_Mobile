package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.auth.LoginAttemptLimiter
import com.eunilsung.ods.server.auth.PasswordHasher
import com.eunilsung.ods.server.repository.AuthTokenRepository
import com.eunilsung.ods.server.repository.StaffRepository
import com.eunilsung.ods.shared.api.ApiError
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.shared.api.LoginRequestDto
import com.eunilsung.ods.shared.api.LoginResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

/** 로그인·로그아웃. */
fun Route.authRoutes(staff: StaffRepository, tokens: AuthTokenRepository, limiter: LoginAttemptLimiter) {

    /**
     * 로그인. 아이디가 없는 것과 비밀번호가 틀린 것을 같은 응답으로 돌려준다 —
     * 구분해 주면 어떤 아이디가 있는지 밖에서 알아낼 수 있다.
     */
    post("/auth/login") {
        val request = call.receiveBody<LoginRequestDto>()
        val id = request.id.trim().lowercase()
        requireValid(id.isNotEmpty() && request.password.isNotEmpty(), "아이디와 비밀번호가 필요합니다")

        /** 잠겨 있는 동안에는 비밀번호를 확인하지 않는다. 맞는 비밀번호여도 받지 않아야 넣어 보기가 멈춘다. */
        limiter.lockedUntil(id)?.let { until ->
            call.respondLocked(until, limiter.now())
            return@post
        }

        val response = db {
            val credential = staff.findCredential(id)
            /**
             * 없는 아이디여도 해시 검증을 한 번 돌린다. 건너뛰면 응답이 눈에 띄게 빨라져서,
             * 걸린 시간만으로 아이디가 있는지 알아낼 수 있다.
             */
            val isValid = PasswordHasher.verify(request.password, credential?.passwordHash ?: UNKNOWN_STAFF_HASH)
            if (credential != null && isValid) LoginResponse(tokens.issue(credential.staff.id), credential.staff) else null
        }

        if (response != null) {
            limiter.recordSuccess(id)
            call.respond(response)
            return@post
        }
        val failure = limiter.recordFailure(id)
        val lockedUntil = failure.lockedUntilEpochSeconds
        if (lockedUntil != null) {
            call.respondLocked(lockedUntil, limiter.now())
        } else {
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError(
                    ApiErrorCode.INVALID_CREDENTIALS,
                    "아이디 또는 비밀번호가 올바르지 않습니다",
                    remainingAttempts = failure.remainingAttempts,
                ),
            )
        }
    }

    /** 로그아웃 — 이 기기의 토큰만 없앤다. 다른 기기의 로그인은 그대로다. */
    post("/auth/logout") {
        call.callerStaffId(tokens) ?: return@post
        val token = call.bearerToken()
        db { tokens.revoke(token) }
        call.respond(HttpStatusCode.NoContent)
    }
}

private suspend fun ApplicationCall.respondLocked(lockedUntilEpochSeconds: Long, nowEpochSeconds: Long) {
    response.header(HttpHeaders.RetryAfter, (lockedUntilEpochSeconds - nowEpochSeconds).coerceAtLeast(1).toString())
    respond(
        HttpStatusCode.TooManyRequests,
        ApiError(
            ApiErrorCode.TOO_MANY_ATTEMPTS,
            "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요",
            lockedUntilEpochSeconds = lockedUntilEpochSeconds,
        ),
    )
}

/** 없는 아이디를 검증할 때 쓰는 자리 채움 해시. 어떤 비밀번호와도 맞지 않는다. */
private val UNKNOWN_STAFF_HASH: String by lazy { PasswordHasher.hash("unknown-staff-placeholder") }
