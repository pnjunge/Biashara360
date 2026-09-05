package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class StorefrontResponse(
    val businessId: String,
    val storefrontSlug: String,
    val businessName: String,
    val businessType: String,
    val county: String? = null,
    val address: String? = null,
    val currency: String,
    val welcomeMessage: String,
    val themeColor: String = "#0F766E",
    val headline: String = "Shop with us online",
    val description: String = "",
    val bannerUrl: String? = null,
    val layout: String = "GRID",
    val tables: List<StorefrontTableResponse> = emptyList(),
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
    val paymentMethod: String = "MPESA",
    val items: List<StorefrontCheckoutItemRequest>,
    val notes: String = "",
    val tableId: String? = null,
    val guestCount: Int = 1
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
    val paymentMethod: String = "MPESA",
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

@Serializable
data class StorefrontTableResponse(val id: String, val name: String, val area: String)
