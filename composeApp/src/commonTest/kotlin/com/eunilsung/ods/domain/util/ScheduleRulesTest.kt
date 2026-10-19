package com.eunilsung.ods.domain.util

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
