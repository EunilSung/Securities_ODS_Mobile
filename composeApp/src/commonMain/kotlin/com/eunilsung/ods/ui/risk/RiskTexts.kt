package com.eunilsung.ods.ui.risk

import androidx.compose.runtime.Composable
import com.eunilsung.ods.domain.model.RiskProfile
import com.eunilsung.ods.domain.model.RiskQuestion
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.risk_desc_active
import ods.composeapp.generated.resources.risk_desc_aggressive
import ods.composeapp.generated.resources.risk_desc_neutral
import ods.composeapp.generated.resources.risk_desc_stable
import ods.composeapp.generated.resources.risk_desc_stable_seeking
import ods.composeapp.generated.resources.risk_q_age
import ods.composeapp.generated.resources.risk_q_age_0
import ods.composeapp.generated.resources.risk_q_age_1
import ods.composeapp.generated.resources.risk_q_age_2
import ods.composeapp.generated.resources.risk_q_age_3
import ods.composeapp.generated.resources.risk_q_age_4
import ods.composeapp.generated.resources.risk_q_asset
import ods.composeapp.generated.resources.risk_q_asset_0
import ods.composeapp.generated.resources.risk_q_asset_1
import ods.composeapp.generated.resources.risk_q_asset_2
import ods.composeapp.generated.resources.risk_q_asset_3
import ods.composeapp.generated.resources.risk_q_experience
import ods.composeapp.generated.resources.risk_q_experience_0
import ods.composeapp.generated.resources.risk_q_experience_1
import ods.composeapp.generated.resources.risk_q_experience_2
import ods.composeapp.generated.resources.risk_q_experience_3
import ods.composeapp.generated.resources.risk_q_experience_4
import ods.composeapp.generated.resources.risk_q_income
import ods.composeapp.generated.resources.risk_q_income_0
import ods.composeapp.generated.resources.risk_q_income_1
import ods.composeapp.generated.resources.risk_q_income_2
import ods.composeapp.generated.resources.risk_q_knowledge
import ods.composeapp.generated.resources.risk_q_knowledge_0
import ods.composeapp.generated.resources.risk_q_knowledge_1
import ods.composeapp.generated.resources.risk_q_knowledge_2
import ods.composeapp.generated.resources.risk_q_knowledge_3
import ods.composeapp.generated.resources.risk_q_loss
import ods.composeapp.generated.resources.risk_q_loss_0
import ods.composeapp.generated.resources.risk_q_loss_1
import ods.composeapp.generated.resources.risk_q_loss_2
import ods.composeapp.generated.resources.risk_q_loss_3
import ods.composeapp.generated.resources.risk_q_period
import ods.composeapp.generated.resources.risk_q_period_0
import ods.composeapp.generated.resources.risk_q_period_1
import ods.composeapp.generated.resources.risk_q_period_2
import ods.composeapp.generated.resources.risk_q_period_3
import ods.composeapp.generated.resources.risk_q_period_4
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** 문항과 선택지 문구. 선택지 순서는 [RiskQuestion.scores] 와 같다. */
private val QUESTION_TEXTS: Map<RiskQuestion, Pair<StringResource, List<StringResource>>> = mapOf(
    RiskQuestion.AGE to (Res.string.risk_q_age to listOf(Res.string.risk_q_age_0, Res.string.risk_q_age_1, Res.string.risk_q_age_2, Res.string.risk_q_age_3, Res.string.risk_q_age_4)),
    RiskQuestion.INVESTMENT_PERIOD to (Res.string.risk_q_period to listOf(Res.string.risk_q_period_0, Res.string.risk_q_period_1, Res.string.risk_q_period_2, Res.string.risk_q_period_3, Res.string.risk_q_period_4)),
    RiskQuestion.EXPERIENCE to (Res.string.risk_q_experience to listOf(Res.string.risk_q_experience_0, Res.string.risk_q_experience_1, Res.string.risk_q_experience_2, Res.string.risk_q_experience_3, Res.string.risk_q_experience_4)),
    RiskQuestion.KNOWLEDGE to (Res.string.risk_q_knowledge to listOf(Res.string.risk_q_knowledge_0, Res.string.risk_q_knowledge_1, Res.string.risk_q_knowledge_2, Res.string.risk_q_knowledge_3)),
    RiskQuestion.LOSS_TOLERANCE to (Res.string.risk_q_loss to listOf(Res.string.risk_q_loss_0, Res.string.risk_q_loss_1, Res.string.risk_q_loss_2, Res.string.risk_q_loss_3)),
    RiskQuestion.INCOME to (Res.string.risk_q_income to listOf(Res.string.risk_q_income_0, Res.string.risk_q_income_1, Res.string.risk_q_income_2)),
    RiskQuestion.ASSET_RATIO to (Res.string.risk_q_asset to listOf(Res.string.risk_q_asset_0, Res.string.risk_q_asset_1, Res.string.risk_q_asset_2, Res.string.risk_q_asset_3)),
)

@Composable
fun RiskQuestion.title(): String = stringResource(QUESTION_TEXTS.getValue(this).first)

@Composable
fun RiskQuestion.options(): List<String> = QUESTION_TEXTS.getValue(this).second.map { stringResource(it) }

@Composable
fun RiskProfile.description(): String = stringResource(
    when (this) {
        RiskProfile.STABLE -> Res.string.risk_desc_stable
        RiskProfile.STABLE_SEEKING -> Res.string.risk_desc_stable_seeking
        RiskProfile.NEUTRAL -> Res.string.risk_desc_neutral
        RiskProfile.ACTIVE -> Res.string.risk_desc_active
        RiskProfile.AGGRESSIVE -> Res.string.risk_desc_aggressive
    }
)
