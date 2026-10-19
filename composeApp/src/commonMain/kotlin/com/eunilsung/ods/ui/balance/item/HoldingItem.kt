package com.eunilsung.ods.ui.balance.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Holding
import com.eunilsung.ods.domain.model.PriceDirection
import com.eunilsung.ods.ui.balance.masked
import com.eunilsung.ods.ui.order.panel.shares
import com.eunilsung.ods.ui.order.panel.won
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.theme.LocalListDensity
import com.eunilsung.ods.ui.theme.tokens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.stock.tickFlash
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.util.toSignedRate
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.holding_average
import ods.composeapp.generated.resources.open_order_current
import ods.composeapp.generated.resources.order_sellable
import org.jetbrains.compose.resources.stringResource

/**
 * 보유 종목 한 줄 — 왼쪽 종목명·수량·평균단가·현재가, 오른쪽 평가금액·평가손익·수익률.
 *
 * 손익 색은 국내 관례대로 이익 빨강, 손실 파랑. 미체결 매도가 있으면 매도가능 수량을 함께 보인다.
 *
 * @param hideAmounts 금액·수량을 가린다. 수익률·현재가는 그대로 둔다.
 * @param isSelected 태블릿에서 오른쪽에 열린 종목이면 배경을 칠한다.
 */
@Composable
fun HoldingItem(
    holding: Holding,
    hideAmounts: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalListDensity.current.tokens
    val profitColor = PriceDirection.of(holding.profit, 0).color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) AppColors.PrimaryBg else AppColors.Transparent)
            .tickFlash(holding.price)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = tokens.rowVerticalPadding),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = holding.name, style = tokens.nameStyle, color = AppColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "${masked(shares(holding.quantity), hideAmounts)} · ${stringResource(Res.string.holding_average, masked(won(holding.averagePrice), hideAmounts))}",
                style = tokens.subStyle,
                color = AppColors.TextSub,
            )
            Text(
                text = "${stringResource(Res.string.open_order_current, holding.price.withComma())} (${holding.dayChangeRate.toSignedRate()}%)",
                style = tokens.subStyle,
                color = holding.dayDirection.color,
            )
            if (holding.openSellQuantity > 0) {
                Text(
                    text = "${stringResource(Res.string.order_sellable)} ${masked(shares(holding.sellableQuantity), hideAmounts)}",
                    style = tokens.subStyle,
                    color = AppColors.TextSub,
                )
            }
        }
        Spacer(Modifier.width(Dimens.Space8))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = masked(won(holding.evaluationAmount), hideAmounts), style = tokens.priceStyle, color = AppColors.Text)
            Text(
                text = masked(signedWon(holding.profit), hideAmounts),
                style = AppTypography.Change.copy(fontSize = tokens.subStyle.fontSize, lineHeight = tokens.subStyle.lineHeight),
                color = profitColor,
            )
            Text(
                text = "${holding.profitRate.toSignedRate()}%",
                style = AppTypography.Change,
                color = profitColor,
            )
        }
    }
}

/** 부호 붙은 금액 — "+175,000원", "-12,300원". */
@Composable
fun signedWon(amount: Long): String = when {
    amount > 0 -> "+${won(amount)}"
    amount < 0 -> "-${won(-amount)}"
    else -> won(0)
}
