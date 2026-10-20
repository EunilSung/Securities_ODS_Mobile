package com.eunilsung.ods.ui.risk

import com.eunilsung.ods.data.local.repository.LocalCustomerRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalSignatureRepositoryImpl
import com.eunilsung.ods.data.mapper.CustomerMapper
import com.eunilsung.ods.data.remote.server.ServerException
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.RiskProfile
import com.eunilsung.ods.domain.model.RiskQuestion
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePoint
import com.eunilsung.ods.domain.model.SignaturePurpose
import com.eunilsung.ods.domain.model.SignatureRecord
import com.eunilsung.ods.domain.repository.CustomerRepository
import com.eunilsung.ods.domain.repository.SignatureRepository
import com.eunilsung.ods.domain.usecase.ObserveCustomerUseCase
import com.eunilsung.ods.domain.usecase.RiskAssessmentUseCases
import com.eunilsung.ods.domain.usecase.SaveRiskAssessmentUseCase
import com.eunilsung.ods.domain.util.RiskAssessmentPolicy
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
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RiskAssessmentViewModelTest {
    /** 서버에 닿지 못해 투자성향을 바꾸지 못하는 저장소. */
    private class OfflineCustomers(delegate: CustomerRepository) : CustomerRepository by delegate {
        override suspend fun saveRiskProfile(customerId: String, profile: RiskProfile, assessedAtMillis: Long): Unit =
            throw ServerException(status = null, code = null)
    }

    /** 투자성향은 바뀌었지만 서명 기록에서 서버에 닿지 못하는 저장소. */
    private class OfflineSignatures(delegate: SignatureRepository) : SignatureRepository by delegate {
        override suspend fun record(customerId: String, purpose: SignaturePurpose, signature: Signature, nowMillis: Long): SignatureRecord =
            throw ServerException(status = null, code = null)
    }

    private val dispatcher = StandardTestDispatcher()
    private val database = createTestDatabase()
    private val customers = LocalCustomerRepositoryImpl(database, CustomerMapper(testLocalSecret()))
    private val signatures = LocalSignatureRepositoryImpl(database)
    private val login = MapSettings().let { LocalLoginRepositoryImpl(it, testSecureSettings(it), simulatedRoundTripMillis = 0) }
    private val signature = Signature(listOf(List(20) { SignaturePoint(0.1f + it * 0.03f, 0.4f + (it % 3) * 0.05f) }))

    private fun viewModel(
        savingCustomers: CustomerRepository = customers,
        savingSignatures: SignatureRepository = signatures,
    ) = RiskAssessmentViewModel(
        RiskAssessmentUseCases(
            observeCustomer = ObserveCustomerUseCase(customers),
            save = SaveRiskAssessmentUseCase(savingCustomers, savingSignatures, login),
        )
    )

    private suspend fun RiskAssessmentViewModel.answerAll(pick: (RiskQuestion) -> Int) {
        uiState.first { it.customerName.isNotEmpty() }
        RiskQuestion.entries.forEach { question ->
            if (question != RiskQuestion.AGE) onAction(RiskAssessmentActions.OnAnswer(question, pick(question)))
            onAction(RiskAssessmentActions.OnNext)
        }
    }

    @BeforeTest
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        customers.refresh()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun agePresetFromBirthDateAndNextNeedsAnAnswer() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(RiskAssessmentActions.OnEnter("C009"))
        val state = vm.uiState.first { it.customerName.isNotEmpty() }
        assertEquals(RiskAssessmentPolicy.ageOption(78), state.answers[RiskQuestion.AGE])
        vm.onAction(RiskAssessmentActions.OnNext)
        assertEquals(1, vm.uiState.value.stepIndex)
        vm.onAction(RiskAssessmentActions.OnNext)
        assertEquals(1, vm.uiState.value.stepIndex)
    }

    @Test
    fun elderlyCustomerResultIsFlagged() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(RiskAssessmentActions.OnEnter("C009"))
        vm.answerAll { 0 }
        val result = assertNotNull(vm.uiState.value.result)
        assertTrue(result.isElderly)
        assertTrue(vm.uiState.value.isResult)
    }

    @Test
    fun savingNeedsConfirmationAndSignatureThenUpdatesCustomer() = runTest(dispatcher) {
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        val vm = viewModel()
        vm.onAction(RiskAssessmentActions.OnEnter("C005"))
        vm.answerAll { q -> q.scores.indexOf(q.scores.max()) }
        assertFalse(vm.uiState.value.canSave)
        vm.onAction(RiskAssessmentActions.OnConfirmChange(true))
        assertFalse(vm.uiState.value.canSave)
        vm.onAction(RiskAssessmentActions.OnSignatureChange(signature))
        assertTrue(vm.uiState.value.canSave)

        vm.onAction(RiskAssessmentActions.OnSave("진단 메모"))
        assertEquals(RiskAssessmentEvent.Saved, vm.events.first())
        val customer = customers.observeCustomer("C005").first()
        assertEquals(vm.uiState.value.result?.profile, customer?.riskProfile)
        assertNotNull(customer?.riskAssessedAtMillis)
        val note = customers.observeConsultations("C005").first().first()
        assertEquals(ConsultTopic.RISK_PROFILE, note.topic)
        assertEquals("진단 메모", note.content)
        assertEquals(SignaturePurpose.RISK_PROFILE, signatures.observeRecords("C005").first().single().purpose)
    }

    @Test
    fun notSignedInKeepsProfileUnchanged() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(RiskAssessmentActions.OnEnter("C001"))
        vm.answerAll { 0 }
        vm.onAction(RiskAssessmentActions.OnConfirmChange(true))
        vm.onAction(RiskAssessmentActions.OnSignatureChange(signature))
        vm.onAction(RiskAssessmentActions.OnSave("메모"))
        assertEquals(RiskSaveError.NOT_SIGNED_IN, vm.uiState.first { it.error != null }.error)
        assertEquals(RiskProfile.NEUTRAL, customers.observeCustomer("C001").first()?.riskProfile)
        assertTrue(signatures.observeRecords("C001").first().isEmpty())
    }

    @Test
    fun serverFailureKeepsAnswersSoStaffCanRetry() = runTest(dispatcher) {
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        val vm = viewModel(savingCustomers = OfflineCustomers(customers))
        vm.onAction(RiskAssessmentActions.OnEnter("C001"))
        vm.answerAll { 0 }
        vm.onAction(RiskAssessmentActions.OnConfirmChange(true))
        vm.onAction(RiskAssessmentActions.OnSignatureChange(signature))
        vm.onAction(RiskAssessmentActions.OnSave("메모"))

        val state = vm.uiState.first { it.error != null }
        assertEquals(RiskSaveError.UNREACHABLE, state.error)
        assertFalse(state.isSaving)
        assertTrue(state.canSave)
        assertEquals(RiskProfile.NEUTRAL, customers.observeCustomer("C001").first()?.riskProfile)
        assertTrue(signatures.observeRecords("C001").first().isEmpty())
        assertTrue(customers.observeConsultations("C001").first().none { it.topic == ConsultTopic.RISK_PROFILE && it.content == "메모" })
    }

    @Test
    fun signatureRecordFailingAfterProfileSavedStillReportsSaved() = runTest(dispatcher) {
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        val vm = viewModel(savingSignatures = OfflineSignatures(signatures))
        vm.onAction(RiskAssessmentActions.OnEnter("C005"))
        vm.answerAll { q -> q.scores.indexOf(q.scores.max()) }
        vm.onAction(RiskAssessmentActions.OnConfirmChange(true))
        vm.onAction(RiskAssessmentActions.OnSignatureChange(signature))
        vm.onAction(RiskAssessmentActions.OnSave("진단 메모"))

        assertEquals(RiskAssessmentEvent.Saved, vm.events.first())
        assertEquals(vm.uiState.value.result?.profile, customers.observeCustomer("C005").first()?.riskProfile)
        assertEquals("진단 메모", customers.observeConsultations("C005").first().first().content)
    }
}
