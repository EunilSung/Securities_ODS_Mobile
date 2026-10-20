package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.domain.model.NewVisit
import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.model.VisitPurpose
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.shared.api.NewVisitRequest
import com.eunilsung.ods.shared.api.VisitDto

/** 서버 방문 일정 응답을 도메인 모델과 바꾼다. */
class ServerVisitMapper {
    fun toModel(dto: VisitDto): Visit = Visit(
        id = dto.id,
        customerId = dto.customerId,
        customerName = dto.customerName,
        scheduledAtMillis = dto.scheduledAtMillis,
        place = dto.place,
        purpose = dto.purpose.toEnumOr(VisitPurpose.CONSULT),
        memo = dto.memo,
        status = dto.status.toEnumOr(VisitStatus.SCHEDULED),
    )

    fun toRequest(visit: NewVisit): NewVisitRequest = NewVisitRequest(
        customerId = visit.customerId,
        scheduledAtMillis = visit.scheduledAtMillis,
        place = visit.place.trim(),
        purpose = visit.purpose.name,
        memo = visit.memo.trim(),
    )
}
