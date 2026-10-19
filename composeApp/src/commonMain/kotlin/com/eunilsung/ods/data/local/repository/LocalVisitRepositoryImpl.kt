package com.eunilsung.ods.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.eunilsung.ods.data.local.seed.VisitSeed
import com.eunilsung.ods.data.mapper.VisitMapper
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.NewVisit
import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.domain.repository.VisitRepository
import com.eunilsung.ods.domain.util.todayInSeoul
import com.eunilsung.ods.util.newId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 서버 없이 도는 방문 일정 — 일정이 하나도 없을 때 오늘을 기준으로 시연용 일정을 넣는다.
 *
 * 시연용 일정은 고객 id 로 시연용 고객과 이어진다. 고객을 먼저 넣어야 이름이 붙는다.
 */
class LocalVisitRepositoryImpl(
    private val database: AppDatabase,
    private val mapper: VisitMapper,
) : VisitRepository {

    private val queries get() = database.appDatabaseQueries

    override fun observeVisits(fromMillis: Long, untilMillis: Long): Flow<List<Visit>> =
        queries.selectVisitsBetween(fromMillis, untilMillis, mapper::toModel).asFlow().mapToList(Dispatchers.Default)

    override fun observeVisit(id: String): Flow<Visit?> =
        queries.selectVisit(id, mapper::toModel).asFlow().mapToOneOrNull(Dispatchers.Default)

    override fun observeUpcomingVisits(customerId: String, fromMillis: Long): Flow<List<Visit>> =
        queries.selectUpcomingVisits(customerId, fromMillis, mapper::toModel).asFlow().mapToList(Dispatchers.Default)

    override suspend fun refresh(): Boolean = withContext(Dispatchers.Default) {
        if (queries.countVisits().executeAsOne() > 0L) return@withContext true
        val today = todayInSeoul()
        database.transaction {
            VisitSeed.visits.forEach { queries.insertVisit(mapper.toEntity(it, today)) }
        }
        true
    }

    override suspend fun add(visit: NewVisit): Visit = withContext(Dispatchers.Default) {
        val id = newId()
        database.transaction {
            queries.insertVisit(
                mapper.toEntity(
                    Visit(
                        id = id,
                        customerId = visit.customerId,
                        customerName = "",
                        scheduledAtMillis = visit.scheduledAtMillis,
                        place = visit.place.trim(),
                        purpose = visit.purpose,
                        memo = visit.memo.trim(),
                        status = VisitStatus.SCHEDULED,
                    )
                )
            )
        }
        queries.selectVisit(id, mapper::toModel).executeAsOne()
    }

    override suspend fun setStatus(id: String, status: VisitStatus) {
        withContext(Dispatchers.Default) { queries.updateVisitStatus(status.name, id) }
    }
}
