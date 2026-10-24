package com.eunilsung.ods.domain.util

/** 로그인 잠금 규칙 — 같은 아이디로 연달아 [MAX_ATTEMPTS] 번 틀리면 [LOCK_SECONDS] 동안 막는다. 틀린 횟수는 기기가 센다. */
object LoginPolicy {
    const val MAX_ATTEMPTS = 5
    const val LOCK_SECONDS = 5 * 60L
}

/** 계좌번호 가리기 — 앞 4자리와 끝 2자리만 남기고 숫자를 `*` 로 바꾼다. 예) 1234-5678-01 → 1234-****-01 */
fun maskAccountNo(accountNo: String): String {
    val digitCount = accountNo.count(Char::isDigit)
    var seen = 0
    return accountNo.map { char ->
        if (!char.isDigit()) return@map char
        seen++
        if (seen <= 4 || seen > digitCount - 2) char else '*'
    }.joinToString("")
}
