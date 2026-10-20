package com.eunilsung.ods.domain.usecase

import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.NewSubscription
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.model.Subscription
import com.eunilsung.ods.domain.model.SubscriptionRejectedException
import com.eunilsung.ods.domain.model.SubscriptionRejection
import com.eunilsung.ods.domain.model.Suitability
import com.eunilsung.ods.domain.repository.CustomerRepository
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.repository.ProductRepository
import com.eunilsung.ods.domain.repository.SignatureRepository
import com.eunilsung.ods.domain.util.SignaturePolicy
import com.eunilsung.ods.domain.util.SubscriptionPolicy
import com.eunilsung.ods.domain.util.suitabilityFor
import com.eunilsung.ods.domain.util.todayInSeoul
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ObserveProductsUseCase(private val repository: ProductRepository) {
    operator fun invoke(): Flow<List<Product>> = repository.observeProducts()
}

class ObserveProductUseCase(private val repository: ProductRepository) {
    operator fun invoke(id: String): Flow<Product?> = repository.observeProduct(id)
}

class RefreshProductsUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(): Boolean = runCatching { repository.refresh() }.getOrDefault(false)
}

class ObserveSubscriptionsUseCase(private val repository: ProductRepository) {
    operator fun invoke(customerId: String): Flow<List<Subscription>> = repository.observeSubscriptions(customerId)
}

/** 가입 결과. */
sealed interface SubscribeResult {
    data class Subscribed(val subscription: Subscription) : SubscribeResult

    /** 투자성향이 없거나 만료됐다. */
    data object NeedsAssessment : SubscribeResult

    /** 부적합 상품인데 고객 확인을 받지 않았다. */
    data object UnsuitableNotConfirmed : SubscribeResult

    /** 최소 가입금액보다 적거나 만 원 단위가 아니다. */
    data object InvalidAmount : SubscribeResult

    /** 고객의 계좌가 아니다. */
    data object InvalidAccount : SubscribeResult

    data object SignatureRequired : SubscribeResult
    data object NotSignedIn : SubscribeResult

    /** 고객이나 상품을 찾지 못했다. */
    data object NotFound : SubscribeResult
}

/**
 * 상품에 가입한다 — 적합성·금액·계좌·서명을 다시 확인하고, 가입·서명 기록·상담 이력을 남긴다.
 *
 * 화면이 이미 막았더라도 여기서 한 번 더 본다. 고령투자자가 고난도·고위험 상품에 가입하면 숙려기간을 붙인다.
 *
 * @param note 상담 이력에 남길 문구. 화면이 고객 언어로 만들어 넘긴다.
 */
class SubscribeProductUseCase(
    private val customers: CustomerRepository,
    private val products: ProductRepository,
    private val signatures: SignatureRepository,
    private val login: LoginRepository,
) {
    suspend operator fun invoke(
        subscription: NewSubscription,
        signature: Signature,
        note: String,
        now: Long = nowMillis(),
    ): SubscribeResult {
        val staff = login.currentStaff.value ?: return SubscribeResult.NotSignedIn
        val customer = customers.observeCustomer(subscription.customerId).first() ?: return SubscribeResult.NotFound
        val product = products.observeProduct(subscription.productId).first() ?: return SubscribeResult.NotFound
        val today = todayInSeoul(now)
        when (customer.suitabilityFor(product, today)) {
            Suitability.NeedsAssessment -> return SubscribeResult.NeedsAssessment
            is Suitability.Unsuitable -> if (!subscription.unsuitableConfirmed) return SubscribeResult.UnsuitableNotConfirmed
            Suitability.Suitable -> Unit
        }
        if (!SubscriptionPolicy.isValidAmount(subscription.amount, product)) return SubscribeResult.InvalidAmount
        val accounts = customers.observeAccounts(customer.id).first()
        if (accounts.none { it.accountNo == subscription.accountNo }) return SubscribeResult.InvalidAccount
        if (!SignaturePolicy.isEnough(signature)) return SubscribeResult.SignatureRequired

        val coolingOff = if (SubscriptionPolicy.needsCoolingOff(customer, product, today)) {
            now + SubscriptionPolicy.COOLING_OFF_DAYS * DAY_MILLIS
        } else {
            null
        }
        val created = try {
            products.subscribe(subscription, now, coolingOff)
        } catch (e: SubscriptionRejectedException) {
            return when (e.reason) {
                SubscriptionRejection.NEEDS_ASSESSMENT -> SubscribeResult.NeedsAssessment
                SubscriptionRejection.UNSUITABLE_NOT_CONFIRMED -> SubscribeResult.UnsuitableNotConfirmed
                SubscriptionRejection.INVALID_AMOUNT -> SubscribeResult.InvalidAmount
                SubscriptionRejection.INVALID_ACCOUNT -> SubscribeResult.InvalidAccount
            }
        }
        signatures.record(customer.id, SignaturePurpose.PRODUCT_SUBSCRIPTION, signature, now)
        customers.addConsultation(NewConsultation(customer.id, ConsultChannel.VISIT, ConsultTopic.PRODUCT, note), staff.name, now)
        return SubscribeResult.Subscribed(created)
    }

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}

/** 상품 목록이 쓰는 유스케이스 묶음. */
class ProductListUseCases(
    val observeProducts: ObserveProductsUseCase,
    val refresh: RefreshProductsUseCase,
)

/** 상품 상세가 쓰는 유스케이스 묶음 — 상담 중 고객에게 맞는지 바로 보인다. */
class ProductDetailUseCases(
    val observeProduct: ObserveProductUseCase,
    val observeActiveCustomerId: ObserveActiveCustomerIdUseCase,
    val observeCustomer: ObserveCustomerUseCase,
)

/** 가입 절차가 쓰는 유스케이스 묶음. */
class SubscriptionUseCases(
    val observeProduct: ObserveProductUseCase,
    val observeCustomers: ObserveCustomersUseCase,
    val observeCustomer: ObserveCustomerUseCase,
    val observeAccounts: ObserveAccountsUseCase,
    val startConsult: StartConsultUseCase,
    val subscribe: SubscribeProductUseCase,
)
