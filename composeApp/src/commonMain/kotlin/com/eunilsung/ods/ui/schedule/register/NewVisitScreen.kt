package com.eunilsung.ods.ui.schedule.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.VisitPurpose
import com.eunilsung.ods.domain.util.VisitPolicy
import com.eunilsung.ods.ui.customer.displayPhone
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popOrClose
import com.eunilsung.ods.ui.schedule.detail.VisitDetailScreen
import com.eunilsung.ods.ui.schedule.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import com.eunilsung.ods.ui.uikit.button.PrimaryButton
import com.eunilsung.ods.ui.uikit.calendar.WeekStrip
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.segment.SegmentedControl
import com.eunilsung.ods.ui.uikit.textfield.FormField
import com.eunilsung.ods.ui.uikit.textfield.SearchField
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import kotlinx.datetime.LocalDate
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.customer_search_hint
import ods.composeapp.generated.resources.error_server_unreachable
import ods.composeapp.generated.resources.new_visit_change_customer
import ods.composeapp.generated.resources.new_visit_customer
import ods.composeapp.generated.resources.new_visit_customer_error
import ods.composeapp.generated.resources.new_visit_date
import ods.composeapp.generated.resources.new_visit_in_past
import ods.composeapp.generated.resources.new_visit_place_error
import ods.composeapp.generated.resources.new_visit_place_hint
import ods.composeapp.generated.resources.new_visit_submit
import ods.composeapp.generated.resources.new_visit_time_error
import ods.composeapp.generated.resources.schedule_add
import ods.composeapp.generated.resources.visit_memo
import ods.composeapp.generated.resources.visit_place
import ods.composeapp.generated.resources.visit_purpose
import ods.composeapp.generated.resources.visit_time
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 방문 일정 등록 — 오른쪽 스택에 뜬다. 등록하면 그 일정의 상세로 바뀐다.
 *
 * Android 가 화면을 저장할 때 직렬화하므로 날짜는 문자열로 받는다.
 *
 * @param dateIso 처음 고를 날("2026-10-19"). 없으면 오늘.
 * @param customerId 고객 상세에서 열었으면 그 고객.
 */
data class NewVisitScreen(val dateIso: String? = null, val customerId: String? = null) : Screen {
    constructor(date: LocalDate) : this(dateIso = date.toString())

    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: NewVisitViewModel = koinViewModel(key = "new-visit-$key")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current

        LaunchedEffect(Unit) { viewModel.onAction(NewVisitActions.OnEnter(dateIso?.let(LocalDate::parse), customerId)) }
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is NewVisitEvent.Added -> rightNavigator.replace(VisitDetailScreen(event.visitId))
                }
            }
        }

        NewVisitContent(uiState = uiState, onAction = viewModel::onAction, onBack = { rightNavigator.popOrClose() })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewVisitContent(uiState: NewVisitUiState, onAction: (NewVisitActions) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding().imePadding()) {
        TopBar(title = stringResource(Res.string.schedule_add), onBack = onBack)
        LineDivider()
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Dimens.Space12),
            verticalArrangement = Arrangement.spacedBy(Dimens.Space8),
        ) {
            Label(stringResource(Res.string.new_visit_customer))
            CustomerPicker(uiState, onAction)
            if (uiState.showErrors && uiState.customerError) ErrorText(stringResource(Res.string.new_visit_customer_error))

            Label(stringResource(Res.string.new_visit_date))
            WeekStrip(
                days = uiState.weekDays,
                selected = uiState.date,
                today = uiState.today,
                counts = emptyMap(),
                onSelect = { onAction(NewVisitActions.OnDateSelect(it)) },
                onPreviousWeek = { onAction(NewVisitActions.OnPreviousWeek) },
                onNextWeek = { onAction(NewVisitActions.OnNextWeek) },
                tagPrefix = TestTags.NEW_VISIT_PREFIX,
            )

            Label(stringResource(Res.string.visit_time))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VisitPolicy.TIME_SLOTS.forEach { (hour, minute) ->
                    TimeChip(hour, minute, selected = uiState.time == hour to minute) { onAction(NewVisitActions.OnTimeSelect(hour, minute)) }
                }
            }
            if (uiState.showErrors && uiState.timeError) ErrorText(stringResource(Res.string.new_visit_time_error))
            if (uiState.inPast) ErrorText(stringResource(Res.string.new_visit_in_past))

            Column(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                FormField(
                    value = uiState.place,
                    onValueChange = { onAction(NewVisitActions.OnPlaceChange(it)) },
                    label = stringResource(Res.string.visit_place),
                    placeholder = stringResource(Res.string.new_visit_place_hint),
                    testTag = TestTags.NEW_VISIT_PLACE,
                    error = if (uiState.showErrors && uiState.placeError) stringResource(Res.string.new_visit_place_error) else null,
                )
                Text(text = stringResource(Res.string.visit_purpose), style = AppTypography.Caption, color = AppColors.TextSub)
                SegmentedControl(
                    options = VisitPurpose.entries,
                    selected = uiState.purpose,
                    label = { it.label() },
                    onSelect = { onAction(NewVisitActions.OnPurposeChange(it)) },
                )
                FormField(
                    value = uiState.memo,
                    onValueChange = { onAction(NewVisitActions.OnMemoChange(it)) },
                    label = stringResource(Res.string.visit_memo),
                    testTag = TestTags.NEW_VISIT_MEMO,
                    singleLine = false,
                )
                if (uiState.saveFailed) {
                    Text(text = stringResource(Res.string.error_server_unreachable), style = AppTypography.Caption, color = AppColors.Caution)
                }
                PrimaryButton(
                    text = stringResource(Res.string.new_visit_submit),
                    onClick = { onAction(NewVisitActions.OnSubmit) },
                    loading = uiState.isSaving,
                    modifier = Modifier.padding(top = Dimens.Space8).testTag(TestTags.NEW_VISIT_SUBMIT),
                )
            }
        }
    }
}

@Composable
private fun CustomerPicker(uiState: NewVisitUiState, onAction: (NewVisitActions) -> Unit) {
    val customer = uiState.customer
    if (customer != null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = customer.name, style = AppTypography.ListName, color = AppColors.Text, modifier = Modifier.weight(1f))
            OutlineButton(text = stringResource(Res.string.new_visit_change_customer), onClick = { onAction(NewVisitActions.OnCustomerClear) })
        }
        return
    }
    SearchField(
        value = uiState.customerQuery,
        onValueChange = { onAction(NewVisitActions.OnCustomerQueryChange(it)) },
        placeholder = stringResource(Res.string.customer_search_hint),
        testTag = TestTags.NEW_VISIT_CUSTOMER_SEARCH,
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
    )
    uiState.customerMatches.forEach { match -> CustomerMatchRow(match) { onAction(NewVisitActions.OnCustomerSelect(match.id)) } }
}

@Composable
private fun CustomerMatchRow(customer: Customer, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.NEW_VISIT_CUSTOMER)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
    ) {
        Text(text = customer.name, style = AppTypography.Body, color = AppColors.Text, modifier = Modifier.weight(1f))
        Text(text = customer.displayPhone(mask = true), style = AppTypography.CaptionNumber, color = AppColors.TextSub)
    }
}

@Composable
private fun TimeChip(hour: Int, minute: Int, selected: Boolean, onClick: () -> Unit) {
    val text = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    Text(
        text = text,
        style = AppTypography.BodyNumber,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) AppColors.White else AppColors.Text,
        modifier = Modifier
            .testTag("${TestTags.TIME_SLOT_PREFIX}${text.replace(":", "")}")
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AppColors.PrimaryMain else AppColors.Transparent)
            .border(1.dp, if (selected) AppColors.PrimaryMain else AppColors.Line, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = AppTypography.Section,
        color = AppColors.Text,
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).padding(top = Dimens.Space8),
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = AppTypography.Caption, color = AppColors.Caution, modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))
}
