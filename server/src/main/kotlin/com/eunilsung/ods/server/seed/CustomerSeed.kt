package com.eunilsung.ods.server.seed

/**
 * 앱 로컬 모드와 같은 시연용 고객. 이름·연락처·주소는 지어낸 값이다.
 *
 * 시각은 넣는 순간을 기준으로 며칠 전인지로 적어 둔다 — 언제 켜도 투자성향 유효기간·상담 이력이 그럴듯하게 보이게.
 */
internal data class CustomerSeedRow(
    val id: String,
    val name: String,
    val birthDate: String,
    val gender: String,
    val phone: String,
    val address: String,
    val grade: String,
    val riskProfile: String?,
    val riskAssessedDaysAgo: Int?,
    val registeredDaysAgo: Int,
)

/** (계좌번호, 고객 id, 종류, 며칠 전 개설) */
internal data class AccountSeedRow(val accountNo: String, val customerId: String, val type: String, val openedDaysAgo: Int)

/** (고객 id, 며칠 전, 채널, 주제, 내용, 직원) */
internal data class ConsultationSeedRow(
    val customerId: String,
    val daysAgo: Int,
    val channel: String,
    val topic: String,
    val content: String,
    val staffName: String,
)

internal object CustomerSeed {
    val customers: List<CustomerSeedRow> = listOf(
        CustomerSeedRow("C001", "강하늘", "1968-03-14", "MALE", "010-2231-4587", "서울 강남구 테헤란로 152", "VIP", "NEUTRAL", 120, 2400),
        CustomerSeedRow("C002", "김서윤", "1975-11-02", "FEMALE", "010-3342-1190", "서울 서초구 반포대로 58", "VIP", "ACTIVE", 400, 1800),
        CustomerSeedRow("C003", "박준호", "1982-07-21", "MALE", "010-5512-7731", "경기 성남시 분당구 판교역로 235", "GOLD", "AGGRESSIVE", 30, 900),
        CustomerSeedRow("C004", "이미영", "1959-01-30", "FEMALE", "010-8890-2214", "서울 송파구 올림픽로 300", "GOLD", "STABLE", 200, 3100),
        CustomerSeedRow("C005", "정우성", "1990-05-09", "MALE", "010-7781-3302", "서울 마포구 월드컵북로 396", "GENERAL", null, null, 60),
        CustomerSeedRow("C006", "최지민", "1995-12-17", "FEMALE", "010-4456-9008", "서울 영등포구 여의대로 108", "GENERAL", "STABLE_SEEKING", 500, 700),
        CustomerSeedRow("C007", "한동훈", "1971-09-05", "MALE", "010-6620-1457", "경기 용인시 수지구 포은대로 499", "GOLD", "NEUTRAL", 15, 1500),
        CustomerSeedRow("C008", "윤소희", "1987-04-26", "FEMALE", "010-9034-6671", "인천 연수구 센트럴로 160", "GENERAL", "ACTIVE", 90, 400),
        CustomerSeedRow("C009", "오세훈", "1948-08-11", "MALE", "010-2105-8846", "서울 종로구 사직로 161", "VIP", "STABLE", 330, 5200),
        CustomerSeedRow("C010", "서지수", "2001-02-03", "FEMALE", "010-3127-5590", "서울 관악구 관악로 1", "GENERAL", null, null, 10),
        CustomerSeedRow("C011", "임재현", "1979-06-28", "MALE", "010-7743-0012", "부산 해운대구 센텀중앙로 79", "GOLD", "STABLE_SEEKING", 700, 2000),
        CustomerSeedRow("C012", "장미란", "1965-10-19", "FEMALE", "010-5068-3349", "대전 유성구 대학로 99", "GENERAL", "NEUTRAL", 240, 1200),
    )

    val accounts: List<AccountSeedRow> = listOf(
        AccountSeedRow("7001-2231-01", "C001", "BROKERAGE", 2400),
        AccountSeedRow("7001-2231-02", "C001", "PENSION", 1500),
        AccountSeedRow("7001-3342-01", "C002", "BROKERAGE", 1800),
        AccountSeedRow("7001-3342-02", "C002", "ISA", 600),
        AccountSeedRow("7001-5512-01", "C003", "BROKERAGE", 900),
        AccountSeedRow("7001-8890-01", "C004", "CMA", 3100),
        AccountSeedRow("7001-4456-01", "C006", "BROKERAGE", 700),
        AccountSeedRow("7001-6620-01", "C007", "BROKERAGE", 1500),
        AccountSeedRow("7001-6620-02", "C007", "ISA", 300),
        AccountSeedRow("7001-9034-01", "C008", "BROKERAGE", 400),
        AccountSeedRow("7001-2105-01", "C009", "CMA", 5200),
        AccountSeedRow("7001-2105-02", "C009", "PENSION", 2600),
        AccountSeedRow("7001-7743-01", "C011", "BROKERAGE", 2000),
        AccountSeedRow("7001-5068-01", "C012", "CMA", 1200),
    )

    val consultations: List<ConsultationSeedRow> = listOf(
        ConsultationSeedRow("C001", 3, "VISIT", "MARKET", "반도체 업황 문의. 보유 비중 유지 권유, 분기 실적 발표 후 재점검하기로 함.", "김민준"),
        ConsultationSeedRow("C001", 45, "PHONE", "PRODUCT", "연금저축 추가 납입 한도 안내.", "김민준"),
        ConsultationSeedRow("C001", 120, "VISIT", "RISK_PROFILE", "투자성향 재진단 — 위험중립형.", "김민준"),
        ConsultationSeedRow("C002", 10, "BRANCH", "PRODUCT", "ISA 만기 도래 안내. 만기 후 연금 이전 희망.", "이서연"),
        ConsultationSeedRow("C002", 400, "VISIT", "RISK_PROFILE", "투자성향 진단 — 적극투자형. 유효기간 지남, 다음 방문 때 재진단 필요.", "이서연"),
        ConsultationSeedRow("C003", 30, "VISIT", "RISK_PROFILE", "투자성향 진단 — 공격투자형. 해외주식 비중 확대 관심.", "김민준"),
        ConsultationSeedRow("C004", 7, "PHONE", "GENERAL", "CMA 금리 변경 문의. 변경 내역 문자 발송.", "김민준"),
        ConsultationSeedRow("C007", 15, "VISIT", "ACCOUNT_OPENING", "ISA 계좌 개설 상담. 서류 준비 안내.", "박도윤"),
        ConsultationSeedRow("C009", 20, "VISIT", "MARKET", "금리 인하 전망과 채권형 상품 문의.", "김민준"),
        ConsultationSeedRow("C011", 60, "PHONE", "GENERAL", "주소 변경 요청 — 지점 방문 필요 안내.", "이서연"),
    )
}
