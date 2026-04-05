package com.app.biashara.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.app.biashara.db.Biashara360Database

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = Biashara360Database.Schema,
            context = context,
            name = "biashara360.db"
        )
    }
}
