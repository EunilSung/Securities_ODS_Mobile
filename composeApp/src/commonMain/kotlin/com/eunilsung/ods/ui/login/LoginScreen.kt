package com.eunilsung.ods.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.util.AppLockPolicy
import com.eunilsung.ods.domain.util.LoginPolicy
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.util.formatOrderTime
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.app_name
import ods.composeapp.generated.resources.login_button
import ods.composeapp.generated.resources.login_data_wiped
import ods.composeapp.generated.resources.login_demo_hint
import ods.composeapp.generated.resources.login_error_locked
import ods.composeapp.generated.resources.login_error_unreachable
import ods.composeapp.generated.resources.login_error_wrong
import ods.composeapp.generated.resources.login_hide_password
import ods.composeapp.generated.resources.login_id
import ods.composeapp.generated.resources.login_password
import ods.composeapp.generated.resources.login_save_id
import ods.composeapp.generated.resources.login_show_password
import ods.composeapp.generated.resources.login_tagline
import ods.composeapp.generated.resources.logo_icon
import ods.composeapp.generated.resources.visibility_icon
import ods.composeapp.generated.resources.visibility_off_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 로그인 화면 — 로그인 전에는 앱 전체를 덮는다(Talk 와 같이 메인 위에 올린다).
 *
 * 화면 전체를 칠해 뒤 화면이 비치지 않게 하고, 키보드가 올라오면 입력칸이 가리지 않게 밀어 올린다.
 */
@Composable
fun LoginScreen() {
    val viewModel: LoginViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    LoginContent(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
fun LoginContent(uiState: LoginUiState, onAction: (LoginActions) -> Unit) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Bg)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Space24, vertical = Dimens.Space24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.Space12),
        ) {
            Brand()
            Spacer(Modifier.height(Dimens.Space16))

            if (uiState.dataWiped) {
                Text(
                    text = stringResource(Res.string.login_data_wiped, AppLockPolicy.MAX_ATTEMPTS.toString()),
                    style = AppTypography.Body,
                    color = AppColors.Caution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Gray50Bg)
                        .padding(Dimens.Space12),
                )
            }

            LoginField(
                value = uiState.id,
                onValueChange = { onAction(LoginActions.OnIdChange(it)) },
                label = stringResource(Res.string.login_id),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                testTag = TestTags.LOGIN_ID,
            )
            LoginField(
                value = uiState.password,
                onValueChange = { onAction(LoginActions.OnPasswordChange(it)) },
                label = stringResource(Res.string.login_password),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onAction(LoginActions.OnLoginClick)
                }),
                testTag = TestTags.LOGIN_PW,
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Icon(
                        painter = painterResource(if (uiState.isPasswordVisible) Res.drawable.visibility_off_icon else Res.drawable.visibility_icon),
                        contentDescription = stringResource(if (uiState.isPasswordVisible) Res.string.login_hide_password else Res.string.login_show_password),
                        tint = AppColors.TextSub,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(cornerRadius = 20.dp, onClick = { onAction(LoginActions.OnTogglePasswordVisible) })
                            .padding(9.dp),
                    )
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(cornerRadius = 8.dp, onClick = { onAction(LoginActions.OnSaveIdChange(!uiState.saveId)) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = uiState.saveId,
                    onCheckedChange = { onAction(LoginActions.OnSaveIdChange(it)) },
                    colors = CheckboxDefaults.colors(checkedColor = AppColors.PrimaryMain),
                )
                Text(text = stringResource(Res.string.login_save_id), style = AppTypography.Body, color = AppColors.Text)
            }

            uiState.error?.let { error ->
                Text(
                    text = error.message(),
                    style = AppTypography.Body,
                    color = AppColors.Caution,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LoginButton(enabled = uiState.canSubmit, loading = uiState.isLoading) {
                focusManager.clearFocus()
                onAction(LoginActions.OnLoginClick)
            }

            if (uiState.usesDemoData) {
                Text(
                    text = stringResource(Res.string.login_demo_hint),
                    style = AppTypography.Caption,
                    color = AppColors.TextSub,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = Dimens.Space8)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Gray50Bg)
                        .padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
                )
            }
        }
    }
}

/** 브랜드 — 남색 상자 안 차트 아이콘, 앱 이름, 한 줄 소개. */
@Composable
private fun Brand() {
    Box(
        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(AppColors.PrimaryMain),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(Res.drawable.logo_icon), contentDescription = null, tint = AppColors.White, modifier = Modifier.size(44.dp))
    }
    Text(text = stringResource(Res.string.app_name), style = AppTypography.PriceHero, color = AppColors.Text)
    Text(text = stringResource(Res.string.login_tagline), style = AppTypography.Body, color = AppColors.TextSub)
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    testTag: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.PrimaryMain,
            focusedLabelColor = AppColors.PrimaryMain,
            unfocusedBorderColor = AppColors.Line,
            cursorColor = AppColors.PrimaryMain,
            focusedTextColor = AppColors.Text,
            unfocusedTextColor = AppColors.Text,
            unfocusedLabelColor = AppColors.TextSub,
        ),
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
}

@Composable
private fun LoginButton(enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = Dimens.Space4)
            .fillMaxWidth()
            .height(52.dp)
            .testTag(TestTags.LOGIN_SUBMIT)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled || loading) AppColors.PrimaryMain else AppColors.Gray100Bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = AppColors.White)
        } else {
            Text(
                text = stringResource(Res.string.login_button),
                style = AppTypography.Section,
                fontWeight = FontWeight.Bold,
                color = if (enabled) AppColors.White else AppColors.TextDisabled,
            )
        }
    }
}

@Composable
private fun LoginError.message(): String = when (this) {
    is LoginError.Wrong -> stringResource(Res.string.login_error_wrong, LoginPolicy.MAX_ATTEMPTS.toString(), remainingAttempts.toString())
    is LoginError.Locked -> stringResource(Res.string.login_error_locked, LoginPolicy.MAX_ATTEMPTS.toString(), formatOrderTime(untilEpochSeconds))
    LoginError.Unreachable -> stringResource(Res.string.login_error_unreachable)
}
