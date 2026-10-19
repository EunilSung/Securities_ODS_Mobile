package com.eunilsung.ods.ui.orderauth

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eunilsung.ods.domain.util.OrderPinPolicy
import com.eunilsung.ods.ui.orderauth.component.SecurePinPad
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.util.formatOrderTime
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.close_icon
import ods.composeapp.generated.resources.order_auth_biometric_failed
import ods.composeapp.generated.resources.order_auth_biometric_negative
import ods.composeapp.generated.resources.order_auth_biometric_subtitle
import ods.composeapp.generated.resources.order_auth_biometric_waiting
import ods.composeapp.generated.resources.order_auth_cancel
import ods.composeapp.generated.resources.order_auth_confirm_subtitle
import ods.composeapp.generated.resources.order_auth_confirm_title
import ods.composeapp.generated.resources.order_auth_current_subtitle
import ods.composeapp.generated.resources.order_auth_current_title
import ods.composeapp.generated.resources.order_auth_delete
import ods.composeapp.generated.resources.order_auth_enter_subtitle
import ods.composeapp.generated.resources.order_auth_enter_title
import ods.composeapp.generated.resources.order_auth_locked_body
import ods.composeapp.generated.resources.order_auth_locked_title
import ods.composeapp.generated.resources.order_auth_mismatch
import ods.composeapp.generated.resources.order_auth_new_title
import ods.composeapp.generated.resources.order_auth_register_subtitle
import ods.composeapp.generated.resources.order_auth_register_title
import ods.composeapp.generated.resources.order_auth_same_as_current
import ods.composeapp.generated.resources.order_auth_title
import ods.composeapp.generated.resources.order_auth_use_biometric
import ods.composeapp.generated.resources.order_auth_use_biometric_on_register
import ods.composeapp.generated.resources.order_auth_weak
import ods.composeapp.generated.resources.order_auth_wrong
import ods.composeapp.generated.resources.order_done
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 주문 인증 ViewModel 키. 주문 화면·미체결 화면이 같은 것을 쓴다(한 번에 하나만 뜬다). */
private const val ORDER_AUTH_VIEW_MODEL_KEY = "order-auth"

/**
 * 주문 인증 창 — 확인 화면에서 보내기를 누르면 그 위에 뜬다. 끝나면 [onAuthorized], 닫으면 [onCancel].
 *
 * 화면 전체를 덮어 뒤 내용이 비치지 않게 한다. 보내기 직전에 여는 창이라 화면이 다시 그려져도 처음부터 다시 인증한다.
 *
 * @param purpose 주문 인증, 설정 전 본인 확인, 비밀번호 변경 중 무엇인지.
 */
@Composable
fun OrderAuthDialog(
    onAuthorized: () -> Unit,
    onCancel: () -> Unit,
    purpose: OrderAuthPurpose = OrderAuthPurpose.AUTHORIZE,
) {
    val viewModel: OrderAuthViewModel = koinViewModel(key = ORDER_AUTH_VIEW_MODEL_KEY)
    val uiState by viewModel.uiState.collectAsState()
    val prompt = BiometricPromptText(
        title = stringResource(Res.string.order_auth_title),
        subtitle = stringResource(Res.string.order_auth_biometric_subtitle),
        negative = stringResource(Res.string.order_auth_biometric_negative),
    )
    val cancel = {
        viewModel.onAction(OrderAuthActions.OnCancel)
        onCancel()
    }

    LaunchedEffect(Unit) { viewModel.onAction(OrderAuthActions.OnStart(prompt, purpose)) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OrderAuthEvent.Authorized -> onAuthorized()
            }
        }
    }

    Dialog(
        onDismissRequest = cancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        OrderAuthContent(
            uiState = uiState,
            onAction = viewModel::onAction,
            onCancel = cancel,
            prompt = prompt,
        )
    }
}

@Composable
private fun OrderAuthContent(
    uiState: OrderAuthUiState,
    onAction: (OrderAuthActions) -> Unit,
    onCancel: () -> Unit,
    prompt: BiometricPromptText,
) {
    Box(modifier = Modifier.fillMaxSize().background(AppColors.Bg).safeDrawingPadding()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Dimens.Space8)
                .size(44.dp)
                .clickable(cornerRadius = 22.dp, onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.close_icon),
                contentDescription = stringResource(Res.string.order_auth_cancel),
                tint = AppColors.Text,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = Dimens.Space24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val step = uiState.step) {
                OrderAuthStep.Loading -> CircularProgressIndicator()
                OrderAuthStep.Biometric -> {
                    Heading(stringResource(Res.string.order_auth_title), stringResource(Res.string.order_auth_biometric_waiting))
                    Spacer(Modifier.height(Dimens.Space24))
                    CircularProgressIndicator()
                }
                is OrderAuthStep.EnterPin -> {
                    val asksCurrent = uiState.purpose != OrderAuthPurpose.AUTHORIZE
                    Heading(
                        title = stringResource(if (asksCurrent) Res.string.order_auth_current_title else Res.string.order_auth_enter_title),
                        subtitle = step.notice?.message()
                            ?: stringResource(if (asksCurrent) Res.string.order_auth_current_subtitle else Res.string.order_auth_enter_subtitle),
                        subtitleColor = if (step.notice != null) AppColors.Caution else AppColors.TextSub,
                    )
                    PinPad(uiState, onAction)
                    if (uiState.purpose == OrderAuthPurpose.AUTHORIZE && uiState.biometricEnabled && uiState.biometricAvailable) {
                        TextButton(stringResource(Res.string.order_auth_use_biometric)) {
                            onAction(OrderAuthActions.OnRetryBiometric(prompt))
                        }
                    }
                }
                is OrderAuthStep.RegisterPin -> {
                    Heading(
                        title = stringResource(if (uiState.isSettingNewPin) Res.string.order_auth_new_title else Res.string.order_auth_register_title),
                        subtitle = step.notice?.message() ?: stringResource(Res.string.order_auth_register_subtitle, OrderPinPolicy.LENGTH.toString()),
                        subtitleColor = if (step.notice != null) AppColors.Caution else AppColors.TextSub,
                    )
                    PinPad(uiState, onAction)
                    if (uiState.offersBiometricOnRegister) {
                        Row(
                            modifier = Modifier
                                .padding(top = Dimens.Space8)
                                .clickable(cornerRadius = 8.dp, onClick = {
                                    onAction(OrderAuthActions.OnUseBiometricChange(!uiState.useBiometricOnRegister))
                                }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = uiState.useBiometricOnRegister,
                                onCheckedChange = { onAction(OrderAuthActions.OnUseBiometricChange(it)) },
                                colors = CheckboxDefaults.colors(checkedColor = AppColors.PrimaryMain),
                            )
                            Text(text = stringResource(Res.string.order_auth_use_biometric_on_register), style = AppTypography.Body, color = AppColors.Text)
                        }
                    }
                }
                OrderAuthStep.ConfirmPin -> {
                    Heading(stringResource(Res.string.order_auth_confirm_title), stringResource(Res.string.order_auth_confirm_subtitle))
                    PinPad(uiState, onAction)
                }
                is OrderAuthStep.Locked -> {
                    Heading(
                        title = stringResource(Res.string.order_auth_locked_title),
                        subtitle = stringResource(
                            Res.string.order_auth_locked_body,
                            OrderPinPolicy.MAX_ATTEMPTS.toString(),
                            formatOrderTime(step.untilEpochSeconds),
                        ),
                        subtitleColor = AppColors.Caution,
                    )
                    Spacer(Modifier.height(Dimens.Space24))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.PrimaryMain)
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = stringResource(Res.string.order_done), style = AppTypography.Section, fontWeight = FontWeight.Bold, color = AppColors.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(title: String, subtitle: String, subtitleColor: Color = AppColors.TextSub) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Space8),
    ) {
        Text(text = title, style = AppTypography.Title, color = AppColors.Text, textAlign = TextAlign.Center)
        Text(text = subtitle, style = AppTypography.Body, color = subtitleColor, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PinPad(uiState: OrderAuthUiState, onAction: (OrderAuthActions) -> Unit) {
    Spacer(Modifier.height(32.dp))
    SecurePinPad(
        onEntered = { onAction(OrderAuthActions.OnPinEntered(it)) },
        resetKey = uiState.errorKey,
        deleteLabel = stringResource(Res.string.order_auth_delete),
    )
}

@Composable
private fun TextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = AppTypography.Body,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.PrimaryMain,
        modifier = Modifier
            .padding(top = Dimens.Space16)
            .clickable(cornerRadius = 8.dp, onClick = onClick)
            .padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
    )
}

@Composable
private fun PinNotice.message(): String = when (this) {
    is PinNotice.Wrong -> stringResource(Res.string.order_auth_wrong, remainingAttempts.toString())
    PinNotice.Weak -> stringResource(Res.string.order_auth_weak)
    PinNotice.Mismatch -> stringResource(Res.string.order_auth_mismatch)
    PinNotice.SameAsCurrent -> stringResource(Res.string.order_auth_same_as_current)
    PinNotice.BiometricFailed -> stringResource(Res.string.order_auth_biometric_failed)
}
