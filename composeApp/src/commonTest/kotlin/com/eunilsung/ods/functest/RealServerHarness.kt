package com.eunilsung.ods.functest

import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalProductRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalSignatureRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalStockRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalVisitRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.data.mapper.ProductMapper
import com.eunilsung.ods.data.mapper.ServerCustomerMapper
import com.eunilsung.ods.data.mapper.ServerMarketMapper
import com.eunilsung.ods.data.mapper.ServerProductMapper
import com.eunilsung.ods.data.mapper.ServerVisitMapper
import com.eunilsung.ods.data.mapper.StockMapper
import com.eunilsung.ods.data.mapper.VisitMapper
import com.eunilsung.ods.data.remote.server.AuthTokenStore
import com.eunilsung.ods.data.remote.server.OdsServerClient
import com.eunilsung.ods.data.remote.server.OdsSocket
import com.eunilsung.ods.data.remote.server.ServerResult
import com.eunilsung.ods.data.repository.CustomerRepositoryImpl
import com.eunilsung.ods.data.repository.IdVerificationRepositoryImpl
import com.eunilsung.ods.data.repository.LoginRepositoryImpl
import com.eunilsung.ods.data.repository.ProductRepositoryImpl
import com.eunilsung.ods.data.repository.SignatureRepositoryImpl
import com.eunilsung.ods.data.repository.StockRepositoryImpl
import com.eunilsung.ods.data.repository.VisitRepositoryImpl
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePoint
import com.eunilsung.ods.domain.usecase.AddVisitUseCase
import com.eunilsung.ods.domain.usecase.CancelVisitUseCase
import com.eunilsung.ods.domain.usecase.OpenAccountUseCase
import com.eunilsung.ods.domain.usecase.SubscribeProductUseCase
import com.eunilsung.ods.domain.usecase.VerifyIdCardUseCase
import com.eunilsung.ods.shared.api.NewCustomerRequest
import com.eunilsung.ods.testsupport.testLocalSecret
import com.eunilsung.ods.testsupport.testSecureSettings
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlin.random.Random

/**
 * 기능 테스트를 **진짜 서버에 붙여** 돌리기 위한 준비 도구.
 *
 * ### 무엇이 진짜이고 무엇이 대역인가
 *
 * 서버와 주고받는 것은 전부 앱이 실제로 쓰는 코드다 — [OdsServerClient](REST), [OdsSocket](WebSocket),
 * 그 위의 서버 모드 저장소(`data/repository`)와 유스케이스. 다른 테스트는 이 자리에 서버 대역을 끼우기 때문에,
 * 앱과 서버가 계약을 다르게 알고 있어도 양쪽 테스트가 모두 통과할 수 있다. 여기서만 그 어긋남이 드러난다.
 *
 * 대역은 기기에 묶인 것뿐이다. 기기 DB 는 메모리 DB, 설정은 메모리 설정, 보안 저장소(Keystore·Keychain)는 테스트용 암호.
 *
 * ### 기본은 꺼져 있다
 *
 * 서버 주소(`ODS_FUNCTEST_URL`)가 없으면 [startOrNull] 이 null 을 돌려주고 시나리오는 아무 일도 하지
 * 않고 통과한다. **평소 테스트에서 서버로 요청이 나가면 안 되기 때문이다.**
 *
 * ### 지울 수 없는 서버에 남는다
 *
 * 서버에는 지우는 길이 없고 로컬 서버 DB(`server/.localdb`)는 실행이 끝나도 남는다. 그래서 만든 고객은
 * 이름에 이번 실행의 이름표([runTag], `FT-<시각>`)를 붙이고 전화번호를 실행마다 새로 뽑는다. 시나리오는
 * "이번에 만든 것"만 보고 전체 개수를 세지 않는다 — 지난 실행이 남긴 것이 있어도 판정이 흐려지지 않는다.
 *
 * ### 상대는 서버 호출로만 움직인다
 *
 * 나(test1)는 앱 전체로, 상대([Peer])는 서버 호출만으로 움직인다. 상대가 한 일이 알림으로 도착해
 * 화면이 보는 자리(기기 DB)에 반영되는지를 본다.
 */
class RealServerHarness private constructor(private val baseUrl: String) {

    private val httpClient: HttpClient = functionalTestHttpClient()
    private val secret = testLocalSecret()
    private val settings = MapSettings()
    private val secureSettings = testSecureSettings(settings, secret)
    private val tokenStore = AuthTokenStore(secureSettings)
    private val peers = mutableListOf<Peer>()
    private val random = Random(nowMillis())
    private var created = 0

    /** 이번 실행이 만든 것에 붙는 이름표. */
    val runTag: String = "$TAG_PREFIX${nowMillis()}"

    val database = createFunctionalTestDatabase()
    val server = OdsServerClient(httpClient, tokenStore, baseUrl)
    private val events = OdsSocket(httpClient, tokenStore, baseUrl)

    private val customerMapper = CustomerMapper(secret)
    private val productMapper = ProductMapper(secret)
    private val visitMapper = VisitMapper(secret)
    private val stockMapper = StockMapper()
    private val serverCustomerMapper = ServerCustomerMapper()
    private val serverProductMapper = ServerProductMapper()

    val login = LoginRepositoryImpl(LocalLoginRepositoryImpl(settings, secureSettings), server, tokenStore, events)
    val customers = CustomerRepositoryImpl(
        LocalCustomerRepositoryImpl(database, customerMapper), database, server, events,
        customerMapper, productMapper, serverCustomerMapper, serverProductMapper,
    )
    val products = ProductRepositoryImpl(LocalProductRepositoryImpl(database, productMapper), database, server, productMapper, serverProductMapper)
    val signatures = SignatureRepositoryImpl(LocalSignatureRepositoryImpl(database), database, server, serverCustomerMapper)
    val visits = VisitRepositoryImpl(LocalVisitRepositoryImpl(database, visitMapper), database, server, events, visitMapper, ServerVisitMapper())
    val stocks = StockRepositoryImpl(LocalStockRepositoryImpl(database, stockMapper), database, server, events, stockMapper, ServerMarketMapper())
    private val idVerification = IdVerificationRepositoryImpl(server, serverCustomerMapper)

    val verifyIdCard = VerifyIdCardUseCase(customers, idVerification)
    val openAccount = OpenAccountUseCase(customers, signatures, login)
    val subscribe = SubscribeProductUseCase(customers, products, signatures, login)
    val addVisit = AddVisitUseCase(visits)
    val cancelVisit = CancelVisitUseCase(visits)

    /** 시연용 직원으로 로그인한다. 성공하면 저장소들이 서버 알림을 듣기 시작한다. */
    suspend fun signIn(id: String = ME, password: String = PASSWORD): LoginResult =
        login.login(LoginRequest(id, password), nowMillis() / 1000)

    /** 다른 시연용 직원으로 로그인한 상대. 서버 호출만 한다. */
    suspend fun peer(id: String): Peer {
        val peerTokens = AuthTokenStore(testSecureSettings(MapSettings()))
        val client = OdsServerClient(httpClient, peerTokens, baseUrl)
        val session = client.login(id, PASSWORD).must("상대 로그인 $id")
        peerTokens.save(session.token)
        return Peer(session.staff.id, client, session.token).also { peers += it }
    }

    /**
     * 이번 실행의 이름표가 붙은 고객 정보. 전화번호는 실행마다 새로 뽑는다 —
     * 계좌번호 앞부분이 전화번호 가운데 자리라 지난 실행과 겹치지 않게 하고,
     * 실화면 흐름이 끝 네 자리로 찾는 시연용 고객([FLOW_SEARCH_DIGITS])과 섞이지 않게 한다.
     */
    fun newCustomer(birthDate: LocalDate = LocalDate(1990, 1, 2), gender: Gender = Gender.MALE): NewCustomer =
        NewCustomer("$runTag-${++created}", birthDate, gender, uniquePhone(), "FT")

    /** [newCustomer] 를 앱 저장소로 등록한다. */
    suspend fun registerCustomer(birthDate: LocalDate = LocalDate(1990, 1, 2)): Customer =
        customers.register(newCustomer(birthDate), nowMillis())

    /** 서버에 같은 모양의 등록 요청을 보낸다. 상대가 고객을 등록할 때 쓴다. */
    fun newCustomerRequest(): NewCustomerRequest = newCustomer().let {
        NewCustomerRequest(it.name, it.birthDate.toString(), it.gender.name, it.phone, it.address)
    }

    /** 서명 정책을 넘는 두 획짜리 서명. 좌표는 어느 플랫폼에서나 같은 글자로 찍히는 값만 쓴다. */
    fun signature(): Signature = Signature(
        listOf(
            (0..7).map { SignaturePoint(0.125f * it, 0.25f) },
            (0..7).map { SignaturePoint(0.5f, 0.125f * it) },
        )
    )

    suspend fun customer(id: String): Customer? = customers.observeCustomer(id).first()

    /** 상대와 내 토큰을 모두 지운다. 시나리오가 중간에 깨져도 서버에 로그인이 쌓이지 않게 한다. */
    suspend fun close() {
        peers.forEach { runCatching { it.client.logout(it.token) } }
        val token = tokenStore.token()
        runCatching { login.logout() }
        runCatching { server.logout(token) }
        httpClient.close()
    }

    private fun uniquePhone(): String {
        while (true) {
            val middle = random.nextInt(1000, 10000).toString()
            val last = random.nextInt(0, 10000).toString().padStart(4, '0')
            val digits = "010$middle$last"
            if (FLOW_SEARCH_DIGITS.none { it in digits }) return "010-$middle-$last"
        }
    }

    /** 서버 호출만 하는 상대. */
    class Peer(val id: String, val client: OdsServerClient, internal val token: String)

    companion object {
        const val ME = "test1"
        const val PEER = "test2"
        const val SPARE = "test3"
        const val PASSWORD = "1234"

        /** 이번 실행이 만든 것에 붙는 이름표의 머리말. */
        const val TAG_PREFIX = "FT-"

        /** 실화면 흐름(`.maestro/flows`)이 고객을 찾는 숫자. 테스트가 만든 고객이 먼저 걸리면 흐름이 엉뚱한 고객을 연다. */
        private val FLOW_SEARCH_DIGITS = listOf("4587", "3302", "7731")

        /** 서버 주소가 있으면 준비된 도구를, 없으면 null 을 돌려준다. */
        fun startOrNull(): RealServerHarness? {
            val url = functionalTestServerUrl()
            if (url == null) {
                FT.line("SKIP reason=ODS_FUNCTEST_URL 없음 — 기능 테스트를 돌리지 않는다")
                return null
            }
            FT.line("SERVER url=$url")
            return RealServerHarness(url.trimEnd('/'))
        }
    }
}

/** 서버가 받아 준 값. 거절이나 불통이면 무엇이 그랬는지 남기고 단계를 깬다. */
fun <T> ServerResult<T>.must(what: String): T = when (this) {
    is ServerResult.Success -> value
    is ServerResult.Rejected -> throw StepFailure("$what — 서버가 거절했다 status=$status code=${error?.code}")
    ServerResult.Unreachable -> throw StepFailure("$what — 서버에 닿지 못했다")
}
