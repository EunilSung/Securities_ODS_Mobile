package com.eunilsung.ods.ui.customer.consultation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.eunilsung.ods.domain.model.ConsultTopic
import com.eunilsung.ods.ui.main.LocalRightNavigator
import com.eunilsung.ods.ui.main.popOrClose
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.ui.theme.Dimens
import com.eunilsung.ods.ui.uikit.line.LineDivider
import com.eunilsung.ods.ui.uikit.topbar.TopBar
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.consult_add
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 상담 기록 화면 — 고객 상세·방문 상세 위에 쌓인다. 저장하면 닫혀 앞 화면으로 돌아간다.
 *
 * @param topic 처음 고를 주제. 없으면 일반.
 */
data class ConsultationEditorScreen(val customerId: String, val topic: ConsultTopic? = null) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val viewModel: ConsultationViewModel = koinViewModel(key = "consultation-editor-$customerId")
        val uiState by viewModel.uiState.collectAsState()
        val rightNavigator = LocalRightNavigator.current

        LaunchedEffect(customerId) { viewModel.onAction(ConsultationActions.OnEnter(customerId, topic)) }
        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    ConsultationEvent.Saved -> rightNavigator.popOrClose()
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().background(AppColors.Bg).navigationBarsPadding().imePadding()) {
            TopBar(title = stringResource(Res.string.consult_add), onBack = { rightNavigator.popOrClose() })
            LineDivider()
            ConsultationForm(
                uiState = uiState,
                onAction = viewModel::onAction,
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(Dimens.ScreenPadding),
            )
        }
    }
}
