package com.eunilsung.ods.domain.util

import com.eunilsung.ods.domain.model.ProductRiskGrade
import com.eunilsung.ods.domain.model.RiskAssessmentResult
import com.eunilsung.ods.domain.model.RiskProfile
import com.eunilsung.ods.domain.model.RiskQuestion
import com.eunilsung.ods.domain.model.Signature
import kotlin.math.roundToInt

/** 투자성향 진단 규칙. */
object RiskAssessmentPolicy {
    /** 이 나이부터 고령투자자로 본다. */
    const val ELDERLY_AGE = 65

    /** 0~100 점수를 다섯 단계로 나누는 경계. 경계 점수는 위 단계에 들어간다. */
    private val BOUNDS = listOf(20 to RiskProfile.STABLE_SEEKING, 40 to RiskProfile.NEUTRAL, 60 to RiskProfile.ACTIVE, 80 to RiskProfile.AGGRESSIVE)

    /**
     * 답(문항별 고른 선택지 번호)으로 성향을 정한다. 모든 문항에 답해야 한다.
     *
     * 점수 합을 최저·최고 합 사이의 비율로 바꿔 0~100 으로 만든다.
     */
    fun assess(answers: Map<RiskQuestion, Int>, age: Int): RiskAssessmentResult {
        require(RiskQuestion.entries.all { it in answers }) { "모든 문항에 답해야 한다" }
        val sum = RiskQuestion.entries.sumOf { it.scores[answers.getValue(it)] }
        val min = RiskQuestion.entries.sumOf { it.scores.min() }
        val max = RiskQuestion.entries.sumOf { it.scores.max() }
        val score = ((sum - min) * 100.0 / (max - min)).roundToInt()
        val profile = BOUNDS.lastOrNull { score >= it.first }?.second ?: RiskProfile.STABLE
        return RiskAssessmentResult(profile = profile, score = score, isElderly = age >= ELDERLY_AGE)
    }

    /** 나이 문항에서 고를 선택지 — 19세 이하, 20~40, 41~50, 51~60, 61세 이상. */
    fun ageOption(age: Int): Int = when {
        age <= 19 -> 0
        age <= 40 -> 1
        age <= 50 -> 2
        age <= 60 -> 3
        else -> 4
    }
}

/** 이 성향에 이 위험등급 상품을 권유할 수 있는지 — 성향이 허락하는 가장 위험한 등급보다 위험하지 않아야 한다. */
fun RiskProfile.allows(grade: ProductRiskGrade): Boolean = grade.level >= maxProductRisk.level

/** 서명 규칙 — 점 몇 개로 끝낸 낙서를 서명으로 받지 않는다. */
object SignaturePolicy {
    const val MIN_POINTS = 10

    /** 서명이 칸의 가로나 세로로 이만큼은 퍼져 있어야 한다. */
    const val MIN_SPREAD = 0.15f

    fun isEnough(signature: Signature): Boolean {
        if (signature.pointCount < MIN_POINTS) return false
        val points = signature.strokes.flatten()
        val width = points.maxOf { it.x } - points.minOf { it.x }
        val height = points.maxOf { it.y } - points.minOf { it.y }
        return width >= MIN_SPREAD || height >= MIN_SPREAD
    }
}
