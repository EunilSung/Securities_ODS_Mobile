package com.eunilsung.ods.ui.product

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eunilsung.ods.domain.model.ProductRiskGrade
import com.eunilsung.ods.domain.model.ProductType
import com.eunilsung.ods.ui.theme.AppColors
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.product_grade
import ods.composeapp.generated.resources.product_grade_high
import ods.composeapp.generated.resources.product_grade_low
import ods.composeapp.generated.resources.product_grade_minimal
import ods.composeapp.generated.resources.product_grade_moderate
import ods.composeapp.generated.resources.product_grade_very_high
import ods.composeapp.generated.resources.product_grade_very_low
import ods.composeapp.generated.resources.product_type_bond
import ods.composeapp.generated.resources.product_type_els
import ods.composeapp.generated.resources.product_type_fund
import ods.composeapp.generated.resources.product_type_isa
import ods.composeapp.generated.resources.product_type_rp
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductType.label(): String = stringResource(
    when (this) {
        ProductType.FUND -> Res.string.product_type_fund
        ProductType.ELS -> Res.string.product_type_els
        ProductType.BOND -> Res.string.product_type_bond
        ProductType.RP -> Res.string.product_type_rp
        ProductType.ISA -> Res.string.product_type_isa
    }
)

/** "2등급 높은 위험" */
@Composable
fun ProductRiskGrade.label(): String = stringResource(
    Res.string.product_grade,
    level,
    stringResource(
        when (this) {
            ProductRiskGrade.VERY_HIGH -> Res.string.product_grade_very_high
            ProductRiskGrade.HIGH -> Res.string.product_grade_high
            ProductRiskGrade.MODERATE -> Res.string.product_grade_moderate
            ProductRiskGrade.LOW -> Res.string.product_grade_low
            ProductRiskGrade.VERY_LOW -> Res.string.product_grade_very_low
            ProductRiskGrade.MINIMAL -> Res.string.product_grade_minimal
        }
    ),
)

/** 위험등급 색 — 금융투자협회 위험등급 표시처럼 1등급 빨강에서 6등급 초록 쪽으로. */
val ProductRiskGrade.color: Color
    @Composable get() = when (this) {
        ProductRiskGrade.VERY_HIGH, ProductRiskGrade.HIGH -> AppColors.Rise
        ProductRiskGrade.MODERATE -> AppColors.Caution
        ProductRiskGrade.LOW -> AppColors.PrimaryMain
        ProductRiskGrade.VERY_LOW, ProductRiskGrade.MINIMAL -> AppColors.Fall
    }
