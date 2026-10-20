package com.eunilsung.ods.ui.schedule.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.ui.customer.consultation.ConsultationEditorScreen
import com.eunilsung.ods.ui.customer.list.openCustomerDetail
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.schedule.color
import com.eunilsung.ods.ui.schedule.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.badge.Badge
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import com.eunilsung.ods.ui.uikit.button.PrimaryButton
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.dialog.ConfirmDialog
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.section.InfoRow
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import com.eunilsung.ods.ui.util.formatDateTime
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.error_server_unreachable
import ods.composeapp.generated.resources.visit_cancel
import ods.composeapp.generated.resources.visit_cancel_confirm
import ods.composeapp.generated.resources.visit_cancel_keep
import ods.composeapp.generated.resources.visit_cancel_message
import ods.composeapp.generated.resources.visit_complete
import ods.composeapp.generated.resources.visit_complete_note
import ods.composeapp.generated.resources.visit_detail
import ods.composeapp.generated.resources.visit_memo
import ods.composeapp.generated.resources.visit_open_customer
import ods.composeapp.generated.resources.visit_place
import ods.composeapp.generated.resources.visit_purpose
import ods.composeapp.generated.resources.visit_time
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 방문 상세 — 오른쪽 스택에 뜬다. 완료하면 위에 상담 기록이 쌓인다. */
data class VisitDetailScreen(val visitId: String) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: VisitDetailViewModel = koinViewModel(key = "visit-$visitId")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current

        LaunchedEffect(visitId) { viewModel.onAction(VisitDetailActions.OnEnter(visitId)) }
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is VisitDetailEvent.OpenCustomer -> rightNavigator.openCustomerDetail(event.customerId)
                    is VisitDetailEvent.OpenConsultation -> rightNavigator.push(ConsultationEditorScreen(event.customerId, event.topic))
                }
            }
        }

        VisitDetailContent(uiState = uiState, onAction = viewModel::onAction, onBack = { rightNavigator.popToEmptyRoot() })
    }
}

@Composable
fun VisitDetailContent(uiState: VisitDetailUiState, onAction: (VisitDetailActions) -> Unit, onBack: () -> Unit) {
    val visit = uiState.visit
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding()) {
        TopBar(title = visit?.customerName ?: stringResource(Res.string.visit_detail), onBack = onBack)
        LineDivider()
        if (visit == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Dimens.Space12)) {
            Row(
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.Space12),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = visit.customerName, style = AppTypography.Title, color = AppColors.Text)
                Badge(text = visit.status.label(), color = visit.status.color)
            }
            InfoRow(stringResource(Res.string.visit_time), formatDateTime(visit.scheduledAtMillis))
            InfoRow(stringResource(Res.string.visit_place), visit.place)
            InfoRow(stringResource(Res.string.visit_purpose), visit.purpose.label())
            if (visit.memo.isNotBlank()) InfoRow(stringResource(Res.string.visit_memo), visit.memo)

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space24),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
            ) {
                OutlineButton(
                    text = stringResource(Res.string.visit_open_customer),
                    onClick = { onAction(VisitDetailActions.OnOpenCustomer) },
                    modifier = Modifier.testTag(TestTags.VISIT_OPEN_CUSTOMER),
                )
                if (visit.status == VisitStatus.SCHEDULED) {
                    if (uiState.actionFailed) {
                        Text(text = stringResource(Res.string.error_server_unreachable), style = AppTypography.Body, color = AppColors.Caution)
                    }
                    Text(text = stringResource(Res.string.visit_complete_note), style = AppTypography.Caption, color = AppColors.TextSub)
                    PrimaryButton(
                        text = stringResource(Res.string.visit_complete),
                        onClick = { onAction(VisitDetailActions.OnComplete) },
                        modifier = Modifier.testTag(TestTags.VISIT_COMPLETE),
                    )
                    Text(
                        text = stringResource(Res.string.visit_cancel),
                        style = AppTypography.Body,
                        color = AppColors.Caution,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag(TestTags.VISIT_CANCEL)
                            .clickable(cornerRadius = 8.dp, onClick = { onAction(VisitDetailActions.OnCancelClick) })
                            .padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
                    )
                }
            }
        }
    }

    if (uiState.isConfirmingCancel) {
        ConfirmDialog(
            title = stringResource(Res.string.visit_cancel),
            message = stringResource(Res.string.visit_cancel_message),
            confirmText = stringResource(Res.string.visit_cancel_confirm),
            dismissText = stringResource(Res.string.visit_cancel_keep),
            onConfirm = { onAction(VisitDetailActions.OnCancelConfirm) },
            onDismiss = { onAction(VisitDetailActions.OnCancelDismiss) },
            confirmTestTag = TestTags.VISIT_CONFIRM_CANCEL,
        )
    }
}
