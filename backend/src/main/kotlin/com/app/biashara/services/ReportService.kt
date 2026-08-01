package com.app.biashara.services

import com.app.biashara.db.OrdersTable
import com.app.biashara.db.PaymentsTable
import com.app.biashara.models.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ReportService {
    fun paymentReport(businessId: String, startDate: String, endDate: String): PaymentReportResponse {
        val range = reportRange(startDate, endDate)
        return transaction {
            val rows = PaymentsTable.select {
                (PaymentsTable.businessId eq businessId) and
                    (PaymentsTable.transactionDate greaterEq range.first) and
                    (PaymentsTable.transactionDate less range.second)
            }.orderBy(PaymentsTable.transactionDate, SortOrder.DESC).toList()
            val successful = rows.filter { it[PaymentsTable.status] in setOf("SUCCESS", "COMPLETED", "PAID") }
            PaymentReportResponse(
                period = "$startDate to $endDate",
                totalTransactions = rows.size,
                totalAmount = successful.sumOf { it[PaymentsTable.amount] },
                reconciledAmount = successful.filter { it[PaymentsTable.reconciled] }.sumOf { it[PaymentsTable.amount] },
                byMethod = rows.toBreakdown(PaymentsTable.method, PaymentsTable.amount),
                byChannel = rows.toBreakdown(PaymentsTable.channel, PaymentsTable.amount),
                payments = rows.take(MAX_REPORT_ROWS).map {
                    PaymentReportRow(
                        transactionCode = it[PaymentsTable.transactionCode],
                        orderId = it[PaymentsTable.orderId],
                        amount = it[PaymentsTable.amount],
                        payerName = it[PaymentsTable.payerName],
                        payerPhone = it[PaymentsTable.payerPhone],
                        method = it[PaymentsTable.method],
                        channel = it[PaymentsTable.channel],
                        status = it[PaymentsTable.status],
                        reconciled = it[PaymentsTable.reconciled],
                        transactionDate = it[PaymentsTable.transactionDate].toString()
                    )
                }
            )
        }
    }

    fun orderReport(businessId: String, startDate: String, endDate: String): OrderReportResponse {
        val range = reportRange(startDate, endDate)
        return transaction {
            val rows = OrdersTable.select {
                (OrdersTable.businessId eq businessId) and
                    (OrdersTable.createdAt greaterEq range.first) and
                    (OrdersTable.createdAt less range.second)
            }.orderBy(OrdersTable.createdAt, SortOrder.DESC).toList()
            OrderReportResponse(
                period = "$startDate to $endDate",
                totalOrders = rows.size,
                totalValue = rows.sumOf { it[OrdersTable.subtotal] },
                paidValue = rows.filter { it[OrdersTable.paymentStatus] == "PAID" }.sumOf { it[OrdersTable.subtotal] },
                byPaymentMethod = rows.toBreakdown(OrdersTable.paymentMethod, OrdersTable.subtotal),
                byChannel = rows.toBreakdown(OrdersTable.salesChannel, OrdersTable.subtotal),
                orders = rows.take(MAX_REPORT_ROWS).map {
                    OrderReportRow(
                        orderId = it[OrdersTable.id],
                        orderNumber = it[OrdersTable.orderNumber],
                        customerName = it[OrdersTable.customerName],
                        subtotal = it[OrdersTable.subtotal],
                        paymentStatus = it[OrdersTable.paymentStatus],
                        deliveryStatus = it[OrdersTable.deliveryStatus],
                        paymentMethod = it[OrdersTable.paymentMethod],
                        salesChannel = it[OrdersTable.salesChannel],
                        createdAt = it[OrdersTable.createdAt].toString()
                    )
                }
            )
        }
    }

    private fun reportRange(startDate: String, endDate: String): Pair<kotlinx.datetime.Instant, kotlinx.datetime.Instant> {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        require(end >= start) { "endDate must not be before startDate" }
        val zone = TimeZone.of("Africa/Nairobi")
        return start.atStartOfDayIn(zone) to end.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)
    }

    private fun List<ResultRow>.toBreakdown(label: Column<String>, amount: Column<Double>): List<ReportBreakdown> =
        groupBy { it[label] }
            .map { (value, rows) -> ReportBreakdown(value, rows.size, rows.sumOf { it[amount] }) }
            .sortedByDescending { it.amount }

    private companion object {
        const val MAX_REPORT_ROWS = 1_000
    }
}
