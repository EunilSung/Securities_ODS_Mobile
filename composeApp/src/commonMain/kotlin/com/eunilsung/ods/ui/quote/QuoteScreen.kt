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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.Config
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.ui.quote.chart.CandleChart
import com.eunilsung.ods.ui.quote.orderbook.OrderBookView
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
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

/**
 * 현재가 화면 전체 — 상태 없이 값과 콜백만 받는다. 시장 탭의 종목 상세가 쓴다.
 *
 * 폭이 [Config.Layout.QUOTE_SIDE_BY_SIDE_MIN_WIDTH_DP] 이상이면 차트와 호가를 좌우로, 아니면 위아래로 놓는다.
 *
 * @param onBack 주면 상단에 뒤로 버튼을 그린다(종목 상세).
 */
@Composable
fun QuoteContent(
    uiState: QuoteUiState,
    onAction: (QuoteActions) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val stock = uiState.stock
    Column(modifier = modifier.fillMaxSize().background(AppColors.Bg)) {
        TopBar(title = stock?.name ?: stringResource(Res.string.tab_quote), onBack = onBack)
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
                        OrderBookSection(uiState, stock)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    QuoteHeader(stock = stock, today = uiState.today)
                    SectionGap()
                    ChartSection(uiState, stock, chartHeight = 240.dp, onAction = onAction)
                    SectionGap()
                    OrderBookSection(uiState, stock)
                }
            }
        }
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
private fun OrderBookSection(uiState: QuoteUiState, stock: Stock) {
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
