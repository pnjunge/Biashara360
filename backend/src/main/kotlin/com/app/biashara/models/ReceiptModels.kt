package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class PublicBusinessProfile(
    val id: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val county: String = "",
    val kraPin: String = "",
    val receiptHeader: String = "Welcome to our store!",
    val receiptFooter: String = "Thank you for shopping with us!",
    val receiptLogo: String? = null,
    val receiptLogoWidthMm: Int = 42,
    val receiptLogoHeightMm: Int = 20,
    val receiptShowTax: Boolean = true,
    val receiptShowCustomer: Boolean = true
)

@Serializable
data class PublicBranchInfo(
    val id: String,
    val name: String,
    val code: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val receiptHeader: String? = null,
    val receiptFooter: String? = null
)

@Serializable
data class PublicKraFiscal(
    val invoiceNumber: String? = null,
    val qrCodeContent: String? = null,
    val qrCodeBase64: String? = null,
    val sdcId: String? = null,
    val rcptSign: String? = null
)

@Serializable
data class PublicReceiptResponse(
    val order: OrderResponse,
    val business: PublicBusinessProfile,
    val branch: PublicBranchInfo? = null,
    val receiptUrl: String,
    val kraFiscal: PublicKraFiscal? = null
)

@Serializable
data class SendEReceiptRequest(
    val channel: String, // "SMS" | "EMAIL" | "WHATSAPP"
    val recipient: String? = null
)

@Serializable
data class SendEReceiptResponse(
    val channel: String,
    val recipient: String,
    val status: String,
    val whatsappUrl: String? = null,
    val messageText: String? = null
)
