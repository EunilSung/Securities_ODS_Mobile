package com.eunilsung.ods.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.stockdetail.StockDetailScreen
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.order_pane_empty
import ods.composeapp.generated.resources.tab_order
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 태블릿 가로의 오른쪽 끝 주문 칸 — 가운데 칸에 열린 종목을 따라간다. 호가는 가운데 칸에 있으므로 패널만 그린다.
 */
@Composable
fun OrderPane() {
    val viewModel: OrderViewModel = koinViewModel(key = ORDER_PANE_VIEW_MODEL_KEY)
    val uiState by viewModel.uiState.collectAsState()
    val openedCode = when (val top = LocalRightNavigator.current.lastItem) {
        is StockDetailScreen -> top.code
        is OrderTicketScreen -> top.code
        else -> null
    }

    LaunchedEffect(openedCode) {
        viewModel.onAction(OrderActions.OnPaneCodeChange(openedCode))
    }

    if (openedCode == null) {
        Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg)) {
            TopBar(title = stringResource(Res.string.tab_order))
            LineDivider()
            Box(Modifier.fillMaxSize().padding(Dimens.Space24), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.order_pane_empty),
                    style = AppTypography.Body,
                    color = AppColors.TextSub,
                    textAlign = TextAlign.Center,
                )
            }
        }
    } else {
        OrderContent(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = null,
            modifier = Modifier.navigationBarsPadding().imePadding(),
        )
    }
}
