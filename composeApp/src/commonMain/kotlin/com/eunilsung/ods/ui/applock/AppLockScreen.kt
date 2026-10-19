package com.eunilsung.ods.ui.applock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.util.AppLockPolicy
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.pin.SecurePinPad
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.app_lock_biometric_failed
import ods.composeapp.generated.resources.app_lock_biometric_negative
import ods.composeapp.generated.resources.app_lock_biometric_subtitle
import ods.composeapp.generated.resources.app_lock_biometric_waiting
import ods.composeapp.generated.resources.app_lock_change_current_title
import ods.composeapp.generated.resources.app_lock_close
import ods.composeapp.generated.resources.app_lock_confirm_subtitle
import ods.composeapp.generated.resources.app_lock_confirm_title
import ods.composeapp.generated.resources.app_lock_delete
import ods.composeapp.generated.resources.app_lock_enter_subtitle
import ods.composeapp.generated.resources.app_lock_logout
import ods.composeapp.generated.resources.app_lock_mismatch
import ods.composeapp.generated.resources.app_lock_new_subtitle
import ods.composeapp.generated.resources.app_lock_new_title
import ods.composeapp.generated.resources.app_lock_register_title
import ods.composeapp.generated.resources.app_lock_same_as_current
import ods.composeapp.generated.resources.app_lock_title
import ods.composeapp.generated.resources.app_lock_use_biometric
import ods.composeapp.generated.resources.app_lock_use_biometric_on_register
import ods.composeapp.generated.resources.app_lock_weak
import ods.composeapp.generated.resources.app_lock_wrong
import ods.composeapp.generated.resources.close_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 잠금 화면 — 업무 화면 전체를 덮는다. 뒤 화면이 비치지 않게 화면 전체를 칠한다.
 *
 * @param purpose 풀기·등록·변경 중 무엇인지. 바뀌면 처음부터 다시 시작한다.
 * @param onClose 변경 창의 닫기. 풀기·등록은 닫을 수 없어 null.
 * @param onPinChanged 변경을 마쳤다.
 */
@Composable
fun AppLockScreen(
    purpose: AppLockPurpose,
    onClose: (() -> Unit)? = null,
    onPinChanged: () -> Unit = {},
) {
    val viewModel: AppLockViewModel = koinViewModel(key = "app-lock-${purpose.name}")
    val uiState by viewModel.uiState.collectAsState()
    val prompt = BiometricPromptText(
        title = stringResource(Res.string.app_lock_title),
        subtitle = stringResource(Res.string.app_lock_biometric_subtitle),
        negative = stringResource(Res.string.app_lock_biometric_negative),
    )

    LaunchedEffect(purpose) { viewModel.onAction(AppLockActions.OnStart(purpose, prompt)) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AppLockEvent.PinChanged -> onPinChanged()
            }
        }
    }

    AppLockContent(uiState = uiState, prompt = prompt, onAction = viewModel::onAction, onClose = onClose)
}

@Composable
fun AppLockContent(
    uiState: AppLockUiState,
    prompt: BiometricPromptText,
    onAction: (AppLockActions) -> Unit,
    onClose: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Bg)
            .testTag(TestTags.APP_LOCK_SCREEN)
            .clickable(rippleColor = null, onClick = {})
            .safeDrawingPadding(),
    ) {
        if (onClose != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.Space8)
                    .size(44.dp)
                    .clickable(cornerRadius = 22.dp, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.close_icon),
                    contentDescription = stringResource(Res.string.app_lock_close),
                    tint = AppColors.Text,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Space24, vertical = Dimens.Space24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val step = uiState.step) {
                AppLockStep.Loading -> CircularProgressIndicator()
                AppLockStep.Biometric -> {
                    Heading(uiState.staffName, stringResource(Res.string.app_lock_title), stringResource(Res.string.app_lock_biometric_waiting))
                    Spacer(Modifier.height(Dimens.Space24))
                    CircularProgressIndicator()
                }
                is AppLockStep.EnterPin -> {
                    Heading(
                        staffName = uiState.staffName,
                        title = stringResource(
                            if (uiState.purpose == AppLockPurpose.CHANGE) Res.string.app_lock_change_current_title else Res.string.app_lock_title
                        ),
                        subtitle = step.notice?.message() ?: stringResource(Res.string.app_lock_enter_subtitle, AppLockPolicy.LENGTH.toString()),
                        subtitleColor = if (step.notice != null) AppColors.Caution else AppColors.TextSub,
                    )
                    PinPad(uiState, onAction)
                    if (uiState.showsBiometricButton) {
                        LinkButton(stringResource(Res.string.app_lock_use_biometric)) { onAction(AppLockActions.OnBiometricClick(prompt)) }
                    }
                }
                is AppLockStep.NewPin -> {
                    Heading(
                        staffName = uiState.staffName,
                        title = stringResource(
                            if (uiState.purpose == AppLockPurpose.REGISTER) Res.string.app_lock_register_title else Res.string.app_lock_new_title
                        ),
                        subtitle = step.notice?.message() ?: stringResource(Res.string.app_lock_new_subtitle, AppLockPolicy.LENGTH.toString()),
                        subtitleColor = if (step.notice != null) AppColors.Caution else AppColors.TextSub,
                    )
                    PinPad(uiState, onAction)
                    if (uiState.offersBiometricOnRegister) {
                        Row(
                            modifier = Modifier
                                .padding(top = Dimens.Space8)
                                .clickable(cornerRadius = 8.dp, onClick = { onAction(AppLockActions.OnUseBiometricChange(!uiState.useBiometricOnRegister)) }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = uiState.useBiometricOnRegister,
                                onCheckedChange = { onAction(AppLockActions.OnUseBiometricChange(it)) },
                                colors = CheckboxDefaults.colors(checkedColor = AppColors.PrimaryMain),
                            )
                            Text(text = stringResource(Res.string.app_lock_use_biometric_on_register), style = AppTypography.Body, color = AppColors.Text)
                        }
                    }
                }
                AppLockStep.ConfirmPin -> {
                    Heading(uiState.staffName, stringResource(Res.string.app_lock_confirm_title), stringResource(Res.string.app_lock_confirm_subtitle))
                    PinPad(uiState, onAction)
                }
            }
            if (uiState.purpose != AppLockPurpose.CHANGE && uiState.step != AppLockStep.Loading) {
                LinkButton(
                    text = stringResource(Res.string.app_lock_logout),
                    color = AppColors.TextSub,
                    modifier = Modifier.testTag(TestTags.APP_LOCK_LOGOUT),
                ) { onAction(AppLockActions.OnLogoutClick) }
            }
        }
    }
}

@Composable
private fun Heading(staffName: String, title: String, subtitle: String, subtitleColor: Color = AppColors.TextSub) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Space8),
    ) {
        if (staffName.isNotBlank()) {
            Text(text = staffName, style = AppTypography.Section, color = AppColors.PrimaryMain)
        }
        Text(text = title, style = AppTypography.Title, color = AppColors.Text, textAlign = TextAlign.Center)
        Text(text = subtitle, style = AppTypography.Body, color = subtitleColor, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PinPad(uiState: AppLockUiState, onAction: (AppLockActions) -> Unit) {
    Spacer(Modifier.height(32.dp))
    SecurePinPad(
        length = AppLockPolicy.LENGTH,
        onEntered = { onAction(AppLockActions.OnPinEntered(it)) },
        resetKey = uiState.errorKey,
        deleteLabel = stringResource(Res.string.app_lock_delete),
    )
}

@Composable
private fun LinkButton(
    text: String,
    color: Color = AppColors.PrimaryMain,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = AppTypography.Body,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .padding(top = Dimens.Space16)
            .clickable(cornerRadius = 8.dp, onClick = onClick)
            .padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
    )
}

@Composable
private fun PinNotice.message(): String = when (this) {
    is PinNotice.Wrong -> stringResource(Res.string.app_lock_wrong, remainingAttempts.toString())
    PinNotice.Weak -> stringResource(Res.string.app_lock_weak)
    PinNotice.Mismatch -> stringResource(Res.string.app_lock_mismatch)
    PinNotice.SameAsCurrent -> stringResource(Res.string.app_lock_same_as_current)
    PinNotice.BiometricFailed -> stringResource(Res.string.app_lock_biometric_failed)
}
