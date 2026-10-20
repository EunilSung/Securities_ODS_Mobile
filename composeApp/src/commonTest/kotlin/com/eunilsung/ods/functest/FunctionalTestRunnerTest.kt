package com.eunilsung.ods.functest

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 러너 자체 — 서버 없이 돈다. 깨진 시나리오를 통과로 세지 않는지, 시연용 계정이 아니면 아무것도 돌리지 않는지 본다. */
class FunctionalTestRunnerTest {

    @Test
    fun passesOnlyWhenEveryScenarioPasses() = runTest {
        val passed = FunctionalTestRunner.run("통과", "test1", listOf(scenario("a") { step("한 단계") { require(true, "") } }))
        assertTrue(passed)
    }

    @Test
    fun failedStepFailsSuiteAndSkipsRestOfScenario() = runTest {
        var ranAfterFailure = false
        var nextScenarioRan = false
        val passed = FunctionalTestRunner.run(
            "실패",
            "test1",
            listOf(
                scenario("깨짐") {
                    step("깨지는 단계") { require(false, "일부러") }
                    ranAfterFailure = true
                },
                scenario("다음") { nextScenarioRan = true },
            ),
        )
        assertFalse(passed)
        assertFalse(ranAfterFailure)
        assertTrue(nextScenarioRan)
    }

    @Test
    fun errorOutsideStepCountsAsFailure() = runTest {
        val passed = FunctionalTestRunner.run("준비 실패", "test1", listOf(scenario("준비") { error("준비하다 깨짐") }))
        assertFalse(passed)
    }

    @Test
    fun refusesAccountsOutsideDemoStaff() = runTest {
        var ran = 0
        val passed = FunctionalTestRunner.run("거부", "someone", listOf(scenario("a") { ran++ }))
        assertFalse(passed)
        assertEquals(0, ran)
    }
}
