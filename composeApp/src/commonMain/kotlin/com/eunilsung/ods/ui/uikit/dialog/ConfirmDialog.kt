package com.eunilsung.ods.ui.uikit.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.exposeTestTags

/**
 * 되돌리기 어려운 일을 하기 전에 한 번 더 묻는 창 — 제목, 설명, 취소·확인 버튼.
 *
 * @param confirmTestTag 확인 버튼을 화면 자동화가 찾을 이름.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmTestTag: String,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AppColors.Bg,
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().exposeTestTags(),
        ) {
            Column(modifier = Modifier.padding(Dimens.Space24), verticalArrangement = Arrangement.spacedBy(Dimens.Space12)) {
                Text(text = title, style = AppTypography.Title, color = AppColors.Text)
                Text(text = message, style = AppTypography.Body, color = AppColors.TextSub)
                Row(modifier = Modifier.fillMaxWidth().padding(top = Dimens.Space8), horizontalArrangement = Arrangement.spacedBy(Dimens.Space8, Alignment.End)) {
                    DialogButton(text = dismissText, color = AppColors.TextSub, onClick = onDismiss)
                    DialogButton(text = confirmText, color = AppColors.Caution, onClick = onConfirm, modifier = Modifier.testTag(confirmTestTag))
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppTypography.Section,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.clickable(cornerRadius = 8.dp, onClick = onClick).padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
    )
}
