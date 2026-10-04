package com.app.biashara.models

import kotlinx.serialization.Serializable

// ─── Coupons & Promotion Models ───────────────────────────────────────────────

@Serializable
data class CreateCouponRequest(
    val code: String,
    val description: String = "",
    val discountType: String = "PERCENTAGE", // PERCENTAGE | FIXED_AMOUNT
    val discountValue: Double,
    val minimumOrderAmount: Double = 0.0,
    val maxDiscountAmount: Double? = null,
    val usageLimit: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

@Serializable
data class CouponResponse(
    val id: String,
    val businessId: String,
    val code: String,
    val description: String,
    val discountType: String,
    val discountValue: Double,
    val minimumOrderAmount: Double,
    val maxDiscountAmount: Double?,
    val usageLimit: Int?,
    val usageCount: Int,
    val startDate: String?,
    val endDate: String?,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class ValidateCouponRequest(
    val code: String,
    val orderAmount: Double
)

@Serializable
data class ValidateCouponResponse(
    val valid: Boolean,
    val code: String,
    val discountType: String = "PERCENTAGE",
    val discountValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val message: String
)

@Serializable
data class CreatePromotionRuleRequest(
    val name: String,
    val ruleType: String, // BUY_X_GET_Y_FREE | TIERED_SPEND | BUNDLE_DEAL
    val triggerProductId: String? = null,
    val triggerQuantity: Int = 1,
    val rewardProductId: String? = null,
    val rewardQuantity: Int = 1,
    val discountPercent: Double = 0.0,
    val minimumSpend: Double = 0.0
)

@Serializable
data class PromotionRuleResponse(
    val id: String,
    val businessId: String,
    val name: String,
    val ruleType: String,
    val triggerProductId: String?,
    val triggerProductName: String? = null,
    val triggerQuantity: Int,
    val rewardProductId: String?,
    val rewardProductName: String? = null,
    val rewardQuantity: Int,
    val discountPercent: Double,
    val minimumSpend: Double,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class CartItemEvaluation(
    val productId: String,
    val productName: String = "",
    val quantity: Int,
    val unitPrice: Double
)

@Serializable
data class EvaluateCartPromotionsRequest(
    val items: List<CartItemEvaluation>,
    val couponCode: String? = null
)

@Serializable
data class AppliedPromotionSummary(
    val ruleName: String,
    val ruleType: String,
    val discountAmount: Double,
    val rewardDescription: String
)

@Serializable
data class CartPromotionsResponse(
    val subtotal: Double,
    val couponDiscount: Double,
    val rulesDiscount: Double,
    val totalDiscount: Double,
    val finalTotal: Double,
    val appliedCoupon: ValidateCouponResponse? = null,
    val appliedRules: List<AppliedPromotionSummary> = emptyList()
)

// ─── Product UOM Models ───────────────────────────────────────────────────────

@Serializable
data class CreateProductUomRequest(
    val unitName: String,
    val conversionFactor: Double, // e.g. 24.0 (1 Carton = 24 PCS)
    val sellingPrice: Double,
    val buyingPrice: Double? = null,
    val barcode: String? = null,
    val isDefault: Boolean = false
)

@Serializable
data class ProductUomResponse(
    val id: String,
    val businessId: String,
    val productId: String,
    val unitName: String,
    val conversionFactor: Double,
    val sellingPrice: Double,
    val buyingPrice: Double?,
    val barcode: String?,
    val isDefault: Boolean,
    val createdAt: String
)

@Serializable
data class UpdateProductBaseUnitRequest(
    val baseUnit: String // e.g. "PCS", "KG", "LITRE", "METRE"
)

// ─── Custom Domain Models ─────────────────────────────────────────────────────

@Serializable
data class CustomDomainConfigResponse(
    val customDomain: String?,
    val status: String, // UNCONFIGURED | PENDING_DNS | ACTIVE | ERROR
    val cnameTarget: String,
    val verifiedAt: String?,
    val instructions: String,
    val standardStorefrontUrl: String
)

@Serializable
data class SetCustomDomainRequest(
    val domain: String
)

@Serializable
data class VerifyCustomDomainResponse(
    val domain: String,
    val status: String,
    val dnsResolved: Boolean,
    val message: String
)

@Serializable
data class ResolvedStorefrontDomainResponse(
    val found: Boolean,
    val businessId: String? = null,
    val storefrontSlug: String? = null,
    val businessName: String? = null
)

// ─── Double-Entry General Ledger Models ────────────────────────────────────────

@Serializable
data class AccountResponse(
    val id: String,
    val accountCode: String,
    val accountName: String,
    val accountType: String, // ASSET | LIABILITY | EQUITY | REVENUE | EXPENSE
    val normalBalance: String, // DEBIT | CREDIT
    val currentBalance: Double,
    val isSystem: Boolean,
    val isActive: Boolean
)

@Serializable
data class CreateAccountRequest(
    val accountCode: String,
    val accountName: String,
    val accountType: String,
    val normalBalance: String = "DEBIT"
)

@Serializable
data class JournalEntryLineRequest(
    val accountId: String,
    val description: String = "",
    val debit: Double = 0.0,
    val credit: Double = 0.0
)

@Serializable
data class CreateJournalEntryRequest(
    val narration: String,
    val sourceModule: String = "MANUAL",
    val sourceReferenceId: String? = null,
    val lines: List<JournalEntryLineRequest>
)

@Serializable
data class JournalEntryLineResponse(
    val id: String,
    val accountId: String,
    val accountCode: String,
    val accountName: String,
    val description: String,
    val debit: Double,
    val credit: Double
)

@Serializable
data class JournalEntryResponse(
    val id: String,
    val entryNumber: String,
    val entryDate: String,
    val narration: String,
    val sourceModule: String,
    val sourceReferenceId: String?,
    val totalDebit: Double,
    val totalCredit: Double,
    val isBalanced: Boolean,
    val lines: List<JournalEntryLineResponse>,
    val createdBy: String?,
    val createdAt: String
)

@Serializable
data class TrialBalanceAccountRow(
    val accountCode: String,
    val accountName: String,
    val accountType: String,
    val debit: Double,
    val credit: Double
)

@Serializable
data class TrialBalanceResponse(
    val asOfDate: String,
    val totalDebit: Double,
    val totalCredit: Double,
    val isBalanced: Boolean,
    val accounts: List<TrialBalanceAccountRow>
)

// ─── Super Admin: Platform Operating Expenses & Subscriptions ────────────────

@Serializable
data class PlatformExpenseResponse(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val vendor: String,
    val expenseDate: String,
    val notes: String,
    val createdBy: String?,
    val createdAt: String
)

@Serializable
data class CreatePlatformExpenseRequest(
    val title: String,
    val category: String, // INFRASTRUCTURE | API_FEES | SMS_GATEWAY | DOMAIN_SSL | SUPPORT | GENERAL
    val amount: Double,
    val currency: String = "KES",
    val vendor: String = "",
    val expenseDate: String? = null,
    val notes: String = ""
)

@Serializable
data class PlatformExpensesSummaryResponse(
    val totalAmount: Double,
    val currency: String = "KES",
    val byCategory: Map<String, Double>,
    val expenses: List<PlatformExpenseResponse>
)

@Serializable
data class SubscriptionRecordResponse(
    val businessId: String,
    val businessName: String,
    val businessType: String,
    val ownerEmail: String,
    val ownerPhone: String,
    val subscriptionTier: String,
    val isTrial: Boolean,
    val subscriptionEnabled: Boolean,
    val userCount: Int,
    val maxUsers: Int,
    val validUntil: String?,
    val daysRemaining: Long?,
    val isExpired: Boolean,
    val monthlyRevenue: Double,
    val createdAt: String
)

@Serializable
data class PlatformSubscriptionSummaryResponse(
    val totalTenants: Int,
    val activeSubscriptions: Int,
    val trialSubscriptions: Int,
    val expiredSubscriptions: Int,
    val totalEstimatedMRR: Double,
    val subscriptions: List<SubscriptionRecordResponse>
)
