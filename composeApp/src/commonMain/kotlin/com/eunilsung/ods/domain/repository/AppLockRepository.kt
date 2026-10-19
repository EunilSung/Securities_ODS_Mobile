package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.AppLockState
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱 잠금 — 직원별 잠금 비밀번호, 생체 인증 사용 여부, 지금 잠겨 있는지와 마지막 조작 시각.
 *
 * 비밀번호는 해시로만 남기고, 틀린 횟수는 기기가 센다. 잠금 여부와 조작 시각은 실행 중에만 들고 있다.
 */
interface AppLockRepository {
    val state: StateFlow<AppLockState>

    /** [staffId] 가 로그인했다. 그 직원의 비밀번호·생체 설정을 읽고 잠금을 푼 상태로 시작한다. */
    fun startSession(staffId: String, nowMillis: Long)

    /** 로그아웃했다. 잠금 상태를 비운다(비밀번호는 남긴다). */
    fun endSession()

    /** 비밀번호를 새로 정한다. 틀린 횟수를 지우고 잠금을 푼다. */
    fun setPin(pin: String)

    /** 비밀번호가 맞는지. 틀리면 횟수를 하나 올리고 남은 횟수를 돌려준다. 맞으면 횟수를 지운다. */
    fun checkPin(pin: String): PinCheck

    fun setBiometricEnabled(enabled: Boolean)

    fun lock()

    fun unlock()

    /** 화면을 만졌다. */
    fun recordActivity(nowMillis: Long)

    val lastActivityMillis: Long

    /** 앱이 뒤로 갔다. */
    fun recordBackground(nowMillis: Long)

    /** 마지막으로 뒤로 간 시각. 앞으로 와 있는 동안은 null. */
    fun takeBackgroundSince(): Long?

    /** 이 기기에 남은 모든 직원의 잠금 비밀번호와 생체 설정을 지운다. */
    fun clearAll()

    sealed interface PinCheck {
        data object Matched : PinCheck
        data class Wrong(val failures: Int) : PinCheck
    }
}

/** 기기 안 업무 데이터(고객·상담·일정·가입 기록)를 지운다. 단말 분실 대비용이다. */
interface LocalDataWiper {
    suspend fun wipe()
}
