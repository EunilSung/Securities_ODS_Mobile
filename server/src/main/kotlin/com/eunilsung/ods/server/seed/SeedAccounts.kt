package com.eunilsung.ods.server.seed

import com.eunilsung.ods.shared.api.StaffDto

/**
 * 시연용 직원 계정 `test1` ~ `test3`.
 *
 * 가입이 없는 시연용 서버라 계정을 서버가 미리 만들어 둔다. 앱 로컬 모드(`DemoAccounts`)와 같은 사람들이라
 * 서버 모드로 바꿔도 같은 아이디로 로그인된다.
 */
object SeedAccounts {

    /** 전 계정 공통 비밀번호. */
    const val PASSWORD = "1234"

    val ALL: List<StaffDto> = listOf(
        StaffDto(id = "test1", name = "김민준", branch = "강남금융센터", position = "대리"),
        StaffDto(id = "test2", name = "이서연", branch = "여의도지점", position = "과장"),
        StaffDto(id = "test3", name = "박도윤", branch = "판교WM센터", position = "차장"),
    )
}
