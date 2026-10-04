package com.app.biashara.db

import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.ReferenceOption.SET_NULL
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

// ─── Coupons & Promotion Rules ────────────────────────────────────────────────

object CouponCodesTable : Table("coupon_codes") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id, onDelete = CASCADE)
    val code = varchar("code", 32)
    val description = text("description").default("")
    val discountType = varchar("discount_type", 20).default("PERCENTAGE") // PERCENTAGE | FIXED_AMOUNT
    val discountValue = double("discount_value").default(0.0)
    val minimumOrderAmount = double("minimum_order_amount").default(0.0)
    val maxDiscountAmount = double("max_discount_amount").nullable()
    val usageLimit = integer("usage_limit").nullable()
    val usageCount = integer("usage_count").default(0)
    val startDate = timestamp("start_date").nullable()
    val endDate = timestamp("end_date").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
    val bizCodeIdx = index("uq_coupon_business_code", true, businessId, code)
}

object PromotionRulesTable : Table("promotion_rules") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id, onDelete = CASCADE)
    val name = varchar("name", 100)
    val ruleType = varchar("rule_type", 30) // BUY_X_GET_Y_FREE | TIERED_SPEND | BUNDLE_DEAL
    val triggerProductId = varchar("trigger_product_id", 36).references(ProductsTable.id, onDelete = CASCADE).nullable()
    val triggerQuantity = integer("trigger_quantity").default(1)
    val rewardProductId = varchar("reward_product_id", 36).references(ProductsTable.id, onDelete = SET_NULL).nullable()
    val rewardQuantity = integer("reward_quantity").default(1)
    val discountPercent = double("discount_percent").default(0.0)
    val minimumSpend = double("minimum_spend").default(0.0)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
    val bizRuleIdx = index("idx_promotion_rules_biz", false, businessId, isActive)
}

// ─── Product Unit of Measure (UOM) Conversions ────────────────────────────────

object ProductUomConversionsTable : Table("product_uom_conversions") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id, onDelete = CASCADE)
    val productId = varchar("product_id", 36).references(ProductsTable.id, onDelete = CASCADE)
    val unitName = varchar("unit_name", 50) // Carton, Box, Dozen, Bale, etc.
    val conversionFactor = double("conversion_factor").default(1.0) // 1 Carton = 24 PCS
    val sellingPrice = double("selling_price")
    val buyingPrice = double("buying_price").nullable()
    val barcode = varchar("barcode", 100).nullable()
    val isDefault = bool("is_default").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
    val prodIdx = index("idx_uom_conversions_product", false, productId)
    val bizIdx = index("idx_uom_conversions_biz", false, businessId)
}

// ─── Double-Entry General Ledger: Chart of Accounts & Journal Entries ─────────

object ChartOfAccountsTable : Table("chart_of_accounts") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id, onDelete = CASCADE)
    val accountCode = varchar("account_code", 20)
    val accountName = varchar("account_name", 100)
    val accountType = varchar("account_type", 20) // ASSET | LIABILITY | EQUITY | REVENUE | EXPENSE
    val normalBalance = varchar("normal_balance", 10).default("DEBIT") // DEBIT | CREDIT
    val currentBalance = double("current_balance").default(0.0)
    val isSystem = bool("is_system").default(false)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
    val bizAccCodeIdx = index("uq_chart_of_accounts_code", true, businessId, accountCode)
}

object JournalEntriesTable : Table("journal_entries") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id, onDelete = CASCADE)
    val entryNumber = varchar("entry_number", 30)
    val entryDate = timestamp("entry_date")
    val narration = text("narration")
    val sourceModule = varchar("source_module", 30).default("MANUAL") // MANUAL | POS | PURCHASES | EXPENSES | TAX
    val sourceReferenceId = varchar("source_reference_id", 64).nullable()
    val totalDebit = double("total_debit").default(0.0)
    val totalCredit = double("total_credit").default(0.0)
    val isBalanced = bool("is_balanced").default(true)
    val createdBy = varchar("created_by", 36).references(UsersTable.id, onDelete = SET_NULL).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
    val bizDateIdx = index("idx_journal_entries_biz_date", false, businessId, entryDate)
}

object JournalEntryLinesTable : Table("journal_entry_lines") {
    val id = varchar("id", 36)
    val entryId = varchar("entry_id", 36).references(JournalEntriesTable.id, onDelete = CASCADE)
    val accountId = varchar("account_id", 36).references(ChartOfAccountsTable.id)
    val description = varchar("description", 255).default("")
    val debit = double("debit").default(0.0)
    val credit = double("credit").default(0.0)
    override val primaryKey = PrimaryKey(id)
    val entryIdx = index("idx_journal_lines_entry", false, entryId)
    val accIdx = index("idx_journal_lines_account", false, accountId)
}

// ─── Super Admin: Platform Operating Expenses ────────────────────────────────

object PlatformExpensesTable : Table("platform_expenses") {
    val id = varchar("id", 36)
    val title = varchar("title", 150)
    val category = varchar("category", 50) // INFRASTRUCTURE | API_FEES | SMS_GATEWAY | DOMAIN_SSL | SUPPORT | GENERAL
    val amount = double("amount")
    val currency = varchar("currency", 10).default("KES")
    val vendor = varchar("vendor", 100).default("")
    val expenseDate = timestamp("expense_date")
    val notes = text("notes").default("")
    val createdBy = varchar("created_by", 36).references(UsersTable.id, onDelete = SET_NULL).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
    val dateIdx = index("idx_platform_expenses_date", false, expenseDate)
    val catIdx = index("idx_platform_expenses_cat", false, category)
}
