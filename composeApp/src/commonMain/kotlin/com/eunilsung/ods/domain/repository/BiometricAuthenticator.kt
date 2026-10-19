package com.eunilsung.ods.domain.repository

import com.eunilsung.ods.domain.model.BiometricResult

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
