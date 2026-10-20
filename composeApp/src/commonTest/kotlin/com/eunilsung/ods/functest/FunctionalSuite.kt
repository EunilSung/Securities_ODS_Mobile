package com.eunilsung.ods.functest

import com.eunilsung.ods.domain.model.LoginResult
import kotlinx.coroutines.runBlocking
import kotlin.test.assertTrue

/**
 * 시나리오 묶음 하나를 진짜 서버에 돌린다.
 *
 * 서버 주소가 없으면 아무것도 하지 않고 지나간다. 있으면 시연용 직원(test1)으로 로그인하고, 시나리오를 전부
 * 돌린 뒤, 로그인을 정리하고, 하나라도 깨졌으면 테스트를 실패시킨다.
 *
 * 가상 시간(`runTest`)으로 돌리지 않는다. 서버의 응답과 알림은 실제 시간에 오고, 가상 시간에서는
 * 요청의 시간 제한이 응답보다 먼저 터진다.
 */
fun functionalSuite(suiteName: String, scenarios: (RealServerHarness) -> List<Scenario>) = runBlocking {
    val harness = RealServerHarness.startOrNull() ?: return@runBlocking
    try {
        val signedIn = harness.signIn()
        assertTrue(signedIn is LoginResult.Success, "$suiteName — 시연용 계정으로 로그인하지 못했다: $signedIn")
        val passed = FunctionalTestRunner.run(suiteName, harness.login.currentStaff.value?.id, scenarios(harness))
        assertTrue(passed, "$suiteName — 깨진 시나리오가 있다. ${FT.TAG} 로그의 STEP-FAIL 을 볼 것")
    } finally {
        harness.close()
    }
}

/**
 * 기기 DB 의 고객 장부가 [condition] 을 만족하고, 진행 중인 장부 다시 받기가 없을 때까지 기다린다.
 *
 * 내가 쓴 직후에도 그보다 먼저 시작된 다시 받기(다른 쓰기의 바뀜 알림)가 늦게 끝나며 방금 넣은 행을 잠깐 지울 수 있다.
 * 내 쓰기의 알림으로 도는 다음 다시 받기가 되돌려 놓는다. 유스케이스는 기기 DB 를 읽으므로 그 전에 부르지 않는다.
 */
suspend fun ScenarioScope.awaitBook(harness: RealServerHarness, what: String, condition: suspend () -> Boolean) =
    awaitTrue(what) { !harness.customers.isRefreshing.value && condition() }
