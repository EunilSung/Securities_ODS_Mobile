package com.eunilsung.ods.ui.watchlist.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.theme.LocalListDensity
import com.eunilsung.ods.ui.theme.tokens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.stock.tickFlash
import com.eunilsung.ods.ui.util.changeLabel
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.util.withComma

/**
 * 관심종목 한 줄 — 왼쪽 종목명·코드, 오른쪽 현재가·전일 대비. 시세가 바뀌면 행이 잠깐 깜빡인다.
 *
 * 크기는 목록 밀도(`LocalListDensity`)를 따른다.
 *
 * @param isSelected 태블릿에서 오른쪽에 열린 종목이면 배경을 칠한다.
 */
@Composable
fun WatchlistItem(
    stock: Stock,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalListDensity.current.tokens
    val priceColor = stock.direction.color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) AppColors.PrimaryBg else AppColors.Transparent)
            .tickFlash(stock.price)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = tokens.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stock.name,
                style = tokens.nameStyle,
                color = AppColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${stock.code} · ${stock.market.name}",
                style = tokens.subStyle,
                color = AppColors.TextSub,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stock.price.withComma(),
                style = tokens.priceStyle,
                color = priceColor,
                textAlign = TextAlign.End,
            )
            Text(
                text = stock.changeLabel(),
                style = AppTypography.Change.copy(fontSize = tokens.subStyle.fontSize, lineHeight = tokens.subStyle.lineHeight),
                color = priceColor,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
