package com.eunilsung.ods.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.eunilsung.ods.Config
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.ui.openorder.OpenOrdersContent
import com.eunilsung.ods.ui.openorder.OpenOrdersViewModel
import com.eunilsung.ods.ui.order.confirm.OrderConfirmDialog
import com.eunilsung.ods.ui.orderauth.OrderAuthDialog
import com.eunilsung.ods.ui.order.panel.OrderPanel
import com.eunilsung.ods.ui.quote.orderbook.OrderBookView
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.stock.StockPickerRow
import com.eunilsung.ods.ui.uikit.stock.tickFlash
import com.eunilsung.ods.ui.uikit.tab.UnderlineTabs
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import com.eunilsung.ods.ui.util.changeLabel
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.order_section_open
import ods.composeapp.generated.resources.order_section_order
import ods.composeapp.generated.resources.tab_order
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 주문 탭 — 위쪽에서 [주문 | 미체결] 을 고른다.
 *
 * 주문은 마지막으로 본 종목을 열고 종목 줄에서 관심종목을 바꿔 주문한다. 미체결은 정정·취소를 한다.
 * 어느 쪽을 보고 있는지는 화면 안에만 두는 값이라 `rememberSaveable` 로 들고 있는다.
 */
object OrderScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: OrderViewModel = koinViewModel(key = "order-tab")
        val uiState by viewModel.uiState.collectAsState()
        val openOrdersViewModel: OpenOrdersViewModel = koinViewModel()
        val openOrdersState by openOrdersViewModel.uiState.collectAsState()
        var section by rememberSaveable { mutableStateOf(OrderSection.ORDER) }

        LaunchedEffect(Unit) {
            viewModel.onAction(OrderActions.OnEnterTab)
        }

        Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg)) {
            TopBar(title = stringResource(Res.string.tab_order))
            val openCount = openOrdersState.orders.size
            UnderlineTabs(
                titles = listOf(
                    stringResource(Res.string.order_section_order),
                    stringResource(Res.string.order_section_open) + if (openCount > 0) " $openCount" else "",
                ),
                selectedIndex = section.ordinal,
                onSelect = { section = OrderSection.entries[it] },
            )
            LineDivider()
            when (section) {
                OrderSection.ORDER -> OrderContent(uiState = uiState, onAction = viewModel::onAction, onBack = null, showTopBar = false)
                OrderSection.OPEN -> OpenOrdersContent(uiState = openOrdersState, onAction = openOrdersViewModel::onAction)
            }
        }
    }
}

/** 주문 탭 안의 구역. */
private enum class OrderSection { ORDER, OPEN }

/**
 * 주문 화면 전체 — 상태 없이 값과 콜백만 받는다. 주문 탭·주문 화면·주문 칸이 함께 쓴다.
 *
 * 호가가 있으면 폭이 [Config.Layout.ORDER_SPLIT_MIN_WIDTH_DP] 이상일 때 왼쪽 호가 · 오른쪽 패널, 좁으면 패널 아래 호가로 놓는다.
 * 호가 행을 누르면 그 가격이 패널에 들어간다.
 *
 * @param onBack 주면 상단에 뒤로 버튼을 그린다.
 * @param showTopBar 주문 탭처럼 바깥에서 제목줄을 그리면 false.
 */
@Composable
fun OrderContent(
    uiState: OrderUiState,
    onAction: (OrderActions) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
) {
    Column(modifier = modifier.fillMaxSize().background(AppColors.Bg)) {
        if (showTopBar) TopBar(title = stringResource(Res.string.tab_order), onBack = onBack)
        if (uiState.watchlist.isNotEmpty()) {
            StockPickerRow(
                stocks = uiState.watchlist,
                selectedCode = uiState.code,
                onSelect = { onAction(OrderActions.OnSelectStock(it)) },
            )
        }
        LineDivider()
        val stock = uiState.stock
        if (stock == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        StockSummaryRow(stock)
        LineDivider()

        val orderBook = uiState.orderBook
        val selectedPrice = uiState.priceText.toLongOrNull()?.takeIf { uiState.type == OrderType.LIMIT }
        val onPriceClick: (Long) -> Unit = { onAction(OrderActions.OnPriceSelected(stock.code, it)) }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                orderBook == null -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    OrderPanel(uiState = uiState, onAction = onAction)
                }
                maxWidth >= Config.Layout.ORDER_SPLIT_MIN_WIDTH_DP.dp -> Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        OrderBookView(
                            orderBook = orderBook,
                            currentPrice = stock.price,
                            basePrice = stock.previousClose,
                            compact = true,
                            selectedPrice = selectedPrice,
                            onPriceClick = onPriceClick,
                        )
                    }
                    LineDivider(modifier = Modifier.fillMaxHeight(), isWidth = true)
                    Column(Modifier.weight(1.1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        OrderPanel(uiState = uiState, onAction = onAction)
                    }
                }
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    OrderPanel(uiState = uiState, onAction = onAction)
                    LineDivider(thickness = Dimens.SectionGap, color = AppColors.Gray100Bg)
                    OrderBookView(
                        orderBook = orderBook,
                        currentPrice = stock.price,
                        basePrice = stock.previousClose,
                        compact = true,
                        selectedPrice = selectedPrice,
                        onPriceClick = onPriceClick,
                    )
                }
            }
        }
    }

    uiState.confirm?.let { confirm ->
        if ((confirm as? OrderConfirmState.Review)?.awaitingAuth == true) {
            OrderAuthDialog(
                onAuthorized = { onAction(OrderActions.OnAuthorized) },
                onCancel = { onAction(OrderActions.OnAuthCanceled) },
            )
        } else {
            OrderConfirmDialog(
                state = confirm,
                onConfirm = { onAction(OrderActions.OnConfirm) },
                onDismiss = { onAction(OrderActions.OnDismissConfirm) },
            )
        }
    }
}

/** 종목 한 줄 — 이름·코드와 현재가·전일 대비. 시세가 바뀌면 현재가가 깜빡인다. */
@Composable
private fun StockSummaryRow(stock: Stock) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stock.name, style = AppTypography.ListName, color = AppColors.Text, maxLines = 1)
            Text(text = "${stock.code} · ${stock.market.name}", style = AppTypography.Caption, color = AppColors.TextSub)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stock.price.withComma(),
                style = AppTypography.ListPrice,
                color = stock.direction.color,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).tickFlash(stock.price),
            )
            Text(text = stock.changeLabel(), style = AppTypography.Caption, color = stock.direction.color)
        }
    }
}
