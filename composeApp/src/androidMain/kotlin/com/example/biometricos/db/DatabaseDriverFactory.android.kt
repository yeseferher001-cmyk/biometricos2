package com.example.biometricos.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.biometricos.db.AppDatabase
import com.example.biometricos.currentActivity

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(AppDatabase.Schema, currentActivity, "app.db")
    }
}
