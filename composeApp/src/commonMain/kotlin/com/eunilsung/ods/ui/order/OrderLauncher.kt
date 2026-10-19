package com.eunilsung.ods.ui.order

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.ui.main.LocalOrderPaneVisible
import com.eunilsung.ods.ui.main.LocalRightNavigator
import org.koin.compose.viewmodel.koinViewModel

/** 넓은 태블릿 주문 칸의 ViewModel 키. 주문 칸과 현재가 화면이 같은 것을 받는다. */
const val ORDER_PANE_VIEW_MODEL_KEY = "order-pane"

/**
 * 현재가에서 주문으로 넘어가는 길. 주문 칸이 떠 있으면(태블릿 가로) 거기에 넣고, 아니면 주문 화면을 오른쪽 스택에 연다.
 */
class OrderLauncher internal constructor(
    private val rightNavigator: Navigator,
    private val pane: OrderViewModel?,
) {
    /** 매수·매도 버튼. */
    fun open(code: String, side: OrderSide) {
        if (pane != null) pane.onAction(OrderActions.OnSideChange(side))
        else rightNavigator.push(OrderTicketScreen(code = code, side = side, price = null))
    }

    /** 호가 행을 눌렀다. 주문 칸이 있으면 가격만 바꾸고, 없으면 그 가격으로 매수 주문 화면을 연다. */
    fun pickPrice(code: String, price: Long) {
        if (pane != null) pane.onAction(OrderActions.OnPriceSelected(code, price))
        else rightNavigator.push(OrderTicketScreen(code = code, side = OrderSide.BUY, price = price))
    }
}

@Composable
fun rememberOrderLauncher(): OrderLauncher {
    val rightNavigator = LocalRightNavigator.current
    val pane: OrderViewModel? = if (LocalOrderPaneVisible.current) koinViewModel(key = ORDER_PANE_VIEW_MODEL_KEY) else null
    return remember(rightNavigator, pane) { OrderLauncher(rightNavigator, pane) }
}
