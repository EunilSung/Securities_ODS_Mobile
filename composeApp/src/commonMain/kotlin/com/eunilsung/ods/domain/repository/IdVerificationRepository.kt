package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.IdCardInfo
import com.eunilsung.ods.domain.model.IdVerification

/**
 * 신분증 진위 확인 — 서버 모드에서는 서버가 발급기관(행정안전부·도로교통공단)에 묻는다.
 *
 * 신분증 사진은 확인에만 쓰고 기기에 남기지 않는다.
 */
interface IdVerificationRepository {
    suspend fun verify(info: IdCardInfo, photo: ByteArray?, nowMillis: Long): IdVerification
}
