package com.eunilsung.ods.ui.customer.consultation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.button.PrimaryButton
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.consult_channel
import ods.composeapp.generated.resources.consult_content_hint
import ods.composeapp.generated.resources.consult_save
import ods.composeapp.generated.resources.consult_save_failed
import ods.composeapp.generated.resources.consult_saved
import ods.composeapp.generated.resources.consult_topic
import org.jetbrains.compose.resources.stringResource

/**
 * 상담 기록 입력 — 만난 곳, 주제, 내용, 저장 버튼. 상태 없이 값과 콜백만 받는다.
 *
 * @param minContentHeight 내용 칸의 최소 높이. 상담 칸처럼 좁은 곳에서는 낮춘다.
 */
@Composable
fun ConsultationForm(
    uiState: ConsultationUiState,
    onAction: (ConsultationActions) -> Unit,
    modifier: Modifier = Modifier,
    minContentHeight: Dp = 160.dp,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = stringResource(Res.string.consult_channel), style = AppTypography.Caption, color = AppColors.TextSub)
        SegmentedControl(
            options = ConsultChannel.entries,
            selected = uiState.channel,
            label = { it.label() },
            onSelect = { onAction(ConsultationActions.OnChannelChange(it)) },
        )
        Text(text = stringResource(Res.string.consult_topic), style = AppTypography.Caption, color = AppColors.TextSub)
        SegmentedControl(
            options = ConsultTopic.entries,
            selected = uiState.topic,
            label = { it.label() },
            onSelect = { onAction(ConsultationActions.OnTopicChange(it)) },
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        )
        OutlinedTextField(
            value = uiState.content,
            onValueChange = { onAction(ConsultationActions.OnContentChange(it)) },
            placeholder = { Text(stringResource(Res.string.consult_content_hint), style = AppTypography.Body) },
            textStyle = AppTypography.Body.copy(color = AppColors.Text),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.PrimaryMain,
                unfocusedBorderColor = AppColors.Line,
                cursorColor = AppColors.PrimaryMain,
            ),
            supportingText = { Text("${uiState.content.length} / ${ConsultationUiState.MAX_LENGTH}", style = AppTypography.CaptionNumber) },
            modifier = Modifier.fillMaxWidth().heightIn(min = minContentHeight).testTag(TestTags.CONSULT_CONTENT),
        )
        when {
            uiState.saveFailed -> Text(text = stringResource(Res.string.consult_save_failed), style = AppTypography.Body, color = AppColors.Caution)
            uiState.justSaved -> Text(text = stringResource(Res.string.consult_saved), style = AppTypography.Body, color = AppColors.PrimaryMain)
        }
        PrimaryButton(
            text = stringResource(Res.string.consult_save),
            onClick = { onAction(ConsultationActions.OnSave) },
            enabled = uiState.canSave,
            loading = uiState.isSaving,
            modifier = Modifier.padding(top = 4.dp).testTag(TestTags.CONSULT_SAVE),
        )
    }
}
