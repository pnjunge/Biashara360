package com.app.biashara.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ─── Per-business Mpesa Configuration ────────────────────────────────────────

object MpesaConfigsTable : Table("mpesa_configs") {
    val id             = varchar("id", 36)
    val businessId     = varchar("business_id", 36).references(BusinessesTable.id).uniqueIndex()
    val consumerKey    = varchar("consumer_key", 255)
    val consumerSecret = varchar("consumer_secret", 255)
    val shortCode      = varchar("short_code", 20)
    val passKey        = varchar("pass_key", 500)
    val callbackUrl    = varchar("callback_url", 500)
    val environment    = varchar("environment", 20).default("sandbox")  // sandbox | production
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Per-business CyberSource Configuration ───────────────────────────────────

object CyberSourceConfigsTable : Table("cybersource_configs") {
    val id                = varchar("id", 36)
    val businessId        = varchar("business_id", 36).references(BusinessesTable.id).uniqueIndex()
    val merchantId        = varchar("merchant_id", 255)
    val merchantKeyId     = varchar("merchant_key_id", 255)
    val merchantSecretKey = varchar("merchant_secret_key", 500)
    val environment       = varchar("environment", 20).default("sandbox")  // sandbox | production
    val createdAt         = timestamp("created_at")
    val updatedAt         = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}
