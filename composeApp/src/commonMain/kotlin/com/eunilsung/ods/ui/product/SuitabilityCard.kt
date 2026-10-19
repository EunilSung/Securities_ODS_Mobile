package com.eunilsung.ods.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.Product
import com.eunilsung.ods.domain.model.Suitability
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.suitability_needs_assessment
import ods.composeapp.generated.resources.suitability_suitable
import ods.composeapp.generated.resources.suitability_title
import ods.composeapp.generated.resources.suitability_unsuitable
import org.jetbrains.compose.resources.stringResource

/** 적합성 판정 카드 — 고객 성향과 상품 등급을 나란히 보이고, 맞으면 파랑, 아니면 주황으로 알린다. */
@Composable
fun SuitabilityCard(customer: Customer, product: Product, suitability: Suitability, modifier: Modifier = Modifier) {
    val (message, color) = when (suitability) {
        Suitability.Suitable -> stringResource(Res.string.suitability_suitable) to AppColors.PrimaryMain
        Suitability.NeedsAssessment -> stringResource(Res.string.suitability_needs_assessment) to AppColors.Caution
        is Suitability.Unsuitable -> stringResource(Res.string.suitability_unsuitable, suitability.profile.label(), product.riskGrade.label()) to AppColors.Caution
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.SUITABILITY_CARD)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(Dimens.Space16),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(Res.string.suitability_title, customer.name, customer.riskProfile?.label() ?: "-"),
            style = AppTypography.Body,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Text,
        )
        Text(text = message, style = AppTypography.Body, color = color)
    }
}
