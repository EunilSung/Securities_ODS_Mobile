package com.eunilsung.ods.data.local

import com.russhwolf.settings.Settings

/**
 * 설정 가운데 **잠가 두는 문자열**(앱 잠금 비밀번호의 salt·해시, 저장한 사번).
 *
 * 설정 파일(SharedPreferences · NSUserDefaults)은 평문이다. 6자리 비밀번호의 해시가 그대로 있으면
 * 파일만 빼내도 100만 번 대입으로 비밀번호를 찾을 수 있다. 잠가 두면 기기의 키 없이는 대입을 시작할 수 없다.
 *
 * 잠그기 전에 저장된 평문은 처음 읽을 때 잠가 다시 쓴다. 풀지 못하는 값은 없는 것으로 본다.
 */
class SecureSettings(
    private val settings: Settings,
    private val secret: LocalSecret,
) {
    fun getStringOrNull(key: String): String? {
        val stored = settings.getStringOrNull(key) ?: return null
        val plain = secret.decryptOrNull(stored) ?: return null
        if (!secret.isSealed(stored) && plain.isNotEmpty()) settings.putString(key, secret.encrypt(plain))
        return plain
    }

    fun putString(key: String, value: String) = settings.putString(key, secret.encrypt(value))

    fun remove(key: String) = settings.remove(key)
}
