package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.CalendarMode
import com.eunilsung.ods.domain.model.ListDensity
import kotlinx.coroutines.flow.StateFlow

/** 기기에 남기는 사용자 선택. */
interface UserPreferenceRepository {
    val listDensity: StateFlow<ListDensity>

    fun setListDensity(density: ListDensity)

    /**
     * 고객 개인정보(전화번호·생년월일·계좌번호) 가리기. 고객 앞이나 공공장소에서 태블릿을 펼칠 때를 위해 기본으로 켜 두고,
     * 다음 실행에도 그대로 둔다.
     */
    val maskPersonalInfo: StateFlow<Boolean>

    fun setMaskPersonalInfo(mask: Boolean)

    /** 일정 탭 달력을 주로 볼지 달로 볼지. 마지막에 고른 것을 다음 실행에도 그대로 둔다. */
    val scheduleCalendarMode: StateFlow<CalendarMode>

    fun setScheduleCalendarMode(mode: CalendarMode)
}
