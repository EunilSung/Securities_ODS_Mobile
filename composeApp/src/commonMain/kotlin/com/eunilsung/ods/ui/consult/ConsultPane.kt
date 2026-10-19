package com.eunilsung.ods.ui.consult

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.ui.customer.color
import com.eunilsung.ods.ui.customer.consultation.ConsultationActions
import com.eunilsung.ods.ui.customer.consultation.ConsultationForm
import com.eunilsung.ods.ui.customer.consultation.ConsultationItem
import com.eunilsung.ods.ui.customer.consultation.ConsultationViewModel
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.badge.Badge
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.section.SectionGap
import com.eunilsung.ods.ui.uikit.section.SectionHeader
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.consult_pane_empty
import ods.composeapp.generated.resources.consult_pane_end
import ods.composeapp.generated.resources.consult_pane_note
import ods.composeapp.generated.resources.consult_pane_recent
import ods.composeapp.generated.resources.consult_pane_title
import ods.composeapp.generated.resources.risk_none
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 넓은 태블릿 오른쪽 끝의 상담 칸. 상담 중인 고객이 없으면 안내만 보인다. */
@Composable
fun ConsultPane() {
    val viewModel: ConsultPaneViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val formViewModel: ConsultationViewModel = koinViewModel(key = "consult-pane-form")
    val formState by formViewModel.uiState.collectAsState()
    val customerId = uiState.customer?.id

    LaunchedEffect(customerId) {
        if (customerId != null) formViewModel.onAction(ConsultationActions.OnEnter(customerId))
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding().testTag(TestTags.CONSULT_PANE)) {
        TopBar(title = stringResource(Res.string.consult_pane_title))
        LineDivider()
        val customer = uiState.customer
        if (customer == null) {
            Box(Modifier.fillMaxSize().padding(Dimens.Space24), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.consult_pane_empty),
                    style = AppTypography.Body,
                    color = AppColors.TextSub,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            CustomerSummary(customer, onEnd = { viewModel.onAction(ConsultPaneActions.OnEndConsult) })
            SectionGap()
            SectionHeader(stringResource(Res.string.consult_pane_note))
            ConsultationForm(
                uiState = formState,
                onAction = formViewModel::onAction,
                minContentHeight = 120.dp,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.Space16),
            )
            if (uiState.recent.isNotEmpty()) {
                SectionGap()
                SectionHeader(stringResource(Res.string.consult_pane_recent))
                uiState.recent.forEach { consultation ->
                    ConsultationItem(consultation)
                    LineDivider(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))
                }
            }
        }
    }
}

@Composable
private fun CustomerSummary(customer: Customer, onEnd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = customer.name, style = AppTypography.Title, color = AppColors.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Badge(text = customer.grade.label(), color = AppColors.TextSub)
                val profile = customer.riskProfile
                if (profile == null) Badge(text = stringResource(Res.string.risk_none), color = AppColors.Caution)
                else Badge(text = profile.label(), color = profile.color)
            }
        }
        OutlineButton(text = stringResource(Res.string.consult_pane_end), onClick = onEnd)
    }
}
