package com.eunilsung.ods.domain.model

/**
 * 로그인한 직원 — 지점 밖에서 고객을 만나는 영업 직원.
 *
 * @param id 사번. 로그인 아이디로 쓴다.
 * @param branch 소속 지점.
 * @param position 직급.
 */
data class Staff(
    val id: String,
    val name: String,
    val branch: String,
    val position: String,
)

data class LoginRequest(
    val id: String,
    val password: String,
)

/**
 * 로그인 결과. 사번이 없는 것과 비밀번호가 틀린 것을 가르지 않는다 — 어느 사번이 있는지 드러내지 않기 위해서다.
 */
sealed interface LoginResult {
    data class Success(val staff: Staff) : LoginResult

    /** 사번 또는 비밀번호가 맞지 않다. [remainingAttempts] 번 더 틀리면 잠긴다. */
    data class Wrong(val remainingAttempts: Int) : LoginResult

    data class Locked(val untilEpochSeconds: Long) : LoginResult

    /** 로그인을 마치지 못했다(기기 저장소 오류 등). 다시 시도할 수 있다. */
    data object Failed : LoginResult
}
