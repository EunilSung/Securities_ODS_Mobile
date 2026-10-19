package com.eunilsung.ods.data.local.seed

import com.eunilsung.ods.domain.model.Customer

/**
 * 서버 없이 로그인을 시연하기 위한 계정(`test1` ~ `test3`, 비밀번호 공통 `1234`). Talk 의 시연 계정과 같은 형식이다.
 *
 * 계좌 내용(예수금·보유 종목·미체결)은 계정과 상관없이 기기 하나에 하나다.
 */
internal object DemoAccounts {
    const val PASSWORD = "1234"

    val ALL: List<Customer> = listOf(
        Customer(id = "test1", name = "김민준", accountNo = "1234-5678-01"),
        Customer(id = "test2", name = "이서연", accountNo = "2345-6789-01"),
        Customer(id = "test3", name = "박도윤", accountNo = "3456-7890-01"),
    )

    fun find(id: String): Customer? = ALL.firstOrNull { it.id.equals(id, ignoreCase = true) }
}
