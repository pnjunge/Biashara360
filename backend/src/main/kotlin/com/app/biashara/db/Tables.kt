package com.app.biashara.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.ReferenceOption.SET_NULL

// ─── Businesses ───────────────────────────────────────────────────────────────

object BusinessesTable : Table("businesses") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val type = varchar("type", 50)
    val ownerName = varchar("owner_name", 255).nullable()
    val ownerPhone = varchar("owner_phone", 20)
    val ownerEmail = varchar("owner_email", 255)
    val county = varchar("county", 100).nullable()
    val address = varchar("address", 500).nullable()
    val kraPin = varchar("kra_pin", 20).nullable()
    val paybillNumber = varchar("paybill_number", 20).nullable()
    val accountNumber = varchar("account_number", 50).nullable()
    val mpesaShortCode = varchar("mpesa_short_code", 20).nullable()
    val currency = varchar("currency", 10).default("KES")
    val subscriptionTier = varchar("subscription_tier", 20).default("FREEMIUM")
    val enabledModules = text("enabled_modules").default("INVENTORY,SALES,CRM,EXPENSES,PAYMENTS,REPORTS")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Users ────────────────────────────────────────────────────────────────────

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id, CASCADE, SET_NULL).nullable()
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val phone = varchar("phone", 20).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 20).default("STAFF")
    val twoFactorEnabled = bool("two_factor_enabled").default(true)
    val preferredLanguage = varchar("preferred_language", 10).default("ENGLISH")
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── OTP ──────────────────────────────────────────────────────────────────────

object OtpTable : Table("otp_codes") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val code = varchar("code", 10)
    val channel = varchar("channel", 20)   // SMS, EMAIL, APP
    val used = bool("used").default(false)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Refresh Tokens ───────────────────────────────────────────────────────────

object RefreshTokensTable : Table("refresh_tokens") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val token = varchar("token", 512).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Products ─────────────────────────────────────────────────────────────────

object ProductsTable : Table("products") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val sku = varchar("sku", 100)
    val name = varchar("name", 255)
    val description = text("description").default("")
    val buyingPrice = double("buying_price")
    val sellingPrice = double("selling_price")
    val currentStock = integer("current_stock").default(0)
    val lowStockThreshold = integer("low_stock_threshold").default(5)
    val category = varchar("category", 100).default("")
    val imageUrl = varchar("image_url", 500).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object StockMovementsTable : Table("stock_movements") {
    val id = varchar("id", 36)
    val productId = varchar("product_id", 36).references(ProductsTable.id)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val type = varchar("type", 20)   // STOCK_IN, STOCK_OUT, ADJUSTMENT
    val quantity = integer("quantity")
    val note = text("note").default("")
    val orderId = varchar("order_id", 36).nullable()
    val recordedAt = timestamp("recorded_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Customers ────────────────────────────────────────────────────────────────

object CustomersTable : Table("customers") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val name = varchar("name", 255)
    val phone = varchar("phone", 20)
    val email = varchar("email", 255).nullable()
    val location = varchar("location", 255).default("")
    val notes = text("notes").default("")
    val loyaltyPoints = integer("loyalty_points").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Orders ───────────────────────────────────────────────────────────────────

object OrdersTable : Table("orders") {
    val id = varchar("id", 36)
    val orderNumber = varchar("order_number", 20).uniqueIndex()
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val customerId = varchar("customer_id", 36).nullable()
    val customerName = varchar("customer_name", 255)
    val customerPhone = varchar("customer_phone", 20)
    val deliveryLocation = text("delivery_location").default("")
    val paymentStatus = varchar("payment_status", 20).default("PENDING")
    val deliveryStatus = varchar("delivery_status", 20).default("PENDING")
    val paymentMethod = varchar("payment_method", 30).default("MPESA")
    val mpesaTransactionCode = varchar("mpesa_transaction_code", 50).nullable()
    val stkCheckoutRequestId = varchar("stk_checkout_request_id", 100).nullable()
    val notes = text("notes").default("")
    val subtotal = double("subtotal")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object OrderItemsTable : Table("order_items") {
    val id = varchar("id", 36)
    val orderId = varchar("order_id", 36).references(OrdersTable.id)
    val productId = varchar("product_id", 36)
    val productName = varchar("product_name", 255)
    val quantity = integer("quantity")
    val unitPrice = double("unit_price")
    val buyingPrice = double("buying_price")
    override val primaryKey = PrimaryKey(id)
}

// ─── Expenses ─────────────────────────────────────────────────────────────────

object ExpensesTable : Table("expenses") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val category = varchar("category", 50)
    val amount = double("amount")
    val description = text("description")
    val receiptUrl = varchar("receipt_url", 500).nullable()
    val expenseDate = date("expense_date")
    val recordedAt = timestamp("recorded_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Payments ─────────────────────────────────────────────────────────────────

object PaymentsTable : Table("payments") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val orderId = varchar("order_id", 36).nullable()
    val transactionCode = varchar("transaction_code", 50)
    val amount = double("amount")
    val payerPhone = varchar("payer_phone", 20)
    val payerName = varchar("payer_name", 255)
    val method = varchar("method", 30)
    val status = varchar("status", 20)
    val channel = varchar("channel", 50)
    val reconciled = bool("reconciled").default(false)
    val notes = text("notes").default("")
    val transactionDate = timestamp("transaction_date")
    override val primaryKey = PrimaryKey(id)
}

// ─── CyberSource Transactions ─────────────────────────────────────────────────
// Stores full CyberSource auth/capture lifecycle separate from generic payments

object CyberSourceTransactionsTable : Table("cybersource_transactions") {
    val id                  = varchar("id", 36)
    val businessId          = varchar("business_id", 36).references(BusinessesTable.id)
    val orderId             = varchar("order_id", 36).nullable()
    val paymentId           = varchar("payment_id", 36).nullable()   // FK to payments table

    // CyberSource IDs
    val csTransactionId     = varchar("cs_transaction_id", 100).nullable()  // CS `id` field
    val csReconciliationId  = varchar("cs_reconciliation_id", 100).nullable()
    val csApprovalCode      = varchar("cs_approval_code", 50).nullable()

    // Payment details
    val amount              = double("amount")
    val currency            = varchar("currency", 10).default("KES")
    val cardLast4           = varchar("card_last4", 4).nullable()
    val cardType            = varchar("card_type", 20).nullable()   // VISA, MASTERCARD, AMEX
    val cardholderName      = varchar("cardholder_name", 255).nullable()

    // Transaction lifecycle
    val transactionType     = varchar("transaction_type", 30)  // AUTHORIZATION, CAPTURE, REFUND, VOID
    val status              = varchar("status", 30)            // AUTHORIZED, DECLINED, CAPTURED, REFUNDED, VOIDED, ERROR
    val processorResponse   = varchar("processor_response", 10).nullable()
    val errorReason         = varchar("error_reason", 100).nullable()
    val errorMessage        = text("error_message").nullable()

    // Token (for saved cards)
    val customerTokenId     = varchar("customer_token_id", 255).nullable()

    // Client reference
    val clientReference     = varchar("client_reference", 100).nullable()

    val createdAt           = timestamp("created_at")
    val updatedAt           = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── CyberSource Customer Tokens (saved cards) ────────────────────────────────
object CsCustomerTokensTable : Table("cs_customer_tokens") {
    val id              = varchar("id", 36)
    val businessId      = varchar("business_id", 36).references(BusinessesTable.id)
    val customerId      = varchar("customer_id", 36).nullable()
    val csCustomerId    = varchar("cs_customer_id", 255)    // CyberSource TMS customer ID
    val cardLast4       = varchar("card_last4", 4)
    val cardType        = varchar("card_type", 20)
    val expiryMonth     = varchar("expiry_month", 2)
    val expiryYear      = varchar("expiry_year", 4)
    val holderName      = varchar("holder_name", 255)
    val isDefault       = bool("is_default").default(false)
    val createdAt       = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
