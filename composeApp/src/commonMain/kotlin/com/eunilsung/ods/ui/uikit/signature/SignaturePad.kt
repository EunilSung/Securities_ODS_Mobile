package com.eunilsung.ods.ui.uikit.signature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Signature
import com.eunilsung.ods.domain.model.SignaturePoint
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.signature_clear
import ods.composeapp.generated.resources.signature_hint
import org.jetbrains.compose.resources.stringResource

/**
 * 전자서명 칸 — 고객이 손가락이나 펜으로 서명한다. 한 획을 마칠 때마다 [onSignatureChange] 로 지금까지의 서명을 넘긴다.
 *
 * 칸 안의 터치는 모두 이 칸이 가져간다. 서명하다가 화면이 스크롤되거나 폰에서 화면이 밀려 닫히지 않게 하려는 것이다.
 * 점은 칸 크기에 대한 비율로 남겨 기기마다 크기가 달라도 같은 서명으로 읽힌다.
 *
 * @param resetKey 바뀌면 그린 것을 지운다.
 */
@Composable
fun SignaturePad(
    onSignatureChange: (Signature) -> Unit,
    resetKey: Int,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val current = remember { mutableStateListOf<Offset>() }
    val size = remember { arrayOf(IntSize.Zero) }

    LaunchedEffect(resetKey) {
        strokes.clear()
        current.clear()
    }

    fun publish() {
        val (w, h) = size[0].width.toFloat().coerceAtLeast(1f) to size[0].height.toFloat().coerceAtLeast(1f)
        onSignatureChange(Signature(strokes.map { stroke -> stroke.map { SignaturePoint(it.x / w, it.y / h) } }))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.Gray50Bg)
                .border(1.dp, AppColors.Line, RoundedCornerShape(12.dp))
                .testTag(TestTags.SIGNATURE_PAD)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        size[0] = this.size
                        down.consume()
                        current.clear()
                        current.add(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            current.add(change.position)
                            change.consume()
                        }
                        if (current.size > 1) {
                            strokes.add(current.toList())
                            publish()
                        }
                        current.clear()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (strokes.isEmpty() && current.isEmpty()) {
                Text(text = stringResource(Res.string.signature_hint), style = AppTypography.Body, color = AppColors.TextDisabled)
            }
            val ink = AppColors.Text
            Canvas(modifier = Modifier.fillMaxSize()) {
                (strokes + listOf(current.toList())).forEach { stroke ->
                    if (stroke.size < 2) return@forEach
                    val path = Path().apply {
                        moveTo(stroke.first().x, stroke.first().y)
                        stroke.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, color = ink, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = Dimens.Space8), horizontalArrangement = Arrangement.End) {
            OutlineButton(text = stringResource(Res.string.signature_clear), onClick = {
                strokes.clear()
                publish()
            })
        }
    }
}
