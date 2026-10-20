package com.eunilsung.ods.functest

import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.NewSubscription
import com.eunilsung.ods.domain.model.ProductRiskGrade
import com.eunilsung.ods.domain.model.RiskProfile
import com.eunilsung.ods.domain.model.SubscriptionRejectedException
import com.eunilsung.ods.domain.model.SubscriptionRejection
import com.eunilsung.ods.domain.usecase.SubscribeResult
import com.eunilsung.ods.domain.util.allows
import kotlinx.coroutines.flow.first
import kotlin.test.Test

/** 투자성향과 상품 가입 — 저장한 성향이 서버에 남고, 맞는 상품은 가입되며, 부적합 상품은 확인 없이 서버가 받지 않는지. */
class ProductScenarioTest {

    @Test
    fun 상품가입() = functionalSuite("상품가입") { harness ->
        listOf(
            scenario("성향저장후가입") {
                val customer = harness.registerCustomer()
                val account = harness.customers.openAccount(customer.id, AccountType.BROKERAGE, nowMillis())
                step("등록한 고객과 계좌가 기기 DB 에 들어온다") {
                    awaitBook(harness, "고객과 계좌가 기기 DB 에 있다") {
                        harness.customer(customer.id) != null &&
                            harness.customers.observeAccounts(customer.id).first().any { it.accountNo == account.accountNo }
                    }
                }
                step("투자성향을 저장하면 서버 장부에 남는다") {
                    harness.customers.saveRiskProfile(customer.id, RiskProfile.STABLE, nowMillis())
                    val onServer = harness.server.customers().must("고객 장부").customers.single { it.id == customer.id }
                    requireEquals(RiskProfile.STABLE.name, onServer.riskProfile, "서버의 투자성향")
                    awaitBook(harness, "기기 DB 의 투자성향이 안정형이다") { harness.customer(customer.id)?.riskProfile == RiskProfile.STABLE }
                }
                step("판매 상품을 서버에서 받는다") {
                    require(harness.products.refresh(), "상품을 받지 못했다")
                    require(harness.products.observeProducts().first().isNotEmpty(), "상품이 없다")
                }
                step("성향에 맞는 상품은 유스케이스로 가입되고 가입·서명·상담이 서버에 남는다") {
                    val product = harness.products.observeProducts().first().first { it.riskGrade == ProductRiskGrade.VERY_LOW }
                    val result = harness.subscribe(
                        NewSubscription(customer.id, product.id, account.accountNo, product.minAmount, unsuitableConfirmed = false),
                        harness.signature(),
                        note = "${harness.runTag} ${product.name}",
                    )
                    require(result is SubscribeResult.Subscribed, "가입되지 않았다: $result")
                    val book = harness.server.customers().must("고객 장부")
                    require(book.subscriptions.any { it.customerId == customer.id && it.productId == product.id }, "서버에 가입이 없다")
                    require(book.signatures.any { it.customerId == customer.id && it.purpose == "PRODUCT_SUBSCRIPTION" }, "서버에 서명이 없다")
                    awaitTrue("기기 DB 에 가입이 있다") {
                        harness.products.observeSubscriptions(customer.id).first().any { it.productId == product.id }
                    }
                }
                step("부적합 상품을 확인 없이 보내면 서버가 거절한다") {
                    val risky = harness.products.observeProducts().first().first { !RiskProfile.STABLE.allows(it.riskGrade) }
                    val rejected = runCatching {
                        harness.products.subscribe(
                            NewSubscription(customer.id, risky.id, account.accountNo, risky.minAmount, unsuitableConfirmed = false),
                            nowMillis(),
                            coolingOffUntilMillis = null,
                        )
                    }.exceptionOrNull()
                    require(rejected is SubscriptionRejectedException, "서버가 받아 줬다: $rejected")
                    requireEquals(SubscriptionRejection.UNSUITABLE_NOT_CONFIRMED, (rejected as SubscriptionRejectedException).reason, "거절 까닭")
                    require(harness.products.observeSubscriptions(customer.id).first().none { it.productId == risky.id }, "거절된 가입이 기기 DB 에 남았다")
                }
            },
        )
    }
}
