package com.eunilsung.ods.ui.order

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popOrClose
import org.koin.compose.viewmodel.koinViewModel

/**
 * 현재가에서 매수·매도(또는 호가)를 눌러 연 주문 화면. 오른쪽 스택에 쌓이고, 뒤로 가면 현재가로 돌아간다.
 *
 * ViewModel 은 하나를 돌려 쓰고 열 때마다 입력을 비운다 — 열 때마다 새로 만들면 닫은 주문 화면의 ViewModel 이 쌓인다.
 *
 * @param price 호가를 눌러 열었으면 그 가격. 없으면 현재가로 시작한다.
 */
data class OrderTicketScreen(
    val code: String,
    val side: OrderSide,
    val price: Long?,
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: OrderViewModel = koinViewModel(key = "order-ticket")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current

        LaunchedEffect(key) {
            viewModel.onAction(OrderActions.OnEnter(code, side, price))
        }

        OrderContent(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { rightNavigator.popOrClose() },
            modifier = Modifier.navigationBarsPadding().imePadding(),
        )
    }
}
