package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.repository.AuthTokenRepository
import com.eunilsung.ods.server.repository.StockRepository
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** 관심종목 시세. 이후 체결은 `/ws` 의 시세 알림으로 받는다. */
fun Route.marketRoutes(stocks: StockRepository, tokens: AuthTokenRepository) {
    get("/stocks") {
        call.callerStaffId(tokens) ?: return@get
        call.respond(db { stocks.watchlist() })
    }
}
