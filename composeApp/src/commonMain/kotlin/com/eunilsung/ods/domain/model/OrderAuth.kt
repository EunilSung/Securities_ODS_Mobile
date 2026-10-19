package com.eunilsung.ods.domain.model

/** 생체 인증 결과. */
enum class BiometricResult {
    SUCCESS,

    /** 인식은 됐지만 맞지 않았다. */
    FAILED,

    /** 사용자가 취소했다(취소 버튼·시스템 취소). */
    CANCELED,

    /** 쓸 수 없다 — 기기 미지원, 등록된 생체 없음. */
    UNAVAILABLE,
}

/**
 * 주문 인증 설정.
 *
 * @param lockedUntilEpochSeconds 비밀번호를 여러 번 틀려 잠겼으면 풀리는 시각. 잠기지 않았으면 null.
 */
data class OrderAuthConfig(
    val isPinSet: Boolean,
    val biometricEnabled: Boolean,
    val lockedUntilEpochSeconds: Long?,
)

/** 주문 비밀번호 확인 결과. */
sealed interface PinCheckResult {
    data object Matched : PinCheckResult

    /** 틀렸다. [remainingAttempts] 번 더 틀리면 잠긴다. */
    data class Wrong(val remainingAttempts: Int) : PinCheckResult

    /** 잠겨 있다(이번에 잠겼거나 이미 잠겨 있었다). */
    data class Locked(val untilEpochSeconds: Long) : PinCheckResult
}
