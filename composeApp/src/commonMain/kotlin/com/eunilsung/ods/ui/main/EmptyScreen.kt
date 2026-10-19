package com.eunilsung.ods.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.eunilsung.ods.ui.theme.AppColors
import ods.composeapp.generated.resources.Res
import ods.composeapp.generated.resources.logo_icon
import org.jetbrains.compose.resources.painterResource

/** 오른쪽 칸의 바닥 화면. 태블릿에서 아무것도 고르지 않았을 때 보인다. */
object EmptyScreen : Screen {
    @Composable
    override fun Content() {
        Box(Modifier.fillMaxSize().background(AppColors.Gray50Bg), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Res.drawable.logo_icon),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = AppColors.TextDisabled
            )
        }
    }
}

/**
 * 오른쪽 칸을 빈 화면([EmptyScreen])으로 되돌린다 — 화면 닫기의 공통 경로.
 *
 * [Navigator.popUntilRoot] 만 쓰면 스택 크기가 1 일 때 조용히 아무것도 하지 않는다(minSize = 1).
 * 루트가 [EmptyScreen] 이 아닌 화면으로 굳은 상태에서도 반드시 빠져나가게 루트를 직접 되돌린다.
 */
fun Navigator.popToEmptyRoot() {
    popUntilRoot()
    if (lastItem !is EmptyScreen) replaceAll(EmptyScreen)
}

/**
 * 한 단계 뒤로 — 위에 쌓인 화면이 있으면 그것만 닫고, 아니면 오른쪽을 비운다.
 */
fun Navigator.popOrClose() {
    if (size > 2) pop() else popToEmptyRoot()
}
