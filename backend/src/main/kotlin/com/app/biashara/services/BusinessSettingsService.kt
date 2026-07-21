package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.CyberSourceConfigsTable
import com.app.biashara.db.MpesaConfigsTable
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class BusinessSettingsService {

    // ── Mpesa Config ──────────────────────────────────────────────────────────

    fun getMpesaConfig(businessId: String): MpesaConfigResponse? = transaction {
        MpesaConfigsTable
            .select { MpesaConfigsTable.businessId eq businessId }
            .firstOrNull()
            ?.let {
                MpesaConfigResponse(
                    businessId   = businessId,
                    shortCode    = it[MpesaConfigsTable.shortCode],
                    callbackUrl  = it[MpesaConfigsTable.callbackUrl],
                    environment  = it[MpesaConfigsTable.environment],
                    accountType  = it[MpesaConfigsTable.accountType],
                    passkeyConfigured = it[MpesaConfigsTable.passKey].isNotBlank(),
                    updatedAt    = it[MpesaConfigsTable.updatedAt].toString()
                )
            }
    }

    fun saveMpesaConfig(businessId: String, req: MpesaConfigRequest): ApiResponse<MpesaConfigResponse> = transaction {
        if (req.shortCode.isBlank() || req.callbackUrl.isBlank()) {
            return@transaction ApiResponse(false, message = "Shortcode and callback URL are required")
        }
        val env = req.environment.lowercase()
        if (env !in listOf("sandbox", "production")) {
            return@transaction ApiResponse(false, message = "Environment must be 'sandbox' or 'production'")
        }
        val acctType = req.accountType.lowercase()
        if (acctType !in listOf("paybill", "till")) {
            return@transaction ApiResponse(false, message = "accountType must be 'paybill' or 'till'")
        }

        val now = Clock.System.now()
        val exists = MpesaConfigsTable.select { MpesaConfigsTable.businessId eq businessId }.count() > 0

        if (exists) {
            MpesaConfigsTable.update({ MpesaConfigsTable.businessId eq businessId }) {
                it[shortCode]      = req.shortCode
                it[callbackUrl]    = req.callbackUrl
                it[environment]    = env
                it[accountType]    = acctType
                if (!req.passKey.isNullOrBlank()) it[passKey] = req.passKey
                it[updatedAt]      = now
            }
        } else {
            MpesaConfigsTable.insert {
                it[id]             = generateId()
                it[MpesaConfigsTable.businessId] = businessId
                it[shortCode]      = req.shortCode
                it[callbackUrl]    = req.callbackUrl
                it[environment]    = env
                it[accountType]    = acctType
                if (!req.passKey.isNullOrBlank()) it[passKey] = req.passKey
                it[createdAt]      = now
                it[updatedAt]      = now
            }
        }

        val passkeyConfigured = MpesaConfigsTable
            .select { MpesaConfigsTable.businessId eq businessId }
            .first()[MpesaConfigsTable.passKey].isNotBlank()
        val resp = MpesaConfigResponse(
            businessId  = businessId,
            shortCode   = req.shortCode,
            callbackUrl = req.callbackUrl,
            environment = env,
            accountType = acctType,
            passkeyConfigured = passkeyConfigured,
            updatedAt   = now.toString()
        )
        ApiResponse(success = true, data = resp, message = "Mpesa configuration saved")
    }

    // ── CyberSource Config ────────────────────────────────────────────────────

    fun getCyberSourceConfig(businessId: String): CyberSourceConfigResponse? = transaction {
        CyberSourceConfigsTable
            .select { CyberSourceConfigsTable.businessId eq businessId }
            .firstOrNull()
            ?.let {
                CyberSourceConfigResponse(
                    businessId   = businessId,
                    merchantId   = it[CyberSourceConfigsTable.merchantId],
                    merchantKeyId = it[CyberSourceConfigsTable.merchantKeyId],
                    environment  = it[CyberSourceConfigsTable.environment],
                    updatedAt    = it[CyberSourceConfigsTable.updatedAt].toString()
                )
            }
    }

    fun saveCyberSourceConfig(businessId: String, req: CyberSourceConfigRequest): ApiResponse<CyberSourceConfigResponse> = transaction {
        if (req.merchantId.isBlank() || req.merchantKeyId.isBlank() || req.merchantSecretKey.isBlank()) {
            return@transaction ApiResponse(false, message = "merchantId, merchantKeyId, and merchantSecretKey are required")
        }
        val env = req.environment.lowercase()
        if (env !in listOf("sandbox", "production")) {
            return@transaction ApiResponse(false, message = "Environment must be 'sandbox' or 'production'")
        }

        val now = Clock.System.now()
        val exists = CyberSourceConfigsTable.select { CyberSourceConfigsTable.businessId eq businessId }.count() > 0

        if (exists) {
            CyberSourceConfigsTable.update({ CyberSourceConfigsTable.businessId eq businessId }) {
                it[merchantId]        = req.merchantId
                it[merchantKeyId]     = req.merchantKeyId
                it[merchantSecretKey] = req.merchantSecretKey
                it[environment]       = env
                it[updatedAt]         = now
            }
        } else {
            CyberSourceConfigsTable.insert {
                it[id]                                 = generateId()
                it[CyberSourceConfigsTable.businessId] = businessId
                it[merchantId]                         = req.merchantId
                it[merchantKeyId]                      = req.merchantKeyId
                it[merchantSecretKey]                  = req.merchantSecretKey
                it[environment]                        = env
                it[createdAt]                          = now
                it[updatedAt]                          = now
            }
        }

        val resp = CyberSourceConfigResponse(
            businessId    = businessId,
            merchantId    = req.merchantId,
            merchantKeyId = req.merchantKeyId,
            environment   = env,
            updatedAt     = now.toString()
        )
        ApiResponse(success = true, data = resp, message = "CyberSource configuration saved")
    }

    // ── DB helpers for runtime lookup ─────────────────────────────────────────

    fun loadMpesaConfigForBusiness(businessId: String): MpesaRuntimeConfig? = transaction {
        MpesaConfigsTable
            .select { MpesaConfigsTable.businessId eq businessId }
            .firstOrNull()
            ?.let {
                MpesaRuntimeConfig(
                    consumerKey    = it[MpesaConfigsTable.consumerKey],
                    consumerSecret = it[MpesaConfigsTable.consumerSecret],
                    shortCode      = it[MpesaConfigsTable.shortCode],
                    passKey        = it[MpesaConfigsTable.passKey],
                    callbackUrl    = it[MpesaConfigsTable.callbackUrl],
                    isSandbox      = it[MpesaConfigsTable.environment] == "sandbox",
                    accountType    = it[MpesaConfigsTable.accountType]
                )
            }
    }

    fun loadCyberSourceConfigForBusiness(businessId: String): CyberSourceConfig? = transaction {
        CyberSourceConfigsTable
            .select { CyberSourceConfigsTable.businessId eq businessId }
            .firstOrNull()
            ?.let {
                CyberSourceConfig(
                    merchantId        = it[CyberSourceConfigsTable.merchantId],
                    merchantKeyId     = it[CyberSourceConfigsTable.merchantKeyId],
                    merchantSecretKey = it[CyberSourceConfigsTable.merchantSecretKey],
                    environment       = it[CyberSourceConfigsTable.environment]
                )
            }
    }
}

// ─── Runtime config holder (not serialized) ──────────────────────────────────

data class MpesaRuntimeConfig(
    val consumerKey: String,
    val consumerSecret: String,
    val shortCode: String,
    val passKey: String,
    val callbackUrl: String,
    val isSandbox: Boolean,
    val accountType: String = "paybill",   // paybill | till
    val initiatorName: String = "",
    val initiatorPassword: String = "",
    val certificateBase64: String = "",
    val resultUrl: String = "",
    val timeoutUrl: String = ""
)
