package com.eunilsung.ods.ui.util

import com.eunilsung.ods.domain.util.SeoulZone
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

private fun Int.twoDigits(): String = toString().padStart(2, '0')

/** "2026.10.19"(한국 시간). */
fun formatDate(epochMillis: Long): String = formatDate(Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(SeoulZone).date)

fun formatDate(date: LocalDate): String = "${date.year}.${date.monthNumber.twoDigits()}.${date.dayOfMonth.twoDigits()}"

/** "2026.10.19 14:05"(한국 시간). */
fun formatDateTime(epochMillis: Long): String {
    val time = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(SeoulZone)
    return "${formatDate(time.date)} ${time.hour.twoDigits()}:${time.minute.twoDigits()}"
}

/** "14:05"(한국 시간). */
fun formatTime(epochMillis: Long): String {
    val time = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(SeoulZone)
    return "${time.hour.twoDigits()}:${time.minute.twoDigits()}"
}
