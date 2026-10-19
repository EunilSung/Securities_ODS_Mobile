package com.eunilsung.ods.domain.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderPinPolicyTest {
    @Test
    fun repeatedAndSequentialPinsAreWeak() {
        listOf("1111", "0000", "1234", "4321", "6789", "9876").forEach { assertTrue(OrderPinPolicy.isWeak(it), it) }
    }

    @Test
    fun otherFourDigitPinsAreAllowed() {
        listOf("1357", "2580", "9012", "1122", "0918").forEach { assertFalse(OrderPinPolicy.isWeak(it), it) }
    }

    @Test
    fun wrongLengthOrNonDigitsAreWeak() {
        listOf("123", "12345", "12a4", "").forEach { assertTrue(OrderPinPolicy.isWeak(it), it) }
    }
}
