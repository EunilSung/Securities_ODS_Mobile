package com.eunilsung.ods.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 글자 스타일. 색은 넣지 않는다 — 시세 글자는 등락에 따라 색이 바뀌므로 쓰는 쪽이 정한다.
 *
 * 숫자가 들어가는 스타일은 고정폭 숫자(`tnum`)로 그려 시세가 바뀌어도 자리가 흔들리지 않는다.
 * 글꼴은 아직 기기 기본(Android 본고딕·iOS 애플 SD 고딕)이다. 전용 글꼴을 넣을 때는 여기 스타일에만 `fontFamily` 를 더한다.
 */
object AppTypography {
    private const val TABULAR_NUMBERS = "tnum"

    /** 종목 화면 머리의 현재가. */
    val PriceHero = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR_NUMBERS)

    /** 상단 제목줄. */
    val Title = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)

    /** 화면 안 구역 제목(차트·호가). */
    val Section = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)

    val ListName = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val ListPrice = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = TABULAR_NUMBERS)

    val Body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val BodyNumber = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMBERS)

    /** 전일 대비 변화("▲700 +0.99%"). */
    val Change = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMBERS)

    /** 호가 가격·잔량. */
    val OrderBook = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMBERS)

    val Caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val CaptionNumber = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TABULAR_NUMBERS)

    /** 차트 눈금·시각. */
    val ChartLabel = TextStyle(fontSize = 10.sp, lineHeight = 12.sp, fontFeatureSettings = TABULAR_NUMBERS)

    /** 하단 탭 이름. */
    val Tab = TextStyle(fontSize = 11.sp, lineHeight = 11.sp)
}
