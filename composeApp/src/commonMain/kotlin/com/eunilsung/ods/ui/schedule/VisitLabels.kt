package com.eunilsung.ods.ui.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eunilsung.ods.domain.model.VisitPurpose
import com.eunilsung.ods.domain.model.VisitStatus
import com.eunilsung.ods.ui.theme.AppColors
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.visit_purpose_account
import ods.composeapp.generated.resources.visit_purpose_consult
import ods.composeapp.generated.resources.visit_purpose_product
import ods.composeapp.generated.resources.visit_purpose_risk
import ods.composeapp.generated.resources.visit_status_canceled
import ods.composeapp.generated.resources.visit_status_completed
import ods.composeapp.generated.resources.visit_status_scheduled
import org.jetbrains.compose.resources.stringResource

@Composable
fun VisitPurpose.label(): String = stringResource(
    when (this) {
        VisitPurpose.CONSULT -> Res.string.visit_purpose_consult
        VisitPurpose.ACCOUNT_OPENING -> Res.string.visit_purpose_account
        VisitPurpose.PRODUCT -> Res.string.visit_purpose_product
        VisitPurpose.RISK_PROFILE -> Res.string.visit_purpose_risk
    }
)

@Composable
fun VisitStatus.label(): String = stringResource(
    when (this) {
        VisitStatus.SCHEDULED -> Res.string.visit_status_scheduled
        VisitStatus.COMPLETED -> Res.string.visit_status_completed
        VisitStatus.CANCELED -> Res.string.visit_status_canceled
    }
)

val VisitStatus.color: Color
    @Composable get() = when (this) {
        VisitStatus.SCHEDULED -> AppColors.PrimaryMain
        VisitStatus.COMPLETED -> AppColors.TextSub
        VisitStatus.CANCELED -> AppColors.TextDisabled
    }
