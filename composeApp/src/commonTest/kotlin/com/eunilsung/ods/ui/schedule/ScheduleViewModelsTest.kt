package com.eunilsung.ods.ui.schedule

import com.eunilsung.ods.data.local.repository.LocalConsultSessionRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalVisitRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.data.mapper.VisitMapper
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.NewVisit
import com.eunilsung.ods.domain.model.VisitPurpose
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.domain.usecase.AddVisitResult
import com.eunilsung.ods.domain.usecase.AddVisitUseCase
import com.eunilsung.ods.domain.usecase.CancelVisitUseCase
import com.eunilsung.ods.domain.usecase.CompleteVisitUseCase
import com.eunilsung.ods.domain.usecase.NewVisitUseCases
import com.eunilsung.ods.domain.usecase.ObserveCustomersUseCase
import com.eunilsung.ods.domain.usecase.ObserveDayVisitsUseCase
import com.eunilsung.ods.domain.usecase.ObserveUpcomingVisitsUseCase
import com.eunilsung.ods.domain.usecase.ObserveVisitUseCase
import com.eunilsung.ods.domain.usecase.ObserveWeekVisitCountsUseCase
import com.eunilsung.ods.domain.usecase.RefreshVisitsUseCase
import com.eunilsung.ods.domain.usecase.ScheduleUseCases
import com.eunilsung.ods.domain.usecase.VisitDetailUseCases
import com.eunilsung.ods.domain.util.atTimeMillis
import com.eunilsung.ods.domain.util.todayInSeoul
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.testsupport.testLocalSecret
import com.eunilsung.ods.ui.schedule.detail.VisitDetailActions
import com.eunilsung.ods.ui.schedule.detail.VisitDetailEvent
import com.eunilsung.ods.ui.schedule.detail.VisitDetailViewModel
import com.eunilsung.ods.ui.schedule.register.NewVisitActions
import com.eunilsung.ods.ui.schedule.register.NewVisitEvent
import com.eunilsung.ods.ui.schedule.register.NewVisitViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

/** 시연용 일정 중 내일 잡힌 것(서지수). 취소한 일정은 세지 않는다. */
private const val VisitSeedTomorrowCount = 1

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val database = createTestDatabase()
    private val customers = LocalCustomerRepositoryImpl(database, CustomerMapper(testLocalSecret()))
    private val visits = LocalVisitRepositoryImpl(database, VisitMapper(testLocalSecret()))
    private val session = LocalConsultSessionRepositoryImpl()
    private val today = todayInSeoul()

    @BeforeTest
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        customers.refresh()
        visits.refresh()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun scheduleOpensTodayWithNamedVisitsAndMovesByWeek() = runTest(dispatcher) {
        val vm = ScheduleViewModel(
            ScheduleUseCases(ObserveDayVisitsUseCase(visits), ObserveWeekVisitCountsUseCase(visits), RefreshVisitsUseCase(visits))
        )
        backgroundScope.launch { vm.uiState.collect {} }
        val state = vm.uiState.first { it.visits.isNotEmpty() }
        assertEquals(today, state.selected)
        assertEquals(listOf("정우성", "김서윤", "오세훈"), state.visits.map { it.customerName })
        assertEquals(3, state.counts[today])

        vm.onAction(ScheduleActions.OnNextWeek)
        assertEquals(today.plus(DatePeriod(days = 7)), vm.uiState.first { it.selected != today }.selected)
    }

    @Test
    fun addingRejectsPastTimesAndUpcomingListsOnlyScheduled() = runTest {
        val add = AddVisitUseCase(visits)
        val past = add(NewVisit("C006", today.atTimeMillis(8, 0), "여의도", VisitPurpose.CONSULT, ""), now = today.atTimeMillis(9, 0))
        assertEquals(AddVisitResult.InPast, past)

        val tomorrow = today.plus(DatePeriod(days = 1))
        val added = add(NewVisit("C006", tomorrow.atTimeMillis(9, 0), " 여의도 ", VisitPurpose.PRODUCT, ""), now = today.atTimeMillis(9, 0))
        assertIs<AddVisitResult.Added>(added)
        assertEquals("여의도", added.visit.place)
        assertEquals("최지민", added.visit.customerName)

        val upcoming = ObserveUpcomingVisitsUseCase(visits)
        assertEquals(1, upcoming("C006", now = today.atTimeMillis(9, 0)).first().size)
        CancelVisitUseCase(visits)(added.visit.id)
        assertEquals(0, upcoming("C006", now = today.atTimeMillis(9, 0)).first().size)

        val week = ObserveWeekVisitCountsUseCase(visits)(tomorrow).first()
        assertEquals(VisitSeedTomorrowCount, week[tomorrow])
    }

    @Test
    fun completingVisitStartsConsultAndOpensNoteWithPurposeTopic() = runTest(dispatcher) {
        val vm = VisitDetailViewModel(VisitDetailUseCases(ObserveVisitUseCase(visits), CompleteVisitUseCase(visits, session), CancelVisitUseCase(visits)))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(VisitDetailActions.OnEnter("V001"))
        vm.uiState.first { it.visit != null }

        vm.onAction(VisitDetailActions.OnComplete)
        assertEquals(VisitDetailEvent.OpenConsultation("C005", ConsultTopic.ACCOUNT_OPENING), vm.events.first())
        assertEquals("C005", session.activeCustomerId.value)
        assertEquals(VisitStatus.COMPLETED, vm.uiState.first { it.visit?.status == VisitStatus.COMPLETED }.visit?.status)
    }

    @Test
    fun cancelingNeedsConfirmation() = runTest(dispatcher) {
        val vm = VisitDetailViewModel(VisitDetailUseCases(ObserveVisitUseCase(visits), CompleteVisitUseCase(visits, session), CancelVisitUseCase(visits)))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(VisitDetailActions.OnEnter("V003"))
        vm.uiState.first { it.visit != null }
        vm.onAction(VisitDetailActions.OnCancelClick)
        assertTrue(vm.uiState.first { it.isConfirmingCancel }.isConfirmingCancel)
        vm.onAction(VisitDetailActions.OnCancelDismiss)
        assertFalse(vm.uiState.first { !it.isConfirmingCancel }.isConfirmingCancel)

        vm.onAction(VisitDetailActions.OnCancelClick)
        vm.onAction(VisitDetailActions.OnCancelConfirm)
        assertEquals(VisitStatus.CANCELED, vm.uiState.first { it.visit?.status == VisitStatus.CANCELED }.visit?.status)
    }

    @Test
    fun newVisitValidatesThenAddsForPresetCustomer() = runTest(dispatcher) {
        val vm = NewVisitViewModel(NewVisitUseCases(ObserveCustomersUseCase(customers), AddVisitUseCase(visits)))
        val tomorrow = today.plus(DatePeriod(days = 1))
        vm.onAction(NewVisitActions.OnEnter(tomorrow, "C008"))
        assertEquals("윤소희", vm.uiState.first { it.customer != null }.customer?.name)

        vm.onAction(NewVisitActions.OnSubmit)
        assertTrue(vm.uiState.value.showErrors && vm.uiState.value.timeError && vm.uiState.value.placeError)

        vm.onAction(NewVisitActions.OnTimeSelect(14, 30))
        vm.onAction(NewVisitActions.OnPlaceChange("인천 고객 사무실"))
        vm.onAction(NewVisitActions.OnPurposeChange(VisitPurpose.RISK_PROFILE))
        vm.onAction(NewVisitActions.OnSubmit)
        val event = vm.events.first()
        assertIs<NewVisitEvent.Added>(event)
        val saved = visits.observeVisit(event.visitId).first()
        assertEquals(tomorrow.atTimeMillis(14, 30), saved?.scheduledAtMillis)
        assertEquals(VisitPurpose.RISK_PROFILE, saved?.purpose)
    }

    @Test
    fun newVisitFindsCustomerByQuery() = runTest(dispatcher) {
        val vm = NewVisitViewModel(NewVisitUseCases(ObserveCustomersUseCase(customers), AddVisitUseCase(visits)))
        vm.onAction(NewVisitActions.OnEnter(null, null))
        vm.uiState.first { it.customerMatches.isNotEmpty() }
        vm.onAction(NewVisitActions.OnCustomerQueryChange("ㅇㅅㅎ"))
        assertEquals(listOf("오세훈", "윤소희"), vm.uiState.value.customerMatches.map { it.name })
        vm.onAction(NewVisitActions.OnCustomerSelect("C009"))
        assertEquals("오세훈", vm.uiState.value.customer?.name)
    }
}
