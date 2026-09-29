package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import com.app.biashara.security.TokenCipher
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.ApplicationConfig
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ReportSchedulerService(
    private val config: ApplicationConfig,
    private val httpClient: HttpClient,
    private val emailService: EmailService,
    private val reportService: ReportService,
    private val expenseService: ExpenseService,
    private val productService: ProductService,
    private val auditLogService: AuditLogService? = null
) {
    private val logger = LoggerFactory.getLogger(ReportSchedulerService::class.java)
    private val systemWaToken = config.propertyOrNull("whatsapp.otpToken")?.getString()?.trim().orEmpty()
    private val systemWaPhoneId = config.propertyOrNull("whatsapp.otpPhoneNumberId")?.getString()?.trim().orEmpty()
    private val tokenCipher: TokenCipher? = runCatching {
        val raw = config.propertyOrNull("social.tokenEncryptionKey")?.getString()?.trim()
            ?: System.getenv("SOCIAL_TOKEN_ENCRYPTION_KEY")?.trim()
        raw?.takeIf { it.isNotBlank() }?.let { TokenCipher(it) }
    }.getOrNull()
    private val naf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private var executorService: ScheduledExecutorService? = null

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    fun startScheduler() {
        if (executorService != null) return
        logger.info("[ReportScheduler] Starting background report scheduler service...")
        executorService = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "biashara-report-scheduler").apply { isDaemon = true }
        }
        // Run every 60 seconds, initial delay 20 seconds
        executorService?.scheduleAtFixedRate({
            try {
                checkAndRunDueSchedules()
            } catch (e: Throwable) {
                logger.error("[ReportScheduler] Unexpected error during schedule tick: ${e.message}", e)
            }
        }, 20, 60, TimeUnit.SECONDS)
    }

    fun stopScheduler() {
        logger.info("[ReportScheduler] Stopping background report scheduler service...")
        executorService?.shutdown()
        try {
            if (executorService?.awaitTermination(5, TimeUnit.SECONDS) == false) {
                executorService?.shutdownNow()
            }
        } catch (_: InterruptedException) {
            executorService?.shutdownNow()
        }
        executorService = null
    }

    // ─── Scheduled Tick Execution ──────────────────────────────────────────────

    private fun checkAndRunDueSchedules() {
        val eatZone = ZoneId.of("Africa/Nairobi")
        val nowEat = ZonedDateTime.now(eatZone)
        val currentHHmm = nowEat.format(DateTimeFormatter.ofPattern("HH:mm"))
        val currentDayOfWeek = nowEat.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        val currentDayOfMonth = nowEat.dayOfMonth

        val activeSchedules = transaction {
            ReportSchedulesTable
                .select { ReportSchedulesTable.isActive eq true }
                .map { toResponse(it) }
        }

        for (schedule in activeSchedules) {
            if (schedule.timeOfDay != currentHHmm) continue

            val lastRunAt = schedule.lastRunAt?.let {
                try {
                    java.time.Instant.parse(it).atZone(eatZone)
                } catch (_: Exception) { null }
            }

            val isDue = when (schedule.frequency.uppercase()) {
                "DAILY" -> {
                    // Only run if not already run today
                    lastRunAt == null || lastRunAt.toLocalDate() != nowEat.toLocalDate()
                }
                "WEEKLY" -> {
                    val targetDay = schedule.dayOfWeek ?: 1
                    targetDay == currentDayOfWeek && (lastRunAt == null || java.time.temporal.ChronoUnit.DAYS.between(lastRunAt.toLocalDate(), nowEat.toLocalDate()) >= 6)
                }
                "MONTHLY" -> {
                    val targetDay = schedule.dayOfMonth ?: 1
                    targetDay == currentDayOfMonth && (lastRunAt == null || java.time.temporal.ChronoUnit.DAYS.between(lastRunAt.toLocalDate(), nowEat.toLocalDate()) >= 25)
                }
                else -> false
            }

            if (isDue) {
                logger.info("[ReportScheduler] Triggering scheduled report: '${schedule.name}' (${schedule.id}) for business ${schedule.businessId}")
                try {
                    executeSchedule(schedule.id, schedule.businessId, isManualTest = false)
                } catch (e: Exception) {
                    logger.error("[ReportScheduler] Error running schedule ${schedule.id}: ${e.message}", e)
                }
            }
        }
    }

    // ─── CRUD Operations ───────────────────────────────────────────────────────

    fun listSchedules(businessId: String): List<ReportScheduleResponse> = transaction {
        ReportSchedulesTable
            .select { ReportSchedulesTable.businessId eq businessId }
            .orderBy(ReportSchedulesTable.createdAt, SortOrder.DESC)
            .map { toResponse(it) }
    }

    fun getSchedule(id: String, businessId: String): ReportScheduleResponse? = transaction {
        ReportSchedulesTable
            .select { (ReportSchedulesTable.id eq id) and (ReportSchedulesTable.businessId eq businessId) }
            .firstOrNull()
            ?.let { toResponse(it) }
    }

    fun createSchedule(businessId: String, req: CreateReportScheduleRequest): ApiResponse<ReportScheduleResponse> = transaction {
        val validTypes = setOf("SALES_SUMMARY", "PAYMENTS", "PROFIT_LOSS", "LOW_STOCK")
        if (req.reportType.uppercase() !in validTypes) {
            return@transaction ApiResponse(false, message = "Invalid reportType. Choose SALES_SUMMARY, PAYMENTS, PROFIT_LOSS, or LOW_STOCK")
        }

        val validFreqs = setOf("DAILY", "WEEKLY", "MONTHLY")
        if (req.frequency.uppercase() !in validFreqs) {
            return@transaction ApiResponse(false, message = "Invalid frequency. Choose DAILY, WEEKLY, or MONTHLY")
        }

        val validChannels = setOf("EMAIL", "WHATSAPP", "EMAIL,WHATSAPP")
        val normalizedChannels = req.channels.uppercase().replace(" ", "")
        if (normalizedChannels !in validChannels) {
            return@transaction ApiResponse(false, message = "Invalid channels. Choose EMAIL, WHATSAPP, or EMAIL,WHATSAPP")
        }

        val timeOfDay = req.timeOfDay.trim()
        if (!timeOfDay.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) {
            return@transaction ApiResponse(false, message = "Invalid timeOfDay format. Must be HH:mm (e.g. 20:00)")
        }

        val newId = generateId()
        val now = Clock.System.now()

        ReportSchedulesTable.insert {
            it[id] = newId
            it[ReportSchedulesTable.businessId] = businessId
            it[branchId] = req.branchId?.trim()?.takeIf { b -> b.isNotBlank() }
            it[name] = req.name.trim().ifBlank { "${req.frequency} ${req.reportType.replace('_', ' ')}" }
            it[reportType] = req.reportType.uppercase()
            it[frequency] = req.frequency.uppercase()
            it[ReportSchedulesTable.timeOfDay] = timeOfDay
            it[dayOfWeek] = req.dayOfWeek
            it[dayOfMonth] = req.dayOfMonth
            it[channels] = normalizedChannels
            it[emailRecipients] = req.emailRecipients?.trim()?.takeIf { e -> e.isNotBlank() }
            it[whatsappRecipients] = req.whatsappRecipients?.trim()?.takeIf { w -> w.isNotBlank() }
            it[isActive] = req.isActive
            it[createdAt] = now
            it[updatedAt] = now
        }

        auditLogService?.logEvent(businessId, null, null, "CREATE_REPORT_SCHEDULE", null, "Created report schedule: ${req.name}")
        val created = ReportSchedulesTable.select { ReportSchedulesTable.id eq newId }.first()
        ApiResponse(true, data = toResponse(created), message = "Report schedule created successfully")
    }

    fun updateSchedule(id: String, businessId: String, req: UpdateReportScheduleRequest): ApiResponse<ReportScheduleResponse> = transaction {
        val scheduleExists = ReportSchedulesTable
            .select { (ReportSchedulesTable.id eq id) and (ReportSchedulesTable.businessId eq businessId) }
            .firstOrNull() != null
        if (!scheduleExists) return@transaction ApiResponse(false, message = "Report schedule not found")

        val now = Clock.System.now()
        ReportSchedulesTable.update({ (ReportSchedulesTable.id eq id) and (ReportSchedulesTable.businessId eq businessId) }) {
            req.name?.let { n -> it[name] = n.trim() }
            req.reportType?.let { t ->
                val upper = t.uppercase()
                if (upper in setOf("SALES_SUMMARY", "PAYMENTS", "PROFIT_LOSS", "LOW_STOCK")) it[reportType] = upper
            }
            req.frequency?.let { f ->
                val upper = f.uppercase()
                if (upper in setOf("DAILY", "WEEKLY", "MONTHLY")) it[frequency] = upper
            }
            req.timeOfDay?.let { tod ->
                if (tod.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))) it[timeOfDay] = tod
            }
            req.dayOfWeek?.let { dow -> it[dayOfWeek] = dow }
            req.dayOfMonth?.let { dom -> it[dayOfMonth] = dom }
            req.channels?.let { ch ->
                val norm = ch.uppercase().replace(" ", "")
                if (norm in setOf("EMAIL", "WHATSAPP", "EMAIL,WHATSAPP")) it[channels] = norm
            }
            if (req.branchId != null) {
                it[branchId] = req.branchId.trim().takeIf { b -> b.isNotBlank() }
            }
            if (req.emailRecipients != null) {
                it[emailRecipients] = req.emailRecipients.trim().takeIf { e -> e.isNotBlank() }
            }
            if (req.whatsappRecipients != null) {
                it[whatsappRecipients] = req.whatsappRecipients.trim().takeIf { w -> w.isNotBlank() }
            }
            req.isActive?.let { a -> it[isActive] = a }
            it[updatedAt] = now
        }

        auditLogService?.logEvent(businessId, null, null, "UPDATE_REPORT_SCHEDULE", null, "Updated report schedule: $id")
        val updated = ReportSchedulesTable.select { ReportSchedulesTable.id eq id }.first()
        ApiResponse(true, data = toResponse(updated), message = "Report schedule updated")
    }

    fun deleteSchedule(id: String, businessId: String): ApiResponse<Unit> = transaction {
        val deleted = ReportSchedulesTable.deleteWhere {
            (ReportSchedulesTable.id eq id) and (ReportSchedulesTable.businessId eq businessId)
        }
        if (deleted > 0) {
            auditLogService?.logEvent(businessId, null, null, "DELETE_REPORT_SCHEDULE", null, "Deleted report schedule: $id")
            ApiResponse(true, message = "Report schedule deleted")
        } else {
            ApiResponse(false, message = "Report schedule not found")
        }
    }

    fun toggleActive(id: String, businessId: String, isActive: Boolean): ApiResponse<ReportScheduleResponse> = transaction {
        val updated = ReportSchedulesTable.update({ (ReportSchedulesTable.id eq id) and (ReportSchedulesTable.businessId eq businessId) }) {
            it[ReportSchedulesTable.isActive] = isActive
            it[updatedAt] = Clock.System.now()
        }
        if (updated == 0) return@transaction ApiResponse(false, message = "Schedule not found")
        val row = ReportSchedulesTable.select { ReportSchedulesTable.id eq id }.first()
        ApiResponse(true, data = toResponse(row), message = if (isActive) "Schedule activated" else "Schedule paused")
    }

    fun getLogs(businessId: String, scheduleId: String? = null, limit: Int = 50): List<ReportScheduleLogResponse> = transaction {
        var query = ReportScheduleLogsTable.select { ReportScheduleLogsTable.businessId eq businessId }
        if (!scheduleId.isNullOrBlank()) {
            query = query.andWhere { ReportScheduleLogsTable.scheduleId eq scheduleId }
        }
        query.orderBy(ReportScheduleLogsTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .map {
                ReportScheduleLogResponse(
                    id = it[ReportScheduleLogsTable.id],
                    scheduleId = it[ReportScheduleLogsTable.scheduleId],
                    businessId = it[ReportScheduleLogsTable.businessId],
                    reportType = it[ReportScheduleLogsTable.reportType],
                    period = it[ReportScheduleLogsTable.period],
                    channels = it[ReportScheduleLogsTable.channels],
                    status = it[ReportScheduleLogsTable.status],
                    summaryText = it[ReportScheduleLogsTable.summaryText],
                    recipientsCount = it[ReportScheduleLogsTable.recipientsCount],
                    errorMessage = it[ReportScheduleLogsTable.errorMessage],
                    createdAt = it[ReportScheduleLogsTable.createdAt].toString()
                )
            }
    }

    // ─── Execution Logic (Scheduled or Send-Now) ──────────────────────────────

    fun executeSchedule(scheduleId: String, businessId: String, @Suppress("UNUSED_PARAMETER") isManualTest: Boolean = false): SendReportNowResult {
        val scheduleRow = transaction {
            ReportSchedulesTable
                .select { (ReportSchedulesTable.id eq scheduleId) and (ReportSchedulesTable.businessId eq businessId) }
                .firstOrNull()
        } ?: return SendReportNowResult(false, "Report schedule not found")

        val schedule = toResponse(scheduleRow)
        val businessRow = transaction {
            BusinessesTable.select { BusinessesTable.id eq businessId }.firstOrNull()
        } ?: return SendReportNowResult(false, "Business not found")

        val businessName = businessRow[BusinessesTable.name].ifBlank { "Biashara360 Merchant" }
        val businessEmail = businessRow[BusinessesTable.ownerEmail]
        val businessPhone = businessRow[BusinessesTable.ownerPhone]
        val currency = businessRow[BusinessesTable.currency].ifBlank { "KES" }

        // Date range calculation (EAT)
        val eatZone = ZoneId.of("Africa/Nairobi")
        val nowEat = ZonedDateTime.now(eatZone)
        val (startDate, endDate, periodLabel) = calculateReportRange(schedule.frequency, nowEat)

        // Generate report data and formatted payloads
        val generated = generateReportContent(
            reportType = schedule.reportType,
            businessId = businessId,
            branchId = schedule.branchId,
            businessName = businessName,
            currency = currency,
            startDate = startDate,
            endDate = endDate,
            periodLabel = periodLabel
        )

        val channels = schedule.channels.split(",").map { it.trim().uppercase() }
        val errors = mutableListOf<String>()
        val directWaUrls = mutableListOf<String>()
        var recipientsCount = 0

        // 1. Dispatch Email
        if ("EMAIL" in channels) {
            val emailList = parseEmailList(schedule.emailRecipients, businessEmail)
            if (emailList.isEmpty()) {
                errors.add("No email recipients found (owner email is also blank)")
            } else {
                for (email in emailList) {
                    recipientsCount++
                    val subject = "[$businessName] ${schedule.name} — $periodLabel"
                    val sendRes = emailService.sendHtmlEmail(email, subject, generated.html)
                    if (sendRes.isFailure) {
                        val errMsg = sendRes.exceptionOrNull()?.message ?: "SMTP delivery error"
                        logger.warn("[ReportScheduler] Email to $email failed: $errMsg")
                        errors.add("Email ($email): $errMsg")
                    } else {
                        logger.info("[ReportScheduler] Email report successfully sent to $email")
                    }
                }
            }
        }

        // 2. Dispatch WhatsApp
        if ("WHATSAPP" in channels) {
            val phoneList = parsePhoneList(schedule.whatsappRecipients, businessPhone)
            if (phoneList.isEmpty()) {
                errors.add("No WhatsApp phone recipients found")
            } else {
                for (phone in phoneList) {
                    recipientsCount++
                    val normalizedPhone = normalizePhone(phone)
                    val waUrl = "https://api.whatsapp.com/send?phone=$normalizedPhone&text=${urlEncode(generated.summaryText)}"
                    directWaUrls.add(waUrl)

                    // Attempt Meta WhatsApp Cloud API send
                    val waSent = sendWhatsAppText(businessId, normalizedPhone, generated.summaryText)
                    if (!waSent) {
                        logger.info("[ReportScheduler] WhatsApp Cloud API not configured or failed for $phone; provided direct click-to-chat URL.")
                    } else {
                        logger.info("[ReportScheduler] WhatsApp message delivered via Cloud API to $phone")
                    }
                }
            }
        }

        val status = when {
            errors.isEmpty() -> "SUCCESS"
            errors.size < recipientsCount -> "PARTIAL"
            else -> "FAILED"
        }
        val errCombined = if (errors.isNotEmpty()) errors.joinToString("; ") else null

        // Record execution log & update schedule status
        val logId = generateId()
        val nowInstant = Clock.System.now()
        transaction {
            ReportScheduleLogsTable.insert {
                it[id] = logId
                it[ReportScheduleLogsTable.scheduleId] = scheduleId
                it[ReportScheduleLogsTable.businessId] = businessId
                it[ReportScheduleLogsTable.reportType] = schedule.reportType
                it[period] = periodLabel
                it[ReportScheduleLogsTable.channels] = schedule.channels
                it[ReportScheduleLogsTable.status] = status
                it[summaryText] = generated.summaryText
                it[ReportScheduleLogsTable.recipientsCount] = recipientsCount
                it[errorMessage] = errCombined
                it[createdAt] = nowInstant
            }

            ReportSchedulesTable.update({ ReportSchedulesTable.id eq scheduleId }) {
                it[lastRunAt] = nowInstant
                it[lastStatus] = status
                it[lastError] = errCombined
                it[updatedAt] = nowInstant
            }
        }

        val success = status != "FAILED"
        val msg = when (status) {
            "SUCCESS" -> "Report dispatched to $recipientsCount recipient(s) successfully"
            "PARTIAL" -> "Report delivered with some warnings: $errCombined"
            else -> "Report delivery failed: $errCombined"
        }

        return SendReportNowResult(
            success = success,
            message = msg,
            logId = logId,
            summaryText = generated.summaryText,
            whatsappUrls = directWaUrls
        )
    }

    // ─── WhatsApp Dispatch Helper ─────────────────────────────────────────────

    private fun sendWhatsAppText(businessId: String, toPhone: String, messageText: String): Boolean {
        // First check if business has a connected WhatsApp channel
        val channelTokenAndPhone = transaction {
            SocialChannelsTable
                .select { (SocialChannelsTable.businessId eq businessId) and (SocialChannelsTable.platform eq "WHATSAPP") and (SocialChannelsTable.isActive eq true) }
                .firstOrNull()
                ?.let {
                    val phoneId = it[SocialChannelsTable.phoneNumberId] ?: it[SocialChannelsTable.externalId]
                    val rawToken = it[SocialChannelsTable.accessToken].takeIf { t -> t.isNotBlank() }
                    val decryptedToken = if (rawToken != null && TokenCipher.isEncrypted(rawToken)) {
                        try {
                            tokenCipher?.decrypt(rawToken) ?: rawToken
                        } catch (e: Exception) {
                            logger.warn("[ReportScheduler] Failed to decrypt WhatsApp merchant token: ${e.message}")
                            rawToken
                        }
                    } else rawToken
                    val token = decryptedToken ?: systemWaToken
                    token to phoneId
                }
        }

        val token = channelTokenAndPhone?.first ?: systemWaToken
        val phoneId = channelTokenAndPhone?.second ?: systemWaPhoneId

        if (token.isBlank() || phoneId.isBlank()) {
            return false
        }

        return try {
            val payload = """{"messaging_product":"whatsapp","to":"$toPhone","type":"text","text":{"body":${Json.encodeToString(messageText)}}}"""
            val res = kotlinx.coroutines.runBlocking {
                httpClient.post("https://graph.facebook.com/v20.0/$phoneId/messages") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(payload)
                }
            }
            res.status.isSuccess()
        } catch (e: Exception) {
            logger.warn("[ReportScheduler] Failed WhatsApp Cloud API send to $toPhone: ${e.message}")
            false
        }
    }

    // ─── Report Content Generators ─────────────────────────────────────────────

    private data class GeneratedReport(
        val summaryText: String,
        val html: String
    )

    private fun generateReportContent(
        reportType: String,
        businessId: String,
        branchId: String?,
        businessName: String,
        currency: String,
        startDate: String,
        endDate: String,
        periodLabel: String
    ): GeneratedReport {
        return when (reportType.uppercase()) {
            "PAYMENTS" -> generatePaymentsContent(businessId, businessName, currency, startDate, endDate, periodLabel)
            "PROFIT_LOSS" -> generateProfitLossContent(businessId, businessName, currency, startDate, endDate, periodLabel)
            "LOW_STOCK" -> generateLowStockContent(businessId, businessName, currency, periodLabel)
            else -> generateSalesSummaryContent(businessId, branchId, businessName, currency, startDate, endDate, periodLabel)
        }
    }

    private fun generateSalesSummaryContent(
        businessId: String,
        @Suppress("UNUSED_PARAMETER") branchId: String?,
        businessName: String,
        currency: String,
        startDate: String,
        endDate: String,
        periodLabel: String
    ): GeneratedReport {
        val orderReport = reportService.orderReport(businessId, startDate, endDate)
        val totalOrders = orderReport.totalOrders
        val totalRevenue = orderReport.totalValue
        val paidRevenue = orderReport.paidValue
        val paidOrders = orderReport.orders.filter { it.paymentStatus == "PAID" }
        val pendingOrders = orderReport.orders.filter { it.paymentStatus != "PAID" }

        val methodBreakdown = orderReport.byPaymentMethod.joinToString("\n") {
            "  • ${it.label}: $currency ${formatMoney(it.amount)} (${it.count} orders)"
        }.ifBlank { "  • No sales recorded" }

        val channelBreakdown = orderReport.byChannel.joinToString("\n") {
            "  • ${it.label}: $currency ${formatMoney(it.amount)} (${it.count} orders)"
        }.ifBlank { "  • In-Store: $currency ${formatMoney(totalRevenue)}" }

        // WhatsApp Text
        val waText = """
📊 *${businessName.uppercase()} — SALES REPORT*
🗓 *Period:* $periodLabel
⏱ *Generated:* ${currentDateTimeEat()}

*Executive Summary:*
💰 *Total Sales Value:* $currency ${formatMoney(totalRevenue)}
✅ *Paid & Collected:* $currency ${formatMoney(paidRevenue)}
⏳ *Pending / Unpaid:* $currency ${formatMoney(totalRevenue - paidRevenue)}
🛒 *Total Orders:* $totalOrders (${paidOrders.size} paid, ${pendingOrders.size} pending)

*Payment Methods:*
$methodBreakdown

*Sales Channels:*
$channelBreakdown

🌐 _Powered by Biashara360 — Multi-Channel Business Operations_
https://biashara360.co.ke
        """.trimIndent()

        // Branded HTML Email
        val html = buildBrandedHtmlEmail(
            title = "Sales Summary Report",
            businessName = businessName,
            periodLabel = periodLabel,
            kpis = listOf(
                KPI("Total Sales", "$currency ${formatMoney(totalRevenue)}", "#0F766E"),
                KPI("Paid & Collected", "$currency ${formatMoney(paidRevenue)}", "#16A34A"),
                KPI("Pending Amount", "$currency ${formatMoney(totalRevenue - paidRevenue)}", "#D97706"),
                KPI("Total Orders", "$totalOrders orders", "#3B82F6")
            ),
            sections = listOf(
                EmailSection(
                    title = "Payment Methods Breakdown",
                    headers = listOf("Payment Method", "Orders Count", "Total Amount"),
                    rows = orderReport.byPaymentMethod.map {
                        listOf(it.label, "${it.count}", "$currency ${formatMoney(it.amount)}")
                    }
                ),
                EmailSection(
                    title = "Sales Channels",
                    headers = listOf("Channel", "Orders Count", "Total Value"),
                    rows = orderReport.byChannel.map {
                        listOf(it.label, "${it.count}", "$currency ${formatMoney(it.amount)}")
                    }
                ),
                EmailSection(
                    title = "Recent Transactions (Sample)",
                    headers = listOf("Order #", "Customer", "Method", "Status", "Amount"),
                    rows = orderReport.orders.take(8).map {
                        listOf(it.orderNumber, it.customerName.ifBlank { "Walk-in" }, it.paymentMethod, it.paymentStatus, "$currency ${formatMoney(it.subtotal)}")
                    }
                )
            )
        )

        return GeneratedReport(waText, html)
    }

    private fun generatePaymentsContent(
        businessId: String,
        businessName: String,
        currency: String,
        startDate: String,
        endDate: String,
        periodLabel: String
    ): GeneratedReport {
        val paymentReport = reportService.paymentReport(businessId, startDate, endDate)
        val totalAmount = paymentReport.totalAmount
        val reconciledAmount = paymentReport.reconciledAmount
        val totalTxns = paymentReport.totalTransactions

        val methodSummary = paymentReport.byMethod.joinToString("\n") {
            "  • ${it.label}: $currency ${formatMoney(it.amount)} (${it.count} txns)"
        }.ifBlank { "  • No payments received" }

        val waText = """
💳 *${businessName.uppercase()} — PAYMENTS & COLLECTIONS*
🗓 *Period:* $periodLabel
⏱ *Generated:* ${currentDateTimeEat()}

*Collections Overview:*
💵 *Total Collected:* $currency ${formatMoney(totalAmount)}
🎯 *Reconciled:* $currency ${formatMoney(reconciledAmount)}
🔢 *Total Transactions:* $totalTxns

*Breakdown by Payment Method:*
$methodSummary

🌐 _Powered by Biashara360_
https://biashara360.co.ke
        """.trimIndent()

        val html = buildBrandedHtmlEmail(
            title = "Payments & Collections Report",
            businessName = businessName,
            periodLabel = periodLabel,
            kpis = listOf(
                KPI("Total Collected", "$currency ${formatMoney(totalAmount)}", "#16A34A"),
                KPI("Reconciled Amount", "$currency ${formatMoney(reconciledAmount)}", "#0F766E"),
                KPI("Total Transactions", "$totalTxns", "#3B82F6")
            ),
            sections = listOf(
                EmailSection(
                    title = "Payment Methods Breakdown",
                    headers = listOf("Payment Method", "Transactions", "Total Amount"),
                    rows = paymentReport.byMethod.map {
                        listOf(it.label, "${it.count}", "$currency ${formatMoney(it.amount)}")
                    }
                ),
                EmailSection(
                    title = "Recent Successful Collections",
                    headers = listOf("Ref / Code", "Payer", "Method", "Channel", "Amount"),
                    rows = paymentReport.payments.filter { it.status in setOf("SUCCESS", "COMPLETED", "PAID") }.take(10).map {
                        listOf(it.transactionCode, it.payerName.ifBlank { "Walk-in" }, it.method, it.channel, "$currency ${formatMoney(it.amount)}")
                    }
                )
            )
        )

        return GeneratedReport(waText, html)
    }

    private fun generateProfitLossContent(
        businessId: String,
        businessName: String,
        currency: String,
        startDate: String,
        endDate: String,
        periodLabel: String
    ): GeneratedReport {
        val summary = expenseService.getProfitSummary(businessId, startDate, endDate)
        val revenue = summary.totalRevenue
        val cogs = summary.totalCostOfGoods
        val grossProfit = summary.grossProfit
        val expenses = summary.totalExpenses
        val netProfit = summary.netProfit
        val netMargin = summary.netMargin

        val waText = """
📈 *${businessName.uppercase()} — PROFIT & LOSS SUMMARY*
🗓 *Period:* $periodLabel
⏱ *Generated:* ${currentDateTimeEat()}

*Executive Financial Snapshot:*
💰 *Total Revenue:* $currency ${formatMoney(revenue)}
📦 *Cost of Goods (COGS):* $currency ${formatMoney(cogs)}
✨ *Gross Profit:* $currency ${formatMoney(grossProfit)}
🧾 *Total Operating Expenses:* $currency ${formatMoney(expenses)}
🏆 *Net Profit:* $currency ${formatMoney(netProfit)}
📊 *Net Margin:* ${"%.1f".format(netMargin)}%

🌐 _Biashara360 Cloud POS & Enterprise Accounting_
https://biashara360.co.ke
        """.trimIndent()

        val html = buildBrandedHtmlEmail(
            title = "Profit & Loss Executive Report",
            businessName = businessName,
            periodLabel = periodLabel,
            kpis = listOf(
                KPI("Total Revenue", "$currency ${formatMoney(revenue)}", "#0F766E"),
                KPI("Cost of Goods", "$currency ${formatMoney(cogs)}", "#64748B"),
                KPI("Operating Expenses", "$currency ${formatMoney(expenses)}", "#DC2626"),
                KPI("Net Profit", "$currency ${formatMoney(netProfit)}", if (netProfit >= 0) "#16A34A" else "#DC2626")
            ),
            sections = listOf(
                EmailSection(
                    title = "Financial Performance Statement",
                    headers = listOf("Line Item", "Amount ($currency)", "Description"),
                    rows = listOf(
                        listOf("Total Sales Revenue", "$currency ${formatMoney(revenue)}", "Gross revenue from paid sales"),
                        listOf("Cost of Goods Sold (COGS)", "$currency ${formatMoney(cogs)}", "Wholesale inventory buying cost of items sold"),
                        listOf("Gross Profit", "$currency ${formatMoney(grossProfit)}", "Revenue minus COGS"),
                        listOf("Operating Expenses", "$currency ${formatMoney(expenses)}", "Rent, utilities, wages, supplies, overhead"),
                        listOf("Net Profit", "$currency ${formatMoney(netProfit)}", "Final profit before taxes"),
                        listOf("Net Profit Margin", "${"%.1f".format(netMargin)}%", "Net profit percentage of total revenue")
                    )
                )
            )
        )

        return GeneratedReport(waText, html)
    }

    private fun generateLowStockContent(
        businessId: String,
        businessName: String,
        currency: String,
        periodLabel: String
    ): GeneratedReport {
        val lowStockProducts = productService.getAll(businessId, null, lowStockOnly = true)
        val count = lowStockProducts.size
        val outOfStock = lowStockProducts.filter { it.currentStock <= 0 }

        val itemsSample = lowStockProducts.take(12).joinToString("\n") {
            val statusEmoji = if (it.currentStock <= 0) "🔴" else "🟡"
            "  $statusEmoji *${it.name}*: ${it.currentStock} units left (Min: ${it.lowStockThreshold})"
        }.ifBlank { "  🟢 All products are well-stocked above thresholds!" }

        val waText = """
⚠️ *${businessName.uppercase()} — INVENTORY & LOW STOCK ALERT*
🗓 *Status as of:* ${currentDateTimeEat()}

*Stock Summary:*
📦 *Low Stock Items:* $count items requiring attention
🚨 *Out of Stock:* ${outOfStock.size} items completely sold out

*Items to Reorder:*
$itemsSample

🌐 _Manage Purchase Invoices & Stock on Biashara360:_
https://biashara360.co.ke
        """.trimIndent()

        val html = buildBrandedHtmlEmail(
            title = "Inventory Alert: Low Stock & Reorder Notice",
            businessName = businessName,
            periodLabel = periodLabel.ifBlank { "Current Inventory Status" },
            kpis = listOf(
                KPI("Items Below Threshold", "$count items", "#D97706"),
                KPI("Out of Stock (Zero)", "${outOfStock.size} items", "#DC2626"),
                KPI("Status Alert", if (count == 0) "Optimal" else "Reorder Required", if (count == 0) "#16A34A" else "#D97706")
            ),
            sections = listOf(
                EmailSection(
                    title = "Items Requiring Purchase Reorder",
                    headers = listOf("Product", "SKU / Code", "Current Stock", "Min Threshold", "Buying Price"),
                    rows = lowStockProducts.take(25).map {
                        listOf(it.name, it.sku.ifBlank { "—" }, "${it.currentStock} units", "${it.lowStockThreshold}", "$currency ${formatMoney(it.buyingPrice)}")
                    }
                )
            )
        )

        return GeneratedReport(waText, html)
    }

    // ─── Branded HTML Email Builder ───────────────────────────────────────────

    private data class KPI(val label: String, val value: String, val color: String)
    private data class EmailSection(val title: String, val headers: List<String>, val rows: List<List<String>>)

    private fun buildBrandedHtmlEmail(
        title: String,
        businessName: String,
        periodLabel: String,
        kpis: List<KPI>,
        sections: List<EmailSection>
    ): String {
        val kpiCards = kpis.joinToString("") { k ->
            """
            <td style="padding: 10px; width: ${100 / kpis.size}%;">
                <div style="background: #ffffff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px; text-align: center; box-shadow: 0 1px 3px rgba(0,0,0,0.04);">
                    <div style="font-size: 11px; text-transform: uppercase; color: #64748b; font-weight: 600; letter-spacing: 0.5px;">${escapeHtml(k.label)}</div>
                    <div style="font-size: 18px; font-weight: 700; color: ${k.color}; margin-top: 6px;">${escapeHtml(k.value)}</div>
                </div>
            </td>
            """.trimIndent()
        }

        val sectionsHtml = sections.joinToString("") { s ->
            val headerCells = s.headers.joinToString("") { h ->
                """<th style="padding: 10px 14px; background: #f8fafc; color: #475569; font-size: 12px; font-weight: 600; text-align: left; border-bottom: 1px solid #e2e8f0;">${escapeHtml(h)}</th>"""
            }
            val bodyRows = if (s.rows.isEmpty()) {
                """<tr><td colspan="${s.headers.size}" style="padding: 16px; text-align: center; color: #94a3b8; font-size: 13px;">No data recorded in this period.</td></tr>"""
            } else {
                s.rows.joinToString("") { r ->
                    val cells = r.joinToString("") { c ->
                        """<td style="padding: 10px 14px; font-size: 13px; color: #1e293b; border-bottom: 1px solid #f1f5f9;">${escapeHtml(c)}</td>"""
                    }
                    """<tr>$cells</tr>"""
                }
            }
            """
            <div style="margin-top: 28px;">
                <h3 style="font-size: 15px; font-weight: 700; color: #0f172a; margin: 0 0 12px 0;">${escapeHtml(s.title)}</h3>
                <div style="border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; background: #ffffff;">
                    <table style="width: 100%; border-collapse: collapse;">
                        <thead><tr>$headerCells</tr></thead>
                        <tbody>$bodyRows</tbody>
                    </table>
                </div>
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>${escapeHtml(title)}</title>
        </head>
        <body style="margin: 0; padding: 24px 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b;">
            <div style="max-width: 640px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06); border: 1px solid #e2e8f0;">
                <!-- Header Banner -->
                <div style="background: linear-gradient(135deg, #0F766E 0%, #16A34A 100%); padding: 32px 28px; text-align: left; color: #ffffff;">
                    <div style="font-size: 12px; text-transform: uppercase; letter-spacing: 1.5px; opacity: 0.85; font-weight: 600;">Biashara360 Automated Report</div>
                    <h1 style="margin: 6px 0 0 0; font-size: 24px; font-weight: 800; color: #ffffff;">${escapeHtml(businessName)}</h1>
                    <div style="margin-top: 8px; font-size: 14px; opacity: 0.95;">${escapeHtml(title)} &bull; <strong>${escapeHtml(periodLabel)}</strong></div>
                </div>

                <!-- Content Area -->
                <div style="padding: 24px 28px;">
                    <!-- KPI Cards Grid -->
                    <table style="width: 100%; border-collapse: collapse; margin-bottom: 8px;">
                        <tr>$kpiCards</tr>
                    </table>

                    $sectionsHtml

                    <!-- Action Link -->
                    <div style="margin-top: 32px; padding: 20px; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 10px; text-align: center;">
                        <div style="font-size: 14px; font-weight: 600; color: #166534; margin-bottom: 8px;">Access Live Real-time Business Analytics</div>
                        <a href="https://biashara360.co.ke" style="display: inline-block; background: #0F766E; color: #ffffff; text-decoration: none; padding: 10px 24px; border-radius: 8px; font-size: 13px; font-weight: 600;">Log In to Biashara360 Dashboard</a>
                    </div>
                </div>

                <!-- Footer -->
                <div style="background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px 28px; text-align: center; font-size: 12px; color: #64748b;">
                    <div>Automated Report scheduled via <strong>Biashara360 Report Scheduler</strong></div>
                    <div style="margin-top: 4px;">Sent to authorized recipients &bull; Generated on ${currentDateTimeEat()} EAT</div>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun calculateReportRange(frequency: String, nowEat: ZonedDateTime): Triple<String, String, String> {
        val ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = nowEat.toLocalDate()

        return when (frequency.uppercase()) {
            "DAILY" -> {
                val str = today.format(ymd)
                Triple(str, str, "Today ($str)")
            }
            "WEEKLY" -> {
                val start = today.minusDays(6)
                Triple(start.format(ymd), today.format(ymd), "${start.format(ymd)} to ${today.format(ymd)} (Past 7 Days)")
            }
            "MONTHLY" -> {
                val start = today.withDayOfMonth(1)
                Triple(start.format(ymd), today.format(ymd), "${start.format(ymd)} to ${today.format(ymd)} (Month to Date)")
            }
            else -> {
                val str = today.format(ymd)
                Triple(str, str, str)
            }
        }
    }

    private fun parseEmailList(recipients: String?, fallbackEmail: String?): List<String> {
        val set = mutableSetOf<String>()
        recipients?.split(",", ";", "\n", "\r")?.forEach { token ->
            val cleaned = token.trim().lowercase()
            if (cleaned.contains("@") && cleaned.length in 5..100) set.add(cleaned)
        }
        if (set.isEmpty() && !fallbackEmail.isNullOrBlank()) {
            val fb = fallbackEmail.trim().lowercase()
            if (fb.contains("@")) set.add(fb)
        }
        return set.toList()
    }

    private fun parsePhoneList(recipients: String?, fallbackPhone: String?): List<String> {
        val list = mutableListOf<String>()
        recipients?.split(",", ";", "\n", "\r")?.forEach { token ->
            val cleaned = token.trim()
            if (cleaned.isNotBlank()) list.add(cleaned)
        }
        if (list.isEmpty() && !fallbackPhone.isNullOrBlank()) {
            list.add(fallbackPhone.trim())
        }
        return list
    }

    private fun normalizePhone(phone: String): String {
        val cleaned = phone.replace(Regex("[\\s\\-()+]"), "")
        return when {
            cleaned.startsWith("0") -> "254${cleaned.drop(1)}"
            cleaned.startsWith("254") -> cleaned
            else -> cleaned
        }
    }

    private fun urlEncode(str: String): String = URLEncoder.encode(str, StandardCharsets.UTF_8.toString())

    private fun formatMoney(amount: Double): String = naf.format(amount)

    private fun escapeHtml(str: String?): String {
        return (str ?: "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun currentDateTimeEat(): String {
        val eatZone = ZoneId.of("Africa/Nairobi")
        return ZonedDateTime.now(eatZone).format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
    }

    private fun toResponse(row: ResultRow): ReportScheduleResponse {
        val bId = row[ReportSchedulesTable.branchId]
        val bName = bId?.let { branchId ->
            BranchesTable.select { BranchesTable.id eq branchId }.firstOrNull()?.get(BranchesTable.name)
        }
        return ReportScheduleResponse(
            id = row[ReportSchedulesTable.id],
            businessId = row[ReportSchedulesTable.businessId],
            branchId = bId,
            branchName = bName,
            name = row[ReportSchedulesTable.name],
            reportType = row[ReportSchedulesTable.reportType],
            frequency = row[ReportSchedulesTable.frequency],
            timeOfDay = row[ReportSchedulesTable.timeOfDay],
            dayOfWeek = row[ReportSchedulesTable.dayOfWeek],
            dayOfMonth = row[ReportSchedulesTable.dayOfMonth],
            channels = row[ReportSchedulesTable.channels],
            emailRecipients = row[ReportSchedulesTable.emailRecipients],
            whatsappRecipients = row[ReportSchedulesTable.whatsappRecipients],
            isActive = row[ReportSchedulesTable.isActive],
            lastRunAt = row[ReportSchedulesTable.lastRunAt]?.toString(),
            lastStatus = row[ReportSchedulesTable.lastStatus],
            lastError = row[ReportSchedulesTable.lastError],
            createdAt = row[ReportSchedulesTable.createdAt].toString(),
            updatedAt = row[ReportSchedulesTable.updatedAt].toString()
        )
    }
}
