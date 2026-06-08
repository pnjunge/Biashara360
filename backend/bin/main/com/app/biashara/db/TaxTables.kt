package com.app.biashara.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.kotlin.datetime.date

// ─── Tax Rates ────────────────────────────────────────────────────────────────
// Stores configurable tax rates per business. Supports Kenya: VAT, TOT, WHT, Excise
object TaxRatesTable : Table("tax_rates") {
    val id           = varchar("id", 36)
    val businessId   = varchar("business_id", 36).references(BusinessesTable.id)
    val taxType      = varchar("tax_type", 30)     // VAT, TOT, WHT, EXCISE, CUSTOM
    val name         = varchar("name", 100)          // e.g. "Value Added Tax"
    val rate         = double("rate")                // e.g. 0.16 for 16%
    val isInclusive  = bool("is_inclusive").default(false)  // price includes tax?
    val isActive     = bool("is_active").default(true)
    val appliesTo    = varchar("applies_to", 50).default("ALL") // ALL, PRODUCTS, SERVICES
    val description  = text("description").default("")
    val createdAt    = timestamp("created_at")
    val updatedAt    = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

// ─── Order Tax Lines ──────────────────────────────────────────────────────────
// Each tax applied to an order is recorded as a separate line
object OrderTaxLinesTable : Table("order_tax_lines") {
    val id          = varchar("id", 36)
    val orderId     = varchar("order_id", 36).references(OrdersTable.id)
    val taxRateId   = varchar("tax_rate_id", 36).references(TaxRatesTable.id)
    val taxType     = varchar("tax_type", 30)
    val taxName     = varchar("tax_name", 100)
    val rate        = double("rate")
    val taxableAmount = double("taxable_amount")
    val taxAmount   = double("tax_amount")
    override val primaryKey = PrimaryKey(id)
}

// ─── Tax Remittances ──────────────────────────────────────────────────────────
// Track filed/paid tax returns (VAT returns, TOT returns)
object TaxRemittancesTable : Table("tax_remittances") {
    val id            = varchar("id", 36)
    val businessId    = varchar("business_id", 36).references(BusinessesTable.id)
    val taxType       = varchar("tax_type", 30)
    val periodStart   = date("period_start")
    val periodEnd     = date("period_end")
    val taxableAmount = double("taxable_amount")
    val taxAmount     = double("tax_amount")
    val status        = varchar("status", 20).default("PENDING") // PENDING, FILED, PAID
    val receiptNumber = varchar("receipt_number", 100).nullable()
    val filedAt       = timestamp("filed_at").nullable()
    val paidAt        = timestamp("paid_at").nullable()
    val notes         = text("notes").default("")
    val createdAt     = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
