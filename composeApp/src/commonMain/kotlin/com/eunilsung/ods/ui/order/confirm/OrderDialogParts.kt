package com.eunilsung.ods.ui.order.confirm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.order_done
import org.jetbrains.compose.resources.stringResource

/**
 * 주문·정정·취소 창의 틀 — 가운데 뜨는 둥근 카드.
 *
 * @param busy 보내는 중이면 true. 그동안은 바깥을 눌러도, 뒤로 가기로도 닫히지 않는다.
 */
@Composable
fun OrderDialogFrame(
    busy: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.Bg)
                .padding(Dimens.Space20),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
            content = content,
        )
    }
}

/** 결과 한 장 — 제목, 설명, 확인 버튼. */
@Composable
fun ResultBody(title: String, body: String, onDone: () -> Unit, titleColor: Color = AppColors.Text) {
    Text(text = title, style = AppTypography.Title, color = titleColor)
    Text(text = body, style = AppTypography.Body, color = AppColors.TextSub)
    Row(modifier = Modifier.padding(top = Dimens.Space4)) {
        DialogButton(
            text = stringResource(Res.string.order_done),
            background = AppColors.PrimaryMain,
            textColor = AppColors.White,
            enabled = true,
            weight = 1f,
            onClick = onDone,
        )
    }
}

/** 이름 · 값 한 줄. */
@Composable
fun SummaryRow(label: String, value: String, valueColor: Color = AppColors.Text, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = AppTypography.Body, color = AppColors.TextSub, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = AppTypography.BodyNumber,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = valueColor,
        )
    }
}

/** 창 아래 버튼. [loading] 이면 글자 대신 도는 표시를 그린다. */
@Composable
fun RowScope.DialogButton(
    text: String,
    background: Color,
    textColor: Color,
    enabled: Boolean,
    weight: Float,
    onClick: () -> Unit,
    loading: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .weight(weight)
            .height(48.dp)
            .clip(shape)
            .background(background)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = textColor)
        } else {
            Text(
                text = text,
                style = AppTypography.Section,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = if (enabled) 1f else 0.5f),
            )
        }
    }
}
