package com.app.biashara.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.presentation.viewmodel.PaymentsViewModel
import com.app.biashara.presentation.viewmodel.ReportsViewModel
import com.app.biashara.presentation.viewmodel.SocialViewModel
import com.app.biashara.presentation.viewmodel.BusinessViewModel
import com.app.biashara.domain.model.PaymentChannel
import com.app.biashara.domain.model.TransactionStatus
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.Base64
import kotlinx.serialization.Serializable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import kotlinx.coroutines.launch

private val ModernGreen = Color(0xFF00B874)
private val ModernNavy = Color(0xFF0F1F3A)
private val ModernMuted = Color(0xFF64748B)
private val ModernBorder = Color(0xFFE2E8F0)
private val ModernBackground = Color(0xFFF8FAFC)

@Composable
fun DesktopReportsLiveScreen(
    viewModel: ReportsViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    var selectedPeriod by remember { mutableStateOf("This Month") }
    var periodMenuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.loadReport(selectedPeriod) }
    val summary = state.profitSummary

    ModernPage(
        "Reports",
        "Dashboard",
        "Reports",
        if (state.isSyncing) "Syncing…" else "Refresh",
        Icons.Default.Refresh,
        { viewModel.loadReport(selectedPeriod) },
        actionEnabled = !state.isSyncing
    ) {
        state.error?.let { ComplianceError(it) }
        Box {
            OutlinedButton(onClick = { periodMenuExpanded = true }) {
                Icon(Icons.Default.CalendarMonth, null)
                Spacer(Modifier.width(8.dp))
                Text(selectedPeriod)
            }
            DropdownMenu(
                expanded = periodMenuExpanded,
                onDismissRequest = { periodMenuExpanded = false }
            ) {
                listOf("Today", "This Week", "This Month", "This Quarter", "This Year").forEach { period ->
                    DropdownMenuItem(
                        text = { Text(period) },
                        onClick = {
                            selectedPeriod = period
                            periodMenuExpanded = false
                            viewModel.loadReport(period)
                        }
                    )
                }
            }
        }

        if (summary != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernSummary(Modifier.weight(1f), "Revenue", summary.totalRevenue, selectedPeriod, Icons.Default.MonetizationOn, ModernGreen, Color(0xFFE1F8EF))
                ModernSummary(Modifier.weight(1f), "Expenses", summary.totalExpenses, selectedPeriod, Icons.Default.ReceiptLong, Color(0xFFEF4444), Color(0xFFFEECEC))
                ModernSummary(Modifier.weight(1f), "Gross Profit", summary.grossProfit, selectedPeriod, Icons.Default.TrendingUp, Color(0xFF2563EB), Color(0xFFE8F1FF))
                ModernSummary(Modifier.weight(1f), "Net Profit", summary.netProfit, selectedPeriod, Icons.Default.AccountBalance, Color(0xFF7C3AED), Color(0xFFF1EAFE))
            }
        } else if (!state.isLoading) {
            ComplianceError("No synchronized report data is available for $selectedPeriod.")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ModernBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Detailed trends", color = ModernNavy, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "Charts and category breakdowns are hidden until the reporting API provides time-series and expense-category data. The totals above come from synchronized orders and expenses.",
                    color = ModernMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun DesktopPaymentsModernScreen(
    viewModel: PaymentsViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPayments() }
    var selectedChannel by remember { mutableStateOf<PaymentChannel?>(null) }
    var selectedStatus by remember { mutableStateOf<TransactionStatus?>(null) }
    var thisMonthOnly by remember { mutableStateOf(true) }
    val currentMonth = remember { kotlinx.datetime.Clock.System.now().toString().take(7) }
    val visiblePayments = state.payments.filter {
        (!thisMonthOnly || it.transactionDate.toString().startsWith(currentMonth)) &&
            (selectedChannel == null || it.channel == selectedChannel) &&
            (selectedStatus == null || it.status == selectedStatus)
    }
    val mpesaCount = state.payments.count { it.channel.name.startsWith("MPESA") }
    val failedCount = state.payments.count { it.status == TransactionStatus.FAILED || it.status == TransactionStatus.CANCELLED }

    ModernPage("Payments", "Dashboard", "Payments", if (state.isSyncing) "Syncing…" else "Sync Backend", Icons.Default.Sync, viewModel::refreshPayments, actionEnabled = !state.isSyncing) {
        state.error?.let {
            Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFCA5A5))) {
                Text("Payment sync failed: $it", color = Color(0xFF991B1B), modifier = Modifier.fillMaxWidth().padding(12.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernSummary(Modifier.weight(1f), "Total Collected", state.totalReconciled, "Successful payments", Icons.Default.AccountBalanceWallet, ModernGreen, Color(0xFFE1F8EF))
            ModernSummary(Modifier.weight(1f), "Unreconciled", state.totalUnmatched, "Requires matching", Icons.Default.Schedule, Color(0xFFF59E0B), Color(0xFFFFF3D6))
            ModernSummary(Modifier.weight(1f), "M-Pesa Transactions", mpesaCount.toDouble(), "M-Pesa channels only", Icons.Default.PhoneAndroid, Color(0xFF2563EB), Color(0xFFE8F1FF), money = false)
            ModernSummary(Modifier.weight(1f), "Failed Payments", failedCount.toDouble(), "Failed or cancelled", Icons.Default.ErrorOutline, Color(0xFFEF4444), Color(0xFFFEECEC), money = false)
        }

        ModernTableCard(
            toolbar = {
                PaymentFilter(if (thisMonthOnly) "This Month" else "All Dates", Icons.Default.CalendarMonth, listOf("This Month", "All Dates")) {
                    thisMonthOnly = it == "This Month"
                }
                PaymentFilter(selectedChannel?.name?.replace("_", " ") ?: "All Channels", Icons.Default.Payments, listOf("All Channels") + PaymentChannel.entries.map { it.name.replace("_", " ") }) { label ->
                    selectedChannel = PaymentChannel.entries.firstOrNull { it.name.replace("_", " ") == label }
                }
                PaymentFilter(selectedStatus?.name ?: "All Status", Icons.Default.FilterAlt, listOf("All Status") + TransactionStatus.entries.map { it.name }) { label ->
                    selectedStatus = TransactionStatus.entries.firstOrNull { it.name == label }
                }
            },
            headers = listOf("Transaction", "Customer", "Phone", "Amount", "Channel", "Status", "Date", "Actions"),
            weights = listOf(1.2f, 1.2f, 1.2f, 1f, .8f, 1f, 1.1f, .7f),
            empty = visiblePayments.isEmpty(),
            emptyIcon = Icons.Default.ReceiptLong,
            emptyTitle = "No payment transactions",
            emptySubtitle = "Completed M-Pesa and card payments will appear here."
        ) {
            visiblePayments.forEach { payment ->
                val reconciled = payment.reconciled
                val linkedOrderId = payment.orderId
                ModernDataRow(
                    weights = listOf(1.2f, 1.2f, 1.2f, 1f, .8f, 1f, 1.1f, .7f),
                    cells = {
                        ModernCell(payment.transactionCode, color = ModernGreen, bold = true)
                        ModernCell(payment.payerName)
                        ModernCell(payment.payerPhone)
                        ModernCell("KES ${String.format("%,.0f", payment.amount)}", bold = true)
                        ModernCell(payment.channel.name.replace("_", " "))
                        Box(Modifier.weight(1f)) { PaymentStatusBadge(payment.status) }
                        ModernCell(payment.transactionDate.toString().take(16).replace('T', ' '))
                        Box(Modifier.weight(.7f)) {
                            if (!reconciled && payment.status == TransactionStatus.SUCCESS && !linkedOrderId.isNullOrBlank()) {
                                TextButton(onClick = { viewModel.reconcilePayment(payment.id, linkedOrderId) }) {
                                    Text("Match", color = ModernGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DesktopTaxModernScreen() {
    val client: HttpClient = remember { inject() }
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf<TaxSummaryDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    val month = remember { java.time.LocalDate.now() }
    val from = remember { month.withDayOfMonth(1).toString() }
    val to = remember { month.withDayOfMonth(month.lengthOfMonth()).toString() }
    val loadTax = {
        scope.launch {
            loading = true; error = null
            runCatching {
                val response: ApiResponse<TaxSummaryDto> = client.get("$BASE_URL/tax/summary") {
                    url { parameters.append("from", from); parameters.append("to", to) }
                }.body()
                if (!response.success || response.data == null) error(response.message.ifBlank { "Tax summary unavailable" })
                response.data
            }.onSuccess { summary = it }.onFailure { error = it.message }
            loading = false
        }
        Unit
    }
    LaunchedEffect(Unit) { loadTax() }
    val rows = summary?.let {
        listOf(
            Triple("VAT", it.netVat, "See KRA calendar"),
            Triple("TOT", it.totAmount, "See KRA calendar"),
            Triple("WHT", it.whtAmount, "See KRA calendar")
        )
    }.orEmpty().filter { selectedType == null || it.first == selectedType }
    var selectedTax by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    ModernPage(
        "Tax Management", "Dashboard", "Tax", if (loading) "Refreshing…" else "Refresh", Icons.Default.Refresh, loadTax,
        actionEnabled = !loading
    ) {
        error?.let { ComplianceError(it) }
        if (summary != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModernSummary(Modifier.weight(1f), "VAT Liability", summary!!.netVat, "Current filing period", Icons.Default.ReceiptLong, Color(0xFFEF4444), Color(0xFFFEECEC))
                ModernSummary(Modifier.weight(1f), "TOT Liability", summary!!.totAmount, "Current filing period", Icons.Default.AccountBalance, Color(0xFF2563EB), Color(0xFFE8F1FF))
                ModernSummary(Modifier.weight(1f), "Withholding Tax", summary!!.whtAmount, "Current filing period", Icons.Default.RequestQuote, Color(0xFF7C3AED), Color(0xFFF1EAFE))
                ModernSummary(Modifier.weight(1f), "Total Liability", summary!!.totalTaxLiability, "${summary!!.pendingRemittances} pending remittances", Icons.Default.Event, ModernGreen, Color(0xFFE1F8EF))
            }
        } else if (!loading) {
            ComplianceError("No tax summary is available. No liability values are being shown.")
        }
        ModernTableCard(
            toolbar = {
                PaymentFilter(
                    selectedType ?: "All Tax Types",
                    Icons.Default.AccountBalance,
                    listOf("All Tax Types", "VAT", "TOT", "WHT")
                ) { selectedType = it.takeUnless { value -> value == "All Tax Types" } }
            },
            headers = listOf("Tax Type", "Taxable Period", "Liability", "Due Date", "Status", "Actions"),
            weights = listOf(1.4f, 1.2f, 1.2f, 1f, 1f, .7f),
            empty = rows.isEmpty(),
            emptyIcon = Icons.Default.ReceiptLong,
            emptyTitle = "No tax summary",
            emptySubtitle = "Refresh after tax reporting is configured."
        ) {
            rows.forEach { (type, amount, due) ->
                ModernDataRow(listOf(1.4f, 1.2f, 1.2f, 1f, 1f, .7f)) {
                    ModernCell(type, bold = true)
                    ModernCell("$from to $to")
                    ModernCell("KES ${String.format("%,.0f", amount)}", bold = true)
                    ModernCell(due)
                    Box(Modifier.weight(1f)) { ModernStatus(if ((summary?.pendingRemittances ?: 0) > 0) "PENDING" else "CURRENT", false) }
                    Box(Modifier.weight(.7f)) {
                        IconButton(onClick = { selectedTax = Triple(type, "KES ${String.format("%,.0f", amount)}", due) }) { Icon(Icons.Default.Visibility, "View", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
    selectedTax?.let { tax ->
        AlertDialog(
            onDismissRequest = { selectedTax = null },
            title = { Text(tax.first, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current liability: ${tax.second}")
                    Text("Filing deadline: ${tax.third}")
                    Text("This summary is calculated for the currently selected reporting period.", color = ModernMuted)
                }
            },
            confirmButton = { Button(onClick = { selectedTax = null }, colors = ButtonDefaults.buttonColors(containerColor = ModernGreen)) { Text("Close") } }
        )
    }
}

@Composable
fun DesktopKraModernScreen() {
    val client: HttpClient = remember { inject() }
    val scope = rememberCoroutineScope()
    var compliance by remember { mutableStateOf<KraComplianceDto?>(null) }
    var history by remember { mutableStateOf<List<EtimsInvoiceDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var downloadingKey by remember { mutableStateOf<String?>(null) }
    val now = remember { java.time.LocalDate.now() }
    val loadKra = {
        scope.launch {
            loading = true; error = null
            runCatching {
                val c: ApiResponse<KraComplianceDto> = client.get("$BASE_URL/kra/compliance").body()
                if (!c.success || c.data == null) error(c.message.ifBlank { "KRA compliance unavailable" })
                val h: ApiResponse<List<EtimsInvoiceDto>> = client.get("$BASE_URL/kra/etims/history").body()
                compliance = c.data
                history = h.data ?: emptyList()
            }.onFailure { error = it.message }
            loading = false
        }
        Unit
    }
    LaunchedEffect(Unit) { loadKra() }
    val pending = (compliance?.pendingReturns.orEmpty() + compliance?.overdueReturns.orEmpty())
        .filter { selectedType == null || it.returnType == selectedType }
        .filter {
            selectedStatus == null ||
                (selectedStatus == "OVERDUE" && it.isOverdue) ||
                (selectedStatus == "PENDING" && !it.isOverdue)
        }
    ModernPage("KRA & eTIMS", "Dashboard", "KRA", if (loading) "Refreshing…" else "Refresh", Icons.Default.Refresh, loadKra, actionEnabled = !loading) {
        error?.let { ComplianceError(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernSummary(Modifier.weight(1f), "Compliance Score", (compliance?.complianceScore ?: 0).toDouble(), if (compliance?.pin == null) "KRA profile required" else "Live compliance", Icons.Default.VerifiedUser, ModernGreen, Color(0xFFE1F8EF), money = false, suffix = "%")
            ModernSummary(Modifier.weight(1f), "eTIMS Invoices", history.count { it.status == "TRANSMITTED" }.toDouble(), "Successfully transmitted", Icons.Default.Description, Color(0xFF2563EB), Color(0xFFE8F1FF), money = false)
            ModernSummary(Modifier.weight(1f), "Pending Returns", pending.size.toDouble(), "Require submission", Icons.Default.Schedule, Color(0xFFF59E0B), Color(0xFFFFF3D6), money = false)
            ModernSummary(Modifier.weight(1f), "Transmission Errors", history.count { it.status in listOf("ERROR", "REJECTED") }.toDouble(), "Requires attention", Icons.Default.ErrorOutline, Color(0xFFEF4444), Color(0xFFFEECEC), money = false)
        }
        ModernTableCard(
            toolbar = {
                PaymentFilter(
                    selectedType ?: "All Return Types",
                    Icons.Default.Assignment,
                    listOf("All Return Types", "VAT3", "TOT", "WHT")
                ) { selectedType = it.takeUnless { value -> value == "All Return Types" } }
                PaymentFilter(
                    selectedStatus ?: "All Status",
                    Icons.Default.FilterAlt,
                    listOf("All Status", "PENDING", "OVERDUE")
                ) { selectedStatus = it.takeUnless { value -> value == "All Status" } }
            },
            headers = listOf("Return", "Period", "Tax Amount", "Submission Status", "Last Updated", "Actions"),
            weights = listOf(1.5f, 1.1f, 1.2f, 1.2f, 1.2f, .7f),
            empty = pending.isEmpty(),
            emptyIcon = Icons.Default.Assignment,
            emptyTitle = "",
            emptySubtitle = ""
        ) {
            pending.forEach { item ->
                ModernDataRow(listOf(1.5f, 1.1f, 1.2f, 1.2f, 1.2f, .7f)) {
                    ModernCell(item.returnType, bold = true)
                    ModernCell(item.period)
                    ModernCell("KES ${String.format("%,.0f", item.estimatedAmount)}", bold = true)
                    Box(Modifier.weight(1.2f)) { ModernStatus(if (item.isOverdue) "OVERDUE" else "PENDING", false) }
                    ModernCell(item.dueDate)
                    Box(Modifier.weight(.7f)) {
                        IconButton(
                            onClick = {
                                val key = "${item.returnType}:${item.period}"
                                downloadingKey = key
                                scope.launch {
                                    downloadKraCsv(client, item.returnType, now.year, now.monthValue) { error = it }
                                    downloadingKey = null
                                }
                            },
                            enabled = downloadingKey == null
                        ) {
                            if (downloadingKey == "${item.returnType}:${item.period}") {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Download, "Download", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopSocialModernScreen(
    viewModel: SocialViewModel = remember { inject() },
    businessViewModel: BusinessViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    val businessState by businessViewModel.profileState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadChannelsAndInbox()
        businessViewModel.loadProfile()
    }
    val colors = mapOf(
        "WHATSAPP" to Color(0xFF22C55E),
        "INSTAGRAM" to Color(0xFFE1306C),
        "FACEBOOK" to Color(0xFF1877F2),
        "TIKTOK" to Color(0xFF111827)
    )
    var search by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    var showPaymentPrompt by remember { mutableStateOf(false) }
    var storefrontCopied by remember { mutableStateOf(false) }
    val storefrontUrl = businessState.profile?.storefrontSlug?.takeIf { it.isNotBlank() }
        ?.let { "https://enw9p7mvty.us-east-1.awsapprunner.com/shop/$it" }
        .orEmpty()
    val storefrontMessage = "Shop online with ${businessState.profile?.name ?: "us"}: $storefrontUrl"
    val filteredConversations = state.conversations.filter {
        it.customerName.contains(search, ignoreCase = true) ||
            it.platform.contains(search, ignoreCase = true) ||
            it.lastMessage.contains(search, ignoreCase = true)
    }
    val activeConversation = state.conversations.firstOrNull { it.id == state.activeConversationId }

    ModernPage("Social Inbox", "Dashboard", "Social", "Connect Channel", Icons.Default.Add, { openDesktopWeb("/social-onboarding") }) {
        state.error?.let {
            Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFCA5A5))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFDC2626))
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = Color(0xFF991B1B), modifier = Modifier.weight(1f))
                    IconButton(onClick = viewModel::dismissError) { Icon(Icons.Default.Close, "Dismiss") }
                }
            }
        }
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ModernBorder)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Storefront, null, tint = ModernGreen)
                Column(Modifier.weight(1f)) {
                    Text("Customer storefront", color = ModernNavy, fontWeight = FontWeight.Bold)
                    Text(
                        storefrontUrl.ifBlank { if (businessState.isLoading) "Loading storefront link…" else "Storefront link unavailable" },
                        color = ModernMuted,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    enabled = storefrontUrl.isNotBlank(),
                    onClick = {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(storefrontUrl), null)
                        storefrontCopied = true
                    }
                ) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(5.dp)); Text(if (storefrontCopied) "Copied" else "Copy") }
                OutlinedButton(enabled = storefrontUrl.isNotBlank(), onClick = { openExternalUrl("mailto:?subject=${urlEncode("Shop online with ${businessState.profile?.name ?: "us"}")}&body=${urlEncode(storefrontMessage)}") }) {
                    Icon(Icons.Default.Email, null); Spacer(Modifier.width(5.dp)); Text("Email")
                }
                OutlinedButton(enabled = storefrontUrl.isNotBlank(), onClick = { openExternalUrl("https://wa.me/?text=${urlEncode(storefrontMessage)}") }) {
                    Icon(Icons.Default.Chat, null); Spacer(Modifier.width(5.dp)); Text("WhatsApp")
                }
                Button(enabled = storefrontUrl.isNotBlank(), onClick = { openExternalUrl(storefrontUrl) }, colors = ButtonDefaults.buttonColors(containerColor = ModernGreen)) {
                    Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(5.dp)); Text("Open")
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernSummary(Modifier.weight(1f), "Open Conversations", state.conversations.count { it.status == "OPEN" }.toDouble(), "Across all channels", Icons.Default.Forum, ModernGreen, Color(0xFFE1F8EF), money = false)
            ModernSummary(Modifier.weight(1f), "Unread Messages", state.conversations.sumOf { it.unreadCount }.toDouble(), "Awaiting response", Icons.Default.MarkChatUnread, Color(0xFFEF4444), Color(0xFFFEECEC), money = false)
            ModernSummary(Modifier.weight(1f), "Orders from Social", state.conversations.count { !it.assignedOrderId.isNullOrBlank() }.toDouble(), "Linked conversations", Icons.Default.ShoppingCart, Color(0xFF2563EB), Color(0xFFE8F1FF), money = false)
            ModernSummary(Modifier.weight(1f), "Connected Channels", state.channels.count { it.isActive }.toDouble(), "Active integrations", Icons.Default.Hub, Color(0xFF7C3AED), Color(0xFFF1EAFE), money = false)
        }

        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ModernBorder)
        ) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(360.dp).fillMaxHeight()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("Search conversations…") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(9.dp)
                        )
                        IconButton(onClick = viewModel::loadChannelsAndInbox, enabled = !state.isLoading) {
                            if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, "Refresh inbox", tint = ModernGreen)
                        }
                    }
                    HorizontalDivider(color = ModernBorder)
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    filteredConversations.forEach { conversation ->
                        val platform = conversation.platform.uppercase()
                        Surface(
                            onClick = { viewModel.selectConversation(conversation.id) },
                            color = if (state.activeConversationId == conversation.id) Color(0xFFEAF8F2) else Color.White
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(shape = CircleShape, color = colors[platform] ?: ModernGreen) {
                                    Text(conversation.customerName.firstOrNull()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(conversation.customerName, color = ModernNavy, fontWeight = FontWeight.Bold)
                                    Text(conversation.lastMessage, color = ModernMuted, fontSize = 12.sp, maxLines = 1)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(platform, color = colors[platform] ?: ModernGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    if (conversation.unreadCount > 0) {
                                        Surface(shape = CircleShape, color = Color(0xFFEF4444)) {
                                            Text(conversation.unreadCount.toString(), color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                    }
                }
                VerticalDivider(color = ModernBorder)
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    if (activeConversation == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = CircleShape, color = Color(0xFFF1F3FA)) {
                                Icon(Icons.Default.Forum, null, tint = Color(0xFFA7ACC4), modifier = Modifier.padding(20.dp).size(44.dp))
                            }
                            Text("Select a conversation", color = ModernNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("Choose a customer conversation to view and reply.", color = ModernMuted, fontSize = 13.sp)
                        }
                    } else {
                        val conversation = activeConversation
                        val platform = conversation.platform.uppercase()
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(conversation.customerName, color = ModernNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    conversation.customerPhone?.let { Text(it, color = ModernMuted, fontSize = 12.sp) }
                                }
                                Spacer(Modifier.weight(1f))
                                Text(platform, color = colors[platform] ?: ModernGreen, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = ModernBorder)
                            Column(
                                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                state.messages.forEach { message ->
                                    val outbound = message.direction == "OUTBOUND"
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (outbound) Arrangement.End else Arrangement.Start) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (outbound) Color(0xFFE1F8EF) else Color(0xFFF1F5F9),
                                            modifier = Modifier.widthIn(max = 520.dp)
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(message.content, color = ModernNavy)
                                                Text(message.createdAt.take(16).replace('T', ' '), color = ModernMuted, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                                if (state.messages.isEmpty() && !state.isLoading) {
                                    Text("No messages in this conversation.", color = ModernMuted)
                                }
                            }
                            if (state.suggestedReplies.isNotEmpty()) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.suggestedReplies.take(2).forEach { suggestion ->
                                        AssistChip(onClick = { reply = suggestion }, label = { Text(suggestion, maxLines = 1) })
                                    }
                                }
                            }
                            HorizontalDivider(color = ModernBorder)
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = viewModel::generateAiSuggestion, enabled = !state.aiLoading) {
                                    Icon(Icons.Default.AutoAwesome, "Suggest reply", tint = Color(0xFF7C3AED))
                                }
                                IconButton(onClick = { showPaymentPrompt = true }) {
                                    Icon(Icons.Default.Payments, "Send payment prompt", tint = ModernGreen)
                                }
                                IconButton(
                                    onClick = { reply = storefrontMessage },
                                    enabled = storefrontUrl.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Storefront, "Share storefront", tint = Color(0xFF2563EB))
                                }
                                OutlinedTextField(
                                    value = reply,
                                    onValueChange = { reply = it },
                                    placeholder = { Text("Type a reply…") },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val content = reply.trim()
                                                if (content.isNotBlank()) {
                                                    viewModel.handleSendMessage(content)
                                                    reply = ""
                                                }
                                            },
                                            enabled = reply.isNotBlank() && !state.isSendingMessage
                                        ) { Icon(Icons.Default.Send, "Send", tint = ModernGreen) }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentPrompt) {
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPaymentPrompt = false },
            title = { Text("Send M-Pesa Payment Prompt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount (KES)") }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        amount.toDoubleOrNull()?.let { viewModel.handleSendPaymentPrompt(it, description.trim()) }
                        showPaymentPrompt = false
                    },
                    enabled = (amount.toDoubleOrNull() ?: 0.0) > 0 && description.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ModernGreen)
                ) { Text("Send Prompt") }
            },
            dismissButton = { TextButton(onClick = { showPaymentPrompt = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ModernPage(
    title: String,
    parent: String,
    current: String,
    actionLabel: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
    actionEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(ModernBackground).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = ModernNavy, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(parent, color = ModernMuted, fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Text(current, color = ModernMuted, fontSize = 14.sp)
                }
            }
            Button(
                onClick = onAction,
                enabled = actionEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = ModernGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
            ) {
                Icon(actionIcon, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
        content()
    }
}

@Composable
private fun ModernSummary(
    modifier: Modifier,
    title: String,
    value: Double,
    caption: String,
    icon: ImageVector,
    color: Color,
    background: Color,
    money: Boolean = true,
    suffix: String = ""
) {
    Card(
        modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ModernBorder)
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = CircleShape, color = background) {
                Icon(icon, null, tint = color, modifier = Modifier.padding(13.dp).size(25.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = ModernMuted, fontSize = 12.sp)
                Text(
                    if (money) "KES ${String.format("%,.0f", value)}" else "${String.format("%,.0f", value)}$suffix",
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(caption, color = ModernMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ColumnScope.ModernTableCard(
    toolbar: @Composable RowScope.() -> Unit,
    headers: List<String>,
    weights: List<Float>,
    empty: Boolean,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptySubtitle: String,
    rows: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth().weight(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ModernBorder)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { toolbar() }
            HorizontalDivider(color = ModernBorder)
            Row(Modifier.fillMaxWidth().background(Color(0xFFFCFDFE)).padding(horizontal = 22.dp, vertical = 16.dp)) {
                headers.forEachIndexed { index, header ->
                    Text(header, Modifier.weight(weights[index]), color = ModernNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = ModernBorder)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (empty) {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F3FA)) {
                            Icon(emptyIcon, null, tint = Color(0xFFA7ACC4), modifier = Modifier.padding(19.dp).size(42.dp))
                        }
                        Text(emptyTitle, color = ModernNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(emptySubtitle, color = ModernMuted, fontSize = 13.sp)
                    }
                } else {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { rows() }
                }
            }
            HorizontalDivider(color = ModernBorder)
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                Surface(shape = RoundedCornerShape(7.dp), color = ModernGreen) {
                    Text("1", color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }
    }
}

@Composable
private fun PaymentFilter(
    label: String,
    icon: ImageVector,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(9.dp),
            border = BorderStroke(1.dp, ModernBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ModernNavy)
        ) {
            Icon(icon, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace("_", " ")) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun PaymentStatusBadge(status: TransactionStatus) {
    val (color, label) = when (status) {
        TransactionStatus.SUCCESS -> ModernGreen to "SUCCESS"
        TransactionStatus.PENDING -> Color(0xFFF59E0B) to "PENDING"
        TransactionStatus.FAILED -> Color(0xFFEF4444) to "FAILED"
        TransactionStatus.CANCELLED -> Color(0xFFEF4444) to "CANCELLED"
        TransactionStatus.REVERSED -> Color(0xFF7C3AED) to "REVERSED"
    }
    Surface(shape = RoundedCornerShape(7.dp), color = color.copy(alpha = 0.12f)) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Serializable
private data class TaxSummaryDto(
    val period: String,
    val vatCollected: Double,
    val vatPayable: Double,
    val vatOnPurchases: Double,
    val netVat: Double,
    val totAmount: Double,
    val whtAmount: Double,
    val exciseAmount: Double,
    val customAmount: Double,
    val totalTaxLiability: Double,
    val totalTaxableRevenue: Double,
    val effectiveTaxRate: Double,
    val filedRemittances: Int,
    val pendingRemittances: Int
)

@Serializable
private data class PendingReturnDto(
    val returnType: String,
    val period: String,
    val dueDate: String,
    val isOverdue: Boolean,
    val estimatedAmount: Double
)

@Serializable
private data class KraComplianceDto(
    val businessId: String,
    val pin: String?,
    val isEtimsRegistered: Boolean,
    val isVatRegistered: Boolean,
    val isTotRegistered: Boolean,
    val pendingReturns: List<PendingReturnDto>,
    val overdueReturns: List<PendingReturnDto>,
    val lastEtimsTransmission: String?,
    val etimsTransmissionRate: Double,
    val complianceScore: Int,
    val recommendations: List<String>
)

@Serializable
private data class EtimsInvoiceDto(
    val internalId: String,
    val orderId: String,
    val invoiceNumber: String,
    val etimsInvoiceNumber: String?,
    val status: String,
    val errorMessage: String? = null,
    val submittedAt: String? = null,
    val createdAt: String
)

@Serializable
private data class CsvExportRequestDto(
    val returnType: String,
    val periodYear: Int,
    val periodMonth: Int,
    val format: String = "KRA_CSV"
)

@Serializable
private data class CsvExportResponseDto(
    val fileName: String,
    val format: String,
    val rowCount: Int,
    val periodLabel: String,
    val downloadBase64: String,
    val contentType: String
)

@Composable
private fun ComplianceError(message: String) {
    Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFCA5A5))) {
        Text(message, color = Color(0xFF991B1B), modifier = Modifier.fillMaxWidth().padding(12.dp))
    }
}

private suspend fun downloadKraCsv(
    client: HttpClient,
    returnType: String,
    year: Int,
    month: Int,
    onError: (String) -> Unit
) {
    runCatching {
        val normalizedType = when {
            returnType.contains("VAT", true) -> "VAT3"
            returnType.contains("TOT", true) -> "TOT"
            returnType.contains("WHT", true) -> "WHT"
            else -> returnType
        }
        val response: ApiResponse<CsvExportResponseDto> = client.post("$BASE_URL/kra/export/csv") {
            contentType(ContentType.Application.Json)
            setBody(CsvExportRequestDto(normalizedType, year, month))
        }.body()
        if (!response.success || response.data == null) error(response.message.ifBlank { "KRA export failed" })
        val export = requireNotNull(response.data)
        val directory = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
        val requested = File(directory, export.fileName)
        val output = if (requested.exists()) {
            File(directory, "${requested.nameWithoutExtension}-${System.currentTimeMillis()}.${requested.extension}")
        } else {
            requested
        }
        output.writeBytes(Base64.getDecoder().decode(export.downloadBase64))
        if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(output)
    }.onFailure { onError(it.message ?: "KRA export failed") }
}

internal fun openDesktopWeb(path: String) {
    runCatching {
        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
            java.awt.Desktop.getDesktop().browse(URI("https://enw9p7mvty.us-east-1.awsapprunner.com$path"))
        }
    }
}

private fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

internal fun openExternalUrl(url: String) {
    runCatching {
        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
            java.awt.Desktop.getDesktop().browse(URI(url))
        }
    }
}

fun exportDesktopFile(fileName: String, content: String) {
    runCatching {
        val directory = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
        val output = File(directory, fileName)
        output.writeText(content)
        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
            java.awt.Desktop.getDesktop().open(output)
        }
    }
}

@Composable
private fun ModernDataRow(weights: List<Float>, cells: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) { cells() }
    HorizontalDivider(color = Color(0xFFF1F5F9))
}

@Composable
private fun RowScope.ModernCell(text: String, color: Color = ModernMuted, bold: Boolean = false) {
    Text(text, Modifier.weight(1f), color = color, fontSize = 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
}

@Composable
private fun ModernStatus(label: String, success: Boolean) {
    val color = if (success) ModernGreen else Color(0xFFF59E0B)
    Surface(shape = RoundedCornerShape(7.dp), color = color.copy(alpha = 0.12f)) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}
