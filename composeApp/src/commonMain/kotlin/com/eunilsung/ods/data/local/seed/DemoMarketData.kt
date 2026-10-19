package com.eunilsung.ods.data.local.seed

import com.eunilsung.ods.domain.model.Candle
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.model.OrderBook
import com.eunilsung.ods.domain.model.OrderBookLevel
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.domain.util.ceilToTick
import com.eunilsung.ods.domain.util.floorToTick
import com.eunilsung.ods.domain.util.tickDown
import com.eunilsung.ods.domain.util.tickUp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * 서버 없이 현재가 화면을 띄우기 위한 시연용 호가·봉. 값은 지어낸 것이며 실제 시세가 아니다.
 *
 * 같은 종목·같은 시세면 같은 값을 만든다(난수 씨앗을 종목과 시세로 정한다). 그래서 화면을 다시 그려도 숫자가 튀지 않고,
 * 시세가 한 호가 움직일 때만 잔량이 바뀐다.
 */
internal object DemoMarketData {
    private val Seoul = TimeZone.of("Asia/Seoul")

    /** 현재가를 매수 1호가로 두고, 위로 [depth] 단계 매도·아래로 [depth] 단계 매수 호가를 만든다. 상·하한가를 넘지 않는다. */
    fun orderBook(stock: Stock, depth: Int = 10): OrderBook {
        val random = Random(stock.code.hashCode() * 31 + stock.price.hashCode() * 17 + stock.volume.hashCode())
        val baseQuantity = (30_000_000L / stock.price).coerceAtLeast(10)
        fun quantity(): Long = (baseQuantity * (0.15 + random.nextDouble() * 2.6)).roundToLong().coerceAtLeast(1)

        val asks = generateSequence(tickUp(stock.price)) { tickUp(it) }
            .takeWhile { it <= stock.upperLimit }
            .take(depth)
            .map { OrderBookLevel(it, quantity()) }
            .toList()
        val bids = generateSequence(stock.price) { tickDown(it) }
            .takeWhile { it >= stock.lowerLimit }
            .take(depth)
            .map { OrderBookLevel(it, quantity()) }
            .toList()
        return OrderBook(code = stock.code, asks = asks, bids = bids)
    }

    /**
     * 현재가에서 거꾸로 걸어가며 [count] 개의 봉을 만든다(오래된 것부터 돌려준다).
     *
     * 마지막 봉의 종가는 현재가다. 일봉은 바로 앞 봉의 종가를 전일 종가로 맞춰 화면의 전일 대비와 어긋나지 않게 한다.
     */
    fun candles(stock: Stock, period: ChartPeriod, now: Instant, count: Int = 120): List<Candle> {
        val random = Random(stock.code.hashCode() * 31 + period.ordinal)
        val volatility = when (period) {
            ChartPeriod.MINUTE -> 0.0012
            ChartPeriod.DAY -> 0.016
            ChartPeriod.WEEK -> 0.035
            ChartPeriod.MONTH -> 0.07
        }
        val dailyVolume = stock.volume.coerceAtLeast(1_000)
        val baseVolume = when (period) {
            ChartPeriod.MINUTE -> dailyVolume / 300
            ChartPeriod.DAY -> dailyVolume
            ChartPeriod.WEEK -> dailyVolume * 5
            ChartPeriod.MONTH -> dailyVolume * 21
        }.coerceAtLeast(1)
        val times = candleTimes(period, count, now)

        val newestFirst = ArrayList<Candle>(count)
        var close = stock.price.toDouble()
        for (index in count - 1 downTo 0) {
            val isLatest = index == count - 1
            val anchorsPreviousClose = isLatest && period == ChartPeriod.DAY
            val open = if (anchorsPreviousClose) {
                stock.previousClose * (1 + noise(random) * volatility * 0.3)
            } else {
                close * (1 + noise(random) * volatility)
            }
            val high = max(open, close) * (1 + abs(noise(random)) * volatility * 0.6)
            val low = min(open, close) * (1 - abs(noise(random)) * volatility * 0.6)
            val volume = if (anchorsPreviousClose) stock.volume else (baseVolume * (0.4 + random.nextDouble() * 1.4)).roundToLong()
            newestFirst += Candle(
                epochSeconds = times[index],
                open = floorToTick(open.toPositivePrice()),
                high = ceilToTick(high.toPositivePrice()),
                low = floorToTick(low.toPositivePrice()),
                close = if (isLatest) stock.price else floorToTick(close.toPositivePrice()),
                volume = volume,
            )
            close = if (anchorsPreviousClose) stock.previousClose.toDouble() else open * (1 + noise(random) * volatility * 0.2)
        }
        return newestFirst.reversed()
    }

    /** 봉이 시작한 시각들(오래된 것부터). 일봉은 주말을 건너뛴다. 공휴일은 따지지 않는다. */
    private fun candleTimes(period: ChartPeriod, count: Int, now: Instant): List<Long> {
        val today = now.toLocalDateTime(Seoul).date
        val newestFirst: List<Long> = when (period) {
            ChartPeriod.MINUTE -> {
                val minute = now.epochSeconds / 60 * 60
                List(count) { minute - it * 60L }
            }
            ChartPeriod.DAY -> generateSequence(today) { it.minus(1, DateTimeUnit.DAY) }
                .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
                .take(count)
                .map { it.startEpochSeconds() }
                .toList()
            ChartPeriod.WEEK -> {
                val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
                List(count) { monday.minus(it * 7, DateTimeUnit.DAY).startEpochSeconds() }
            }
            ChartPeriod.MONTH -> {
                val first = LocalDate(today.year, today.monthNumber, 1)
                List(count) { first.minus(it, DateTimeUnit.MONTH).startEpochSeconds() }
            }
        }
        return newestFirst.reversed()
    }

    private fun LocalDate.startEpochSeconds(): Long = atStartOfDayIn(Seoul).epochSeconds

    private fun Double.toPositivePrice(): Long = roundToLong().coerceAtLeast(1)

    /** 대략 −1..1 사이에 몰린 난수. 고른 난수 셋의 평균이라 가운데가 잦다. */
    private fun noise(random: Random): Double =
        (random.nextDouble(-1.0, 1.0) + random.nextDouble(-1.0, 1.0) + random.nextDouble(-1.0, 1.0)) / 3
}
