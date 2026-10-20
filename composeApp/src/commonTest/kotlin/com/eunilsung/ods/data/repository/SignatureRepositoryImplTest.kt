package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalSignatureRepositoryImpl
import com.eunilsung.ods.data.mapper.ServerCustomerMapper
import com.eunilsung.ods.data.remote.server.ServerException
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePoint
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.shared.api.SignaturePointDto
import com.eunilsung.ods.testsupport.FakeOdsServer
import com.eunilsung.ods.testsupport.createTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** 서버 모드 서명 — 그림은 서버로만 가고 기기에는 서버가 준 지문만 남는다. */
class SignatureRepositoryImplTest {
    private val database = createTestDatabase()
    private val server = FakeOdsServer()
    private val local = LocalSignatureRepositoryImpl(database)
    private val repository = SignatureRepositoryImpl(local, database, server, ServerCustomerMapper())
    private val signature = Signature(listOf(listOf(SignaturePoint(0.1f, 0.2f), SignaturePoint(0.3f, 0.4f))))

    @Test
    fun strokesGoToServerAndOnlyDigestStays() = runTest {
        val record = repository.record("C1", SignaturePurpose.RISK_PROFILE, signature, nowMillis = 7)

        val (customerId, request) = server.signatureRequests.single()
        assertEquals("C1", customerId)
        assertEquals(listOf(listOf(SignaturePointDto(0.1f, 0.2f), SignaturePointDto(0.3f, 0.4f))), request.strokes)
        assertEquals(listOf(record), repository.observeRecords("C1").first())
        assertTrue(record.digest.startsWith("server-digest-"))
    }

    @Test
    fun unreachableServerLeavesNothing() = runTest {
        server.isReachable = false

        assertFailsWith<ServerException> { repository.record("C1", SignaturePurpose.RISK_PROFILE, signature, nowMillis = 7) }
        assertTrue(repository.observeRecords("C1").first().isEmpty())
    }
}
