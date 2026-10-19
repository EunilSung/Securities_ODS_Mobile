package com.eunilsung.ods.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eunilsung.ods.domain.model.ListDensity

/** 목록 밀도별 크기. 관심종목 행과 호가 행이 따른다. */
data class ListDensityTokens(
    val rowVerticalPadding: Dp,
    val nameStyle: TextStyle,
    val priceStyle: TextStyle,
    val subStyle: TextStyle,
    val orderBookRowHeight: Dp,
)

private val Comfortable = ListDensityTokens(
    rowVerticalPadding = 14.dp,
    nameStyle = AppTypography.ListName,
    priceStyle = AppTypography.ListPrice,
    subStyle = AppTypography.Caption,
    orderBookRowHeight = 32.dp,
)

private val Compact = ListDensityTokens(
    rowVerticalPadding = 8.dp,
    nameStyle = AppTypography.ListName.copy(fontSize = 14.sp, lineHeight = 18.sp),
    priceStyle = AppTypography.ListPrice.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    subStyle = AppTypography.Caption.copy(fontSize = 11.sp, lineHeight = 14.sp),
    orderBookRowHeight = 26.dp,
)

val ListDensity.tokens: ListDensityTokens
    get() = when (this) {
        ListDensity.COMFORTABLE -> Comfortable
        ListDensity.COMPACT -> Compact
    }

/** 사용자가 고른 목록 밀도. [com.eunilsung.ods.ui.main.MainScreen] 이 내려 준다. */
val LocalListDensity = compositionLocalOf { ListDensity.COMFORTABLE }
