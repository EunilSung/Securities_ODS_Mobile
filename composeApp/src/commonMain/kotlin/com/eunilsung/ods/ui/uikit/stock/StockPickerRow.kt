package com.eunilsung.ods.ui.uikit.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable

/** 탭 화면 위쪽 종목 줄 — 관심종목 이름을 가로로 늘어놓고 하나를 고른다(현재가·주문 탭). */
@Composable
fun StockPickerRow(stocks: List<Stock>, selectedCode: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
    ) {
        items(stocks, key = { it.code }) { stock ->
            val isSelected = stock.code == selectedCode
            Text(
                text = stock.name,
                style = AppTypography.Caption,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) AppColors.White else AppColors.TextSub,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) AppColors.PrimaryMain else AppColors.Gray100Bg)
                    .clickable(onClick = { onSelect(stock.code) })
                    .padding(horizontal = Dimens.Space12, vertical = 6.dp),
            )
        }
    }
}
