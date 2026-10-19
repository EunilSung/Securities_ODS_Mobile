package com.eunilsung.ods.ui.uikit.textfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.close_icon
import ods.composeapp.generated.resources.search_clear
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 목록 위 검색칸 — 회색 바탕 한 줄. 글자가 있으면 오른쪽에 지우기 버튼을 둔다.
 *
 * @param testTag 화면 자동화가 찾을 이름.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.Gray100Bg)
            .padding(start = Dimens.Space12, end = Dimens.Space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(text = placeholder, style = AppTypography.Body, color = AppColors.TextDisabled, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = AppTypography.Body.copy(color = AppColors.Text),
                cursorBrush = SolidColor(AppColors.PrimaryMain),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().testTag(testTag),
            )
        }
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier.size(36.dp).clickable(cornerRadius = 18.dp, onClick = { onValueChange("") }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.close_icon),
                    contentDescription = stringResource(Res.string.search_clear),
                    tint = AppColors.TextSub,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
