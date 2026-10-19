package com.eunilsung.ods.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 앱 색상. 라이트·다크 값을 [Light]·[Dark] 에 두고, 화면은 테마를 따라 바뀌는 getter 를 쓴다.
 *
 * 시세 색은 국내 증권 관례를 따른다 — 상승 [Rise] 빨강, 하락 [Fall] 파랑, 보합은 기본 글자색.
 * 매수는 상승색, 매도는 하락색을 함께 쓴다.
 */
object AppColors {
    val Main = Color(0xFF1B3A8C)
    val Transparent = Color(0x00000000)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val ClickRipple = Color(0xFF888888)

    object Light {
        val PrimaryMain = Color(0xFF1B3A8C)
        val PrimaryBg = Color(0xFFE8EDF8)
        val Text = Color(0xFF191F28)
        val TextSub = Color(0xFF6B7684)
        val TextDisabled = Color(0xFFB0B8C1)
        val Line = Color(0xFFE5E8EB)
        val Gray50Bg = Color(0xFFF7F8FA)
        val Gray100Bg = Color(0xFFF2F4F6)
        val Bg = Color(0xFFFFFFFF)
        val Rise = Color(0xFFE42939)
        val RiseBg = Color(0xFFFDECEE)
        val Fall = Color(0xFF1D64DA)
        val FallBg = Color(0xFFE8F0FC)
        val Tab = Color(0xFF8B95A1)
        val Caution = Color(0xFFD9480F)
    }

    object Dark {
        val PrimaryMain = Color(0xFF6E8FE8)
        val PrimaryBg = Color(0xFF1E2A44)
        val Text = Color(0xFFF2F4F6)
        val TextSub = Color(0xFF8B95A1)
        val TextDisabled = Color(0xFF4E5968)
        val Line = Color(0xFF2C323B)
        val Gray50Bg = Color(0xFF17191D)
        val Gray100Bg = Color(0xFF202329)
        val Bg = Color(0xFF101114)
        val Rise = Color(0xFFFF5C69)
        val RiseBg = Color(0xFF3A1F23)
        val Fall = Color(0xFF5B95F5)
        val FallBg = Color(0xFF1C2A40)
        val Tab = Color(0xFF6B7684)
        val Caution = Color(0xFFFF922B)
    }

    val PrimaryMain: Color @Composable get() = if (isSystemInDarkTheme()) Dark.PrimaryMain else Light.PrimaryMain
    val PrimaryBg: Color @Composable get() = if (isSystemInDarkTheme()) Dark.PrimaryBg else Light.PrimaryBg
    val Text: Color @Composable get() = if (isSystemInDarkTheme()) Dark.Text else Light.Text
    val TextSub: Color @Composable get() = if (isSystemInDarkTheme()) Dark.TextSub else Light.TextSub
    val TextDisabled: Color @Composable get() = if (isSystemInDarkTheme()) Dark.TextDisabled else Light.TextDisabled
    val Line: Color @Composable get() = if (isSystemInDarkTheme()) Dark.Line else Light.Line
    val Gray50Bg: Color @Composable get() = if (isSystemInDarkTheme()) Dark.Gray50Bg else Light.Gray50Bg
    val Gray100Bg: Color @Composable get() = if (isSystemInDarkTheme()) Dark.Gray100Bg else Light.Gray100Bg
    val Bg: Color @Composable get() = if (isSystemInDarkTheme()) Dark.Bg else Light.Bg
    val Rise: Color @Composable get() = if (isSystemInDarkTheme()) Dark.Rise else Light.Rise
    val RiseBg: Color @Composable get() = if (isSystemInDarkTheme()) Dark.RiseBg else Light.RiseBg
    val Fall: Color @Composable get() = if (isSystemInDarkTheme()) Dark.Fall else Light.Fall
    val FallBg: Color @Composable get() = if (isSystemInDarkTheme()) Dark.FallBg else Light.FallBg
    val Tab: Color @Composable get() = if (isSystemInDarkTheme()) Dark.Tab else Light.Tab

    /** 주문 검사의 오류·경고 글자. 상승·매수 빨강과 섞이지 않게 주황을 쓴다. */
    val Caution: Color @Composable get() = if (isSystemInDarkTheme()) Dark.Caution else Light.Caution
}
