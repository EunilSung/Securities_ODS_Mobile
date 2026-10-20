package com.eunilsung.ods.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.AccountTypeBlock
import com.eunilsung.ods.domain.model.IdCardType
import com.eunilsung.ods.domain.model.IdMismatch
import com.eunilsung.ods.domain.model.Terms
import com.eunilsung.ods.domain.util.AccountOpeningPolicy
import com.eunilsung.ods.domain.util.maskAccountNo
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popOrClose
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import com.eunilsung.ods.ui.uikit.button.PrimaryButton
import com.eunilsung.ods.ui.uikit.camera.rememberPhotoCapture
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.pin.SecurePinPad
import com.eunilsung.ods.ui.uikit.section.InfoRow
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import com.eunilsung.ods.ui.uikit.signature.SignaturePad
import com.eunilsung.ods.ui.uikit.textfield.FormField
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.app_lock_delete
import ods.composeapp.generated.resources.error_server_unreachable
import ods.composeapp.generated.resources.opening_account_no
import ods.composeapp.generated.resources.opening_block_isa_age
import ods.composeapp.generated.resources.opening_block_isa_duplicate
import ods.composeapp.generated.resources.opening_block_pension_duplicate
import ods.composeapp.generated.resources.opening_capture
import ods.composeapp.generated.resources.opening_capture_guide
import ods.composeapp.generated.resources.opening_capture_note
import ods.composeapp.generated.resources.opening_close
import ods.composeapp.generated.resources.opening_demo_id
import ods.composeapp.generated.resources.opening_demo_id_card
import ods.composeapp.generated.resources.opening_done
import ods.composeapp.generated.resources.opening_error_id
import ods.composeapp.generated.resources.opening_error_not_found
import ods.composeapp.generated.resources.opening_error_not_signed_in
import ods.composeapp.generated.resources.opening_error_pin
import ods.composeapp.generated.resources.opening_error_signature
import ods.composeapp.generated.resources.opening_error_terms
import ods.composeapp.generated.resources.opening_error_type
import ods.composeapp.generated.resources.opening_id_name
import ods.composeapp.generated.resources.opening_id_rejected
import ods.composeapp.generated.resources.opening_id_type
import ods.composeapp.generated.resources.opening_id_type_driver
import ods.composeapp.generated.resources.opening_id_type_resident
import ods.composeapp.generated.resources.opening_id_unreachable
import ods.composeapp.generated.resources.opening_id_verified
import ods.composeapp.generated.resources.opening_mismatch_birth
import ods.composeapp.generated.resources.opening_mismatch_format
import ods.composeapp.generated.resources.opening_mismatch_gender
import ods.composeapp.generated.resources.opening_mismatch_name
import ods.composeapp.generated.resources.opening_note
import ods.composeapp.generated.resources.opening_pin_confirm
import ods.composeapp.generated.resources.opening_pin_done
import ods.composeapp.generated.resources.opening_pin_enter
import ods.composeapp.generated.resources.opening_pin_guide
import ods.composeapp.generated.resources.opening_pin_mismatch
import ods.composeapp.generated.resources.opening_pin_weak
import ods.composeapp.generated.resources.opening_retake
import ods.composeapp.generated.resources.opening_rrn_back
import ods.composeapp.generated.resources.opening_rrn_front
import ods.composeapp.generated.resources.opening_step
import ods.composeapp.generated.resources.opening_step_id
import ods.composeapp.generated.resources.opening_step_identity
import ods.composeapp.generated.resources.opening_step_pin
import ods.composeapp.generated.resources.opening_step_sign
import ods.composeapp.generated.resources.opening_step_terms
import ods.composeapp.generated.resources.opening_step_type
import ods.composeapp.generated.resources.opening_submit
import ods.composeapp.generated.resources.opening_terms_account
import ods.composeapp.generated.resources.opening_terms_all
import ods.composeapp.generated.resources.opening_terms_electronic
import ods.composeapp.generated.resources.opening_terms_marketing
import ods.composeapp.generated.resources.opening_terms_optional
import ods.composeapp.generated.resources.opening_terms_personal
import ods.composeapp.generated.resources.opening_terms_required
import ods.composeapp.generated.resources.opening_title
import ods.composeapp.generated.resources.opening_type_brokerage_desc
import ods.composeapp.generated.resources.opening_type_cma_desc
import ods.composeapp.generated.resources.opening_type_isa_desc
import ods.composeapp.generated.resources.opening_type_pension_desc
import ods.composeapp.generated.resources.opening_verify
import ods.composeapp.generated.resources.risk_next
import ods.composeapp.generated.resources.risk_previous
import ods.composeapp.generated.resources.risk_result_sign
import ods.composeapp.generated.resources.subscription_customer
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 계좌 개설 — 고객 상세 위에 쌓인다. 끝나면 완료 화면을 보이고, 닫으면 상세로 돌아가 새 계좌가 보인다. */
data class AccountOpeningScreen(val customerId: String) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: AccountOpeningViewModel = koinViewModel(key = "account-opening-$key")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current
        val capture = rememberPhotoCapture { viewModel.onAction(AccountOpeningActions.OnPhotoCaptured(it)) }

        LaunchedEffect(customerId) { viewModel.onAction(AccountOpeningActions.OnEnter(customerId)) }

        AccountOpeningContent(uiState, onAction = viewModel::onAction, onCapture = capture, onClose = { rightNavigator.popOrClose() })
    }
}

@Composable
fun AccountOpeningContent(
    uiState: AccountOpeningUiState,
    onAction: (AccountOpeningActions) -> Unit,
    onCapture: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding().imePadding()) {
        TopBar(title = stringResource(Res.string.opening_title), onBack = onClose)
        LineDivider()
        if (uiState.customer == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        if (uiState.step != OpeningStep.DONE) StepHeader(uiState.step)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
        ) {
            Text(text = uiState.customer.name, style = AppTypography.Section, color = AppColors.TextSub)
            when (uiState.step) {
                OpeningStep.ID_CAPTURE -> CaptureStep(uiState, onAction, onCapture)
                OpeningStep.ID_CHECK -> IdentityStep(uiState, onAction)
                OpeningStep.ACCOUNT_TYPE -> TypeStep(uiState, onAction)
                OpeningStep.TERMS -> TermsStep(uiState, onAction)
                OpeningStep.PIN -> PinStep(uiState, onAction)
                OpeningStep.SIGN -> SignStep(uiState, onAction)
                OpeningStep.DONE -> DoneStep(uiState)
            }
            uiState.error?.let { ErrorText(it.message()) }
        }
        LineDivider()
        BottomButtons(uiState, onAction, onClose)
    }
}

@Composable
private fun StepHeader(step: OpeningStep) {
    val names = listOf(
        Res.string.opening_step_id,
        Res.string.opening_step_identity,
        Res.string.opening_step_type,
        Res.string.opening_step_terms,
        Res.string.opening_step_pin,
        Res.string.opening_step_sign,
    )
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.opening_step, step.ordinal + 1, names.size),
            style = AppTypography.CaptionNumber,
            color = AppColors.TextSub,
            modifier = Modifier.padding(end = Dimens.Space8),
        )
        Text(text = stringResource(names[step.ordinal]), style = AppTypography.Section, color = AppColors.PrimaryMain)
    }
    LineDivider()
}

@Composable
private fun ColumnScope.CaptureStep(uiState: AccountOpeningUiState, onAction: (AccountOpeningActions) -> Unit, onCapture: () -> Unit) {
    Text(text = stringResource(Res.string.opening_capture_guide), style = AppTypography.Body, color = AppColors.Text)
    IdPreview(uiState)
    Text(text = stringResource(Res.string.opening_capture_note), style = AppTypography.Caption, color = AppColors.TextSub)
    PrimaryButton(
        text = stringResource(if (uiState.hasPhoto) Res.string.opening_retake else Res.string.opening_capture),
        onClick = onCapture,
        modifier = Modifier.testTag(TestTags.OPENING_CAPTURE),
    )
    if (uiState.offersDemoId) {
        OutlineButton(
            text = stringResource(Res.string.opening_demo_id),
            onClick = { onAction(AccountOpeningActions.OnUseDemoId) },
            modifier = Modifier.testTag(TestTags.OPENING_DEMO_ID),
        )
    }
}

/** 찍은 신분증 미리보기. 시연용 신분증이면 그림 대신 카드 모양을 그린다. */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun IdPreview(uiState: AccountOpeningUiState) {
    val photo = uiState.photo
    val bitmap = remember(photo) { photo?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    Box(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Gray100Bg)
            .border(1.dp, AppColors.Line, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            uiState.isDemoId -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = stringResource(Res.string.opening_demo_id_card), style = AppTypography.Section, color = AppColors.PrimaryMain)
                Text(text = uiState.idName, style = AppTypography.Title, color = AppColors.Text)
                Text(text = "${uiState.rrnFront} - ${uiState.rrnBackFirst}●●●●●●", style = AppTypography.BodyNumber, color = AppColors.TextSub)
            }
            else -> Text(text = stringResource(Res.string.opening_capture), style = AppTypography.Body, color = AppColors.TextDisabled)
        }
    }
}

@Composable
private fun ColumnScope.IdentityStep(uiState: AccountOpeningUiState, onAction: (AccountOpeningActions) -> Unit) {
    Text(text = stringResource(Res.string.opening_id_type), style = AppTypography.Caption, color = AppColors.TextSub)
    SegmentedControl(
        options = IdCardType.entries,
        selected = uiState.idType,
        label = { stringResource(if (it == IdCardType.RESIDENT) Res.string.opening_id_type_resident else Res.string.opening_id_type_driver) },
        onSelect = { onAction(AccountOpeningActions.OnIdTypeChange(it)) },
    )
    FormField(
        value = uiState.idName,
        onValueChange = { onAction(AccountOpeningActions.OnIdNameChange(it)) },
        label = stringResource(Res.string.opening_id_name),
        testTag = TestTags.OPENING_ID_NAME,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), verticalAlignment = Alignment.CenterVertically) {
        FormField(
            value = uiState.rrnFront,
            onValueChange = { onAction(AccountOpeningActions.OnRrnFrontChange(it)) },
            label = stringResource(Res.string.opening_rrn_front),
            testTag = TestTags.OPENING_RRN_FRONT,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.weight(1.4f),
        )
        Text(text = "-", style = AppTypography.Title, color = AppColors.TextSub)
        FormField(
            value = uiState.rrnBackFirst,
            onValueChange = { onAction(AccountOpeningActions.OnRrnBackChange(it)) },
            label = stringResource(Res.string.opening_rrn_back),
            testTag = TestTags.OPENING_RRN_BACK,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.weight(1f),
        )
        Text(text = "●●●●●●", style = AppTypography.Body, color = AppColors.TextDisabled)
    }
    OutlineButton(
        text = stringResource(Res.string.opening_verify),
        onClick = { onAction(AccountOpeningActions.OnVerify) },
        enabled = uiState.canVerify,
        modifier = Modifier.testTag(TestTags.OPENING_VERIFY),
    )
    when {
        uiState.isVerifying -> CircularProgressIndicator()
        uiState.verification != null -> Text(
            text = stringResource(Res.string.opening_id_verified),
            style = AppTypography.Body,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.PrimaryMain,
            modifier = Modifier.testTag(TestTags.OPENING_ID_VERIFIED),
        )
        else -> {
            uiState.idMismatches.forEach { ErrorText(it.message()) }
            when (uiState.idError) {
                IdError.REJECTED -> ErrorText(stringResource(Res.string.opening_id_rejected))
                IdError.UNREACHABLE -> ErrorText(stringResource(Res.string.opening_id_unreachable))
                null -> Unit
            }
        }
    }
}

@Composable
private fun ColumnScope.TypeStep(uiState: AccountOpeningUiState, onAction: (AccountOpeningActions) -> Unit) {
    AccountType.entries.forEach { type ->
        val block = uiState.typeBlocks[type]
        val selected = uiState.accountType == type
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${TestTags.OPENING_TYPE_PREFIX}${type.name.lowercase()}")
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) AppColors.PrimaryBg else AppColors.Transparent)
                .border(1.dp, if (selected) AppColors.PrimaryMain else AppColors.Line, RoundedCornerShape(10.dp))
                .then(if (block == null) Modifier.clickable(onClick = { onAction(AccountOpeningActions.OnTypeSelect(type)) }) else Modifier)
                .padding(Dimens.Space16),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = type.label(), style = AppTypography.ListName, color = if (block == null) AppColors.Text else AppColors.TextDisabled)
            Text(text = type.description(), style = AppTypography.Caption, color = AppColors.TextSub)
            block?.let { Text(text = it.message(), style = AppTypography.Caption, color = AppColors.Caution) }
        }
    }
}

@Composable
private fun ColumnScope.TermsStep(uiState: AccountOpeningUiState, onAction: (AccountOpeningActions) -> Unit) {
    CheckRow(
        text = stringResource(Res.string.opening_terms_all),
        checked = uiState.agreedTerms.size == Terms.entries.size,
        bold = true,
        testTag = TestTags.OPENING_TERMS_ALL,
    ) { onAction(AccountOpeningActions.OnAgreeAll) }
    LineDivider()
    Terms.entries.forEach { terms ->
        CheckRow(
            text = "${stringResource(if (terms.required) Res.string.opening_terms_required else Res.string.opening_terms_optional)} ${terms.label()}",
            checked = terms in uiState.agreedTerms,
            testTag = "${TestTags.OPENING_TERMS_PREFIX}${terms.name.lowercase()}",
        ) { onAction(AccountOpeningActions.OnTermToggle(terms)) }
    }
}

@Composable
private fun ColumnScope.PinStep(uiState: AccountOpeningUiState, onAction: (AccountOpeningActions) -> Unit) {
    Text(text = stringResource(Res.string.opening_pin_guide), style = AppTypography.Body, color = AppColors.TextSub)
    if (uiState.pinStage == PinStage.DONE) {
        Text(
            text = stringResource(Res.string.opening_pin_done),
            style = AppTypography.Title,
            color = AppColors.PrimaryMain,
            modifier = Modifier.testTag(TestTags.OPENING_PIN_DONE),
        )
        return
    }
    Text(
        text = when (uiState.pinNotice) {
            PinNotice.WEAK -> stringResource(Res.string.opening_pin_weak)
            PinNotice.MISMATCH -> stringResource(Res.string.opening_pin_mismatch)
            null -> stringResource(if (uiState.pinStage == PinStage.ENTER) Res.string.opening_pin_enter else Res.string.opening_pin_confirm)
        },
        style = AppTypography.Section,
        color = if (uiState.pinNotice != null) AppColors.Caution else AppColors.Text,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    SecurePinPad(
        length = AccountOpeningPolicy.PIN_LENGTH,
        onEntered = { onAction(AccountOpeningActions.OnPinEntered(it)) },
        resetKey = uiState.pinResetKey,
        deleteLabel = stringResource(Res.string.app_lock_delete),
        modifier = Modifier.widthIn(max = 360.dp).align(Alignment.CenterHorizontally),
    )
}

@Composable
private fun ColumnScope.SignStep(uiState: AccountOpeningUiState, onAction: (AccountOpeningActions) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AppColors.Gray50Bg).padding(vertical = Dimens.Space8)) {
        InfoRow(stringResource(Res.string.subscription_customer), uiState.customer?.name.orEmpty())
        InfoRow(stringResource(Res.string.opening_step_type), uiState.accountType?.label().orEmpty())
        InfoRow(stringResource(Res.string.opening_id_type), stringResource(if (uiState.idType == IdCardType.RESIDENT) Res.string.opening_id_type_resident else Res.string.opening_id_type_driver))
    }
    Text(text = stringResource(Res.string.risk_result_sign), style = AppTypography.Section, color = AppColors.Text)
    SignaturePad(onSignatureChange = { onAction(AccountOpeningActions.OnSignatureChange(it)) }, resetKey = 0)
}

@Composable
private fun ColumnScope.DoneStep(uiState: AccountOpeningUiState) {
    Text(
        text = stringResource(Res.string.opening_done),
        style = AppTypography.Title,
        color = AppColors.PrimaryMain,
        modifier = Modifier.testTag(TestTags.OPENING_DONE),
    )
    uiState.opened?.let { account ->
        InfoRow(stringResource(Res.string.opening_step_type), account.type.label())
        InfoRow(stringResource(Res.string.opening_account_no), maskAccountNo(account.accountNo))
    }
}

@Composable
private fun BottomButtons(uiState: AccountOpeningUiState, onAction: (AccountOpeningActions) -> Unit, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding), horizontalArrangement = Arrangement.spacedBy(Dimens.Space8), verticalAlignment = Alignment.CenterVertically) {
        when (uiState.step) {
            OpeningStep.DONE -> PrimaryButton(text = stringResource(Res.string.opening_close), onClick = onClose, modifier = Modifier.testTag(TestTags.OPENING_CLOSE))
            OpeningStep.SIGN -> {
                OutlineButton(text = stringResource(Res.string.risk_previous), onClick = { onAction(AccountOpeningActions.OnPrevious) })
                val note = stringResource(
                    Res.string.opening_note,
                    uiState.accountType?.label().orEmpty(),
                    stringResource(if (uiState.idType == IdCardType.RESIDENT) Res.string.opening_id_type_resident else Res.string.opening_id_type_driver),
                )
                PrimaryButton(
                    text = stringResource(Res.string.opening_submit),
                    onClick = { onAction(AccountOpeningActions.OnSubmit(note)) },
                    enabled = uiState.canProceed,
                    loading = uiState.isSaving,
                    modifier = Modifier.weight(1f).testTag(TestTags.OPENING_SUBMIT),
                )
            }
            else -> {
                if (uiState.step != OpeningStep.ID_CAPTURE) {
                    OutlineButton(text = stringResource(Res.string.risk_previous), onClick = { onAction(AccountOpeningActions.OnPrevious) })
                }
                PrimaryButton(
                    text = stringResource(Res.string.risk_next),
                    onClick = { onAction(AccountOpeningActions.OnNext) },
                    enabled = uiState.canProceed,
                    modifier = Modifier.weight(1f).testTag(TestTags.OPENING_NEXT),
                )
            }
        }
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, testTag: String, bold: Boolean = false, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(testTag).clickable(cornerRadius = 8.dp, onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = AppColors.PrimaryMain))
        Text(text = text, style = AppTypography.Body, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = AppColors.Text)
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = AppTypography.Body, color = AppColors.Caution)
}

@Composable
private fun AccountType.description(): String = stringResource(
    when (this) {
        AccountType.BROKERAGE -> Res.string.opening_type_brokerage_desc
        AccountType.CMA -> Res.string.opening_type_cma_desc
        AccountType.ISA -> Res.string.opening_type_isa_desc
        AccountType.PENSION -> Res.string.opening_type_pension_desc
    }
)

@Composable
private fun AccountTypeBlock.message(): String = stringResource(
    when (this) {
        AccountTypeBlock.ISA_AGE -> Res.string.opening_block_isa_age
        AccountTypeBlock.ISA_DUPLICATE -> Res.string.opening_block_isa_duplicate
        AccountTypeBlock.PENSION_DUPLICATE -> Res.string.opening_block_pension_duplicate
    }
)

@Composable
private fun Terms.label(): String = stringResource(
    when (this) {
        Terms.ELECTRONIC_FINANCE -> Res.string.opening_terms_electronic
        Terms.PERSONAL_INFO -> Res.string.opening_terms_personal
        Terms.ACCOUNT -> Res.string.opening_terms_account
        Terms.MARKETING -> Res.string.opening_terms_marketing
    }
)

@Composable
private fun IdMismatch.message(): String = stringResource(
    when (this) {
        IdMismatch.FORMAT -> Res.string.opening_mismatch_format
        IdMismatch.NAME -> Res.string.opening_mismatch_name
        IdMismatch.BIRTH -> Res.string.opening_mismatch_birth
        IdMismatch.GENDER -> Res.string.opening_mismatch_gender
    }
)

@Composable
private fun OpenError.message(): String = stringResource(
    when (this) {
        OpenError.ID_NOT_VERIFIED -> Res.string.opening_error_id
        OpenError.TYPE_BLOCKED -> Res.string.opening_error_type
        OpenError.TERMS -> Res.string.opening_error_terms
        OpenError.PIN -> Res.string.opening_error_pin
        OpenError.SIGNATURE -> Res.string.opening_error_signature
        OpenError.NOT_SIGNED_IN -> Res.string.opening_error_not_signed_in
        OpenError.NOT_FOUND -> Res.string.opening_error_not_found
        OpenError.UNREACHABLE -> Res.string.error_server_unreachable
    }
)
