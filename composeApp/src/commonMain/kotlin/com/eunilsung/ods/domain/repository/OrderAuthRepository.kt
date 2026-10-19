package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.BiometricResult
import com.eunilsung.ods.domain.model.OrderAuthConfig
import com.eunilsung.ods.domain.model.PinCheckResult
import kotlinx.coroutines.flow.StateFlow

/** 주문 인증 — 주문 비밀번호와 생체 인증 사용 여부. */
interface OrderAuthRepository {
    val config: StateFlow<OrderAuthConfig>

    /** 비밀번호를 새로 등록한다. 틀린 횟수와 잠금도 푼다. */
    fun setPin(pin: String)

    /** 비밀번호를 확인한다. [nowEpochSeconds] 는 잠금 계산에 쓴다. */
    fun verifyPin(pin: String, nowEpochSeconds: Long): PinCheckResult

    fun setBiometricEnabled(enabled: Boolean)
}

/** 생체 인증 — Android 지문·얼굴 / iOS Face ID·Touch ID. 구현은 플랫폼마다 둔다. */
interface BiometricAuthenticator {
    /** 기기에 지금 쓸 수 있는 생체가 등록돼 있는지. */
    fun isAvailable(): Boolean

    /**
     * 생체 프롬프트를 띄우고 결과를 돌려준다. 화면(메인) 스레드에서 부른다.
     *
     * @param negativeText 취소 버튼 문구. iOS 는 취소 버튼 문구로 쓴다.
     */
    suspend fun authenticate(title: String, subtitle: String, negativeText: String): BiometricResult
}
