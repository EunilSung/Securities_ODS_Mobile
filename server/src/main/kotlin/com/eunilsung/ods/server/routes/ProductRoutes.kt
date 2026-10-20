package com.eunilsung.ods.server.routes

import com.eunilsung.ods.server.realtime.EventHub
import com.eunilsung.ods.server.repository.AuthTokenRepository
import com.eunilsung.ods.server.repository.ProductRepository
import com.eunilsung.ods.shared.api.NewSubscriptionRequest
import com.eunilsung.ods.shared.api.ServerEvent
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/** 판매 상품과 상품 가입. 가입은 고객 장부의 일부라 고객 장부가 바뀌었다고 알린다. */
fun Route.productRoutes(products: ProductRepository, tokens: AuthTokenRepository, hub: EventHub) {

    get("/products") {
        call.callerStaffId(tokens) ?: return@get
        call.respond(db { products.all() })
    }

    post("/subscriptions") {
        call.callerStaffId(tokens) ?: return@post
        val request = call.receiveBody<NewSubscriptionRequest>()

        val subscription = db { products.subscribe(request, System.currentTimeMillis()) }
        hub.broadcast(ServerEvent(ServerEvent.TYPE_CHANGED, topic = ServerEvent.TOPIC_CUSTOMERS))
        call.respond(HttpStatusCode.Created, subscription)
    }
}
