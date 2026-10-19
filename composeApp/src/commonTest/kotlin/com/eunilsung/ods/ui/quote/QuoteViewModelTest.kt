package com.eunilsung.ods.ui.quote

import com.eunilsung.ods.data.local.repository.LocalQuoteRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalStockRepositoryImpl
import com.eunilsung.ods.data.local.repository.LocalTickSimulator
import com.eunilsung.ods.data.mapper.StockMapper
import com.eunilsung.ods.domain.repository.AppVersionProvider
import com.eunilsung.ods.domain.usecase.GetAppInfoUseCase
import com.eunilsung.ods.domain.usecase.ObserveCandlesUseCase
import com.eunilsung.ods.domain.usecase.ObserveOrderBookUseCase
import com.eunilsung.ods.domain.usecase.ObserveStockUseCase
import com.eunilsung.ods.domain.usecase.ObserveWatchlistUseCase
import com.eunilsung.ods.domain.usecase.QuoteUseCases
import com.eunilsung.ods.testsupport.createTestDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
class QuoteViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val database = createTestDatabase()
    private val stocks = LocalStockRepositoryImpl(database, StockMapper())

    private object FakeVersion : AppVersionProvider {
        override fun versionCode() = 1L
        override fun versionName() = "1.0"
    }

    @BeforeTest
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        stocks.fetchWatchlist()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun presentationStepsThroughWatchlist() = runTest(dispatcher) {
        val quotes = LocalQuoteRepositoryImpl(stocks, LocalTickSimulator(database))
        val vm = QuoteViewModel(
            QuoteUseCases(
                observeStock = ObserveStockUseCase(stocks),
                observeOrderBook = ObserveOrderBookUseCase(quotes),
                observeCandles = ObserveCandlesUseCase(quotes),
                observeWatchlist = ObserveWatchlistUseCase(stocks),
                getAppInfo = GetAppInfoUseCase(FakeVersion),
            )
        )
        backgroundScope.launch { vm.uiState.collect {} }
        vm.onAction(QuoteActions.OnEnter("005930"))
        vm.onAction(QuoteActions.OnPresentClick)
        val first = vm.uiState.first { it.isPresenting && it.nextName != null }
        assertNull(first.previousName)
        assertEquals("SK하이닉스", first.nextName)
        assertTrue(first.usesDemoData)

        vm.onAction(QuoteActions.OnPresentStep(forward = true))
        val second = vm.uiState.first { it.code == "000660" && it.previousName != null }
        assertEquals("삼성전자", second.previousName)

        vm.onAction(QuoteActions.OnPresentDismiss)
        assertFalse(vm.uiState.first { !it.isPresenting }.isPresenting)
    }
}
