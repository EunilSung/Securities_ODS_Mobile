package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.CalendarMode
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
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

/** [months]달 뒤의 같은 날. 그 달에 없는 날이면 그 달 마지막 날로 맞춘다(10월 31일 → 11월 30일). */
fun LocalDate.plusMonthsClamped(months: Int): LocalDate = plus(months, DateTimeUnit.MONTH)

/** 이 날이 들어 있는 달의 달력 — 1일이 든 주의 월요일부터 6주(42일). */
fun LocalDate.monthGridDays(): List<LocalDate> {
    val first = LocalDate(year, month, 1).startOfWeek()
    return (0 until 42).map { first.plus(DatePeriod(days = it)) }
}

/** 달력 [mode]에 보일 날들 — 한 주 또는 한 달 달력. */
fun LocalDate.calendarDays(mode: CalendarMode): List<LocalDate> = when (mode) {
    CalendarMode.WEEK -> weekDays()
    CalendarMode.MONTH -> monthGridDays()
}

/** 달력을 [steps]칸(주 또는 달) 넘긴 날. 주는 같은 요일, 달은 같은 날(없으면 그 달 마지막 날)로 간다. */
fun LocalDate.movedBy(mode: CalendarMode, steps: Int): LocalDate = when (mode) {
    CalendarMode.WEEK -> plus(DatePeriod(days = 7 * steps))
    CalendarMode.MONTH -> plusMonthsClamped(steps)
}

/** [from] 이 든 주·달에서 이 날이 든 주·달까지 몇 칸인지. 앞이면 음수. */
fun LocalDate.periodsFrom(from: LocalDate, mode: CalendarMode): Int = when (mode) {
    CalendarMode.WEEK -> from.startOfWeek().daysUntil(startOfWeek()) / 7
    CalendarMode.MONTH -> (year * 12 + monthNumber) - (from.year * 12 + from.monthNumber)
}

/** 이 날 [hour]시 [minute]분(한국 시간). */
fun LocalDate.atTimeMillis(hour: Int, minute: Int): Long =
    LocalDateTime(year, monthNumber, dayOfMonth, hour, minute).toInstant(SeoulZone).toEpochMilliseconds()

/** 방문 일정 규칙. */
object VisitPolicy {
    /** 고를 수 있는 시각 — 08:00 부터 20:30 까지 30분 간격. */
    val TIME_SLOTS: List<Pair<Int, Int>> = (8..20).flatMap { hour -> listOf(hour to 0, hour to 30) }
}
