package com.app.biashara.models

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// KRA iTax / eTIMS Integration Models
//
// iTax integration uses three channels:
//   1. eTIMS API  — real-time invoice signing & transmission (mandatory from Jan 2024)
//   2. CSV/Excel upload — monthly VAT return (VAT3), TOT return, WHT return
//   3. SFTP upload  — bulk data submission for large businesses
// ─────────────────────────────────────────────────────────────────────────────

// ─── Business KRA Profile ────────────────────────────────────────────────────

@Serializable
data class KraProfileRequest(
    val pin: String,               // KRA PIN e.g. P051234567X
    val companyName: String,
    val vatNumber: String?,        // VAT registration number (same as PIN for most)
    val etimsSdcId: String?,       // eTIMS Software Developer Code (from KRA eTIMS portal)
    val etimsDeviceSerialNo: String?, // Assigned virtual device serial number
    val etimsEnvironment: String = "sandbox"  // sandbox | production
)

@Serializable
data class KraProfileResponse(
    val id: String,
    val businessId: String,
    val pin: String,
    val companyName: String,
    val vatNumber: String?,
    val etimsSdcId: String?,
    val etimsDeviceSerialNo: String?,
    val etimsEnvironment: String,
    val isVerified: Boolean,
    val lastSyncAt: String?,
    val createdAt: String
)

// ─── eTIMS Invoice Submission ─────────────────────────────────────────────────
// KRA eTIMS API spec: https://www.kra.go.ke/etims
// Used to sign and transmit every invoice in real-time

@Serializable
data class EtimsInvoiceRequest(
    val orderId: String,           // B360 order to transmit
    val invoiceNumber: String,     // Your internal invoice number
    val receiptType: String = "NS",// NS=Normal Sale, CR=Credit Note, CS=Copy of Receipt
    val paymentType: String = "01" // 01=Cash, 02=Credit, 03=Mpesa, 04=Card
)

@Serializable
data class EtimsInvoiceResponse(
    val internalId: String,        // Our DB id
    val orderId: String,
    val invoiceNumber: String,
    val etimsInvoiceNumber: String?, // KRA-assigned number e.g. "NS00000001"
    val qrCode: String?,            // Base64 QR code to print on receipt
    val qrCodeUrl: String?,         // URL encoded in the QR code
    val sdcId: String?,
    val sdcDateTime: String?,       // KRA timestamp
    val status: String,             // PENDING | TRANSMITTED | REJECTED | ERROR
    val errorMessage: String?,
    val submittedAt: String?,
    val createdAt: String
)

// ─── VAT3 Return ─────────────────────────────────────────────────────────────
// Monthly VAT return form filed via iTax CSV upload or online form
// Due: 20th of following month

@Serializable
data class Vat3ReturnRequest(
    val periodYear: Int,           // e.g. 2026
    val periodMonth: Int,          // 1–12
    val includeZeroRated: Boolean = true,
    val includeExempt: Boolean = true
)

@Serializable
data class Vat3ReturnResponse(
    val id: String,
    val businessId: String,
    val periodYear: Int,
    val periodMonth: Int,
    val periodLabel: String,       // e.g. "March 2026"
    val dueDate: String,           // e.g. "2026-04-20"
    // Sales breakdown
    val standardRatedSales: Double,     // 16% VAT sales
    val zeroRatedSales: Double,         // 0% VAT exports
    val exemptSales: Double,            // VAT exempt sales
    val totalSales: Double,
    // Output VAT
    val outputVat: Double,              // VAT charged to customers
    // Input VAT (purchases - would be from purchase invoices module)
    val inputVat: Double,               // VAT paid on purchases (claimable)
    val netVatPayable: Double,          // outputVat - inputVat
    // Filing status
    val status: String,                 // DRAFT | GENERATED | SUBMITTED | ACKNOWLEDGED
    val iTaxAcknowledgementNo: String?,
    val csvDownloadReady: Boolean,
    val generatedAt: String?,
    val submittedAt: String?
)

// ─── TOT Return ──────────────────────────────────────────────────────────────
// Monthly Turnover Tax return — 1.5% of gross receipts
// For businesses with KES 1M–5M annual turnover

@Serializable
data class TotReturnRequest(
    val periodYear: Int,
    val periodMonth: Int
)

@Serializable
data class TotReturnResponse(
    val id: String,
    val businessId: String,
    val periodYear: Int,
    val periodMonth: Int,
    val periodLabel: String,
    val dueDate: String,
    val grossReceipts: Double,
    val totAmount: Double,          // 1.5% of gross receipts
    val status: String,
    val iTaxAcknowledgementNo: String?,
    val csvDownloadReady: Boolean,
    val generatedAt: String?,
    val submittedAt: String?
)

// ─── WHT Return ──────────────────────────────────────────────────────────────
// Monthly Withholding Tax return — filed by the withholding agent

@Serializable
data class WhtReturnRequest(
    val periodYear: Int,
    val periodMonth: Int
)

@Serializable
data class WhtReturnResponse(
    val id: String,
    val businessId: String,
    val periodYear: Int,
    val periodMonth: Int,
    val periodLabel: String,
    val dueDate: String,
    val totalPayments: Double,      // Gross payments on which WHT deducted
    val whtAmount: Double,          // WHT deducted
    val status: String,
    val iTaxAcknowledgementNo: String?,
    val csvDownloadReady: Boolean,
    val generatedAt: String?,
    val submittedAt: String?
)

// ─── Return Submission Result ─────────────────────────────────────────────────

@Serializable
data class ReturnSubmissionResult(
    val returnId: String,
    val returnType: String,         // VAT3 | TOT | WHT
    val submissionChannel: String,  // MANUAL_UPLOAD | SFTP | DIRECT_API
    val status: String,             // SUBMITTED | FAILED | PENDING_UPLOAD
    val acknowledgementNo: String?,
    val message: String,
    val instructions: List<String>  // Step-by-step upload instructions
)

// ─── CSV Export ───────────────────────────────────────────────────────────────

@Serializable
data class CsvExportRequest(
    val returnType: String,     // VAT3 | TOT | WHT | ETIMS_INVOICES
    val periodYear: Int,
    val periodMonth: Int,
    val format: String = "KRA_CSV" // KRA_CSV | EXCEL | PDF
)

@Serializable
data class CsvExportResponse(
    val fileName: String,
    val format: String,
    val rowCount: Int,
    val periodLabel: String,
    val downloadBase64: String,  // base64-encoded file content
    val contentType: String,     // text/csv or application/vnd.ms-excel
    val uploadInstructions: KraUploadInstructions
)

@Serializable
data class KraUploadInstructions(
    val portalUrl: String,
    val menuPath: List<String>,     // Navigation path on iTax portal
    val fileFormatRequired: String,
    val steps: List<String>
)

// ─── eTIMS Device Registration ────────────────────────────────────────────────

@Serializable
data class EtimsDeviceInitRequest(
    val pin: String,
    val sdcId: String,
    val mrcNo: String,     // Machine Registration Certificate number
    val serialNo: String
)

@Serializable
data class EtimsDeviceInitResponse(
    val deviceSdcId: String,
    val serverDate: String,
    val standardCodeList: List<EtimsCodeItem>,
    val status: String
)

@Serializable
data class EtimsCodeItem(val cd: String, val cdNm: String, val cdDesc: String?)

// ─── KRA compliance checklist ─────────────────────────────────────────────────

@Serializable
data class KraComplianceStatus(
    val businessId: String,
    val pin: String?,
    val isEtimsRegistered: Boolean,
    val isVatRegistered: Boolean,
    val isTotRegistered: Boolean,
    val pendingReturns: List<PendingReturn>,
    val overdueReturns: List<PendingReturn>,
    val lastEtimsTransmission: String?,
    val etimsTransmissionRate: Double,    // % of invoices successfully transmitted
    val complianceScore: Int,             // 0–100
    val recommendations: List<String>
)

@Serializable
data class PendingReturn(
    val returnType: String,
    val period: String,
    val dueDate: String,
    val isOverdue: Boolean,
    val estimatedAmount: Double
)
