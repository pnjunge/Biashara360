package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class PurchaseLineItem(
    val productId: String,
    val productName: String,
    val sku: String = "",
    val quantity: Int,
    val unitCost: Double,
    val lineTotal: Double = quantity * unitCost
)

@Serializable
data class PurchaseInvoice(
    val id: String,
    val businessId: String,
    val invoiceNumber: String,
    val supplierName: String,
    val supplierPhone: String? = null,
    val invoiceDate: String,
    val totalAmount: Double,
    val paymentStatus: String = "PAID",
    val paymentMethod: String = "CASH",
    val notes: String = "",
    val items: List<PurchaseLineItem> = emptyList(),
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreatePurchaseInvoiceRequest(
    val invoiceNumber: String,
    val supplierName: String,
    val supplierPhone: String? = null,
    val invoiceDate: String? = null,
    val paymentStatus: String = "PAID",
    val paymentMethod: String = "CASH",
    val notes: String = "",
    val items: List<PurchaseLineItem>
)
