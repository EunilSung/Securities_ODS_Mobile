package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalVisitRepositoryImpl
import com.eunilsung.ods.data.mapper.ServerVisitMapper
import com.eunilsung.ods.data.mapper.VisitMapper
import com.eunilsung.ods.data.remote.server.OdsServer
import com.eunilsung.ods.data.remote.server.ServerEvents
import com.eunilsung.ods.data.remote.server.getOrThrow
import com.eunilsung.ods.data.remote.server.valueOrNull
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.NewVisit
import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.domain.repository.VisitRepository
import com.eunilsung.ods.shared.api.ServerEvent
import com.eunilsung.ods.shared.api.VisitStatusRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 서버 모드 방문 일정. [refresh] 는 로그인한 직원의 일정을 서버가 준 그대로 바꾸고, 등록·상태 변경은 서버가 받아 준 뒤에 DB 에 반영한다.
 *
 * 고객 이름은 기기 DB 의 고객과 이어 읽는다. 다시 연결되거나 일정이 바뀌었다는 알림이 오면 다시 받는다.
 */
class VisitRepositoryImpl(
    private val local: LocalVisitRepositoryImpl,
    private val database: AppDatabase,
    private val server: OdsServer,
    serverEvents: ServerEvents,
    private val mapper: VisitMapper,
    private val serverMapper: ServerVisitMapper,
) : VisitRepository by local {

    private val queries get() = database.appDatabaseQueries
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshLock = Mutex()

    init {
        scope.launch { serverEvents.connected.collect { refresh() } }
        scope.launch {
            serverEvents.events.collect {
                if (it.type == ServerEvent.TYPE_CHANGED && it.topic == ServerEvent.TOPIC_VISITS) refresh()
            }
        }
    }

    override suspend fun refresh(): Boolean = refreshLock.withLock {
        val visits = server.visits().valueOrNull() ?: return@withLock false
        withContext(Dispatchers.Default) {
            database.transaction {
                queries.deleteVisits()
                visits.forEach { queries.upsertVisit(mapper.toEntity(serverMapper.toModel(it))) }
            }
        }
        true
    }

    override suspend fun add(visit: NewVisit): Visit {
        val created = serverMapper.toModel(server.addVisit(serverMapper.toRequest(visit)).getOrThrow())
        withContext(Dispatchers.Default) { queries.upsertVisit(mapper.toEntity(created)) }
        return created
    }

    override suspend fun setStatus(id: String, status: VisitStatus) {
        server.setVisitStatus(id, VisitStatusRequest(status.name)).getOrThrow()
        local.setStatus(id, status)
    }
}
