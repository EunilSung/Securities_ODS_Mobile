package com.eunilsung.ods.ui.balance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.eunilsung.ods.domain.model.BalanceSummary
import com.eunilsung.ods.domain.model.PriceDirection
import com.eunilsung.ods.ui.balance.item.HoldingItem
import com.eunilsung.ods.ui.balance.item.signedWon
import com.eunilsung.ods.ui.main.LocalIsTablet
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.order.panel.won
import com.eunilsung.ods.ui.stockdetail.StockDetailScreen
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.util.toSignedRate
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.balance_cash
import ods.composeapp.generated.resources.balance_evaluation
import ods.composeapp.generated.resources.balance_hide_amounts
import ods.composeapp.generated.resources.balance_holdings
import ods.composeapp.generated.resources.balance_holdings_empty
import ods.composeapp.generated.resources.balance_profit
import ods.composeapp.generated.resources.balance_purchase
import ods.composeapp.generated.resources.balance_show_amounts
import ods.composeapp.generated.resources.balance_total_asset
import ods.composeapp.generated.resources.order_buying_power
import ods.composeapp.generated.resources.tab_balance
import ods.composeapp.generated.resources.visibility_icon
import ods.composeapp.generated.resources.visibility_off_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 잔고 탭 — 위에 총자산·평가손익 요약, 아래에 보유 종목. 종목을 누르면 현재가 상세를 연다. */
object BalanceScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: BalanceViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current
        val isTablet = LocalIsTablet.current
        val openedCode = (rightNavigator.lastItem as? StockDetailScreen)?.code

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is BalanceEvent.OpenStockDetail -> {
                        val next = StockDetailScreen(event.code)
                        when (val top = rightNavigator.lastItem) {
                            is StockDetailScreen -> if (top.code != event.code) rightNavigator.replace(next)
                            else -> rightNavigator.push(next)
                        }
                    }
                }
            }
        }

        BalanceContent(
            uiState = uiState,
            selectedCode = if (isTablet) openedCode else null,
            onAction = viewModel::onAction,
        )
    }
}

/**
 * 잔고 화면 전체 — 상태 없이 값과 콜백만 받는다.
 *
 * @param selectedCode 태블릿에서 오른쪽에 열린 종목. 폰에서는 null.
 */
@Composable
fun BalanceContent(
    uiState: BalanceUiState,
    selectedCode: String?,
    onAction: (BalanceActions) -> Unit,
) {
    val hide = uiState.hideAmounts
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg)) {
        TopBar(
            title = stringResource(Res.string.tab_balance),
            actions = {
                Box(
                    modifier = Modifier.size(40.dp).clickable(cornerRadius = 20.dp, onClick = { onAction(BalanceActions.OnToggleHideAmounts) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (hide) Res.drawable.visibility_off_icon else Res.drawable.visibility_icon),
                        contentDescription = stringResource(if (hide) Res.string.balance_show_amounts else Res.string.balance_hide_amounts),
                        tint = AppColors.TextSub,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
        )
        LineDivider()
        if (!uiState.isLoaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "summary") { SummarySection(uiState.summary, hide) }
            item(key = "gap") { LineDivider(thickness = Dimens.SectionGap, color = AppColors.Gray100Bg) }
            item(key = "holdingsHeader") {
                Text(
                    text = "${stringResource(Res.string.balance_holdings)} ${uiState.holdings.size}",
                    style = AppTypography.Section,
                    color = AppColors.Text,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space12),
                )
                LineDivider()
            }
            if (uiState.holdings.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(Res.string.balance_holdings_empty), style = AppTypography.Body, color = AppColors.TextSub)
                    }
                }
            }
            items(uiState.holdings, key = { it.code }) { holding ->
                HoldingItem(
                    holding = holding,
                    hideAmounts = hide,
                    isSelected = holding.code == selectedCode,
                    onClick = { onAction(BalanceActions.OnHoldingClick(holding.code)) },
                )
                LineDivider(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))
            }
        }
    }
}

/** 요약 — 큰 총자산과 평가손익·수익률, 그 아래 예수금·주문가능·매입금액·평가금액. */
@Composable
private fun SummarySection(summary: BalanceSummary, hide: Boolean) {
    val profitColor = PriceDirection.of(summary.profit, 0).color
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space16),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space4),
    ) {
        Text(text = stringResource(Res.string.balance_total_asset), style = AppTypography.Caption, color = AppColors.TextSub)
        Text(text = masked(won(summary.totalAsset), hide), style = AppTypography.PriceHero, color = AppColors.Text)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${stringResource(Res.string.balance_profit)} ",
                style = AppTypography.Change,
                color = AppColors.TextSub,
            )
            Text(
                text = "${masked(signedWon(summary.profit), hide)} (${summary.profitRate.toSignedRate()}%)",
                style = AppTypography.Change,
                fontWeight = FontWeight.SemiBold,
                color = profitColor,
            )
        }
        LineDivider(modifier = Modifier.padding(vertical = Dimens.Space8))
        SummaryRow(stringResource(Res.string.balance_cash), masked(won(summary.cash), hide))
        SummaryRow(stringResource(Res.string.order_buying_power), masked(won(summary.buyingPower), hide))
        SummaryRow(stringResource(Res.string.balance_purchase), masked(won(summary.purchaseAmount), hide))
        SummaryRow(stringResource(Res.string.balance_evaluation), masked(won(summary.evaluationAmount), hide))
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = AppTypography.Body, color = AppColors.TextSub, modifier = Modifier.weight(1f))
        Text(text = value, style = AppTypography.BodyNumber, color = AppColors.Text)
    }
}
