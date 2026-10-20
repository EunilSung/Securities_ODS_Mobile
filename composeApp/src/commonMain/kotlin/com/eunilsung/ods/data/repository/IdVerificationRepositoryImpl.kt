package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.mapper.ServerCustomerMapper
import com.eunilsung.ods.data.remote.server.OdsServer
import com.eunilsung.ods.data.remote.server.ServerResult
import com.eunilsung.ods.domain.model.IdCardInfo
import com.eunilsung.ods.domain.model.IdVerification
import com.eunilsung.ods.domain.repository.IdVerificationRepository

/**
 * 서버 모드 진위 확인 — 서버가 발급기관에 묻는다. 사진은 보내지도 남기지도 않고, 주민등록번호는 앞 6자리와 뒷자리 첫 숫자만 보낸다.
 *
 * 서버가 거절하거나 닿지 못하면 다시 시도할 수 있는 [IdVerification.Unreachable] 이다.
 */
class IdVerificationRepositoryImpl(
    private val server: OdsServer,
    private val serverMapper: ServerCustomerMapper,
) : IdVerificationRepository {
    override suspend fun verify(info: IdCardInfo, photo: ByteArray?, nowMillis: Long): IdVerification =
        when (val result = server.verifyId(serverMapper.toRequest(info))) {
            is ServerResult.Success ->
                if (result.value.verified) IdVerification.Verified(info.type, result.value.verifiedAtMillis) else IdVerification.Rejected
            is ServerResult.Rejected, ServerResult.Unreachable -> IdVerification.Unreachable
        }
}
