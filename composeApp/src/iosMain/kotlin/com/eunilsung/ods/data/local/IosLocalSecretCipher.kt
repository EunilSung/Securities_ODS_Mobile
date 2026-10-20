package com.eunilsung.ods.data.local

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import korlibs.crypto.SecureRandom
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrService
import platform.posix.size_tVar
import kotlin.concurrent.Volatile
import kotlin.io.encoding.Base64

/**
 * iOS — AES-256-CBC 로 잠그고 HMAC-SHA256 으로 위·변조를 막는다(EzQ 와 같은 방식, 암호화한 뒤 서명).
 *
 * Kotlin 에 열린 CommonCrypto 에는 GCM 이 없어 두 단계로 나눈다. 잠금과 서명에 서로 다른 키를 쓴다.
 * 저장 형식은 `[16바이트 IV][암호문][32바이트 서명]` 이다.
 *
 * 키를 [keyStore] 로 받는 이유는 테스트 실행 파일에 Keychain 이 없기 때문이다(`errSecNotAvailable`).
 * 앱에서는 [IosKeychainKeyStore] 를, 테스트에서는 메모리 키를 물려 같은 암호 코드를 검증한다.
 */
internal class IosLocalSecretCipher(
    private val keyStore: KeyStore,
) : LocalSecretCipher {

    /** 64바이트(잠금 32 + 서명 32) 키를 두는 곳. */
    interface KeyStore {
        /** 없으면 null. **읽지 못하면 던진다** — 없는 것과 못 읽은 것을 갈라야 있던 키를 덮어쓰지 않는다. */
        fun read(): ByteArray?
        fun write(key: ByteArray): Boolean
        fun delete()
    }

    @Volatile private var cached: ByteArray? = null

    override fun seal(plain: ByteArray): ByteArray? {
        if (plain.isEmpty()) return null
        val material = material() ?: return null
        val iv = ByteArray(IV_SIZE).also { SecureRandom.nextBytes(it) }
        val cipherText = crypt(kCCEncrypt, plain, material.copyOfRange(0, CIPHER_KEY_SIZE), iv) ?: return null
        return iv + cipherText + hmac(material.copyOfRange(CIPHER_KEY_SIZE, KEY_SIZE), iv + cipherText)
    }

    override fun open(payload: ByteArray): ByteArray? {
        if (payload.size <= IV_SIZE + MAC_SIZE) return null
        val material = material() ?: return null
        val iv = payload.copyOfRange(0, IV_SIZE)
        val cipherText = payload.copyOfRange(IV_SIZE, payload.size - MAC_SIZE)
        val mac = payload.copyOfRange(payload.size - MAC_SIZE, payload.size)
        if (!hmac(material.copyOfRange(CIPHER_KEY_SIZE, KEY_SIZE), iv + cipherText).matches(mac)) return null
        return crypt(kCCDecrypt, cipherText, material.copyOfRange(0, CIPHER_KEY_SIZE), iv)
    }

    override fun destroyKey() {
        cached = null
        keyStore.delete()
    }

    /**
     * 키. 처음 한 번만 읽거나 만든다. 읽지 못하면 만들지 않는다 — 있던 키를 덮어쓰면 잠근 값이 영영 풀리지 않는다.
     * 처음 만드는 일은 앱이 DB 를 열 때 메인 스레드에서 한 번 일어난다([LocalKeyGuard.ensureReadable]).
     */
    private fun material(): ByteArray? {
        cached?.let { return it }
        val stored = runCatching { keyStore.read() }.getOrElse { return null }
        if (stored != null) return stored.takeIf { it.size == KEY_SIZE }?.also { cached = it }
        val fresh = ByteArray(KEY_SIZE).also { SecureRandom.nextBytes(it) }
        return if (keyStore.write(fresh)) fresh.also { cached = it } else null
    }

    /** 서명 비교는 끝까지 본다 — 중간에 끊으면 걸린 자리로 값을 좁혀 갈 수 있다. */
    private fun ByteArray.matches(other: ByteArray): Boolean {
        if (size != other.size) return false
        var diff = 0
        for (i in indices) diff = diff or (this[i].toInt() xor other[i].toInt())
        return diff == 0
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun crypt(operation: UInt, data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray? = memScoped {
        if (data.isEmpty()) return null
        val out = ByteArray(data.size + IV_SIZE)
        val moved = alloc<size_tVar>()
        val status = data.usePinned { dataPin ->
            key.usePinned { keyPin ->
                iv.usePinned { ivPin ->
                    out.usePinned { outPin ->
                        CCCrypt(
                            operation, kCCAlgorithmAES, kCCOptionPKCS7Padding,
                            keyPin.addressOf(0), key.size.convert(), ivPin.addressOf(0),
                            dataPin.addressOf(0), data.size.convert(),
                            outPin.addressOf(0), out.size.convert(), moved.ptr,
                        )
                    }
                }
            }
        }
        if (status.toInt() != kCCSuccess.toInt()) return null
        out.copyOfRange(0, moved.value.toInt())
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val out = ByteArray(MAC_SIZE)
        key.usePinned { keyPin ->
            data.usePinned { dataPin ->
                out.usePinned { outPin ->
                    CCHmac(
                        kCCHmacAlgSHA256,
                        keyPin.addressOf(0), key.size.convert(),
                        dataPin.addressOf(0), data.size.convert(),
                        outPin.addressOf(0),
                    )
                }
            }
        }
        return out
    }

    internal companion object {
        const val IV_SIZE = 16
        const val MAC_SIZE = 32
        const val CIPHER_KEY_SIZE = 32

        /** 잠금·서명 키를 합친 길이. */
        const val KEY_SIZE = 64
    }
}

/**
 * 키를 **Keychain** 에 둔다. 접근 등급은 `WhenUnlockedThisDeviceOnly` 다.
 *
 * - ODS 는 화면이 켜져 있을 때만 데이터를 쓴다(푸시로 깨어나 저장하는 일이 없다). 그래서 기기가 잠기면 키도 잠긴다.
 * - 백업이나 다른 기기로 따라가지 않는다. 백업이 복원돼 키가 없으면 [LocalKeyGuard] 가 업무 데이터를 지우고 새로 시작한다.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalSettingsImplementation::class, ExperimentalSettingsApi::class)
internal object IosKeychainKeyStore : IosLocalSecretCipher.KeyStore {

    private const val ENTRY = "local_key_v1"
    private const val SERVICE = "com.eunilsung.ods.localsecret"

    private val keychain by lazy {
        KeychainSettings(
            kSecAttrService to CFStringCreateWithCString(null, SERVICE, kCFStringEncodingUTF8),
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        )
    }

    /** multiplatform-settings 는 "없음" 이 아닌 Keychain 오류를 던진다. 그대로 올려 보낸다. */
    override fun read(): ByteArray? = keychain.getStringOrNull(ENTRY)?.let { Base64.decode(it) }

    override fun write(key: ByteArray): Boolean = runCatching {
        keychain.putString(ENTRY, Base64.encode(key))
        true
    }.getOrDefault(false)

    override fun delete() {
        runCatching { keychain.remove(ENTRY) }
    }
}
