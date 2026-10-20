package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalSignatureRepositoryImpl
import com.eunilsung.ods.data.mapper.ServerCustomerMapper
import com.eunilsung.ods.data.remote.server.OdsServer
import com.eunilsung.ods.data.remote.server.getOrThrow
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.model.SignatureRecord
import com.eunilsung.ods.domain.repository.SignatureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 서버 모드 서명 — 서명 그림은 서버로만 보내고, 서버가 받아 준 기록(지문)만 기기 DB 에 남긴다.
 */
class SignatureRepositoryImpl(
    private val local: LocalSignatureRepositoryImpl,
    private val database: AppDatabase,
    private val server: OdsServer,
    private val serverMapper: ServerCustomerMapper,
) : SignatureRepository by local {

    override suspend fun record(customerId: String, purpose: SignaturePurpose, signature: Signature, nowMillis: Long): SignatureRecord {
        val request = serverMapper.toRequest(purpose, signature, nowMillis)
        val record = serverMapper.toModel(server.recordSignature(customerId, request).getOrThrow())
        withContext(Dispatchers.Default) { database.appDatabaseQueries.upsertSignature(serverMapper.toEntity(record)) }
        return record
    }
}
