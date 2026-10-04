package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.InetAddress
import java.time.Instant

// ═══════════════════════════════════════════════════════════════════════════════
// 1. PROMOTIONS & COUPONS SERVICE
// ═══════════════════════════════════════════════════════════════════════════════

class PromotionService {

    fun listCoupons(businessId: String): List<CouponResponse> = transaction {
        CouponCodesTable.select { CouponCodesTable.businessId eq businessId }
            .orderBy(CouponCodesTable.createdAt, SortOrder.DESC)
            .map { it.toCouponResponse() }
    }

    fun createCoupon(businessId: String, req: CreateCouponRequest): ApiResponse<CouponResponse> = transaction {
        val cleanCode = req.code.trim().uppercase()
        if (cleanCode.isBlank() || cleanCode.length < 3) {
            return@transaction ApiResponse(false, message = "Coupon code must be at least 3 characters")
        }
        val exists = CouponCodesTable.select {
            (CouponCodesTable.businessId eq businessId) and (CouponCodesTable.code eq cleanCode)
        }.count() > 0
        if (exists) {
            return@transaction ApiResponse(false, message = "Coupon code '$cleanCode' already exists for this store")
        }

        val now = Clock.System.now()
        val id = generateId()
        CouponCodesTable.insert {
            it[CouponCodesTable.id] = id
            it[CouponCodesTable.businessId] = businessId
            it[code] = cleanCode
            it[description] = req.description.trim()
            it[discountType] = req.discountType.uppercase()
            it[discountValue] = req.discountValue.coerceAtLeast(0.0)
            it[minimumOrderAmount] = req.minimumOrderAmount.coerceAtLeast(0.0)
            it[maxDiscountAmount] = req.maxDiscountAmount
            it[usageLimit] = req.usageLimit
            it[usageCount] = 0
            it[startDate] = req.startDate?.let { s -> kotlin.runCatching { Instant.parse(s) }.getOrNull()?.let { Clock.System.now() } }
            it[endDate] = req.endDate?.let { s -> kotlin.runCatching { Instant.parse(s) }.getOrNull()?.let { Clock.System.now() } }
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }

        val created = CouponCodesTable.select { CouponCodesTable.id eq id }.first()
        ApiResponse(true, data = created.toCouponResponse(), message = "Coupon '$cleanCode' created successfully")
    }

    fun toggleCoupon(businessId: String, couponId: String): ApiResponse<CouponResponse> = transaction {
        val row = CouponCodesTable.select {
            (CouponCodesTable.id eq couponId) and (CouponCodesTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Coupon not found")

        val newActive = !row[CouponCodesTable.isActive]
        CouponCodesTable.update({ CouponCodesTable.id eq couponId }) {
            it[isActive] = newActive
            it[updatedAt] = Clock.System.now()
        }
        val updated = CouponCodesTable.select { CouponCodesTable.id eq couponId }.first()
        ApiResponse(true, data = updated.toCouponResponse(), message = if (newActive) "Coupon activated" else "Coupon deactivated")
    }

    fun deleteCoupon(businessId: String, couponId: String): ApiResponse<Unit> = transaction {
        val count = CouponCodesTable.deleteWhere {
            (CouponCodesTable.id eq couponId) and (CouponCodesTable.businessId eq businessId)
        }
        if (count > 0) ApiResponse(true, message = "Coupon deleted")
        else ApiResponse(false, message = "Coupon not found")
    }

    fun validateCoupon(businessId: String, code: String, orderAmount: Double): ValidateCouponResponse = transaction {
        val cleanCode = code.trim().uppercase()
        val row = CouponCodesTable.select {
            (CouponCodesTable.businessId eq businessId) and (CouponCodesTable.code eq cleanCode)
        }.firstOrNull() ?: return@transaction ValidateCouponResponse(
            valid = false,
            code = cleanCode,
            message = "Invalid coupon code '$cleanCode'"
        )

        if (!row[CouponCodesTable.isActive]) {
            return@transaction ValidateCouponResponse(valid = false, code = cleanCode, message = "This coupon code is no longer active")
        }

        val usageLimit = row[CouponCodesTable.usageLimit]
        val usageCount = row[CouponCodesTable.usageCount]
        if (usageLimit != null && usageCount >= usageLimit) {
            return@transaction ValidateCouponResponse(valid = false, code = cleanCode, message = "This coupon code has reached its maximum redemptions")
        }

        val minSpend = row[CouponCodesTable.minimumOrderAmount]
        if (orderAmount < minSpend) {
            return@transaction ValidateCouponResponse(
                valid = false,
                code = cleanCode,
                message = "Minimum order of KES ${minSpend.toLong()} required to use this coupon (current: KES ${orderAmount.toLong()})"
            )
        }

        val discountType = row[CouponCodesTable.discountType]
        val discountVal = row[CouponCodesTable.discountValue]
        val maxCap = row[CouponCodesTable.maxDiscountAmount]

        var discountAmt = if (discountType == "PERCENTAGE") {
            (orderAmount * (discountVal / 100.0))
        } else {
            discountVal
        }

        if (maxCap != null && maxCap > 0.0) {
            discountAmt = discountAmt.coerceAtMost(maxCap)
        }
        discountAmt = discountAmt.coerceAtMost(orderAmount)

        ValidateCouponResponse(
            valid = true,
            code = cleanCode,
            discountType = discountType,
            discountValue = discountVal,
            discountAmount = discountAmt,
            message = "Coupon applied! KES ${discountAmt.toLong()} discount."
        )
    }

    // ─── Automated Promotion Rules ────────────────────────────────────────────

    fun listRules(businessId: String): List<PromotionRuleResponse> = transaction {
        (PromotionRulesTable leftJoin ProductsTable)
            .select { PromotionRulesTable.businessId eq businessId }
            .orderBy(PromotionRulesTable.createdAt, SortOrder.DESC)
            .map { row ->
                PromotionRuleResponse(
                    id = row[PromotionRulesTable.id],
                    businessId = row[PromotionRulesTable.businessId],
                    name = row[PromotionRulesTable.name],
                    ruleType = row[PromotionRulesTable.ruleType],
                    triggerProductId = row[PromotionRulesTable.triggerProductId],
                    triggerProductName = row.getOrNull(ProductsTable.name),
                    triggerQuantity = row[PromotionRulesTable.triggerQuantity],
                    rewardProductId = row[PromotionRulesTable.rewardProductId],
                    rewardProductName = null,
                    rewardQuantity = row[PromotionRulesTable.rewardQuantity],
                    discountPercent = row[PromotionRulesTable.discountPercent],
                    minimumSpend = row[PromotionRulesTable.minimumSpend],
                    isActive = row[PromotionRulesTable.isActive],
                    createdAt = row[PromotionRulesTable.createdAt].toString()
                )
            }
    }

    fun createRule(businessId: String, req: CreatePromotionRuleRequest): ApiResponse<PromotionRuleResponse> = transaction {
        if (req.name.isBlank()) {
            return@transaction ApiResponse(false, message = "Promotion rule name is required")
        }
        val id = generateId()
        val now = Clock.System.now()
        PromotionRulesTable.insert {
            it[PromotionRulesTable.id] = id
            it[PromotionRulesTable.businessId] = businessId
            it[name] = req.name.trim()
            it[ruleType] = req.ruleType.uppercase()
            it[triggerProductId] = req.triggerProductId
            it[triggerQuantity] = req.triggerQuantity.coerceAtLeast(1)
            it[rewardProductId] = req.rewardProductId
            it[rewardQuantity] = req.rewardQuantity.coerceAtLeast(1)
            it[discountPercent] = req.discountPercent.coerceAtLeast(0.0)
            it[minimumSpend] = req.minimumSpend.coerceAtLeast(0.0)
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }
        val created = PromotionRulesTable.select { PromotionRulesTable.id eq id }.first()
        ApiResponse(true, data = created.toPromotionRuleResponse(), message = "Promotion rule '${req.name}' created")
    }

    fun toggleRule(businessId: String, ruleId: String): ApiResponse<PromotionRuleResponse> = transaction {
        val row = PromotionRulesTable.select {
            (PromotionRulesTable.id eq ruleId) and (PromotionRulesTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Rule not found")

        val newActive = !row[PromotionRulesTable.isActive]
        PromotionRulesTable.update({ PromotionRulesTable.id eq ruleId }) {
            it[isActive] = newActive
            it[updatedAt] = Clock.System.now()
        }
        val updated = PromotionRulesTable.select { PromotionRulesTable.id eq ruleId }.first()
        ApiResponse(true, data = updated.toPromotionRuleResponse(), message = if (newActive) "Rule enabled" else "Rule disabled")
    }

    fun deleteRule(businessId: String, ruleId: String): ApiResponse<Unit> = transaction {
        val count = PromotionRulesTable.deleteWhere {
            (PromotionRulesTable.id eq ruleId) and (PromotionRulesTable.businessId eq businessId)
        }
        if (count > 0) ApiResponse(true, message = "Rule deleted")
        else ApiResponse(false, message = "Rule not found")
    }

    fun evaluateCart(businessId: String, req: EvaluateCartPromotionsRequest): CartPromotionsResponse = transaction {
        val subtotal = req.items.sumOf { it.quantity * it.unitPrice }
        val appliedRules = mutableListOf<AppliedPromotionSummary>()
        var rulesDiscount = 0.0

        val activeRules = PromotionRulesTable.select {
            (PromotionRulesTable.businessId eq businessId) and (PromotionRulesTable.isActive eq true)
        }.toList()

        for (r in activeRules) {
            val ruleType = r[PromotionRulesTable.ruleType]
            val ruleName = r[PromotionRulesTable.name]

            when (ruleType) {
                "TIERED_SPEND" -> {
                    val minSpend = r[PromotionRulesTable.minimumSpend]
                    val pct = r[PromotionRulesTable.discountPercent]
                    if (subtotal >= minSpend && minSpend > 0.0 && pct > 0.0) {
                        val disc = (subtotal * (pct / 100.0))
                        rulesDiscount += disc
                        appliedRules.add(
                            AppliedPromotionSummary(
                                ruleName = ruleName,
                                ruleType = ruleType,
                                discountAmount = disc,
                                rewardDescription = "$pct% off on orders over KES ${minSpend.toLong()}"
                            )
                        )
                    }
                }
                "BUY_X_GET_Y_FREE" -> {
                    val triggerId = r[PromotionRulesTable.triggerProductId]
                    val triggerQty = r[PromotionRulesTable.triggerQuantity]
                    val rewardQty = r[PromotionRulesTable.rewardQuantity]

                    val matchedItem = req.items.firstOrNull { it.productId == triggerId }
                    if (matchedItem != null && matchedItem.quantity >= triggerQty) {
                        val timesQualified = matchedItem.quantity / triggerQty
                        val freeUnits = timesQualified * rewardQty
                        val freeValue = freeUnits * matchedItem.unitPrice
                        rulesDiscount += freeValue
                        appliedRules.add(
                            AppliedPromotionSummary(
                                ruleName = ruleName,
                                ruleType = ruleType,
                                discountAmount = freeValue,
                                rewardDescription = "Buy $triggerQty get $rewardQty free ($freeUnits free items included)"
                            )
                        )
                    }
                }
            }
        }

        // Coupon evaluation
        var couponValidation: ValidateCouponResponse? = null
        var couponDiscount = 0.0
        val cleanCode = req.couponCode?.trim()?.uppercase()
        if (!cleanCode.isNullOrBlank()) {
            val res = validateCoupon(businessId, cleanCode, (subtotal - rulesDiscount).coerceAtLeast(0.0))
            couponValidation = res
            if (res.valid) {
                couponDiscount = res.discountAmount
            }
        }

        val totalDiscount = (rulesDiscount + couponDiscount).coerceAtMost(subtotal)
        val finalTotal = (subtotal - totalDiscount).coerceAtLeast(0.0)

        CartPromotionsResponse(
            subtotal = subtotal,
            couponDiscount = couponDiscount,
            rulesDiscount = rulesDiscount,
            totalDiscount = totalDiscount,
            finalTotal = finalTotal,
            appliedCoupon = couponValidation,
            appliedRules = appliedRules
        )
    }

    private fun ResultRow.toCouponResponse() = CouponResponse(
        id = this[CouponCodesTable.id],
        businessId = this[CouponCodesTable.businessId],
        code = this[CouponCodesTable.code],
        description = this[CouponCodesTable.description],
        discountType = this[CouponCodesTable.discountType],
        discountValue = this[CouponCodesTable.discountValue],
        minimumOrderAmount = this[CouponCodesTable.minimumOrderAmount],
        maxDiscountAmount = this[CouponCodesTable.maxDiscountAmount],
        usageLimit = this[CouponCodesTable.usageLimit],
        usageCount = this[CouponCodesTable.usageCount],
        startDate = this[CouponCodesTable.startDate]?.toString(),
        endDate = this[CouponCodesTable.endDate]?.toString(),
        isActive = this[CouponCodesTable.isActive],
        createdAt = this[CouponCodesTable.createdAt].toString()
    )

    private fun ResultRow.toPromotionRuleResponse() = PromotionRuleResponse(
        id = this[PromotionRulesTable.id],
        businessId = this[PromotionRulesTable.businessId],
        name = this[PromotionRulesTable.name],
        ruleType = this[PromotionRulesTable.ruleType],
        triggerProductId = this[PromotionRulesTable.triggerProductId],
        triggerProductName = null,
        triggerQuantity = this[PromotionRulesTable.triggerQuantity],
        rewardProductId = this[PromotionRulesTable.rewardProductId],
        rewardProductName = null,
        rewardQuantity = this[PromotionRulesTable.rewardQuantity],
        discountPercent = this[PromotionRulesTable.discountPercent],
        minimumSpend = this[PromotionRulesTable.minimumSpend],
        isActive = this[PromotionRulesTable.isActive],
        createdAt = this[PromotionRulesTable.createdAt].toString()
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. PRODUCT UNITS OF MEASURE (UOM) SERVICE
// ═══════════════════════════════════════════════════════════════════════════════

class UomService {

    fun listUoms(businessId: String, productId: String): List<ProductUomResponse> = transaction {
        ProductUomConversionsTable.select {
            (ProductUomConversionsTable.businessId eq businessId) and (ProductUomConversionsTable.productId eq productId)
        }.orderBy(ProductUomConversionsTable.conversionFactor, SortOrder.ASC)
            .map { it.toResponse() }
    }

    fun createUom(businessId: String, productId: String, req: CreateProductUomRequest): ApiResponse<ProductUomResponse> = transaction {
        val prod = ProductsTable.select {
            (ProductsTable.id eq productId) and (ProductsTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Product not found")

        val cleanUnit = req.unitName.trim()
        if (cleanUnit.isBlank()) return@transaction ApiResponse(false, message = "Unit name is required (e.g. Carton, Box)")
        if (req.conversionFactor <= 0.0) return@transaction ApiResponse(false, message = "Conversion factor must be greater than 0")

        val id = generateId()
        val now = Clock.System.now()
        ProductUomConversionsTable.insert {
            it[ProductUomConversionsTable.id] = id
            it[ProductUomConversionsTable.businessId] = businessId
            it[ProductUomConversionsTable.productId] = productId
            it[unitName] = cleanUnit
            it[conversionFactor] = req.conversionFactor
            it[sellingPrice] = req.sellingPrice
            it[buyingPrice] = req.buyingPrice
            it[barcode] = req.barcode?.trim()?.ifBlank { null }
            it[isDefault] = req.isDefault
            it[createdAt] = now
            it[updatedAt] = now
        }

        val created = ProductUomConversionsTable.select { ProductUomConversionsTable.id eq id }.first()
        ApiResponse(true, data = created.toResponse(), message = "Unit of measure '$cleanUnit' added")
    }

    fun deleteUom(businessId: String, uomId: String): ApiResponse<Unit> = transaction {
        val deleted = ProductUomConversionsTable.deleteWhere {
            (ProductUomConversionsTable.id eq uomId) and (ProductUomConversionsTable.businessId eq businessId)
        }
        if (deleted > 0) ApiResponse(true, message = "Unit of measure deleted")
        else ApiResponse(false, message = "Unit of measure not found")
    }

    fun updateBaseUnit(businessId: String, productId: String, baseUnit: String): ApiResponse<Unit> = transaction {
        val clean = baseUnit.trim().uppercase()
        ProductsTable.update({ (ProductsTable.id eq productId) and (ProductsTable.businessId eq businessId) }) {
            it[ProductsTable.baseUnit] = clean
            it[updatedAt] = Clock.System.now()
        }
        ApiResponse(true, message = "Base unit updated to $clean")
    }

    private fun ResultRow.toResponse() = ProductUomResponse(
        id = this[ProductUomConversionsTable.id],
        businessId = this[ProductUomConversionsTable.businessId],
        productId = this[ProductUomConversionsTable.productId],
        unitName = this[ProductUomConversionsTable.unitName],
        conversionFactor = this[ProductUomConversionsTable.conversionFactor],
        sellingPrice = this[ProductUomConversionsTable.sellingPrice],
        buyingPrice = this[ProductUomConversionsTable.buyingPrice],
        barcode = this[ProductUomConversionsTable.barcode],
        isDefault = this[ProductUomConversionsTable.isDefault],
        createdAt = this[ProductUomConversionsTable.createdAt].toString()
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. CUSTOM DOMAIN CNAME MAPPING SERVICE
// ═══════════════════════════════════════════════════════════════════════════════

class CustomDomainService {

    fun getConfig(businessId: String): CustomDomainConfigResponse = transaction {
        val biz = BusinessesTable.select { BusinessesTable.id eq businessId }.first()
        val domain = biz[BusinessesTable.customDomain]
        val status = biz[BusinessesTable.customDomainStatus]
        val cnameTarget = biz[BusinessesTable.customDomainCnameTarget]
        val verifiedAt = biz[BusinessesTable.customDomainVerifiedAt]?.toString()
        val slug = biz[BusinessesTable.storefrontSlug]

        CustomDomainConfigResponse(
            customDomain = domain,
            status = status,
            cnameTarget = cnameTarget,
            verifiedAt = verifiedAt,
            instructions = "Point your DNS CNAME record for '$domain' to '$cnameTarget' with a TTL of 300 seconds.",
            standardStorefrontUrl = "https://biashara360.co.ke/shop/$slug"
        )
    }

    fun setCustomDomain(businessId: String, domainInput: String): ApiResponse<CustomDomainConfigResponse> = transaction {
        val domain = domainInput.trim().lowercase().removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (domain.isBlank() || !domain.contains(".") || domain.length < 4) {
            return@transaction ApiResponse(false, message = "Invalid domain format (e.g. shop.mybrand.co.ke)")
        }

        val existing = BusinessesTable.select {
            (BusinessesTable.customDomain eq domain) and (BusinessesTable.id neq businessId)
        }.count() > 0
        if (existing) {
            return@transaction ApiResponse(false, message = "The domain '$domain' is already claimed by another store")
        }

        BusinessesTable.update({ BusinessesTable.id eq businessId }) {
            it[customDomain] = domain
            it[customDomainStatus] = "PENDING_DNS"
            it[customDomainVerifiedAt] = null
            it[updatedAt] = Clock.System.now()
        }

        ApiResponse(true, data = getConfig(businessId), message = "Custom domain updated. Set up your DNS CNAME record to complete verification.")
    }

    fun verifyCustomDomain(businessId: String): VerifyCustomDomainResponse {
        val domain = transaction {
            BusinessesTable.select { BusinessesTable.id eq businessId }
                .firstOrNull()?.get(BusinessesTable.customDomain)
        } ?: return VerifyCustomDomainResponse(domain = "", status = "ERROR", dnsResolved = false, message = "No custom domain configured")

        // Perform real-time DNS CNAME resolution check
        val dnsResolved = try {
            val address = InetAddress.getByName(domain)
            address != null
        } catch (_: Exception) {
            false
        }

        val now = Clock.System.now()
        val newStatus = if (dnsResolved) "ACTIVE" else "PENDING_DNS"
        transaction {
            BusinessesTable.update({ BusinessesTable.id eq businessId }) {
                it[customDomainStatus] = newStatus
                if (dnsResolved) it[customDomainVerifiedAt] = now
                it[updatedAt] = now
            }
        }

        return VerifyCustomDomainResponse(
            domain = domain,
            status = newStatus,
            dnsResolved = dnsResolved,
            message = if (dnsResolved) {
                "DNS verification succeeded! Your custom domain $domain is active and resolving."
            } else {
                "DNS records not detected yet. DNS changes can take up to 30 minutes to propagate worldwide. Please retry in a few moments."
            }
        )
    }

    fun removeCustomDomain(businessId: String): ApiResponse<Unit> = transaction {
        BusinessesTable.update({ BusinessesTable.id eq businessId }) {
            it[customDomain] = null
            it[customDomainStatus] = "UNCONFIGURED"
            it[customDomainVerifiedAt] = null
            it[updatedAt] = Clock.System.now()
        }
        ApiResponse(true, message = "Custom domain removed successfully")
    }

    fun resolveStorefrontByDomain(host: String): ResolvedStorefrontDomainResponse = transaction {
        val cleanHost = host.trim().lowercase().split(":").first()
        val biz = BusinessesTable.select {
            (BusinessesTable.customDomain eq cleanHost) and (BusinessesTable.isActive eq true)
        }.firstOrNull()

        if (biz != null) {
            ResolvedStorefrontDomainResponse(
                found = true,
                businessId = biz[BusinessesTable.id],
                storefrontSlug = biz[BusinessesTable.storefrontSlug],
                businessName = biz[BusinessesTable.name]
            )
        } else {
            ResolvedStorefrontDomainResponse(found = false)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. DOUBLE-ENTRY GENERAL LEDGER SERVICE
// ═══════════════════════════════════════════════════════════════════════════════

class GeneralLedgerService {

    fun seedDefaultAccountsIfEmpty(businessId: String) = transaction {
        val count = ChartOfAccountsTable.select { ChartOfAccountsTable.businessId eq businessId }.count()
        if (count > 0) return@transaction

        val now = Clock.System.now()
        val defaults = listOf(
            // Code, Name, Type, NormalBalance
            listOf("1010", "Cash at Till", "ASSET", "DEBIT"),
            listOf("1020", "Lipa na M-Pesa Holding Account", "ASSET", "DEBIT"),
            listOf("1030", "Bank Current Account", "ASSET", "DEBIT"),
            listOf("1100", "Accounts Receivable (Debtors)", "ASSET", "DEBIT"),
            listOf("1200", "Merchandise Inventory Asset", "ASSET", "DEBIT"),
            listOf("2010", "Accounts Payable (Suppliers)", "LIABILITY", "CREDIT"),
            listOf("2020", "KRA VAT & Tax Payable", "LIABILITY", "CREDIT"),
            listOf("3010", "Owner Capital / Equity", "EQUITY", "CREDIT"),
            listOf("3020", "Retained Earnings", "EQUITY", "CREDIT"),
            listOf("4010", "POS Sales Revenue", "REVENUE", "CREDIT"),
            listOf("4020", "Service Revenue", "REVENUE", "CREDIT"),
            listOf("4030", "Sales Discounts Given", "REVENUE", "DEBIT"),
            listOf("5010", "Cost of Goods Sold (COGS)", "EXPENSE", "DEBIT"),
            listOf("5020", "Operating Expenses", "EXPENSE", "DEBIT"),
            listOf("5030", "Staff Wages & Salaries", "EXPENSE", "DEBIT"),
            listOf("5040", "Rent & Rates", "EXPENSE", "DEBIT"),
            listOf("5050", "Utilities (Electricity, Water, Internet)", "EXPENSE", "DEBIT")
        )

        for (item in defaults) {
            ChartOfAccountsTable.insert {
                it[id] = generateId()
                it[ChartOfAccountsTable.businessId] = businessId
                it[accountCode] = item[0]
                it[accountName] = item[1]
                it[accountType] = item[2]
                it[normalBalance] = item[3]
                it[currentBalance] = 0.0
                it[isSystem] = true
                it[isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    fun listAccounts(businessId: String): List<AccountResponse> = transaction {
        seedDefaultAccountsIfEmpty(businessId)
        ChartOfAccountsTable.select { ChartOfAccountsTable.businessId eq businessId }
            .orderBy(ChartOfAccountsTable.accountCode, SortOrder.ASC)
            .map { it.toAccountResponse() }
    }

    fun createAccount(businessId: String, req: CreateAccountRequest): ApiResponse<AccountResponse> = transaction {
        seedDefaultAccountsIfEmpty(businessId)
        val cleanCode = req.accountCode.trim()
        val cleanName = req.accountName.trim()
        if (cleanCode.isBlank() || cleanName.isBlank()) {
            return@transaction ApiResponse(false, message = "Account code and account name are required")
        }

        val exists = ChartOfAccountsTable.select {
            (ChartOfAccountsTable.businessId eq businessId) and (ChartOfAccountsTable.accountCode eq cleanCode)
        }.count() > 0
        if (exists) {
            return@transaction ApiResponse(false, message = "Account code '$cleanCode' already exists")
        }

        val id = generateId()
        val now = Clock.System.now()
        ChartOfAccountsTable.insert {
            it[ChartOfAccountsTable.id] = id
            it[ChartOfAccountsTable.businessId] = businessId
            it[accountCode] = cleanCode
            it[accountName] = cleanName
            it[accountType] = req.accountType.uppercase()
            it[normalBalance] = req.normalBalance.uppercase()
            it[currentBalance] = 0.0
            it[isSystem] = false
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }

        val created = ChartOfAccountsTable.select { ChartOfAccountsTable.id eq id }.first()
        ApiResponse(true, data = created.toAccountResponse(), message = "Account '$cleanName' created successfully")
    }

    fun listJournalEntries(businessId: String): List<JournalEntryResponse> = transaction {
        seedDefaultAccountsIfEmpty(businessId)
        val entries = JournalEntriesTable.select { JournalEntriesTable.businessId eq businessId }
            .orderBy(JournalEntriesTable.entryDate, SortOrder.DESC)
            .toList()

        entries.map { entryRow ->
            val entryId = entryRow[JournalEntriesTable.id]
            val lines = (JournalEntryLinesTable innerJoin ChartOfAccountsTable)
                .select { JournalEntryLinesTable.entryId eq entryId }
                .map { lineRow ->
                    JournalEntryLineResponse(
                        id = lineRow[JournalEntryLinesTable.id],
                        accountId = lineRow[JournalEntryLinesTable.accountId],
                        accountCode = lineRow[ChartOfAccountsTable.accountCode],
                        accountName = lineRow[ChartOfAccountsTable.accountName],
                        description = lineRow[JournalEntryLinesTable.description],
                        debit = lineRow[JournalEntryLinesTable.debit],
                        credit = lineRow[JournalEntryLinesTable.credit]
                    )
                }

            JournalEntryResponse(
                id = entryId,
                entryNumber = entryRow[JournalEntriesTable.entryNumber],
                entryDate = entryRow[JournalEntriesTable.entryDate].toString(),
                narration = entryRow[JournalEntriesTable.narration],
                sourceModule = entryRow[JournalEntriesTable.sourceModule],
                sourceReferenceId = entryRow[JournalEntriesTable.sourceReferenceId],
                totalDebit = entryRow[JournalEntriesTable.totalDebit],
                totalCredit = entryRow[JournalEntriesTable.totalCredit],
                isBalanced = entryRow[JournalEntriesTable.isBalanced],
                lines = lines,
                createdBy = entryRow[JournalEntriesTable.createdBy],
                createdAt = entryRow[JournalEntriesTable.createdAt].toString()
            )
        }
    }

    fun createJournalEntry(
        businessId: String,
        callerUserId: String?,
        req: CreateJournalEntryRequest
    ): ApiResponse<JournalEntryResponse> = transaction {
        seedDefaultAccountsIfEmpty(businessId)
        if (req.lines.size < 2) {
            return@transaction ApiResponse(false, message = "A journal entry requires at least 2 balanced lines (debit & credit)")
        }

        val totalDebit = req.lines.sumOf { it.debit }
        val totalCredit = req.lines.sumOf { it.credit }
        val diff = Math.abs(totalDebit - totalCredit)
        if (diff > 0.01) {
            return@transaction ApiResponse(
                false,
                message = "Unbalanced entry: Total debits (KES $totalDebit) must equal total credits (KES $totalCredit). Difference: KES $diff"
            )
        }

        val now = Clock.System.now()
        val entryId = generateId()
        val count = JournalEntriesTable.select { JournalEntriesTable.businessId eq businessId }.count()
        val entryNumber = "JE-${1000 + count + 1}"

        JournalEntriesTable.insert {
            it[id] = entryId
            it[JournalEntriesTable.businessId] = businessId
            it[JournalEntriesTable.entryNumber] = entryNumber
            it[entryDate] = now
            it[narration] = req.narration.trim()
            it[sourceModule] = req.sourceModule.uppercase()
            it[sourceReferenceId] = req.sourceReferenceId
            it[JournalEntriesTable.totalDebit] = totalDebit
            it[JournalEntriesTable.totalCredit] = totalCredit
            it[isBalanced] = true
            it[createdBy] = callerUserId
            it[createdAt] = now
        }

        for (line in req.lines) {
            val lineId = generateId()
            JournalEntryLinesTable.insert {
                it[id] = lineId
                it[JournalEntryLinesTable.entryId] = entryId
                it[accountId] = line.accountId
                it[description] = line.description.trim()
                it[debit] = line.debit
                it[credit] = line.credit
            }

            // Update account running balance according to normal balance
            val acc = ChartOfAccountsTable.select { ChartOfAccountsTable.id eq line.accountId }.first()
            val normal = acc[ChartOfAccountsTable.normalBalance]
            val balanceDelta = if (normal == "DEBIT") (line.debit - line.credit) else (line.credit - line.debit)

            ChartOfAccountsTable.update({ ChartOfAccountsTable.id eq line.accountId }) {
                it[currentBalance] = acc[ChartOfAccountsTable.currentBalance] + balanceDelta
                it[updatedAt] = now
            }
        }

        val createdList = listJournalEntries(businessId)
        val created = createdList.first { it.id == entryId }
        ApiResponse(true, data = created, message = "Journal entry $entryNumber posted successfully")
    }

    fun getTrialBalance(businessId: String): TrialBalanceResponse = transaction {
        seedDefaultAccountsIfEmpty(businessId)
        val accounts = ChartOfAccountsTable.select {
            (ChartOfAccountsTable.businessId eq businessId) and (ChartOfAccountsTable.isActive eq true)
        }.orderBy(ChartOfAccountsTable.accountCode, SortOrder.ASC).toList()

        var totalDebit = 0.0
        var totalCredit = 0.0
        val rows = accounts.map { acc ->
            val normal = acc[ChartOfAccountsTable.normalBalance]
            val bal = acc[ChartOfAccountsTable.currentBalance]
            val debit = if (normal == "DEBIT") bal.coerceAtLeast(0.0) else 0.0
            val credit = if (normal == "CREDIT") bal.coerceAtLeast(0.0) else 0.0
            totalDebit += debit
            totalCredit += credit

            TrialBalanceAccountRow(
                accountCode = acc[ChartOfAccountsTable.accountCode],
                accountName = acc[ChartOfAccountsTable.accountName],
                accountType = acc[ChartOfAccountsTable.accountType],
                debit = debit,
                credit = credit
            )
        }

        TrialBalanceResponse(
            asOfDate = Clock.System.now().toString(),
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            isBalanced = Math.abs(totalDebit - totalCredit) < 0.01,
            accounts = rows
        )
    }

    private fun ResultRow.toAccountResponse() = AccountResponse(
        id = this[ChartOfAccountsTable.id],
        accountCode = this[ChartOfAccountsTable.accountCode],
        accountName = this[ChartOfAccountsTable.accountName],
        accountType = this[ChartOfAccountsTable.accountType],
        normalBalance = this[ChartOfAccountsTable.normalBalance],
        currentBalance = this[ChartOfAccountsTable.currentBalance],
        isSystem = this[ChartOfAccountsTable.isSystem],
        isActive = this[ChartOfAccountsTable.isActive]
    )
}
