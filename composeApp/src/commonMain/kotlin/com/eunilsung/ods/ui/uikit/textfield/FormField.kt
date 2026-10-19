package com.eunilsung.ods.ui.uikit.textfield

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography

/**
 * 입력 양식의 한 칸 — 이름표가 테두리에 붙는 칸. [error] 가 있으면 칸 아래에 주황 글자로 보인다.
 *
 * @param testTag 화면 자동화가 찾을 이름.
 */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        placeholder = placeholder?.let { { Text(text = it, color = AppColors.TextDisabled) } },
        singleLine = singleLine,
        isError = error != null,
        supportingText = error?.let { { Text(text = it, color = AppColors.Caution, style = AppTypography.Caption) } },
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.PrimaryMain,
            focusedLabelColor = AppColors.PrimaryMain,
            unfocusedBorderColor = AppColors.Line,
            cursorColor = AppColors.PrimaryMain,
            focusedTextColor = AppColors.Text,
            unfocusedTextColor = AppColors.Text,
            unfocusedLabelColor = AppColors.TextSub,
            errorBorderColor = AppColors.Caution,
            errorLabelColor = AppColors.Caution,
        ),
        modifier = modifier.fillMaxWidth().testTag(testTag),
    )
}
