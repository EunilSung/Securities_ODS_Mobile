package com.eunilsung.ods.ui.quote.chart

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Candle
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.domain.model.PriceDirection
import com.eunilsung.ods.domain.util.floorToTick
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.ui.util.formatAxisTime
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.chart_high
import ods.composeapp.generated.resources.chart_low
import ods.composeapp.generated.resources.quote_chart
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * 봉 차트 — 위는 가격 봉, 아래는 거래량 막대, 오른쪽은 가격 눈금.
 *
 * 폭에 들어가는 만큼 최근 봉만 그린다. 보이는 구간의 최고·최저를 표시하고, 현재가는 점선과 오른쪽 상자로 보인다.
 * 양봉은 상승색, 음봉은 하락색이다. 외부 차트 라이브러리 없이 그려 Android·iOS 가 같은 코드를 쓴다.
 *
 * @param basePrice 현재가 색을 정할 기준가격(전일 종가).
 */
@Composable
fun CandleChart(
    candles: List<Candle>,
    period: ChartPeriod,
    currentPrice: Long,
    basePrice: Long,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val colors = ChartColors(
        rise = AppColors.Rise,
        fall = AppColors.Fall,
        grid = AppColors.Line,
        current = PriceDirection.of(currentPrice, basePrice).color,
    )
    val labelStyle = AppTypography.ChartLabel.copy(color = AppColors.TextSub)
    val markerStyle = AppTypography.ChartLabel.copy(color = AppColors.Text)
    val currentLabelStyle = AppTypography.ChartLabel.copy(color = AppColors.White, fontWeight = FontWeight.SemiBold)
    val highLabel = stringResource(Res.string.chart_high)
    val lowLabel = stringResource(Res.string.chart_low)
    val description = stringResource(Res.string.quote_chart)

    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        if (candles.isEmpty()) return@Canvas

        val axisWidth = 60.dp.toPx()
        val timeAxisHeight = 18.dp.toPx()
        val volumeGap = 8.dp.toPx()
        val labelInset = 6.dp.toPx()
        val plotWidth = size.width - axisWidth
        val volumeHeight = (size.height - timeAxisHeight) * 0.2f
        val priceHeight = size.height - timeAxisHeight - volumeHeight - volumeGap
        val slot = 7.dp.toPx()
        val bodyWidth = slot * 0.7f

        val visible = candles.takeLast((plotWidth / slot).toInt().coerceIn(1, candles.size))
        val maxHigh = max(visible.maxOf { it.high }, currentPrice)
        val minLow = min(visible.minOf { it.low }, currentPrice)
        val padding = ((maxHigh - minLow) * 0.08).coerceAtLeast(1.0)
        val top = maxHigh + padding
        val bottom = minLow - padding
        fun y(price: Long): Float = ((top - price) / (top - bottom) * priceHeight).toFloat()
        val startX = plotWidth - visible.size * slot
        fun x(index: Int): Float = startX + slot * index + slot / 2

        val currentY = y(currentPrice)
        val priceLayout = textMeasurer.measure(currentPrice.withComma(), currentLabelStyle)
        val boxHeight = priceLayout.size.height + 4.dp.toPx()

        for (line in 0..3) {
            val fraction = (line * 2 + 1) / 8f
            val value = floorToTick((top - (top - bottom) * fraction).roundToLong())
            val valueY = y(value)
            drawLine(colors.grid, Offset(0f, valueY), Offset(plotWidth, valueY), strokeWidth = 1f)
            if (abs(valueY - currentY) < boxHeight) continue
            val layout = textMeasurer.measure(value.withComma(), labelStyle)
            drawText(layout, topLeft = Offset(plotWidth + labelInset, valueY - layout.size.height / 2f))
        }

        val volumeTop = priceHeight + volumeGap
        val maxVolume = visible.maxOf { it.volume }.coerceAtLeast(1)
        drawLine(colors.grid, Offset(0f, volumeTop + volumeHeight), Offset(plotWidth, volumeTop + volumeHeight), strokeWidth = 1f)
        visible.forEachIndexed { index, candle ->
            val centerX = x(index)
            val color = if (candle.isRising) colors.rise else colors.fall
            drawLine(color, Offset(centerX, y(candle.high)), Offset(centerX, y(candle.low)), strokeWidth = 1.dp.toPx())
            val bodyTop = y(max(candle.open, candle.close))
            val bodyBottom = y(min(candle.open, candle.close))
            drawRect(color, Offset(centerX - bodyWidth / 2, bodyTop), Size(bodyWidth, max(bodyBottom - bodyTop, 1.dp.toPx())))
            val volumeBar = candle.volume.toFloat() / maxVolume * volumeHeight
            drawRect(color.copy(alpha = 0.4f), Offset(centerX - bodyWidth / 2, volumeTop + volumeHeight - volumeBar), Size(bodyWidth, volumeBar))
        }

        val highIndex = visible.indices.maxBy { visible[it].high }
        val lowIndex = visible.indices.minBy { visible[it].low }
        drawMarker(textMeasurer, "$highLabel ${visible[highIndex].high.withComma()}", markerStyle, x(highIndex), y(visible[highIndex].high), above = true, plotWidth)
        drawMarker(textMeasurer, "$lowLabel ${visible[lowIndex].low.withComma()}", markerStyle, x(lowIndex), y(visible[lowIndex].low), above = false, plotWidth)

        drawLine(
            colors.current, Offset(0f, currentY), Offset(plotWidth, currentY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
        drawRect(colors.current, Offset(plotWidth, currentY - boxHeight / 2), Size(axisWidth, boxHeight))
        drawText(priceLayout, topLeft = Offset(plotWidth + labelInset, currentY - priceLayout.size.height / 2f))

        val timeY = size.height - timeAxisHeight + 4.dp.toPx()
        listOf(0, visible.size / 2, visible.lastIndex).distinct().forEach { index ->
            val layout = textMeasurer.measure(period.formatAxisTime(visible[index].epochSeconds), labelStyle)
            val left = (x(index) - layout.size.width / 2f).coerceIn(0f, (plotWidth - layout.size.width).coerceAtLeast(0f))
            drawText(layout, topLeft = Offset(left, timeY))
        }
    }
}

private class ChartColors(val rise: Color, val fall: Color, val grid: Color, val current: Color)

/** 최고·최저 표시. 봉이 오른쪽 절반에 있으면 글자를 봉 왼쪽으로 붙여 차트 밖으로 나가지 않게 한다. */
private fun DrawScope.drawMarker(
    textMeasurer: TextMeasurer,
    text: String,
    style: TextStyle,
    centerX: Float,
    priceY: Float,
    above: Boolean,
    plotWidth: Float,
) {
    val layout = textMeasurer.measure(text, style)
    val gap = 3.dp.toPx()
    val left = if (centerX > plotWidth / 2) centerX - layout.size.width - gap else centerX + gap
    val top = if (above) priceY - layout.size.height - gap else priceY + gap
    drawText(layout, topLeft = Offset(left.coerceIn(0f, (plotWidth - layout.size.width).coerceAtLeast(0f)), top.coerceAtLeast(0f)))
}
