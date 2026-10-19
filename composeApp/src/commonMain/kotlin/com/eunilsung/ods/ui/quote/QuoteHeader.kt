package com.eunilsung.ods.ui.quote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Candle
import com.eunilsung.ods.domain.model.PriceDirection
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.stock.tickFlash
import com.eunilsung.ods.ui.util.changeLabel
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.quote_high
import ods.composeapp.generated.resources.quote_low
import ods.composeapp.generated.resources.quote_lower_limit
import ods.composeapp.generated.resources.quote_open
import ods.composeapp.generated.resources.quote_previous_close
import ods.composeapp.generated.resources.quote_upper_limit
import ods.composeapp.generated.resources.quote_volume
import org.jetbrains.compose.resources.stringResource

/**
 * 현재가 머리 — 큰 현재가와 전일 대비, 그 아래 시가·고가·저가·전일·상한가·하한가.
 *
 * @param today 오늘 일봉. 아직 없으면 시가·고가·저가를 비운다.
 */
@Composable
fun QuoteHeader(stock: Stock, today: Candle?, modifier: Modifier = Modifier) {
    val base = stock.previousClose
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space16),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space4),
    ) {
        Text(text = "${stock.code} · ${stock.market.name}", style = AppTypography.Caption, color = AppColors.TextSub)
        Text(
            text = stock.price.withComma(),
            style = AppTypography.PriceHero,
            color = stock.direction.color,
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).tickFlash(stock.price),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stock.changeLabel(separator = " "), style = AppTypography.Change, color = stock.direction.color)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${stringResource(Res.string.quote_volume)} ${stock.volume.withComma()}",
                style = AppTypography.CaptionNumber,
                color = AppColors.TextSub,
            )
        }
        Spacer(Modifier.height(Dimens.Space8))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
            StatCell(stringResource(Res.string.quote_open), today?.open, base)
            StatCell(stringResource(Res.string.quote_high), today?.high, base)
            StatCell(stringResource(Res.string.quote_low), today?.low, base)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
            StatCell(stringResource(Res.string.quote_previous_close), base, base)
            StatCell(stringResource(Res.string.quote_upper_limit), stock.upperLimit, base, AppColors.Rise)
            StatCell(stringResource(Res.string.quote_lower_limit), stock.lowerLimit, base, AppColors.Fall)
        }
    }
}

/** 이름과 값 한 칸. 값 색은 기준가격 대비 등락이고, [fixedColor] 를 주면 그 색을 쓴다. */
@Composable
private fun RowScope.StatCell(label: String, value: Long?, basePrice: Long, fixedColor: Color? = null) {
    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = AppTypography.Caption, color = AppColors.TextSub)
        Spacer(Modifier.weight(1f))
        Text(
            text = value?.withComma() ?: "-",
            style = AppTypography.CaptionNumber,
            color = fixedColor ?: value?.let { PriceDirection.of(it, basePrice).color } ?: AppColors.TextSub,
            maxLines = 1,
        )
    }
}
