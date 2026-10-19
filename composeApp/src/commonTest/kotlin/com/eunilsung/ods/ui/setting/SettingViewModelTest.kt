package com.eunilsung.ods.ui.setting

import com.eunilsung.ods.data.local.repository.LocalLoginRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalUserPreferenceRepositoryImpl
import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.domain.model.LoginRequest
import com.eunilsung.ods.domain.repository.AppVersionProvider
import com.eunilsung.ods.domain.usecase.GetAppInfoUseCase
import com.eunilsung.ods.domain.usecase.LogoutUseCase
import com.eunilsung.ods.domain.usecase.ObserveListDensityUseCase
import com.eunilsung.ods.domain.usecase.ObserveMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.ObserveStaffUseCase
import com.eunilsung.ods.domain.usecase.SetListDensityUseCase
import com.eunilsung.ods.domain.usecase.SetMaskPersonalInfoUseCase
import com.eunilsung.ods.domain.usecase.SettingUseCases
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private object FakeVersion : AppVersionProvider {
        override fun versionCode() = 7L
        override fun versionName() = "1.2.3"
    }

    private val preferences = LocalUserPreferenceRepositoryImpl(MapSettings())
    private val login = LocalLoginRepositoryImpl(MapSettings(), simulatedRoundTripMillis = 0)

    private fun viewModel() = SettingViewModel(
        SettingUseCases(
            observeListDensity = ObserveListDensityUseCase(preferences),
            setListDensity = SetListDensityUseCase(preferences),
            observeMaskPersonalInfo = ObserveMaskPersonalInfoUseCase(preferences),
            setMaskPersonalInfo = SetMaskPersonalInfoUseCase(preferences),
            getAppInfo = GetAppInfoUseCase(FakeVersion),
            observeStaff = ObserveStaffUseCase(login),
            logout = LogoutUseCase(login),
        )
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun personalInfoIsMaskedByDefault() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.maskPersonalInfo)
    }

    @Test
    fun displaySettingsAndAppInfoFlowThrough() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(SettingActions.OnListDensityChange(ListDensity.COMPACT))
        vm.onAction(SettingActions.OnMaskPersonalInfoChange(false))
        advanceUntilIdle()
        val state = vm.uiState.first()
        assertEquals(ListDensity.COMPACT, state.listDensity)
        assertFalse(state.maskPersonalInfo)
        assertEquals("1.2.3", state.appInfo.versionName)
        assertTrue(state.appInfo.usesDemoData)
    }

    @Test
    fun logoutClearsStaff() = runTest(dispatcher) {
        login.login(LoginRequest("test1", "1234"), nowEpochSeconds = 0)
        val vm = viewModel()
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals("test1", vm.uiState.value.staff?.id)
        vm.onAction(SettingActions.OnLogoutClick)
        advanceUntilIdle()
        assertNull(vm.uiState.value.staff)
    }
}
