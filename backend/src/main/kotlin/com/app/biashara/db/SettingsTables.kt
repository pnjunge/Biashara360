package com.app.biashara.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ─── Per-Business CyberSource Configuration ───────────────────────────────────
// Admins set their own CyberSource merchant credentials here.
// If no row exists for a business, the global application config is used as fallback.
object CyberSourceConfigsTable : Table("cybersource_configs") {
    val id                = varchar("id", 36)
    val businessId        = varchar("business_id", 36).references(BusinessesTable.id).uniqueIndex()
    val merchantId        = varchar("merchant_id", 100)
    val merchantKeyId     = varchar("merchant_key_id", 100)
    val merchantSecretKey = text("merchant_secret_key")
    val environment       = varchar("environment", 20).default("sandbox")
    val isActive          = bool("is_active").default(true)
    val createdAt         = timestamp("created_at")
    val updatedAt         = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Per-Business M-Pesa (Daraja) Configuration ───────────────────────────────
// Admins set their own Safaricom Daraja API credentials here.
// If no row exists for a business, the global application config is used as fallback.
object MpesaConfigsTable : Table("mpesa_configs") {
    val id            = varchar("id", 36)
    val businessId    = varchar("business_id", 36).references(BusinessesTable.id).uniqueIndex()
    val consumerKey   = text("consumer_key")
    val consumerSecret = text("consumer_secret")
    val shortCode     = varchar("short_code", 20)
    val passKey       = text("pass_key")
    val callbackUrl   = varchar("callback_url", 500)
    val environment   = varchar("environment", 20).default("sandbox")
    val isActive      = bool("is_active").default(true)
    val createdAt     = timestamp("created_at")
    val updatedAt     = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}
