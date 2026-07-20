package com.app.biashara.constants

/**
 * Application-wide enumerations for type-safe status management.
 */

// ──── User & Authentication ──────────────────────────────────────────────────

enum class UserRole(val value: String) {
    SUPER_ADMIN("SUPER_ADMIN"),
    ADMIN("ADMIN"),
    MANAGER("MANAGER"),
    STAFF("STAFF"),
    CUSTOMER("CUSTOMER");
    
    companion object {
        fun from(value: String): UserRole? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
        fun adminRoles() = listOf(SUPER_ADMIN, ADMIN)
        fun staffRoles() = listOf(ADMIN, MANAGER, STAFF)
    }
}

enum class OtpChannel(val value: String) {
    SMS("SMS"),
    EMAIL("EMAIL"),
    WHATSAPP("WHATSAPP");
    
    companion object {
        fun from(value: String): OtpChannel? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_VERIFICATION;
    
    fun canLogin(): Boolean = this == ACTIVE
}

// ──── Payment ────────────────────────────────────────────────────────────────

enum class PaymentStatus(val value: String) {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
    REFUNDED("REFUNDED");
    
    companion object {
        fun from(value: String): PaymentStatus? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
        fun finalStatuses() = listOf(COMPLETED, FAILED, CANCELLED, REFUNDED)
    }
    
    fun isFinal(): Boolean = this in finalStatuses()
    fun canTransitionTo(target: PaymentStatus): Boolean {
        return when (this) {
            PENDING -> target in listOf(PROCESSING, FAILED, CANCELLED)
            PROCESSING -> target in listOf(COMPLETED, FAILED)
            COMPLETED -> target == REFUNDED
            FAILED, CANCELLED, REFUNDED -> false
        }
    }
}

enum class PaymentMethod(val value: String) {
    CASH("CASH"),
    MPESA("MPESA"),
    CARD("CARD"),
    BANK_TRANSFER("BANK_TRANSFER"),
    CREDIT("CREDIT");
    
    companion object {
        fun from(value: String): PaymentMethod? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
        fun digitalMethods() = listOf(MPESA, CARD, BANK_TRANSFER)
    }
    
    fun isDigital(): Boolean = this in digitalMethods()
}

// ──── Order ──────────────────────────────────────────────────────────────────

enum class OrderStatus(val value: String) {
    DRAFT("DRAFT"),
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    PROCESSING("PROCESSING"),
    READY("READY"),
    SHIPPED("SHIPPED"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED"),
    RETURNED("RETURNED");
    
    companion object {
        fun from(value: String): OrderStatus? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
        fun activeStatuses() = listOf(PENDING, CONFIRMED, PROCESSING, READY, SHIPPED)
        fun finalStatuses() = listOf(DELIVERED, CANCELLED, RETURNED)
    }
    
    fun isActive(): Boolean = this in activeStatuses()
    fun isFinal(): Boolean = this in finalStatuses()
    
    fun canTransitionTo(target: OrderStatus): Boolean {
        return when (this) {
            DRAFT -> target in listOf(PENDING, CANCELLED)
            PENDING -> target in listOf(CONFIRMED, CANCELLED)
            CONFIRMED -> target in listOf(PROCESSING, CANCELLED)
            PROCESSING -> target in listOf(READY, CANCELLED)
            READY -> target in listOf(SHIPPED, CANCELLED)
            SHIPPED -> target in listOf(DELIVERED, RETURNED)
            DELIVERED -> target == RETURNED
            CANCELLED, RETURNED -> false
        }
    }
}

enum class DeliveryStatus(val value: String) {
    PENDING("PENDING"),
    PREPARING("PREPARING"),
    READY_FOR_PICKUP("READY_FOR_PICKUP"),
    IN_TRANSIT("IN_TRANSIT"),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY"),
    DELIVERED("DELIVERED"),
    FAILED("FAILED"),
    RETURNED("RETURNED");
    
    companion object {
        fun from(value: String): DeliveryStatus? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
    
    fun isFinal(): Boolean = this in listOf(DELIVERED, FAILED, RETURNED)
}

// ──── Inventory ──────────────────────────────────────────────────────────────

enum class StockTransactionType(val value: String) {
    STOCK_IN("STOCK_IN"),              // Purchase/restock
    STOCK_OUT("STOCK_OUT"),            // Sale/removal
    ADJUSTMENT("ADJUSTMENT"),          // Manual correction
    RETURN("RETURN"),                  // Customer return
    DAMAGE("DAMAGE"),                  // Damaged goods
    TRANSFER("TRANSFER");              // Transfer between locations
    
    companion object {
        fun from(value: String): StockTransactionType? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
    
    fun affectsStock(): Boolean = true
    fun increasesStock(): Boolean = this in listOf(STOCK_IN, RETURN)
    fun decreasesStock(): Boolean = this in listOf(STOCK_OUT, DAMAGE)
}

enum class ProductCategory(val value: String) {
    ELECTRONICS("ELECTRONICS"),
    CLOTHING("CLOTHING"),
    FOOD("FOOD"),
    BEVERAGES("BEVERAGES"),
    HOUSEHOLD("HOUSEHOLD"),
    BEAUTY("BEAUTY"),
    HEALTH("HEALTH"),
    BOOKS("BOOKS"),
    TOYS("TOYS"),
    SPORTS("SPORTS"),
    AUTOMOTIVE("AUTOMOTIVE"),
    OTHER("OTHER");
    
    companion object {
        fun from(value: String): ProductCategory? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

// ──── Business & Subscription ────────────────────────────────────────────────

enum class BusinessType(val value: String) {
    RETAIL("RETAIL"),
    WHOLESALE("WHOLESALE"),
    RESTAURANT("RESTAURANT"),
    ECOMMERCE("ECOMMERCE"),
    SERVICE("SERVICE"),
    MANUFACTURING("MANUFACTURING"),
    OTHER("OTHER");
    
    companion object {
        fun from(value: String): BusinessType? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

enum class SubscriptionTier(val value: String) {
    FREE("FREE"),
    BASIC("BASIC"),
    PREMIUM("PREMIUM"),
    ENTERPRISE("ENTERPRISE");
    
    companion object {
        fun from(value: String): SubscriptionTier? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
    
    fun allowsFeature(feature: String): Boolean {
        // Feature flag logic can be expanded
        return when (this) {
            FREE -> feature in listOf("basic_pos", "basic_inventory")
            BASIC -> feature in listOf("basic_pos", "basic_inventory", "reports", "mpesa")
            PREMIUM, ENTERPRISE -> true  // All features
        }
    }
}

// ──── Tax & KRA ──────────────────────────────────────────────────────────────

enum class TaxType(val value: String) {
    VAT("VAT"),
    TOT("TOT"),           // Tourism Turnover Tax
    WHT("WHT"),           // Withholding Tax
    EXCISE("EXCISE"),
    NONE("NONE");
    
    companion object {
        fun from(value: String): TaxType? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

enum class KraReturnStatus(val value: String) {
    DRAFT("DRAFT"),
    GENERATED("GENERATED"),
    SUBMITTED("SUBMITTED"),
    ACCEPTED("ACCEPTED"),
    REJECTED("REJECTED");
    
    companion object {
        fun from(value: String): KraReturnStatus? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

enum class EtimsInvoiceStatus(val value: String) {
    PENDING("PENDING"),
    TRANSMITTED("TRANSMITTED"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),
    CANCELLED("CANCELLED");
    
    companion object {
        fun from(value: String): EtimsInvoiceStatus? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

// ──── Social Commerce ────────────────────────────────────────────────────────

enum class SocialPlatform(val value: String) {
    WHATSAPP("WHATSAPP"),
    INSTAGRAM("INSTAGRAM"),
    FACEBOOK("FACEBOOK"),
    TIKTOK("TIKTOK"),
    TWITTER("TWITTER");
    
    companion object {
        fun from(value: String): SocialPlatform? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

enum class MessageStatus(val value: String) {
    PENDING("PENDING"),
    SENT("SENT"),
    DELIVERED("DELIVERED"),
    READ("READ"),
    FAILED("FAILED");
    
    companion object {
        fun from(value: String): MessageStatus? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

// ──── Expense ────────────────────────────────────────────────────────────────

enum class ExpenseCategory(val value: String) {
    RENT("RENT"),
    UTILITIES("UTILITIES"),
    SALARIES("SALARIES"),
    SUPPLIES("SUPPLIES"),
    MARKETING("MARKETING"),
    TRANSPORT("TRANSPORT"),
    EQUIPMENT("EQUIPMENT"),
    MAINTENANCE("MAINTENANCE"),
    INSURANCE("INSURANCE"),
    TAXES("TAXES"),
    OTHER("OTHER");
    
    companion object {
        fun from(value: String): ExpenseCategory? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

// ──── Notification ───────────────────────────────────────────────────────────

enum class NotificationType(val value: String) {
    ORDER_CREATED("ORDER_CREATED"),
    ORDER_PAID("ORDER_PAID"),
    LOW_STOCK("LOW_STOCK"),
    PAYMENT_RECEIVED("PAYMENT_RECEIVED"),
    PAYMENT_FAILED("PAYMENT_FAILED"),
    USER_INVITED("USER_INVITED"),
    SYSTEM_ALERT("SYSTEM_ALERT");
    
    companion object {
        fun from(value: String): NotificationType? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

// ──── Report ─────────────────────────────────────────────────────────────────

enum class ReportPeriod(val value: String) {
    TODAY("TODAY"),
    WEEK("WEEK"),
    MONTH("MONTH"),
    QUARTER("QUARTER"),
    YEAR("YEAR"),
    CUSTOM("CUSTOM");
    
    companion object {
        fun from(value: String): ReportPeriod? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}

enum class ReportFormat(val value: String) {
    JSON("JSON"),
    CSV("CSV"),
    PDF("PDF"),
    EXCEL("EXCEL");
    
    companion object {
        fun from(value: String): ReportFormat? = entries.find { it.value == value.uppercase() }
        fun isValid(value: String): Boolean = from(value) != null
    }
}
