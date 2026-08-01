package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class ReportBreakdown(val label: String, val count: Int, val amount: Double)

@Serializable
data class PaymentReportRow(
    val transactionCode: String,
    val orderId: String? = null,
    val amount: Double,
    val payerName: String,
    val payerPhone: String,
    val method: String,
    val channel: String,
    val status: String,
    val reconciled: Boolean,
    val transactionDate: String
)

@Serializable
data class PaymentReportResponse(
    val period: String,
    val totalTransactions: Int,
    val totalAmount: Double,
    val reconciledAmount: Double,
    val byMethod: List<ReportBreakdown>,
    val byChannel: List<ReportBreakdown>,
    val payments: List<PaymentReportRow>
)

@Serializable
data class OrderReportRow(
    val orderId: String,
    val orderNumber: String,
    val customerName: String,
    val subtotal: Double,
    val paymentStatus: String,
    val deliveryStatus: String,
    val serviceType: String = "RETAIL",
    val tabStatus: String = "CLOSED",
    val paymentMethod: String,
    val salesChannel: String,
    val createdAt: String
)

@Serializable
data class OrderReportResponse(
    val period: String,
    val totalOrders: Int,
    val totalValue: Double,
    val paidValue: Double,
    val byPaymentMethod: List<ReportBreakdown>,
    val byChannel: List<ReportBreakdown>,
    val orders: List<OrderReportRow>
)
