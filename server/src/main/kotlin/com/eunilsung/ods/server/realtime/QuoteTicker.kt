package com.eunilsung.ods.server.realtime

import com.eunilsung.ods.domain.util.lowerLimitPrice
import com.eunilsung.ods.domain.util.tickDown
import com.eunilsung.ods.domain.util.tickUp
import com.eunilsung.ods.domain.util.upperLimitPrice
import com.eunilsung.ods.server.repository.StockRepository
import com.eunilsung.ods.shared.api.QuoteDto
import com.eunilsung.ods.shared.api.ServerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * 체결 흉내 — 실제 시세 연동 대신, 누가 붙어 있는 동안 관심종목마다 [intervalMillis] 범위의 간격으로
 * 한 호가씩 움직여 DB 에 쓰고 붙어 있는 모두에게 알린다. 앱 로컬 모드의 `LocalTickSimulator` 와 같은 움직임이다.
 *
 * 오를 확률 45%, 내릴 확률 45%, 그대로 10%. 상한가·하한가(전일 종가 ±30%)를 넘지 않는다.
 * 테스트는 [start] 를 부르지 않고 [tick] 으로 한 번씩 직접 움직인다.
 */
class QuoteTicker(
    private val stocks: StockRepository,
    private val hub: EventHub,
    private val random: Random = Random.Default,
    private val intervalMillis: LongRange = DEFAULT_INTERVAL_MILLIS,
) {

    /** 종목마다 따로 도는 움직임을 [scope] 에 띄운다. [scope] 가 끝나면 함께 멈춘다. */
    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            stocks.watchlist().forEach { stock ->
                launch {
                    while (isActive) {
                        delay(random.nextLong(intervalMillis.first, intervalMillis.last + 1))
                        if (hub.connectionCount() > 0) runCatching { tick(stock.code) }
                    }
                }
            }
        }
    }

    /** [code] 를 한 번 체결시키고 알린다. 종목이 없으면 null. */
    suspend fun tick(code: String): QuoteDto? {
        val quote = withContext(Dispatchers.IO) {
            stocks.move(code) { stock ->
                val roll = random.nextInt(100)
                val next = when {
                    roll < UP_PERCENT -> tickUp(stock.price)
                    roll < UP_PERCENT + DOWN_PERCENT -> tickDown(stock.price)
                    else -> stock.price
                }.coerceIn(lowerLimitPrice(stock.previousClose), upperLimitPrice(stock.previousClose))
                QuoteDto(code, next, stock.volume + random.nextLong(MIN_VOLUME, MAX_VOLUME + 1))
            }
        } ?: return null
        hub.broadcast(ServerEvent(ServerEvent.TYPE_QUOTE, quote = quote))
        return quote
    }

    private companion object {
        val DEFAULT_INTERVAL_MILLIS = 1_200L..2_800L
        const val UP_PERCENT = 45
        const val DOWN_PERCENT = 45
        const val MIN_VOLUME = 100L
        const val MAX_VOLUME = 5_000L
    }
}
