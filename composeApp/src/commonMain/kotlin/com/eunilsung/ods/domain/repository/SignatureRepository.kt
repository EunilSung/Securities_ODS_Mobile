package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.model.SignatureRecord
import kotlinx.coroutines.flow.Flow

/** 고객 전자서명. 서명 그림은 기기에 남기지 않고 지문(해시)과 시각만 남긴다. */
interface SignatureRepository {
    suspend fun record(customerId: String, purpose: SignaturePurpose, signature: Signature, nowMillis: Long): SignatureRecord

    fun observeRecords(customerId: String): Flow<List<SignatureRecord>>
}
