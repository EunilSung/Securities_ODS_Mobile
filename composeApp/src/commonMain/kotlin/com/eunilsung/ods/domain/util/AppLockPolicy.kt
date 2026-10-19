package com.eunilsung.ods.domain.util

/**
 * 앱 잠금 규칙 — 단말을 잃어버리거나 고객 앞에 둔 채 자리를 비웠을 때를 대비한다.
 *
 * 비밀번호는 숫자 [LENGTH] 자리이고, 연달아 [MAX_ATTEMPTS] 번 틀리면 기기 안 업무 데이터를 지우고 로그아웃한다.
 * 잠금을 시간으로 푸는 것이 아니라 지우는 것은, 주운 사람이 시간을 두고 계속 시도할 수 없게 하려는 것이다.
 */
object AppLockPolicy {
    const val LENGTH = 6

    const val MAX_ATTEMPTS = 5

    /** 이만큼 아무 조작이 없으면 잠근다. */
    const val IDLE_LOCK_SECONDS = 5 * 60L

    /** 백그라운드에 이보다 오래 있다가 돌아오면 잠근다. 잠깐 전화를 받는 정도는 봐준다. */
    const val BACKGROUND_LOCK_SECONDS = 60L

    /** 같은 숫자만(111111)이거나 1씩 오르내리는(123456·654321) 비밀번호는 등록하지 않는다. */
    fun isWeak(pin: String): Boolean {
        if (pin.length != LENGTH || !pin.all(Char::isDigit)) return true
        val steps = pin.zipWithNext { a, b -> b - a }.distinct()
        return steps.size == 1 && steps.first() in -1..1
    }
}
