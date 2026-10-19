package com.eunilsung.ods.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.eunilsung.ods.Config
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.ui.main.LocalOrderPaneVisible
import com.eunilsung.ods.ui.order.rememberOrderLauncher
import com.eunilsung.ods.ui.quote.chart.CandleChart
import com.eunilsung.ods.ui.quote.orderbook.OrderBookView
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import com.eunilsung.ods.ui.uikit.stock.StockPickerRow
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.chart_period_day
import ods.composeapp.generated.resources.chart_period_minute
import ods.composeapp.generated.resources.chart_period_month
import ods.composeapp.generated.resources.chart_period_week
import ods.composeapp.generated.resources.quote_chart
import ods.composeapp.generated.resources.quote_order_book
import ods.composeapp.generated.resources.tab_quote
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 현재가 탭 — 마지막으로 본 종목을 열고, 위쪽 종목 줄에서 관심종목을 바꿔 볼 수 있다. */
object QuoteScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: QuoteViewModel = koinViewModel(key = "quote-tab")
        val uiState by viewModel.uiState.collectAsState()

        val orderLauncher = rememberOrderLauncher()
        val orderPaneVisible = LocalOrderPaneVisible.current

        LaunchedEffect(Unit) {
            viewModel.onAction(QuoteActions.OnEnterTab)
        }

        QuoteContent(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = null,
            bottomBar = if (orderPaneVisible) null else {
                { QuoteOrderButtons(onSideClick = { orderLauncher.open(uiState.code, it) }) }
            },
            onOrderBookPriceClick = { orderLauncher.pickPrice(uiState.code, it) },
        )
    }
}

/**
 * 현재가 화면 전체 — 상태 없이 값과 콜백만 받는다. 현재가 탭과 종목 상세가 함께 쓴다.
 *
 * 폭이 [Config.Layout.QUOTE_SIDE_BY_SIDE_MIN_WIDTH_DP] 이상이면 차트와 호가를 좌우로, 아니면 위아래로 놓는다.
 *
 * @param onBack 주면 상단에 뒤로 버튼을 그린다(종목 상세).
 * @param bottomBar 화면 아래에 붙는 줄(매수·매도 버튼). 태블릿 가로처럼 주문 칸이 따로 있으면 null.
 * @param onOrderBookPriceClick 호가 행을 눌렀을 때 — 그 가격으로 주문한다.
 */
@Composable
fun QuoteContent(
    uiState: QuoteUiState,
    onAction: (QuoteActions) -> Unit,
    onBack: (() -> Unit)?,
    bottomBar: (@Composable () -> Unit)? = null,
    onOrderBookPriceClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val stock = uiState.stock
    Column(modifier = modifier.fillMaxSize().background(AppColors.Bg)) {
        TopBar(title = stock?.name ?: stringResource(Res.string.tab_quote), onBack = onBack)
        if (uiState.watchlist.isNotEmpty()) {
            StockPickerRow(
                stocks = uiState.watchlist,
                selectedCode = uiState.code,
                onSelect = { onAction(QuoteActions.OnSelectStock(it)) },
            )
        }
        LineDivider()
        if (stock == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= Config.Layout.QUOTE_SIDE_BY_SIDE_MIN_WIDTH_DP.dp) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1.4f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        QuoteHeader(stock = stock, today = uiState.today)
                        SectionGap()
                        ChartSection(uiState, stock, chartHeight = 320.dp, onAction = onAction)
                    }
                    LineDivider(modifier = Modifier.fillMaxHeight(), isWidth = true)
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        OrderBookSection(uiState, stock, onOrderBookPriceClick)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    QuoteHeader(stock = stock, today = uiState.today)
                    SectionGap()
                    ChartSection(uiState, stock, chartHeight = 240.dp, onAction = onAction)
                    SectionGap()
                    OrderBookSection(uiState, stock, onOrderBookPriceClick)
                }
            }
        }
        bottomBar?.invoke()
    }
}

@Composable
private fun ChartSection(uiState: QuoteUiState, stock: Stock, chartHeight: Dp, onAction: (QuoteActions) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space12)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = stringResource(Res.string.quote_chart), style = AppTypography.Section, color = AppColors.Text, modifier = Modifier.weight(1f))
            SegmentedControl(
                options = ChartPeriod.entries,
                selected = uiState.period,
                label = { it.label() },
                onSelect = { onAction(QuoteActions.OnPeriodChange(it)) },
            )
        }
        CandleChart(
            candles = uiState.candles,
            period = uiState.period,
            currentPrice = stock.price,
            basePrice = stock.previousClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .padding(start = Dimens.ScreenPadding, end = Dimens.Space8, top = Dimens.Space12),
        )
    }
}

@Composable
private fun OrderBookSection(uiState: QuoteUiState, stock: Stock, onPriceClick: ((Long) -> Unit)?) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space12)) {
        Text(
            text = stringResource(Res.string.quote_order_book),
            style = AppTypography.Section,
            color = AppColors.Text,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.Space8),
        )
        val orderBook = uiState.orderBook
        if (orderBook == null) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LineDivider()
            OrderBookView(
                orderBook = orderBook,
                currentPrice = stock.price,
                basePrice = stock.previousClose,
                onPriceClick = onPriceClick,
            )
        }
    }
}

@Composable
private fun SectionGap() {
    LineDivider(thickness = Dimens.SectionGap, color = AppColors.Gray100Bg)
}

@Composable
private fun ChartPeriod.label(): String = stringResource(
    when (this) {
        ChartPeriod.MINUTE -> Res.string.chart_period_minute
        ChartPeriod.DAY -> Res.string.chart_period_day
        ChartPeriod.WEEK -> Res.string.chart_period_week
        ChartPeriod.MONTH -> Res.string.chart_period_month
    }
)
