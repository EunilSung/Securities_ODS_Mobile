package com.eunilsung.ods.ui.openorder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eunilsung.ods.ui.openorder.change.OrderChangeDialog
import com.eunilsung.ods.ui.openorder.item.OpenOrderItem
import com.eunilsung.ods.ui.order.panel.won
import com.eunilsung.ods.ui.orderauth.OrderAuthDialog
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.open_orders_count
import ods.composeapp.generated.resources.open_orders_empty
import ods.composeapp.generated.resources.order_buying_power
import org.jetbrains.compose.resources.stringResource

/**
 * 미체결 목록 — 위에 건수와 주문가능 금액, 아래에 주문마다 정정·취소. 상태 없이 값과 콜백만 받는다.
 */
@Composable
fun OpenOrdersContent(
    uiState: OpenOrdersUiState,
    onAction: (OpenOrdersActions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.open_orders_count, uiState.orders.size.toString()),
                style = AppTypography.Caption,
                color = AppColors.TextSub,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${stringResource(Res.string.order_buying_power)} ${won(uiState.buyingPower)}",
                style = AppTypography.CaptionNumber,
                color = AppColors.TextSub,
            )
        }
        LineDivider()
        when {
            !uiState.isLoaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(Res.string.open_orders_empty), style = AppTypography.Body, color = AppColors.TextSub)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.orders, key = { it.orderNo }) { order ->
                    OpenOrderItem(
                        order = order,
                        stock = uiState.stocks[order.code],
                        onAmend = { onAction(OpenOrdersActions.OnAmendClick(order)) },
                        onCancel = { onAction(OpenOrdersActions.OnCancelClick(order)) },
                    )
                    LineDivider(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))
                }
            }
        }
    }

    uiState.change?.let { change ->
        val awaitingAuth = (change as? OrderChangeState.Amend)?.awaitingAuth == true ||
            (change as? OrderChangeState.Cancel)?.awaitingAuth == true
        if (awaitingAuth) {
            OrderAuthDialog(
                onAuthorized = { onAction(OpenOrdersActions.OnAuthorized) },
                onCancel = { onAction(OpenOrdersActions.OnAuthCanceled) },
            )
        } else {
            OrderChangeDialog(state = change, onAction = onAction)
        }
    }
}
