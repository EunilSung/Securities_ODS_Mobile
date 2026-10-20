package com.eunilsung.ods.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.eunilsung.ods.Config
import com.eunilsung.ods.db.AppDatabase

/** 앱 전용 DB. `allowBackup=false` 와 `dataExtractionRules` 로 백업·기기 이전에서 빠진다. */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        Config.Database.LEGACY_NAMES.forEach(context::deleteDatabase)
        return AndroidSqliteDriver(AppDatabase.Schema, context, Config.Database.NAME)
    }
}
