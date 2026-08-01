package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class StorefrontResponse(
    val businessId: String,
    val businessName: String,
    val businessType: String,
    val county: String? = null,
    val address: String? = null,
    val currency: String,
    val welcomeMessage: String,
    val products: List<StorefrontProductResponse>
)

@Serializable
data class StorefrontProductResponse(
    val id: String,
    val sku: String,
    val name: String,
    val description: String,
    val sellingPrice: Double,
    val availableQuantity: Int,
    val category: String,
    val imageUrl: String? = null
)

@Serializable
data class StorefrontCheckoutItemRequest(val productId: String, val quantity: Int)

@Serializable
data class StorefrontCheckoutRequest(
    val clientReference: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryLocation: String,
    val items: List<StorefrontCheckoutItemRequest>,
    val notes: String = ""
)

@Serializable
data class StorefrontPaymentRetryRequest(
    val clientReference: String,
    val customerPhone: String
)

@Serializable
data class StorefrontCheckoutResponse(
    val orderId: String,
    val orderNumber: String,
    val clientReference: String,
    val amount: Double,
    val paymentStatus: String,
    val customerMessage: String? = null,
    val checkoutRequestId: String? = null
)

@Serializable
data class StorefrontOrderStatusResponse(
    val orderId: String,
    val orderNumber: String,
    val amount: Double,
    val paymentStatus: String,
    val deliveryStatus: String
)
