package com.eunilsung.ods.ui.customer.consultation

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
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.domain.model.Consultation
import com.eunilsung.ods.ui.customer.label
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.TestTags
import com.eunilsung.ods.ui.uikit.badge.Badge
import com.eunilsung.ods.ui.util.formatDateTime

/** 상담 이력 한 건 — 시각·채널·주제, 내용, 기록한 직원. */
@Composable
fun ConsultationItem(consultation: Consultation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.CONSULT_ITEM)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space12),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = formatDateTime(consultation.consultedAtMillis), style = AppTypography.CaptionNumber, color = AppColors.TextSub)
            Badge(text = consultation.channel.label(), color = AppColors.TextSub)
            Badge(text = consultation.topic.label(), color = AppColors.PrimaryMain)
            Text(text = consultation.staffName, style = AppTypography.Caption, color = AppColors.TextSub, modifier = Modifier.weight(1f), maxLines = 1)
        }
        Text(text = consultation.content, style = AppTypography.Body, color = AppColors.Text)
    }
}
