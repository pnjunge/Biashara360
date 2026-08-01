package com.app.biashara.services

import com.app.biashara.db.CyberSourceConfigsTable
import com.app.biashara.db.CyberSourceTransactionsTable
import com.app.biashara.db.OrdersTable
import com.app.biashara.db.PaymentsTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.insert
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * CyberSource Secure Acceptance Hosted Checkout (SAHC) Service
 *
 * Flow:
 *  1. Merchant calls buildSaForm() → receives signed hidden fields
 *  2. Frontend auto-submits form → customer lands on CyberSource's hosted card page
 *  3. CyberSource POSTs result to /v1/payments/card/sa-notify (server-to-server)
 *  4. CyberSource redirects browser to /v1/payments/card/sa-return
 *
 * HMAC: HMAC-SHA256 using the Secure Acceptance profile's secret key.
 */
class SecureAcceptanceService(
    private val settingsService: BusinessSettingsService
) {

    companion object {
        private const val SANDBOX_URL    = "https://testsecureacceptance.cybersource.com/pay"
        private const val PRODUCTION_URL = "https://secureacceptance.cybersource.com/pay"

        private const val DEFAULT_PROFILE_ID = "632ECA4B-6F81-44A2-A6CD-C0C362D55F85"
        private const val DEFAULT_ACCESS_KEY = "f82a7e397ee238c99debbfe04efceb15"
        private const val DEFAULT_SECRET_KEY = "51e8916680ec49b3afab8a1c60b39e5a81bcd216bdc349f7bd5be2dbb6169688028aa14facd44344aaa6c14d33234e530ea495aedc8249f9b4435569dc3603b0c3a10df3a5084b85b922d509fe4e08f8179dbe7978974f6fbab95c0048d807cd7b10eed553064f2aa3400a4e985e9ab4db39f6089004491fba2d578da9b7cab2"

        // Fields that CyberSource requires to be signed.
        // Order MATTERS for the signature string.
        private val SIGNED_FIELDS = listOf(
            "access_key", "profile_id", "transaction_uuid", "signed_field_names",
            "unsigned_field_names", "signed_date_time", "locale", "transaction_type",
            "reference_number", "amount", "currency", "payment_method",
            "bill_to_forename", "bill_to_surname", "bill_to_email", "bill_to_phone",
            "bill_to_address_line1", "bill_to_address_city", "bill_to_address_country",
            "merchant_defined_data1", "merchant_defined_data2",
            "override_custom_receipt_page_url", "override_custom_cancel_page_url"
        )
    }

    /**
     * Load full SA config (profileId + accessKey + secretKey) for a business.
     */
    private fun loadSaConfig(businessId: String): SaConfig = transaction {
        val existing = CyberSourceConfigsTable
            .select { CyberSourceConfigsTable.businessId eq businessId }
            .firstOrNull()

        if (existing != null) {
            val profileId = existing[CyberSourceConfigsTable.profileId].ifBlank { DEFAULT_PROFILE_ID }
            val accessKey = existing[CyberSourceConfigsTable.accessKey].ifBlank { DEFAULT_ACCESS_KEY }
            val secretKey = existing[CyberSourceConfigsTable.merchantSecretKey].ifBlank { DEFAULT_SECRET_KEY }
            val env       = existing[CyberSourceConfigsTable.environment].ifBlank { "production" }
            SaConfig(profileId, accessKey, secretKey, env)
        } else {
            SaConfig(DEFAULT_PROFILE_ID, DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY, "production")
        }
    }

    /**
     * Build the Secure Acceptance signed form fields.
     * Returns the CyberSource action URL and all hidden field key-value pairs.
     */
    fun buildSaForm(req: SaFormRequest): SaFormResult {
        val config = loadSaConfig(req.businessId)
            ?: return SaFormResult(
                success = false,
                error = "CyberSource Secure Acceptance is not configured for this business. " +
                        "Please set Profile ID, Access Key, and Secret Key in Settings → CyberSource."
            )

        // Split customer name into forename/surname
        val nameParts   = (req.customerName ?: "Customer").trim().split(" ")
        val forename    = nameParts.firstOrNull() ?: "Customer"
        val surname     = nameParts.drop(1).joinToString(" ").ifEmpty { "." }

        // SA date format: yyyy-MM-dd'T'HH:mm:ss'Z'
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val signedDateTime = sdf.format(Date())

        val txnUuid = UUID.randomUUID().toString().replace("-", "")

        // Determine redirect URLs based on runtime API base
        val apiBase = "https://api.biashara360.co.ke/v1"
        val receiptUrl = "$apiBase/public/payments/card/sa-return"
        val cancelUrl  = "$apiBase/public/payments/card/sa-return"

        val fields = linkedMapOf(
            "access_key"                      to config.accessKey,
            "profile_id"                      to config.profileId,
            "transaction_uuid"                to txnUuid,
            "signed_field_names"              to SIGNED_FIELDS.joinToString(","),
            "unsigned_field_names"            to "",
            "signed_date_time"                to signedDateTime,
            "locale"                          to "en-us",
            "transaction_type"                to "sale",   // auth + capture in one step
            "reference_number"                to req.orderId,
            "amount"                          to "%.2f".format(req.amount),
            "currency"                        to "KES",
            "payment_method"                  to "card",
            "bill_to_forename"                to forename,
            "bill_to_surname"                 to surname,
            "bill_to_email"                   to (req.customerEmail?.ifBlank { null } ?: "customer@biashara360.co.ke"),
            "bill_to_phone"                   to (req.customerPhone?.ifBlank { null } ?: "0700000000"),
            "bill_to_address_line1"           to "Nairobi",
            "bill_to_address_city"            to "Nairobi",
            "bill_to_address_country"         to "KE",
            "merchant_defined_data1"          to req.businessId,   // carry businessId through for notify
            "merchant_defined_data2"          to req.orderId,
            "override_custom_receipt_page_url" to receiptUrl,
            "override_custom_cancel_page_url"  to cancelUrl
        )

        fields["signature"] = sign(fields, config.secretKey)

        val actionUrl = if (config.environment == "production") PRODUCTION_URL else SANDBOX_URL

        return SaFormResult(
            success   = true,
            actionUrl = actionUrl,
            fields    = fields
        )
    }

    /**
     * Verify an incoming SA notification / browser return.
     * Returns the result decision and parsed fields if signature is valid.
     */
    fun verifyAndProcess(postFields: Map<String, String>): SaNotifyResult {
        val businessId = postFields["merchant_defined_data1"] ?: ""
        val orderId    = postFields["merchant_defined_data2"] ?: postFields["reference_number"] ?: ""
        val decision   = postFields["decision"] ?: "ERROR"
        val reasonCode = postFields["reason_code"] ?: ""
        val authCode   = postFields["auth_code"] ?: ""
        val csTransId  = postFields["transaction_id"] ?: ""
        val amount     = postFields["amount"]?.toDoubleOrNull() ?: 0.0
        val cardLast4  = postFields["req_card_number"]?.takeLast(4)
        val cardType   = mapCardType(postFields["req_card_type"] ?: "")
        val holderName = postFields["req_bill_to_forename"].orEmpty() + " " + postFields["req_bill_to_surname"].orEmpty()
        val reqProfileId = postFields["req_profile_id"] ?: ""

        // Load config to verify signature
        val config = loadSaConfig(businessId)
        if (config == null) {
            return SaNotifyResult(false, orderId, businessId, "ERROR", "Business SA config not found")
        }

        // Re-construct signed field names from the post
        val signedFieldNames = postFields["signed_field_names"]
            ?.split(",")?.map { it.trim() } ?: emptyList()
        val signedString = signedFieldNames.joinToString(",") { fieldName ->
            "$fieldName=${postFields[fieldName] ?: ""}"
        }
        val expectedSig = hmacSha256(signedString, config.secretKey)
        val receivedSig = postFields["signature"] ?: ""

        if (expectedSig != receivedSig) {
            return SaNotifyResult(false, orderId, businessId, "ERROR", "Signature mismatch — possible tampering")
        }

        // Persist transaction record
        val success = decision == "ACCEPT"
        val status  = if (success) "CAPTURED" else "DECLINED"
        val txnId   = UUID.randomUUID().toString()

        transaction {
            CyberSourceTransactionsTable.insert {
                it[CyberSourceTransactionsTable.id]                = txnId
                it[CyberSourceTransactionsTable.businessId]        = businessId
                it[CyberSourceTransactionsTable.orderId]           = orderId.takeIf { o -> o.isNotBlank() }
                it[CyberSourceTransactionsTable.csTransactionId]   = csTransId.takeIf { c -> c.isNotBlank() }
                it[CyberSourceTransactionsTable.csReconciliationId]= postFields["reconciliation_id"]
                it[CyberSourceTransactionsTable.csApprovalCode]    = authCode.takeIf { a -> a.isNotBlank() }
                it[CyberSourceTransactionsTable.amount]            = amount
                it[CyberSourceTransactionsTable.currency]          = "KES"
                it[CyberSourceTransactionsTable.cardLast4]         = cardLast4
                it[CyberSourceTransactionsTable.cardType]          = cardType.takeIf { c -> c.isNotBlank() }
                it[CyberSourceTransactionsTable.cardholderName]    = holderName.trim().takeIf { n -> n.isNotBlank() }
                it[CyberSourceTransactionsTable.transactionType]   = "SALE"
                it[CyberSourceTransactionsTable.status]            = status
                it[CyberSourceTransactionsTable.errorReason]       = if (!success) "DECLINED reason=$reasonCode" else null
                it[CyberSourceTransactionsTable.errorMessage]      = if (!success) "Decision: $decision, Reason: $reasonCode" else null
                it[CyberSourceTransactionsTable.clientReference]   = orderId
                it[CyberSourceTransactionsTable.createdAt]         = Clock.System.now()
                it[CyberSourceTransactionsTable.updatedAt]         = Clock.System.now()
            }

            if (success && orderId.isNotBlank()) {
                // Mark order as PAID
                OrdersTable.update({ OrdersTable.id eq orderId }) {
                    it[OrdersTable.paymentStatus] = "PAID"
                    it[OrdersTable.tabStatus] = "CLOSED"
                    it[OrdersTable.updatedAt]     = Clock.System.now()
                }
                // Insert payment record for reconciliation
                PaymentsTable.insert {
                    it[PaymentsTable.id]              = UUID.randomUUID().toString()
                    it[PaymentsTable.businessId]      = businessId
                    it[PaymentsTable.orderId]         = orderId
                    it[PaymentsTable.transactionCode] = csTransId
                    it[PaymentsTable.amount]          = amount
                    it[PaymentsTable.payerPhone]      = postFields["req_bill_to_phone"] ?: ""
                    it[PaymentsTable.payerName]       = holderName.trim()
                    it[PaymentsTable.method]          = "CARD"
                    it[PaymentsTable.status]          = "SUCCESS"
                    it[PaymentsTable.channel]         = "CYBERSOURCE_SA"
                    it[PaymentsTable.reconciled]      = true
                    it[PaymentsTable.notes]           = "Secure Acceptance — CS txn: $csTransId"
                    it[PaymentsTable.transactionDate] = Clock.System.now()
                }
            }
        }

        return SaNotifyResult(
            verified   = true,
            orderId    = orderId,
            businessId = businessId,
            decision   = decision,
            message    = if (success) "Payment accepted" else "Payment declined: reason $reasonCode"
        )
    }

    // ── Private Helpers ────────────────────────────────────────────────────────

    private fun sign(fields: Map<String, String>, secretKey: String): String {
        val signedFieldNames = SIGNED_FIELDS.joinToString(",")
        val message = SIGNED_FIELDS.joinToString(",") { name -> "$name=${fields[name] ?: ""}" }
        return hmacSha256(message, secretKey)
    }

    private fun hmacSha256(message: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }

    private fun mapCardType(csCode: String) = when (csCode) {
        "001" -> "VISA"
        "002" -> "MASTERCARD"
        "003" -> "AMEX"
        else  -> ""
    }
}

// ─── Data Classes ──────────────────────────────────────────────────────────────

data class SaConfig(
    val profileId   : String,
    val accessKey   : String,
    val secretKey   : String,
    val environment : String
)

data class SaFormRequest(
    val businessId    : String,
    val orderId       : String,
    val amount        : Double,
    val customerName  : String? = null,
    val customerEmail : String? = null,
    val customerPhone : String? = null
)

data class SaFormResult(
    val success   : Boolean,
    val actionUrl : String = "",
    val fields    : Map<String, String> = emptyMap(),
    val error     : String? = null
)

data class SaNotifyResult(
    val verified   : Boolean,
    val orderId    : String,
    val businessId : String,
    val decision   : String,
    val message    : String
)
