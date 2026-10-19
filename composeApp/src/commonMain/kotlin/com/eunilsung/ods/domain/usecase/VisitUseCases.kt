package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.NewVisit
import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.domain.repository.ConsultSessionRepository
import com.eunilsung.ods.domain.repository.VisitRepository
import com.eunilsung.ods.domain.util.SeoulZone
import com.eunilsung.ods.domain.util.dayRangeMillis
import com.eunilsung.ods.domain.util.weekDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/** 하루의 방문(시각순). 취소한 것도 함께 준다 — 화면이 흐리게 보인다. */
class ObserveDayVisitsUseCase(private val repository: VisitRepository) {
    operator fun invoke(date: LocalDate): Flow<List<Visit>> {
        val range = date.dayRangeMillis()
        return repository.observeVisits(range.first, range.last + 1)
    }
}

/** 한 주 날짜별 방문 건수(취소 제외). 주간 달력의 점에 쓴다. */
class ObserveWeekVisitCountsUseCase(private val repository: VisitRepository) {
    operator fun invoke(anyDayInWeek: LocalDate): Flow<Map<LocalDate, Int>> {
        val days = anyDayInWeek.weekDays()
        val from = days.first().dayRangeMillis().first
        val until = days.last().dayRangeMillis().last + 1
        return repository.observeVisits(from, until).map { visits ->
            visits.filter { it.status != VisitStatus.CANCELED }
                .groupingBy { Instant.fromEpochMilliseconds(it.scheduledAtMillis).toLocalDateTime(SeoulZone).date }
                .eachCount()
        }
    }
}

class ObserveVisitUseCase(private val repository: VisitRepository) {
    operator fun invoke(id: String): Flow<Visit?> = repository.observeVisit(id)
}

class ObserveUpcomingVisitsUseCase(private val repository: VisitRepository) {
    operator fun invoke(customerId: String, now: Long = nowMillis()): Flow<List<Visit>> =
        repository.observeUpcomingVisits(customerId, now)
}

class RefreshVisitsUseCase(private val repository: VisitRepository) {
    suspend operator fun invoke(): Boolean = runCatching { repository.refresh() }.getOrDefault(false)
}

/** 일정 등록 결과. */
sealed interface AddVisitResult {
    data class Added(val visit: Visit) : AddVisitResult

    /** 이미 지난 시각이다. */
    data object InPast : AddVisitResult
}

/** 방문 일정을 잡는다. 지난 시각에는 잡지 않는다. */
class AddVisitUseCase(private val repository: VisitRepository) {
    suspend operator fun invoke(visit: NewVisit, now: Long = nowMillis()): AddVisitResult =
        if (visit.scheduledAtMillis < now) AddVisitResult.InPast else AddVisitResult.Added(repository.add(visit))
}

/** 방문을 마쳤다. 그 고객과 상담 중인 것으로 두어 바로 상담을 기록할 수 있게 한다. */
class CompleteVisitUseCase(
    private val repository: VisitRepository,
    private val session: ConsultSessionRepository,
) {
    suspend operator fun invoke(visit: Visit) {
        repository.setStatus(visit.id, VisitStatus.COMPLETED)
        session.start(visit.customerId)
    }
}

class CancelVisitUseCase(private val repository: VisitRepository) {
    suspend operator fun invoke(id: String) = repository.setStatus(id, VisitStatus.CANCELED)
}

/** 일정 화면이 쓰는 유스케이스 묶음. */
class ScheduleUseCases(
    val observeDayVisits: ObserveDayVisitsUseCase,
    val observeWeekVisitCounts: ObserveWeekVisitCountsUseCase,
    val refresh: RefreshVisitsUseCase,
)

/** 방문 상세가 쓰는 유스케이스 묶음. */
class VisitDetailUseCases(
    val observeVisit: ObserveVisitUseCase,
    val completeVisit: CompleteVisitUseCase,
    val cancelVisit: CancelVisitUseCase,
)

/** 일정 등록이 쓰는 유스케이스 묶음. */
class NewVisitUseCases(
    val observeCustomers: ObserveCustomersUseCase,
    val addVisit: AddVisitUseCase,
)
