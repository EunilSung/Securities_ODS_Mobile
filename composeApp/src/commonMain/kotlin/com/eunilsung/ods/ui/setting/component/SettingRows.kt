package com.eunilsung.ods.ui.setting.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.AppTypography
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.click.clickable

/** 설정 구역 제목 — 작은 회색 글자. */
@Composable
fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        style = AppTypography.Caption,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.TextSub,
        modifier = Modifier.fillMaxWidth().padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = Dimens.Space20, bottom = Dimens.Space8),
    )
}

/**
 * 누르면 상세로 가는 줄. 오른쪽에 지금 값과 ›.
 *
 * @param isSelected 태블릿에서 오른쪽에 열린 상세면 배경을 칠한다.
 */
@Composable
fun SettingNavigateRow(title: String, value: String? = null, isSelected: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) AppColors.PrimaryBg else AppColors.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = AppTypography.ListName.copy(fontWeight = FontWeight.Normal), color = AppColors.Text, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(text = value, style = AppTypography.Body, color = AppColors.TextSub)
            Spacer(Modifier.width(Dimens.Space8))
        }
        Text(text = "›", style = AppTypography.Title, color = AppColors.TextDisabled)
    }
}

/** 켜고 끄는 줄 — 제목, 아래 설명, 오른쪽 스위치. 줄 전체를 눌러도 바뀐다. */
@Composable
fun SettingToggleRow(
    title: String,
    description: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = { onCheckedChange(!checked) }) else Modifier)
            .semantics { role = Role.Switch }
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = AppTypography.ListName.copy(fontWeight = FontWeight.Normal), color = if (enabled) AppColors.Text else AppColors.TextDisabled)
            if (description != null) Text(text = description, style = AppTypography.Caption, color = AppColors.TextSub)
        }
        Spacer(Modifier.width(Dimens.Space12))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = AppColors.PrimaryMain, checkedThumbColor = AppColors.White),
        )
    }
}

/** 여럿 중 하나를 고르는 줄 — 왼쪽 라디오, 제목과 설명. */
@Composable
fun SettingRadioRow(title: String, description: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.RadioButton }
            .padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = AppColors.PrimaryMain))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = AppTypography.ListName.copy(fontWeight = FontWeight.Normal), color = AppColors.Text)
            if (description != null) Text(text = description, style = AppTypography.Caption, color = AppColors.TextSub)
        }
    }
}

/** 이름 · 값 줄(바꿀 수 없는 정보). */
@Composable
fun SettingValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = AppTypography.ListName.copy(fontWeight = FontWeight.Normal), color = AppColors.Text, modifier = Modifier.weight(1f))
        Text(text = value, style = AppTypography.BodyNumber, color = AppColors.TextSub)
    }
}

/** 방금 끝난 일을 알리는 띠. */
@Composable
fun SettingNoticeBanner(text: String) {
    Text(
        text = text,
        style = AppTypography.Body,
        color = AppColors.PrimaryMain,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.Space8)
            .background(AppColors.PrimaryBg, RoundedCornerShape(Dimens.Space8))
            .padding(horizontal = Dimens.Space12, vertical = Dimens.Space12),
    )
}
