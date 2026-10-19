package com.eunilsung.ods.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eunilsung.ods.domain.model.ChartPeriod
import com.eunilsung.ods.ui.quote.chart.CandleChart
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.exposeTestTags
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import com.eunilsung.ods.ui.uikit.stock.tickFlash
import com.eunilsung.ods.ui.util.changeLabel
import com.eunilsung.ods.ui.util.color
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.close_icon
import ods.composeapp.generated.resources.present_close
import ods.composeapp.generated.resources.present_demo_notice
import ods.composeapp.generated.resources.present_next
import ods.composeapp.generated.resources.present_previous
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 고객에게 보여 주기 — 화면 전체에 한 종목의 현재가와 차트를 크게 그린다. 직원용 정보(고객 목록·상담 칸·호가)는 가린다.
 *
 * 태블릿을 고객 쪽으로 돌려 시장 상황을 설명할 때 쓴다. 이전·다음 단추로 관심종목을 넘겨 가며 보여 준다.
 *
 * 시연용 시세면 실제 시세가 아니라는 것을 화면에 알린다.
 */
@Composable
fun PresentationView(
    uiState: QuoteUiState,
    periodLabel: @Composable (ChartPeriod) -> String,
    onAction: (QuoteActions) -> Unit,
) {
    val stock = uiState.stock ?: return
    Dialog(
        onDismissRequest = { onAction(QuoteActions.OnPresentDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Bg)
                .exposeTestTags()
                .testTag(TestTags.PRESENTATION)
                .safeDrawingPadding()
                .padding(Dimens.Space24),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stock.name, style = AppTypography.Title.copy(fontSize = 30.sp, lineHeight = 36.sp), color = AppColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "${stock.code} · ${stock.market.name}", style = AppTypography.Body, color = AppColors.TextSub)
                }
                Box(
                    modifier = Modifier.size(48.dp).testTag(TestTags.PRESENTATION_CLOSE).clickable(cornerRadius = 24.dp, onClick = { onAction(QuoteActions.OnPresentDismiss) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painter = painterResource(Res.drawable.close_icon), contentDescription = stringResource(Res.string.present_close), tint = AppColors.Text, modifier = Modifier.size(28.dp))
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Dimens.Space16)) {
                Text(
                    text = stock.price.withComma(),
                    style = AppTypography.PriceHero.copy(fontSize = 56.sp, lineHeight = 64.sp),
                    color = stock.direction.color,
                    modifier = Modifier.tickFlash(stock.price),
                )
                Text(
                    text = stock.changeLabel(separator = " "),
                    style = AppTypography.Change.copy(fontSize = 22.sp, lineHeight = 28.sp),
                    color = stock.direction.color,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            SegmentedControl(
                options = ChartPeriod.entries,
                selected = uiState.period,
                label = { periodLabel(it) },
                onSelect = { onAction(QuoteActions.OnPeriodChange(it)) },
            )
            CandleChart(
                candles = uiState.candles,
                period = uiState.period,
                currentPrice = stock.price,
                basePrice = stock.previousClose,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                OutlineButton(
                    text = stringResource(Res.string.present_previous, uiState.previousName.orEmpty()),
                    onClick = { onAction(QuoteActions.OnPresentStep(forward = false)) },
                    enabled = uiState.previousName != null,
                )
                if (uiState.usesDemoData) {
                    Text(text = stringResource(Res.string.present_demo_notice), style = AppTypography.Caption, color = AppColors.TextSub, modifier = Modifier.weight(1f))
                } else {
                    Box(Modifier.weight(1f))
                }
                OutlineButton(
                    text = stringResource(Res.string.present_next, uiState.nextName.orEmpty()),
                    onClick = { onAction(QuoteActions.OnPresentStep(forward = true)) },
                    enabled = uiState.nextName != null,
                    modifier = Modifier.testTag(TestTags.PRESENTATION_NEXT),
                )
            }
        }
    }
}
