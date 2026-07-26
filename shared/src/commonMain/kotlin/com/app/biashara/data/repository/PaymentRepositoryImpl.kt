package com.app.biashara.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.app.biashara.db.Biashara360Database
import com.app.biashara.db.PaymentEntity
import com.app.biashara.domain.model.*
import com.app.biashara.domain.repository.PaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class PaymentDto(
    val id: String,
    val businessId: String,
    val orderId: String?,
    val transactionCode: String,
    val amount: Double,
    val payerPhone: String,
    val payerName: String,
    val method: String,
    val status: String,
    val channel: String,
    val reconciled: Boolean,
    val notes: String?,
    val transactionDate: String
)

@Serializable
data class StkInitiateRequest(
    val orderId: String,
    val phoneNumber: String,
    val accountType: String? = null
)

@Serializable
private data class ReconcilePaymentRequest(val orderId: String)

class PaymentRepositoryImpl(
    private val database: Biashara360Database,
    private val client: HttpClient
) : PaymentRepository {

    private val queries = database.biashara360DatabaseQueries

    override fun getPayments(businessId: String): Flow<List<Payment>> =
        queries.selectAllPayments(businessId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toDomain() } }

    override fun getUnreconciledPayments(businessId: String): Flow<List<Payment>> =
        queries.selectUnreconciledPayments(businessId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { it.map { entity -> entity.toDomain() } }

    override suspend fun initiateSTKPush(request: MpesaStkPushRequest): Result<MpesaStkPushResponse> =
        runCatching {
            val body = StkInitiateRequest(
                orderId = request.accountReference, // use accountReference as fallback
                phoneNumber = request.phoneNumber,
                accountType = request.accountType
            )
            val response: ApiResponse<MpesaStkPushResponse> = client.post("$BASE_URL/payments/initiate") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
            if (!response.success || response.data == null) {
                throw Exception(response.message.ifBlank { "STK Push failed" })
            }
            response.data
        }

    override suspend fun reconcilePayment(paymentId: String, orderId: String): Result<Unit> =
        runCatching {
            require(orderId.isNotBlank()) { "Select an order before matching this payment" }
            val response: ApiResponse<Unit> = client.post("$BASE_URL/payments/$paymentId/reconcile") {
                contentType(ContentType.Application.Json)
                setBody(ReconcilePaymentRequest(orderId))
            }.body()
            if (!response.success) {
                throw Exception(response.message.ifBlank { "Failed to reconcile payment on server" })
            }
            queries.updateReconciliation(orderId = orderId, paymentId = paymentId)
        }

    override suspend fun savePayment(payment: Payment): Result<Payment> = runCatching {
        queries.insertPayment(
            id = payment.id,
            business_id = payment.businessId,
            order_id = payment.orderId,
            transaction_code = payment.transactionCode,
            amount = payment.amount,
            payer_phone = payment.payerPhone,
            payer_name = payment.payerName,
            method = payment.method.name,
            status = payment.status.name,
            channel = payment.channel.name,
            reconciled = if (payment.reconciled) 1L else 0L,
            notes = payment.notes,
            transaction_date = payment.transactionDate.toString()
        )
        payment
    }

    override suspend fun getPaymentDashboard(businessId: String): PaymentDashboard {
        val now = Clock.System.now()
        val start = now.toString().substring(0, 7) + "-01T00:00:00Z" // First of month
        val collected = queries.sumPaymentsByPeriod(businessId, start, now.toString())
            .executeAsOne().SUM ?: 0.0
        return PaymentDashboard(
            businessId = businessId,
            totalCollected = collected,
            pendingAmount = 0.0,
            transactionCount = 0,
            byChannel = emptyMap(),
            recentTransactions = emptyList()
        )
    }

     override fun getPaymentsByDateRange(
         businessId: String,
         start: LocalDate,
         end: LocalDate
     ): Flow<List<Payment>> {
         val startStr = start.atStartOfDayIn(TimeZone.of("Africa/Nairobi")).toString()
         val endStr = end.atStartOfDayIn(TimeZone.of("Africa/Nairobi")).toString()
         return getPayments(businessId).map { payments ->
             payments.filter {
                 it.transactionDate.toString() >= startStr &&
                     it.transactionDate.toString() <= endStr
             }
         }
     }

    /** Sync payments from API and update local cache **/
    suspend fun syncPaymentsFromApi(businessId: String): Result<List<Payment>> = runCatching {
        val response: ApiResponse<List<PaymentDto>> = client.get("$BASE_URL/payments") {
            url { parameters.append("businessId", businessId) }
        }.body()

        if (!response.success || response.data == null) {
            throw Exception(response.message.ifBlank { "Failed to fetch payments" })
        }

        // Update local cache
        response.data.forEach { dto ->
            queries.insertPayment(
                id = dto.id,
                business_id = dto.businessId,
                order_id = dto.orderId,
                transaction_code = dto.transactionCode,
                amount = dto.amount,
                payer_phone = dto.payerPhone,
                payer_name = dto.payerName,
                method = dto.method,
                status = dto.status,
                channel = dto.channel,
                reconciled = if (dto.reconciled) 1L else 0L,
                notes = dto.notes ?: "",
                transaction_date = dto.transactionDate
            )
        }

        response.data.map { it.toDomain() }
    }

    private fun PaymentDto.toDomain() = Payment(
        id = id,
        businessId = businessId,
        orderId = orderId,
        transactionCode = transactionCode,
        amount = amount,
        payerPhone = payerPhone,
        payerName = payerName,
        method = runCatching { PaymentMethod.valueOf(method) }.getOrDefault(PaymentMethod.MPESA),
        status = runCatching { TransactionStatus.valueOf(status) }.getOrDefault(TransactionStatus.SUCCESS),
        channel = runCatching { PaymentChannel.valueOf(channel) }.getOrDefault(PaymentChannel.MPESA_C2B),
        reconciled = reconciled,
        notes = notes ?: "",
        transactionDate = Instant.parse(transactionDate)
    )

     private fun PaymentEntity.toDomain() = Payment(
         id = id,
         businessId = business_id,
         orderId = order_id,
         transactionCode = transaction_code,
         amount = amount,
         payerPhone = payer_phone,
         payerName = payer_name,
         method = runCatching { PaymentMethod.valueOf(method) }.getOrDefault(PaymentMethod.MPESA),
         status = runCatching { TransactionStatus.valueOf(status) }
             .getOrDefault(TransactionStatus.SUCCESS),
         channel = runCatching { PaymentChannel.valueOf(channel) }
             .getOrDefault(PaymentChannel.MPESA_C2B),
         reconciled = reconciled == 1L,
         notes = notes,
         transactionDate = runCatching { Instant.parse(transaction_date) }
             .getOrDefault(Clock.System.now())
     )
}
