package com.eunilsung.ods.functest

import com.eunilsung.ods.data.remote.server.ServerResult
import com.eunilsung.ods.domain.model.NewVisit
import com.eunilsung.ods.domain.model.VisitPurpose
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.domain.usecase.AddVisitResult
import com.eunilsung.ods.shared.api.VisitStatusRequest
import kotlinx.coroutines.flow.first
import kotlin.test.Test

/** 방문 일정 — 잡은 일정과 바꾼 상태가 서버에 남고, 다른 직원에게는 내 일정이 보이지 않는지. */
class VisitScenarioTest {

    @Test
    fun 방문일정() = functionalSuite("방문일정") { harness ->
        listOf(
            scenario("일정잡고취소") {
                val customer = harness.registerCustomer()
                /** 실화면 흐름이 보는 오늘·다음 주와 겹치지 않게 1년 넘게 뒤로 잡는다. */
                val at = nowMillis() + FAR_FUTURE_MILLIS
                var visitId = ""
                step("일정을 잡으면 서버가 번호를 주고 기기 DB 에 들어간다") {
                    val result = harness.addVisit(NewVisit(customer.id, at, "${harness.runTag} place", VisitPurpose.CONSULT, "FT"))
                    require(result is AddVisitResult.Added, "잡히지 않았다: $result")
                    val visit = (result as AddVisitResult.Added).visit
                    visitId = visit.id
                    requireEquals(customer.name, visit.customerName, "일정의 고객 이름")
                    requireEquals(VisitStatus.SCHEDULED, harness.visits.observeVisit(visitId).first()?.status, "기기 DB 의 상태")
                }
                step("취소하면 서버에서 다시 받아도 취소다") {
                    harness.cancelVisit(visitId)
                    require(harness.visits.refresh(), "일정을 다시 받지 못했다")
                    requireEquals(VisitStatus.CANCELED, harness.visits.observeVisit(visitId).first()?.status, "다시 받은 상태")
                }
                step("지난 시각에는 유스케이스가 잡지 않는다") {
                    val result = harness.addVisit(NewVisit(customer.id, nowMillis() - HOUR_MILLIS, "past", VisitPurpose.CONSULT, ""))
                    requireEquals(AddVisitResult.InPast, result, "지난 시각")
                }
            },
            scenario("직원마다따로") {
                val peer = harness.peer(RealServerHarness.PEER)
                step("내 일정은 다른 직원의 일정 목록에 없다") {
                    require(harness.visits.refresh(), "일정을 받지 못했다")
                    val mine = harness.server.visits().must("내 일정").map { it.id }.toSet()
                    require(SEED_VISITS.all { it in mine }, "내 시연용 일정이 없다: $mine")
                    val theirs = peer.client.visits().must("상대 일정").map { it.id }.toSet()
                    require(theirs.none { it in mine }, "상대에게 내 일정이 보인다: ${theirs.intersect(mine)}")
                }
                step("다른 직원은 내 일정 상태를 바꾸지 못한다") {
                    val result = peer.client.setVisitStatus(SEED_VISITS.first(), VisitStatusRequest(VisitStatus.CANCELED.name))
                    require(result is ServerResult.Rejected && result.status == 404, "남의 일정이 바뀌었다: $result")
                }
            },
        )
    }

    private companion object {
        const val HOUR_MILLIS = 60 * 60 * 1000L
        const val FAR_FUTURE_MILLIS = 400 * 24 * HOUR_MILLIS
        val SEED_VISITS = (1..7).map { "V00$it" }
    }
}
