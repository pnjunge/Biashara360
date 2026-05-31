package com.app.biashara.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ─── Per-business Mpesa Configuration ────────────────────────────────────────
// 🔒 SECURITY: Secrets are now encrypted at application level before storage

object MpesaConfigsTable : Table("mpesa_configs") {
    val id             = varchar("id", 36)
    val businessId     = varchar("business_id", 36).references(BusinessesTable.id).uniqueIndex()
    // 🔒 SECURITY FIX: Increased field size for encrypted values (base64 encoded)
    val consumerKey    = text("consumer_key")        // Encrypted
    val consumerSecret = text("consumer_secret")     // Encrypted
    val shortCode      = varchar("short_code", 20)
    val passKey        = text("pass_key")            // Encrypted
    val callbackUrl    = varchar("callback_url", 500)
    val environment    = varchar("environment", 20).default("sandbox")  // sandbox | production
    val accountType    = varchar("account_type", 10).default("paybill") // paybill | till
    // 🔒 SECURITY: Track encryption version for key rotation
    val encryptionVersion = integer("encryption_version").default(1)
    val createdAt      = timestamp("created_at")
    val updatedAt      = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Per-business CyberSource Configuration ───────────────────────────────────
// 🔒 SECURITY: Secrets are now encrypted at application level before storage

object CyberSourceConfigsTable : Table("cybersource_configs") {
    val id                = varchar("id", 36)
    val businessId        = varchar("business_id", 36).references(BusinessesTable.id).uniqueIndex()
    val merchantId        = varchar("merchant_id", 255)
    val merchantKeyId     = varchar("merchant_key_id", 255)
    // 🔒 SECURITY FIX: Increased field size for encrypted values (base64 encoded)
    val merchantSecretKey = text("merchant_secret_key") // Encrypted
    val environment       = varchar("environment", 20).default("sandbox")  // sandbox | production
    // 🔒 SECURITY: Track encryption version for key rotation
    val encryptionVersion = integer("encryption_version").default(1)
    val createdAt         = timestamp("created_at")
    val updatedAt         = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Audit Log for Configuration Changes ───────────────────────────────────────
// 🔒 SECURITY: Track who changed what and when for compliance
object ConfigurationAuditTable : Table("configuration_audit") {
    val id            = varchar("id", 36)
    val businessId    = varchar("business_id", 36).references(BusinessesTable.id)
    val userId        = varchar("user_id", 36).nullable().references(UsersTable.id)
    val entityType    = varchar("entity_type", 50)  // MPESA_CONFIG, CYBERSOURCE_CONFIG, etc.
    val entityId      = varchar("entity_id", 36)
    val action        = varchar("action", 20)       // CREATE, UPDATE, DELETE
    val changesSummary = text("changes_summary")    // JSON description of what changed
    val timestamp     = timestamp("timestamp")
    override val primaryKey = PrimaryKey(id)
    index(false, businessId, timestamp)
}
