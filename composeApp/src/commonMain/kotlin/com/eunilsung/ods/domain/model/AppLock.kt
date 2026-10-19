package com.eunilsung.ods.domain.model

/**
 * 앱 잠금 상태.
 *
 * @param isPinSet 지금 로그인한 직원이 앱 잠금 비밀번호를 정해 두었는지. 다른 직원의 비밀번호는 쓰지 않는다.
 * @param isLocked 잠겨 있는지. 잠겨 있으면 업무 화면 위를 잠금 화면이 덮는다.
 */
data class AppLockState(
    val isPinSet: Boolean = false,
    val biometricEnabled: Boolean = false,
    val isLocked: Boolean = false,
)

/** 앱 잠금 비밀번호 확인 결과. */
sealed interface UnlockResult {
    data object Unlocked : UnlockResult

    /** 틀렸다. [remainingAttempts] 번 더 틀리면 업무 데이터를 지운다. */
    data class Wrong(val remainingAttempts: Int) : UnlockResult

    /** 너무 많이 틀려 기기 안 업무 데이터를 지우고 로그아웃했다. */
    data object Wiped : UnlockResult
}

/** 로그아웃한 까닭. 로그인 화면이 안내 문구를 고르는 데 쓴다. */
enum class LogoutReason {
    /** 직원이 직접 로그아웃했다. */
    USER,

    /** 앱 잠금 비밀번호를 너무 많이 틀려 업무 데이터를 지웠다. */
    WIPED,
}
