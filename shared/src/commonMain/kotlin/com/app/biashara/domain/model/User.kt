package com.app.biashara.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val phone: String,
    val name: String,
    val role: UserRole,
    val businessId: String,
    val createdAt: Instant,
    val twoFactorEnabled: Boolean = true,
    val preferredLanguage: Language = Language.ENGLISH
)

@Serializable
enum class UserRole {
    ADMIN, STAFF, VIEWER
}

@Serializable
enum class Language {
    ENGLISH, SWAHILI
}

@Serializable
data class Business(
    val id: String,
    val name: String,
    val type: BusinessType,
    val ownerPhone: String,
    val ownerEmail: String,
    val mpesaShortCode: String? = null,
    val currency: String = "KES",
    val createdAt: Instant,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREEMIUM,
    val enabledModules: List<ModuleType> = ModuleType.entries
)

@Serializable
enum class BusinessType {
    RETAIL,          // Clothing, accessories, home bakers
    SERVICE,         // Barbers, salons, tutors
    HYBRID,          // Bakeries with catering
    ONLINE_SELLER    // Instagram/WhatsApp sellers
}

@Serializable
enum class SubscriptionTier {
    FREEMIUM, PREMIUM
}

@Serializable
enum class ModuleType {
    INVENTORY, SALES, CRM, EXPENSES, PAYMENTS, REPORTS
}
