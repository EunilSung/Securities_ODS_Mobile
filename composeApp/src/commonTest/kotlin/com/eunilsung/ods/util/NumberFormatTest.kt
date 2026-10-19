package com.eunilsung.ods.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatTest {
    @Test
    fun withCommaGroupsThousands() {
        assertEquals("0", 0L.withComma())
        assertEquals("999", 999L.withComma())
        assertEquals("1,000", 1000L.withComma())
        assertEquals("12,345,678", 12_345_678L.withComma())
        assertEquals("-3,500", (-3500L).withComma())
    }

    @Test
    fun signedRateRoundsToTwoDigits() {
        assertEquals("+0.99", 0.9887.toSignedRate())
        assertEquals("-1.46", (-1.4594).toSignedRate())
        assertEquals("0.00", 0.0.toSignedRate())
        assertEquals("0.00", 0.004.toSignedRate())
    }
}
