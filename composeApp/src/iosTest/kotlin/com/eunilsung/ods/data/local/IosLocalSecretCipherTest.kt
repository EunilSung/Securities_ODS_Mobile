package com.eunilsung.ods.data.local

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** 테스트 실행 파일에는 Keychain 이 없어 메모리 키 저장소를 물리고 같은 CommonCrypto 코드를 돌린다. */
class IosLocalSecretCipherTest {

    private class MemoryKeyStore(var key: ByteArray? = null, var failing: Boolean = false) : IosLocalSecretCipher.KeyStore {
        var writes = 0
        override fun read(): ByteArray? = if (failing) error("keychain locked") else key
        override fun write(key: ByteArray): Boolean {
            writes++
            this.key = key
            return true
        }
        override fun delete() {
            key = null
        }
    }

    @Test
    fun sealsAndOpensWithStoredKey() {
        val store = MemoryKeyStore()
        val sealed = assertNotNull(IosLocalSecretCipher(store).seal("홍길동".encodeToByteArray()))
        assertContentEquals("홍길동".encodeToByteArray(), IosLocalSecretCipher(store).open(sealed))
    }

    @Test
    fun tamperedValueDoesNotOpen() {
        val cipher = IosLocalSecretCipher(MemoryKeyStore())
        val sealed = assertNotNull(cipher.seal("서울 중구".encodeToByteArray()))
        sealed[20] = (sealed[20].toInt() xor 1).toByte()
        assertNull(cipher.open(sealed))
    }

    @Test
    fun destroyedKeyLeavesOldValuesUnreadable() {
        val cipher = IosLocalSecretCipher(MemoryKeyStore())
        val sealed = assertNotNull(cipher.seal("7001-1234-01".encodeToByteArray()))
        cipher.destroyKey()
        assertNull(cipher.open(sealed))
    }

    @Test
    fun unreadableKeychainDoesNotCreateNewKey() {
        val store = MemoryKeyStore(failing = true)
        assertNull(IosLocalSecretCipher(store).seal("a".encodeToByteArray()))
        assertNull(store.key)
        assertEquals(0, store.writes)
    }
}
