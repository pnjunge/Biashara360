package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.CyberSourceConfigsTable
import com.app.biashara.db.MpesaConfigsTable
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class SettingsService {

    // ── CyberSource ────────────────────────────────────────────────────────────

    fun getCyberSourceSettings(businessId: String): ApiResponse<CyberSourceSettingsResponse> = transaction {
        val row = CyberSourceConfigsTable
            .select { CyberSourceConfigsTable.businessId eq businessId }
            .firstOrNull()

        if (row == null) {
            ApiResponse(
                success = true,
                data = CyberSourceSettingsResponse(
                    merchantId    = "",
                    merchantKeyId = "",
                    environment   = "sandbox",
                    isConfigured  = false
                )
            )
        } else {
            ApiResponse(
                success = true,
                data = CyberSourceSettingsResponse(
                    merchantId    = row[CyberSourceConfigsTable.merchantId],
                    merchantKeyId = row[CyberSourceConfigsTable.merchantKeyId],
                    environment   = row[CyberSourceConfigsTable.environment],
                    isConfigured  = row[CyberSourceConfigsTable.isActive]
                )
            )
        }
    }

    fun saveCyberSourceSettings(businessId: String, req: CyberSourceSettingsRequest): ApiResponse<CyberSourceSettingsResponse> = transaction {
        if (req.merchantId.isBlank() || req.merchantKeyId.isBlank() || req.merchantSecretKey.isBlank()) {
            return@transaction ApiResponse(false, message = "merchantId, merchantKeyId, and merchantSecretKey are required")
        }
        val env = req.environment.lowercase()
        if (env !in listOf("sandbox", "production")) {
            return@transaction ApiResponse(false, message = "environment must be 'sandbox' or 'production'")
        }

        val now      = Clock.System.now()
        val existing = CyberSourceConfigsTable
            .select { CyberSourceConfigsTable.businessId eq businessId }
            .firstOrNull()

        if (existing != null) {
            CyberSourceConfigsTable.update({ CyberSourceConfigsTable.businessId eq businessId }) {
                it[merchantId]        = req.merchantId
                it[merchantKeyId]     = req.merchantKeyId
                it[merchantSecretKey] = req.merchantSecretKey
                it[environment]       = env
                it[isActive]          = true
                it[updatedAt]         = now
            }
        } else {
            CyberSourceConfigsTable.insert {
                it[id]                = generateId()
                it[CyberSourceConfigsTable.businessId] = businessId
                it[merchantId]        = req.merchantId
                it[merchantKeyId]     = req.merchantKeyId
                it[merchantSecretKey] = req.merchantSecretKey
                it[environment]       = env
                it[isActive]          = true
                it[createdAt]         = now
                it[updatedAt]         = now
            }
        }

        ApiResponse(
            success = true,
            data = CyberSourceSettingsResponse(
                merchantId    = req.merchantId,
                merchantKeyId = req.merchantKeyId,
                environment   = env,
                isConfigured  = true
            ),
            message = "CyberSource settings saved"
        )
    }

    // ── M-Pesa ─────────────────────────────────────────────────────────────────

    fun getMpesaSettings(businessId: String): ApiResponse<MpesaSettingsResponse> = transaction {
        val row = MpesaConfigsTable
            .select { MpesaConfigsTable.businessId eq businessId }
            .firstOrNull()

        if (row == null) {
            ApiResponse(
                success = true,
                data = MpesaSettingsResponse(
                    shortCode    = "",
                    callbackUrl  = "",
                    environment  = "sandbox",
                    isConfigured = false
                )
            )
        } else {
            ApiResponse(
                success = true,
                data = MpesaSettingsResponse(
                    shortCode    = row[MpesaConfigsTable.shortCode],
                    callbackUrl  = row[MpesaConfigsTable.callbackUrl],
                    environment  = row[MpesaConfigsTable.environment],
                    isConfigured = row[MpesaConfigsTable.isActive]
                )
            )
        }
    }

    fun saveMpesaSettings(businessId: String, req: MpesaSettingsRequest): ApiResponse<MpesaSettingsResponse> = transaction {
        if (req.consumerKey.isBlank() || req.consumerSecret.isBlank() || req.shortCode.isBlank() || req.passKey.isBlank() || req.callbackUrl.isBlank()) {
            return@transaction ApiResponse(false, message = "consumerKey, consumerSecret, shortCode, passKey, and callbackUrl are required")
        }
        val env = req.environment.lowercase()
        if (env !in listOf("sandbox", "production")) {
            return@transaction ApiResponse(false, message = "environment must be 'sandbox' or 'production'")
        }

        val now      = Clock.System.now()
        val existing = MpesaConfigsTable
            .select { MpesaConfigsTable.businessId eq businessId }
            .firstOrNull()

        if (existing != null) {
            MpesaConfigsTable.update({ MpesaConfigsTable.businessId eq businessId }) {
                it[consumerKey]    = req.consumerKey
                it[consumerSecret] = req.consumerSecret
                it[shortCode]      = req.shortCode
                it[passKey]        = req.passKey
                it[callbackUrl]    = req.callbackUrl
                it[environment]    = env
                it[isActive]       = true
                it[updatedAt]      = now
            }
        } else {
            MpesaConfigsTable.insert {
                it[id]                        = generateId()
                it[MpesaConfigsTable.businessId] = businessId
                it[consumerKey]               = req.consumerKey
                it[consumerSecret]            = req.consumerSecret
                it[shortCode]                 = req.shortCode
                it[passKey]                   = req.passKey
                it[callbackUrl]               = req.callbackUrl
                it[environment]               = env
                it[isActive]                  = true
                it[createdAt]                 = now
                it[updatedAt]                 = now
            }
        }

        ApiResponse(
            success = true,
            data = MpesaSettingsResponse(
                shortCode    = req.shortCode,
                callbackUrl  = req.callbackUrl,
                environment  = env,
                isConfigured = true
            ),
            message = "M-Pesa settings saved"
        )
    }

    // ── Helpers for other services ─────────────────────────────────────────────

    /** Returns the CyberSource DB config for a business, or null if not configured. */
    fun loadCyberSourceConfig(businessId: String): CyberSourceConfig? = transaction {
        val row = CyberSourceConfigsTable
            .select { (CyberSourceConfigsTable.businessId eq businessId) and (CyberSourceConfigsTable.isActive eq true) }
            .firstOrNull() ?: return@transaction null

        CyberSourceConfig(
            merchantId        = row[CyberSourceConfigsTable.merchantId],
            merchantKeyId     = row[CyberSourceConfigsTable.merchantKeyId],
            merchantSecretKey = row[CyberSourceConfigsTable.merchantSecretKey],
            environment       = row[CyberSourceConfigsTable.environment]
        )
    }

    /** Returns the M-Pesa DB config for a business, or null if not configured. */
    fun loadMpesaConfig(businessId: String): MpesaDbConfig? = transaction {
        val row = MpesaConfigsTable
            .select { (MpesaConfigsTable.businessId eq businessId) and (MpesaConfigsTable.isActive eq true) }
            .firstOrNull() ?: return@transaction null

        MpesaDbConfig(
            consumerKey    = row[MpesaConfigsTable.consumerKey],
            consumerSecret = row[MpesaConfigsTable.consumerSecret],
            shortCode      = row[MpesaConfigsTable.shortCode],
            passKey        = row[MpesaConfigsTable.passKey],
            callbackUrl    = row[MpesaConfigsTable.callbackUrl],
            environment    = row[MpesaConfigsTable.environment]
        )
    }
}

/** Holds resolved M-Pesa credentials from the database. */
data class MpesaDbConfig(
    val consumerKey: String,
    val consumerSecret: String,
    val shortCode: String,
    val passKey: String,
    val callbackUrl: String,
    val environment: String
)
