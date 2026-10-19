package com.eunilsung.ods.ui.balance

/** 가린 금액·수량 자리에 보일 글자. 숫자 길이를 드러내지 않도록 늘 같은 길이다. */
const val MASKED_VALUE = "••••••"

/** [hide] 면 [MASKED_VALUE], 아니면 [text]. */
fun masked(text: String, hide: Boolean): String = if (hide) MASKED_VALUE else text
