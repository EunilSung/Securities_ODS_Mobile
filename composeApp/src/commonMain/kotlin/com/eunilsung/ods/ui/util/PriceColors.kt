package com.eunilsung.ods.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eunilsung.ods.domain.model.PriceDirection
import com.eunilsung.ods.domain.model.Stock
import com.eunilsung.ods.ui.theme.AppColors
import com.eunilsung.ods.util.toSignedRate
import com.eunilsung.ods.util.withComma
import kotlin.math.abs

/** 등락 방향의 글자색 — 상승 빨강, 하락 파랑, 보합 기본색. */
val PriceDirection.color: Color
    @Composable get() = when (this) {
        PriceDirection.UP -> AppColors.Rise
        PriceDirection.DOWN -> AppColors.Fall
        PriceDirection.FLAT -> AppColors.Text
    }

/** 등락 기호. 색만으로 등락을 알리지 않도록 국내 시세 표기처럼 ▲·▼ 를 함께 쓴다. */
val PriceDirection.symbol: String
    get() = when (this) {
        PriceDirection.UP -> "▲"
        PriceDirection.DOWN -> "▼"
        PriceDirection.FLAT -> ""
    }

/** 전일 대비 한 줄 — "▲700 +0.99%". [separator] 는 변화량과 등락률 사이. */
fun Stock.changeLabel(separator: String = "  "): String =
    "${direction.symbol}${abs(change).withComma()}$separator${changeRate.toSignedRate()}%"
