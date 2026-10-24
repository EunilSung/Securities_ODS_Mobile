package com.eunilsung.ods.data.local.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.eunilsung.ods.data.local.seed.ProductSeed
import com.eunilsung.ods.data.mapper.ProductMapper
import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.domain.model.NewSubscription
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.Subscription
import com.eunilsung.ods.domain.repository.ProductRepository
import com.eunilsung.ods.util.newId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 서버 없이 도는 상품 저장소 — 상품이 하나도 없을 때 시연용 상품을 넣고, 가입은 기기 DB 에만 남긴다.
 */
class LocalProductRepositoryImpl(
    private val database: AppDatabase,
    private val mapper: ProductMapper,
) : ProductRepository {

    private val queries get() = database.appDatabaseQueries

    override fun observeProducts(): Flow<List<Product>> =
        queries.selectProducts().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map(mapper::toModel) }

    override fun observeProduct(id: String): Flow<Product?> =
        queries.selectProduct(id).asFlow().mapToOneOrNull(Dispatchers.Default).map { it?.let(mapper::toModel) }

    override suspend fun refresh(): Boolean = withContext(Dispatchers.Default) {
        if (queries.countProducts().executeAsOne() > 0L) return@withContext true
        database.transaction {
            ProductSeed.products.forEachIndexed { index, row -> queries.insertProduct(mapper.toEntity(row, index.toLong())) }
        }
        true
    }

    override fun observeSubscriptions(customerId: String): Flow<List<Subscription>> =
        queries.selectSubscriptions(customerId, mapper::toSubscription).asFlow().mapToList(Dispatchers.Default)

    override suspend fun subscribe(subscription: NewSubscription, nowMillis: Long, coolingOffUntilMillis: Long?): Subscription {
        val created = Subscription(
            id = newId(),
            customerId = subscription.customerId,
            productId = subscription.productId,
            productName = "",
            accountNo = subscription.accountNo,
            amount = subscription.amount,
            subscribedAtMillis = nowMillis,
            unsuitableConfirmed = subscription.unsuitableConfirmed,
            coolingOffUntilMillis = coolingOffUntilMillis,
        )
        return withContext(Dispatchers.Default) {
            queries.insertSubscription(mapper.toEntity(created))
            queries.selectSubscriptions(subscription.customerId, mapper::toSubscription).executeAsList().first { it.id == created.id }
        }
    }
}
