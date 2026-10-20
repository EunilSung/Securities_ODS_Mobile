package com.eunilsung.ods.data.local

import java.io.File
import java.nio.file.Files
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** JVM 에는 Android Keystore 가 없어 같은 AES-GCM 을 소프트웨어 키로 돌리는 KEK 를 물린다. */
class EnvelopeLocalSecretCipherTest {
    private val directory: File = Files.createTempDirectory("ods-dek").toFile()
    private val dekFile = File(directory, "dek")
    private val kek = SoftwareKek(ByteArray(32) { it.toByte() })

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun sealsWithDataKeyKeptWrappedOnDisk() {
        val sealed = assertNotNull(EnvelopeLocalSecretCipher(kek, dekFile).seal("홍길동".encodeToByteArray()))
        assertTrue(dekFile.isFile)
        val reopened = EnvelopeLocalSecretCipher(kek, dekFile).open(sealed)
        assertContentEquals("홍길동".encodeToByteArray(), reopened)
    }

    @Test
    fun tamperedValueDoesNotOpen() {
        val cipher = EnvelopeLocalSecretCipher(kek, dekFile)
        val sealed = assertNotNull(cipher.seal("서울 중구".encodeToByteArray()))
        sealed[sealed.lastIndex] = (sealed.last().toInt() xor 1).toByte()
        assertNull(cipher.open(sealed))
    }

    @Test
    fun destroyedKeyLeavesOldValuesUnreadable() {
        val cipher = EnvelopeLocalSecretCipher(kek, dekFile)
        val sealed = assertNotNull(cipher.seal("7001-1234-01".encodeToByteArray()))
        cipher.destroyKey()
        assertNull(cipher.open(sealed))
        assertNotNull(cipher.seal("new".encodeToByteArray()))
    }

    @Test
    fun keyFileThatCannotBeUnwrappedIsNotOverwritten() {
        EnvelopeLocalSecretCipher(kek, dekFile).seal("a".encodeToByteArray())
        val before = dekFile.readBytes()
        val otherKek = SoftwareKek(ByteArray(32) { 9 })
        assertNull(EnvelopeLocalSecretCipher(otherKek, dekFile).seal("b".encodeToByteArray()))
        assertContentEquals(before, dekFile.readBytes())
    }

    private class SoftwareKek(raw: ByteArray) : LocalSecretCipher {
        private val key = SecretKeySpec(raw, "AES")

        override fun seal(plain: ByteArray): ByteArray? {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return cipher.iv + cipher.doFinal(plain)
        }

        override fun open(payload: ByteArray): ByteArray? = runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, payload, 0, 12))
            cipher.doFinal(payload, 12, payload.size - 12)
        }.getOrNull()

        override fun destroyKey() = Unit
    }
}
