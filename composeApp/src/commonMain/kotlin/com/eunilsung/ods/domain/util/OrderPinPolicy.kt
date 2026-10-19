package com.eunilsung.ods.domain.util

/**
 * 주문 비밀번호 규칙 — 국내 증권사 계좌 비밀번호처럼 숫자 4자리.
 *
 * 로컬 모드에서는 기기가 틀린 횟수를 센다. 서버가 붙으면 서버가 계좌 비밀번호를 확인하고 횟수를 센다.
 */
object OrderPinPolicy {
    const val LENGTH = 4

    /** 이만큼 연달아 틀리면 [LOCK_SECONDS] 동안 주문 인증을 막는다. */
    const val MAX_ATTEMPTS = 5

    const val LOCK_SECONDS = 10 * 60L

    /** 같은 숫자만(1111)이거나 1씩 오르내리는(1234·4321) 비밀번호는 등록하지 않는다. */
    fun isWeak(pin: String): Boolean {
        if (pin.length != LENGTH || !pin.all(Char::isDigit)) return true
        val steps = pin.zipWithNext { a, b -> b - a }.distinct()
        return steps.size == 1 && steps.first() in -1..1
    }
}
