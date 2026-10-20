package com.eunilsung.ods.server

import com.eunilsung.ods.server.auth.LoginAttemptLimiter
import com.eunilsung.ods.server.config.ServerConfig
import com.eunilsung.ods.server.db.Database
import com.eunilsung.ods.server.realtime.EventHub
import com.eunilsung.ods.server.realtime.QuoteTicker
import com.eunilsung.ods.server.repository.AuthTokenRepository
import com.eunilsung.ods.server.repository.CustomerRepository
import com.eunilsung.ods.server.repository.ProductRepository
import com.eunilsung.ods.server.repository.StaffRepository
import com.eunilsung.ods.server.repository.StockRepository
import com.eunilsung.ods.server.repository.VisitRepository
import com.eunilsung.ods.server.routes.authRoutes
import com.eunilsung.ods.server.routes.customerRoutes
import com.eunilsung.ods.server.routes.eventRoutes
import com.eunilsung.ods.server.routes.healthRoutes
import com.eunilsung.ods.server.routes.idVerificationRoutes
import com.eunilsung.ods.server.routes.marketRoutes
import com.eunilsung.ods.server.routes.productRoutes
import com.eunilsung.ods.server.routes.visitRoutes
import com.eunilsung.ods.server.seed.DemoData
import com.eunilsung.ods.shared.api.ApiError
import com.eunilsung.ods.shared.api.ApiErrorCode
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

fun main() {
    val config = ServerConfig.fromEnv()
    embeddedServer(Netty, port = config.port) { module(config) }.start(wait = true)
}

/** 운영·로컬 진입점 — 설정대로 DB 에 붙고 마이그레이션을 돌린 뒤 [module] 을 올린다. */
fun Application.module(config: ServerConfig) {
    LoggerFactory.getLogger("Application").info(LocalAddresses.describe(config.port))
    module(dataSource = Database.connect(config.db))
}

/**
 * 서버 본체. 이미 준비된 [dataSource] 를 받는다 — 테스트가 자기 DB 를 끼워 넣는 자리다.
 *
 * 테스트는 [hub]·[quoteTicker] 를 직접 만들어 넘기고 [startTicker] 를 끈다. 시세가 저절로 움직이지 않고
 * 테스트가 [QuoteTicker.tick] 을 부를 때만 움직여, 알림 순서를 정확히 확인할 수 있다.
 */
fun Application.module(
    dataSource: DataSource,
    hub: EventHub = EventHub(),
    quoteTicker: QuoteTicker = QuoteTicker(StockRepository(dataSource), hub),
    startTicker: Boolean = true,
) {
    val log = LoggerFactory.getLogger("Application")

    install(ContentNegotiation) { json(ServerJson) }
    install(CallLogging) { level = Level.INFO }
    install(WebSockets) {
        pingPeriod = WEBSOCKET_PING_SECONDS.seconds
    }
    install(StatusPages) {
        exception<ApiException> { call, cause -> call.respond(cause.status, cause.error) }
        exception<Throwable> { call, cause ->
            log.error("처리하지 못한 예외", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError(ApiErrorCode.INTERNAL_ERROR, "일시적인 오류입니다"))
        }
    }

    DemoData.ensure(dataSource)
    val tokens = AuthTokenRepository(dataSource)
    val staff = StaffRepository(dataSource)
    if (startTicker) quoteTicker.start(this)

    routing {
        healthRoutes(dataSource)
        authRoutes(staff, tokens, LoginAttemptLimiter())
        customerRoutes(CustomerRepository(dataSource), staff, tokens, hub)
        productRoutes(ProductRepository(dataSource), tokens, hub)
        visitRoutes(VisitRepository(dataSource), tokens, hub)
        marketRoutes(StockRepository(dataSource), tokens)
        idVerificationRoutes(tokens)
        eventRoutes(tokens, hub)
    }
}

/** 앱과 같은 규칙으로 읽고 쓴다 — 모르는 필드는 넘기고 기본값도 실어 보낸다. */
val ServerJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** 중간 장비가 조용한 연결을 끊기 전에 살아 있음을 알리는 간격. */
private const val WEBSOCKET_PING_SECONDS = 15
