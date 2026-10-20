package com.eunilsung.ods.functest

import com.eunilsung.ods.domain.model.LoginResult
import kotlin.test.Test

/** 로그인 — 맞는 비밀번호로 직원 정보가 채워지는지, 틀린 비밀번호와 없는 사번이 같은 답으로 거절되는지. */
class LoginScenarioTest {

    @Test
    fun 로그인() = functionalSuite("로그인") { harness ->
        listOf(
            scenario("로그인상태") {
                step("로그인하면 서버가 준 직원 정보가 채워진다") {
                    val staff = harness.login.currentStaff.value
                    requireEquals(RealServerHarness.ME, staff?.id, "직원 사번")
                    require(!staff?.name.isNullOrBlank() && !staff?.branch.isNullOrBlank(), "이름·지점이 비어 있다: $staff")
                }
                step("받은 토큰으로 서버가 나를 알아본다") {
                    harness.server.customers().must("토큰으로 고객 장부 조회")
                }
            },
            scenario("틀린비밀번호") {
                /**
                 * 로그인에 쓰지 않는 계정으로 한 번만 틀린다. 연달아 틀리면 그 사번이 잠기는데,
                 * 바로 뒤에 맞게 로그인하면 횟수가 지워져 다음 실행에 남지 않는다.
                 */
                val other = RealServerHarness.startOrNull() ?: return@scenario
                try {
                    step("틀린 비밀번호는 남은 횟수와 함께 거절된다") {
                        val result = other.signIn(RealServerHarness.SPARE, "wrong-password")
                        require(result is LoginResult.Wrong, "틀림으로 거절되지 않았다: $result")
                        require((result as LoginResult.Wrong).remainingAttempts > 0, "남은 횟수가 없다: $result")
                    }
                    step("맞는 비밀번호로는 들어간다") {
                        val result = other.signIn(RealServerHarness.SPARE)
                        require(result is LoginResult.Success, "로그인하지 못했다: $result")
                    }
                } finally {
                    other.close()
                }
            },
            scenario("없는사번") {
                val other = RealServerHarness.startOrNull() ?: return@scenario
                try {
                    step("없는 사번도 틀린 비밀번호와 같은 답이다") {
                        /** 실행마다 새 사번을 쓴다 — 같은 없는 사번으로 계속 틀리면 지난 실행의 횟수가 쌓여 잠긴다. */
                        val result = other.signIn("${other.runTag.lowercase()}-nobody", RealServerHarness.PASSWORD)
                        require(result is LoginResult.Wrong, "틀림으로 거절되지 않았다: $result")
                        requireEquals(null, other.login.currentStaff.value, "로그인한 직원")
                    }
                } finally {
                    other.close()
                }
            },
        )
    }
}
