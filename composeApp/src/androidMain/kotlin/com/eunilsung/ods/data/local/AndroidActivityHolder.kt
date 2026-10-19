package com.eunilsung.ods.data.local

import androidx.fragment.app.FragmentActivity

/** 지금 화면에 떠 있는 액티비티. 생체 인증 프롬프트처럼 액티비티가 있어야 하는 기능이 쓴다. */
object AndroidActivityHolder {
    @Volatile var activity: FragmentActivity? = null
}
