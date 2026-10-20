package com.eunilsung.ods.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseFileContext
import com.eunilsung.ods.Config
import com.eunilsung.ods.db.AppDatabase

/** 앱 전용 DB. DB 폴더를 iCloud·Finder 백업에서 뺀다([excludeFromBackup]). */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        Config.Database.LEGACY_NAMES.forEach { DatabaseFileContext.deleteDatabase(it) }
        val driver = NativeSqliteDriver(AppDatabase.Schema, Config.Database.NAME)
        excludeFromBackup(DatabaseFileContext.databasePath(Config.Database.NAME, null).substringBeforeLast('/'))
        return driver
    }
}
