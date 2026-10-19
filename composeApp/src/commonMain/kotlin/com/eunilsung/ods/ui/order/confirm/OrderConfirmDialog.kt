package com.eunilsung.ods.ui.order.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.ui.order.OrderConfirmState
import com.eunilsung.ods.ui.order.panel.label
import com.eunilsung.ods.ui.order.panel.message
import com.eunilsung.ods.ui.order.panel.shares
import com.eunilsung.ods.ui.order.panel.won
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.order_accepted_body
import ods.composeapp.generated.resources.order_accepted_title
import ods.composeapp.generated.resources.order_amount
import ods.composeapp.generated.resources.order_amount_market_note
import ods.composeapp.generated.resources.order_confirm_cancel
import ods.composeapp.generated.resources.order_confirm_ok_buy
import ods.composeapp.generated.resources.order_confirm_ok_sell
import ods.composeapp.generated.resources.order_confirm_side
import ods.composeapp.generated.resources.order_confirm_stock
import ods.composeapp.generated.resources.order_confirm_title_buy
import ods.composeapp.generated.resources.order_confirm_title_sell
import ods.composeapp.generated.resources.order_confirm_type
import ods.composeapp.generated.resources.order_price
import ods.composeapp.generated.resources.order_quantity
import ods.composeapp.generated.resources.order_rejected_title
import ods.composeapp.generated.resources.order_type_market
import ods.composeapp.generated.resources.order_unreachable_body
import ods.composeapp.generated.resources.order_unreachable_title
import org.jetbrains.compose.resources.stringResource

/**
 * 주문 확인 화면 — 종목·구분·유형·가격·수량·금액과 경고를 한 번 더 보이고, 누르면 보낸다. 보낸 뒤에는 결과를 같은 창에 보인다.
 *
 * 보내는 중에는 닫히지 않는다. 인증(비밀번호·생체) 단계가 생기면 확인과 보내기 사이에 넣는다.
 */
@Composable
fun OrderConfirmDialog(
    state: OrderConfirmState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val submitting = (state as? OrderConfirmState.Review)?.isSubmitting == true
    OrderDialogFrame(busy = submitting, onDismiss = onDismiss) {
        when (state) {
            is OrderConfirmState.Review -> ReviewBody(state, onConfirm, onDismiss)
            is OrderConfirmState.Accepted -> ResultBody(
                title = stringResource(Res.string.order_accepted_title),
                body = stringResource(
                    Res.string.order_accepted_body,
                    state.order.orderNo,
                    state.order.name,
                    state.order.quantity.withComma(),
                    state.order.side.label(),
                ),
                onDone = onDismiss,
            )
            is OrderConfirmState.Rejected -> ResultBody(
                title = stringResource(Res.string.order_rejected_title),
                body = state.reason.message(),
                onDone = onDismiss,
                titleColor = AppColors.Caution,
            )
            OrderConfirmState.Unreachable -> ResultBody(
                title = stringResource(Res.string.order_unreachable_title),
                body = stringResource(Res.string.order_unreachable_body),
                onDone = onDismiss,
                titleColor = AppColors.Caution,
            )
        }
    }
}

@Composable
private fun ReviewBody(review: OrderConfirmState.Review, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val draft = review.draft
    val isBuy = draft.side == OrderSide.BUY
    val sideColor = if (isBuy) AppColors.Rise else AppColors.Fall
    Text(
        text = stringResource(if (isBuy) Res.string.order_confirm_title_buy else Res.string.order_confirm_title_sell),
        style = AppTypography.Title,
        color = sideColor,
    )
    LineDivider()
    SummaryRow(stringResource(Res.string.order_confirm_stock), "${review.stockName} (${draft.code})")
    SummaryRow(stringResource(Res.string.order_confirm_side), draft.side.label(), sideColor)
    SummaryRow(stringResource(Res.string.order_confirm_type), draft.type.label())
    SummaryRow(
        stringResource(Res.string.order_price),
        if (draft.type == OrderType.MARKET) stringResource(Res.string.order_type_market) else won(draft.price),
    )
    SummaryRow(stringResource(Res.string.order_quantity), shares(draft.quantity))
    LineDivider()
    SummaryRow(
        label = stringResource(Res.string.order_amount) +
            if (draft.type == OrderType.MARKET && isBuy) " (${stringResource(Res.string.order_amount_market_note)})" else "",
        value = won(review.amount),
        emphasized = true,
    )
    for (warning in review.warnings) {
        Text(text = "! ${warning.message()}", style = AppTypography.Caption, color = AppColors.Caution)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), modifier = Modifier.padding(top = Dimens.Space4)) {
        DialogButton(
            text = stringResource(Res.string.order_confirm_cancel),
            background = AppColors.Gray100Bg,
            textColor = AppColors.Text,
            enabled = !review.isSubmitting,
            weight = 1f,
            onClick = onCancel,
        )
        DialogButton(
            text = stringResource(if (isBuy) Res.string.order_confirm_ok_buy else Res.string.order_confirm_ok_sell),
            background = sideColor,
            textColor = AppColors.White,
            enabled = !review.isSubmitting,
            weight = 1.4f,
            loading = review.isSubmitting,
            onClick = onConfirm,
        )
    }
}
