package com.eunilsung.ods.ui.setting.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popToEmptyRoot
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.topbar.TopBar

/** 설정 상세 화면의 틀 — 뒤로 버튼이 있는 제목줄과 스크롤되는 본문. */
@Composable
fun SettingDetailScaffold(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val rightNavigator = LocalRightNavigator.current
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding()) {
        TopBar(title = title, onBack = { rightNavigator.popToEmptyRoot() })
        LineDivider()
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), content = content)
    }
}
