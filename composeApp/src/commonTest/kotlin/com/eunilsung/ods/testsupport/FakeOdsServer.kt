package com.eunilsung.ods.testsupport

import com.eunilsung.ods.data.remote.server.OdsServer
import com.eunilsung.ods.data.remote.server.ServerResult
import com.eunilsung.ods.shared.api.AccountDto
import com.eunilsung.ods.shared.api.ApiError
import com.eunilsung.ods.shared.api.ApiErrorCode
import com.eunilsung.ods.shared.api.ConsultationDto
import com.eunilsung.ods.shared.api.CustomerBookResponse
import com.eunilsung.ods.shared.api.CustomerDto
import com.eunilsung.ods.shared.api.IdVerificationRequest
import com.eunilsung.ods.shared.api.IdVerificationResponse
import com.eunilsung.ods.shared.api.LoginResponse
import com.eunilsung.ods.shared.api.NewConsultationRequest
import com.eunilsung.ods.shared.api.NewCustomerRequest
import com.eunilsung.ods.shared.api.NewSubscriptionRequest
import com.eunilsung.ods.shared.api.NewVisitRequest
import com.eunilsung.ods.shared.api.OpenAccountRequest
import com.eunilsung.ods.shared.api.ProductDto
import com.eunilsung.ods.shared.api.RiskProfileRequest
import com.eunilsung.ods.shared.api.SignatureRecordDto
import com.eunilsung.ods.shared.api.SignatureRequest
import com.eunilsung.ods.shared.api.StaffDto
import com.eunilsung.ods.shared.api.StockDto
import com.eunilsung.ods.shared.api.SubscriptionDto
import com.eunilsung.ods.shared.api.VisitDto
import com.eunilsung.ods.shared.api.VisitStatusRequest

/**
 * 서버 대역 — 메모리 안의 고객 장부·일정·상품·시세로 실제 서버처럼 답한다.
 *
 * 로그인은 [STAFF](`test1` / [PASSWORD])만 된다. [isReachable] 을 끄면 서버에 닿지 못한 상황이 되고,
 * [openAccountRejection] 으로 계좌 개설을 409 로 막는다. [signatureRequests] 로 서버에 간 서명을 본다.
 */
class FakeOdsServer : OdsServer {

    var isReachable = true

    /** 정해 두면 로그인이 429 로 잠긴다. */
    var lockedUntilEpochSeconds: Long? = null
    var remainingAttempts = 4
    var openAccountRejection: String? = null

    var book = CustomerBookResponse(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    var products: List<ProductDto> = emptyList()
    var visits: List<VisitDto> = emptyList()
    var stocks: List<StockDto> = emptyList()

    val revoked = mutableListOf<String>()
    val signatureRequests = mutableListOf<Pair<String, SignatureRequest>>()
    private var issued = 0

    override suspend fun login(id: String, password: String): ServerResult<LoginResponse> = reply {
        lockedUntilEpochSeconds?.let {
            return@reply ServerResult.Rejected(429, ApiError(ApiErrorCode.TOO_MANY_ATTEMPTS, lockedUntilEpochSeconds = it))
        }
        if (id != STAFF.id || password != PASSWORD) {
            return@reply ServerResult.Rejected(401, ApiError(ApiErrorCode.INVALID_CREDENTIALS, remainingAttempts = remainingAttempts))
        }
        ServerResult.Success(LoginResponse("token-${++issued}", STAFF))
    }

    override suspend fun logout(token: String) {
        if (isReachable) revoked += token
    }

    override suspend fun customers(): ServerResult<CustomerBookResponse> = reply { ServerResult.Success(book) }

    override suspend fun registerCustomer(request: NewCustomerRequest): ServerResult<CustomerDto> = reply {
        val created = CustomerDto(
            "S${++issued}", request.name, request.birthDate, request.gender, request.phone, request.address, "GENERAL",
            registeredAtMillis = 1_000,
        )
        book = book.copy(customers = book.customers + created)
        ServerResult.Success(created)
    }

    override suspend fun openAccount(customerId: String, request: OpenAccountRequest): ServerResult<AccountDto> = reply {
        openAccountRejection?.let { return@reply ServerResult.Rejected(409, ApiError(it)) }
        val account = AccountDto("9001-0000-${++issued}", customerId, request.type, 2_000)
        book = book.copy(accounts = book.accounts + account)
        ServerResult.Success(account)
    }

    override suspend fun saveRiskProfile(customerId: String, request: RiskProfileRequest): ServerResult<Unit> =
        reply { ServerResult.Success(Unit) }

    override suspend fun addConsultation(customerId: String, request: NewConsultationRequest): ServerResult<ConsultationDto> = reply {
        val created = ConsultationDto("SC${++issued}", customerId, 3_000, request.channel, request.topic, request.content, STAFF.name)
        book = book.copy(consultations = book.consultations + created)
        ServerResult.Success(created)
    }

    override suspend fun recordSignature(customerId: String, request: SignatureRequest): ServerResult<SignatureRecordDto> = reply {
        signatureRequests += customerId to request
        ServerResult.Success(SignatureRecordDto("SG${++issued}", customerId, request.purpose, request.signedAtMillis, "server-digest-$issued"))
    }

    override suspend fun verifyId(request: IdVerificationRequest): ServerResult<IdVerificationResponse> =
        reply { ServerResult.Success(IdVerificationResponse(verified = true, verifiedAtMillis = 4_000)) }

    override suspend fun products(): ServerResult<List<ProductDto>> = reply { ServerResult.Success(products) }

    override suspend fun subscribe(request: NewSubscriptionRequest): ServerResult<SubscriptionDto> = reply {
        ServerResult.Success(
            SubscriptionDto(
                "SB${++issued}", request.customerId, request.productId, request.productId, request.accountNo, request.amount, 5_000,
                request.unsuitableConfirmed, request.coolingOffUntilMillis,
            )
        )
    }

    override suspend fun visits(): ServerResult<List<VisitDto>> = reply { ServerResult.Success(visits) }

    override suspend fun addVisit(request: NewVisitRequest): ServerResult<VisitDto> = reply {
        val created = VisitDto("SV${++issued}", request.customerId, "", request.scheduledAtMillis, request.place, request.purpose, request.memo, "SCHEDULED")
        visits = visits + created
        ServerResult.Success(created)
    }

    override suspend fun setVisitStatus(visitId: String, request: VisitStatusRequest): ServerResult<Unit> = reply {
        visits = visits.map { if (it.id == visitId) it.copy(status = request.status) else it }
        ServerResult.Success(Unit)
    }

    override suspend fun stocks(): ServerResult<List<StockDto>> = reply { ServerResult.Success(stocks) }

    private inline fun <T> reply(answer: () -> ServerResult<T>): ServerResult<T> =
        if (isReachable) answer() else ServerResult.Unreachable

    companion object {
        const val PASSWORD = "1234"
        val STAFF = StaffDto(id = "test1", name = "김민준", branch = "강남금융센터", position = "대리")
    }
}
