package com.eunilsung.ods.util

import kotlin.math.abs
import kotlin.math.roundToLong

/** 세 자리마다 쉼표를 넣는다. 예) 1234567 → "1,234,567" */
fun Long.withComma(): String {
    val digits = abs(this).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (this < 0) "-$grouped" else grouped
}

/** 소수 둘째 자리까지 반올림해 부호와 함께 쓴다. 예) 1.234 → "+1.23", -0.5 → "-0.50" */
fun Double.toSignedRate(): String {
    val scaled = (abs(this) * 100).roundToLong()
    val text = "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}"
    return when {
        scaled == 0L -> "0.00"
        this > 0 -> "+$text"
        else -> "-$text"
    }
}
