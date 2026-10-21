package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.CalendarMode
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleRulesTest {
    @Test
    fun weekStartsOnMonday() {
        val sunday = LocalDate(2026, 10, 25)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(LocalDate(2026, 10, 19), sunday.startOfWeek())
        assertEquals(LocalDate(2026, 10, 19), LocalDate(2026, 10, 19).startOfWeek())
        assertEquals((19..25).toList(), sunday.weekDays().map { it.dayOfMonth })
    }

    @Test
    fun monthMovesKeepDayOfMonthClampedToLastDay() {
        assertEquals(LocalDate(2026, 11, 30), LocalDate(2026, 10, 31).plusMonthsClamped(1))
        assertEquals(LocalDate(2027, 2, 28), LocalDate(2027, 1, 31).plusMonthsClamped(1))
        assertEquals(LocalDate(2028, 2, 29), LocalDate(2028, 3, 31).plusMonthsClamped(-1))
        assertEquals(LocalDate(2027, 1, 15), LocalDate(2026, 12, 15).plusMonthsClamped(1))
        assertEquals(LocalDate(2026, 11, 30), LocalDate(2026, 10, 31).movedBy(CalendarMode.MONTH, 1))
        assertEquals(LocalDate(2026, 10, 24), LocalDate(2026, 10, 31).movedBy(CalendarMode.WEEK, -1))
    }

    @Test
    fun monthGridIsSixWeeksFromMondayOnOrBeforeFirst() {
        val october = LocalDate(2026, 10, 21).monthGridDays()
        assertEquals(42, october.size)
        assertEquals(LocalDate(2026, 9, 28), october.first())
        assertEquals(LocalDate(2026, 11, 8), october.last())
        val june = LocalDate(2026, 6, 30).monthGridDays()
        assertEquals(LocalDate(2026, 6, 1), june.first())
        assertEquals(DayOfWeek.MONDAY, june.first().dayOfWeek)
        assertEquals(42, LocalDate(2026, 6, 1).calendarDays(CalendarMode.MONTH).size)
        assertEquals(7, LocalDate(2026, 6, 1).calendarDays(CalendarMode.WEEK).size)
    }

    @Test
    fun dayRangeIsSeoulMidnightToMidnight() {
        val range = LocalDate(2026, 10, 19).dayRangeMillis()
        assertEquals(24 * 60 * 60 * 1000L, range.last + 1 - range.first)
        assertEquals(LocalDate(2026, 10, 19).atTimeMillis(0, 0), range.first)
        assertEquals(LocalDate(2026, 10, 19).atTimeMillis(9, 30), range.first + (9 * 60 + 30) * 60 * 1000L)
    }

    @Test
    fun timeSlotsAreEveryThirtyMinutesFromEightToHalfPastEight() {
        assertEquals(8 to 0, VisitPolicy.TIME_SLOTS.first())
        assertEquals(20 to 30, VisitPolicy.TIME_SLOTS.last())
        assertEquals(26, VisitPolicy.TIME_SLOTS.size)
    }
}
