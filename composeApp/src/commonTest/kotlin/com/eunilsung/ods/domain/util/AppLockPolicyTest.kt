package com.eunilsung.ods.domain.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLockPolicyTest {
    @Test
    fun repeatedAndSequentialPinsAreWeak() {
        listOf("111111", "000000", "123456", "654321", "456789", "987654").forEach { assertTrue(AppLockPolicy.isWeak(it), it) }
    }

    @Test
    fun otherSixDigitPinsAreAllowed() {
        listOf("135790", "258025", "901234", "112233", "091827").forEach { assertFalse(AppLockPolicy.isWeak(it), it) }
    }

    @Test
    fun wrongLengthOrNonDigitsAreWeak() {
        listOf("1357", "1234567", "12a456", "").forEach { assertTrue(AppLockPolicy.isWeak(it), it) }
    }
}
