package com.eunilsung.ods.server.seed

/**
 * 시연용 방문 일정. 오늘(한국 날짜)을 기준으로 며칠 뒤 몇 시인지로 적어 둔다. 지난 일정은 완료로, 다가오는 일정은 예정으로 넣는다.
 *
 * V001~V007 은 앱 로컬 모드와 같은 일정이고 `test1` 의 것이다. 직원마다 자기 일정만 보이는 것을 확인할 수 있도록
 * `test2`·`test3` 에게도 몇 건씩 둔다.
 */
internal data class VisitSeedRow(
    val id: String,
    val staffId: String,
    val customerId: String,
    val dayOffset: Int,
    val hour: Int,
    val minute: Int,
    val place: String,
    val purpose: String,
    val memo: String,
)

internal object VisitSeed {
    val visits: List<VisitSeedRow> = listOf(
        VisitSeedRow("V001", "test1", "C005", 0, 10, 0, "고객 자택(마포구)", "ACCOUNT_OPENING", "종합매매 계좌 개설 희망. 신분증 지참 요청함."),
        VisitSeedRow("V002", "test1", "C002", 0, 14, 30, "고객 사무실(서초구)", "RISK_PROFILE", "투자성향 유효기간 지남 — 재진단 후 ISA 만기 상품 안내."),
        VisitSeedRow("V003", "test1", "C009", 0, 16, 0, "종로 카페", "PRODUCT", "채권형 펀드 설명 자료 준비."),
        VisitSeedRow("V004", "test1", "C010", 1, 11, 0, "관악구 고객 자택", "CONSULT", "사회초년생 자산관리 상담."),
        VisitSeedRow("V005", "test1", "C003", 2, 15, 0, "판교 고객 회사 로비", "PRODUCT", "해외주식형 상품 문의."),
        VisitSeedRow("V006", "test1", "C001", -2, 10, 30, "고객 사무실(강남구)", "CONSULT", "반도체 업황 상담."),
        VisitSeedRow("V007", "test1", "C004", -1, 13, 0, "송파 고객 자택", "CONSULT", "CMA 금리 안내."),
        VisitSeedRow("V008", "test2", "C006", 0, 11, 0, "여의도 고객 회사 로비", "PRODUCT", "단기채권 펀드 안내."),
        VisitSeedRow("V009", "test2", "C011", 1, 15, 30, "해운대 고객 자택", "CONSULT", "주소 변경 서류 전달."),
        VisitSeedRow("V010", "test3", "C007", 0, 13, 30, "수지구 고객 사무실", "PRODUCT", "ISA 국내주식 포트폴리오 설명."),
        VisitSeedRow("V011", "test3", "C012", 2, 10, 0, "유성구 고객 자택", "RISK_PROFILE", "투자성향 재진단."),
    )
}
