package com.eunilsung.ods.data.remote.server

import com.eunilsung.ods.data.local.SecureSettings

/**
 * 서버 인증 토큰 보관소. 로그인할 때 받아 두고 이후 모든 요청과 실시간 알림 연결에 싣는다.
 *
 * [SecureSettings] 로 잠가 둔다 — 설정 파일이 빠져나가도 토큰이 읽히지 않는다.
 */
class AuthTokenStore(private val secureSettings: SecureSettings) {

    private var cached: String? = null

    /** 없으면 빈 문자열 — 로그인 전이라는 뜻이다. */
    fun token(): String =
        cached ?: secureSettings.getStringOrNull(KEY_TOKEN).orEmpty().also { cached = it }

    fun save(token: String) {
        cached = token
        secureSettings.putString(KEY_TOKEN, token)
    }

    fun clear() {
        cached = ""
        secureSettings.remove(KEY_TOKEN)
    }

    private companion object {
        const val KEY_TOKEN = "server.authToken"
    }
}
