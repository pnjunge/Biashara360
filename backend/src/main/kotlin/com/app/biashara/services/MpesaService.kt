package com.app.biashara.services
import io.ktor.server.config.ApplicationConfig

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.security.cert.CertificateFactory
import java.io.ByteArrayInputStream
import javax.crypto.Cipher

class MpesaService(
    private val httpClient: HttpClient,
    private val config: ApplicationConfig,
    private val settingsService: BusinessSettingsService? = null,
    private val systemSettingsService: SystemSettingsService? = null
) {
    // Fall-back to application-level config when no per-business DB config is found.
    // For the callback URL, the system settings DB value takes precedence over application.conf.
    private val defaultConsumerKey get() = config.propertyOrNull("mpesa.consumerKey")?.getString() ?: ""
    private val defaultConsumerSecret get() = config.propertyOrNull("mpesa.consumerSecret")?.getString() ?: ""
    private val defaultShortCode get() = config.propertyOrNull("mpesa.shortCode")?.getString() ?: ""
    private val defaultPassKey get() = config.propertyOrNull("mpesa.passKey")?.getString() ?: ""
    private val merchantPasskeysJson get() = config.propertyOrNull("mpesa.passkeysByBusiness")?.getString() ?: ""
    private val defaultCallbackUrl get() =
        systemSettingsService?.getMpesaCallbackUrl()
            ?: config.propertyOrNull("mpesa.callbackUrl")?.getString()
            ?: ""
    private val defaultIsSandbox get() = config.propertyOrNull("mpesa.environment")?.getString() != "production"
    private val defaultAccountType get() = config.propertyOrNull("mpesa.accountType")?.getString() ?: "paybill"
    private val defaultInitiatorName get() = config.propertyOrNull("mpesa.initiatorName")?.getString() ?: ""
    private val defaultInitiatorPassword get() = config.propertyOrNull("mpesa.initiatorPassword")?.getString() ?: ""
    private val defaultCertificateBase64 get() = config.propertyOrNull("mpesa.certificateBase64")?.getString() ?: ""
    private val defaultResultUrl get() = config.propertyOrNull("mpesa.resultUrl")?.getString() ?: ""
    private val defaultTimeoutUrl get() = config.propertyOrNull("mpesa.timeoutUrl")?.getString() ?: ""

    private fun resolveConfig(businessId: String?): MpesaRuntimeConfig {
        if (businessId != null && settingsService != null) {
            val dbConfig = settingsService.loadMpesaConfigForBusiness(businessId)
            if (dbConfig != null) return dbConfig.copy(
                consumerKey = defaultConsumerKey,
                consumerSecret = defaultConsumerSecret,
                passKey = passKeyForBusiness(businessId),
                initiatorName = defaultInitiatorName,
                initiatorPassword = defaultInitiatorPassword,
                certificateBase64 = defaultCertificateBase64,
                resultUrl = defaultResultUrl,
                timeoutUrl = defaultTimeoutUrl
            )
        }
        return MpesaRuntimeConfig(
            consumerKey    = defaultConsumerKey,
            consumerSecret = defaultConsumerSecret,
            shortCode      = defaultShortCode,
            passKey        = passKeyForBusiness(businessId),
            callbackUrl    = defaultCallbackUrl,
            isSandbox      = defaultIsSandbox,
            accountType    = defaultAccountType,
            initiatorName = defaultInitiatorName,
            initiatorPassword = defaultInitiatorPassword,
            certificateBase64 = defaultCertificateBase64,
            resultUrl = defaultResultUrl,
            timeoutUrl = defaultTimeoutUrl
        )
    }

    /** Resolves the passkey only inside the backend. The JSON map is injected
     * through a secret environment variable and keyed by business ID. */
    private fun passKeyForBusiness(businessId: String?): String {
        if (businessId.isNullOrBlank() || merchantPasskeysJson.isBlank()) return defaultPassKey
        return try {
            val element = lenientJson.parseToJsonElement(merchantPasskeysJson)
            (element as? JsonObject)?.get(businessId)?.jsonPrimitive?.contentOrNull ?: defaultPassKey
        } catch (_: Exception) { defaultPassKey }
    }

    private fun baseUrl(isSandbox: Boolean) = if (isSandbox)
        "https://sandbox.safaricom.co.ke"
    else
        "https://api.safaricom.co.ke"

    // ── Get OAuth Token ──────────────────────────────────────────────────────

    private suspend fun getAccessToken(cfg: MpesaRuntimeConfig): String {
        val credentials = Base64.getEncoder()
            .encodeToString("${cfg.consumerKey}:${cfg.consumerSecret}".toByteArray())

        val response: DarajaTokenResponse = httpClient.get("${baseUrl(cfg.isSandbox)}/oauth/v1/generate?grant_type=client_credentials") {
            headers { append(HttpHeaders.Authorization, "Basic $credentials") }
        }.body()
        return response.access_token
    }

    // ── STK Push ─────────────────────────────────────────────────────────────

    suspend fun initiateSTKPush(
        phoneNumber: String,
        amount: Double,
        accountReference: String,
        transactionDesc: String,
        businessId: String? = null
    ): StkPushResult {
        return try {
            val cfg = resolveConfig(businessId)
            val missing = validateConfig(cfg)
            if (missing.isNotEmpty()) {
                return StkPushResult.Error(
                    "M-Pesa is not configured. Update: ${missing.joinToString(", ")}"
                )
            }
            val token = getAccessToken(cfg)
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val password = Base64.getEncoder().encodeToString(
                "${cfg.shortCode}${cfg.passKey}$timestamp".toByteArray()
            )

            val transactionType = if (cfg.accountType == "paybill") "CustomerPayBillOnline" else "CustomerBuyGoodsOnline"
            val payload = StkPushPayload(
                BusinessShortCode = cfg.shortCode,
                Password = password,
                Timestamp = timestamp,
                TransactionType = transactionType,
                Amount = amount.toInt(),
                PartyA = phoneNumber,
                PartyB = cfg.shortCode,
                PhoneNumber = phoneNumber,
                CallBackURL = cfg.callbackUrl,
                AccountReference = accountReference,
                TransactionDesc = transactionDesc
            )

            val httpResponse = httpClient.post("${baseUrl(cfg.isSandbox)}/mpesa/stkpush/v1/processrequest") {
                headers { append(HttpHeaders.Authorization, "Bearer $token") }
                contentType(ContentType.Application.Json)
                println("[MpesaSTK] Sending STK request")
                setBody(payload)
            }

            val rawBody = httpResponse.bodyAsText()
            println("[MpesaSTK] Response status ${httpResponse.status.value}")

            if (!httpResponse.status.isSuccess()) {
                // Parse Daraja error shape: {"requestId":"...","errorCode":"...","errorMessage":"..."}
                val errMsg = try {
                    val obj = lenientJson.decodeFromString(JsonObject.serializer(), rawBody)
                    obj["errorMessage"]?.jsonPrimitive?.contentOrNull
                        ?: obj["ResultDesc"]?.jsonPrimitive?.contentOrNull
                        ?: "Daraja error (${httpResponse.status.value})"
                } catch (_: Exception) { "Daraja error (${httpResponse.status.value}): $rawBody" }
                return StkPushResult.Error(errMsg)
            }

            val response = try {
                lenientJson.decodeFromString(StkPushResponse.serializer(), rawBody)
            } catch (e: Exception) {
                println("[MpesaSTK] Failed to parse success body: ${e.message}")
                return StkPushResult.Error("Unexpected Daraja response format: $rawBody")
            }

            StkPushResult.Success(
                merchantRequestId = response.MerchantRequestID ?: "",
                checkoutRequestId = response.CheckoutRequestID ?: "",
                responseCode      = response.ResponseCode ?: "0",
                customerMessage   = response.CustomerMessage ?: "Payment request sent"
            )
        } catch (e: ClientRequestException) {
            val rawErr = try { e.response.bodyAsText() } catch (_: Exception) { "" }
            println("[MpesaSTK] ClientRequestException: ${e.response.status.value} — $rawErr")
            val friendly = try {
                val obj = lenientJson.decodeFromString(JsonObject.serializer(), rawErr)
                obj["errorMessage"]?.jsonPrimitive?.contentOrNull
                    ?: "M-Pesa request failed (${e.response.status.value})"
            } catch (_: Exception) { "M-Pesa request failed (${e.response.status.value})" }
            StkPushResult.Error(friendly)
        } catch (e: Exception) {
            println("[MpesaSTK] Exception: ${e.message}")
            StkPushResult.Error(e.message ?: "Failed to initiate payment")
        }
    }

    private fun validateConfig(cfg: MpesaRuntimeConfig): List<String> {
        val missing = mutableListOf<String>()
        if (cfg.consumerKey.isBlank() || cfg.consumerKey.contains("your_")) missing += "MPESA_CONSUMER_KEY"
        if (cfg.consumerSecret.isBlank() || cfg.consumerSecret.contains("your_")) missing += "MPESA_CONSUMER_SECRET"
        if (cfg.passKey.isBlank() || cfg.passKey.contains("your_")) missing += "MPESA_PASS_KEY"
        if (cfg.shortCode.isBlank() || cfg.shortCode.contains("your_")) missing += "MPESA_SHORT_CODE"
        if (cfg.callbackUrl.isBlank() || cfg.callbackUrl.contains("your-domain")) missing += "MPESA_CALLBACK_URL"
        return missing
    }

    suspend fun queryTransaction(transactionId: String, businessId: String? = null): TransactionQueryResult {
        return try {
            val cfg = resolveConfig(businessId)
            val missing = mutableListOf<String>()
            if (cfg.shortCode.isBlank()) missing += "MPESA_SHORT_CODE"
            if (cfg.initiatorName.isBlank()) missing += "MPESA_INITIATOR_NAME"
            if (cfg.initiatorPassword.isBlank()) missing += "MPESA_INITIATOR_PASSWORD"
            if (cfg.certificateBase64.isBlank()) missing += "MPESA_CERTIFICATE_BASE64"
            if (cfg.resultUrl.isBlank()) missing += "MPESA_RESULT_URL"
            if (cfg.timeoutUrl.isBlank()) missing += "MPESA_TIMEOUT_URL"
            if (missing.isNotEmpty()) return TransactionQueryResult(false, "M-Pesa transaction query is not configured. Update: ${missing.joinToString(", ")}")

            val token = getAccessToken(cfg)
            val certBytes = Base64.getDecoder().decode(cfg.certificateBase64)
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(certBytes))
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, cert.publicKey)
            val credential = Base64.getEncoder().encodeToString(cipher.doFinal(cfg.initiatorPassword.toByteArray(Charsets.UTF_8)))
            val payload = TransactionQueryPayload(cfg.initiatorName, credential, "TransactionStatusQuery", transactionId, cfg.shortCode, "4", cfg.resultUrl, cfg.timeoutUrl, "Biashara360 transaction status query", transactionId)
            val response = httpClient.post("${baseUrl(cfg.isSandbox)}/mpesa/transactionstatus/v1/query") {
                headers { append(HttpHeaders.Authorization, "Bearer $token") }
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            val body = response.bodyAsText()
            if (response.status.isSuccess()) TransactionQueryResult(true, "Transaction status query submitted", body)
            else TransactionQueryResult(false, "M-Pesa query failed (${response.status.value})")
        } catch (e: Exception) {
            TransactionQueryResult(false, e.message ?: "M-Pesa transaction query failed")
        }
    }

}

data class TransactionQueryResult(val success: Boolean, val message: String, val response: String? = null)

@Serializable
private data class TransactionQueryPayload(
    val Initiator: String,
    val SecurityCredential: String,
    val CommandID: String,
    val TransactionID: String,
    val PartyA: String,
    val IdentifierType: String,
    val ResultURL: String,
    val QueueTimeOutURL: String,
    val Remarks: String,
    val Occasion: String
)

// ── Daraja DTOs ───────────────────────────────────────────────────────────────

private val lenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
}

@Serializable
data class DarajaTokenResponse(val access_token: String, val expires_in: String)

@Serializable
data class StkPushPayload(
    val BusinessShortCode: String,
    val Password: String,
    val Timestamp: String,
    val TransactionType: String = "CustomerPayBillOnline",
    val Amount: Int,
    val PartyA: String,
    val PartyB: String,
    val PhoneNumber: String,
    val CallBackURL: String,
    val AccountReference: String,
    val TransactionDesc: String
)

// All fields nullable — Safaricom returns different shapes for success vs error
@Serializable
data class StkPushResponse(
    val MerchantRequestID: String?     = null,
    val CheckoutRequestID: String?     = null,
    val ResponseCode: String?          = null,
    val ResponseDescription: String?   = null,
    val CustomerMessage: String?       = null
)

sealed class StkPushResult {
    data class Success(
        val merchantRequestId: String,
        val checkoutRequestId: String,
        val responseCode: String,
        val customerMessage: String
    ) : StkPushResult()
    data class Error(val message: String) : StkPushResult()
}
