package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.NewVisit
import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.model.VisitStatus
import kotlinx.coroutines.flow.Flow

/** 방문 일정. 화면은 기기 DB 를 보고, [refresh] 가 서버(또는 시연용 데이터)에서 받아 채운다. */
interface VisitRepository {
    /** [fromMillis] 이상 [untilMillis] 미만에 잡힌 방문(시각순). 취소한 것도 함께 준다. */
    fun observeVisits(fromMillis: Long, untilMillis: Long): Flow<List<Visit>>

    fun observeVisit(id: String): Flow<Visit?>

    /** 한 고객의 다가오는 방문(예정 상태만, 시각순). */
    fun observeUpcomingVisits(customerId: String, fromMillis: Long): Flow<List<Visit>>

    /** 일정을 다시 받는다. 실패하면 가지고 있던 것을 그대로 둔다. */
    suspend fun refresh(): Boolean

    suspend fun add(visit: NewVisit): Visit

    suspend fun setStatus(id: String, status: VisitStatus)
}
