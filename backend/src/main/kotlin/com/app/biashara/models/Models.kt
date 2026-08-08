package com.app.biashara.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

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
data class PinLoginRequest(val email: String, val pin: String)

@Serializable
data class SetLoginPinRequest(
    val currentPassword: String,
    val pin: String? = null,
    val disable: Boolean = false
)

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
data class ResendOtpRequest(val userId: String, val channel: String = "SMS")

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
    val preferredLanguage: String,
    val businessName: String? = null,
    val isActive: Boolean = true,
    val hasPinSet: Boolean = false,
    val assignedGroups: List<String> = emptyList()
)

@Serializable
data class AdminSetStaffPinRequest(val pin: String)

@Serializable
data class AuditLogResponse(
    val id: String,
    val businessId: String?,
    val actorUserId: String?,
    val actorName: String? = null,
    val targetUserId: String?,
    val targetName: String? = null,
    val action: String,
    val ipAddress: String?,
    val details: String?,
    val createdAt: String
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
    val imageUrl: String? = null,
    val barcode: String? = null,
    val expectedUpdatedAt: String? = null
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
    val barcode: String? = null,
    val imageUrl: String?,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class UpdateProductStatusRequest(val isActive: Boolean)

@Serializable
data class InventoryCategoryResponse(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val productCount: Int,
    val imageUrl: String? = null
)

@Serializable
data class CreateInventoryCategoryRequest(val name: String, val imageUrl: String? = null)

@Serializable
data class UpdateInventoryCategoryRequest(
    val name: String? = null,
    val isActive: Boolean? = null,
    val imageUrl: String? = null
)

@Serializable(with = StockUpdateRequestSerializer::class)
data class StockUpdateRequest(
    val type: String = "STOCK_IN",   // STOCK_IN, STOCK_OUT, ADJUSTMENT
    val quantity: Int = 0,
    val note: String? = ""
)

@Serializable
private data class StockUpdateRequestSurrogate(
    val type: String = "STOCK_IN",
    val quantity: Int = 0,
    val note: String? = ""
)

object StockUpdateRequestSerializer : KSerializer<StockUpdateRequest> {
    private val surrogateSerializer = StockUpdateRequestSurrogate.serializer()
    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: StockUpdateRequest) {
        encoder.encodeSerializableValue(
            surrogateSerializer,
            StockUpdateRequestSurrogate(value.type, value.quantity, value.note)
        )
    }

    override fun deserialize(decoder: Decoder): StockUpdateRequest {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val jsonElement = jsonDecoder.decodeJsonElement()
            if (jsonElement is JsonObject) {
                val typeStr = jsonElement["type"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "STOCK_IN"

                val rawQuantityElement = jsonElement["quantity"] ?: jsonElement["quantityToAdd"]
                val qtyInt = when (rawQuantityElement) {
                    is JsonPrimitive -> {
                        rawQuantityElement.intOrNull
                            ?: rawQuantityElement.doubleOrNull?.toInt()
                            ?: rawQuantityElement.contentOrNull?.toDoubleOrNull()?.toInt()
                            ?: rawQuantityElement.contentOrNull?.toIntOrNull()
                            ?: 0
                    }
                    else -> 0
                }

                val noteStr = jsonElement["note"]?.jsonPrimitive?.contentOrNull ?: ""

                return StockUpdateRequest(
                    type = typeStr,
                    quantity = qtyInt,
                    note = noteStr
                )
            }
        }

        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return StockUpdateRequest(surrogate.type, surrogate.quantity, surrogate.note)
    }
}

// ─── Orders ───────────────────────────────────────────────────────────────────

@Serializable
data class CreateOrderRequest(
    val clientReference: String? = null,
    val customerId: String? = null,
    val customerName: String,
    val customerPhone: String? = null,
    val deliveryLocation: String = "",
    val items: List<OrderItemRequest>,
    val paymentMethod: String = "MPESA",
    val paymentStatus: String? = null,
    val deliveryStatus: String? = null,
    val notes: String = "",
    val serviceType: String = "RETAIL",
    val hospitalityTableId: String? = null,
    val serverUserId: String? = null,
    val guestCount: Int = 1,
    val tabStatus: String = "CLOSED",
    val includeTax: Boolean = false,
    val taxRate: Double = 0.16
)

@Serializable
data class OrderItemRequest(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double,
    val modifiers: List<MenuOption> = emptyList(),
    val itemNote: String = "",
    val discountAmount: Double = 0.0,
    val complimentary: Boolean = false
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
    val salesChannel: String = "WEB",
    val serviceType: String = "RETAIL",
    val hospitalityTableId: String? = null,
    val serverUserId: String? = null,
    val guestCount: Int = 1,
    val tabStatus: String = "CLOSED",
    val mpesaTransactionCode: String?,
    val baseAmount: Double = 0.0,
    val taxIncluded: Boolean = false,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
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
    val lineProfit: Double,
    val modifiers: List<MenuOption> = emptyList(),
    val itemNote: String = "",
    val discountAmount: Double = 0.0,
    val complimentary: Boolean = false
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
    val createdAt: String,
    val updatedAt: String
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
    val phoneNumber: String,
    val accountType: String? = null
)

@Serializable
data class MpesaTransactionQueryRequest(val transactionId: String)

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

// Safaricom sends Value as a JSON number (not string) for Amount, PhoneNumber, etc.
// This serializer coerces any JSON primitive into a Kotlin String.
object AnyToStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AnyToString", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String?) { encoder.encodeString(value ?: "") }
    override fun deserialize(decoder: Decoder): String? {
        val json = (decoder as? JsonDecoder)?.decodeJsonElement() ?: return decoder.decodeString()
        return when (json) {
            is JsonNull    -> null
            is JsonPrimitive -> json.content   // works for both quoted strings and bare numbers
            else           -> json.toString()
        }
    }
}

@Serializable
data class MpesaCallbackItem(
    val Name: String,
    @Serializable(with = AnyToStringSerializer::class)
    val Value: String? = null
)

@Serializable
data class ReconcileRequest(val orderId: String)

/** Typed acknowledgement sent back to Safaricom after processing a callback. */
@Serializable
data class DarajaAck(val ResultCode: Int = 0, val ResultDesc: String = "Accepted")

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
    val accountNumber: String = "",
    val receiptHeader: String = "Welcome to our store!",
    val receiptFooter: String = "Thank you for shopping with us!",
    val receiptLogo: String? = null,
    val receiptShowTax: Boolean = true,
    val receiptShowCustomer: Boolean = true,
    val storefrontThemeColor: String = "#0F766E",
    val storefrontHeadline: String = "Shop with us online",
    val storefrontDescription: String = "",
    val storefrontBannerUrl: String? = null,
    val storefrontLayout: String = "GRID",
    val dayStartTime: String = "06:00",
    val dayCloseTime: String = "23:00"
)

@Serializable
data class BusinessProfileResponse(
    val id: String,
    val storefrontSlug: String,
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
    val subscriptionTier: String,
    val subscriptionEnabled: Boolean,
    val hospitalityEnabled: Boolean = false,
    val receiptHeader: String = "Welcome to our store!",
    val receiptFooter: String = "Thank you for shopping with us!",
    val receiptLogo: String? = null,
    val receiptShowTax: Boolean = true,
    val receiptShowCustomer: Boolean = true,
    val storefrontThemeColor: String = "#0F766E",
    val storefrontHeadline: String = "Shop with us online",
    val storefrontDescription: String = "",
    val storefrontBannerUrl: String? = null,
    val storefrontLayout: String = "GRID",
    val dayStartTime: String = "06:00",
    val dayCloseTime: String = "23:00"
)

// ─── Super Admin — Business Management ───────────────────────────────────────

/** Create a business without an admin user — admin is added later via Users menu */
@Serializable
data class CreateBusinessOnlyRequest(
    val businessName: String,
    val businessType: String
)

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
    val subscriptionEnabled: Boolean,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class BusinessWithAdminResponse(
    val business: BusinessResponse,
    val admin: UserResponse
)

@Serializable
data class LinkUserToBusinessRequest(
    val businessId: String,
    val role: String? = null
)

@Serializable
data class UpdateBusinessStatusRequest(val isActive: Boolean)

@Serializable
data class UpdateSubscriptionRequest(
    val enabled: Boolean,
    val tier: String? = null
)

// ─── System Settings ─────────────────────────────────────────────────────────

@Serializable
data class SystemSettingRequest(val value: String)

@Serializable
data class SystemSettingResponse(val key: String, val value: String)

@Serializable
data class SessionTimeoutConfigRequest(
    val webTimeoutSeconds: Long,
    val androidTimeoutSeconds: Long,
    val desktopTimeoutSeconds: Long
)

@Serializable
data class SessionTimeoutConfigResponse(
    val businessId: String,
    val webTimeoutSeconds: Long,
    val androidTimeoutSeconds: Long,
    val desktopTimeoutSeconds: Long,
    val updatedAt: String? = null
)

// ─── Business Settings — Mpesa ────────────────────────────────────────────────

@Serializable
data class MpesaConfigRequest(
    val shortCode: String,
    val callbackUrl: String = "",
    val passKey: String? = null,
    val environment: String = "sandbox",
    val accountType: String = "paybill"   // paybill | till
)

@Serializable
data class MpesaConfigResponse(
    val businessId: String,
    val shortCode: String,
    val callbackUrl: String,
    val environment: String,
    val accountType: String,
    val passkeyConfigured: Boolean,
    val updatedAt: String
)

// ─── Business Settings — CyberSource ──────────────────────────────────────────

@Serializable
data class CyberSourceConfigRequest(
    val merchantId: String,
    val merchantKeyId: String,
    val merchantSecretKey: String? = null,
    val profileId: String? = null,
    val accessKey: String? = null,
    val environment: String = "sandbox"
)

@Serializable
data class CyberSourceConfigResponse(
    val businessId: String,
    val merchantId: String,
    val merchantKeyId: String,
    val profileId: String = "",
    val accessKey: String = "",
    val environment: String,
    val secretConfigured: Boolean,
    val updatedAt: String
)

// ─── Secure Acceptance Hosted Checkout ────────────────────────────────────────

@Serializable
data class SaInitiateRequest(
    val businessId    : String,
    val orderId       : String,
    val amount        : Double,
    val customerName  : String? = null,
    val customerEmail : String? = null,
    val customerPhone : String? = null,
    val returnStoreSlug: String? = null
)

@Serializable
data class SaInitiateResponse(
    val actionUrl : String,
    val fields    : Map<String, String>   // All signed hidden form fields to auto-submit
)

@Serializable
data class CsPaymentLinkRequest(
    val orderId: String,
    val amount: Double,
    val description: String = "",
    val customerName: String? = null,
    val customerEmail: String? = null,
    val customerPhone: String? = null,
    val expiryHours: Int = 24
)

@Serializable
data class CsPaymentLinkResponse(
    val linkUrl: String,
    val orderId: String,
    val amount: Double,
    val clientReference: String,
    val expiresAt: String
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
