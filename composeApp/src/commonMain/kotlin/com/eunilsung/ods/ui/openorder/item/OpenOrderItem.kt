package com.eunilsung.ods.ui.openorder.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Order
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.domain.model.OrderType
import com.eunilsung.ods.domain.model.PriceDirection
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.ui.order.panel.label
import com.eunilsung.ods.ui.order.panel.shares
import com.eunilsung.ods.ui.order.panel.won
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.theme.LocalListDensity
import com.eunilsung.ods.ui.theme.tokens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.ui.util.formatOrderTime
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.open_order_amend
import ods.composeapp.generated.resources.open_order_cancel
import ods.composeapp.generated.resources.open_order_current
import ods.composeapp.generated.resources.open_order_no
import ods.composeapp.generated.resources.open_order_original_no
import org.jetbrains.compose.resources.stringResource

/**
 * 미체결 주문 한 건 — 구분·종목·수량, 주문 가격과 지금 현재가, 주문번호(정정이면 원주문번호)·시각, 정정·취소 버튼.
 *
 * 시장가 주문은 정정할 수 없어 정정 버튼을 막는다. 크기는 목록 밀도를 따른다.
 *
 * @param stock 종목 시세. 아직 없으면 현재가를 비운다.
 */
@Composable
fun OpenOrderItem(
    order: Order,
    stock: Stock?,
    onAmend: () -> Unit,
    onCancel: () -> Unit,
) {
    val tokens = LocalListDensity.current.tokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = tokens.rowVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SideBadge(order.side)
            Spacer(Modifier.width(Dimens.Space8))
            Text(
                text = order.name,
                style = tokens.nameStyle,
                color = AppColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = shares(order.quantity), style = tokens.priceStyle, color = AppColors.Text)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${order.type.label()} ${if (order.type == OrderType.MARKET) "" else won(order.price)}".trim(),
                style = AppTypography.BodyNumber,
                color = AppColors.Text,
            )
            if (stock != null) {
                Text(
                    text = "  ·  ${stringResource(Res.string.open_order_current, stock.price.withComma())}",
                    style = AppTypography.CaptionNumber,
                    color = PriceDirection.of(stock.price, stock.previousClose).color,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(text = formatOrderTime(order.acceptedAtEpochSeconds), style = AppTypography.CaptionNumber, color = AppColors.TextSub)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val numbers = buildString {
                append(stringResource(Res.string.open_order_no, order.orderNo))
                order.originalOrderNo?.let { append("  ·  ").append(stringResource(Res.string.open_order_original_no, it)) }
            }
            Text(text = numbers, style = AppTypography.CaptionNumber, color = AppColors.TextSub, modifier = Modifier.weight(1f))
            SmallButton(stringResource(Res.string.open_order_amend), enabled = order.type == OrderType.LIMIT, onClick = onAmend)
            Spacer(Modifier.width(Dimens.Space8))
            SmallButton(stringResource(Res.string.open_order_cancel), enabled = true, onClick = onCancel)
        }
    }
}

/** 매수·매도 표시. 매수는 상승색, 매도는 하락색. */
@Composable
private fun SideBadge(side: OrderSide) {
    val color = if (side == OrderSide.BUY) AppColors.Rise else AppColors.Fall
    val background = if (side == OrderSide.BUY) AppColors.RiseBg else AppColors.FallBg
    Text(
        text = side.label(),
        style = AppTypography.Caption,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun SmallButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text,
        style = AppTypography.Caption,
        fontWeight = FontWeight.SemiBold,
        color = if (enabled) AppColors.Text else AppColors.TextDisabled,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, AppColors.Line, shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Dimens.Space12, vertical = 6.dp),
    )
}
