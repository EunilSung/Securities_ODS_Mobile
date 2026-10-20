package com.eunilsung.ods.functest

import kotlinx.coroutines.flow.first
import kotlin.test.Test

/** 관심종목 — 서버 시세로 기기 DB 를 채우고, 실시간 체결 알림이 기기 DB 시세를 바로 고치는지. */
class MarketScenarioTest {

    @Test
    fun 시세() = functionalSuite("시세") { harness ->
        listOf(
            scenario("체결알림") {
                step("관심종목을 서버에서 받는다") {
                    require(harness.stocks.fetchWatchlist(), "관심종목을 받지 못했다")
                    requireEquals("005930", harness.stocks.observeWatchlist().first().firstOrNull()?.code, "첫 종목")
                }
                step("체결 알림이 오면 몇 초 안에 기기 DB 의 거래량이 바뀐다") {
                    /** 체결마다 거래량은 늘고 값은 그대로일 수도 있어 거래량으로 본다. 다시 받기는 부르지 않는다. */
                    awaitTrue("로그인 직후의 다시 받기가 끝난다") { !harness.stocks.isFetching.value }
                    val before = harness.stocks.observeWatchlist().first().associate { it.code to it.volume }
                    awaitTrue("체결 알림으로 바뀐 종목", timeoutMs = QUOTE_TIMEOUT_MS) {
                        harness.stocks.observeWatchlist().first().any { stock -> before[stock.code].let { it != null && stock.volume > it } }
                    }
                }
            },
        )
    }

    private companion object {
        /** 서버는 종목마다 1.2~2.8초 간격으로 체결시킨다. */
        const val QUOTE_TIMEOUT_MS = 10_000L
    }
}
