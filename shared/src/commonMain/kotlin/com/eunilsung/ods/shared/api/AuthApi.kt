package com.eunilsung.ods.shared.api

import kotlinx.serialization.Serializable

/** `POST /auth/login` */
@Serializable
data class LoginRequestDto(val id: String, val password: String)

/**
 * 로그인 성공. 이후 요청은 `Authorization: Bearer <token>` 을 단다. 실시간 알림(`/ws`)은 `?token=` 으로 붙는다.
 * 실패는 401 [ApiErrorCode.INVALID_CREDENTIALS](남은 횟수) 또는 429 [ApiErrorCode.TOO_MANY_ATTEMPTS](풀리는 시각)다.
 */
@Serializable
data class LoginResponse(val token: String, val staff: StaffDto)

@Serializable
data class StaffDto(
    val id: String,
    val name: String,
    val branch: String,
    val position: String,
)
