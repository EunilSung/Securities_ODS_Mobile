package com.eunilsung.ods.data.local

import com.eunilsung.ods.db.AppDatabase
import com.eunilsung.ods.util.Log
import com.russhwolf.settings.Settings

/**
 * 기기 DB 와 그 DB 를 잠근 키가 짝이 맞는지 지킨다.
 *
 * 설정에 정해진 값을 잠가 둔 확인용 값을 두고, 앱이 DB 를 열 때 풀어 본다.
 * 풀리지 않으면 키가 바뀐 것이다(앱 데이터를 다른 기기로 옮겼거나 보안 저장소가 초기화됐다).
 * 그때는 풀 수 없는 업무 데이터와 남은 키를 지운다 — 다시 로그인하면 시연용 데이터로 처음부터 시작한다.
 * 잠가 둔 설정은 [SecureSettings] 가 없는 것으로 읽으므로 앱 잠금 비밀번호도 다시 정하게 된다.
 */
class LocalKeyGuard(
    private val database: AppDatabase,
    private val settings: Settings,
    private val secret: LocalSecret,
) {
    /** 앱이 DB 를 처음 열 때 한 번 부른다. 키가 없으면 여기서 만든다. */
    fun ensureReadable() {
        val check = settings.getStringOrNull(KEY_CHECK)
        if (check != null && secret.decryptOrNull(check) == CHECK_VALUE) return
        if (check != null) {
            database.appDatabaseQueries.wipeBusinessData()
            secret.destroyKey()
            Log.message("[LocalKey] key changed, business data wiped")
        }
        settings.putString(KEY_CHECK, secret.encrypt(CHECK_VALUE))
    }

    /** 키를 버리고 새 키로 확인용 값을 다시 둔다. 업무 데이터를 지운 뒤 부른다. */
    fun shred() {
        secret.destroyKey()
        settings.putString(KEY_CHECK, secret.encrypt(CHECK_VALUE))
    }

    private companion object {
        const val KEY_CHECK = "localKey.check"
        const val CHECK_VALUE = "ods"
    }
}
