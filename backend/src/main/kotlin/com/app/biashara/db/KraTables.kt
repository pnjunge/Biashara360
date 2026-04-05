package com.app.biashara.db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ─── KRA Business Profile ─────────────────────────────────────────────────────
object KraProfilesTable : Table("kra_profiles") {
    val id                   = varchar("id", 36)
    val businessId           = varchar("business_id", 36).references(BusinessesTable.id).uniqueIndex()
    val pin                  = varchar("kra_pin", 20)         // e.g. P051234567X
    val companyName          = varchar("company_name", 255)
    val vatNumber            = varchar("vat_number", 20).nullable()
    val etimsSdcId           = varchar("etims_sdc_id", 100).nullable()
    val etimsDeviceSerialNo  = varchar("etims_device_serial_no", 100).nullable()
    val etimsEnvironment     = varchar("etims_environment", 20).default("sandbox")
    val isVerified           = bool("is_verified").default(false)
    val lastSyncAt           = timestamp("last_sync_at").nullable()
    val createdAt            = timestamp("created_at")
    val updatedAt            = timestamp("updated_at")
    override val primaryKey  = PrimaryKey(id)
}

// ─── eTIMS Invoice Log ────────────────────────────────────────────────────────
// Every order/invoice transmitted to KRA eTIMS is logged here
object EtimsInvoicesTable : Table("etims_invoices") {
    val id                   = varchar("id", 36)
    val businessId           = varchar("business_id", 36).references(BusinessesTable.id)
    val orderId              = varchar("order_id", 36).nullable()
    val invoiceNumber        = varchar("invoice_number", 50)  // our internal invoice no
    val etimsInvoiceNumber   = varchar("etims_invoice_number", 50).nullable()  // KRA assigned
    val receiptType          = varchar("receipt_type", 5).default("NS")   // NS, CR, CS
    val paymentType          = varchar("payment_type", 5).default("01")   // 01=Cash,03=Mpesa
    // Amounts
    val taxableAmount        = double("taxable_amount")
    val taxAmount            = double("tax_amount")
    val totalAmount          = double("total_amount")
    // KRA response fields
    val qrCodeContent        = text("qr_code_content").nullable()   // URL for QR code
    val qrCodeBase64         = text("qr_code_base64").nullable()    // base64 image
    val sdcId                = varchar("sdc_id", 100).nullable()
    val sdcDateTime          = varchar("sdc_date_time", 30).nullable()
    val intrlData            = text("intrl_data").nullable()        // KRA internal data
    val rcptSign             = text("rcpt_sign").nullable()         // Receipt signature
    // Status tracking
    val status               = varchar("status", 20).default("PENDING")  // PENDING|TRANSMITTED|REJECTED|ERROR
    val retryCount           = integer("retry_count").default(0)
    val errorMessage         = text("error_message").nullable()
    val rawResponse          = text("raw_response").nullable()
    val submittedAt          = timestamp("submitted_at").nullable()
    val createdAt            = timestamp("created_at")
    override val primaryKey  = PrimaryKey(id)
}

// ─── Tax Returns (VAT3 / TOT / WHT) ──────────────────────────────────────────
object TaxReturnsTable : Table("tax_returns") {
    val id                       = varchar("id", 36)
    val businessId               = varchar("business_id", 36).references(BusinessesTable.id)
    val returnType               = varchar("return_type", 10)  // VAT3 | TOT | WHT
    val periodYear               = integer("period_year")
    val periodMonth              = integer("period_month")
    // VAT3 specific
    val standardRatedSales       = double("standard_rated_sales").default(0.0)
    val zeroRatedSales           = double("zero_rated_sales").default(0.0)
    val exemptSales              = double("exempt_sales").default(0.0)
    val outputVat                = double("output_vat").default(0.0)
    val inputVat                 = double("input_vat").default(0.0)
    val netVatPayable            = double("net_vat_payable").default(0.0)
    // TOT / WHT specific
    val grossReceipts            = double("gross_receipts").default(0.0)
    val taxAmount                = double("tax_amount").default(0.0)
    // Filing metadata
    val status                   = varchar("status", 20).default("DRAFT")  // DRAFT|GENERATED|SUBMITTED|ACKNOWLEDGED
    val iTaxAcknowledgementNo    = varchar("itax_acknowledgement_no", 100).nullable()
    val csvFileName              = varchar("csv_file_name", 255).nullable()
    val submittedAt              = timestamp("submitted_at").nullable()
    val generatedAt              = timestamp("generated_at").nullable()
    val createdAt                = timestamp("created_at")
    override val primaryKey      = PrimaryKey(id)
    // Unique constraint: one return per type/period/business
    init { uniqueIndex(businessId, returnType, periodYear, periodMonth) }
}
