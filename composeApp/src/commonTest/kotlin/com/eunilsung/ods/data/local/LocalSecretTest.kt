package com.eunilsung.ods.data.local

import com.eunilsung.ods.db.SubscriptionEntity
import com.eunilsung.ods.testsupport.FakeLocalSecretCipher
import com.eunilsung.ods.testsupport.createTestDatabase
import com.eunilsung.ods.testsupport.testLocalSecret
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalSecretTest {
    private val cipher = FakeLocalSecretCipher()
    private val secret = testLocalSecret(cipher)

    @Test
    fun sealedValueOpensBackAndHidesPlainText() {
        val sealed = secret.encrypt("010-1234-5678")
        assertTrue(secret.isSealed(sealed))
        assertFalse(sealed.contains("1234"))
        assertEquals("010-1234-5678", secret.decrypt(sealed))
    }

    @Test
    fun emptyAndPlainValuesPassThrough() {
        assertEquals("", secret.encrypt(""))
        assertEquals("평문", secret.decryptOrNull("평문"))
    }

    @Test
    fun failingToSealThrowsInsteadOfStoringPlainText() {
        cipher.available = false
        assertFailsWith<LocalSecretException> { secret.encrypt("홍길동") }
    }

    @Test
    fun destroyedKeyCannotOpenOldValues() {
        val sealed = secret.encrypt("서울 중구")
        secret.destroyKey()
        assertNull(secret.decryptOrNull(sealed))
        assertEquals("", secret.decrypt(sealed))
    }
}

class SecureSettingsTest {
    private val settings = MapSettings()
    private val secret = testLocalSecret()
    private val secure = SecureSettings(settings, secret)

    @Test
    fun storesSealedAndReadsPlain() {
        secure.putString("appLock.test1.hash", "abc123")
        assertTrue(secret.isSealed(settings.getString("appLock.test1.hash", "")))
        assertEquals("abc123", secure.getStringOrNull("appLock.test1.hash"))
    }

    @Test
    fun plainValueStoredBeforeSealingIsSealedOnFirstRead() {
        settings.putString("login.savedId", "test1")
        assertEquals("test1", secure.getStringOrNull("login.savedId"))
        assertTrue(secret.isSealed(settings.getString("login.savedId", "")))
    }

    @Test
    fun unreadableValueCountsAsMissing() {
        secure.putString("appLock.test1.hash", "abc123")
        secret.destroyKey()
        assertNull(secure.getStringOrNull("appLock.test1.hash"))
    }
}

class LocalKeyGuardTest {
    private val database = createTestDatabase()
    private val settings = MapSettings()
    private val cipher = FakeLocalSecretCipher()
    private val secret = testLocalSecret(cipher)

    private fun insertSealedRow() = database.appDatabaseQueries.insertSubscription(
        SubscriptionEntity("S1", "C1", "P1", secret.encrypt("7001-0000-01"), 1_000, 1_000, 0, null),
    )

    private fun subscriptionCount() = database.appDatabaseQueries.selectSubscriptions("C1").executeAsList().size

    @Test
    fun keepsDataWhenKeyMatches() {
        LocalKeyGuard(database, settings, secret).ensureReadable()
        insertSealedRow()
        LocalKeyGuard(database, settings, secret).ensureReadable()
        assertEquals(1, subscriptionCount())
    }

    @Test
    fun wipesDataWhenKeyChanged() {
        LocalKeyGuard(database, settings, secret).ensureReadable()
        insertSealedRow()
        val otherDevice = testLocalSecret(FakeLocalSecretCipher(keyId = 7))
        LocalKeyGuard(database, settings, otherDevice).ensureReadable()
        assertEquals(0, subscriptionCount())
        LocalKeyGuard(database, settings, otherDevice).ensureReadable()
        assertEquals(0, subscriptionCount())
    }
}
