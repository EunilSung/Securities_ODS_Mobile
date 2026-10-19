package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.ListDensity
import kotlinx.coroutines.flow.StateFlow

/** 기기에 남기는 사용자 선택. */
interface UserPreferenceRepository {
    val listDensity: StateFlow<ListDensity>

    fun setListDensity(density: ListDensity)

    /** 현재가 탭이 열 종목 — 마지막으로 본 종목. 아직 없으면 null. */
    fun lastQuoteCode(): String?

    fun setLastQuoteCode(code: String)

    /** 잔고 금액 가리기. 공공장소에서 앱을 열 때를 위해 다음 실행에도 그대로 둔다. */
    val hideAmounts: StateFlow<Boolean>

    fun setHideAmounts(hide: Boolean)
}
