package com.eunilsung.ods.ui.account

import com.eunilsung.ods.data.local.repository.LocalConsultSessionRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalIdVerificationRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalSignatureRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.AccountTypeBlock
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.IdMismatch
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePoint
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.model.Terms
import com.eunilsung.ods.domain.usecase.AccountOpeningUseCases
import com.eunilsung.ods.domain.usecase.ObserveAccountsUseCase
import com.eunilsung.ods.domain.usecase.ObserveCustomerUseCase
import com.eunilsung.ods.domain.usecase.OpenAccountUseCase
import com.eunilsung.ods.domain.usecase.StartConsultUseCase
import com.eunilsung.ods.domain.usecase.VerifyIdCardUseCase
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.testsupport.testLocalSecret
import com.eunilsung.ods.testsupport.testSecureSettings
import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
class AccountOpeningViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val database = createTestDatabase()
    private val customers = LocalCustomerRepositoryImpl(database, CustomerMapper(testLocalSecret()))
    private val signatures = LocalSignatureRepositoryImpl(database)
    private val session = LocalConsultSessionRepositoryImpl()
    private val login = MapSettings().let { LocalLoginRepositoryImpl(it, testSecureSettings(it), simulatedRoundTripMillis = 0) }
    private val signature = Signature(listOf(List(20) { SignaturePoint(0.1f + it * 0.03f, 0.5f) }))

    private fun viewModel() = AccountOpeningViewModel(
        AccountOpeningUseCases(
            observeCustomer = ObserveCustomerUseCase(customers),
            observeAccounts = ObserveAccountsUseCase(customers),
            verifyIdCard = VerifyIdCardUseCase(customers, LocalIdVerificationRepositoryImpl(simulatedRoundTripMillis = 0)),
            openAccount = OpenAccountUseCase(customers, signatures, login),
            startConsult = StartConsultUseCase(session),
        )
    )

    @BeforeTest
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        customers.refresh()
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun wrongIdDetailsAreListedAndNotVerified() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(AccountOpeningActions.OnEnter("C005"))
        vm.uiState.first { it.customer != null }
        assertEquals("C005", session.activeCustomerId.value)
        vm.onAction(AccountOpeningActions.OnPhotoCaptured(byteArrayOf(1, 2, 3)))
        vm.onAction(AccountOpeningActions.OnNext)
        vm.onAction(AccountOpeningActions.OnIdNameChange("정민재"))
        vm.onAction(AccountOpeningActions.OnRrnFrontChange("900510"))
        vm.onAction(AccountOpeningActions.OnRrnBackChange("2"))
        vm.onAction(AccountOpeningActions.OnVerify)
        val state = vm.uiState.first { it.idMismatches.isNotEmpty() }
        assertEquals(listOf(IdMismatch.BIRTH, IdMismatch.GENDER), state.idMismatches)
        assertNull(state.verification)
        assertFalse(state.canProceed)
    }

    @Test
    fun opensBrokerageAccountEndToEnd() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(AccountOpeningActions.OnEnter("C005"))
        vm.uiState.first { it.customer != null }
        vm.onAction(AccountOpeningActions.OnUseDemoId)
        vm.onAction(AccountOpeningActions.OnNext)
        vm.onAction(AccountOpeningActions.OnVerify)
        vm.uiState.first { it.verification != null }
        vm.onAction(AccountOpeningActions.OnNext)
        assertEquals(OpeningStep.ACCOUNT_TYPE, vm.uiState.value.step)
        vm.onAction(AccountOpeningActions.OnTypeSelect(AccountType.BROKERAGE))
        vm.onAction(AccountOpeningActions.OnNext)
        vm.onAction(AccountOpeningActions.OnTermToggle(Terms.ACCOUNT))
        assertFalse(vm.uiState.value.canProceed)
        vm.onAction(AccountOpeningActions.OnAgreeAll)
        vm.onAction(AccountOpeningActions.OnNext)
        assertEquals(OpeningStep.PIN, vm.uiState.value.step)

        vm.onAction(AccountOpeningActions.OnPinEntered("1234"))
        assertEquals(PinNotice.WEAK, vm.uiState.value.pinNotice)
        vm.onAction(AccountOpeningActions.OnPinEntered("2580"))
        vm.onAction(AccountOpeningActions.OnPinEntered("2581"))
        assertEquals(PinNotice.MISMATCH, vm.uiState.value.pinNotice)
        vm.onAction(AccountOpeningActions.OnPinEntered("2580"))
        vm.onAction(AccountOpeningActions.OnPinEntered("2580"))
        assertEquals(PinStage.DONE, vm.uiState.value.pinStage)
        vm.onAction(AccountOpeningActions.OnNext)
        vm.onAction(AccountOpeningActions.OnSignatureChange(signature))
        vm.onAction(AccountOpeningActions.OnSubmit("개설 메모"))

        val done = vm.uiState.first { it.step == OpeningStep.DONE }
        val account = assertNotNull(done.opened)
        assertEquals("7001-0005-01", account.accountNo)
        assertEquals(listOf(account), customers.observeAccounts("C005").first())
        assertEquals(ConsultTopic.ACCOUNT_OPENING, customers.observeConsultations("C005").first().first().topic)
        assertEquals(SignaturePurpose.ACCOUNT_OPENING, signatures.observeRecords("C005").first().single().purpose)
    }

    @Test
    fun duplicateIsaIsBlocked() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(AccountOpeningActions.OnEnter("C002"))
        val state = vm.uiState.first { it.customer != null && it.typeBlocks.isNotEmpty() }
        assertEquals(AccountTypeBlock.ISA_DUPLICATE, state.typeBlocks[AccountType.ISA])
        assertTrue(AccountType.BROKERAGE !in state.typeBlocks)
    }
}
