package com.eunilsung.ods.data.local.seed

import com.eunilsung.ods.domain.model.Staff

/**
 * 서버 없이 로그인을 시연하기 위한 직원 계정(`test1` ~ `test3`, 비밀번호 공통 `1234`). Talk 의 시연 계정과 같은 형식이다.
 *
 * 고객·일정 같은 업무 데이터는 계정과 상관없이 기기 하나에 하나다.
 */
internal object DemoAccounts {
    const val PASSWORD = "1234"

    val ALL: List<Staff> = listOf(
        Staff(id = "test1", name = "김민준", branch = "강남금융센터", position = "대리"),
        Staff(id = "test2", name = "이서연", branch = "여의도지점", position = "과장"),
        Staff(id = "test3", name = "박도윤", branch = "판교WM센터", position = "차장"),
    )

    fun find(id: String): Staff? = ALL.firstOrNull { it.id.equals(id, ignoreCase = true) }
}
