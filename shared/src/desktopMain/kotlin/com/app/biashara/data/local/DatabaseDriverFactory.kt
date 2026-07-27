package com.app.biashara.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.app.biashara.db.Biashara360Database
import java.io.File
import java.sql.DriverManager

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbPath = File(System.getProperty("user.home"), ".biashara360/biashara360.db")
        val isNewDb = !dbPath.exists()
        dbPath.parentFile.mkdirs()
        if (!isNewDb) {
            migrateLegacyProductColumnOrder(dbPath)
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbPath.absolutePath}")
        if (isNewDb) {
            try {
                Biashara360Database.Schema.create(driver)
            } catch (e: Exception) {
                // Ignore if schema already created
            }
        }
        return driver
    }

    private fun migrateLegacyProductColumnOrder(dbPath: File) {
        DriverManager.getConnection("jdbc:sqlite:${dbPath.absolutePath}").use { connection ->
            val actualColumns = connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info(ProductEntity)").use { rows ->
                    buildList {
                        while (rows.next()) add(rows.getString("name"))
                    }
                }
            }
            val expectedColumns = listOf(
                "id", "business_id", "sku", "name", "description",
                "buying_price", "selling_price", "current_stock",
                "low_stock_threshold", "category", "barcode", "image_url",
                "is_active", "created_at", "updated_at"
            )
            if (actualColumns.isEmpty() || actualColumns == expectedColumns) return

            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        CREATE TABLE ProductEntity_migrated (
                            id TEXT NOT NULL PRIMARY KEY,
                            business_id TEXT NOT NULL,
                            sku TEXT NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT NOT NULL DEFAULT '',
                            buying_price REAL NOT NULL,
                            selling_price REAL NOT NULL,
                            current_stock INTEGER NOT NULL DEFAULT 0,
                            low_stock_threshold INTEGER NOT NULL DEFAULT 5,
                            category TEXT NOT NULL DEFAULT '',
                            barcode TEXT,
                            image_url TEXT,
                            is_active INTEGER NOT NULL DEFAULT 1,
                            created_at TEXT NOT NULL,
                            updated_at TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    statement.executeUpdate(
                        """
                        INSERT OR REPLACE INTO ProductEntity_migrated (
                            id, business_id, sku, name, description,
                            buying_price, selling_price, current_stock,
                            low_stock_threshold, category, barcode, image_url,
                            is_active, created_at, updated_at
                        )
                        SELECT
                            id, business_id, sku, name, description,
                            buying_price, selling_price, current_stock,
                            low_stock_threshold, category, barcode, image_url,
                            is_active, created_at, updated_at
                        FROM ProductEntity
                        """.trimIndent()
                    )
                    statement.executeUpdate("DROP TABLE ProductEntity")
                    statement.executeUpdate("ALTER TABLE ProductEntity_migrated RENAME TO ProductEntity")
                    statement.executeUpdate("CREATE INDEX idx_product_business ON ProductEntity(business_id)")
                    statement.executeUpdate("CREATE INDEX idx_product_stock ON ProductEntity(current_stock)")
                }
                connection.commit()
            } catch (error: Exception) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }
}
