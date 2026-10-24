package com.eunilsung.ods.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.db.SignatureEntity
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.model.SignatureRecord
import com.eunilsung.ods.domain.repository.SignatureRepository
import com.eunilsung.ods.util.newId
import korlibs.crypto.SHA256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 서명 기록을 기기 DB 에 남긴다. 획의 점을 글자로 늘어놓아 SHA-256 한 값을 지문으로 쓴다.
 */
class LocalSignatureRepositoryImpl(
    private val database: AppDatabase,
) : SignatureRepository {

    private val queries get() = database.appDatabaseQueries

    override suspend fun record(customerId: String, purpose: SignaturePurpose, signature: Signature, nowMillis: Long): SignatureRecord {
        val record = SignatureRecord(
            id = newId(),
            customerId = customerId,
            purpose = purpose,
            signedAtMillis = nowMillis,
            digest = digest(signature),
        )
        withContext(Dispatchers.Default) {
            queries.insertSignature(SignatureEntity(record.id, record.customerId, record.purpose.name, record.signedAtMillis, record.digest))
        }
        return record
    }

    override fun observeRecords(customerId: String): Flow<List<SignatureRecord>> =
        queries.selectSignatures(customerId).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { row ->
                SignatureRecord(
                    id = row.id,
                    customerId = row.customerId,
                    purpose = SignaturePurpose.entries.firstOrNull { it.name == row.purpose } ?: SignaturePurpose.RISK_PROFILE,
                    signedAtMillis = row.signedAt,
                    digest = row.digest,
                )
            }
        }

    private fun digest(signature: Signature): String =
        SHA256.digest(
            signature.strokes.joinToString("|") { stroke -> stroke.joinToString(";") { "${it.x},${it.y}" } }.encodeToByteArray()
        ).hex
}
