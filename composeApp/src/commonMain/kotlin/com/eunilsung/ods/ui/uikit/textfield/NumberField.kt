package com.eunilsung.ods.ui.uikit.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.uikit.click.clickable

/**
 * 숫자 입력칸 — 왼쪽 −, 가운데 숫자(세 자리 쉼표), 오른쪽 +. 주문 가격·수량에 쓴다.
 *
 * [value] 는 숫자만 담는다. 쉼표는 보이기만 하고 값에는 들어가지 않는다.
 *
 * @param disabledText 주면 입력칸 대신 이 글자를 보이고 입력·−/+ 를 막는다(시장가의 가격).
 * @param label 화면 읽기 프로그램이 읽을 이름.
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    onStep: (up: Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    disabledText: String? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val enabled = disabledText == null
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .border(1.dp, AppColors.Line, shape)
            .background(if (enabled) AppColors.Bg else AppColors.Gray100Bg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(symbol = "−", enabled = enabled, onClick = { onStep(false) })
        Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterEnd) {
            if (enabled) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = AppTypography.BodyNumber.copy(color = AppColors.Text, textAlign = TextAlign.End),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    visualTransformation = ThousandsSeparatorTransformation,
                    cursorBrush = SolidColor(AppColors.PrimaryMain),
                    modifier = Modifier.semantics { contentDescription = label },
                )
            } else {
                Text(text = disabledText.orEmpty(), style = AppTypography.Body, color = AppColors.TextSub)
            }
        }
        StepButton(symbol = "+", enabled = enabled, onClick = { onStep(true) })
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(34.dp)
            .fillMaxHeight()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = AppTypography.ListName, color = if (enabled) AppColors.Text else AppColors.TextDisabled)
    }
}

/** 숫자 사이에 세 자리마다 쉼표를 넣어 보인다. 커서 위치는 쉼표를 건너뛰어 원래 숫자 자리와 맞춘다. */
object ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val length = digits.length
        val builder = StringBuilder()
        val originalToTransformed = IntArray(length + 1)
        val transformedToOriginal = mutableListOf<Int>()
        for (index in 0 until length) {
            if (index > 0 && (length - index) % 3 == 0) {
                builder.append(',')
                transformedToOriginal += index
            }
            originalToTransformed[index] = builder.length
            builder.append(digits[index])
            transformedToOriginal += index
        }
        originalToTransformed[length] = builder.length
        transformedToOriginal += length
        return TransformedText(
            AnnotatedString(builder.toString()),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = originalToTransformed[offset.coerceIn(0, length)]
                override fun transformedToOriginal(offset: Int): Int =
                    transformedToOriginal[offset.coerceIn(0, transformedToOriginal.lastIndex)]
            },
        )
    }
}
