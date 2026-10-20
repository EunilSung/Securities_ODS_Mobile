package com.eunilsung.ods.ui.product.subscribe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.Suitability
import com.eunilsung.ods.domain.util.SubscriptionPolicy
import com.eunilsung.ods.domain.util.maskAccountNo
import com.eunilsung.ods.ui.customer.displayPhone
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popOrClose
import com.eunilsung.ods.ui.product.SuitabilityCard
import com.eunilsung.ods.ui.product.label
import com.eunilsung.ods.ui.risk.RiskAssessmentScreen
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import com.eunilsung.ods.ui.uikit.button.PrimaryButton
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.section.InfoRow
import com.eunilsung.ods.ui.uikit.signature.SignaturePad
import com.eunilsung.ods.ui.uikit.textfield.NumberField
import com.eunilsung.ods.ui.uikit.textfield.SearchField
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import com.eunilsung.ods.ui.util.formatDateTime
import com.eunilsung.ods.util.withComma
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.customer_search_hint
import ods.composeapp.generated.resources.error_server_unreachable
import ods.composeapp.generated.resources.risk_assess
import ods.composeapp.generated.resources.risk_next
import ods.composeapp.generated.resources.risk_previous
import ods.composeapp.generated.resources.risk_result_sign
import ods.composeapp.generated.resources.subscription_account
import ods.composeapp.generated.resources.subscription_amount
import ods.composeapp.generated.resources.subscription_amount_rule
import ods.composeapp.generated.resources.subscription_close
import ods.composeapp.generated.resources.subscription_cooling_off
import ods.composeapp.generated.resources.subscription_cooling_off_until
import ods.composeapp.generated.resources.subscription_customer
import ods.composeapp.generated.resources.subscription_done
import ods.composeapp.generated.resources.subscription_error_account
import ods.composeapp.generated.resources.subscription_error_amount
import ods.composeapp.generated.resources.subscription_error_assessment
import ods.composeapp.generated.resources.subscription_error_not_found
import ods.composeapp.generated.resources.subscription_error_not_signed_in
import ods.composeapp.generated.resources.subscription_error_signature
import ods.composeapp.generated.resources.subscription_error_unsuitable
import ods.composeapp.generated.resources.subscription_explain_note
import ods.composeapp.generated.resources.subscription_no_account
import ods.composeapp.generated.resources.subscription_note
import ods.composeapp.generated.resources.subscription_product
import ods.composeapp.generated.resources.subscription_step
import ods.composeapp.generated.resources.subscription_step_customer
import ods.composeapp.generated.resources.subscription_step_details
import ods.composeapp.generated.resources.subscription_step_explanation
import ods.composeapp.generated.resources.subscription_step_sign
import ods.composeapp.generated.resources.subscription_step_suitability
import ods.composeapp.generated.resources.subscription_submit
import ods.composeapp.generated.resources.subscription_title
import ods.composeapp.generated.resources.subscription_unsuitable_confirm
import ods.composeapp.generated.resources.won
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 상품 가입 절차 — 상품 상세 위에 쌓인다. 끝나면 완료 화면을 보이고 닫으면 상품 상세로 돌아간다.
 *
 * @param customerId 상담 중 고객. 있으면 고객 고르기를 건너뛴다.
 */
data class SubscriptionScreen(val productId: String, val customerId: String? = null) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: SubscriptionViewModel = koinViewModel(key = "subscription-$key")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current

        LaunchedEffect(Unit) { viewModel.onAction(SubscriptionActions.OnEnter(productId, customerId)) }
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is SubscriptionEvent.OpenRiskAssessment -> rightNavigator.push(RiskAssessmentScreen(event.customerId))
                }
            }
        }

        SubscriptionContent(uiState, onAction = viewModel::onAction, onClose = { rightNavigator.popOrClose() })
    }
}

@Composable
fun SubscriptionContent(uiState: SubscriptionUiState, onAction: (SubscriptionActions) -> Unit, onClose: () -> Unit) {
    val product = uiState.product
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding().imePadding()) {
        TopBar(title = stringResource(Res.string.subscription_title), onBack = onClose)
        LineDivider()
        if (product == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        if (uiState.step != SubscriptionStep.DONE) StepHeader(uiState.step)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
        ) {
            Text(text = product.name, style = AppTypography.Section, color = AppColors.TextSub)
            when (uiState.step) {
                SubscriptionStep.CUSTOMER -> CustomerStep(uiState, onAction)
                SubscriptionStep.SUITABILITY -> SuitabilityStep(uiState, product, onAction)
                SubscriptionStep.EXPLANATION -> ExplanationStep(uiState, product, onAction)
                SubscriptionStep.DETAILS -> DetailsStep(uiState, product, onAction)
                SubscriptionStep.SIGN -> SignStep(uiState, product, onAction)
                SubscriptionStep.DONE -> DoneStep(uiState, product)
            }
            uiState.error?.let { ErrorText(it.message()) }
        }
        LineDivider()
        BottomButtons(uiState, product, onAction, onClose)
    }
}

@Composable
private fun StepHeader(step: SubscriptionStep) {
    val names = listOf(
        Res.string.subscription_step_customer,
        Res.string.subscription_step_suitability,
        Res.string.subscription_step_explanation,
        Res.string.subscription_step_details,
        Res.string.subscription_step_sign,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.subscription_step, step.ordinal + 1, names.size),
            style = AppTypography.CaptionNumber,
            color = AppColors.TextSub,
            modifier = Modifier.padding(end = Dimens.Space8),
        )
        Text(text = stringResource(names[step.ordinal]), style = AppTypography.Section, color = AppColors.PrimaryMain)
    }
    LineDivider()
}

@Composable
private fun ColumnScope.CustomerStep(uiState: SubscriptionUiState, onAction: (SubscriptionActions) -> Unit) {
    SearchField(
        value = uiState.customerQuery,
        onValueChange = { onAction(SubscriptionActions.OnCustomerQueryChange(it)) },
        placeholder = stringResource(Res.string.customer_search_hint),
        testTag = TestTags.SUBSCRIPTION_CUSTOMER_SEARCH,
    )
    uiState.customerMatches.forEach { customer ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.SUBSCRIPTION_CUSTOMER)
                .clickable(cornerRadius = 8.dp, onClick = { onAction(SubscriptionActions.OnCustomerSelect(customer.id)) })
                .padding(vertical = Dimens.Space8),
        ) {
            Text(text = customer.name, style = AppTypography.Body, color = AppColors.Text, modifier = Modifier.weight(1f))
            Text(text = customer.displayPhone(mask = true), style = AppTypography.CaptionNumber, color = AppColors.TextSub)
        }
    }
}

@Composable
private fun ColumnScope.SuitabilityStep(uiState: SubscriptionUiState, product: Product, onAction: (SubscriptionActions) -> Unit) {
    val customer = uiState.customer ?: return
    val suitability = uiState.suitability ?: return
    SuitabilityCard(customer, product, suitability)
    when (suitability) {
        Suitability.NeedsAssessment -> OutlineButton(
            text = stringResource(Res.string.risk_assess),
            onClick = { onAction(SubscriptionActions.OnAssessClick) },
            modifier = Modifier.testTag(TestTags.SUBSCRIPTION_ASSESS),
        )
        is Suitability.Unsuitable -> CheckRow(
            text = stringResource(Res.string.subscription_unsuitable_confirm),
            checked = uiState.unsuitableConfirmed,
            testTag = TestTags.SUBSCRIPTION_UNSUITABLE_CONFIRM,
            onToggle = { onAction(SubscriptionActions.OnUnsuitableConfirm(!uiState.unsuitableConfirmed)) },
        )
        Suitability.Suitable -> Unit
    }
    if (uiState.needsCoolingOff) Notice(stringResource(Res.string.subscription_cooling_off, SubscriptionPolicy.COOLING_OFF_DAYS))
}

@Composable
private fun ColumnScope.ExplanationStep(uiState: SubscriptionUiState, product: Product, onAction: (SubscriptionActions) -> Unit) {
    Text(text = stringResource(Res.string.subscription_explain_note), style = AppTypography.Body, color = AppColors.TextSub)
    product.keyPoints.forEachIndexed { index, point ->
        CheckRow(
            text = point,
            checked = index in uiState.explainedPoints,
            testTag = "${TestTags.KEY_POINT_PREFIX}$index",
            onToggle = { onAction(SubscriptionActions.OnKeyPointToggle(index)) },
        )
    }
}

@Composable
private fun ColumnScope.DetailsStep(uiState: SubscriptionUiState, product: Product, onAction: (SubscriptionActions) -> Unit) {
    Text(text = stringResource(Res.string.subscription_account), style = AppTypography.Section, color = AppColors.Text)
    if (uiState.accounts.isEmpty()) {
        Notice(stringResource(Res.string.subscription_no_account))
    }
    uiState.accounts.forEach { account ->
        val selected = account.accountNo == uiState.accountNo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.SUBSCRIPTION_ACCOUNT)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, if (selected) AppColors.PrimaryMain else AppColors.Line, RoundedCornerShape(10.dp))
                .clickable(onClick = { onAction(SubscriptionActions.OnAccountSelect(account.accountNo)) })
                .padding(end = Dimens.Space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = { onAction(SubscriptionActions.OnAccountSelect(account.accountNo)) },
                colors = RadioButtonDefaults.colors(selectedColor = AppColors.PrimaryMain),
            )
            Text(text = account.type.label(), style = AppTypography.Body, color = AppColors.Text, modifier = Modifier.weight(1f))
            Text(text = maskAccountNo(account.accountNo), style = AppTypography.CaptionNumber, color = AppColors.TextSub)
        }
    }
    Text(text = stringResource(Res.string.subscription_amount), style = AppTypography.Section, color = AppColors.Text)
    NumberField(
        value = uiState.amountText,
        onValueChange = { onAction(SubscriptionActions.OnAmountChange(it)) },
        onStep = { onAction(SubscriptionActions.OnAmountStep(it)) },
        label = stringResource(Res.string.subscription_amount),
        testTag = TestTags.SUBSCRIPTION_AMOUNT,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = stringResource(Res.string.subscription_amount_rule, product.minAmount.withComma(), SubscriptionPolicy.AMOUNT_UNIT.withComma()),
        style = AppTypography.Caption,
        color = if (uiState.amountValid || uiState.amountText.isEmpty()) AppColors.TextSub else AppColors.Caution,
    )
}

@Composable
private fun ColumnScope.SignStep(uiState: SubscriptionUiState, product: Product, onAction: (SubscriptionActions) -> Unit) {
    Summary(uiState, product)
    if (uiState.needsCoolingOff) Notice(stringResource(Res.string.subscription_cooling_off, SubscriptionPolicy.COOLING_OFF_DAYS))
    Text(text = stringResource(Res.string.risk_result_sign), style = AppTypography.Section, color = AppColors.Text)
    SignaturePad(onSignatureChange = { onAction(SubscriptionActions.OnSignatureChange(it)) }, resetKey = 0)
}

@Composable
private fun ColumnScope.DoneStep(uiState: SubscriptionUiState, product: Product) {
    Text(
        text = stringResource(Res.string.subscription_done),
        style = AppTypography.Title,
        color = AppColors.PrimaryMain,
        modifier = Modifier.testTag(TestTags.SUBSCRIPTION_DONE),
    )
    Summary(uiState, product)
    uiState.result?.coolingOffUntilMillis?.let {
        Notice(stringResource(Res.string.subscription_cooling_off_until, formatDateTime(it)))
    }
}

@Composable
private fun Summary(uiState: SubscriptionUiState, product: Product) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppColors.Gray50Bg).padding(vertical = Dimens.Space8)) {
        InfoRow(stringResource(Res.string.subscription_customer), uiState.customer?.name.orEmpty())
        InfoRow(stringResource(Res.string.subscription_product), product.name)
        InfoRow(stringResource(Res.string.subscription_account), uiState.accountNo?.let(::maskAccountNo).orEmpty())
        InfoRow(stringResource(Res.string.subscription_amount), stringResource(Res.string.won, uiState.amount.withComma()))
    }
}

@Composable
private fun BottomButtons(uiState: SubscriptionUiState, product: Product, onAction: (SubscriptionActions) -> Unit, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), verticalAlignment = Alignment.CenterVertically) {
        when (uiState.step) {
            SubscriptionStep.DONE -> PrimaryButton(text = stringResource(Res.string.subscription_close), onClick = onClose, modifier = Modifier.testTag(TestTags.SUBSCRIPTION_CLOSE))
            SubscriptionStep.SIGN -> {
                OutlineButton(text = stringResource(Res.string.risk_previous), onClick = { onAction(SubscriptionActions.OnPrevious) })
                val note = stringResource(
                    Res.string.subscription_note,
                    product.name,
                    uiState.amount.withComma(),
                    uiState.accountNo?.let(::maskAccountNo).orEmpty(),
                )
                PrimaryButton(
                    text = stringResource(Res.string.subscription_submit),
                    onClick = { onAction(SubscriptionActions.OnSubmit(note)) },
                    enabled = uiState.canProceed,
                    loading = uiState.isSaving,
                    modifier = Modifier.weight(1f).testTag(TestTags.SUBSCRIPTION_SUBMIT),
                )
            }
            else -> {
                if (uiState.step != SubscriptionStep.CUSTOMER) {
                    OutlineButton(text = stringResource(Res.string.risk_previous), onClick = { onAction(SubscriptionActions.OnPrevious) })
                }
                PrimaryButton(
                    text = stringResource(Res.string.risk_next),
                    onClick = { onAction(SubscriptionActions.OnNext) },
                    enabled = uiState.canProceed,
                    modifier = Modifier.weight(1f).testTag(TestTags.SUBSCRIPTION_NEXT),
                )
            }
        }
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, testTag: String, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (checked) AppColors.PrimaryMain else AppColors.Line, RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(end = Dimens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = AppColors.PrimaryMain))
        Text(
            text = text,
            style = AppTypography.Body,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
            color = AppColors.Text,
            modifier = Modifier.padding(vertical = Dimens.Space12),
        )
    }
}

@Composable
private fun Notice(text: String) {
    Text(
        text = text,
        style = AppTypography.Body,
        color = AppColors.Caution,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AppColors.Gray50Bg).padding(Dimens.Space12),
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = AppTypography.Body, color = AppColors.Caution)
}

@Composable
private fun SubscribeError.message(): String = stringResource(
    when (this) {
        SubscribeError.NEEDS_ASSESSMENT -> Res.string.subscription_error_assessment
        SubscribeError.UNSUITABLE -> Res.string.subscription_error_unsuitable
        SubscribeError.INVALID_AMOUNT -> Res.string.subscription_error_amount
        SubscribeError.INVALID_ACCOUNT -> Res.string.subscription_error_account
        SubscribeError.SIGNATURE -> Res.string.subscription_error_signature
        SubscribeError.NOT_SIGNED_IN -> Res.string.subscription_error_not_signed_in
        SubscribeError.NOT_FOUND -> Res.string.subscription_error_not_found
        SubscribeError.UNREACHABLE -> Res.string.error_server_unreachable
    }
)
