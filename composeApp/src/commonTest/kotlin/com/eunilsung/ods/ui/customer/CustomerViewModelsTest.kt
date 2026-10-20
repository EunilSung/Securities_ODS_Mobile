package com.eunilsung.ods.ui.customer

import com.eunilsung.ods.data.local.repository.LocalConsultSessionRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalProductRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalUserPreferenceRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalVisitRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.data.mapper.ProductMapper
import com.eunilsung.ods.data.mapper.VisitMapper
import com.eunilsung.ods.data.remote.server.ServerException
import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.NewConsultation
import com.eunilsung.ods.domain.model.NewCustomer
import com.eunilsung.ods.domain.repository.CustomerRepository
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
import com.eunilsung.ods.domain.usecase.ObserveSubscriptionsUseCase
import com.eunilsung.ods.domain.usecase.ObserveUpcomingVisitsUseCase
import com.eunilsung.ods.domain.usecase.RefreshCustomersUseCase
import com.eunilsung.ods.domain.usecase.RegisterCustomerUseCase
import com.eunilsung.ods.domain.usecase.SetMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.StartConsultUseCase
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.testsupport.testLocalSecret
import com.eunilsung.ods.testsupport.testSecureSettings
import com.eunilsung.ods.ui.customer.consultation.ConsultSaveError
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModelsTest {
    /** 서버에 닿지 못해 등록·기록하지 못하는 저장소. */
    private class OfflineCustomers(delegate: CustomerRepository) : CustomerRepository by delegate {
        override suspend fun register(customer: NewCustomer, nowMillis: Long): Customer = throw ServerException(status = null, code = null)
        override suspend fun addConsultation(consultation: NewConsultation, staffName: String, nowMillis: Long): Consultation =
            throw ServerException(status = null, code = null)
    }

    private val dispatcher = StandardTestDispatcher()
    private val settings = MapSettings()
    private val database = createTestDatabase()
    private val customers = LocalCustomerRepositoryImpl(database, CustomerMapper(testLocalSecret()))
    private val session = LocalConsultSessionRepositoryImpl()
    private val login = LocalLoginRepositoryImpl(settings, testSecureSettings(settings), simulatedRoundTripMillis = 0)
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
            observeUpcomingVisits = ObserveUpcomingVisitsUseCase(LocalVisitRepositoryImpl(database, VisitMapper(testLocalSecret()))),
            observeSubscriptions = ObserveSubscriptionsUseCase(LocalProductRepositoryImpl(database, ProductMapper(testLocalSecret()))),
        )
    )

    private fun consultationViewModel(repository: CustomerRepository = customers) = ConsultationViewModel(
        ConsultUseCases(
            observeActiveCustomerId = ObserveActiveCustomerIdUseCase(session),
            observeCustomer = ObserveCustomerUseCase(customers),
            observeConsultations = ObserveConsultationsUseCase(customers),
            addConsultation = AddConsultationUseCase(repository, login),
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
        assertEquals(ConsultSaveError.NOT_SIGNED_IN, vm.uiState.value.saveError)
        assertEquals("메모", vm.uiState.value.content)
    }

    @Test
    fun consultationServerFailureKeepsContent() = runTest(dispatcher) {
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        val vm = consultationViewModel(OfflineCustomers(customers))
        vm.onAction(ConsultationActions.OnEnter("C004"))
        vm.onAction(ConsultationActions.OnContentChange("메모"))
        vm.onAction(ConsultationActions.OnSave)
        advanceUntilIdle()
        assertEquals(ConsultSaveError.UNREACHABLE, vm.uiState.value.saveError)
        assertFalse(vm.uiState.value.isSaving)
        assertEquals("메모", vm.uiState.value.content)
    }

    @Test
    fun newCustomerServerFailureKeepsInputSoStaffCanRetry() = runTest(dispatcher) {
        val vm = NewCustomerViewModel(RegisterCustomerUseCase(OfflineCustomers(customers), session))
        vm.onAction(NewCustomerActions.OnNameChange("홍길동"))
        vm.onAction(NewCustomerActions.OnBirthChange("19900102"))
        vm.onAction(NewCustomerActions.OnPhoneChange("01011112222"))
        vm.onAction(NewCustomerActions.OnSubmit)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.saveFailed)
        assertFalse(state.isSaving)
        assertEquals("홍길동", state.name)
        assertEquals("01011112222", state.phoneDigits)
        assertNull(session.activeCustomerId.value)
        assertTrue(customers.observeCustomers().first().none { it.name == "홍길동" })
    }
}
