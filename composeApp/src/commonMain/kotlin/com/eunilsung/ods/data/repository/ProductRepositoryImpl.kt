package com.eunilsung.ods.data.repository

import com.eunilsung.ods.data.local.repository.LocalProductRepositoryImpl
import com.eunilsung.ods.data.mapper.ProductMapper
import com.eunilsung.ods.data.mapper.ServerProductMapper
import com.eunilsung.ods.data.remote.server.OdsServer
import com.eunilsung.ods.data.remote.server.ServerResult
import com.eunilsung.ods.data.remote.server.getOrThrow
import com.eunilsung.ods.data.remote.server.valueOrNull
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.NewSubscription
import com.eunilsung.ods.domain.model.Subscription
import com.eunilsung.ods.domain.model.SubscriptionRejectedException
import com.eunilsung.ods.domain.model.SubscriptionRejection
import com.eunilsung.ods.domain.repository.ProductRepository
import com.eunilsung.ods.shared.api.ApiErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 서버 모드 상품 저장소. 판매 상품은 서버 목록으로 기기 DB 를 통째로 바꾸고, 가입은 서버가 받아 준 결과만 DB 에 남긴다.
 *
 * 서버가 적합성·금액·계좌를 다시 보고 막으면 [SubscriptionRejectedException] 을 던진다.
 */
class ProductRepositoryImpl(
    private val local: LocalProductRepositoryImpl,
    private val database: AppDatabase,
    private val server: OdsServer,
    private val mapper: ProductMapper,
    private val serverMapper: ServerProductMapper,
) : ProductRepository by local {

    private val queries get() = database.appDatabaseQueries

    override suspend fun refresh(): Boolean {
        val products = server.products().valueOrNull() ?: return false
        withContext(Dispatchers.Default) {
            database.transaction {
                queries.deleteProducts()
                products.forEachIndexed { index, dto -> queries.insertProduct(mapper.toEntity(serverMapper.toModel(dto), index.toLong())) }
            }
        }
        return true
    }

    override suspend fun subscribe(subscription: NewSubscription, nowMillis: Long, coolingOffUntilMillis: Long?): Subscription {
        val result = server.subscribe(serverMapper.toRequest(subscription, coolingOffUntilMillis))
        if (result is ServerResult.Rejected) rejectionOf(result.error?.code)?.let { throw SubscriptionRejectedException(it) }
        val created = serverMapper.toModel(result.getOrThrow())
        withContext(Dispatchers.Default) { queries.upsertSubscription(mapper.toEntity(created)) }
        return created
    }

    private fun rejectionOf(code: String?): SubscriptionRejection? = when (code) {
        ApiErrorCode.NEEDS_ASSESSMENT -> SubscriptionRejection.NEEDS_ASSESSMENT
        ApiErrorCode.UNSUITABLE_NOT_CONFIRMED -> SubscriptionRejection.UNSUITABLE_NOT_CONFIRMED
        ApiErrorCode.INVALID_AMOUNT -> SubscriptionRejection.INVALID_AMOUNT
        ApiErrorCode.ACCOUNT_MISMATCH -> SubscriptionRejection.INVALID_ACCOUNT
        else -> null
    }
}
