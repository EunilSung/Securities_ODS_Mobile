package com.eunilsung.ods.testsupport

import com.eunilsung.ods.data.local.LocalSecret
import com.eunilsung.ods.data.local.LocalSecretCipher
import com.eunilsung.ods.data.local.SecureSettings
import com.russhwolf.settings.Settings

/**
 * 테스트용 암호. 플랫폼 보안 저장소(Keystore·Keychain)는 테스트 실행 파일에서 쓸 수 없어 대신 끼운다.
 *
 * 암호의 강도가 아니라 **저장하는 쪽이 잠그고 푸는 자리를 지키는지**를 보려는 것이다. 바이트를 뒤섞어 원문이 드러나지 않게 하고,
 * 키 번호를 함께 넣어 [destroyKey] 뒤에는 예전 값을 풀지 못하게 한다. 같은 키 번호면 다른 인스턴스끼리도 서로 푼다.
 */
class FakeLocalSecretCipher(private var keyId: Byte = 0) : LocalSecretCipher {
    var available: Boolean = true

    override fun seal(plain: ByteArray): ByteArray? =
        if (!available) null else byteArrayOf(MARK, keyId) + plain.map { (it.toInt() xor MASK).toByte() }

    override fun open(payload: ByteArray): ByteArray? {
        if (!available || payload.size < 2 || payload[0] != MARK || payload[1] != keyId) return null
        return payload.copyOfRange(2, payload.size).map { (it.toInt() xor MASK).toByte() }.toByteArray()
    }

    override fun destroyKey() {
        keyId++
    }

    private companion object {
        const val MARK: Byte = 0x4F
        const val MASK = 0x5A
    }
}

fun testLocalSecret(cipher: LocalSecretCipher = FakeLocalSecretCipher()): LocalSecret = LocalSecret(cipher)

fun testSecureSettings(settings: Settings, secret: LocalSecret = testLocalSecret()): SecureSettings = SecureSettings(settings, secret)
