package com.app.biashara.services
import io.ktor.server.config.ApplicationConfig

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

class MpesaService(
    private val httpClient: HttpClient,
    private val config: ApplicationConfig,
    private val settingsService: BusinessSettingsService? = null
) {
    // Fall-back to application-level config when no per-business DB config is found
    private val defaultConsumerKey get() = config.propertyOrNull("mpesa.consumerKey")?.getString() ?: ""
    private val defaultConsumerSecret get() = config.propertyOrNull("mpesa.consumerSecret")?.getString() ?: ""
    private val defaultShortCode get() = config.propertyOrNull("mpesa.shortCode")?.getString() ?: ""
    private val defaultPassKey get() = config.propertyOrNull("mpesa.passKey")?.getString() ?: ""
    private val defaultCallbackUrl get() = config.propertyOrNull("mpesa.callbackUrl")?.getString() ?: ""
    private val defaultIsSandbox get() = config.propertyOrNull("mpesa.environment")?.getString() != "production"

    private fun resolveConfig(businessId: String?): MpesaRuntimeConfig {
        if (businessId != null && settingsService != null) {
            val dbConfig = settingsService.loadMpesaConfigForBusiness(businessId)
            if (dbConfig != null) return dbConfig
        }
        return MpesaRuntimeConfig(
            consumerKey    = defaultConsumerKey,
            consumerSecret = defaultConsumerSecret,
            shortCode      = defaultShortCode,
            passKey        = defaultPassKey,
            callbackUrl    = defaultCallbackUrl,
            isSandbox      = defaultIsSandbox
        )
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
            val token = getAccessToken(cfg)
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val password = Base64.getEncoder().encodeToString(
                "${cfg.shortCode}${cfg.passKey}$timestamp".toByteArray()
            )

            val payload = StkPushPayload(
                BusinessShortCode = cfg.shortCode,
                Password = password,
                Timestamp = timestamp,
                Amount = amount.toInt(),
                PartyA = phoneNumber,
                PartyB = cfg.shortCode,
                PhoneNumber = phoneNumber,
                CallBackURL = cfg.callbackUrl,
                AccountReference = accountReference,
                TransactionDesc = transactionDesc
            )

            val response: StkPushResponse = httpClient.post("${baseUrl(cfg.isSandbox)}/mpesa/stkpush/v1/processrequest") {
                headers { append(HttpHeaders.Authorization, "Bearer $token") }
                contentType(ContentType.Application.Json)
                setBody(payload)
            }.body()

            StkPushResult.Success(
                merchantRequestId = response.MerchantRequestID,
                checkoutRequestId = response.CheckoutRequestID,
                responseCode = response.ResponseCode,
                customerMessage = response.CustomerMessage
            )
        } catch (e: Exception) {
            StkPushResult.Error(e.message ?: "Failed to initiate payment")
        }
    }

    // ── Process Callback ──────────────────────────────────────────────────────

    fun processCallback(callback: com.app.biashara.models.MpesaCallbackRequest): MpesaCallbackResult {
        val stkCallback = callback.Body.stkCallback
        return if (stkCallback.ResultCode == 0) {
            val metadata = stkCallback.CallbackMetadata?.Item ?: emptyList()
            val amount = metadata.find { it.Name == "Amount" }?.Value?.toDoubleOrNull() ?: 0.0
            val txCode = metadata.find { it.Name == "MpesaReceiptNumber" }?.Value ?: ""
            val phone = metadata.find { it.Name == "PhoneNumber" }?.Value ?: ""
            val name = metadata.find { it.Name == "TransactionDate" }?.Value ?: ""

            MpesaCallbackResult.Success(
                transactionCode = txCode,
                amount = amount,
                phoneNumber = phone,
                checkoutRequestId = stkCallback.CheckoutRequestID
            )
        } else {
            MpesaCallbackResult.Failed(
                resultCode = stkCallback.ResultCode,
                resultDesc = stkCallback.ResultDesc,
                checkoutRequestId = stkCallback.CheckoutRequestID
            )
        }
    }
}

// ── Daraja DTOs ───────────────────────────────────────────────────────────────

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

@Serializable
data class StkPushResponse(
    val MerchantRequestID: String,
    val CheckoutRequestID: String,
    val ResponseCode: String,
    val ResponseDescription: String,
    val CustomerMessage: String
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

sealed class MpesaCallbackResult {
    data class Success(
        val transactionCode: String,
        val amount: Double,
        val phoneNumber: String,
        val checkoutRequestId: String
    ) : MpesaCallbackResult()
    data class Failed(
        val resultCode: Int,
        val resultDesc: String,
        val checkoutRequestId: String
    ) : MpesaCallbackResult()
}
