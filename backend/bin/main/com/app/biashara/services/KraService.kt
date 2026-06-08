package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.atStartOfDayIn
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Base64

// ─────────────────────────────────────────────────────────────────────────────
// KRA iTax Return Service
//
// Handles: KRA profile management, VAT3/TOT/WHT return generation,
// CSV export in KRA-required format, compliance scoring
// ─────────────────────────────────────────────────────────────────────────────

class KraService {

    // ── KRA Profile CRUD ──────────────────────────────────────────────────────

    fun getProfile(businessId: String): KraProfileResponse? = transaction {
        KraProfilesTable.select { KraProfilesTable.businessId eq businessId }
            .firstOrNull()?.toKraResponse()
    }

    fun saveProfile(businessId: String, req: KraProfileRequest): ApiResponse<KraProfileResponse> = transaction {
        val existing = KraProfilesTable.select { KraProfilesTable.businessId eq businessId }.firstOrNull()
        val now      = Clock.System.now()

        if (existing != null) {
            KraProfilesTable.update({ KraProfilesTable.businessId eq businessId }) {
                it[KraProfilesTable.pin]                 = req.pin.uppercase().trim()
                it[KraProfilesTable.companyName]         = req.companyName
                it[KraProfilesTable.vatNumber] = req.vatNumber
                it[KraProfilesTable.etimsSdcId] = req.etimsSdcId
                it[KraProfilesTable.etimsDeviceSerialNo] = req.etimsDeviceSerialNo
                it[KraProfilesTable.etimsEnvironment] = req.etimsEnvironment
                it[updatedAt]           = now
            }
        } else {
            KraProfilesTable.insert {
                it[id]                  = generateId()
                it[KraProfilesTable.businessId] = businessId
                it[KraProfilesTable.pin]                 = req.pin.uppercase().trim()
                it[KraProfilesTable.companyName]         = req.companyName
                it[KraProfilesTable.vatNumber] = req.vatNumber
                it[KraProfilesTable.etimsSdcId] = req.etimsSdcId
                it[KraProfilesTable.etimsDeviceSerialNo] = req.etimsDeviceSerialNo
                it[KraProfilesTable.etimsEnvironment] = req.etimsEnvironment
                it[createdAt]           = now
                it[updatedAt]           = now
            }
        }
        val profile = KraProfilesTable.select { KraProfilesTable.businessId eq businessId }.first().toKraResponse()
        ApiResponse(true, data = profile, message = "KRA profile saved")
    }

    // ── VAT3 Return Generation ────────────────────────────────────────────────

    fun generateVat3Return(businessId: String, req: Vat3ReturnRequest): ApiResponse<Vat3ReturnResponse> = transaction {
        val zId    = kotlinx.datetime.TimeZone.of("Africa/Nairobi")
        val start  = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
        val end    = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).plus(1, kotlinx.datetime.DateTimeUnit.MONTH).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))

        // Aggregate VAT tax lines for the period
        val vatLines = (OrderTaxLinesTable innerJoin OrdersTable)
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrderTaxLinesTable.taxType eq "VAT") and
                (OrdersTable.createdAt greaterEq start) and
                (OrdersTable.createdAt less end)
            }

        val standardRatedSales = vatLines.sumOf { it[OrderTaxLinesTable.taxableAmount] }
        val outputVat          = vatLines.sumOf { it[OrderTaxLinesTable.taxAmount] }
        val totalSales         = OrdersTable.slice(OrdersTable.subtotal.sum())
            .select { (OrdersTable.businessId eq businessId) and (OrdersTable.createdAt greaterEq start) and (OrdersTable.createdAt less end) }
            .firstOrNull()?.get(OrdersTable.subtotal.sum()) ?: 0.0

        // Input VAT would come from purchase invoice module (0 for now - claimable later)
        val inputVat    = 0.0
        val netVat      = outputVat - inputVat
        val dueDate     = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).plus(1, kotlinx.datetime.DateTimeUnit.MONTH).let { kotlinx.datetime.LocalDate(it.year, it.monthNumber, 20) }
        val periodLabel = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")[req.periodMonth] + " " + req.periodYear

        // Upsert return record
        val existingReturn = TaxReturnsTable.select {
            (TaxReturnsTable.businessId eq businessId) and
            (TaxReturnsTable.returnType eq "VAT3") and
            (TaxReturnsTable.periodYear eq req.periodYear) and
            (TaxReturnsTable.periodMonth eq req.periodMonth)
        }.firstOrNull()

        val id  = existingReturn?.get(TaxReturnsTable.id) ?: generateId()
        val now = Clock.System.now()

        if (existingReturn != null) {
            TaxReturnsTable.update({
                (TaxReturnsTable.businessId eq businessId) and
                (TaxReturnsTable.returnType eq "VAT3") and
                (TaxReturnsTable.periodYear eq req.periodYear) and
                (TaxReturnsTable.periodMonth eq req.periodMonth)
            }) {
                it[TaxReturnsTable.standardRatedSales] = standardRatedSales
                it[TaxReturnsTable.zeroRatedSales]     = 0.0
                it[TaxReturnsTable.exemptSales]        = 0.0
                it[TaxReturnsTable.outputVat]          = outputVat
                it[TaxReturnsTable.inputVat]           = inputVat
                it[TaxReturnsTable.netVatPayable]      = netVat
                it[TaxReturnsTable.grossReceipts]      = totalSales
                it[TaxReturnsTable.taxAmount]          = outputVat
                it[status]        = "GENERATED"
                it[generatedAt]   = now
            }
        } else {
            TaxReturnsTable.insert {
                it[TaxReturnsTable.id]                = id
                it[TaxReturnsTable.businessId]        = businessId
                it[returnType]                        = "VAT3"
                it[periodYear]                        = req.periodYear
                it[periodMonth]                       = req.periodMonth
                it[TaxReturnsTable.standardRatedSales] = standardRatedSales
                it[TaxReturnsTable.zeroRatedSales]    = 0.0
                it[TaxReturnsTable.exemptSales]       = 0.0
                it[TaxReturnsTable.outputVat]         = outputVat
                it[TaxReturnsTable.inputVat]          = inputVat
                it[TaxReturnsTable.netVatPayable]     = netVat
                it[TaxReturnsTable.grossReceipts]     = totalSales
                it[TaxReturnsTable.taxAmount]         = outputVat
                it[status]       = "GENERATED"
                it[generatedAt]  = now
                it[createdAt]    = now
            }
        }

        ApiResponse(true, data = Vat3ReturnResponse(
            id                   = id,
            businessId           = businessId,
            periodYear           = req.periodYear,
            periodMonth          = req.periodMonth,
            periodLabel          = periodLabel,
            dueDate              = dueDate.toString(),
            standardRatedSales   = standardRatedSales,
            zeroRatedSales       = 0.0,
            exemptSales          = 0.0,
            totalSales           = totalSales,
            outputVat            = outputVat,
            inputVat             = inputVat,
            netVatPayable        = netVat,
            status               = "GENERATED",
            iTaxAcknowledgementNo = null,
            csvDownloadReady     = true,
            generatedAt          = now.toString(),
            submittedAt          = null
        ))
    }

    // ── TOT Return Generation ─────────────────────────────────────────────────

    fun generateTotReturn(businessId: String, req: TotReturnRequest): ApiResponse<TotReturnResponse> = transaction {
        val zId   = kotlinx.datetime.TimeZone.of("Africa/Nairobi")
        val start = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
        val end   = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).plus(1, kotlinx.datetime.DateTimeUnit.MONTH).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))

        val grossReceipts = OrdersTable.slice(OrdersTable.subtotal.sum())
            .select { (OrdersTable.businessId eq businessId) and (OrdersTable.createdAt greaterEq start) and (OrdersTable.createdAt less end) }
            .firstOrNull()?.get(OrdersTable.subtotal.sum()) ?: 0.0

        val totAmount   = Math.round(grossReceipts * 0.015 * 100.0) / 100.0
        val dueDate     = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).plus(1, kotlinx.datetime.DateTimeUnit.MONTH).let { kotlinx.datetime.LocalDate(it.year, it.monthNumber, 20) }
        val periodLabel = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")[req.periodMonth] + " " + req.periodYear
        val id          = generateId()
        val now         = Clock.System.now()

        TaxReturnsTable.deleteWhere {
            (TaxReturnsTable.businessId eq businessId) and
            (TaxReturnsTable.returnType eq "TOT") and
            (TaxReturnsTable.periodYear eq req.periodYear) and
            (TaxReturnsTable.periodMonth eq req.periodMonth)
        }
        TaxReturnsTable.insert {
            it[TaxReturnsTable.id]          = id
            it[TaxReturnsTable.businessId]  = businessId
            it[returnType]                  = "TOT"
            it[periodYear]                  = req.periodYear
            it[periodMonth]                 = req.periodMonth
            it[TaxReturnsTable.grossReceipts] = grossReceipts
            it[TaxReturnsTable.taxAmount]   = totAmount
            it[status]      = "GENERATED"
            it[generatedAt] = now
            it[createdAt]   = now
        }
        ApiResponse(true, data = TotReturnResponse(
            id = id, businessId = businessId, periodYear = req.periodYear, periodMonth = req.periodMonth,
            periodLabel = periodLabel, dueDate = dueDate.toString(),
            grossReceipts = grossReceipts, totAmount = totAmount, status = "GENERATED",
            iTaxAcknowledgementNo = null, csvDownloadReady = true,
            generatedAt = now.toString(), submittedAt = null
        ))
    }

    // ── WHT Return Generation ─────────────────────────────────────────────────

    fun generateWhtReturn(businessId: String, req: WhtReturnRequest): ApiResponse<WhtReturnResponse> = transaction {
        val zId   = kotlinx.datetime.TimeZone.of("Africa/Nairobi")
        val start = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
        val end   = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).plus(1, kotlinx.datetime.DateTimeUnit.MONTH).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))

        val whtLines = (OrderTaxLinesTable innerJoin OrdersTable)
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrderTaxLinesTable.taxType eq "WHT") and
                (OrdersTable.createdAt greaterEq start) and
                (OrdersTable.createdAt less end)
            }

        val totalPayments = whtLines.sumOf { it[OrderTaxLinesTable.taxableAmount] }
        val whtAmount     = whtLines.sumOf { it[OrderTaxLinesTable.taxAmount] }
        val dueDate       = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).plus(1, kotlinx.datetime.DateTimeUnit.MONTH).let { kotlinx.datetime.LocalDate(it.year, it.monthNumber, 20) }
        val periodLabel   = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")[req.periodMonth] + " " + req.periodYear
        val id            = generateId()
        val now           = Clock.System.now()

        TaxReturnsTable.deleteWhere {
            (TaxReturnsTable.businessId eq businessId) and
            (TaxReturnsTable.returnType eq "WHT") and
            (TaxReturnsTable.periodYear eq req.periodYear) and
            (TaxReturnsTable.periodMonth eq req.periodMonth)
        }
        TaxReturnsTable.insert {
            it[TaxReturnsTable.id]          = id
            it[TaxReturnsTable.businessId]  = businessId
            it[returnType]                  = "WHT"
            it[periodYear]                  = req.periodYear
            it[periodMonth]                 = req.periodMonth
            it[TaxReturnsTable.grossReceipts] = totalPayments
            it[TaxReturnsTable.taxAmount]   = whtAmount
            it[status]      = "GENERATED"
            it[generatedAt] = now
            it[createdAt]   = now
        }
        ApiResponse(true, data = WhtReturnResponse(
            id = id, businessId = businessId, periodYear = req.periodYear, periodMonth = req.periodMonth,
            periodLabel = periodLabel, dueDate = dueDate.toString(), totalPayments = totalPayments,
            whtAmount = whtAmount, status = "GENERATED", iTaxAcknowledgementNo = null,
            csvDownloadReady = true, generatedAt = now.toString(), submittedAt = null
        ))
    }

    // ── CSV Export (KRA iTax format) ──────────────────────────────────────────

    fun generateCsv(businessId: String, req: CsvExportRequest): ApiResponse<CsvExportResponse> = transaction {
        val zId   = kotlinx.datetime.TimeZone.of("Africa/Nairobi")
        val start = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
        val end   = kotlinx.datetime.LocalDate(req.periodYear, req.periodMonth, 1).plus(1, kotlinx.datetime.DateTimeUnit.MONTH).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
        val period = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")[req.periodMonth] + "_" + req.periodYear

        val (csvContent, rowCount, fileName) = when (req.returnType) {
            "VAT3"           -> generateVat3Csv(businessId, start, end, period)
            "TOT"            -> generateTotCsv(businessId, start, end, period)
            "WHT"            -> generateWhtCsv(businessId, start, end, period)
            "ETIMS_INVOICES" -> generateEtimsCsv(businessId, start, end, period)
            else             -> Triple("", 0, "export.csv")
        }

        val encoded = Base64.getEncoder().encodeToString(csvContent.toByteArray())

        ApiResponse(true, data = CsvExportResponse(
            fileName     = fileName,
            format       = "KRA_CSV",
            rowCount     = rowCount,
            periodLabel  = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")[req.periodMonth] + " " + req.periodYear,
            downloadBase64 = encoded,
            contentType  = "text/csv",
            uploadInstructions = KraUploadInstructions(
                portalUrl       = "https://itax.kra.go.ke",
                menuPath        = kraMenuPath(req.returnType),
                fileFormatRequired = "CSV (comma-delimited, .csv)",
                steps           = kraUploadSteps(req.returnType)
            )
        ))
    }

    // ── KRA VAT3 CSV (standard KRA format) ───────────────────────────────────

    private fun generateVat3Csv(businessId: String, start: kotlinx.datetime.Instant, end: kotlinx.datetime.Instant, period: String): Triple<String, Int, String> {
        val sb = StringBuilder()
        // KRA VAT3 header row
        sb.appendLine("\"TRANSACTION DATE\",\"INVOICE NUMBER\",\"CUSTOMER NAME\",\"CUSTOMER PIN\",\"TAXABLE AMOUNT (16%)\",\"VAT AMOUNT (16%)\",\"ZERO RATED AMOUNT\",\"EXEMPT AMOUNT\",\"TOTAL AMOUNT\"")

        val orders = (OrdersTable leftJoin OrderTaxLinesTable)
            .slice(OrdersTable.columns + OrderTaxLinesTable.taxableAmount + OrderTaxLinesTable.taxAmount)
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrdersTable.createdAt greaterEq start) and
                (OrdersTable.createdAt less end)
            }
            .distinctBy { it[OrdersTable.id] }

        var rowCount = 0
        orders.forEach { row ->
            val taxable = row.getOrNull(OrderTaxLinesTable.taxableAmount) ?: row[OrdersTable.subtotal]
            val vatAmt  = row.getOrNull(OrderTaxLinesTable.taxAmount) ?: 0.0
            val total   = taxable + vatAmt
            val date    = row[OrdersTable.createdAt].toString().take(10)
            sb.appendLine("\"$date\",\"${row[OrdersTable.orderNumber]}\",\"${row[OrdersTable.customerName].replace("\"","")}\",\"\",\"${f2(taxable)}\",\"${f2(vatAmt)}\",\"0.00\",\"0.00\",\"${f2(total)}\"")
            rowCount++
        }
        return Triple(sb.toString(), rowCount, "VAT3_${period}.csv")
    }

    private fun generateTotCsv(businessId: String, start: kotlinx.datetime.Instant, end: kotlinx.datetime.Instant, period: String): Triple<String, Int, String> {
        val sb = StringBuilder()
        sb.appendLine("\"TRANSACTION DATE\",\"INVOICE NUMBER\",\"CUSTOMER NAME\",\"GROSS RECEIPTS\",\"TOT AMOUNT (1.5%)\"")
        val orders = OrdersTable.select {
            (OrdersTable.businessId eq businessId) and
            (OrdersTable.createdAt greaterEq start) and
            (OrdersTable.createdAt less end)
        }
        var rowCount = 0
        orders.forEach { row ->
            val receipts = row[OrdersTable.subtotal]
            val tot      = Math.round(receipts * 0.015 * 100.0) / 100.0
            val date     = row[OrdersTable.createdAt].toString().take(10)
            sb.appendLine("\"$date\",\"${row[OrdersTable.orderNumber]}\",\"${row[OrdersTable.customerName].replace("\"","")}\",\"${f2(receipts)}\",\"${f2(tot)}\"")
            rowCount++
        }
        return Triple(sb.toString(), rowCount, "TOT_${period}.csv")
    }

    private fun generateWhtCsv(businessId: String, start: kotlinx.datetime.Instant, end: kotlinx.datetime.Instant, period: String): Triple<String, Int, String> {
        val sb = StringBuilder()
        sb.appendLine("\"TRANSACTION DATE\",\"INVOICE NUMBER\",\"PAYEE NAME\",\"PAYEE PIN\",\"GROSS PAYMENT\",\"WHT RATE\",\"WHT AMOUNT\"")
        val lines = (OrderTaxLinesTable innerJoin OrdersTable)
            .select {
                (OrdersTable.businessId eq businessId) and
                (OrderTaxLinesTable.taxType eq "WHT") and
                (OrdersTable.createdAt greaterEq start) and
                (OrdersTable.createdAt less end)
            }
        var rowCount = 0
        lines.forEach { row ->
            val taxable = row[OrderTaxLinesTable.taxableAmount]
            val wht     = row[OrderTaxLinesTable.taxAmount]
            val date    = row[OrdersTable.createdAt].toString().take(10)
            sb.appendLine("\"$date\",\"${row[OrdersTable.orderNumber]}\",\"${row[OrdersTable.customerName].replace("\"","")}\",\"\",\"${f2(taxable)}\",\"3.00%\",\"${f2(wht)}\"")
            rowCount++
        }
        return Triple(sb.toString(), rowCount, "WHT_${period}.csv")
    }

    private fun generateEtimsCsv(businessId: String, start: kotlinx.datetime.Instant, end: kotlinx.datetime.Instant, period: String): Triple<String, Int, String> {
        val sb = StringBuilder()
        sb.appendLine("\"DATE\",\"INVOICE NO\",\"ETIMS NO\",\"CUSTOMER\",\"TAXABLE\",\"VAT\",\"TOTAL\",\"STATUS\",\"QR CODE URL\"")
        val invoices = EtimsInvoicesTable.select {
            (EtimsInvoicesTable.businessId eq businessId) and
            (EtimsInvoicesTable.createdAt greaterEq start) and
            (EtimsInvoicesTable.createdAt less end)
        }
        var rowCount = 0
        invoices.forEach { row ->
            val date = row[EtimsInvoicesTable.createdAt].toString().take(10)
            sb.appendLine("\"$date\",\"${row[EtimsInvoicesTable.invoiceNumber]}\",\"${row[EtimsInvoicesTable.etimsInvoiceNumber] ?: ""}\",\"\",\"${f2(row[EtimsInvoicesTable.taxableAmount])}\",\"${f2(row[EtimsInvoicesTable.taxAmount])}\",\"${f2(row[EtimsInvoicesTable.totalAmount])}\",\"${row[EtimsInvoicesTable.status]}\",\"${row[EtimsInvoicesTable.qrCodeContent] ?: ""}\"")
            rowCount++
        }
        return Triple(sb.toString(), rowCount, "eTIMS_Invoices_${period}.csv")
    }

    // ── Compliance Status ─────────────────────────────────────────────────────

    fun getComplianceStatus(businessId: String): ApiResponse<KraComplianceStatus> = transaction {
        val profile = KraProfilesTable.select { KraProfilesTable.businessId eq businessId }.firstOrNull()
        val today   = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.of("Africa/Nairobi")).date

        // Check last 3 months for pending returns
        val pending = mutableListOf<PendingReturn>()
        for (i in 1..3) {
            val m = today.monthNumber - i; val y = today.year + (m - 1) / 12; val checkDate = kotlinx.datetime.LocalDate(y, ((m - 1 + 12) % 12) + 1, 1)
            val vatDue    = kotlinx.datetime.LocalDate(checkDate.year, checkDate.monthNumber, 1).plus(1, kotlinx.datetime.DateTimeUnit.MONTH).let { kotlinx.datetime.LocalDate(it.year, it.monthNumber, 20) }
            val isOverdue = today > vatDue
            val filed     = TaxReturnsTable.select {
                (TaxReturnsTable.businessId eq businessId) and
                (TaxReturnsTable.returnType eq "VAT3") and
                (TaxReturnsTable.periodYear eq checkDate.year) and
                (TaxReturnsTable.periodMonth eq checkDate.monthNumber) and
                (TaxReturnsTable.status inList listOf("SUBMITTED","ACKNOWLEDGED"))
            }.count() > 0L

            if (!filed) {
                val est = OrdersTable.slice(OrdersTable.subtotal.sum())
                    .select {
                        val start = kotlinx.datetime.LocalDate(checkDate.year, checkDate.monthNumber, 1).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
                        val end   = kotlinx.datetime.LocalDate(checkDate.year, checkDate.monthNumber, 1).plus(1, kotlinx.datetime.DateTimeUnit.MONTH).atStartOfDayIn(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
                        (OrdersTable.businessId eq businessId) and
                        (OrdersTable.createdAt greaterEq start) and
                        (OrdersTable.createdAt less end)
                    }.firstOrNull()?.get(OrdersTable.subtotal.sum()) ?: 0.0
                pending.add(PendingReturn(
                    returnType       = "VAT3",
                    period           = "${checkDate.month.name.take(3)} ${checkDate.year}",
                    dueDate          = vatDue.toString(),
                    isOverdue        = isOverdue,
                    estimatedAmount  = Math.round(est * 0.16 * 100.0) / 100.0
                ))
            }
        }

        // eTIMS transmission rate
        val totalInvoices = EtimsInvoicesTable.select { EtimsInvoicesTable.businessId eq businessId }.count()
        val transmitted   = EtimsInvoicesTable.select {
            (EtimsInvoicesTable.businessId eq businessId) and (EtimsInvoicesTable.status eq "TRANSMITTED")
        }.count()
        val rate = if (totalInvoices > 0) (transmitted.toDouble() / totalInvoices) else 1.0

        // Score: 100 base, -20 per unfiled return, -20 if not eTIMS registered
        var score = 100
        score -= pending.filter { !it.isOverdue }.size * 10
        score -= pending.filter { it.isOverdue }.size * 20
        if (profile?.get(KraProfilesTable.etimsSdcId) == null) score -= 20
        score = score.coerceAtLeast(0)

        val recommendations = mutableListOf<String>()
        if (pending.any { it.isOverdue }) recommendations.add("⚠️ You have overdue VAT returns. File immediately to avoid penalties (KES 10,000 or 5% of tax due).")
        if (profile?.get(KraProfilesTable.etimsSdcId) == null) recommendations.add("📱 Register on KRA eTIMS portal to get your SDC ID and start transmitting invoices in real-time.")
        if (rate < 0.95 && totalInvoices > 0) recommendations.add("📊 Only ${(rate * 100).toInt()}% of invoices transmitted to eTIMS. Retransmit failed invoices.")
        if (pending.size > 0 && !pending.any { it.isOverdue }) recommendations.add("📅 You have ${pending.size} VAT return(s) due soon. File before the 20th to avoid late penalties.")

        ApiResponse(true, data = KraComplianceStatus(
            businessId             = businessId,
            pin                    = profile?.get(KraProfilesTable.pin),
            isEtimsRegistered      = profile?.get(KraProfilesTable.etimsSdcId) != null,
            isVatRegistered        = profile?.get(KraProfilesTable.vatNumber) != null,
            isTotRegistered        = false, // set via profile flag in future
            pendingReturns         = pending.filter { !it.isOverdue },
            overdueReturns         = pending.filter { it.isOverdue },
            lastEtimsTransmission  = EtimsInvoicesTable.select {
                (EtimsInvoicesTable.businessId eq businessId) and (EtimsInvoicesTable.status eq "TRANSMITTED")
            }.orderBy(EtimsInvoicesTable.submittedAt, SortOrder.DESC).firstOrNull()?.get(EtimsInvoicesTable.submittedAt)?.toString(),
            etimsTransmissionRate  = Math.round(rate * 10000.0) / 10000.0,
            complianceScore        = score,
            recommendations        = recommendations
        ))
    }

    fun markReturnSubmitted(businessId: String, returnId: String, ackNo: String): ApiResponse<Unit> = transaction {
        val updated = TaxReturnsTable.update({
            (TaxReturnsTable.id eq returnId) and (TaxReturnsTable.businessId eq businessId)
        }) {
            it[status]                 = "SUBMITTED"
            it[iTaxAcknowledgementNo]  = ackNo
            it[submittedAt]            = Clock.System.now()
        }
        if (updated == 0) ApiResponse(false, message = "Return not found")
        else ApiResponse(true, message = "Return marked as submitted")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun f2(d: Double) = "%.2f".format(d)

    private fun kraMenuPath(returnType: String) = when (returnType) {
        "VAT3" -> listOf("Returns", "File Returns", "VAT", "VAT3")
        "TOT"  -> listOf("Returns", "File Returns", "Turnover Tax", "TOT Monthly")
        "WHT"  -> listOf("Returns", "File Returns", "Withholding Tax", "WHT Monthly")
        else   -> listOf("Returns", "File Returns")
    }

    private fun kraUploadSteps(returnType: String) = listOf(
        "Log in to iTax at https://itax.kra.go.ke using your KRA PIN and password",
        "Navigate to: ${kraMenuPath(returnType).joinToString(" → ")}",
        "Select the return period (month and year)",
        "Click 'Upload CSV' and select the downloaded .csv file",
        "Review the imported figures and click 'Submit'",
        "Save the acknowledgement number shown on-screen",
        "Return to Biashara360 and enter the acknowledgement number to mark as submitted"
    )

    private fun ResultRow.toKraResponse() = KraProfileResponse(
        id                  = this[KraProfilesTable.id],
        businessId          = this[KraProfilesTable.businessId],
        pin                 = this[KraProfilesTable.pin],
        companyName         = this[KraProfilesTable.companyName],
        vatNumber           = this[KraProfilesTable.vatNumber],
        etimsSdcId          = this[KraProfilesTable.etimsSdcId],
        etimsDeviceSerialNo = this[KraProfilesTable.etimsDeviceSerialNo],
        etimsEnvironment    = this[KraProfilesTable.etimsEnvironment],
        isVerified          = this[KraProfilesTable.isVerified],
        lastSyncAt          = this[KraProfilesTable.lastSyncAt]?.toString(),
        createdAt           = this[KraProfilesTable.createdAt].toString()
    )
}
