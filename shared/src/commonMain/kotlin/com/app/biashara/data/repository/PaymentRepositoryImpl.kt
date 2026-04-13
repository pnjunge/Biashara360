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

class PaymentRepositoryImpl(
    private val database: Biashara360Database
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

    override suspend fun initiateSTKPush(request: MpesaStkPushRequest): Result<MpesaStkPushResponse> {
        // STK push is handled via the backend; return a placeholder result
        return Result.failure(UnsupportedOperationException("STK Push requires backend integration"))
    }

    override suspend fun reconcilePayment(paymentId: String, orderId: String): Result<Unit> =
        runCatching {
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
