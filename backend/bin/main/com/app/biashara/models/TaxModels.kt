package com.app.biashara.models

import kotlinx.serialization.Serializable

// ─── Tax Rate Models ──────────────────────────────────────────────────────────

@Serializable
data class TaxRateRequest(
    val taxType: String,          // VAT | TOT | WHT | EXCISE | CUSTOM
    val name: String,
    val rate: Double,             // 0.16 = 16%
    val isInclusive: Boolean = false,
    val appliesTo: String = "ALL",
    val description: String = ""
)

@Serializable
data class TaxRateResponse(
    val id: String,
    val businessId: String,
    val taxType: String,
    val name: String,
    val rate: Double,
    val ratePercent: Double,      // rate * 100 for display
    val isInclusive: Boolean,
    val isActive: Boolean,
    val appliesTo: String,
    val description: String,
    val createdAt: String
)

// ─── Order Tax ────────────────────────────────────────────────────────────────

@Serializable
data class TaxLineResponse(
    val id: String,
    val taxRateId: String,
    val taxType: String,
    val taxName: String,
    val rate: Double,
    val ratePercent: Double,
    val taxableAmount: Double,
    val taxAmount: Double
)

// Sent by client when creating an order with taxes
@Serializable
data class OrderTaxRequest(
    val taxRateId: String,
    val taxableAmount: Double
)

// ─── Tax Remittance Models ────────────────────────────────────────────────────

@Serializable
data class TaxRemittanceRequest(
    val taxType: String,
    val periodStart: String,   // ISO date: "2025-03-01"
    val periodEnd: String,     // ISO date: "2025-03-31"
    val notes: String = ""
)

@Serializable
data class TaxRemittanceResponse(
    val id: String,
    val businessId: String,
    val taxType: String,
    val periodStart: String,
    val periodEnd: String,
    val taxableAmount: Double,
    val taxAmount: Double,
    val status: String,
    val receiptNumber: String?,
    val filedAt: String?,
    val paidAt: String?,
    val notes: String,
    val createdAt: String
)

@Serializable
data class UpdateRemittanceStatusRequest(
    val status: String,           // FILED | PAID
    val receiptNumber: String? = null
)

// ─── Tax Summary / Report ─────────────────────────────────────────────────────

@Serializable
data class TaxSummaryResponse(
    val period: String,
    val vatCollected: Double,
    val vatPayable: Double,       // VAT on sales
    val vatOnPurchases: Double,   // Input VAT (claimable)
    val netVat: Double,           // vatPayable - vatOnPurchases
    val totAmount: Double,        // Turnover Tax (1.5% of gross turnover)
    val whtAmount: Double,        // Withholding Tax
    val exciseAmount: Double,
    val customAmount: Double,
    val totalTaxLiability: Double,
    val totalTaxableRevenue: Double,
    val effectiveTaxRate: Double,
    val filedRemittances: Int,
    val pendingRemittances: Int
)

// ─── Tax Calculation (utility, not stored) ───────────────────────────────────

@Serializable
data class TaxCalculationRequest(
    val amount: Double,
    val taxRateIds: List<String>
)

@Serializable
data class TaxCalculationResponse(
    val subtotal: Double,
    val taxLines: List<TaxLineCalculation>,
    val totalTax: Double,
    val grandTotal: Double
)

@Serializable
data class TaxLineCalculation(
    val taxRateId: String,
    val taxName: String,
    val taxType: String,
    val rate: Double,
    val taxAmount: Double
)

// ─── Kenya-specific defaults (used to seed new businesses) ───────────────────
object KenyaTaxDefaults {
    val defaults = listOf(
        Triple("VAT",    "Value Added Tax (VAT)",            0.16),
        Triple("TOT",    "Turnover Tax (TOT)",               0.015),
        Triple("WHT",    "Withholding Tax (WHT)",            0.03),
        Triple("EXCISE", "Excise Duty (Alcohol/Tobacco)",    0.20)
    )
}
