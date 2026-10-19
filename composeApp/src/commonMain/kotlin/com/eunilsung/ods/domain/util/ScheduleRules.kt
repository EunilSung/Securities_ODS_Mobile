package com.eunilsung.ods.domain.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

/** 하루의 시작(포함)과 다음 날 시작(제외) — 한국 시간. */
fun LocalDate.dayRangeMillis(): LongRange {
    val start = atStartOfDayIn(SeoulZone).toEpochMilliseconds()
    val end = plus(DatePeriod(days = 1)).atStartOfDayIn(SeoulZone).toEpochMilliseconds()
    return start until end
}

/** 이 날이 들어 있는 주의 월요일. 일정 화면은 월요일부터 일요일까지 한 주를 보인다. */
fun LocalDate.startOfWeek(): LocalDate = minus(DatePeriod(days = dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal))

/** 월요일부터 일곱 날. */
fun LocalDate.weekDays(): List<LocalDate> = (0 until 7).map { startOfWeek().plus(DatePeriod(days = it)) }

/** 이 날 [hour]시 [minute]분(한국 시간). */
fun LocalDate.atTimeMillis(hour: Int, minute: Int): Long =
    LocalDateTime(year, monthNumber, dayOfMonth, hour, minute).toInstant(SeoulZone).toEpochMilliseconds()

/** 방문 일정 규칙. */
object VisitPolicy {
    /** 고를 수 있는 시각 — 08:00 부터 20:30 까지 30분 간격. */
    val TIME_SLOTS: List<Pair<Int, Int>> = (8..20).flatMap { hour -> listOf(hour to 0, hour to 30) }
}
