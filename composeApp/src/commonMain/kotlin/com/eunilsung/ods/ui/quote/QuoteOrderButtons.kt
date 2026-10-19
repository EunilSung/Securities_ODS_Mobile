package com.eunilsung.ods.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.OrderSide
import com.eunilsung.ods.ui.order.panel.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider

/** 현재가 아래 매수·매도 버튼 줄. 매수는 상승색, 매도는 하락색. */
@Composable
fun QuoteOrderButtons(onSideClick: (OrderSide) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(AppColors.Bg)) {
        LineDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
        ) {
            for (side in OrderSide.entries) {
                val color = if (side == OrderSide.BUY) AppColors.Rise else AppColors.Fall
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                        .clickable(onClick = { onSideClick(side) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = side.label(), style = AppTypography.Section, fontWeight = FontWeight.Bold, color = AppColors.White)
                }
            }
        }
    }
}
