package com.eunilsung.ods.ui.watchlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.eunilsung.ods.domain.model.ListDensity
import com.eunilsung.ods.ui.main.LocalIsTablet
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.stockdetail.StockDetailScreen
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.theme.LocalListDensity
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import com.eunilsung.ods.ui.watchlist.item.WatchlistItem
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.density_comfortable
import ods.composeapp.generated.resources.density_compact
import ods.composeapp.generated.resources.refresh
import ods.composeapp.generated.resources.refresh_icon
import ods.composeapp.generated.resources.tab_watchlist
import ods.composeapp.generated.resources.watchlist_count
import ods.composeapp.generated.resources.watchlist_empty
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

object WatchlistScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: WatchlistViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current
        val isTablet = LocalIsTablet.current
        val openedCode = (rightNavigator.lastItem as? StockDetailScreen)?.code

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is WatchlistEvent.OpenStockDetail -> {
                        val next = StockDetailScreen(event.code)
                        when (val top = rightNavigator.lastItem) {
                            is StockDetailScreen -> if (top.code != event.code) rightNavigator.replace(next)
                            else -> rightNavigator.push(next)
                        }
                    }
                }
            }
        }

        WatchlistContent(
            uiState = uiState,
            selectedCode = if (isTablet) openedCode else null,
            onAction = viewModel::onAction,
        )
    }
}

/**
 * 관심종목 화면 전체 — 상태 없이 값과 콜백만 받는다.
 *
 * @param selectedCode 태블릿에서 오른쪽에 열린 종목. 폰에서는 null.
 */
@Composable
fun WatchlistContent(
    uiState: WatchlistUiState,
    selectedCode: String?,
    onAction: (WatchlistActions) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = stringResource(Res.string.tab_watchlist),
            actions = {
                Box(
                    modifier = Modifier.size(40.dp).clickable(cornerRadius = 20.dp, onClick = { onAction(WatchlistActions.OnRefresh) }),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AppColors.TextSub)
                    } else {
                        Icon(
                            painter = painterResource(Res.drawable.refresh_icon),
                            contentDescription = stringResource(Res.string.refresh),
                            tint = AppColors.TextSub,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            },
        )
        LineDivider()
        ListHeader(
            count = uiState.stocks.size,
            density = LocalListDensity.current,
            onDensityChange = { onAction(WatchlistActions.OnListDensityChange(it)) },
        )
        LineDivider()
        when {
            uiState.stocks.isEmpty() && uiState.isRefreshing ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            uiState.stocks.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(Res.string.watchlist_empty), color = AppColors.TextSub)
                }

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.stocks, key = { it.code }) { stock ->
                    WatchlistItem(
                        stock = stock,
                        isSelected = stock.code == selectedCode,
                        onClick = { onAction(WatchlistActions.OnStockClick(stock.code)) },
                    )
                    LineDivider(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))
                }
            }
        }
    }
}

/** 목록 위 한 줄 — 종목 수와 목록 밀도 전환. */
@Composable
private fun ListHeader(count: Int, density: ListDensity, onDensityChange: (ListDensity) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.watchlist_count, count),
            style = AppTypography.Caption,
            color = AppColors.TextSub,
            modifier = Modifier.weight(1f),
        )
        SegmentedControl(
            options = ListDensity.entries,
            selected = density,
            label = {
                stringResource(
                    when (it) {
                        ListDensity.COMFORTABLE -> Res.string.density_comfortable
                        ListDensity.COMPACT -> Res.string.density_compact
                    }
                )
            },
            onSelect = onDensityChange,
        )
    }
}
