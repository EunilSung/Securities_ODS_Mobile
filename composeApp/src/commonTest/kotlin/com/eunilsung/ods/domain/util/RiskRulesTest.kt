package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.ProductRiskGrade
import com.eunilsung.ods.domain.model.RiskProfile
import com.eunilsung.ods.domain.model.RiskQuestion
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RiskRulesTest {
    private fun answersAt(pick: (RiskQuestion) -> Int) = RiskQuestion.entries.associateWith(pick)

    private fun lowest(q: RiskQuestion) = q.scores.indexOf(q.scores.min())
    private fun highest(q: RiskQuestion) = q.scores.indexOf(q.scores.max())

    @Test
    fun lowestAnswersAreStableAndHighestAreAggressive() {
        val low = RiskAssessmentPolicy.assess(answersAt(::lowest), age = 70)
        assertEquals(RiskProfile.STABLE, low.profile)
        assertEquals(0, low.score)
        assertTrue(low.isElderly)

        val high = RiskAssessmentPolicy.assess(answersAt(::highest), age = 30)
        assertEquals(RiskProfile.AGGRESSIVE, high.profile)
        assertEquals(100, high.score)
        assertFalse(high.isElderly)
    }

    @Test
    fun middleAnswersLandInTheMiddle() {
        val result = RiskAssessmentPolicy.assess(answersAt { it.scores.size / 2 }, age = 45)
        assertEquals(RiskProfile.NEUTRAL, result.profile)
    }

    @Test
    fun allQuestionsMustBeAnswered() {
        assertFailsWith<IllegalArgumentException> {
            RiskAssessmentPolicy.assess(answersAt(::lowest) - RiskQuestion.INCOME, age = 40)
        }
    }

    @Test
    fun ageMapsToOption() {
        assertEquals(listOf(0, 1, 1, 2, 3, 4), listOf(19, 20, 40, 41, 60, 61).map(RiskAssessmentPolicy::ageOption))
    }

    @Test
    fun profileAllowsOnlyProductsUpToItsRiskGrade() {
        assertTrue(RiskProfile.STABLE.allows(ProductRiskGrade.MINIMAL))
        assertTrue(RiskProfile.STABLE.allows(ProductRiskGrade.VERY_LOW))
        assertFalse(RiskProfile.STABLE.allows(ProductRiskGrade.LOW))
        assertTrue(RiskProfile.NEUTRAL.allows(ProductRiskGrade.MODERATE))
        assertFalse(RiskProfile.NEUTRAL.allows(ProductRiskGrade.HIGH))
        assertTrue(RiskProfile.AGGRESSIVE.allows(ProductRiskGrade.VERY_HIGH))
    }

    @Test
    fun signatureNeedsEnoughPointsAndSpread() {
        val dot = Signature(listOf(List(30) { SignaturePoint(0.5f, 0.5f) }))
        assertFalse(SignaturePolicy.isEnough(dot))
        val short = Signature(listOf(List(5) { SignaturePoint(it * 0.1f, 0.5f) }))
        assertFalse(SignaturePolicy.isEnough(short))
        val line = Signature(listOf(List(20) { SignaturePoint(0.1f + it * 0.02f, 0.5f) }))
        assertTrue(SignaturePolicy.isEnough(line))
    }
}
