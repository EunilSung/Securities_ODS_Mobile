package com.eunilsung.ods.data.mapper

import com.eunilsung.ods.domain.model.NewSubscription
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.ProductRiskGrade
import com.eunilsung.ods.domain.model.ProductType
import com.eunilsung.ods.domain.model.Subscription
import com.eunilsung.ods.shared.api.NewSubscriptionRequest
import com.eunilsung.ods.shared.api.ProductDto
import com.eunilsung.ods.shared.api.SubscriptionDto

/** 서버 상품·가입 응답을 도메인 모델과 바꾼다. 모르는 위험등급은 가장 위험한 1등급으로 읽는다. */
class ServerProductMapper {
    fun toModel(dto: ProductDto): Product = Product(
        id = dto.id,
        name = dto.name,
        type = dto.type.toEnumOr(ProductType.FUND),
        riskGrade = dto.riskGrade.toEnumOr(ProductRiskGrade.VERY_HIGH),
        summary = dto.summary,
        expectedReturn = dto.expectedReturn,
        feeInfo = dto.feeInfo,
        minAmount = dto.minAmount,
        keyPoints = dto.keyPoints,
        isComplex = dto.isComplex,
    )

    fun toModel(dto: SubscriptionDto): Subscription = Subscription(
        id = dto.id,
        customerId = dto.customerId,
        productId = dto.productId,
        productName = dto.productName,
        accountNo = dto.accountNo,
        amount = dto.amount,
        subscribedAtMillis = dto.subscribedAtMillis,
        unsuitableConfirmed = dto.unsuitableConfirmed,
        coolingOffUntilMillis = dto.coolingOffUntilMillis,
    )

    fun toRequest(subscription: NewSubscription, coolingOffUntilMillis: Long?): NewSubscriptionRequest = NewSubscriptionRequest(
        customerId = subscription.customerId,
        productId = subscription.productId,
        accountNo = subscription.accountNo,
        amount = subscription.amount,
        unsuitableConfirmed = subscription.unsuitableConfirmed,
        coolingOffUntilMillis = coolingOffUntilMillis,
    )
}
