package com.eunilsung.ods.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class LoginPolicyTest {
    @Test
    fun accountNoKeepsFirstFourAndLastTwoDigits() {
        assertEquals("1234-****-01", maskAccountNo("1234-5678-01"))
        assertEquals("1234****01", maskAccountNo("1234567801"))
    }

    @Test
    fun shortAccountNoIsLeftAsIs() {
        assertEquals("12345", maskAccountNo("12345"))
        assertEquals("", maskAccountNo(""))
    }
}
