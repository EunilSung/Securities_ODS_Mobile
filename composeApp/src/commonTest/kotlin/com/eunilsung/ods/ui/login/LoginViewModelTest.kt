package com.eunilsung.ods.ui.login

import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.seed.DemoAccounts
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.model.LoginResult
import com.eunilsung.ods.domain.repository.AppVersionProvider
import com.eunilsung.ods.domain.repository.LoginRepository
import com.eunilsung.ods.domain.usecase.GetAppInfoUseCase
import com.eunilsung.ods.domain.usecase.GetSavedIdUseCase
import com.eunilsung.ods.domain.usecase.IsSaveIdUseCase
import com.eunilsung.ods.domain.usecase.LoginUseCase
import com.eunilsung.ods.domain.usecase.LoginUseCases
import com.eunilsung.ods.domain.usecase.ObserveStaffUseCase
import com.eunilsung.ods.domain.usecase.SetSaveIdUseCase
import com.eunilsung.ods.domain.util.LoginPolicy
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private object FakeVersion : AppVersionProvider {
        override fun versionCode() = 1L
        override fun versionName() = "1.0.0"
    }

    private class FailingRepository(private val delegate: LoginRepository) : LoginRepository by delegate {
        override suspend fun login(request: LoginRequest, nowEpochSeconds: Long): LoginResult = error("offline")
    }

    private val settings = MapSettings()
    private val login = LocalLoginRepositoryImpl(settings, simulatedRoundTripMillis = 0)

    private fun viewModel(repository: LoginRepository = login) = LoginViewModel(
        LoginUseCases(
            login = LoginUseCase(repository),
            observeStaff = ObserveStaffUseCase(repository),
            getSavedId = GetSavedIdUseCase(repository),
            isSaveId = IsSaveIdUseCase(repository),
            setSaveId = SetSaveIdUseCase(repository),
            getAppInfo = GetAppInfoUseCase(FakeVersion),
        ),
    )

    private fun LoginViewModel.enter(id: String, password: String) {
        onAction(LoginActions.OnIdChange(id))
        onAction(LoginActions.OnPasswordChange(password))
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun cannotSubmitUntilBothFieldsAreFilled() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.onAction(LoginActions.OnIdChange("test1"))
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.onAction(LoginActions.OnPasswordChange("1"))
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun wrongPasswordShowsRemainingTriesAndClearsPassword() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.enter("test1", "0000")
        viewModel.onAction(LoginActions.OnLoginClick)
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.password)
        assertEquals(LoginError.Wrong(LoginPolicy.MAX_ATTEMPTS - 1), state.error)

        viewModel.onAction(LoginActions.OnPasswordChange("1"))
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun fifthWrongTryLocks() = runTest(dispatcher) {
        val viewModel = viewModel()
        repeat(LoginPolicy.MAX_ATTEMPTS) {
            viewModel.enter("test1", "0000")
            viewModel.onAction(LoginActions.OnLoginClick)
            advanceUntilIdle()
        }
        assertTrue(viewModel.uiState.value.error is LoginError.Locked)
    }

    @Test
    fun successLogsInAndLogoutBringsBackSavedId() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.enter("test2", DemoAccounts.PASSWORD)
        viewModel.onAction(LoginActions.OnLoginClick)
        advanceUntilIdle()
        assertEquals("test2", login.currentStaff.value?.id)
        assertNull(viewModel.uiState.value.error)

        viewModel.onAction(LoginActions.OnTogglePasswordVisible)
        login.logout()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("test2", state.id)
        assertEquals("", state.password)
        assertFalse(state.isPasswordVisible)
    }

    @Test
    fun turningOffSaveIdStartsEmptyNextTime() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onAction(LoginActions.OnSaveIdChange(false))
        viewModel.enter("test3", DemoAccounts.PASSWORD)
        viewModel.onAction(LoginActions.OnLoginClick)
        advanceUntilIdle()

        val next = viewModel()
        assertEquals("", next.uiState.value.id)
        assertFalse(next.uiState.value.saveId)
    }

    @Test
    fun failureToReachServerKeepsPassword() = runTest(dispatcher) {
        val viewModel = viewModel(FailingRepository(login))
        viewModel.enter("test1", DemoAccounts.PASSWORD)
        viewModel.onAction(LoginActions.OnLoginClick)
        advanceUntilIdle()

        assertEquals(LoginError.Unreachable, viewModel.uiState.value.error)
        assertEquals(DemoAccounts.PASSWORD, viewModel.uiState.value.password)
    }
}
