package com.eunilsung.ods.ui.customer

import com.eunilsung.ods.data.local.repository.LocalConsultSessionRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalUserPreferenceRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalVisitRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalProductRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.data.mapper.ProductMapper
import com.eunilsung.ods.domain.usecase.ObserveSubscriptionsUseCase
import com.eunilsung.ods.data.mapper.VisitMapper
import com.eunilsung.ods.domain.usecase.ObserveUpcomingVisitsUseCase
import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.usecase.AddConsultationUseCase
import com.eunilsung.ods.domain.usecase.ConsultUseCases
import com.eunilsung.ods.domain.usecase.CustomerDetailUseCases
import com.eunilsung.ods.domain.usecase.CustomerListUseCases
import com.eunilsung.ods.domain.usecase.EndConsultUseCase
import com.eunilsung.ods.domain.usecase.ObserveAccountsUseCase
import com.eunilsung.ods.domain.usecase.ObserveActiveCustomerIdUseCase
import com.eunilsung.ods.domain.usecase.ObserveConsultationsUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomerUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomersRefreshingUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomersUseCase
import com.eunilsung.ods.domain.usecase.ObserveMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.RefreshCustomersUseCase
import com.eunilsung.ods.domain.usecase.RegisterCustomerUseCase
import com.eunilsung.ods.domain.usecase.SetMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.StartConsultUseCase
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.ui.customer.consultation.ConsultationActions
import com.eunilsung.ods.ui.customer.consultation.ConsultationEvent
import com.eunilsung.ods.ui.customer.consultation.ConsultationViewModel
import com.eunilsung.ods.ui.customer.detail.CustomerDetailActions
import com.eunilsung.ods.ui.customer.detail.CustomerDetailViewModel
import com.eunilsung.ods.ui.customer.list.CustomerListActions
import com.eunilsung.ods.ui.customer.list.CustomerListEvent
import com.eunilsung.ods.ui.customer.list.CustomerListViewModel
import com.eunilsung.ods.ui.customer.register.NewCustomerActions
import com.eunilsung.ods.ui.customer.register.NewCustomerEvent
import com.eunilsung.ods.ui.customer.register.NewCustomerViewModel
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val settings = MapSettings()
    private val database = createTestDatabase()
    private val customers = LocalCustomerRepositoryImpl(database, CustomerMapper())
    private val session = LocalConsultSessionRepositoryImpl()
    private val login = LocalLoginRepositoryImpl(settings, simulatedRoundTripMillis = 0)
    private val preferences = LocalUserPreferenceRepositoryImpl(settings)

    private fun listViewModel() = CustomerListViewModel(
        CustomerListUseCases(
            observeCustomers = ObserveCustomersUseCase(customers),
            refresh = RefreshCustomersUseCase(customers),
            observeRefreshing = ObserveCustomersRefreshingUseCase(customers),
            observeMaskPersonalInfo = ObserveMaskPersonalInfoUseCase(preferences),
            observeActiveCustomerId = ObserveActiveCustomerIdUseCase(session),
        )
    )

    private fun detailViewModel() = CustomerDetailViewModel(
        CustomerDetailUseCases(
            observeCustomer = ObserveCustomerUseCase(customers),
            observeConsultations = ObserveConsultationsUseCase(customers),
            observeAccounts = ObserveAccountsUseCase(customers),
            observeMaskPersonalInfo = ObserveMaskPersonalInfoUseCase(preferences),
            setMaskPersonalInfo = SetMaskPersonalInfoUseCase(preferences),
            startConsult = StartConsultUseCase(session),
            observeUpcomingVisits = ObserveUpcomingVisitsUseCase(LocalVisitRepositoryImpl(database, VisitMapper())),
            observeSubscriptions = ObserveSubscriptionsUseCase(LocalProductRepositoryImpl(database, ProductMapper())),
        )
    )

    private fun consultationViewModel() = ConsultationViewModel(
        ConsultUseCases(
            observeActiveCustomerId = ObserveActiveCustomerIdUseCase(session),
            observeCustomer = ObserveCustomerUseCase(customers),
            observeConsultations = ObserveConsultationsUseCase(customers),
            addConsultation = AddConsultationUseCase(customers, login),
            endConsult = EndConsultUseCase(session),
        )
    )

    @BeforeTest
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        customers.refresh()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun listFiltersByQueryAndOpensDetail() = runTest(dispatcher) {
        val vm = listViewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(CustomerListActions.OnQueryChange("ㄱㅎㄴ"))
        val state = vm.uiState.first { it.query == "ㄱㅎㄴ" && it.totalCount > 0 }
        assertEquals(listOf("강하늘"), state.customers.map { it.name })
        assertEquals(12, state.totalCount)

        vm.onAction(CustomerListActions.OnCustomerClick("C001"))
        assertEquals(CustomerListEvent.OpenDetail("C001"), vm.events.first())
    }

    @Test
    fun openingDetailStartsConsultAndEyeTogglesMasking() = runTest(dispatcher) {
        val vm = detailViewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(CustomerDetailActions.OnEnter("C002"))
        val state = vm.uiState.first { it.customer != null }
        assertEquals("C002", session.activeCustomerId.value)
        assertEquals("김서윤", state.customer?.name)
        assertTrue(state.maskPersonalInfo)

        vm.onAction(CustomerDetailActions.OnToggleMask)
        vm.uiState.first { !it.maskPersonalInfo }
    }

    @Test
    fun newCustomerShowsErrorsThenRegistersAndStartsConsult() = runTest(dispatcher) {
        val vm = NewCustomerViewModel(RegisterCustomerUseCase(customers, session))
        vm.onAction(NewCustomerActions.OnSubmit)
        assertTrue(vm.uiState.value.showErrors)
        assertTrue(vm.uiState.value.nameError && vm.uiState.value.birthError && vm.uiState.value.phoneError)

        vm.onAction(NewCustomerActions.OnNameChange("홍길동"))
        vm.onAction(NewCustomerActions.OnBirthChange("1990-01-02"))
        vm.onAction(NewCustomerActions.OnGenderChange(Gender.FEMALE))
        vm.onAction(NewCustomerActions.OnPhoneChange("010 1111 2222"))
        assertFalse(vm.uiState.value.hasError)
        vm.onAction(NewCustomerActions.OnSubmit)
        advanceUntilIdle()

        val event = vm.events.first() as NewCustomerEvent.Registered
        assertEquals(event.customerId, session.activeCustomerId.value)
        val saved = customers.observeCustomer(event.customerId).first()
        assertEquals("010-1111-2222", saved?.phone)
        assertEquals(Gender.FEMALE, saved?.gender)
    }

    @Test
    fun consultationIsSavedUnderSignedInStaff() = runTest(dispatcher) {
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        val vm = consultationViewModel()
        vm.onAction(ConsultationActions.OnEnter("C004"))
        assertFalse(vm.uiState.value.canSave)
        vm.onAction(ConsultationActions.OnChannelChange(ConsultChannel.PHONE))
        vm.onAction(ConsultationActions.OnTopicChange(ConsultTopic.MARKET))
        vm.onAction(ConsultationActions.OnContentChange("금리 전망 문의"))
        vm.onAction(ConsultationActions.OnSave)
        advanceUntilIdle()

        assertEquals(ConsultationEvent.Saved, vm.events.first())
        assertEquals("", vm.uiState.value.content)
        assertTrue(vm.uiState.value.justSaved)
        val latest = customers.observeConsultations("C004").first().first()
        assertEquals("금리 전망 문의", latest.content)
        assertEquals("김민준", latest.staffName)
        assertEquals(ConsultTopic.MARKET, latest.topic)
    }

    @Test
    fun consultationIsNotSavedWithoutSignedInStaff() = runTest(dispatcher) {
        val vm = consultationViewModel()
        vm.onAction(ConsultationActions.OnEnter("C004"))
        vm.onAction(ConsultationActions.OnContentChange("메모"))
        vm.onAction(ConsultationActions.OnSave)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.saveFailed)
        assertEquals("메모", vm.uiState.value.content)
    }
}
