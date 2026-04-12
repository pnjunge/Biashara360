package com.app.biashara.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// ─── CyberSource Config ───────────────────────────────────────────────────────

data class CyberSourceConfig(
    val merchantId: String,
    val merchantKeyId: String,        // Key ID from Business Center
    val merchantSecretKey: String,    // Shared secret key (for HTTP sig / JWT)
    val environment: String,          // "sandbox" or "production"
    val p12KeyAlias: String? = null,  // For JWT/P12 auth
    val p12KeyPassword: String? = null,
    val p12KeyPath: String? = null
) {
    val baseUrl: String get() = when (environment) {
        "production" -> "https://api.cybersource.com"
        else         -> "https://apitest.cybersource.com"
    }
}

// ─── Request / Response DTOs ──────────────────────────────────────────────────

@Serializable
data class CsPaymentRequest(
    val clientReferenceInformation: CsClientRef,
    val processingInformation: CsProcessingInfo,
    val paymentInformation: CsPaymentInfo,
    val orderInformation: CsOrderInfo,
    val deviceInformation: CsDeviceInfo? = null,
    val merchantDefinedInformation: List<CsMerchantField>? = null
)

@Serializable data class CsClientRef(val code: String, val comments: String? = null)

@Serializable data class CsProcessingInfo(
    val capture: Boolean = false,       // false = auth-only, true = auth+capture
    val commerceIndicator: String = "internet",
    val actionList: List<String>? = null // e.g. ["VALIDATE_CONSUMER_AUTHENTICATION"]
)

@Serializable data class CsPaymentInfo(
    val card: CsCard? = null,
    val customer: CsCustomerToken? = null,    // for tokenized payments
    val flushToken: CsFlexToken? = null       // for Unified Checkout / Flex token
)

@Serializable data class CsCard(
    val number: String,
    val expirationMonth: String,
    val expirationYear: String,
    val securityCode: String? = null,
    val type: String? = null  // 001=Visa, 002=Mastercard, 003=Amex
)

@Serializable data class CsCustomerToken(val customerId: String)
@Serializable data class CsFlexToken(val transientTokenJwt: String)

@Serializable data class CsOrderInfo(
    val amountDetails: CsAmountDetails,
    val billTo: CsBillTo? = null,
    val shipTo: CsShipTo? = null,
    val lineItems: List<CsLineItem>? = null
)

@Serializable data class CsAmountDetails(
    val totalAmount: String,     // e.g. "150.00"
    val currency: String = "KES" // ISO 4217
)

@Serializable data class CsBillTo(
    val firstName: String,
    val lastName: String,
    val address1: String,
    val locality: String,       // city
    val administrativeArea: String,
    val postalCode: String,
    val country: String,        // ISO 3166 e.g. "KE"
    val email: String,
    val phoneNumber: String
)

@Serializable data class CsShipTo(
    val firstName: String,
    val lastName: String,
    val address1: String,
    val locality: String,
    val country: String
)

@Serializable data class CsLineItem(
    val productName: String,
    val quantity: String,
    val unitPrice: String
)

@Serializable data class CsDeviceInfo(val ipAddress: String? = null)
@Serializable data class CsMerchantField(val key: String, val value: String)

// ── Capture Request ───────────────────────────────────────────────────────────
@Serializable data class CsCaptureRequest(
    val clientReferenceInformation: CsClientRef,
    val orderInformation: CsOrderInfo
)

// ── Refund Request ────────────────────────────────────────────────────────────
@Serializable data class CsRefundRequest(
    val clientReferenceInformation: CsClientRef,
    val orderInformation: CsOrderInfo
)

// ── Void Request ──────────────────────────────────────────────────────────────
@Serializable data class CsVoidRequest(
    val clientReferenceInformation: CsClientRef
)

// ── Customer Token (TMS) ──────────────────────────────────────────────────────
@Serializable data class CsTokenRequest(
    val clientReferenceInformation: CsClientRef,
    val paymentInformation: CsPaymentInfo,
    val billTo: CsBillTo? = null
)

// ── Response ──────────────────────────────────────────────────────────────────
@Serializable data class CsPaymentResponse(
    val id: String? = null,
    val status: String? = null,           // AUTHORIZED, DECLINED, INVALID_REQUEST, etc.
    val errorInformation: CsErrorInfo? = null,
    val processorInformation: CsProcessorInfo? = null,
    val reconciliationId: String? = null,
    val submitTimeUtc: String? = null
)

@Serializable data class CsErrorInfo(
    val reason: String? = null,
    val message: String? = null,
    val details: List<CsErrorDetail>? = null
)

@Serializable data class CsErrorDetail(val field: String, val reason: String)
@Serializable data class CsProcessorInfo(
    val approvalCode: String? = null,
    val responseCode: String? = null,
    val avs: CsAvs? = null
)
@Serializable data class CsAvs(val code: String? = null, val codeRaw: String? = null)

// ─── CyberSource Service ──────────────────────────────────────────────────────

class CyberSourceService(
    private val config: CyberSourceConfig,
    val httpClient: HttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    // ── HTTP Signature Auth Header ─────────────────────────────────────────────
    // Note: HTTP Signature is deprecating Sept 2026; migrate to JWT/P12 when ready.
    // This implementation uses HMAC-SHA256 HTTP Signature (current recommended approach).

    private fun buildSignatureHeaders(
        method: String,
        path: String,
        body: String
    ): Map<String, String> {
        val date = Date().let {
            val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("GMT")
            sdf.format(it)
        }

        val digest = run {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val hash = md.digest(body.toByteArray(Charsets.UTF_8))
            "SHA-256=" + Base64.getEncoder().encodeToString(hash)
        }

        val headers = mutableMapOf(
            "host"             to config.baseUrl.removePrefix("https://"),
            "date"             to date,
            "request-target"  to "${method.lowercase()} $path",
            "digest"           to digest,
            "v-c-merchant-id"  to config.merchantId
        )

        val signatureString = headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
        val headerNames = headers.keys.joinToString(" ")

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(Base64.getDecoder().decode(config.merchantSecretKey), "HmacSHA256"))
        val sig = Base64.getEncoder().encodeToString(mac.doFinal(signatureString.toByteArray(Charsets.UTF_8)))

        val signatureHeader = listOf(
            """keyid="${config.merchantKeyId}"""",
            """algorithm="HmacSHA256"""",
            """headers="$headerNames"""",
            """signature="$sig""""
        ).joinToString(", ")

        return mapOf(
            "v-c-merchant-id" to config.merchantId,
            "Date"            to date,
            "Host"            to headers["host"]!!,
            "Digest"          to digest,
            "Signature"       to signatureHeader
        )
    }

    // ── Core API Call ──────────────────────────────────────────────────────────
    private suspend inline fun <reified T, reified R> csPost(
        path: String,
        payload: T
    ): CsResult<R> = try {
        val body = json.encodeToString(payload)
        val authHeaders = buildSignatureHeaders("POST", path, body)

        val response: HttpResponse = httpClient.post("${config.baseUrl}$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
            authHeaders.forEach { (k, v) -> headers.append(k, v) }
        }

        val responseBody = response.bodyAsText()

        if (response.status.isSuccess()) {
            val parsed = json.decodeFromString<R>(responseBody)
            CsResult.Success(parsed)
        } else {
            val err = runCatching { json.decodeFromString<CsPaymentResponse>(responseBody) }.getOrNull()
            CsResult.Failure(
                httpStatus = response.status.value,
                reason = err?.errorInformation?.reason ?: "UNKNOWN",
                message = err?.errorInformation?.message ?: responseBody
            )
        }
    } catch (e: Exception) {
        CsResult.Failure(0, "NETWORK_ERROR", e.message ?: "Unknown error")
    }

    private suspend inline fun <reified T> csDelete(
        path: String,
        payload: T
    ): CsResult<CsPaymentResponse> = try {
        val body = json.encodeToString(payload)
        val authHeaders = buildSignatureHeaders("DELETE", path, body)
        val response: HttpResponse = httpClient.delete("${config.baseUrl}$path") {
            contentType(ContentType.Application.Json)
            setBody(body)
            authHeaders.forEach { (k, v) -> headers.append(k, v) }
        }
        if (response.status.isSuccess()) {
            CsResult.Success(json.decodeFromString(response.bodyAsText()))
        } else {
            CsResult.Failure(response.status.value, "VOID_FAILED", response.bodyAsText())
        }
    } catch (e: Exception) {
        CsResult.Failure(0, "NETWORK_ERROR", e.message ?: "")
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Authorize a card payment (does NOT capture/settle immediately).
     * Use capturePayment() after delivering goods.
     */
    suspend fun authorizePayment(request: CsPaymentRequest): CsResult<CsPaymentResponse> =
        csPost("/pts/v2/payments", request)

    /**
     * Authorize AND capture in one step (immediate settlement).
     * For Biashara360: use this for COD confirmation or immediate retail sales.
     */
    suspend fun authorizeAndCapture(request: CsPaymentRequest): CsResult<CsPaymentResponse> =
        csPost("/pts/v2/payments", request.copy(
            processingInformation = request.processingInformation.copy(capture = true)
        ))

    /**
     * Capture a previously authorized payment.
     * Call this when order ships / is fulfilled.
     */
    suspend fun capturePayment(
        authId: String,   // The `id` from authorizePayment response
        request: CsCaptureRequest
    ): CsResult<CsPaymentResponse> =
        csPost("/pts/v2/payments/$authId/captures", request)

    /**
     * Refund a captured/settled payment.
     */
    suspend fun refundPayment(
        captureId: String,
        request: CsRefundRequest
    ): CsResult<CsPaymentResponse> =
        csPost("/pts/v2/payments/$captureId/refunds", request)

    /**
     * Void an authorization (before capture/settlement).
     */
    suspend fun voidAuthorization(
        authId: String,
        request: CsVoidRequest
    ): CsResult<CsPaymentResponse> =
        csDelete("/pts/v2/payments/$authId/voids", request)

    /**
     * Create a customer token (TMS) — store card without PCI liability.
     * Returns a customerId to use in future charges.
     */
    suspend fun createCustomerToken(request: CsTokenRequest): CsResult<CsPaymentResponse> =
        csPost("/tms/v2/customers", request)

    /**
     * Generate a Flex capture context (JWT) for Unified Checkout widget on web.
     * Frontend uses this JWT to initialize the CyberSource payment form.
     */
    suspend fun generateCaptureContext(targetOrigin: String): CsResult<String> = try {
        val body = """{"targetOrigins":["$targetOrigin"],"allowedCardNetworks":["VISA","MASTERCARD","AMEX"],"clientVersion":"v2.0"}"""
        val authHeaders = buildSignatureHeaders("POST", "/microform/v2/sessions", body)
        val response: HttpResponse = httpClient.post("${config.baseUrl}/microform/v2/sessions") {
            contentType(ContentType.Application.Json)
            setBody(body)
            authHeaders.forEach { (k, v) -> headers.append(k, v) }
        }
        if (response.status.isSuccess()) {
            CsResult.Success(response.bodyAsText()) // Returns the capture context JWT
        } else {
            CsResult.Failure(response.status.value, "CONTEXT_FAILED", response.bodyAsText())
        }
    } catch (e: Exception) {
        CsResult.Failure(0, "NETWORK_ERROR", e.message ?: "")
    }

    // ── Convenience builders ───────────────────────────────────────────────────

    companion object {
        /**
         * Build a standard B360 payment request from order data.
         */
        fun buildPaymentRequest(
            orderId: String,
            amountKes: Double,
            card: CsCard? = null,
            flexToken: String? = null,
            customerId: String? = null,
            billingName: String = "",
            email: String = "",
            phone: String = "",
            lineItems: List<Triple<String, Int, Double>>? = null,  // name, qty, unitPrice
            capture: Boolean = true
        ): CsPaymentRequest {
            val nameParts = billingName.trim().split(" ")
            val firstName = nameParts.firstOrNull() ?: "Customer"
            val lastName = nameParts.drop(1).joinToString(" ").ifEmpty { "." }

            val paymentInfo = when {
                customerId != null -> CsPaymentInfo(customer = CsCustomerToken(customerId))
                flexToken != null  -> CsPaymentInfo(flushToken = CsFlexToken(flexToken))
                card != null       -> CsPaymentInfo(card = card)
                else               -> throw IllegalArgumentException("Must provide card, flexToken, or customerId")
            }

            return CsPaymentRequest(
                clientReferenceInformation = CsClientRef(
                    code = orderId,
                    comments = "B360 Order $orderId"
                ),
                processingInformation = CsProcessingInfo(capture = capture),
                paymentInformation = paymentInfo,
                orderInformation = CsOrderInfo(
                    amountDetails = CsAmountDetails(
                        totalAmount = String.format("%.2f", amountKes),
                        currency = "KES"
                    ),
                    billTo = CsBillTo(
                        firstName = firstName,
                        lastName = lastName,
                        address1 = "Nairobi",
                        locality = "Nairobi",
                        administrativeArea = "NBI",
                        postalCode = "00100",
                        country = "KE",
                        email = email.ifEmpty { "customer@biashara360.co.ke" },
                        phoneNumber = phone
                    )
                ),
                merchantDefinedInformation = listOf(
                    CsMerchantField("platform", "Biashara360"),
                    CsMerchantField("orderId", orderId)
                )
            )
        }

        // Sandbox test card numbers
        val SANDBOX_VISA_SUCCESS      = "4111111111111111"
        val SANDBOX_VISA_DECLINE      = "4242424242424242"
        val SANDBOX_MASTERCARD        = "5555555555554444"
        val SANDBOX_AMEX              = "378282246310005"
        val SANDBOX_EXPIRY_MONTH      = "12"
        val SANDBOX_EXPIRY_YEAR       = "2031"
        val SANDBOX_CVV               = "123"
    }
}

// ─── Result Wrapper ───────────────────────────────────────────────────────────
sealed class CsResult<out T> {
    data class Success<T>(val data: T) : CsResult<T>()
    data class Failure(val httpStatus: Int, val reason: String, val message: String) : CsResult<Nothing>()
}

// ── Missing request/response types used by CyberSourcePaymentService ──────────
@kotlinx.serialization.Serializable
data class CsChargeRequest(
    val orderId: String,
    val amount: Double,
    val currency: String = "KES",
    val transientToken: String? = null,
    val savedCardId: String? = null,
    val cardNumber: String? = null,
    val cardExpiryMonth: String? = null,
    val cardExpiryYear: String? = null,
    val cardCvv: String? = null,
    val cardholderName: String? = null,
    val saveCard: Boolean = false,
    val customerId: String? = null,
    val capture: Boolean = true,
    val captureImmediately: Boolean = true,
    val billingEmail: String? = null,
    val billingPhone: String? = null
)

@kotlinx.serialization.Serializable
data class CsChargeResponse(
    val transactionId: String = "",
    val csTransactionId: String? = null,
    val status: String = "DECLINED",
    val approvalCode: String? = null,
    val amount: Double = 0.0,
    val currency: String = "KES",
    val cardLast4: String? = null,
    val cardType: String? = null,
    val reconciliationId: String? = null,
    val savedCardId: String? = null,
    val errorMessage: String? = null,
    val errorReason: String? = null
)

@kotlinx.serialization.Serializable
data class CsSavedCard(
    val id: String,
    val csCustomerId: String,
    val cardLast4: String,
    val cardType: String,
    val expiryMonth: String,
    val expiryYear: String,
    val holderName: String,
    val isDefault: Boolean
)

@kotlinx.serialization.Serializable
data class CsCaptureContextResponse(val captureContextJwt: String)

// ── Route-level request wrappers (distinct from CyberSource API request types) ──
@kotlinx.serialization.Serializable
data class CsCaptureRouteRequest(
    val csTransactionId: String,
    val amount: Double? = null
)

@kotlinx.serialization.Serializable
data class CsRefundRouteRequest(
    val csTransactionId: String,
    val amount: Double,
    val reason: String? = null
)

@kotlinx.serialization.Serializable
data class CsVoidRouteRequest(
    val csTransactionId: String
)
