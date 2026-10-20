package com.eunilsung.ods.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android — **데이터 키로 잠그고, 데이터 키는 Keystore 키로 잠근다**(EzQ 와 같은 봉투 방식).
 *
 * 칸마다 Keystore 를 부르면 보안 칩 IPC 때문에 칸당 수 ms 가 든다. 그래서 AES-256 데이터 키 하나를 [kek] 로 잠가
 * [dekFile] 에 두고, 프로세스에서 한 번만 풀어 메모리에 둔다. 칸은 그 키로 소프트웨어 AES-GCM 을 돌린다.
 *
 * - 디스크에는 잠긴 데이터 키만 남는다. Keystore 키는 기기 밖으로 나가지 않아 앱 데이터를 통째로 빼내도 풀 수 없다.
 * - [dekFile] 은 `noBackupFilesDir` 아래라 백업·기기 이전에서 빠진다.
 * - 저장 형식은 `[12바이트 IV][암호문+인증태그]`. GCM 이라 위·변조도 걸러진다.
 */
internal class EnvelopeLocalSecretCipher(
    private val kek: LocalSecretCipher,
    private val dekFile: File,
) : LocalSecretCipher {

    private val random = SecureRandom()
    private val lock = Any()

    @Volatile private var dek: SecretKey? = null

    override fun seal(plain: ByteArray): ByteArray? {
        val key = dataKey() ?: return null
        return runCatching {
            val iv = ByteArray(IV_SIZE).also(random::nextBytes)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
            iv + cipher.doFinal(plain)
        }.getOrNull()
    }

    override fun open(payload: ByteArray): ByteArray? {
        if (payload.size <= IV_SIZE) return null
        val key = dataKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, payload, 0, IV_SIZE))
            cipher.doFinal(payload, IV_SIZE, payload.size - IV_SIZE)
        }.getOrNull()
    }

    override fun destroyKey() = synchronized(lock) {
        dek = null
        dekFile.delete()
        Unit
    }

    /**
     * 데이터 키. 처음 한 번만 풀거나 만든다.
     *
     * 파일이 있는데 풀지 못하면 **덮어쓰지 않는다** — 덮어쓰면 그 키로 잠근 값이 영영 풀리지 않는다.
     * 키가 정말 바뀐 경우는 [LocalKeyGuard] 가 데이터를 지운 뒤 [destroyKey] 로 새로 시작한다.
     */
    private fun dataKey(): SecretKey? {
        dek?.let { return it }
        synchronized(lock) {
            dek?.let { return it }
            return loadOrCreate()?.also { dek = it }
        }
    }

    private fun loadOrCreate(): SecretKey? {
        if (dekFile.isFile) {
            val wrapped = runCatching { dekFile.readBytes() }.getOrNull() ?: return null
            return kek.open(wrapped)?.takeIf { it.size == DEK_SIZE }?.let { SecretKeySpec(it, ALGORITHM) }
        }
        val raw = ByteArray(DEK_SIZE).also(random::nextBytes)
        val wrapped = kek.seal(raw) ?: return null
        val written = runCatching {
            dekFile.parentFile?.mkdirs()
            val temp = File(dekFile.parentFile, dekFile.name + ".tmp")
            temp.writeBytes(wrapped)
            temp.renameTo(dekFile)
        }.getOrDefault(false)
        return if (written) SecretKeySpec(raw, ALGORITHM) else null
    }

    private companion object {
        const val DEK_SIZE = 32
        const val ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
    }
}

/**
 * Keystore 안에서 만든 AES-256-GCM 키. 앱은 키 값을 볼 수 없다. 데이터 키를 잠그는 데만 쓴다([EnvelopeLocalSecretCipher]).
 *
 * 앱이 화면에 떠 있을 때만 쓰지만, 기기 잠금 화면 설정과 상관없이 돌게 사용자 인증은 걸지 않는다 — 직원 인증은 앱 잠금이 한다.
 */
internal object AndroidKeystoreKek : LocalSecretCipher {

    private const val ALIAS = "ods_local_kek_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE_BITS = 128

    private val key: SecretKey by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { loadOrCreateKey() }

    override fun seal(plain: ByteArray): ByteArray? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.iv + cipher.doFinal(plain)
    }.getOrNull()

    override fun open(payload: ByteArray): ByteArray? {
        if (payload.size <= IV_SIZE) return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, payload, 0, IV_SIZE))
            cipher.doFinal(payload, IV_SIZE, payload.size - IV_SIZE)
        }.getOrNull()
    }

    /** 데이터 키를 버리면 충분하다. Keystore 키는 남겨 새 데이터 키를 잠그는 데 다시 쓴다. */
    override fun destroyKey() = Unit

    private fun loadOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }
}
