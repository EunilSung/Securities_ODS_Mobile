package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalVisitRepositoryImpl
import com.eunilsung.ods.data.mapper.ServerVisitMapper
import com.eunilsung.ods.data.mapper.VisitMapper
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.shared.api.ServerEvent
import com.eunilsung.ods.shared.api.VisitDto
import com.eunilsung.ods.testsupport.FakeOdsServer
import com.eunilsung.ods.testsupport.FakeServerEvents
import com.eunilsung.ods.testsupport.awaitUntil
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.testsupport.testLocalSecret
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** 서버 모드 방문 일정 — 일정이 바뀌었다는 알림에 다시 받고, 상태 변경은 서버를 거쳐 DB 에 남는다. */
class VisitRepositoryImplTest {
    private val database = createTestDatabase()
    private val secret = testLocalSecret()
    private val server = FakeOdsServer()
    private val events = FakeServerEvents()
    private val repository = VisitRepositoryImpl(
        LocalVisitRepositoryImpl(database, VisitMapper(secret)), database, server, events, VisitMapper(secret), ServerVisitMapper(),
    )
    private val visit = VisitDto("V1", "C1", "한지민", 1_000, "고객 자택", "CONSULT", "메모", "SCHEDULED")

    @Test
    fun visitsChangedEventRefetchesAndStatusGoesThroughServer() = runTest {
        server.visits = listOf(visit)

        events.push(ServerEvent(ServerEvent.TYPE_CHANGED, topic = ServerEvent.TOPIC_VISITS))
        awaitUntil { repository.observeVisit("V1").first() != null }
        assertEquals("고객 자택", repository.observeVisit("V1").first()?.place)

        repository.setStatus("V1", VisitStatus.COMPLETED)

        assertEquals("COMPLETED", server.visits.single().status)
        assertEquals(VisitStatus.COMPLETED, repository.observeVisit("V1").first()?.status)
    }
}
