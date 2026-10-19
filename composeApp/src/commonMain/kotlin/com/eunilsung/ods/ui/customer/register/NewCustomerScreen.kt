package com.eunilsung.ods.ui.customer.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.customer.list.openCustomerDetail
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.button.PrimaryButton
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import com.eunilsung.ods.ui.uikit.textfield.FormField
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.customer_address
import ods.composeapp.generated.resources.customer_birth
import ods.composeapp.generated.resources.customer_gender
import ods.composeapp.generated.resources.customer_name
import ods.composeapp.generated.resources.customer_new
import ods.composeapp.generated.resources.customer_phone
import ods.composeapp.generated.resources.new_customer_birth_error
import ods.composeapp.generated.resources.new_customer_birth_hint
import ods.composeapp.generated.resources.new_customer_name_error
import ods.composeapp.generated.resources.new_customer_note
import ods.composeapp.generated.resources.new_customer_phone_error
import ods.composeapp.generated.resources.new_customer_phone_hint
import ods.composeapp.generated.resources.new_customer_submit
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 신규 고객 등록 — 오른쪽 스택에 뜬다. 등록하면 그 고객의 상세로 바뀐다. */
class NewCustomerScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: NewCustomerViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is NewCustomerEvent.Registered -> rightNavigator.openCustomerDetail(event.customerId)
                }
            }
        }

        NewCustomerContent(uiState = uiState, onAction = viewModel::onAction, onBack = { rightNavigator.popToEmptyRoot() })
    }
}

@Composable
fun NewCustomerContent(uiState: NewCustomerUiState, onAction: (NewCustomerActions) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding().imePadding()) {
        TopBar(title = stringResource(Res.string.customer_new), onBack = onBack)
        LineDivider()
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space8),
        ) {
            Text(text = stringResource(Res.string.new_customer_note), style = AppTypography.Caption, color = AppColors.TextSub)
            FormField(
                value = uiState.name,
                onValueChange = { onAction(NewCustomerActions.OnNameChange(it)) },
                label = stringResource(Res.string.customer_name),
                testTag = TestTags.NEW_CUSTOMER_NAME,
                error = if (uiState.showErrors && uiState.nameError) stringResource(Res.string.new_customer_name_error) else null,
            )
            FormField(
                value = uiState.birthDigits,
                onValueChange = { onAction(NewCustomerActions.OnBirthChange(it)) },
                label = stringResource(Res.string.customer_birth),
                placeholder = stringResource(Res.string.new_customer_birth_hint),
                testTag = TestTags.NEW_CUSTOMER_BIRTH,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                error = if (uiState.showErrors && uiState.birthError) stringResource(Res.string.new_customer_birth_error) else null,
            )
            Text(text = stringResource(Res.string.customer_gender), style = AppTypography.Caption, color = AppColors.TextSub)
            SegmentedControl(
                options = Gender.entries,
                selected = uiState.gender,
                label = { it.label() },
                onSelect = { onAction(NewCustomerActions.OnGenderChange(it)) },
            )
            FormField(
                value = uiState.phoneDigits,
                onValueChange = { onAction(NewCustomerActions.OnPhoneChange(it)) },
                label = stringResource(Res.string.customer_phone),
                placeholder = stringResource(Res.string.new_customer_phone_hint),
                testTag = TestTags.NEW_CUSTOMER_PHONE,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                error = if (uiState.showErrors && uiState.phoneError) stringResource(Res.string.new_customer_phone_error) else null,
            )
            FormField(
                value = uiState.address,
                onValueChange = { onAction(NewCustomerActions.OnAddressChange(it)) },
                label = stringResource(Res.string.customer_address),
                testTag = TestTags.NEW_CUSTOMER_ADDRESS,
            )
            PrimaryButton(
                text = stringResource(Res.string.new_customer_submit),
                onClick = { onAction(NewCustomerActions.OnSubmit) },
                loading = uiState.isSaving,
                modifier = Modifier.padding(top = Dimens.Space8).testTag(TestTags.NEW_CUSTOMER_SUBMIT),
            )
        }
    }
}
