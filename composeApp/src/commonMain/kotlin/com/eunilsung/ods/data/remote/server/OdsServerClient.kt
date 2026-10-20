package com.eunilsung.ods.data.remote.server

import com.eunilsung.ods.Config
import com.eunilsung.ods.shared.api.AccountDto
import com.eunilsung.ods.shared.api.ApiError
import com.eunilsung.ods.shared.api.ConsultationDto
import com.eunilsung.ods.shared.api.CustomerBookResponse
import com.eunilsung.ods.shared.api.CustomerDto
import com.eunilsung.ods.shared.api.IdVerificationRequest
import com.eunilsung.ods.shared.api.IdVerificationResponse
import com.eunilsung.ods.shared.api.LoginRequestDto
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
import com.eunilsung.ods.shared.api.StockDto
import com.eunilsung.ods.shared.api.SubscriptionDto
import com.eunilsung.ods.shared.api.VisitDto
import com.eunilsung.ods.shared.api.VisitStatusRequest
import com.eunilsung.ods.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 서버 요청 한 번의 결과.
 *
 * 서버가 거절한 것([Rejected])과 서버에 닿지 못한 것([Unreachable])을 가른다. 앞쪽은 다시 보내도
 * 같은 답이 오고, 뒤쪽은 다시 보내 볼 수 있다.
 */
sealed interface ServerResult<out T> {
    data class Success<T>(val value: T) : ServerResult<T>

    /** 서버가 답했지만 거절했다. [error] 는 본문을 읽지 못하면 null. */
    data class Rejected(val status: Int, val error: ApiError?) : ServerResult<Nothing>

    /** 응답이 없거나 읽을 수 없었다 — 네트워크 끊김·타임아웃·서버 다운. */
    data object Unreachable : ServerResult<Nothing>
}

/** 성공했으면 그 값, 아니면 null. */
fun <T> ServerResult<T>.valueOrNull(): T? = (this as? ServerResult.Success)?.value

/** 성공했으면 그 값, 아니면 [ServerException] 을 던진다. 결과를 돌려줘야 하는 저장소 함수에 쓴다. */
fun <T> ServerResult<T>.getOrThrow(): T = when (this) {
    is ServerResult.Success -> value
    is ServerResult.Rejected -> throw ServerException(status, error?.code)
    ServerResult.Unreachable -> throw ServerException(status = null, code = null)
}

/** 서버가 거절했거나([status]) 닿지 못했다(`status == null`). */
class ServerException(val status: Int?, val code: String?) : IllegalStateException("server ${status ?: "unreachable"} ${code.orEmpty()}")

/**
 * ODS 서버와 주고받는 것들. 인터페이스로 두어 테스트에서 대역을 끼운다.
 *
 * 로그인을 뺀 모든 요청에 토큰이 실린다. 열거형은 도메인 이름 글자로, 날짜는 `yyyy-MM-dd`, 시각은 epoch 밀리초로 오간다.
 */
interface OdsServer {
    suspend fun login(id: String, password: String): ServerResult<LoginResponse>

    /** [token] 을 서버에서 지운다. 실패해도 앱은 로그아웃된 것으로 본다. */
    suspend fun logout(token: String)

    /** 담당 고객과 그 고객에 딸린 기록 전부. */
    suspend fun customers(): ServerResult<CustomerBookResponse>

    suspend fun registerCustomer(request: NewCustomerRequest): ServerResult<CustomerDto>

    suspend fun openAccount(customerId: String, request: OpenAccountRequest): ServerResult<AccountDto>

    suspend fun saveRiskProfile(customerId: String, request: RiskProfileRequest): ServerResult<Unit>

    suspend fun addConsultation(customerId: String, request: NewConsultationRequest): ServerResult<ConsultationDto>

    suspend fun recordSignature(customerId: String, request: SignatureRequest): ServerResult<SignatureRecordDto>

    suspend fun verifyId(request: IdVerificationRequest): ServerResult<IdVerificationResponse>

    suspend fun products(): ServerResult<List<ProductDto>>

    suspend fun subscribe(request: NewSubscriptionRequest): ServerResult<SubscriptionDto>

    suspend fun visits(): ServerResult<List<VisitDto>>

    suspend fun addVisit(request: NewVisitRequest): ServerResult<VisitDto>

    suspend fun setVisitStatus(visitId: String, request: VisitStatusRequest): ServerResult<Unit>

    suspend fun stocks(): ServerResult<List<StockDto>>
}

class OdsServerClient(
    private val httpClient: HttpClient,
    private val tokenStore: AuthTokenStore,
    private val baseUrl: String = Config.Server.BASE_URL,
) : OdsServer {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun login(id: String, password: String): ServerResult<LoginResponse> =
        request("login", LoginResponse.serializer()) {
            httpClient.post("$baseUrl/auth/login") { jsonBody(LoginRequestDto.serializer(), LoginRequestDto(id, password)) }
        }

    override suspend fun logout(token: String) {
        if (token.isBlank()) return
        send("logout") {
            httpClient.post("$baseUrl/auth/logout") { header(HttpHeaders.Authorization, "Bearer $token") }
        }
    }

    override suspend fun customers(): ServerResult<CustomerBookResponse> =
        request("customers", CustomerBookResponse.serializer()) {
            httpClient.get("$baseUrl/customers") { auth() }
        }

    override suspend fun registerCustomer(request: NewCustomerRequest): ServerResult<CustomerDto> =
        request("register customer", CustomerDto.serializer()) {
            httpClient.post("$baseUrl/customers") {
                auth()
                jsonBody(NewCustomerRequest.serializer(), request)
            }
        }

    override suspend fun openAccount(customerId: String, request: OpenAccountRequest): ServerResult<AccountDto> =
        request("open account", AccountDto.serializer()) {
            httpClient.post("${customerUrl(customerId)}/accounts") {
                auth()
                jsonBody(OpenAccountRequest.serializer(), request)
            }
        }

    override suspend fun saveRiskProfile(customerId: String, request: RiskProfileRequest): ServerResult<Unit> =
        command("risk profile") {
            httpClient.put("${customerUrl(customerId)}/risk-profile") {
                auth()
                jsonBody(RiskProfileRequest.serializer(), request)
            }
        }

    override suspend fun addConsultation(customerId: String, request: NewConsultationRequest): ServerResult<ConsultationDto> =
        request("consultation", ConsultationDto.serializer()) {
            httpClient.post("${customerUrl(customerId)}/consultations") {
                auth()
                jsonBody(NewConsultationRequest.serializer(), request)
            }
        }

    override suspend fun recordSignature(customerId: String, request: SignatureRequest): ServerResult<SignatureRecordDto> =
        request("signature", SignatureRecordDto.serializer()) {
            httpClient.post("${customerUrl(customerId)}/signatures") {
                auth()
                jsonBody(SignatureRequest.serializer(), request)
            }
        }

    override suspend fun verifyId(request: IdVerificationRequest): ServerResult<IdVerificationResponse> =
        request("id verification", IdVerificationResponse.serializer()) {
            httpClient.post("$baseUrl/id-verifications") {
                auth()
                jsonBody(IdVerificationRequest.serializer(), request)
            }
        }

    override suspend fun products(): ServerResult<List<ProductDto>> =
        request("products", ListSerializer(ProductDto.serializer())) {
            httpClient.get("$baseUrl/products") { auth() }
        }

    override suspend fun subscribe(request: NewSubscriptionRequest): ServerResult<SubscriptionDto> =
        request("subscribe", SubscriptionDto.serializer()) {
            httpClient.post("$baseUrl/subscriptions") {
                auth()
                jsonBody(NewSubscriptionRequest.serializer(), request)
            }
        }

    override suspend fun visits(): ServerResult<List<VisitDto>> =
        request("visits", ListSerializer(VisitDto.serializer())) {
            httpClient.get("$baseUrl/visits") { auth() }
        }

    override suspend fun addVisit(request: NewVisitRequest): ServerResult<VisitDto> =
        request("add visit", VisitDto.serializer()) {
            httpClient.post("$baseUrl/visits") {
                auth()
                jsonBody(NewVisitRequest.serializer(), request)
            }
        }

    override suspend fun setVisitStatus(visitId: String, request: VisitStatusRequest): ServerResult<Unit> =
        command("visit status") {
            httpClient.put("$baseUrl/visits/${visitId.encodeURLPathPart()}/status") {
                auth()
                jsonBody(VisitStatusRequest.serializer(), request)
            }
        }

    override suspend fun stocks(): ServerResult<List<StockDto>> =
        request("stocks", ListSerializer(StockDto.serializer())) {
            httpClient.get("$baseUrl/stocks") { auth() }
        }

    private fun customerUrl(customerId: String): String = "$baseUrl/customers/${customerId.encodeURLPathPart()}"

    /** 인증 헤더. 공용 [httpClient] 에 기본 헤더로 박지 않는다 — 다른 곳으로 나가는 요청에 토큰이 실리지 않게. */
    private fun HttpRequestBuilder.auth() {
        val token = tokenStore.token()
        if (token.isNotBlank()) header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun <T> HttpRequestBuilder.jsonBody(serializer: KSerializer<T>, body: T) {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(serializer, body))
    }

    /** 본문이 있는 응답을 읽는다. */
    private suspend fun <T> request(label: String, serializer: KSerializer<T>, call: suspend () -> HttpResponse): ServerResult<T> {
        val (response, text) = send(label, call) ?: return ServerResult.Unreachable
        if (!response.status.isSuccess()) return rejected(label, response, text)
        val value = runCatching { json.decodeFromString(serializer, text) }.getOrNull()
        if (value == null) {
            Log.message("[Server] $label unreadable response")
            return ServerResult.Unreachable
        }
        return ServerResult.Success(value)
    }

    /** 본문 없이 성공 여부만 돌아오는 요청. */
    private suspend fun command(label: String, call: suspend () -> HttpResponse): ServerResult<Unit> {
        val (response, text) = send(label, call) ?: return ServerResult.Unreachable
        return if (response.status.isSuccess()) ServerResult.Success(Unit) else rejected(label, response, text)
    }

    /** 요청을 보내고 상태와 본문을 받는다. 닿지 못하면 null. 부른 쪽의 취소는 삼키지 않는다. */
    private suspend fun send(label: String, call: suspend () -> HttpResponse): Pair<HttpResponse, String>? {
        val result = try {
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                val response = call()
                response to response.bodyAsText()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.message("[Server] $label failed — ${e.message}")
            return null
        }
        if (result == null) Log.message("[Server] $label timed out — $baseUrl")
        return result
    }

    private fun rejected(label: String, response: HttpResponse, text: String): ServerResult.Rejected {
        val error = runCatching { json.decodeFromString(ApiError.serializer(), text) }.getOrNull()
        Log.message("[Server] $label rejected — ${response.status.value} ${error?.code.orEmpty()}")
        return ServerResult.Rejected(response.status.value, error)
    }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 10_000L
    }
}
