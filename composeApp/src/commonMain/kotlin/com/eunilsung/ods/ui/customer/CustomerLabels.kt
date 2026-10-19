package com.eunilsung.ods.ui.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eunilsung.ods.domain.model.AccountType
import com.eunilsung.ods.domain.model.ConsultChannel
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.domain.model.Customer
import com.eunilsung.ods.domain.model.CustomerGrade
import com.eunilsung.ods.domain.model.Gender
import com.eunilsung.ods.domain.model.RiskProfile
import com.eunilsung.ods.domain.util.maskAccountNo
import com.eunilsung.ods.domain.util.maskAddress
import com.eunilsung.ods.domain.util.maskPhone
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.util.formatDate
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.account_type_brokerage
import ods.composeapp.generated.resources.account_type_cma
import ods.composeapp.generated.resources.account_type_isa
import ods.composeapp.generated.resources.account_type_pension
import ods.composeapp.generated.resources.consult_channel_branch
import ods.composeapp.generated.resources.consult_channel_phone
import ods.composeapp.generated.resources.consult_channel_visit
import ods.composeapp.generated.resources.consult_topic_account
import ods.composeapp.generated.resources.consult_topic_general
import ods.composeapp.generated.resources.consult_topic_market
import ods.composeapp.generated.resources.consult_topic_product
import ods.composeapp.generated.resources.consult_topic_risk
import ods.composeapp.generated.resources.customer_birth_masked
import ods.composeapp.generated.resources.gender_female
import ods.composeapp.generated.resources.gender_male
import ods.composeapp.generated.resources.grade_general
import ods.composeapp.generated.resources.grade_gold
import ods.composeapp.generated.resources.grade_vip
import ods.composeapp.generated.resources.risk_active
import ods.composeapp.generated.resources.risk_aggressive
import ods.composeapp.generated.resources.risk_neutral
import ods.composeapp.generated.resources.risk_stable
import ods.composeapp.generated.resources.risk_stable_seeking
import org.jetbrains.compose.resources.stringResource

/** 고객 화면들이 함께 쓰는 코드 값 이름표와 개인정보 가리기. */

@Composable
fun CustomerGrade.label(): String = stringResource(
    when (this) {
        CustomerGrade.VIP -> Res.string.grade_vip
        CustomerGrade.GOLD -> Res.string.grade_gold
        CustomerGrade.GENERAL -> Res.string.grade_general
    }
)

@Composable
fun Gender.label(): String = stringResource(if (this == Gender.MALE) Res.string.gender_male else Res.string.gender_female)

@Composable
fun RiskProfile.label(): String = stringResource(
    when (this) {
        RiskProfile.STABLE -> Res.string.risk_stable
        RiskProfile.STABLE_SEEKING -> Res.string.risk_stable_seeking
        RiskProfile.NEUTRAL -> Res.string.risk_neutral
        RiskProfile.ACTIVE -> Res.string.risk_active
        RiskProfile.AGGRESSIVE -> Res.string.risk_aggressive
    }
)

/** 투자성향 표시 색 — 위험을 많이 감수할수록 붉게. */
val RiskProfile.color: Color
    @Composable get() = when (this) {
        RiskProfile.STABLE, RiskProfile.STABLE_SEEKING -> AppColors.Fall
        RiskProfile.NEUTRAL -> AppColors.PrimaryMain
        RiskProfile.ACTIVE, RiskProfile.AGGRESSIVE -> AppColors.Rise
    }

@Composable
fun ConsultChannel.label(): String = stringResource(
    when (this) {
        ConsultChannel.VISIT -> Res.string.consult_channel_visit
        ConsultChannel.PHONE -> Res.string.consult_channel_phone
        ConsultChannel.BRANCH -> Res.string.consult_channel_branch
    }
)

@Composable
fun ConsultTopic.label(): String = stringResource(
    when (this) {
        ConsultTopic.GENERAL -> Res.string.consult_topic_general
        ConsultTopic.MARKET -> Res.string.consult_topic_market
        ConsultTopic.ACCOUNT_OPENING -> Res.string.consult_topic_account
        ConsultTopic.RISK_PROFILE -> Res.string.consult_topic_risk
        ConsultTopic.PRODUCT -> Res.string.consult_topic_product
    }
)

@Composable
fun AccountType.label(): String = stringResource(
    when (this) {
        AccountType.BROKERAGE -> Res.string.account_type_brokerage
        AccountType.CMA -> Res.string.account_type_cma
        AccountType.ISA -> Res.string.account_type_isa
        AccountType.PENSION -> Res.string.account_type_pension
    }
)

/** 개인정보 가리기를 켰으면 가운데 자리를 가린 번호, 아니면 그대로. */
fun Customer.displayPhone(mask: Boolean): String = if (mask) maskPhone(phone) else phone

@Composable
fun Customer.displayBirthDate(mask: Boolean): String =
    if (mask) stringResource(Res.string.customer_birth_masked, birthDate.year.toString()) else formatDate(birthDate)

fun Customer.displayAddress(mask: Boolean): String = if (mask) maskAddress(address) else address

fun displayAccountNo(accountNo: String, mask: Boolean): String = if (mask) maskAccountNo(accountNo) else accountNo
