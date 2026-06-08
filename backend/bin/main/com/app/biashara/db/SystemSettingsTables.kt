package com.app.biashara.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ─── System-wide key/value settings (SUPERADMIN managed) ─────────────────────

object SystemSettingsTable : Table("system_settings") {
    val key       = varchar("key", 100)
    val value     = text("value")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(key)
}
