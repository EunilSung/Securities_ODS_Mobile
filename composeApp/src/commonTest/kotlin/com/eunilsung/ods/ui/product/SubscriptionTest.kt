package com.eunilsung.ods.ui.product

import com.eunilsung.ods.data.local.repository.LocalConsultSessionRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalProductRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalSignatureRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.data.mapper.ProductMapper
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.NewSubscription
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePoint
import com.eunilsung.ods.domain.model.Suitability
import com.eunilsung.ods.domain.usecase.ObserveAccountsUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomerUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomersUseCase
import com.eunilsung.ods.domain.usecase.ObserveProductUseCase
import com.eunilsung.ods.domain.usecase.StartConsultUseCase
import com.eunilsung.ods.domain.usecase.SubscribeProductUseCase
import com.eunilsung.ods.domain.usecase.SubscribeResult
import com.eunilsung.ods.domain.usecase.SubscriptionUseCases
import com.eunilsung.ods.domain.util.suitabilityFor
import com.eunilsung.ods.domain.util.todayInSeoul
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.testsupport.testLocalSecret
import com.eunilsung.ods.testsupport.testSecureSettings
import com.eunilsung.ods.ui.product.subscribe.SubscriptionActions
import com.eunilsung.ods.ui.product.subscribe.SubscriptionEvent
import com.eunilsung.ods.ui.product.subscribe.SubscriptionStep
import com.eunilsung.ods.ui.product.subscribe.SubscriptionViewModel
import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionTest {
    private val dispatcher = StandardTestDispatcher()
    private val database = createTestDatabase()
    private val customers = LocalCustomerRepositoryImpl(database, CustomerMapper(testLocalSecret()))
    private val products = LocalProductRepositoryImpl(database, ProductMapper(testLocalSecret()))
    private val signatures = LocalSignatureRepositoryImpl(database)
    private val login = MapSettings().let { LocalLoginRepositoryImpl(it, testSecureSettings(it), simulatedRoundTripMillis = 0) }
    private val session = LocalConsultSessionRepositoryImpl()
    private val subscribe = SubscribeProductUseCase(customers, products, signatures, login)
    private val signature = Signature(listOf(List(20) { SignaturePoint(0.1f + it * 0.03f, 0.5f) }))

    private fun viewModel() = SubscriptionViewModel(
        SubscriptionUseCases(
            observeProduct = ObserveProductUseCase(products),
            observeCustomers = ObserveCustomersUseCase(customers),
            observeCustomer = ObserveCustomerUseCase(customers),
            observeAccounts = ObserveAccountsUseCase(customers),
            startConsult = StartConsultUseCase(session),
            subscribe = subscribe,
        )
    )

    @BeforeTest
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        customers.refresh()
        products.refresh()
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun suitabilityFollowsProfileAndValidity() = runTest {
        val today = todayInSeoul()
        val neutral = customers.observeCustomer("C001").first()!!
        val stable = customers.observeCustomer("C004").first()!!
        val none = customers.observeCustomer("C005").first()!!
        val expired = customers.observeCustomer("C002").first()!!
        val bond = products.observeProduct("P002").first()!!
        val els = products.observeProduct("P007").first()!!
        assertEquals(Suitability.Suitable, neutral.suitabilityFor(bond, today))
        assertIs<Suitability.Unsuitable>(neutral.suitabilityFor(els, today))
        assertEquals(Suitability.Suitable, stable.suitabilityFor(bond, today))
        assertEquals(Suitability.NeedsAssessment, none.suitabilityFor(bond, today))
        assertEquals(Suitability.NeedsAssessment, expired.suitabilityFor(bond, today))
    }

    @Test
    fun useCaseRechecksEverythingBeforeSubscribing() = runTest {
        val base = NewSubscription("C001", "P002", "7001-2231-01", 1_000_000, unsuitableConfirmed = false)
        assertEquals(SubscribeResult.NeedsAssessment, subscribe(base.copy(customerId = "C005"), signature, "n"))
        assertEquals(SubscribeResult.UnsuitableNotConfirmed, subscribe(base.copy(productId = "P007"), signature, "n"))
        assertEquals(SubscribeResult.InvalidAmount, subscribe(base.copy(amount = 50_000), signature, "n"))
        assertEquals(SubscribeResult.InvalidAmount, subscribe(base.copy(amount = 1_005_000), signature, "n"))
        assertEquals(SubscribeResult.InvalidAccount, subscribe(base.copy(accountNo = "7001-3342-01"), signature, "n"))
        assertEquals(SubscribeResult.SignatureRequired, subscribe(base, Signature(emptyList()), "n"))
        assertTrue(products.observeSubscriptions("C001").first().isEmpty())

        val ok = subscribe(base, signature, "가입 메모")
        assertIs<SubscribeResult.Subscribed>(ok)
        assertNull(ok.subscription.coolingOffUntilMillis)
        assertEquals("국고채 3년물", products.observeSubscriptions("C001").first().single().productName)
        assertEquals(ConsultTopic.PRODUCT, customers.observeConsultations("C001").first().first().topic)
    }

    @Test
    fun elderlyComplexProductGetsCoolingOffAndUnsuitableNeedsConfirmation() = runTest {
        val request = NewSubscription("C009", "P007", "7001-2105-01", 1_000_000, unsuitableConfirmed = true)
        val result = subscribe(request, signature, "n")
        assertIs<SubscribeResult.Subscribed>(result)
        assertEquals(result.subscription.subscribedAtMillis + 2 * 24 * 60 * 60 * 1000L, result.subscription.coolingOffUntilMillis)
        assertTrue(result.subscription.unsuitableConfirmed)
    }

    @Test
    fun wizardWalksThroughStepsWithPresetCustomer() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(SubscriptionActions.OnEnter("P003", "C006"))
        val atSuitability = vm.uiState.first { it.step == SubscriptionStep.SUITABILITY }
        assertEquals("최지민", atSuitability.customer?.name)
        assertEquals("C006", session.activeCustomerId.value)
        assertEquals(Suitability.NeedsAssessment, atSuitability.suitability)
        assertFalse(atSuitability.canProceed)

        vm.onAction(SubscriptionActions.OnAssessClick)
        assertEquals(SubscriptionEvent.OpenRiskAssessment("C006"), vm.events.first())
    }

    @Test
    fun wizardCompletesForSuitableCustomer() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(SubscriptionActions.OnEnter("P004", null))
        vm.uiState.first { it.product != null && it.customerMatches.isNotEmpty() }
        vm.onAction(SubscriptionActions.OnCustomerSelect("C007"))
        vm.uiState.first { it.step == SubscriptionStep.SUITABILITY && it.suitability != null }
        vm.onAction(SubscriptionActions.OnNext)
        assertEquals(SubscriptionStep.EXPLANATION, vm.uiState.value.step)
        vm.onAction(SubscriptionActions.OnNext)
        assertEquals(SubscriptionStep.EXPLANATION, vm.uiState.value.step)
        repeat(3) { vm.onAction(SubscriptionActions.OnKeyPointToggle(it)) }
        vm.onAction(SubscriptionActions.OnNext)
        assertEquals(SubscriptionStep.DETAILS, vm.uiState.value.step)
        assertEquals("7001-6620-01", vm.uiState.value.accountNo)
        assertEquals(300_000, vm.uiState.value.amount)
        vm.onAction(SubscriptionActions.OnAmountStep(up = true))
        assertEquals(400_000, vm.uiState.value.amount)
        vm.onAction(SubscriptionActions.OnNext)
        vm.onAction(SubscriptionActions.OnSignatureChange(signature))
        vm.onAction(SubscriptionActions.OnSubmit("메모"))
        val done = vm.uiState.first { it.step == SubscriptionStep.DONE }
        assertNotNull(done.result)
        assertEquals(400_000, products.observeSubscriptions("C007").first().single().amount)
    }
}
