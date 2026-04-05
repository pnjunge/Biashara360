package com.app.biashara.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val id: String,
    val businessId: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val location: String = "",
    val notes: String = "",
    val loyaltyPoints: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class CustomerStats(
    val customerId: String,
    val totalOrders: Int,
    val totalSpent: Double,
    val averageOrderValue: Double,
    val lastOrderDate: Instant?,
    val isRepeatCustomer: Boolean get() = totalOrders > 1
)

@Serializable
data class CustomerMessage(
    val id: String,
    val businessId: String,
    val customerId: String,
    val channel: MessageChannel,
    val message: String,
    val sentAt: Instant,
    val status: MessageStatus
)

@Serializable
enum class MessageChannel {
    WHATSAPP, SMS
}

@Serializable
enum class MessageStatus {
    SENT, DELIVERED, FAILED, PENDING
}
