package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.data.local.seed.ProductSeedRow
import com.eunilsung.ods.db.ProductEntity
import com.eunilsung.ods.db.SubscriptionEntity
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.ProductRiskGrade
import com.eunilsung.ods.domain.model.ProductType
import com.eunilsung.ods.domain.model.Subscription

/** 기기 DB 행·시연용 데이터를 상품 도메인 모델로 바꾼다. 모르는 위험등급은 가장 위험한 1등급으로 읽는다. */
class ProductMapper {
    fun toModel(entity: ProductEntity): Product = Product(
        id = entity.id,
        name = entity.name,
        type = ProductType.entries.firstOrNull { it.name == entity.type } ?: ProductType.FUND,
        riskGrade = ProductRiskGrade.entries.firstOrNull { it.name == entity.riskGrade } ?: ProductRiskGrade.VERY_HIGH,
        summary = entity.summary,
        expectedReturn = entity.expectedReturn,
        feeInfo = entity.feeInfo,
        minAmount = entity.minAmount,
        keyPoints = entity.keyPoints.split(KEY_POINT_SEPARATOR).filter { it.isNotBlank() },
        isComplex = entity.isComplex != 0L,
    )

    internal fun toEntity(row: ProductSeedRow, sortOrder: Long): ProductEntity = ProductEntity(
        id = row.id,
        name = row.name,
        type = row.type,
        riskGrade = row.riskGrade,
        summary = row.summary,
        expectedReturn = row.expectedReturn,
        feeInfo = row.feeInfo,
        minAmount = row.minAmount,
        keyPoints = row.keyPoints.joinToString(KEY_POINT_SEPARATOR),
        isComplex = if (row.isComplex) 1L else 0L,
        sortOrder = sortOrder,
    )

    /** 상품 이름을 붙여 읽은 가입 한 행. SQLDelight 조회의 열 순서와 같다. */
    fun toSubscription(
        id: String,
        customerId: String,
        productId: String,
        productName: String,
        accountNo: String,
        amount: Long,
        subscribedAt: Long,
        unsuitableConfirmed: Long,
        coolingOffUntil: Long?,
    ): Subscription = Subscription(
        id = id,
        customerId = customerId,
        productId = productId,
        productName = productName,
        accountNo = accountNo,
        amount = amount,
        subscribedAtMillis = subscribedAt,
        unsuitableConfirmed = unsuitableConfirmed != 0L,
        coolingOffUntilMillis = coolingOffUntil,
    )

    fun toEntity(subscription: Subscription): SubscriptionEntity = SubscriptionEntity(
        id = subscription.id,
        customerId = subscription.customerId,
        productId = subscription.productId,
        accountNo = subscription.accountNo,
        amount = subscription.amount,
        subscribedAt = subscription.subscribedAtMillis,
        unsuitableConfirmed = if (subscription.unsuitableConfirmed) 1L else 0L,
        coolingOffUntil = subscription.coolingOffUntilMillis,
    )

    private companion object {
        const val KEY_POINT_SEPARATOR = "\n"
    }
}
