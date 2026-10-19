package com.eunilsung.ods.ui.customer.list.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.util.ageOn
import com.eunilsung.ods.domain.util.hasValidRiskProfile
import com.eunilsung.ods.ui.customer.color
import com.eunilsung.ods.ui.customer.displayPhone
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.theme.LocalListDensity
import com.eunilsung.ods.ui.theme.tokens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.badge.Badge
import com.eunilsung.ods.ui.uikit.click.clickable
import kotlinx.datetime.LocalDate
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.customer_age_gender
import ods.composeapp.generated.resources.customer_consulting
import ods.composeapp.generated.resources.risk_expired
import ods.composeapp.generated.resources.risk_none
import org.jetbrains.compose.resources.stringResource

/**
 * 고객 한 줄 — 이름·등급·나이, 아래 전화번호, 오른쪽 투자성향. 투자성향이 없거나 1년이 지났으면 주황으로 알린다.
 *
 * @param isSelected 태블릿에서 오른쪽에 열린 고객이면 배경을 칠한다.
 * @param isConsulting 지금 상담 중인 고객이면 이름 옆에 표시한다.
 */
@Composable
fun CustomerItem(
    customer: Customer,
    today: LocalDate,
    maskPersonalInfo: Boolean,
    isSelected: Boolean,
    isConsulting: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalListDensity.current.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.CUSTOMER_ITEM)
            .background(if (isSelected) AppColors.PrimaryBg else AppColors.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = tokens.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = customer.name, style = tokens.nameStyle, color = AppColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Badge(text = customer.grade.label(), color = AppColors.TextSub)
                if (isConsulting) Badge(text = stringResource(Res.string.customer_consulting), color = AppColors.PrimaryMain)
            }
            Text(
                text = stringResource(Res.string.customer_age_gender, customer.ageOn(today).toString(), customer.gender.label()) +
                    " · " + customer.displayPhone(maskPersonalInfo),
                style = tokens.subStyle,
                color = AppColors.TextSub,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        val profile = customer.riskProfile
        when {
            profile == null -> Badge(text = stringResource(Res.string.risk_none), color = AppColors.Caution)
            !customer.hasValidRiskProfile(today) -> Badge(text = stringResource(Res.string.risk_expired), color = AppColors.Caution)
            else -> Badge(text = profile.label(), color = profile.color)
        }
    }
}
