package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.util.lowerLimitPrice
import com.eunilsung.ods.domain.util.tickDown
import com.eunilsung.ods.domain.util.tickUp
import com.eunilsung.ods.domain.util.upperLimitPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * 서버 없이 실시간 시세를 흉내 낸다 — 누가 종목을 보고 있는 동안 1~3초마다 한 호가씩 움직여 기기 DB 에 쓴다.
 *
 * 서버 모드에서는 쓰지 않는다. 실제 체결 알림이 같은 자리(기기 DB 시세)를 갱신한다.
 * 같은 종목을 여러 화면이 보고 있어도 움직임은 하나만 돈다(보는 화면 수를 센다).
 */
class LocalTickSimulator(private val database: AppDatabase) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private val watcherCounts = mutableMapOf<String, Int>()
    private val jobs = mutableMapOf<String, Job>()

    /** [code] 를 보는 동안 시세를 움직인다. 부른 쪽이 취소될 때까지 돌아오지 않는다. */
    suspend fun watch(code: String) {
        lock.withLock {
            watcherCounts[code] = (watcherCounts[code] ?: 0) + 1
            if (jobs[code] == null) jobs[code] = scope.launch { tickLoop(code) }
        }
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                lock.withLock {
                    val remaining = (watcherCounts[code] ?: 1) - 1
                    if (remaining > 0) {
                        watcherCounts[code] = remaining
                    } else {
                        watcherCounts.remove(code)
                        jobs.remove(code)?.cancel()
                    }
                }
            }
        }
    }

    private suspend fun tickLoop(code: String) {
        val random = Random(code.hashCode())
        val queries = database.appDatabaseQueries
        while (true) {
            delay(random.nextLong(1_200, 2_800))
            val row = queries.selectStock(code).executeAsOneOrNull() ?: continue
            val roll = random.nextInt(100)
            val next = when {
                roll < 45 -> tickUp(row.price)
                roll < 90 -> tickDown(row.price)
                else -> row.price
            }.coerceIn(lowerLimitPrice(row.previousClose), upperLimitPrice(row.previousClose))
            queries.updateQuote(price = next, volume = row.volume + random.nextLong(100, 5_000), code = code)
        }
    }
}
