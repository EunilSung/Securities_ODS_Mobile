package com.eunilsung.ods.data.local.repository

import com.eunilsung.ods.domain.model.IdCardInfo
import com.eunilsung.ods.domain.model.IdVerification
import com.eunilsung.ods.domain.repository.IdVerificationRepository
import com.eunilsung.ods.util.Log
import kotlinx.coroutines.delay

/**
 * 서버 없이 도는 진위 확인 — 발급기관에 묻는 대신 잠깐 기다렸다가 확인된 것으로 돌려준다.
 *
 * 고객 정보와 맞는지는 이 앞에서 화면이 이미 맞춰 보았다. 사진은 보지도 남기지도 않는다.
 */
class LocalIdVerificationRepositoryImpl(
    private val simulatedRoundTripMillis: Long = SIMULATED_ROUND_TRIP_MILLIS,
) : IdVerificationRepository {
    override suspend fun verify(info: IdCardInfo, photo: ByteArray?, nowMillis: Long): IdVerification {
        delay(simulatedRoundTripMillis)
        Log.message("[IdVerify/Local] ${info.type} verified (photo ${photo?.size ?: 0} bytes, discarded)")
        return IdVerification.Verified(info.type, nowMillis)
    }

    private companion object {
        const val SIMULATED_ROUND_TRIP_MILLIS = 600L
    }
}
