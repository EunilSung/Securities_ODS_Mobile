package com.eunilsung.ods.ui.util

import com.eunilsung.ods.domain.model.ChartPeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val Seoul = TimeZone.of("Asia/Seoul")

/** 차트 아래 시각 눈금. 분봉은 "14:05", 일·주봉은 "10.17", 월봉은 "2026.10". 시각은 한국 시간이다. */
fun ChartPeriod.formatAxisTime(epochSeconds: Long): String {
    val time = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(Seoul)
    return when (this) {
        ChartPeriod.MINUTE -> "${time.hour.twoDigits()}:${time.minute.twoDigits()}"
        ChartPeriod.DAY, ChartPeriod.WEEK -> "${time.monthNumber.twoDigits()}.${time.dayOfMonth.twoDigits()}"
        ChartPeriod.MONTH -> "${time.year}.${time.monthNumber.twoDigits()}"
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

/** 주문 시각 "14:05:32"(한국 시간). */
fun formatOrderTime(epochSeconds: Long): String {
    val time = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(Seoul)
    return "${time.hour.twoDigits()}:${time.minute.twoDigits()}:${time.second.twoDigits()}"
}
