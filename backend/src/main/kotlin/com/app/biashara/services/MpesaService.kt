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
    private val config: ApplicationConfig
) {
    private val consumerKey get() = config.property("mpesa.consumerKey").getString()
    private val consumerSecret get() = config.property("mpesa.consumerSecret").getString()
    private val shortCode get() = config.property("mpesa.shortCode").getString()
    private val passKey get() = config.property("mpesa.passKey").getString()
    private val callbackUrl get() = config.property("mpesa.callbackUrl").getString()
    private val isSandbox get() = config.property("mpesa.environment").getString() == "sandbox"

    private val baseUrl get() = if (isSandbox)
        "https://sandbox.safaricom.co.ke"
    else
        "https://api.safaricom.co.ke"

    // ── Get OAuth Token ──────────────────────────────────────────────────────

    private suspend fun getAccessToken(): String {
        val credentials = Base64.getEncoder()
            .encodeToString("$consumerKey:$consumerSecret".toByteArray())

        val response: DarajaTokenResponse = httpClient.get("$baseUrl/oauth/v1/generate?grant_type=client_credentials") {
            headers { append(HttpHeaders.Authorization, "Basic $credentials") }
        }.body()
        return response.access_token
    }

    // ── STK Push ─────────────────────────────────────────────────────────────

    suspend fun initiateSTKPush(
        phoneNumber: String,
        amount: Double,
        accountReference: String,
        transactionDesc: String
    ): StkPushResult {
        return try {
            val token = getAccessToken()
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val password = Base64.getEncoder().encodeToString(
                "$shortCode$passKey$timestamp".toByteArray()
            )

            val payload = StkPushPayload(
                BusinessShortCode = shortCode,
                Password = password,
                Timestamp = timestamp,
                Amount = amount.toInt(),
                PartyA = phoneNumber,
                PartyB = shortCode,
                PhoneNumber = phoneNumber,
                CallBackURL = callbackUrl,
                AccountReference = accountReference,
                TransactionDesc = transactionDesc
            )

            val response: StkPushResponse = httpClient.post("$baseUrl/mpesa/stkpush/v1/processrequest") {
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
