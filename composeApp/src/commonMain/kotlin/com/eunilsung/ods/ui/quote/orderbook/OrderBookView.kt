package com.eunilsung.ods.ui.quote.orderbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eunilsung.ods.domain.model.OrderBook
import com.eunilsung.ods.domain.model.OrderBookLevel
import com.eunilsung.ods.domain.model.PriceDirection
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.LocalListDensity
import com.eunilsung.ods.ui.theme.tokens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.util.toSignedRate
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.order_book_total
import org.jetbrains.compose.resources.stringResource

/**
 * 호가창 — 위는 매도 호가(먼 것부터), 아래는 매수 호가(가까운 것부터). 가운데 열이 가격이다.
 *
 * 가격 색은 기준가격(전일 종가) 대비로 정한다. 잔량 막대는 보이는 단계 중 가장 큰 잔량 대비 비율이고,
 * 현재가 행은 글자색 테두리, 주문 가격 행은 브랜드색 테두리로 표시한다. 행 높이는 목록 밀도를 따른다.
 *
 * @param compact 주문 화면처럼 좁은 칸에 넣을 때. 등락률을 빼고 글자를 줄인다.
 * @param selectedPrice 주문 패널에 들어간 가격. 그 행을 표시한다.
 * @param onPriceClick 주면 행을 눌러 그 가격을 고를 수 있다.
 */
@Composable
fun OrderBookView(
    orderBook: OrderBook,
    currentPrice: Long,
    basePrice: Long,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    selectedPrice: Long? = null,
    onPriceClick: ((Long) -> Unit)? = null,
) {
    val rowHeight = LocalListDensity.current.tokens.orderBookRowHeight
    val maxQuantity = (orderBook.asks + orderBook.bids).maxOfOrNull { it.quantity }?.coerceAtLeast(1) ?: 1
    val askBar = AppColors.Fall.copy(alpha = 0.16f)
    val bidBar = AppColors.Rise.copy(alpha = 0.16f)
    val style = if (compact) AppTypography.OrderBook.copy(fontSize = 12.sp) else AppTypography.OrderBook
    val cell = RowCellStyle(style, compact, selectedPrice, onPriceClick)

    Column(modifier = modifier.fillMaxWidth()) {
        for (level in orderBook.asks.asReversed()) {
            OrderBookRow(level, isAsk = true, isCurrent = false, basePrice, maxQuantity, rowHeight, askBar, AppColors.FallBg, cell)
            LineDivider()
        }
        for (level in orderBook.bids) {
            OrderBookRow(level, isAsk = false, isCurrent = level.price == currentPrice, basePrice, maxQuantity, rowHeight, bidBar, AppColors.RiseBg, cell)
            LineDivider()
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(rowHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = orderBook.totalAskQuantity.withComma(),
                style = style,
                color = AppColors.Fall,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(end = if (compact) 4.dp else 8.dp),
            )
            Text(
                text = stringResource(Res.string.order_book_total),
                style = AppTypography.Caption,
                color = AppColors.TextSub,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(PRICE_COLUMN_WEIGHT),
            )
            Text(
                text = orderBook.totalBidQuantity.withComma(),
                style = style,
                color = AppColors.Rise,
                modifier = Modifier.weight(1f).padding(start = if (compact) 4.dp else 8.dp),
            )
        }
    }
}

private const val PRICE_COLUMN_WEIGHT = 1.15f

/** 행 그리기 설정 — 칸마다 넘기지 않게 묶었다. */
private class RowCellStyle(
    val textStyle: TextStyle,
    val compact: Boolean,
    val selectedPrice: Long?,
    val onPriceClick: ((Long) -> Unit)?,
)

@Composable
private fun OrderBookRow(
    level: OrderBookLevel,
    isAsk: Boolean,
    isCurrent: Boolean,
    basePrice: Long,
    maxQuantity: Long,
    height: Dp,
    barColor: Color,
    priceBackground: Color,
    cell: RowCellStyle,
) {
    val ratio = level.quantity.toFloat() / maxQuantity
    val onClick = cell.onPriceClick
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .then(if (onClick != null) Modifier.clickable(rippleColor = null, onClick = { onClick(level.price) }) else Modifier),
    ) {
        QuantityCell(quantity = level.quantity.takeIf { isAsk }, ratio = ratio, barColor = barColor, alignEnd = true, cell = cell)
        PriceCell(level.price, basePrice, isCurrent, isSelected = level.price == cell.selectedPrice, priceBackground, cell)
        QuantityCell(quantity = level.quantity.takeIf { !isAsk }, ratio = ratio, barColor = barColor, alignEnd = false, cell = cell)
    }
}

/** 잔량 칸. 매도 잔량은 왼쪽 칸에 오른쪽 정렬, 매수 잔량은 오른쪽 칸에 왼쪽 정렬로 가격 쪽에 붙인다. */
@Composable
private fun RowScope.QuantityCell(quantity: Long?, ratio: Float, barColor: Color, alignEnd: Boolean, cell: RowCellStyle) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .drawBehind {
                if (quantity == null) return@drawBehind
                val inset = 4.dp.toPx()
                val barWidth = size.width * ratio
                val left = if (alignEnd) size.width - barWidth else 0f
                drawRect(barColor, Offset(left, inset), Size(barWidth, size.height - inset * 2))
            }
            .padding(horizontal = if (cell.compact) 4.dp else 8.dp),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if (quantity != null) {
            Text(text = quantity.withComma(), style = cell.textStyle, color = AppColors.TextSub, maxLines = 1)
        }
    }
}

@Composable
private fun RowScope.PriceCell(
    price: Long,
    basePrice: Long,
    isCurrent: Boolean,
    isSelected: Boolean,
    background: Color,
    cell: RowCellStyle,
) {
    val direction = PriceDirection.of(price, basePrice)
    val rate = if (basePrice == 0L) 0.0 else (price - basePrice) * 100.0 / basePrice
    val outline = when {
        isSelected -> Modifier.border(2.dp, AppColors.PrimaryMain)
        isCurrent -> Modifier.border(1.5.dp, AppColors.Text)
        else -> Modifier
    }
    Row(
        modifier = Modifier
            .weight(PRICE_COLUMN_WEIGHT)
            .fillMaxHeight()
            .background(background)
            .then(outline),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = price.withComma(), style = cell.textStyle, color = direction.color, maxLines = 1)
        if (!cell.compact) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${rate.toSignedRate()}%",
                style = AppTypography.ChartLabel.copy(fontSize = 10.sp),
                color = direction.color.copy(alpha = 0.8f),
            )
        }
    }
}
