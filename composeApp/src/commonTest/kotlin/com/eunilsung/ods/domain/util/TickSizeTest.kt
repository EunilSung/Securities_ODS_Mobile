package com.eunilsung.ods.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class TickSizeTest {
    @Test
    fun tickSizeFollowsPriceBands() {
        assertEquals(1, tickSize(1_999))
        assertEquals(5, tickSize(2_000))
        assertEquals(10, tickSize(5_000))
        assertEquals(50, tickSize(20_000))
        assertEquals(100, tickSize(50_000))
        assertEquals(500, tickSize(200_000))
        assertEquals(1_000, tickSize(500_000))
    }

    @Test
    fun tickStepsCrossBandBoundaries() {
        assertEquals(50_000, tickUp(49_950))
        assertEquals(49_950, tickDown(50_000))
        assertEquals(200_000, tickUp(199_900))
        assertEquals(199_900, tickDown(200_000))
        assertEquals(71_600, tickUp(71_500))
        assertEquals(71_400, tickDown(71_500))
    }

    @Test
    fun roundingLandsOnTheGrid() {
        assertEquals(71_500, floorToTick(71_550))
        assertEquals(71_600, ceilToTick(71_550))
        assertEquals(50_000, ceilToTick(49_980))
        assertEquals(71_500, ceilToTick(71_500))
    }

    @Test
    fun priceLimitsAreThirtyPercentOnTheGrid() {
        assertEquals(92_000, upperLimitPrice(70_800))
        assertEquals(49_600, lowerLimitPrice(70_800))
        assertEquals(13_000, upperLimitPrice(10_000))
        assertEquals(7_000, lowerLimitPrice(10_000))
    }
}
