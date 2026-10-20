package com.eunilsung.ods.server.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginAttemptLimiterTest {

    private var clock = 1_000L
    private val limiter = LoginAttemptLimiter(maxFailures = 5, lockSeconds = 300, nowEpochSeconds = { clock })

    @Test
    fun `틀릴 때마다 남은 횟수가 줄고 다섯 번째에 5분 잠긴다`() {
        val remaining = (1..4).map { limiter.recordFailure("test1").remainingAttempts }
        assertEquals(listOf(4, 3, 2, 1), remaining)
        assertNull(limiter.lockedUntil("test1"))

        val fifth = limiter.recordFailure("test1")

        assertEquals(0, fifth.remainingAttempts)
        assertEquals(1_300L, fifth.lockedUntilEpochSeconds)
        assertEquals(1_300L, limiter.lockedUntil("test1"))
    }

    @Test
    fun `잠금이 풀리면 처음부터 다시 센다`() {
        repeat(5) { limiter.recordFailure("test1") }
        clock = 1_299
        assertEquals(1_300L, limiter.lockedUntil("test1"))

        clock = 1_300
        assertNull(limiter.lockedUntil("test1"))
        assertEquals(4, limiter.recordFailure("test1").remainingAttempts)
    }

    @Test
    fun `맞게 로그인하면 틀린 횟수가 지워진다`() {
        repeat(3) { limiter.recordFailure("test1") }
        limiter.recordSuccess("test1")

        assertEquals(4, limiter.recordFailure("test1").remainingAttempts)
    }

    @Test
    fun `아이디마다 따로 세고 대소문자와 공백은 같은 아이디로 본다`() {
        limiter.recordFailure("test1")
        limiter.recordFailure(" TEST1 ")

        assertEquals(2, limiter.recordFailure("Test1").remainingAttempts)
        assertEquals(4, limiter.recordFailure("test2").remainingAttempts)
    }
}
