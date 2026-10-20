package com.eunilsung.ods.server.auth

import java.util.concurrent.ConcurrentHashMap

/**
 * 로그인 시도 제한 — 앱 로컬 모드의 `LoginPolicy` 와 같은 규칙.
 *
 * 한 아이디에 틀린 비밀번호가 연달아 [maxFailures] 번 들어오면 그 순간부터 [lockSeconds] 동안 그 아이디의
 * 로그인을 받지 않는다. 잠금이 풀리면 처음부터 다시 센다. 없는 아이디도 똑같이 센다 —
 * 세지 않으면 잠기는지 아닌지로 어떤 아이디가 있는지 알아낼 수 있다.
 *
 * 아이디를 기준으로 센다. 주소를 기준으로 세면 프록시 뒤에서는 모든 사람이 한 주소로 보이고,
 * 공격하는 쪽은 주소를 바꿔 가며 피할 수 있다.
 *
 * 기록은 메모리에만 둔다. 서버를 다시 띄우면 지워지는데, 그 정도 틈은 받아들인다.
 */
class LoginAttemptLimiter(
    private val maxFailures: Int = DEFAULT_MAX_FAILURES,
    private val lockSeconds: Long = DEFAULT_LOCK_SECONDS,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) {

    /** 틀린 뒤의 상태. [lockedUntilEpochSeconds] 가 있으면 이번 실패로 잠겼다. */
    data class Failure(val remainingAttempts: Int, val lockedUntilEpochSeconds: Long?)

    private data class Record(val failures: Int, val lockedUntil: Long?)

    private val records = ConcurrentHashMap<String, Record>()

    fun now(): Long = nowEpochSeconds()

    /** 지금 잠겨 있으면 풀리는 시각(epoch 초), 아니면 null. 잠금이 끝났으면 기록을 지운다. */
    fun lockedUntil(staffId: String): Long? {
        val key = keyOf(staffId)
        val record = records[key] ?: return null
        val until = record.lockedUntil ?: return null
        if (now() < until) return until
        records.remove(key, record)
        return null
    }

    fun recordFailure(staffId: String): Failure {
        val record = records.compute(keyOf(staffId)) { _, current ->
            val failures = (current?.failures ?: 0) + 1
            Record(failures, if (failures >= maxFailures) now() + lockSeconds else null)
        }!!
        return Failure((maxFailures - record.failures).coerceAtLeast(0), record.lockedUntil)
    }

    fun recordSuccess(staffId: String) {
        records.remove(keyOf(staffId))
    }

    private fun keyOf(staffId: String): String = staffId.trim().lowercase()

    private companion object {
        const val DEFAULT_MAX_FAILURES = 5
        const val DEFAULT_LOCK_SECONDS = 5 * 60L
    }
}
