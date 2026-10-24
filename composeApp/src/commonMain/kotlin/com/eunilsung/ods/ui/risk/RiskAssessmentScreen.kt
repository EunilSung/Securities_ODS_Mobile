package com.eunilsung.ods.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.domain.model.RiskAssessmentResult
import com.eunilsung.ods.domain.model.RiskQuestion
import com.eunilsung.ods.ui.customer.color
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popOrClose
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import com.eunilsung.ods.ui.uikit.button.PrimaryButton
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.signature.SignaturePad
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.risk_assess
import ods.composeapp.generated.resources.risk_error_not_signed_in
import ods.composeapp.generated.resources.risk_error_signature
import ods.composeapp.generated.resources.risk_next
import ods.composeapp.generated.resources.risk_note
import ods.composeapp.generated.resources.risk_previous
import ods.composeapp.generated.resources.risk_question_progress
import ods.composeapp.generated.resources.risk_result_allowed
import ods.composeapp.generated.resources.risk_result_confirm
import ods.composeapp.generated.resources.risk_result_elderly
import ods.composeapp.generated.resources.risk_result_save
import ods.composeapp.generated.resources.risk_result_score
import ods.composeapp.generated.resources.risk_result_sign
import ods.composeapp.generated.resources.risk_result_title
import ods.composeapp.generated.resources.risk_see_result
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 투자성향 진단 — 고객 상세 위에 쌓인다. 저장하면 닫혀 상세로 돌아가고 상세의 투자성향이 바뀐다. */
data class RiskAssessmentScreen(val customerId: String) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: RiskAssessmentViewModel = koinViewModel(key = "risk-$key")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current

        LaunchedEffect(customerId) { viewModel.onAction(RiskAssessmentActions.OnEnter(customerId)) }
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    RiskAssessmentEvent.Saved -> rightNavigator.popOrClose()
                }
            }
        }

        RiskAssessmentContent(uiState = uiState, onAction = viewModel::onAction, onBack = { rightNavigator.popOrClose() })
    }
}

@Composable
fun RiskAssessmentContent(uiState: RiskAssessmentUiState, onAction: (RiskAssessmentActions) -> Unit, onBack: () -> Unit) {
    val total = RiskQuestion.entries.size
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding()) {
        TopBar(title = stringResource(Res.string.risk_assess), onBack = onBack)
        LinearProgressIndicator(
            progress = { (uiState.stepIndex.coerceAtMost(total)).toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = AppColors.PrimaryMain,
            trackColor = AppColors.Gray100Bg,
        )
        LineDivider()
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
        ) {
            val question = uiState.question
            val result = uiState.result
            if (question != null) {
                QuestionStep(uiState, question, total, onAction)
            } else if (result != null) {
                ResultStep(uiState, result, onAction)
            }
        }
    }
}

@Composable
private fun QuestionStep(uiState: RiskAssessmentUiState, question: RiskQuestion, total: Int, onAction: (RiskAssessmentActions) -> Unit) {
    Text(text = stringResource(Res.string.risk_question_progress, uiState.stepIndex + 1, total), style = AppTypography.Caption, color = AppColors.TextSub)
    Text(text = question.title(), style = AppTypography.Title, color = AppColors.Text)
    question.options().forEachIndexed { index, text ->
        val selected = uiState.answers[question] == index
        Text(
            text = text,
            style = AppTypography.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) AppColors.PrimaryMain else AppColors.Text,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${TestTags.RISK_OPTION_PREFIX}$index")
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) AppColors.PrimaryBg else AppColors.Transparent)
                .border(1.dp, if (selected) AppColors.PrimaryMain else AppColors.Line, RoundedCornerShape(10.dp))
                .clickable(onClick = { onAction(RiskAssessmentActions.OnAnswer(question, index)) })
                .padding(horizontal = Dimens.Space16, vertical = 14.dp),
        )
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = Dimens.Space8), horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
        if (uiState.stepIndex > 0) {
            OutlineButton(text = stringResource(Res.string.risk_previous), onClick = { onAction(RiskAssessmentActions.OnPrevious) })
        }
        PrimaryButton(
            text = stringResource(if (uiState.stepIndex == total - 1) Res.string.risk_see_result else Res.string.risk_next),
            onClick = { onAction(RiskAssessmentActions.OnNext) },
            enabled = uiState.canGoNext,
            modifier = Modifier.weight(1f).testTag(TestTags.RISK_NEXT),
        )
    }
}

@Composable
private fun ResultStep(uiState: RiskAssessmentUiState, result: RiskAssessmentResult, onAction: (RiskAssessmentActions) -> Unit) {
    Text(text = stringResource(Res.string.risk_result_title, uiState.customerName), style = AppTypography.Body, color = AppColors.TextSub)
    Text(
        text = result.profile.label(),
        style = AppTypography.PriceHero,
        color = result.profile.color,
        modifier = Modifier.testTag(TestTags.RISK_RESULT),
    )
    Text(text = stringResource(Res.string.risk_result_score, result.score), style = AppTypography.BodyNumber, color = AppColors.TextSub)
    ScoreBar(result.score)
    Text(text = result.profile.description(), style = AppTypography.Body, color = AppColors.Text)
    Text(
        text = stringResource(Res.string.risk_result_allowed, result.profile.maxProductRisk.level),
        style = AppTypography.Body,
        color = AppColors.PrimaryMain,
    )
    if (result.isElderly) {
        Text(
            text = stringResource(Res.string.risk_result_elderly),
            style = AppTypography.Body,
            color = AppColors.Caution,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AppColors.Gray50Bg).padding(Dimens.Space12),
        )
    }
    OutlineButton(text = stringResource(Res.string.risk_previous), onClick = { onAction(RiskAssessmentActions.OnPrevious) })
    LineDivider(modifier = Modifier.padding(vertical = Dimens.Space4))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.RISK_CONFIRM)
            .clickable(cornerRadius = 8.dp, onClick = { onAction(RiskAssessmentActions.OnConfirmChange(!uiState.confirmed)) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = uiState.confirmed,
            onCheckedChange = { onAction(RiskAssessmentActions.OnConfirmChange(it)) },
            colors = CheckboxDefaults.colors(checkedColor = AppColors.PrimaryMain),
        )
        Text(text = stringResource(Res.string.risk_result_confirm), style = AppTypography.Body, color = AppColors.Text)
    }
    Text(text = stringResource(Res.string.risk_result_sign), style = AppTypography.Section, color = AppColors.Text)
    SignaturePad(onSignatureChange = { onAction(RiskAssessmentActions.OnSignatureChange(it)) }, resetKey = uiState.stepIndex)
    when (uiState.error) {
        RiskSaveError.SIGNATURE_REQUIRED -> ErrorText(stringResource(Res.string.risk_error_signature))
        RiskSaveError.NOT_SIGNED_IN -> ErrorText(stringResource(Res.string.risk_error_not_signed_in))
        null -> Unit
    }
    val note = stringResource(Res.string.risk_note, result.profile.label(), result.score)
    PrimaryButton(
        text = stringResource(Res.string.risk_result_save),
        onClick = { onAction(RiskAssessmentActions.OnSave(note)) },
        enabled = uiState.canSave,
        loading = uiState.isSaving,
        modifier = Modifier.testTag(TestTags.RISK_SAVE),
    )
}

/** 0~100 점수 막대 — 다섯 단계 경계를 옅은 눈금으로 보인다. */
@Composable
private fun ScoreBar(score: Int) {
    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(AppColors.Gray100Bg)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(score.coerceIn(0, 100) / 100f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(AppColors.PrimaryMain),
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = AppTypography.Body, color = AppColors.Caution)
}
