package com.app.biashara.services

import com.app.biashara.db.CsCustomerTokensTable
import com.app.biashara.db.CyberSourceTransactionsTable
import com.app.biashara.db.PaymentsTable
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class CyberSourcePaymentService(
    private val cs: CyberSourceService,
    private val settingsService: BusinessSettingsService? = null
) {

    // ── Resolve per-business CyberSource service ──────────────────────────────
    private fun csFor(businessId: String): CyberSourceService {
        if (settingsService != null) {
            val dbConfig = settingsService.loadCyberSourceConfigForBusiness(businessId)
            if (dbConfig != null) {
                return CyberSourceService(dbConfig, cs.httpClient)
            }
        }
        return cs
    }

    // ── Charge (Auth + optional capture) ──────────────────────────────────────
    suspend fun charge(businessId: String, req: CsChargeRequest): CsChargeResponse {
        val csService = csFor(businessId)

        // 1. Resolve saved card token if using stored card
        val (payCard, payFlexToken, payCustomerId) = resolvePay(businessId, req)

        // 2. Build CyberSource request
        val csRequest = CyberSourceService.buildPaymentRequest(
            orderId      = req.orderId,
            amountKes    = req.amount,
            card         = payCard,
            flexToken    = payFlexToken,
            customerId   = payCustomerId,
            billingName  = req.cardholderName ?: "",
            email        = req.billingEmail ?: "",
            phone        = req.billingPhone ?: "",
            capture      = req.captureImmediately
        )

        // 3. Call CyberSource
        val result = if (req.captureImmediately) {
            csService.authorizeAndCapture(csRequest)
        } else {
            csService.authorizePayment(csRequest)
        }

        // 4. Persist and return
        return when (result) {
            is CsResult.Success -> {
                val data = result.data
                val txnType = if (req.captureImmediately) "CAPTURE" else "AUTHORIZATION"
                val csStatus = mapCsStatus(data.status, txnType)
                val cardLast4 = req.cardNumber?.takeLast(4) ?: "????"

                val txnId = persistTransaction(
                    businessId     = businessId,
                    orderId        = req.orderId,
                    csTransId      = data.id,
                    reconcId       = data.reconciliationId,
                    approvalCode   = data.processorInformation?.approvalCode,
                    amount         = req.amount,
                    currency       = req.currency,
                    cardLast4      = cardLast4,
                    cardType       = detectCardType(req.cardNumber ?: ""),
                    holderName     = req.cardholderName,
                    txnType        = txnType,
                    status         = csStatus,
                    clientRef      = req.orderId
                )

                // 4b. Save card if requested
                val savedCardId = if (req.saveCard && data.status == "AUTHORIZED" && payCard != null) {
                    saveCardToken(businessId, req, data)
                } else null

                // 4c. Update generic payments table if order present
                if (csStatus == "CAPTURED" || csStatus == "AUTHORIZED") {
                    upsertPaymentRecord(businessId, req.orderId, txnId, req.amount, data.id ?: "")
                }

                CsChargeResponse(
                    transactionId   = txnId,
                    csTransactionId = data.id,
                    status          = csStatus,
                    approvalCode    = data.processorInformation?.approvalCode,
                    reconciliationId= data.reconciliationId,
                    cardLast4       = cardLast4,
                    cardType        = detectCardType(req.cardNumber ?: ""),
                    savedCardId     = savedCardId,
                    errorReason     = data.errorInformation?.reason,
                    errorMessage    = data.errorInformation?.message
                )
            }

            is CsResult.Failure -> {
                persistTransaction(
                    businessId = businessId, orderId = req.orderId,
                    csTransId = null, reconcId = null, approvalCode = null,
                    amount = req.amount, currency = req.currency,
                    cardLast4 = req.cardNumber?.takeLast(4), cardType = null, holderName = req.cardholderName,
                    txnType = "AUTHORIZATION", status = "ERROR",
                    clientRef = req.orderId, errorReason = result.reason, errorMessage = result.message
                )
                CsChargeResponse(
                    transactionId = UUID.randomUUID().toString(),
                    csTransactionId = null, status = "ERROR",
                    approvalCode = null, reconciliationId = null,
                    cardLast4 = req.cardNumber?.takeLast(4), cardType = null, savedCardId = null,
                    errorReason = result.reason, errorMessage = result.message
                )
            }
        }
    }

    // ── Capture a prior auth ──────────────────────────────────────────────────
    suspend fun capture(businessId: String, req: CsCaptureRouteRequest): CsChargeResponse {
        val csService = csFor(businessId)
        val txn = getTransaction(businessId, req.csTransactionId)
            ?: return errorResponse("Transaction not found")

        val csReq = CsCaptureRequest(
            clientReferenceInformation = CsClientRef(txn.orderId ?: "B360-CAPTURE"),
            orderInformation = CsOrderInfo(
                amountDetails = CsAmountDetails(
                    totalAmount = String.format("%.2f", req.amount ?: txn.amount),
                    currency = txn.currency
                )
            )
        )

        return when (val result = csService.capturePayment(txn.csTransactionId!!, csReq)) {
            is CsResult.Success -> {
                updateTransactionStatus(txn.id, "CAPTURED", result.data.id)
                CsChargeResponse(
                    transactionId = txn.id, csTransactionId = result.data.id,
                    status = "CAPTURED", approvalCode = result.data.processorInformation?.approvalCode,
                    reconciliationId = result.data.reconciliationId,
                    cardLast4 = txn.cardLast4, cardType = txn.cardType,
                    savedCardId = null, errorReason = null, errorMessage = null
                )
            }
            is CsResult.Failure -> errorResponse(result.message, result.reason)
        }
    }

    // ── Refund ────────────────────────────────────────────────────────────────
    suspend fun refund(businessId: String, req: CsRefundRouteRequest): CsChargeResponse {
        val csService = csFor(businessId)
        val txn = getTransaction(businessId, req.csTransactionId)
            ?: return errorResponse("Transaction not found")

        val csReq = CsRefundRequest(
            clientReferenceInformation = CsClientRef("REFUND-${txn.orderId}"),
            orderInformation = CsOrderInfo(
                amountDetails = CsAmountDetails(
                    totalAmount = String.format("%.2f", req.amount),
                    currency = txn.currency
                )
            )
        )

        return when (val result = csService.refundPayment(txn.csTransactionId!!, csReq)) {
            is CsResult.Success -> {
                persistTransaction(
                    businessId = businessId, orderId = txn.orderId, csTransId = result.data.id,
                    reconcId = result.data.reconciliationId, approvalCode = null,
                    amount = req.amount, currency = txn.currency,
                    cardLast4 = txn.cardLast4, cardType = txn.cardType, holderName = null,
                    txnType = "REFUND", status = "REFUNDED", clientRef = txn.orderId
                )
                CsChargeResponse(
                    transactionId = txn.id, csTransactionId = result.data.id, status = "REFUNDED",
                    approvalCode = null, reconciliationId = result.data.reconciliationId,
                    cardLast4 = txn.cardLast4, cardType = txn.cardType, savedCardId = null,
                    errorReason = null, errorMessage = null
                )
            }
            is CsResult.Failure -> errorResponse(result.message, result.reason)
        }
    }

    // ── Void ──────────────────────────────────────────────────────────────────
    suspend fun void(businessId: String, req: CsVoidRouteRequest): CsChargeResponse {
        val csService = csFor(businessId)
        val txn = getTransaction(businessId, req.csTransactionId)
            ?: return errorResponse("Transaction not found")

        val csReq = CsVoidRequest(
            clientReferenceInformation = CsClientRef("VOID-${txn.orderId}")
        )

        return when (val result = csService.voidAuthorization(txn.csTransactionId!!, csReq)) {
            is CsResult.Success -> {
                updateTransactionStatus(txn.id, "VOIDED", null)
                CsChargeResponse(
                    transactionId = txn.id, csTransactionId = txn.csTransactionId, status = "VOIDED",
                    approvalCode = null, reconciliationId = null,
                    cardLast4 = txn.cardLast4, cardType = txn.cardType, savedCardId = null,
                    errorReason = null, errorMessage = null
                )
            }
            is CsResult.Failure -> errorResponse(result.message, result.reason)
        }
    }

    // ── Capture Context for Unified Checkout widget ───────────────────────────
    // When businessId is supplied, the tenant's DB-stored credentials are used.
    // If no tenant config is found for that businessId, csFor() falls back to
    // the globally configured service (same behaviour as all other payment ops).
    suspend fun getCaptureContext(targetOrigin: String, businessId: String? = null): String? {
        val csService = if (businessId != null) csFor(businessId) else cs
        return when (val r = csService.generateCaptureContext(targetOrigin)) {
            is CsResult.Success -> r.data
            is CsResult.Failure -> null
        }
    }

    // ── Saved cards ───────────────────────────────────────────────────────────
    fun getSavedCards(businessId: String, customerId: String? = null): List<CsSavedCard> =
        transaction {
            CsCustomerTokensTable
                .select {
                    CsCustomerTokensTable.businessId eq businessId and
                    if (customerId != null) CsCustomerTokensTable.customerId eq customerId
                    else Op.TRUE
                }
                .map { row ->
                    CsSavedCard(
                        id          = row[CsCustomerTokensTable.id],
                        csCustomerId= row[CsCustomerTokensTable.csCustomerId],
                        cardLast4   = row[CsCustomerTokensTable.cardLast4],
                        cardType    = row[CsCustomerTokensTable.cardType],
                        expiryMonth = row[CsCustomerTokensTable.expiryMonth],
                        expiryYear  = row[CsCustomerTokensTable.expiryYear],
                        holderName  = row[CsCustomerTokensTable.holderName],
                        isDefault   = row[CsCustomerTokensTable.isDefault]
                    )
                }
        }

    fun getTransactions(businessId: String): List<CsTransactionRecord> =
        transaction {
            CyberSourceTransactionsTable
                .select { CyberSourceTransactionsTable.businessId eq businessId }
                .orderBy(CyberSourceTransactionsTable.createdAt, SortOrder.DESC)
                .map { rowToRecord(it) }
        }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun resolvePay(businessId: String, req: CsChargeRequest): Triple<CsCard?, String?, String?> {
        if (req.savedCardId != null) {
            val tokenRow = transaction {
                CsCustomerTokensTable.select {
                    (CsCustomerTokensTable.id eq req.savedCardId) and (CsCustomerTokensTable.businessId eq businessId)
                }.firstOrNull()
            }
            val csId = tokenRow?.get(CsCustomerTokensTable.csCustomerId)
                ?: throw IllegalArgumentException("Saved card not found")
            return Triple(null, null, csId)
        }
        if (req.transientToken != null) return Triple(null, req.transientToken, null)
        if (req.cardNumber != null) {
            return Triple(
                CsCard(
                    number = req.cardNumber,
                    expirationMonth = req.cardExpiryMonth ?: "12",
                    expirationYear  = req.cardExpiryYear ?: "2030",
                    securityCode    = req.cardCvv,
                    type            = detectCardTypeCode(req.cardNumber)
                ), null, null
            )
        }
        throw IllegalArgumentException("No payment method provided")
    }

    private fun persistTransaction(
        businessId: String, orderId: String?, csTransId: String?,
        reconcId: String?, approvalCode: String?, amount: Double, currency: String,
        cardLast4: String?, cardType: String?, holderName: String?,
        txnType: String, status: String, clientRef: String?,
        errorReason: String? = null, errorMessage: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val now = Clock.System.now()
        transaction {
            CyberSourceTransactionsTable.insert {
                it[CyberSourceTransactionsTable.id]               = id
                it[CyberSourceTransactionsTable.businessId]       = businessId
                it[CyberSourceTransactionsTable.orderId]          = orderId
                it[CyberSourceTransactionsTable.csTransactionId]  = csTransId
                it[CyberSourceTransactionsTable.csReconciliationId]= reconcId
                it[CyberSourceTransactionsTable.csApprovalCode]   = approvalCode
                it[CyberSourceTransactionsTable.amount]           = amount
                it[CyberSourceTransactionsTable.currency]         = currency
                it[CyberSourceTransactionsTable.cardLast4]        = cardLast4
                it[CyberSourceTransactionsTable.cardType]         = cardType
                it[CyberSourceTransactionsTable.cardholderName]   = holderName
                it[CyberSourceTransactionsTable.transactionType]  = txnType
                it[CyberSourceTransactionsTable.status]           = status
                it[CyberSourceTransactionsTable.errorReason]      = errorReason
                it[CyberSourceTransactionsTable.errorMessage]     = errorMessage
                it[CyberSourceTransactionsTable.clientReference]  = clientRef
                it[CyberSourceTransactionsTable.createdAt]        = now
                it[CyberSourceTransactionsTable.updatedAt]        = now
            }
        }
        return id
    }

    private fun updateTransactionStatus(id: String, status: String, newCsId: String?) {
        transaction {
            CyberSourceTransactionsTable.update({ CyberSourceTransactionsTable.id eq id }) {
                it[CyberSourceTransactionsTable.status] = status
                if (newCsId != null) it[csTransactionId] = newCsId
                it[updatedAt] = Clock.System.now()
            }
        }
    }

    private fun upsertPaymentRecord(businessId: String, orderId: String, txnId: String, amount: Double, csId: String) {
        transaction {
            PaymentsTable.insert {
                it[PaymentsTable.id]              = UUID.randomUUID().toString()
                it[PaymentsTable.businessId]      = businessId
                it[PaymentsTable.orderId]         = orderId
                it[PaymentsTable.transactionCode] = csId
                it[PaymentsTable.amount]          = amount
                it[PaymentsTable.payerPhone]      = ""
                it[PaymentsTable.payerName]       = ""
                it[PaymentsTable.method]          = "CARD"
                it[PaymentsTable.status]          = "SUCCESS"
                it[PaymentsTable.channel]         = "CYBERSOURCE"
                it[PaymentsTable.reconciled]      = true
                it[PaymentsTable.notes]           = "CyberSource txn: $txnId"
                it[PaymentsTable.transactionDate] = Clock.System.now()
            }
        }
    }

    private fun saveCardToken(businessId: String, req: CsChargeRequest, data: CsPaymentResponse): String? {
        // In production, CyberSource returns customer TMS ID in response
        // For now we stub — wire to cs.createCustomerToken() in full integration
        val tokenId = UUID.randomUUID().toString()
        transaction {
            CsCustomerTokensTable.insert {
                it[id]           = tokenId
                it[CsCustomerTokensTable.businessId] = businessId
                it[csCustomerId] = "CS-${tokenId.take(8)}"  // replace with real TMS ID
                it[cardLast4]    = req.cardNumber?.takeLast(4) ?: "????"
                it[cardType]     = detectCardType(req.cardNumber ?: "")
                it[expiryMonth] = req.cardExpiryMonth ?: "??"
                it[expiryYear] = req.cardExpiryYear ?: "????"
                it[holderName]   = req.cardholderName ?: ""
                it[isDefault]    = false
                it[createdAt]    = Clock.System.now()
            }
        }
        return tokenId
    }

    private fun getTransaction(businessId: String, csId: String?): CsTransactionRecord? {
        if (csId == null) return null
        return transaction {
            CyberSourceTransactionsTable.select {
                CyberSourceTransactionsTable.businessId eq businessId and
                (CyberSourceTransactionsTable.csTransactionId eq csId or
                 (CyberSourceTransactionsTable.id eq csId))
            }.firstOrNull()?.let { rowToRecord(it) }
        }
    }

    private fun rowToRecord(row: ResultRow) = CsTransactionRecord(
        id               = row[CyberSourceTransactionsTable.id],
        orderId          = row[CyberSourceTransactionsTable.orderId],
        csTransactionId  = row[CyberSourceTransactionsTable.csTransactionId],
        amount           = row[CyberSourceTransactionsTable.amount],
        currency         = row[CyberSourceTransactionsTable.currency],
        status           = row[CyberSourceTransactionsTable.status],
        transactionType  = row[CyberSourceTransactionsTable.transactionType],
        cardLast4        = row[CyberSourceTransactionsTable.cardLast4],
        cardType         = row[CyberSourceTransactionsTable.cardType],
        cardholderName   = row[CyberSourceTransactionsTable.cardholderName],
        approvalCode     = row[CyberSourceTransactionsTable.csApprovalCode],
        reconciliationId = row[CyberSourceTransactionsTable.csReconciliationId],
        errorReason      = row[CyberSourceTransactionsTable.errorReason],
        createdAt        = row[CyberSourceTransactionsTable.createdAt].toString()
    )

    private fun mapCsStatus(csStatus: String?, txnType: String): String = when (csStatus?.uppercase()) {
        "AUTHORIZED"            -> if (txnType == "CAPTURE") "CAPTURED" else "AUTHORIZED"
        "PARTIAL_AUTHORIZED"    -> "AUTHORIZED"
        "AUTHORIZED_PENDING_REVIEW" -> "AUTHORIZED"
        "DECLINED"              -> "DECLINED"
        "INVALID_REQUEST"       -> "ERROR"
        "SERVER_ERROR"          -> "ERROR"
        null                    -> "ERROR"
        else                    -> csStatus
    }

    private fun detectCardType(number: String): String = when {
        number.startsWith("4")          -> "VISA"
        number.startsWith("5") || number.startsWith("2") -> "MASTERCARD"
        number.startsWith("3")          -> "AMEX"
        else                            -> "UNKNOWN"
    }

    private fun detectCardTypeCode(number: String): String = when {
        number.startsWith("4")          -> "001"  // Visa
        number.startsWith("5") || number.startsWith("2") -> "002"  // MC
        number.startsWith("3")          -> "003"  // Amex
        else                            -> "001"
    }

    private fun errorResponse(msg: String, reason: String = "ERROR") = CsChargeResponse(
        transactionId = UUID.randomUUID().toString(), csTransactionId = null,
        status = "ERROR", approvalCode = null, reconciliationId = null,
        cardLast4 = null, cardType = null, savedCardId = null,
        errorReason = reason, errorMessage = msg
    )
}

// Extension — delete saved card token
fun CyberSourcePaymentService.deleteSavedCard(businessId: String, cardId: String) {
    transaction {
        CsCustomerTokensTable.deleteWhere {
            (CsCustomerTokensTable.id eq cardId) and (CsCustomerTokensTable.businessId eq businessId)
        }
    }
}
