package com.app.biashara.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.app.biashara.db.Biashara360Database
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbPath = File(System.getProperty("user.home"), ".biashara360/biashara360.db")
        val isNewDb = !dbPath.exists()
        dbPath.parentFile.mkdirs()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbPath.absolutePath}")
        if (isNewDb) {
            try {
                Biashara360Database.Schema.create(driver)
            } catch (e: Exception) {
                // Ignore if schema already created
            }
        } else {
            try {
                driver.execute(null, "ALTER TABLE ProductEntity ADD COLUMN barcode TEXT", 0)
            } catch (_: Exception) {
                // Column already exists.
            }
        }
        return driver
    }
}
