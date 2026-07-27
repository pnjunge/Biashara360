package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.CyberSourceConfigsTable
import com.app.biashara.db.BusinessSessionSettingsTable
import com.app.biashara.db.MpesaConfigsTable
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class BusinessSettingsService {

    companion object {
        const val DEFAULT_SESSION_TIMEOUT_SECONDS = 1800L
        const val MIN_SESSION_TIMEOUT_SECONDS = 60L
        const val MAX_SESSION_TIMEOUT_SECONDS = 86_400L
    }

    fun getSessionTimeoutConfig(businessId: String): SessionTimeoutConfigResponse = transaction {
        BusinessSessionSettingsTable
            .select { BusinessSessionSettingsTable.businessId eq businessId }
            .firstOrNull()
            ?.let {
                SessionTimeoutConfigResponse(
                    businessId = businessId,
                    webTimeoutSeconds = it[BusinessSessionSettingsTable.webTimeoutSeconds],
                    androidTimeoutSeconds = it[BusinessSessionSettingsTable.androidTimeoutSeconds],
                    desktopTimeoutSeconds = it[BusinessSessionSettingsTable.desktopTimeoutSeconds],
                    updatedAt = it[BusinessSessionSettingsTable.updatedAt].toString()
                )
            }
            ?: SessionTimeoutConfigResponse(
                businessId = businessId,
                webTimeoutSeconds = DEFAULT_SESSION_TIMEOUT_SECONDS,
                androidTimeoutSeconds = DEFAULT_SESSION_TIMEOUT_SECONDS,
                desktopTimeoutSeconds = DEFAULT_SESSION_TIMEOUT_SECONDS
            )
    }

    fun saveSessionTimeoutConfig(businessId: String, req: SessionTimeoutConfigRequest): ApiResponse<SessionTimeoutConfigResponse> = transaction {
        val values = listOf(req.webTimeoutSeconds, req.androidTimeoutSeconds, req.desktopTimeoutSeconds)
        if (values.any { it !in MIN_SESSION_TIMEOUT_SECONDS..MAX_SESSION_TIMEOUT_SECONDS }) {
            return@transaction ApiResponse(false, message = "Session timeouts must be between 60 and 86400 seconds")
        }
        val now = Clock.System.now()
        val exists = BusinessSessionSettingsTable
            .select { BusinessSessionSettingsTable.businessId eq businessId }
            .count() > 0
        if (exists) {
            BusinessSessionSettingsTable.update({ BusinessSessionSettingsTable.businessId eq businessId }) {
                it[webTimeoutSeconds] = req.webTimeoutSeconds
                it[androidTimeoutSeconds] = req.androidTimeoutSeconds
                it[desktopTimeoutSeconds] = req.desktopTimeoutSeconds
                it[updatedAt] = now
            }
        } else {
            BusinessSessionSettingsTable.insert {
                it[BusinessSessionSettingsTable.businessId] = businessId
                it[webTimeoutSeconds] = req.webTimeoutSeconds
                it[androidTimeoutSeconds] = req.androidTimeoutSeconds
                it[desktopTimeoutSeconds] = req.desktopTimeoutSeconds
                it[updatedAt] = now
            }
        }
        ApiResponse(true, data = SessionTimeoutConfigResponse(
            businessId = businessId,
            webTimeoutSeconds = req.webTimeoutSeconds,
            androidTimeoutSeconds = req.androidTimeoutSeconds,
            desktopTimeoutSeconds = req.desktopTimeoutSeconds,
            updatedAt = now.toString()
        ), message = "Session timeout policy saved")
    }

    // ── Mpesa Config ──────────────────────────────────────────────────────────

    fun getMpesaConfig(businessId: String, accountType: String? = null): MpesaConfigResponse? = transaction {
        MpesaConfigsTable
            .select {
                if (accountType == null) {
                    MpesaConfigsTable.businessId eq businessId
                } else {
                    (MpesaConfigsTable.businessId eq businessId) and
                        (MpesaConfigsTable.accountType eq accountType.lowercase())
                }
            }
            .orderBy(MpesaConfigsTable.accountType to SortOrder.ASC)
            .firstOrNull()
            ?.toMpesaConfigResponse(businessId)
    }

    fun getMpesaConfigs(businessId: String): List<MpesaConfigResponse> = transaction {
        MpesaConfigsTable
            .select { MpesaConfigsTable.businessId eq businessId }
            .orderBy(MpesaConfigsTable.accountType to SortOrder.ASC)
            .map { it.toMpesaConfigResponse(businessId) }
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
        val channelFilter = (MpesaConfigsTable.businessId eq businessId) and
            (MpesaConfigsTable.accountType eq acctType)
        val exists = MpesaConfigsTable.select { channelFilter }.count() > 0

        if (exists) {
            MpesaConfigsTable.update({ channelFilter }) {
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
            .select { channelFilter }
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
                    businessId       = businessId,
                    merchantId       = it[CyberSourceConfigsTable.merchantId],
                    merchantKeyId    = it[CyberSourceConfigsTable.merchantKeyId],
                    profileId        = it[CyberSourceConfigsTable.profileId],
                    accessKey        = it[CyberSourceConfigsTable.accessKey],
                    environment      = it[CyberSourceConfigsTable.environment],
                    secretConfigured = it[CyberSourceConfigsTable.merchantSecretKey].isNotBlank(),
                    updatedAt        = it[CyberSourceConfigsTable.updatedAt].toString()
                )
            }
    }

    fun saveCyberSourceConfig(businessId: String, req: CyberSourceConfigRequest): ApiResponse<CyberSourceConfigResponse> = transaction {
        if (req.merchantId.isBlank() || req.merchantKeyId.isBlank()) {
            return@transaction ApiResponse(false, message = "merchantId and merchantKeyId are required")
        }
        val env = req.environment.lowercase()
        if (env !in listOf("sandbox", "production")) {
            return@transaction ApiResponse(false, message = "Environment must be 'sandbox' or 'production'")
        }

        val now = Clock.System.now()
        val exists = CyberSourceConfigsTable.select { CyberSourceConfigsTable.businessId eq businessId }.count() > 0
        if (!exists && req.merchantSecretKey.isNullOrBlank()) {
            return@transaction ApiResponse(false, message = "merchantSecretKey is required for initial configuration")
        }

        val profId = req.profileId ?: ""
        val accKey = req.accessKey ?: ""

        if (exists) {
            CyberSourceConfigsTable.update({ CyberSourceConfigsTable.businessId eq businessId }) {
                it[merchantId]        = req.merchantId
                it[merchantKeyId]     = req.merchantKeyId
                if (!req.merchantSecretKey.isNullOrBlank()) it[merchantSecretKey] = req.merchantSecretKey
                it[profileId]         = profId
                it[accessKey]         = accKey
                it[environment]       = env
                it[updatedAt]         = now
            }
        } else {
            CyberSourceConfigsTable.insert {
                it[id]                                 = generateId()
                it[CyberSourceConfigsTable.businessId] = businessId
                it[merchantId]                         = req.merchantId
                it[merchantKeyId]                      = req.merchantKeyId
                it[merchantSecretKey]                  = req.merchantSecretKey!!
                it[profileId]                          = profId
                it[accessKey]                          = accKey
                it[environment]                        = env
                it[createdAt]                          = now
                it[updatedAt]                          = now
            }
        }

        val resp = CyberSourceConfigResponse(
            businessId    = businessId,
            merchantId    = req.merchantId,
            merchantKeyId = req.merchantKeyId,
            profileId     = profId,
            accessKey     = accKey,
            environment   = env,
            secretConfigured = exists || !req.merchantSecretKey.isNullOrBlank(),
            updatedAt     = now.toString()
        )
        ApiResponse(success = true, data = resp, message = "CyberSource configuration saved")
    }

    // ── DB helpers for runtime lookup ─────────────────────────────────────────

    fun loadMpesaConfigForBusiness(businessId: String, accountType: String? = null): MpesaRuntimeConfig? = transaction {
        MpesaConfigsTable
            .select {
                if (accountType == null) {
                    MpesaConfigsTable.businessId eq businessId
                } else {
                    (MpesaConfigsTable.businessId eq businessId) and
                        (MpesaConfigsTable.accountType eq accountType.lowercase())
                }
            }
            .orderBy(MpesaConfigsTable.accountType to SortOrder.ASC)
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

    private fun ResultRow.toMpesaConfigResponse(businessId: String) = MpesaConfigResponse(
        businessId = businessId,
        shortCode = this[MpesaConfigsTable.shortCode],
        callbackUrl = this[MpesaConfigsTable.callbackUrl],
        environment = this[MpesaConfigsTable.environment],
        accountType = this[MpesaConfigsTable.accountType],
        passkeyConfigured = this[MpesaConfigsTable.passKey].isNotBlank(),
        updatedAt = this[MpesaConfigsTable.updatedAt].toString()
    )

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
