package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TaxService {

    // ── Tax Rates CRUD ────────────────────────────────────────────────────────

    fun getRates(businessId: String): List<TaxRateResponse> = transaction {
        TaxRatesTable.select { TaxRatesTable.businessId eq businessId }
            .orderBy(TaxRatesTable.taxType)
            .map { it.toRateResponse() }
    }

    fun createRate(businessId: String, req: TaxRateRequest): ApiResponse<TaxRateResponse> = transaction {
        if (req.rate < 0 || req.rate > 10) return@transaction ApiResponse(
            false, message = "Tax rate must be between 0% and 1000%"
        )
        val id = generateId()
        val now = Clock.System.now()
        TaxRatesTable.insert {
            it[TaxRatesTable.id]          = id
            it[TaxRatesTable.businessId]  = businessId
            it[taxType]                   = req.taxType.uppercase()
            it[name]                      = req.name
            it[rate]                      = req.rate
            it[isInclusive]               = req.isInclusive
            it[appliesTo]                 = req.appliesTo.uppercase()
            it[description]               = req.description
            it[createdAt]                 = now
            it[updatedAt]                 = now
        }
        val created = TaxRatesTable.select { TaxRatesTable.id eq id }.first().toRateResponse()
        ApiResponse(true, data = created, message = "Tax rate created")
    }

    fun updateRate(id: String, businessId: String, req: TaxRateRequest): ApiResponse<TaxRateResponse> = transaction {
        val updated = TaxRatesTable.update({
            (TaxRatesTable.id eq id) and (TaxRatesTable.businessId eq businessId)
        }) {
            it[taxType]     = req.taxType.uppercase()
            it[name]        = req.name
            it[rate]        = req.rate
            it[isInclusive] = req.isInclusive
            it[appliesTo]   = req.appliesTo.uppercase()
            it[description] = req.description
            it[updatedAt]   = Clock.System.now()
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Tax rate not found")
        val record = TaxRatesTable.select { TaxRatesTable.id eq id }.first().toRateResponse()
        ApiResponse(true, data = record)
    }

    fun toggleRate(id: String, businessId: String): ApiResponse<TaxRateResponse> = transaction {
        val current = TaxRatesTable.select {
            (TaxRatesTable.id eq id) and (TaxRatesTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Tax rate not found")
        val newState = !current[TaxRatesTable.isActive]
        TaxRatesTable.update({ TaxRatesTable.id eq id }) {
            it[isActive]  = newState
            it[updatedAt] = Clock.System.now()
        }
        val record = TaxRatesTable.select { TaxRatesTable.id eq id }.first().toRateResponse()
        ApiResponse(true, data = record, message = if (newState) "Tax rate enabled" else "Tax rate disabled")
    }

    fun deleteRate(id: String, businessId: String): ApiResponse<Unit> = transaction {
        // Don't delete if used in order tax lines
        val usedInOrders = OrderTaxLinesTable.select { OrderTaxLinesTable.taxRateId eq id }.count()
        if (usedInOrders > 0) {
            TaxRatesTable.update({ TaxRatesTable.id eq id }) { it[isActive] = false }
            return@transaction ApiResponse(true, message = "Tax rate deactivated (in use by $usedInOrders orders")
        }
        TaxRatesTable.deleteWhere {
            (TaxRatesTable.id eq id) and (TaxRatesTable.businessId eq businessId)
        }
        ApiResponse(true, message = "Tax rate deleted")
    }

    // ── Seed Kenya defaults for new businesses ────────────────────────────────

    fun seedKenyaDefaults(businessId: String) = transaction {
        val existing = TaxRatesTable.select { TaxRatesTable.businessId eq businessId }.count()
        if (existing > 0L) return@transaction
        val now = Clock.System.now()
        KenyaTaxDefaults.defaults.forEach { (type, name, rate) ->
            TaxRatesTable.insert {
                it[id]                       = generateId()
                it[TaxRatesTable.businessId] = businessId
                it[taxType]                  = type
                it[TaxRatesTable.name]       = name
                it[TaxRatesTable.rate]       = rate
                it[isInclusive]              = false
                it[appliesTo]                = if (type == "TOT") "ALL" else "PRODUCTS"
                it[description]              = kenyaTaxDescription(type)
                it[createdAt]                = now
                it[updatedAt]                = now
                // TOT replaces VAT for small businesses (<KES 5M turnover) — start inactive
                it[isActive]                 = type == "VAT"
            }
        }
    }

    // ── Tax Calculation ───────────────────────────────────────────────────────

    fun calculateTax(businessId: String, req: TaxCalculationRequest): ApiResponse<TaxCalculationResponse> = transaction {
        val rates = TaxRatesTable.select {
            (TaxRatesTable.businessId eq businessId) and
            (TaxRatesTable.id inList req.taxRateIds) and
            (TaxRatesTable.isActive eq true)
        }.map { it.toRateResponse() }

        val lines = rates.map { rate ->
            val taxAmount = if (rate.isInclusive) {
                req.amount - (req.amount / (1 + rate.rate))
            } else {
                req.amount * rate.rate
            }
            TaxLineCalculation(
                taxRateId  = rate.id,
                taxName    = rate.name,
                taxType    = rate.taxType,
                rate       = rate.rate,
                taxAmount  = Math.round(taxAmount * 100.0) / 100.0
            )
        }

        val totalTax   = lines.sumOf { it.taxAmount }
        val grandTotal = if (rates.any { it.isInclusive }) req.amount else req.amount + totalTax

        ApiResponse(true, data = TaxCalculationResponse(
            subtotal   = req.amount,
            taxLines   = lines,
            totalTax   = totalTax,
            grandTotal = Math.round(grandTotal * 100.0) / 100.0
        ))
    }

    // ── Order Tax Lines ───────────────────────────────────────────────────────

    fun getOrderTaxLines(orderId: String): List<TaxLineResponse> = transaction {
        OrderTaxLinesTable.select { OrderTaxLinesTable.orderId eq orderId }
            .map { it.toTaxLineResponse() }
    }

    fun recordOrderTaxLines(orderId: String, businessId: String, taxes: List<OrderTaxRequest>) = transaction {
        taxes.forEach { t ->
            val rate = TaxRatesTable.select {
                (TaxRatesTable.id eq t.taxRateId) and (TaxRatesTable.businessId eq businessId)
            }.firstOrNull() ?: return@forEach

            val taxAmount = Math.round(t.taxableAmount * rate[TaxRatesTable.rate] * 100.0) / 100.0
            OrderTaxLinesTable.insert {
                it[id]            = generateId()
                it[OrderTaxLinesTable.orderId]  = orderId
                it[taxRateId]     = t.taxRateId
                it[taxType]       = rate[TaxRatesTable.taxType]
                it[taxName]       = rate[TaxRatesTable.name]
                it[OrderTaxLinesTable.rate] = rate[TaxRatesTable.rate]
                it[taxableAmount] = t.taxableAmount
                it[OrderTaxLinesTable.taxAmount] = taxAmount
            }
        }
    }

    // ── Tax Remittances ───────────────────────────────────────────────────────

    fun getRemittances(businessId: String, taxType: String? = null): List<TaxRemittanceResponse> = transaction {
        var query = TaxRemittancesTable.select { TaxRemittancesTable.businessId eq businessId }
        if (!taxType.isNullOrBlank()) query = query.andWhere { TaxRemittancesTable.taxType eq taxType }
        query.orderBy(TaxRemittancesTable.periodStart, SortOrder.DESC).map { it.toRemittanceResponse() }
    }

    fun createRemittance(businessId: String, req: TaxRemittanceRequest): ApiResponse<TaxRemittanceResponse> = transaction {
        val start = kotlinx.datetime.LocalDate.parse(req.periodStart)
        val end   = kotlinx.datetime.LocalDate.parse(req.periodEnd)
        val zoneId = java.time.ZoneId.of("Africa/Nairobi")
        val startJavaDate = java.time.LocalDate.parse(req.periodStart)
        val endJavaDate = java.time.LocalDate.parse(req.periodEnd)
        val startInstant = startJavaDate.atStartOfDay(zoneId).toInstant()
        val endInstant = endJavaDate.plusDays(1).atStartOfDay(zoneId).toInstant()

        // Sum all order tax lines for this period and type
        val join = (OrderTaxLinesTable innerJoin OrdersTable)
        val taxableAmount = join
            .slice(OrderTaxLinesTable.taxableAmount.sum())
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrderTaxLinesTable.taxType eq req.taxType.uppercase()) and
                (OrdersTable.createdAt greaterEq startInstant) and
                (OrdersTable.createdAt lessEq endInstant)
            }.firstOrNull()?.get(OrderTaxLinesTable.taxableAmount.sum()) ?: 0.0

        val taxAmount = join
            .slice(OrderTaxLinesTable.taxAmount.sum())
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrderTaxLinesTable.taxType eq req.taxType.uppercase()) and
                (OrdersTable.createdAt greaterEq startInstant) and
                (OrdersTable.createdAt lessEq endInstant)
            }.firstOrNull()?.get(OrderTaxLinesTable.taxAmount.sum()) ?: 0.0

        val id  = generateId()
        val now = Clock.System.now()
        val record = TaxRemittancesTable.insert {
            it[TaxRemittancesTable.id] = id
            it[TaxRemittancesTable.businessId] = businessId
            it[TaxRemittancesTable.taxType] = req.taxType.uppercase()
            it[TaxRemittancesTable.periodStart] = start
            it[TaxRemittancesTable.periodEnd] = end
            it[TaxRemittancesTable.taxableAmount] = taxableAmount
            it[TaxRemittancesTable.taxAmount] = taxAmount
            it[TaxRemittancesTable.notes] = req.notes
            it[TaxRemittancesTable.createdAt] = now
        }
        val remittance = TaxRemittancesTable.select { TaxRemittancesTable.id eq id }.first().toRemittanceResponse()
        ApiResponse(true, data = remittance, message = "Remittance record created")
    }

    fun updateRemittanceStatus(
        id: String, businessId: String, req: UpdateRemittanceStatusRequest
    ): ApiResponse<TaxRemittanceResponse> = transaction {
        val now = Clock.System.now()
        val updated = TaxRemittancesTable.update({
            (TaxRemittancesTable.id eq id) and (TaxRemittancesTable.businessId eq businessId)
        }) {
            it[status] = req.status.uppercase()
            if (req.status.uppercase() == "FILED") it[filedAt] = now
            if (req.status.uppercase() == "PAID")  it[paidAt]  = now
            if (req.receiptNumber != null)         it[receiptNumber] = req.receiptNumber
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Remittance not found")
        val record = TaxRemittancesTable.select { TaxRemittancesTable.id eq id }.first().toRemittanceResponse()
        ApiResponse(true, data = record)
    }

    // ── Tax Summary Report ────────────────────────────────────────────────────

    fun getTaxSummary(businessId: String, periodStart: String, periodEnd: String): ApiResponse<TaxSummaryResponse> = transaction {
        val start = java.time.LocalDate.parse(periodStart)
        val end   = java.time.LocalDate.parse(periodEnd)
        val zoneId = java.time.ZoneId.of("Africa/Nairobi")
        val startI = start.atStartOfDay(zoneId).toInstant()
        val endI   = end.plusDays(1).atStartOfDay(zoneId).toInstant()

        fun sumTaxType(type: String, col: Column<Double>): Double =
            (OrderTaxLinesTable innerJoin OrdersTable)
                .slice(col.sum())
                .select {
                    (OrdersTable.businessId eq businessId) and
                    (OrderTaxLinesTable.taxType eq type) and
                    (OrdersTable.createdAt greaterEq startI) and
                    (OrdersTable.createdAt lessEq endI)
                }.firstOrNull()?.get(col.sum()) ?: 0.0

        val vatCollected   = sumTaxType("VAT",    OrderTaxLinesTable.taxAmount)
        val totAmount      = sumTaxType("TOT",    OrderTaxLinesTable.taxAmount)
        val whtAmount      = sumTaxType("WHT",    OrderTaxLinesTable.taxAmount)
        val exciseAmount   = sumTaxType("EXCISE", OrderTaxLinesTable.taxAmount)
        val customAmount   = (OrderTaxLinesTable innerJoin OrdersTable)
            .slice(OrderTaxLinesTable.taxAmount.sum())
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrderTaxLinesTable.taxType notInList listOf("VAT","TOT","WHT","EXCISE")) and
                (OrdersTable.createdAt greaterEq startI) and
                (OrdersTable.createdAt lessEq endI)
            }.firstOrNull()?.get(OrderTaxLinesTable.taxAmount.sum()) ?: 0.0

        val totalRevenue   = OrdersTable.slice(OrdersTable.subtotal.sum())
            .select { (OrdersTable.businessId eq businessId) and (OrdersTable.createdAt greaterEq startI) and (OrdersTable.createdAt lessEq endI) }
            .firstOrNull()?.get(OrdersTable.subtotal.sum()) ?: 0.0

        val totalLiability = vatCollected + totAmount + whtAmount + exciseAmount + customAmount
        val effRate        = if (totalRevenue > 0) totalLiability / totalRevenue else 0.0

        val filed   = TaxRemittancesTable.select {
            (TaxRemittancesTable.businessId eq businessId) and
            (TaxRemittancesTable.status inList listOf("FILED","PAID")) and
            (TaxRemittancesTable.periodStart greaterEq start) and
            (TaxRemittancesTable.periodEnd lessEq end)
        }.count().toInt()

        val pending = TaxRemittancesTable.select {
            (TaxRemittancesTable.businessId eq businessId) and
            (TaxRemittancesTable.status eq "PENDING") and
            (TaxRemittancesTable.periodStart greaterEq start) and
            (TaxRemittancesTable.periodEnd lessEq end)
        }.count().toInt()

        ApiResponse(true, data = TaxSummaryResponse(
            period               = "$periodStart to $periodEnd",
            vatCollected         = vatCollected,
            vatPayable           = vatCollected,
            vatOnPurchases       = 0.0, // Would be tracked from purchase invoices
            netVat               = vatCollected,
            totAmount            = totAmount,
            whtAmount            = whtAmount,
            exciseAmount         = exciseAmount,
            customAmount         = customAmount,
            totalTaxLiability    = totalLiability,
            totalTaxableRevenue  = totalRevenue,
            effectiveTaxRate     = Math.round(effRate * 10000.0) / 10000.0,
            filedRemittances     = filed,
            pendingRemittances   = pending
        ))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun kenyaTaxDescription(type: String) = when (type) {
        "VAT"    -> "16% VAT on taxable goods & services. Mandatory for businesses with >KES 5M annual turnover."
        "TOT"    -> "1.5% Turnover Tax on gross receipts. For businesses with KES 1M–5M annual turnover."
        "WHT"    -> "3% Withholding Tax deducted at source on qualifying payments."
        "EXCISE" -> "Excise Duty on alcohol, tobacco, and specified goods."
        else     -> ""
    }
}

// ── Row Mappers ───────────────────────────────────────────────────────────────

fun ResultRow.toRateResponse() = TaxRateResponse(
    id           = this[TaxRatesTable.id],
    businessId   = this[TaxRatesTable.businessId],
    taxType      = this[TaxRatesTable.taxType],
    name         = this[TaxRatesTable.name],
    rate         = this[TaxRatesTable.rate],
    ratePercent  = Math.round(this[TaxRatesTable.rate] * 100 * 100.0) / 100.0,
    isInclusive  = this[TaxRatesTable.isInclusive],
    isActive     = this[TaxRatesTable.isActive],
    appliesTo    = this[TaxRatesTable.appliesTo],
    description  = this[TaxRatesTable.description],
    createdAt    = this[TaxRatesTable.createdAt].toString()
)

fun ResultRow.toTaxLineResponse() = TaxLineResponse(
    id            = this[OrderTaxLinesTable.id],
    taxRateId     = this[OrderTaxLinesTable.taxRateId],
    taxType       = this[OrderTaxLinesTable.taxType],
    taxName       = this[OrderTaxLinesTable.taxName],
    rate          = this[OrderTaxLinesTable.rate],
    ratePercent   = Math.round(this[OrderTaxLinesTable.rate] * 100 * 100.0) / 100.0,
    taxableAmount = this[OrderTaxLinesTable.taxableAmount],
    taxAmount     = this[OrderTaxLinesTable.taxAmount]
)

fun ResultRow.toRemittanceResponse() = TaxRemittanceResponse(
    id            = this[TaxRemittancesTable.id],
    businessId    = this[TaxRemittancesTable.businessId],
    taxType       = this[TaxRemittancesTable.taxType],
    periodStart   = this[TaxRemittancesTable.periodStart].toString(),
    periodEnd     = this[TaxRemittancesTable.periodEnd].toString(),
    taxableAmount = this[TaxRemittancesTable.taxableAmount],
    taxAmount     = this[TaxRemittancesTable.taxAmount],
    status        = this[TaxRemittancesTable.status],
    receiptNumber = this[TaxRemittancesTable.receiptNumber],
    filedAt       = this[TaxRemittancesTable.filedAt]?.toString(),
    paidAt        = this[TaxRemittancesTable.paidAt]?.toString(),
    notes         = this[TaxRemittancesTable.notes],
    createdAt     = this[TaxRemittancesTable.createdAt].toString()
)
