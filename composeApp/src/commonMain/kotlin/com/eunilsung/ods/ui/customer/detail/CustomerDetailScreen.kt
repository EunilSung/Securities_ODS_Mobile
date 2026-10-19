package com.eunilsung.ods.ui.customer.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.eunilsung.ods.domain.model.Account
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.Visit
import com.eunilsung.ods.domain.util.ageOn
import com.eunilsung.ods.domain.util.hasValidRiskProfile
import com.eunilsung.ods.ui.customer.color
import com.eunilsung.ods.ui.customer.consultation.ConsultationEditorScreen
import com.eunilsung.ods.ui.customer.consultation.ConsultationItem
import com.eunilsung.ods.ui.customer.displayAccountNo
import com.eunilsung.ods.ui.customer.displayAddress
import com.eunilsung.ods.ui.customer.displayBirthDate
import com.eunilsung.ods.ui.customer.displayPhone
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.schedule.detail.VisitDetailScreen
import com.eunilsung.ods.ui.schedule.label
import com.eunilsung.ods.ui.schedule.register.NewVisitScreen
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.badge.Badge
import com.eunilsung.ods.ui.uikit.button.OutlineButton
import com.eunilsung.ods.ui.uikit.click.clickable
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.section.InfoRow
import com.eunilsung.ods.ui.uikit.section.SectionGap
import com.eunilsung.ods.ui.uikit.section.SectionHeader
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import com.eunilsung.ods.ui.util.formatDate
import com.eunilsung.ods.ui.util.formatDateTime
import kotlinx.datetime.LocalDate
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.customer_accounts
import ods.composeapp.generated.resources.customer_accounts_empty
import ods.composeapp.generated.resources.customer_address
import ods.composeapp.generated.resources.customer_age_gender
import ods.composeapp.generated.resources.customer_birth
import ods.composeapp.generated.resources.customer_consultations
import ods.composeapp.generated.resources.customer_consultations_empty
import ods.composeapp.generated.resources.customer_detail
import ods.composeapp.generated.resources.customer_hide_info
import ods.composeapp.generated.resources.customer_no_upcoming_visits
import ods.composeapp.generated.resources.customer_upcoming_visits
import ods.composeapp.generated.resources.schedule_add
import ods.composeapp.generated.resources.customer_phone
import ods.composeapp.generated.resources.customer_risk_assessed_on
import ods.composeapp.generated.resources.customer_risk_expired_note
import ods.composeapp.generated.resources.customer_risk_none_note
import ods.composeapp.generated.resources.customer_risk_profile
import ods.composeapp.generated.resources.customer_show_info
import ods.composeapp.generated.resources.consult_add
import ods.composeapp.generated.resources.visibility_icon
import ods.composeapp.generated.resources.visibility_off_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 고객 상세 — 오른쪽 스택에 뜬다. 폰에서는 목록 위로 밀려 들어오고, 태블릿에서는 오른쪽 칸에 뜬다. */
data class CustomerDetailScreen(val customerId: String) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: CustomerDetailViewModel = koinViewModel(key = "customer-$customerId")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current

        LaunchedEffect(customerId) { viewModel.onAction(CustomerDetailActions.OnEnter(customerId)) }
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is CustomerDetailEvent.OpenConsultationEditor -> rightNavigator.push(ConsultationEditorScreen(event.customerId))
                    is CustomerDetailEvent.OpenNewVisit -> rightNavigator.push(NewVisitScreen(customerId = event.customerId))
                    is CustomerDetailEvent.OpenVisit -> rightNavigator.push(VisitDetailScreen(event.visitId))
                }
            }
        }

        CustomerDetailContent(
            uiState = uiState,
            onAction = viewModel::onAction,
            onBack = { rightNavigator.popToEmptyRoot() },
        )
    }
}

@Composable
fun CustomerDetailContent(
    uiState: CustomerDetailUiState,
    onAction: (CustomerDetailActions) -> Unit,
    onBack: () -> Unit,
) {
    val customer = uiState.customer
    val today = uiState.today
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding()) {
        TopBar(
            title = customer?.name ?: stringResource(Res.string.customer_detail),
            onBack = onBack,
            actions = {
                Box(
                    modifier = Modifier.size(40.dp).clickable(cornerRadius = 20.dp, onClick = { onAction(CustomerDetailActions.OnToggleMask) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (uiState.maskPersonalInfo) Res.drawable.visibility_icon else Res.drawable.visibility_off_icon),
                        contentDescription = stringResource(if (uiState.maskPersonalInfo) Res.string.customer_show_info else Res.string.customer_hide_info),
                        tint = AppColors.TextSub,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
        )
        LineDivider()
        if (customer == null || today == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ProfileSection(customer, today, uiState.maskPersonalInfo)
            SectionGap()
            RiskSection(customer, today)
            SectionGap()
            AccountSection(uiState.accounts, uiState.maskPersonalInfo)
            SectionGap()
            VisitSection(
                visits = uiState.upcomingVisits,
                onAdd = { onAction(CustomerDetailActions.OnAddVisitClick) },
                onVisitClick = { onAction(CustomerDetailActions.OnVisitClick(it)) },
            )
            SectionGap()
            ConsultationSection(uiState.consultations, onAdd = { onAction(CustomerDetailActions.OnAddConsultationClick) })
        }
    }
}

@Composable
private fun ProfileSection(customer: Customer, today: LocalDate, mask: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.Space12)) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.Space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
        ) {
            Text(
                text = customer.name,
                style = AppTypography.PriceHero.copy(fontSize = AppTypography.Title.fontSize * 1.2f),
                color = AppColors.Text,
                modifier = Modifier.testTag(TestTags.CUSTOMER_DETAIL_NAME),
            )
            Badge(text = customer.grade.label(), color = AppColors.TextSub)
            Text(
                text = stringResource(Res.string.customer_age_gender, customer.ageOn(today).toString(), customer.gender.label()),
                style = AppTypography.Body,
                color = AppColors.TextSub,
            )
        }
        InfoRow(stringResource(Res.string.customer_phone), customer.displayPhone(mask))
        InfoRow(stringResource(Res.string.customer_birth), customer.displayBirthDate(mask))
        InfoRow(stringResource(Res.string.customer_address), customer.displayAddress(mask))
    }
}

@Composable
private fun RiskSection(customer: Customer, today: LocalDate) {
    SectionHeader(stringResource(Res.string.customer_risk_profile))
    val profile = customer.riskProfile
    Column(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.Space16), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (profile == null) {
            Text(text = stringResource(Res.string.customer_risk_none_note), style = AppTypography.Body, color = AppColors.Caution)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                Badge(text = profile.label(), color = profile.color)
                customer.riskAssessedAtMillis?.let {
                    Text(text = stringResource(Res.string.customer_risk_assessed_on, formatDate(it)), style = AppTypography.Caption, color = AppColors.TextSub)
                }
            }
            if (!customer.hasValidRiskProfile(today)) {
                Text(text = stringResource(Res.string.customer_risk_expired_note), style = AppTypography.Body, color = AppColors.Caution)
            }
        }
    }
}

@Composable
private fun AccountSection(accounts: List<Account>, mask: Boolean) {
    SectionHeader(stringResource(Res.string.customer_accounts))
    if (accounts.isEmpty()) {
        Text(
            text = stringResource(Res.string.customer_accounts_empty),
            style = AppTypography.Body,
            color = AppColors.TextSub,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.Space16),
        )
        return
    }
    accounts.forEach { account ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.type.label(), style = AppTypography.ListName, color = AppColors.Text)
                Text(text = displayAccountNo(account.accountNo, mask), style = AppTypography.CaptionNumber, color = AppColors.TextSub)
            }
            Text(text = formatDate(account.openedAtMillis), style = AppTypography.CaptionNumber, color = AppColors.TextSub)
        }
    }
    Box(Modifier.padding(bottom = Dimens.Space8))
}

@Composable
private fun VisitSection(visits: List<Visit>, onAdd: () -> Unit, onVisitClick: (String) -> Unit) {
    SectionHeader(stringResource(Res.string.customer_upcoming_visits)) {
        OutlineButton(text = stringResource(Res.string.schedule_add), onClick = onAdd, modifier = Modifier.testTag(TestTags.CUSTOMER_ADD_VISIT))
    }
    if (visits.isEmpty()) {
        Text(
            text = stringResource(Res.string.customer_no_upcoming_visits),
            style = AppTypography.Body,
            color = AppColors.TextSub,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.Space16),
        )
        return
    }
    visits.forEach { visit ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onVisitClick(visit.id) })
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
        ) {
            Text(text = formatDateTime(visit.scheduledAtMillis), style = AppTypography.BodyNumber, color = AppColors.Text)
            Text(text = visit.place, style = AppTypography.Body, color = AppColors.TextSub, modifier = Modifier.weight(1f), maxLines = 1)
            Badge(text = visit.purpose.label(), color = AppColors.PrimaryMain)
        }
    }
    Box(Modifier.padding(bottom = Dimens.Space8))
}

@Composable
private fun ConsultationSection(consultations: List<Consultation>, onAdd: () -> Unit) {
    SectionHeader(stringResource(Res.string.customer_consultations)) {
        OutlineButton(text = stringResource(Res.string.consult_add), onClick = onAdd, modifier = Modifier.testTag(TestTags.CONSULT_ADD))
    }
    if (consultations.isEmpty()) {
        Text(
            text = stringResource(Res.string.customer_consultations_empty),
            style = AppTypography.Body,
            color = AppColors.TextSub,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding).padding(bottom = Dimens.Space16),
        )
        return
    }
    consultations.forEach { consultation ->
        ConsultationItem(consultation)
        LineDivider(modifier = Modifier.padding(horizontal = Dimens.ScreenPadding))
    }
}
