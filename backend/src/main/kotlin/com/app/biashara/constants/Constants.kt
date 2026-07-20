package com.app.biashara.constants

/**
 * Application-wide constants and business rules.
 * Centralizes magic numbers and strings for maintainability.
 */
object Constants {
    
    // ──── Authentication & Security ──────────────────────────────────────────
    
    object Auth {
        const val JWT_ISSUER = "biashara360"
        const val JWT_AUDIENCE = "biashara360-api"
        const val JWT_REALM = "Biashara360 API"
        
        const val ACCESS_TOKEN_VALIDITY_HOURS = 24
        const val REFRESH_TOKEN_VALIDITY_DAYS = 30
        
        const val OTP_LENGTH = 6
        const val OTP_VALIDITY_MINUTES = 5
        const val MAX_OTP_ATTEMPTS = 3
        
        const val PASSWORD_MIN_LENGTH = 12
        const val PASSWORD_MAX_LENGTH = 128
        
        const val SESSION_TIMEOUT_MINUTES = 30
    }
    
    // ──── Business Rules ─────────────────────────────────────────────────────
    
    object Business {
        const val DEFAULT_CURRENCY = "KES"
        const val DEFAULT_COUNTRY_CODE = "254"
        const val DEFAULT_LANGUAGE = "en"
        
        const val MIN_ORDER_AMOUNT = 1.0
        const val MAX_ORDER_AMOUNT = 10_000_000.0
        
        const val DEFAULT_LOW_STOCK_THRESHOLD = 5
        const val MIN_STOCK_QUANTITY = 0
        const val MAX_STOCK_QUANTITY = 1_000_000
        
        const val MAX_ORDER_ITEMS = 100
        const val MAX_PRODUCT_NAME_LENGTH = 255
        const val MAX_DESCRIPTION_LENGTH = 1000
        
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
    }
    
    // ──── Subscription & Limits ──────────────────────────────────────────────
    
    object Subscription {
        const val FREE_TIER_MAX_PRODUCTS = 50
        const val FREE_TIER_MAX_ORDERS = 100
        const val FREE_TIER_MAX_USERS = 2
        
        const val BASIC_TIER_MAX_PRODUCTS = 500
        const val BASIC_TIER_MAX_ORDERS = 1000
        const val BASIC_TIER_MAX_USERS = 5
        
        const val PREMIUM_TIER_MAX_PRODUCTS = -1  // Unlimited
        const val PREMIUM_TIER_MAX_ORDERS = -1    // Unlimited
        const val PREMIUM_TIER_MAX_USERS = 50
    }
    
    // ──── Payment Providers ──────────────────────────────────────────────────
    
    object Mpesa {
        const val TIMEOUT_SECONDS = 30
        const val MAX_AMOUNT = 150_000.0  // Mpesa transaction limit
        const val MIN_AMOUNT = 1.0
        
        const val CALLBACK_TIMEOUT_SECONDS = 60
        const val MAX_RETRY_ATTEMPTS = 3
    }
    
    object CyberSource {
        const val TIMEOUT_SECONDS = 45
        const val FLEX_SDK_VERSION = "0.11"
        const val CAPTURE_CONTEXT_VALIDITY_MINUTES = 15
    }
    
    // ──── KRA / Tax ──────────────────────────────────────────────────────────
    
    object Kra {
        const val VAT_RATE_STANDARD = 16.0  // 16% VAT in Kenya
        const val PIN_LENGTH = 11           // Format: P051234567X
        
        const val ETIMS_TIMEOUT_SECONDS = 30
        const val ETIMS_RETRY_ATTEMPTS = 2
    }
    
    // ──── File & Upload ──────────────────────────────────────────────────────
    
    object Upload {
        const val MAX_FILE_SIZE_MB = 10
        const val MAX_IMAGE_SIZE_MB = 5
        
        val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/gif", "image/webp")
        val ALLOWED_DOCUMENT_TYPES = setOf("application/pdf", "application/msword")
    }
    
    // ──── Rate Limiting ──────────────────────────────────────────────────────
    
    object RateLimit {
        const val DEFAULT_REQUESTS_PER_MINUTE = 60
        const val AUTH_REQUESTS_PER_MINUTE = 10
        const val PAYMENT_REQUESTS_PER_MINUTE = 5
        
        const val WINDOW_SIZE_SECONDS = 60
        const val CLEANUP_INTERVAL_MINUTES = 10
    }
    
    // ──── Social Commerce ────────────────────────────────────────────────────
    
    object Social {
        const val AI_RESPONSE_TIMEOUT_SECONDS = 15
        const val MAX_MESSAGE_LENGTH = 4000
        const val WEBHOOK_VERIFY_TOKEN_LENGTH = 32
    }
    
    // ──── API Versioning ─────────────────────────────────────────────────────
    
    object Api {
        const val CURRENT_VERSION = "v1"
        const val BASE_PATH = "/v1"
        
        const val HEALTH_CHECK_PATH = "/health"
        const val DOCS_PATH = "/docs"
    }
    
    // ──── Validation Regex Patterns ──────────────────────────────────────────
    
    object Patterns {
        val EMAIL = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\$")
        val PHONE_KE = Regex("^(\\+?254|0)[1-9]\\d{8}\$")
        val UUID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        val KRA_PIN = Regex("[A-Z]\\d{9}[A-Z]")
        val ALPHANUMERIC = Regex("^[a-zA-Z0-9]+\$")
        val SKU = Regex("^[A-Za-z0-9_-]+\$")
    }
    
    // ──── Error Messages ─────────────────────────────────────────────────────
    
    object ErrorMessages {
        const val UNAUTHORIZED = "Authentication required"
        const val FORBIDDEN = "Access denied"
        const val NOT_FOUND = "Resource not found"
        const val VALIDATION_FAILED = "Validation failed"
        const val INTERNAL_ERROR = "An internal error occurred"
        
        const val INVALID_CREDENTIALS = "Invalid email or password"
        const val ACCOUNT_LOCKED = "Account is locked. Please contact support"
        const val EMAIL_ALREADY_EXISTS = "Email address already registered"
        const val PHONE_ALREADY_EXISTS = "Phone number already registered"
        
        const val INSUFFICIENT_STOCK = "Insufficient stock available"
        const val INVALID_AMOUNT = "Invalid amount specified"
        const val PAYMENT_FAILED = "Payment processing failed"
        const val ORDER_NOT_FOUND = "Order not found"
        const val PRODUCT_NOT_FOUND = "Product not found"
    }
}
