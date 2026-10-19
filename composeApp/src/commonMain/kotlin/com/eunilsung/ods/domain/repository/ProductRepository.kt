package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.NewSubscription
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

/** 판매 상품과 고객의 가입 기록. */
interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>

    fun observeProduct(id: String): Flow<Product?>

    /** 판매 상품을 다시 받는다. 실패하면 가지고 있던 것을 그대로 둔다. */
    suspend fun refresh(): Boolean

    /** 한 고객의 가입(최근 것부터). */
    fun observeSubscriptions(customerId: String): Flow<List<Subscription>>

    suspend fun subscribe(subscription: NewSubscription, nowMillis: Long, coolingOffUntilMillis: Long?): Subscription
}
