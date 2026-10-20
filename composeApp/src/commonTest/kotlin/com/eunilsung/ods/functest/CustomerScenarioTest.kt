package com.eunilsung.ods.functest

import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.AccountTypeBlock
import com.eunilsung.ods.domain.model.AccountTypeBlockedException
import com.eunilsung.ods.domain.model.IdCardInfo
import com.eunilsung.ods.domain.model.IdCardType
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.model.Terms
import com.eunilsung.ods.domain.usecase.IdCheckResult
import com.eunilsung.ods.domain.usecase.OpenAccountResult
import com.eunilsung.ods.domain.util.rrnBackFirst
import com.eunilsung.ods.domain.util.rrnFront
import korlibs.crypto.SHA256
import kotlinx.coroutines.flow.first
import kotlin.test.Test

/**
 * 고객 장부 — 서버 장부로 기기 DB 를 채우고, 등록한 고객에게 신분증 확인을 거쳐 계좌를 열고,
 * 서명은 지문만 남기고, 다른 직원이 쓴 것은 알림으로 다시 받는지.
 */
class CustomerScenarioTest {

    @Test
    fun 고객() = functionalSuite("고객") { harness ->
        listOf(
            scenario("장부받기") {
                step("다시 받으면 서버의 시연용 고객이 기기 DB 에 채워진다") {
                    require(harness.customers.refresh(), "고객 장부를 받지 못했다")
                    val seed = harness.customer(SEED_CUSTOMER)
                    requireEquals("강하늘", seed?.name, "시연용 고객 이름")
                    requireEquals("010-2231-4587", seed?.phone, "시연용 고객 전화번호(잠갔다 푼 값)")
                }
                step("시연용 고객의 계좌·상담 이력도 함께 온다") {
                    require(harness.customers.observeAccounts(SEED_CUSTOMER).first().isNotEmpty(), "계좌가 없다")
                    require(harness.customers.observeConsultations(SEED_CUSTOMER).first().isNotEmpty(), "상담 이력이 없다")
                }
            },
            scenario("등록후계좌개설") {
                val customer = harness.registerCustomer()
                step("등록한 고객이 서버가 정한 번호로 기기 DB 에 들어간다") {
                    require(customer.id.isNotBlank(), "서버가 고객 번호를 주지 않았다")
                    awaitBook(harness, "등록한 고객 ${customer.name} 이 기기 DB 에 있다") { harness.customer(customer.id)?.name == customer.name }
                }
                var verified: IdCheckResult.Verified? = null
                step("신분증을 고객 정보와 맞춰 보고 서버가 진위를 확인한다") {
                    val info = IdCardInfo(IdCardType.RESIDENT, customer.name, customer.rrnFront(), customer.rrnBackFirst())
                    val result = harness.verifyIdCard(customer.id, info, photo = null)
                    require(result is IdCheckResult.Verified, "진위 확인이 되지 않았다: $result")
                    verified = result as IdCheckResult.Verified
                }
                step("종합매매 계좌를 열면 7001-<전화번호 가운데>-NN 번호를 받는다") {
                    val result = openAccount(harness, customer.id, AccountType.BROKERAGE, verified)
                    require(result is OpenAccountResult.Opened, "열리지 않았다: $result")
                    val accountNo = (result as OpenAccountResult.Opened).account.accountNo
                    val middle = customer.phone.split("-")[1]
                    require(Regex("7001-$middle-\\d{2}").matches(accountNo), "계좌번호 모양이 다르다: $accountNo")
                    awaitBook(harness, "기기 DB 에 계좌 $accountNo 가 있다") {
                        harness.customers.observeAccounts(customer.id).first().any { it.accountNo == accountNo }
                    }
                }
                step("ISA 를 열고 나면 두 번째 ISA 는 유스케이스가 막는다") {
                    val first = openAccount(harness, customer.id, AccountType.ISA, verified)
                    require(first is OpenAccountResult.Opened, "첫 ISA 가 열리지 않았다: $first")
                    awaitBook(harness, "기기 DB 에 ISA 가 있다") {
                        harness.customers.observeAccounts(customer.id).first().any { it.type == AccountType.ISA }
                    }
                    val second = openAccount(harness, customer.id, AccountType.ISA, verified)
                    requireEquals(OpenAccountResult.TypeBlocked(AccountTypeBlock.ISA_DUPLICATE), second, "두 번째 ISA")
                }
                step("화면·유스케이스를 건너뛰어도 서버가 ISA 중복을 막는다") {
                    val blocked = runCatching { harness.customers.openAccount(customer.id, AccountType.ISA, nowMillis()) }.exceptionOrNull()
                    require(blocked is AccountTypeBlockedException, "서버가 막지 않았다: $blocked")
                    requireEquals(AccountTypeBlock.ISA_DUPLICATE, (blocked as AccountTypeBlockedException).block, "막은 까닭")
                }
                step("개설마다 서명 기록과 상담 이력이 서버에 남는다") {
                    val book = harness.server.customers().must("고객 장부")
                    val signatures = book.signatures.filter { it.customerId == customer.id && it.purpose == SignaturePurpose.ACCOUNT_OPENING.name }
                    requireEquals(2, signatures.size, "계좌 개설 서명 수")
                    requireEquals(2, book.consultations.count { it.customerId == customer.id }, "상담 이력 수")
                }
            },
            scenario("서명지문") {
                val customer = harness.registerCustomer()
                val signature = harness.signature()
                step("서명은 서버로 보내고 기기에는 앱이 계산하는 것과 같은 지문만 남는다") {
                    val record = harness.signatures.record(customer.id, SignaturePurpose.RISK_PROFILE, signature, nowMillis())
                    val expected = SHA256.digest(
                        signature.strokes.joinToString("|") { stroke -> stroke.joinToString(";") { "${it.x},${it.y}" } }.encodeToByteArray()
                    ).hex
                    requireEquals(expected, record.digest, "서버가 만든 지문")
                    awaitTrue("기기 DB 의 서명 기록이 지문 하나뿐이다") {
                        harness.signatures.observeRecords(customer.id).first().map { it.digest } == listOf(expected)
                    }
                }
            },
            scenario("다른직원의등록") {
                val peer = harness.peer(RealServerHarness.PEER)
                val request = harness.newCustomerRequest()
                step("다른 직원이 등록하면 바뀜 알림으로 내 기기 DB 에도 들어온다") {
                    val created = peer.client.registerCustomer(request).must("상대의 고객 등록")
                    awaitTrue("상대가 등록한 고객 ${created.name} 이 기기 DB 에 들어온다") {
                        harness.customer(created.id)?.name == request.name
                    }
                }
            },
        )
    }

    private suspend fun openAccount(harness: RealServerHarness, customerId: String, type: AccountType, verified: IdCheckResult.Verified?) =
        harness.openAccount(
            customerId = customerId,
            type = type,
            verification = verified?.verification,
            agreedTerms = Terms.entries.filter { it.required }.toSet(),
            pinSet = true,
            signature = harness.signature(),
            note = "${harness.runTag} $type",
        )

    private companion object {
        const val SEED_CUSTOMER = "C001"
    }
}
