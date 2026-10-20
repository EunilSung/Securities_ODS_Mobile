package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.data.local.LocalSecret
import com.eunilsung.ods.data.local.seed.VisitSeedRow
import com.eunilsung.ods.db.VisitEntity
import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.model.VisitPurpose
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.domain.util.SeoulZone
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

/**
 * 기기 DB 행·시연용 데이터를 방문 일정 도메인 모델로 바꾼다.
 *
 * 장소·메모는 [secret] 으로 잠가 저장한다. 함께 읽는 고객 이름은 고객 표에 잠긴 채 들어 있어 여기서 푼다.
 */
class VisitMapper(private val secret: LocalSecret) {
    /** 고객 이름을 붙여 읽은 방문 한 행. SQLDelight 조회의 열 순서와 같다. */
    fun toModel(
        id: String,
        customerId: String,
        customerName: String,
        scheduledAt: Long,
        place: String,
        purpose: String,
        memo: String,
        status: String,
    ): Visit = Visit(
        id = id,
        customerId = customerId,
        customerName = secret.decrypt(customerName),
        scheduledAtMillis = scheduledAt,
        place = secret.decrypt(place),
        purpose = VisitPurpose.entries.firstOrNull { it.name == purpose } ?: VisitPurpose.CONSULT,
        memo = secret.decrypt(memo),
        status = VisitStatus.entries.firstOrNull { it.name == status } ?: VisitStatus.SCHEDULED,
    )

    fun toEntity(visit: Visit): VisitEntity = VisitEntity(
        id = visit.id,
        customerId = visit.customerId,
        scheduledAt = visit.scheduledAtMillis,
        place = secret.encrypt(visit.place),
        purpose = visit.purpose.name,
        memo = secret.encrypt(visit.memo),
        status = visit.status.name,
    )

    /** 오늘([today])을 기준으로 시연용 일정의 시각을 정한다. 지난 일정은 완료로 둔다. */
    internal fun toEntity(row: VisitSeedRow, today: LocalDate): VisitEntity {
        val date = today.plus(DatePeriod(days = row.dayOffset))
        return VisitEntity(
            id = row.id,
            customerId = row.customerId,
            scheduledAt = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, row.hour, row.minute).toInstant(SeoulZone).toEpochMilliseconds(),
            place = secret.encrypt(row.place),
            purpose = row.purpose,
            memo = secret.encrypt(row.memo),
            status = if (row.dayOffset < 0) VisitStatus.COMPLETED.name else VisitStatus.SCHEDULED.name,
        )
    }
}
