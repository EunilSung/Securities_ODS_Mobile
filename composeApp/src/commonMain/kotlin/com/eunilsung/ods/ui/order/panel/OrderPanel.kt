package com.eunilsung.ods.ui.order.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.util.OrderIssue
import com.eunilsung.ods.ui.order.OrderActions
import com.eunilsung.ods.ui.order.OrderUiState
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import com.eunilsung.ods.ui.uikit.textfield.NumberField
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.order_amount
import ods.composeapp.generated.resources.order_amount_market_note
import ods.composeapp.generated.resources.order_buying_power
import ods.composeapp.generated.resources.order_max_quantity
import ods.composeapp.generated.resources.order_price
import ods.composeapp.generated.resources.order_quantity
import ods.composeapp.generated.resources.order_ratio_max
import ods.composeapp.generated.resources.order_sellable
import ods.composeapp.generated.resources.order_submit_buy
import ods.composeapp.generated.resources.order_submit_sell
import ods.composeapp.generated.resources.order_type_market
import ods.composeapp.generated.resources.order_use_current_price
import org.jetbrains.compose.resources.stringResource

/**
 * 매수·매도 패널 — 구분, 지정가·시장가, 가격, 수량(비율 버튼), 주문가능·주문금액, 검사 결과, 주문 버튼.
 *
 * 매수는 상승색, 매도는 하락색으로 칠한다. 수량·가격을 아직 넣지 않았다는 오류는 버튼을 막기만 하고 글로 보이지 않는다.
 */
@Composable
fun OrderPanel(
    uiState: OrderUiState,
    onAction: (OrderActions) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stock = uiState.stock ?: return
    val sideColor = if (uiState.side == OrderSide.BUY) AppColors.Rise else AppColors.Fall
    val isMarket = uiState.type == OrderType.MARKET

    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.Space12),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space8),
    ) {
        SideTabs(selected = uiState.side, onSelect = { onAction(OrderActions.OnSideChange(it)) })
        SegmentedControl(
            options = OrderType.entries,
            selected = uiState.type,
            label = { it.label() },
            onSelect = { onAction(OrderActions.OnTypeChange(it)) },
        )

        FieldLabel(
            text = stringResource(Res.string.order_price),
            trailing = stringResource(Res.string.order_use_current_price),
            onTrailingClick = { onAction(OrderActions.OnUseCurrentPrice) },
        )
        NumberField(
            value = uiState.priceText,
            onValueChange = { onAction(OrderActions.OnPriceChange(it)) },
            onStep = { onAction(OrderActions.OnPriceStep(it)) },
            label = stringResource(Res.string.order_price),
            disabledText = if (isMarket) stringResource(Res.string.order_type_market) else null,
            modifier = Modifier.fillMaxWidth(),
        )

        FieldLabel(
            text = stringResource(Res.string.order_quantity),
            trailing = if (uiState.side == OrderSide.SELL) {
                "${stringResource(Res.string.order_sellable)} ${shares(uiState.sellableQuantity)}"
            } else {
                "${stringResource(Res.string.order_max_quantity)} ${shares(uiState.maxQuantity)}"
            },
        )
        NumberField(
            value = uiState.quantityText,
            onValueChange = { onAction(OrderActions.OnQuantityChange(it)) },
            onStep = { onAction(OrderActions.OnQuantityStep(it)) },
            label = stringResource(Res.string.order_quantity),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space4)) {
            for (percent in QUANTITY_PERCENTS) {
                RatioChip(
                    text = if (percent == 100) stringResource(Res.string.order_ratio_max) else "$percent%",
                    onClick = { onAction(OrderActions.OnQuantityPercent(percent)) },
                )
            }
        }

        LineDivider(modifier = Modifier.padding(vertical = Dimens.Space4))
        if (uiState.side == OrderSide.BUY) {
            InfoRow(stringResource(Res.string.order_buying_power), won(uiState.buyingPower))
        }
        InfoRow(
            label = stringResource(Res.string.order_amount),
            value = won(uiState.orderAmount),
            note = if (isMarket && uiState.side == OrderSide.BUY) stringResource(Res.string.order_amount_market_note) else null,
            emphasized = true,
        )

        val visibleIssues = uiState.check.errors.filterNot { it.isMissingInput() } + uiState.check.warnings
        for (issue in visibleIssues) {
            Text(text = "! ${issue.message()}", style = AppTypography.Caption, color = AppColors.Caution)
        }

        SubmitButton(
            text = submitLabel(uiState.side, uiState.quantity),
            color = sideColor,
            enabled = uiState.check.canSubmit,
            onClick = { onAction(OrderActions.OnSubmitClick) },
        )
        Text(
            text = "${stock.name} · ${stock.code}",
            style = AppTypography.Caption,
            color = AppColors.TextSub,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val QUANTITY_PERCENTS = listOf(10, 25, 50, 100)

private fun OrderIssue.isMissingInput(): Boolean = this == OrderIssue.QuantityRequired || this == OrderIssue.PriceRequired

@Composable
private fun submitLabel(side: OrderSide, quantity: Long): String = when {
    quantity <= 0 -> side.label()
    side == OrderSide.BUY -> stringResource(Res.string.order_submit_buy, quantity.withComma())
    else -> stringResource(Res.string.order_submit_sell, quantity.withComma())
}

/** 매수·매도 고르기. 고른 쪽을 그 색으로 칠한다. */
@Composable
private fun SideTabs(selected: OrderSide, onSelect: (OrderSide) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(modifier = Modifier.fillMaxWidth().height(40.dp).clip(shape).background(AppColors.Gray100Bg)) {
        for (side in OrderSide.entries) {
            val isSelected = side == selected
            val color = if (side == OrderSide.BUY) AppColors.Rise else AppColors.Fall
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(shape)
                    .background(if (isSelected) color else Color.Transparent)
                    .clickable(onClick = { onSelect(side) })
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = side.label(),
                    style = AppTypography.Section,
                    color = if (isSelected) AppColors.White else AppColors.TextSub,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String, trailing: String, onTrailingClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = Dimens.Space4), verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = AppTypography.Caption, color = AppColors.TextSub, modifier = Modifier.weight(1f))
        Text(
            text = trailing,
            style = AppTypography.CaptionNumber,
            color = if (onTrailingClick != null) AppColors.PrimaryMain else AppColors.TextSub,
            fontWeight = if (onTrailingClick != null) FontWeight.SemiBold else FontWeight.Normal,
            modifier = if (onTrailingClick != null) {
                Modifier.clickable(cornerRadius = 4.dp, onClick = onTrailingClick).padding(horizontal = 4.dp, vertical = 2.dp)
            } else {
                Modifier
            },
        )
    }
}

@Composable
private fun RowScope.RatioChip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .clip(shape)
            .border(1.dp, AppColors.Line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AppTypography.Caption, color = AppColors.Text)
    }
}

@Composable
private fun InfoRow(label: String, value: String, note: String? = null, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = AppTypography.Caption, color = AppColors.TextSub)
            if (note != null) Text(text = note, style = AppTypography.ChartLabel, color = AppColors.TextSub)
        }
        Text(
            text = value,
            style = if (emphasized) AppTypography.BodyNumber.copy(fontWeight = FontWeight.Bold) else AppTypography.CaptionNumber,
            color = AppColors.Text,
        )
    }
}

@Composable
private fun SubmitButton(text: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .padding(top = Dimens.Space4)
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(if (enabled) color else AppColors.Gray100Bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.Section,
            fontWeight = FontWeight.Bold,
            color = if (enabled) AppColors.White else AppColors.TextDisabled,
        )
    }
}
