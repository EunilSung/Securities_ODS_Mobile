package com.eunilsung.ods.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator

/**
 * 앱 전역 호스트 — 오른쪽 화면 스택을 내려 주고, 상단·좌우 인셋을 한 번에 처리한다.
 *
 * 하단 인셋은 소비하지 않는다(edge-to-edge). 아래쪽은 [KeyBoardPane] 이 채운다.
 * 토스트·다이얼로그·바텀시트 호스트가 생기면 여기에 겹겹이 감싼다.
 */
@Composable
fun AppHosts(
    rightNavigator: Navigator,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalRightNavigator provides rightNavigator) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            )
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                content()
            }
        }
    }
}
