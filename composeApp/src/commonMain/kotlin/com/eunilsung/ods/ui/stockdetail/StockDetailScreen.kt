package com.eunilsung.ods.ui.stockdetail

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.ui.main.LocalOrderPaneVisible
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.quote.QuoteActions
import com.eunilsung.ods.ui.order.rememberOrderLauncher
import com.eunilsung.ods.ui.quote.QuoteContent
import com.eunilsung.ods.ui.quote.QuoteOrderButtons
import com.eunilsung.ods.ui.quote.QuoteViewModel
import org.koin.compose.viewmodel.koinViewModel

/** 종목 상세 — 현재가 화면을 오른쪽 스택에 띄운다. 폰에서는 목록 위로 밀려 들어오고, 태블릿에서는 오른쪽 칸에 뜬다. */
data class StockDetailScreen(val code: String) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: QuoteViewModel = koinViewModel(key = "quote-$code")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current
        val orderLauncher = rememberOrderLauncher()
        val orderPaneVisible = LocalOrderPaneVisible.current

        LaunchedEffect(code) {
            viewModel.onAction(QuoteActions.OnEnter(code))
        }

        QuoteContent(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { rightNavigator.popToEmptyRoot() },
            bottomBar = if (orderPaneVisible) null else {
                { QuoteOrderButtons(onSideClick = { orderLauncher.open(code, it) }) }
            },
            onOrderBookPriceClick = { orderLauncher.pickPrice(code, it) },
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}
