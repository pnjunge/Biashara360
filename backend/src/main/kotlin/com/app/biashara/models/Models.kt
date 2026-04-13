package com.app.biashara.models

import kotlinx.serialization.Serializable

// ─── Auth ─────────────────────────────────────────────────────────────────────

@Serializable
data class RegisterRequest(
    val name: String,
    val phone: String,
    val email: String,
    val password: String,
    val businessName: String,
    val businessType: String
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    val userId: String,
    val requiresOtp: Boolean,
    val otpChannels: List<String>,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val user: UserResponse? = null
)

@Serializable
data class OtpVerifyRequest(val userId: String, val otp: String, val channel: String)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val businessId: String?,
    val preferredLanguage: String
)

// ─── Products ─────────────────────────────────────────────────────────────────

@Serializable
data class ProductRequest(
    val sku: String,
    val name: String,
    val description: String = "",
    val buyingPrice: Double,
    val sellingPrice: Double,
    val currentStock: Int = 0,
    val lowStockThreshold: Int = 5,
    val category: String = "",
    val imageUrl: String? = null
)

@Serializable
data class ProductResponse(
    val id: String,
    val businessId: String,
    val sku: String,
    val name: String,
    val description: String,
    val buyingPrice: Double,
    val sellingPrice: Double,
    val profitPerItem: Double,
    val profitMargin: Double,
    val currentStock: Int,
    val lowStockThreshold: Int,
    val isLowStock: Boolean,
    val isOutOfStock: Boolean,
    val category: String,
    val imageUrl: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class StockUpdateRequest(
    val type: String,   // STOCK_IN, STOCK_OUT, ADJUSTMENT
    val quantity: Int,
    val note: String = ""
)

// ─── Orders ───────────────────────────────────────────────────────────────────

@Serializable
data class CreateOrderRequest(
    val customerId: String? = null,
    val customerName: String,
    val customerPhone: String,
    val deliveryLocation: String = "",
    val items: List<OrderItemRequest>,
    val paymentMethod: String = "MPESA",
    val notes: String = ""
)

@Serializable
data class OrderItemRequest(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double
)

@Serializable
data class OrderResponse(
    val id: String,
    val orderNumber: String,
    val businessId: String,
    val customerId: String?,
    val customerName: String,
    val customerPhone: String,
    val deliveryLocation: String,
    val items: List<OrderItemResponse>,
    val paymentStatus: String,
    val deliveryStatus: String,
    val paymentMethod: String,
    val mpesaTransactionCode: String?,
    val subtotal: Double,
    val notes: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class OrderItemResponse(
    val id: String,
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val buyingPrice: Double,
    val lineTotal: Double,
    val lineProfit: Double
)

@Serializable
data class UpdatePaymentStatusRequest(
    val status: String,
    val mpesaTransactionCode: String? = null
)

@Serializable
data class UpdateDeliveryStatusRequest(val status: String)

// ─── Customers ────────────────────────────────────────────────────────────────

@Serializable
data class CustomerRequest(
    val name: String,
    val phone: String,
    val email: String? = null,
    val location: String = "",
    val notes: String = ""
)

@Serializable
data class CustomerResponse(
    val id: String,
    val businessId: String,
    val name: String,
    val phone: String,
    val email: String?,
    val location: String,
    val notes: String,
    val loyaltyPoints: Int,
    val totalOrders: Int,
    val totalSpent: Double,
    val isRepeatCustomer: Boolean,
    val createdAt: String
)

// ─── Expenses ─────────────────────────────────────────────────────────────────

@Serializable
data class ExpenseRequest(
    val category: String,
    val amount: Double,
    val description: String,
    val expenseDate: String,    // ISO date: "2025-03-01"
    val receiptUrl: String? = null
)

@Serializable
data class ExpenseResponse(
    val id: String,
    val businessId: String,
    val category: String,
    val amount: Double,
    val description: String,
    val expenseDate: String,
    val receiptUrl: String?,
    val recordedAt: String
)

// ─── Payments / Mpesa ─────────────────────────────────────────────────────────

@Serializable
data class InitiatePaymentRequest(
    val orderId: String,
    val phoneNumber: String
)

@Serializable
data class StkPushResponse(
    val merchantRequestId: String,
    val checkoutRequestId: String,
    val responseCode: String,
    val responseDescription: String,
    val customerMessage: String
)

@Serializable
data class MpesaCallbackRequest(
    val Body: MpesaCallbackBody
)

@Serializable
data class MpesaCallbackBody(
    val stkCallback: MpesaStkCallback
)

@Serializable
data class MpesaStkCallback(
    val MerchantRequestID: String,
    val CheckoutRequestID: String,
    val ResultCode: Int,
    val ResultDesc: String,
    val CallbackMetadata: MpesaCallbackMetadata? = null
)

@Serializable
data class MpesaCallbackMetadata(
    val Item: List<MpesaCallbackItem>
)

@Serializable
data class MpesaCallbackItem(
    val Name: String,
    val Value: String? = null
)

@Serializable
data class ReconcileRequest(val orderId: String)

// ─── Dashboard / Reports ──────────────────────────────────────────────────────

@Serializable
data class DashboardResponse(
    val totalRevenueMonth: Double,
    val netProfitMonth: Double,
    val totalOrdersToday: Int,
    val pendingOrdersCount: Int,
    val lowStockCount: Int,
    val totalCustomers: Int,
    val unpaidOrdersCount: Int,
    val recentOrders: List<OrderResponse>,
    val lowStockProducts: List<ProductResponse>
)

@Serializable
data class ProfitSummaryResponse(
    val period: String,
    val totalRevenue: Double,
    val totalCostOfGoods: Double,
    val grossProfit: Double,
    val grossMargin: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val netMargin: Double,
    val cashflowIn: Double,
    val cashflowOut: Double
)

// ─── User Management ──────────────────────────────────────────────────────────

@Serializable
data class InviteUserRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String = "STAFF"   // ADMIN | STAFF
)

@Serializable
data class UpdateUserRoleRequest(val role: String)

@Serializable
data class UpdateUserStatusRequest(val isActive: Boolean)

// ─── Common ───────────────────────────────────────────────────────────────────

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String = "",
    val errors: List<String> = emptyList()
)

@Serializable
data class PagedResponse<T>(
    val data: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)

// ─── Business Profile ─────────────────────────────────────────────────────────

@Serializable
data class BusinessProfileRequest(
    val name: String,
    val owner: String = "",
    val phone: String,
    val email: String,
    val type: String,
    val county: String = "",
    val address: String = "",
    val kraPin: String = "",
    val paybillNumber: String = "",
    val accountNumber: String = ""
)

@Serializable
data class BusinessProfileResponse(
    val id: String,
    val name: String,
    val owner: String,
    val phone: String,
    val email: String,
    val type: String,
    val county: String,
    val address: String,
    val kraPin: String,
    val paybillNumber: String,
    val accountNumber: String,
    val subscriptionTier: String
)

// ─── Super Admin — Business Management ───────────────────────────────────────

@Serializable
data class CreateBusinessWithAdminRequest(
    val businessName: String,
    val businessType: String,
    val adminName: String,
    val adminEmail: String,
    val adminPhone: String,
    val adminPassword: String
)

@Serializable
data class BusinessResponse(
    val id: String,
    val name: String,
    val type: String,
    val ownerPhone: String,
    val ownerEmail: String,
    val subscriptionTier: String,
    val createdAt: String
)

@Serializable
data class BusinessWithAdminResponse(
    val business: BusinessResponse,
    val admin: UserResponse
)

// ─── System Settings ─────────────────────────────────────────────────────────

@Serializable
data class SystemSettingRequest(val value: String)

@Serializable
data class SystemSettingResponse(val key: String, val value: String)

// ─── Business Settings — Mpesa ────────────────────────────────────────────────

@Serializable
data class MpesaConfigRequest(
    val consumerKey: String,
    val consumerSecret: String,
    val shortCode: String,
    val passKey: String,
    val callbackUrl: String,
    val environment: String = "sandbox",
    val accountType: String = "paybill"   // paybill | till
)

@Serializable
data class MpesaConfigResponse(
    val businessId: String,
    val consumerKey: String,
    val shortCode: String,
    val callbackUrl: String,
    val environment: String,
    val accountType: String,
    val updatedAt: String
)

// ─── Business Settings — CyberSource ──────────────────────────────────────────

@Serializable
data class CyberSourceConfigRequest(
    val merchantId: String,
    val merchantKeyId: String,
    val merchantSecretKey: String,
    val environment: String = "sandbox"
)

@Serializable
data class CyberSourceConfigResponse(
    val businessId: String,
    val merchantId: String,
    val merchantKeyId: String,
    val environment: String,
    val updatedAt: String
)

// ─── CyberSource ──────────────────────────────────────────────────────────────

@Serializable
data class CsTransactionRecord(
    val id: String,
    val orderId: String?,
    val csTransactionId: String?,
    val amount: Double,
    val currency: String,
    val status: String,
    val transactionType: String,
    val cardLast4: String?,
    val cardType: String?,
    val cardholderName: String?,
    val approvalCode: String?,
    val reconciliationId: String?,
    val errorReason: String?,
    val createdAt: String
)

