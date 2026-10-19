package com.eunilsung.ods.ui.openorder.change

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderChangeRejectReason
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.PriceDirection
import com.eunilsung.ods.domain.util.OrderIssue
import com.eunilsung.ods.ui.openorder.OpenOrdersActions
import com.eunilsung.ods.ui.openorder.OrderChangeState
import com.eunilsung.ods.ui.order.confirm.DialogButton
import com.eunilsung.ods.ui.order.confirm.OrderDialogFrame
import com.eunilsung.ods.ui.order.confirm.ResultBody
import com.eunilsung.ods.ui.order.confirm.SummaryRow
import com.eunilsung.ods.ui.order.panel.label
import com.eunilsung.ods.ui.order.panel.message
import com.eunilsung.ods.ui.order.panel.shares
import com.eunilsung.ods.ui.order.panel.won
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.textfield.NumberField
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.amend_current_price
import ods.composeapp.generated.resources.amend_extra_amount
import ods.composeapp.generated.resources.amend_new_price
import ods.composeapp.generated.resources.amend_original_price
import ods.composeapp.generated.resources.amend_submit
import ods.composeapp.generated.resources.amend_title
import ods.composeapp.generated.resources.amended_body
import ods.composeapp.generated.resources.amended_title
import ods.composeapp.generated.resources.cancel_body
import ods.composeapp.generated.resources.cancel_submit
import ods.composeapp.generated.resources.cancel_title
import ods.composeapp.generated.resources.canceled_body
import ods.composeapp.generated.resources.canceled_title
import ods.composeapp.generated.resources.change_close
import ods.composeapp.generated.resources.change_reject_not_open
import ods.composeapp.generated.resources.change_unreachable_body
import ods.composeapp.generated.resources.order_confirm_side
import ods.composeapp.generated.resources.order_confirm_stock
import ods.composeapp.generated.resources.order_quantity
import ods.composeapp.generated.resources.order_reject_buying_power
import ods.composeapp.generated.resources.order_rejected_title
import ods.composeapp.generated.resources.order_unreachable_title
import ods.composeapp.generated.resources.order_use_current_price
import org.jetbrains.compose.resources.stringResource

/**
 * 정정·취소 창 — 정정 가격 입력이나 취소 확인, 보낸 뒤 결과를 같은 창에 보인다.
 *
 * 정정 가격이 비었거나 원래 가격과 같으면 버튼만 막고 글로 알리지 않는다 — 창을 열자마자 오류가 보이지 않게 한다.
 * 응답을 받지 못했으면 정정·취소가 됐을 수도 있으므로, 목록을 보고 다시 하라고만 알린다.
 */
@Composable
fun OrderChangeDialog(state: OrderChangeState, onAction: (OpenOrdersActions) -> Unit) {
    val busy = (state as? OrderChangeState.Amend)?.isSubmitting == true || (state as? OrderChangeState.Cancel)?.isSubmitting == true
    val dismiss = { onAction(OpenOrdersActions.OnDismiss) }
    OrderDialogFrame(busy = busy, onDismiss = dismiss) {
        when (state) {
            is OrderChangeState.Amend -> AmendBody(state, onAction)
            is OrderChangeState.Cancel -> CancelBody(state, onAction)
            is OrderChangeState.Amended -> ResultBody(
                title = stringResource(Res.string.amended_title),
                body = stringResource(
                    Res.string.amended_body,
                    state.order.orderNo,
                    state.order.originalOrderNo.orEmpty(),
                    won(state.order.price),
                ),
                onDone = dismiss,
            )
            is OrderChangeState.Canceled -> ResultBody(
                title = stringResource(Res.string.canceled_title),
                body = stringResource(
                    Res.string.canceled_body,
                    state.order.orderNo,
                    state.order.name,
                    state.order.quantity.withComma(),
                    state.order.side.label(),
                ),
                onDone = dismiss,
            )
            is OrderChangeState.Rejected -> ResultBody(
                title = stringResource(Res.string.order_rejected_title),
                body = stringResource(
                    when (state.reason) {
                        OrderChangeRejectReason.NOT_OPEN -> Res.string.change_reject_not_open
                        OrderChangeRejectReason.NOT_ENOUGH_BUYING_POWER -> Res.string.order_reject_buying_power
                    }
                ),
                onDone = dismiss,
                titleColor = AppColors.Caution,
            )
            OrderChangeState.Unreachable -> ResultBody(
                title = stringResource(Res.string.order_unreachable_title),
                body = stringResource(Res.string.change_unreachable_body),
                onDone = dismiss,
                titleColor = AppColors.Caution,
            )
        }
    }
}

@Composable
private fun OrderSummary(order: Order) {
    val sideColor = if (order.side == OrderSide.BUY) AppColors.Rise else AppColors.Fall
    SummaryRow(stringResource(Res.string.order_confirm_stock), "${order.name} (${order.code})")
    SummaryRow(stringResource(Res.string.order_confirm_side), order.side.label(), sideColor)
    SummaryRow(stringResource(Res.string.order_quantity), shares(order.quantity))
}

@Composable
private fun AmendBody(state: OrderChangeState.Amend, onAction: (OpenOrdersActions) -> Unit) {
    val order = state.order
    Text(text = stringResource(Res.string.amend_title), style = AppTypography.Title, color = AppColors.Text)
    LineDivider()
    OrderSummary(order)
    SummaryRow(stringResource(Res.string.amend_original_price), won(order.price))
    state.stock?.let { stock ->
        SummaryRow(
            stringResource(Res.string.amend_current_price),
            won(stock.price),
            PriceDirection.of(stock.price, stock.previousClose).color,
        )
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = Dimens.Space4), verticalAlignment = Alignment.CenterVertically) {
        Text(text = stringResource(Res.string.amend_new_price), style = AppTypography.Caption, color = AppColors.TextSub, modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.order_use_current_price),
            style = AppTypography.Caption,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.PrimaryMain,
            modifier = Modifier
                .clickable(cornerRadius = 4.dp, onClick = { onAction(OpenOrdersActions.OnAmendUseCurrentPrice) })
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
    NumberField(
        value = state.priceText,
        onValueChange = { onAction(OpenOrdersActions.OnAmendPriceChange(it)) },
        onStep = { onAction(OpenOrdersActions.OnAmendPriceStep(it)) },
        label = stringResource(Res.string.amend_new_price),
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.extraAmount > 0) {
        SummaryRow(stringResource(Res.string.amend_extra_amount), won(state.extraAmount))
    }
    val issues = state.check.errors.filterNot { it == OrderIssue.PriceRequired || it == OrderIssue.SameAsOrderPrice } + state.check.warnings
    for (issue in issues) {
        Text(text = "! ${issue.message()}", style = AppTypography.Caption, color = AppColors.Caution)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), modifier = Modifier.padding(top = Dimens.Space4)) {
        DialogButton(
            text = stringResource(Res.string.change_close),
            background = AppColors.Gray100Bg,
            textColor = AppColors.Text,
            enabled = !state.isSubmitting,
            weight = 1f,
            onClick = { onAction(OpenOrdersActions.OnDismiss) },
        )
        val canSubmit = state.stock != null && state.check.canSubmit
        DialogButton(
            text = stringResource(Res.string.amend_submit),
            background = if (canSubmit) AppColors.PrimaryMain else AppColors.Gray100Bg,
            textColor = if (canSubmit) AppColors.White else AppColors.TextDisabled,
            enabled = canSubmit && !state.isSubmitting,
            weight = 1.4f,
            loading = state.isSubmitting,
            onClick = { onAction(OpenOrdersActions.OnConfirm) },
        )
    }
}

@Composable
private fun CancelBody(state: OrderChangeState.Cancel, onAction: (OpenOrdersActions) -> Unit) {
    val order = state.order
    Text(text = stringResource(Res.string.cancel_title), style = AppTypography.Title, color = AppColors.Text)
    LineDivider()
    OrderSummary(order)
    SummaryRow(stringResource(Res.string.amend_original_price), won(order.price))
    Text(
        text = stringResource(Res.string.cancel_body, order.quantity.withComma()),
        style = AppTypography.Body,
        color = AppColors.TextSub,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), modifier = Modifier.padding(top = Dimens.Space4)) {
        DialogButton(
            text = stringResource(Res.string.change_close),
            background = AppColors.Gray100Bg,
            textColor = AppColors.Text,
            enabled = !state.isSubmitting,
            weight = 1f,
            onClick = { onAction(OpenOrdersActions.OnDismiss) },
        )
        DialogButton(
            text = stringResource(Res.string.cancel_submit),
            background = AppColors.Caution,
            textColor = AppColors.White,
            enabled = !state.isSubmitting,
            weight = 1.4f,
            loading = state.isSubmitting,
            onClick = { onAction(OpenOrdersActions.OnConfirm) },
        )
    }
}
