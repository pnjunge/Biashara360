package com.app.biashara.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.app.biashara.db.Biashara360Database

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = Biashara360Database.Schema,
            name = "biashara360.db"
        )
    }
}
