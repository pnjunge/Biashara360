package com.app.biashara.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.biashara.UserSession
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import com.app.biashara.domain.model.*
import com.app.biashara.presentation.viewmodel.InventoryViewModel
import com.app.biashara.ui.theme.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopPurchasesScreen(
    searchQuery: String = "",
    onNavigateToInventory: () -> Unit = {},
    inventoryViewModel: InventoryViewModel = remember { inject() },
    client: HttpClient = remember { inject() }
) {
    val scope = rememberCoroutineScope()
    val inventoryState by inventoryViewModel.state.collectAsState()
    val businessId = UserSession.getBusinessId()

    var purchases by remember { mutableStateOf<List<PurchaseInvoice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotification by remember { mutableStateOf<String?>(null) }

    var localSearchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var viewingInvoice by remember { mutableStateOf<PurchaseInvoice?>(null) }

    val activeSearch = searchQuery.ifBlank { localSearchQuery }

    val fetchPurchases = {
        scope.launch {
            isLoading = true
            errorMessage = null
            runCatching {
                client.get("$BASE_URL/purchases").body<ApiResponse<List<PurchaseInvoice>>>()
            }.onSuccess { res ->
                isLoading = false
                val data = res.data
                if (res.success && data != null) {
                    purchases = data
                } else {
                    errorMessage = res.message.ifBlank { "Failed to load purchases" }
                }
            }.onFailure { err ->
                isLoading = false
                errorMessage = err.message ?: "Failed to connect to backend"
            }
        }
    }

    LaunchedEffect(Unit) {
        inventoryViewModel.loadProducts(businessId)
        fetchPurchases()
    }

    val filteredPurchases = remember(purchases, activeSearch, statusFilter) {
        purchases.filter { p ->
            val matchesSearch = activeSearch.isBlank() ||
                p.invoiceNumber.contains(activeSearch, ignoreCase = true) ||
                p.supplierName.contains(activeSearch, ignoreCase = true) ||
                p.items.any { it.productName.contains(activeSearch, ignoreCase = true) || it.sku.contains(activeSearch, ignoreCase = true) }

            val matchesStatus = when (statusFilter) {
                "PAID" -> p.paymentStatus.equals("PAID", ignoreCase = true)
                "PENDING" -> p.paymentStatus.equals("PENDING", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesStatus
        }
    }

    val totalPurchasesAmount = remember(purchases) { purchases.sumOf { it.totalAmount } }
    val totalUnitsReceived = remember(purchases) { purchases.sumOf { p -> p.items.sumOf { it.quantity } } }
    val pendingInvoicesCount = remember(purchases) { purchases.count { it.paymentStatus.equals("PENDING", ignoreCase = true) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Purchase Invoices",
                    color = Color(0xFF0F1F3A),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dashboard", color = Color(0xFF64748B), fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Text("Operations", color = Color(0xFF64748B), fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Text("Purchases", color = B360Green, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onNavigateToInventory,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Inventory, null, modifier = Modifier.size(18.dp), tint = B360Green)
                    Spacer(Modifier.width(8.dp))
                    Text("View Inventory", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { fetchPurchases(); inventoryViewModel.syncProducts(businessId) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, B360Green),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp), tint = B360Green)
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh", color = B360Green, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Record Purchase Invoice", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Notification / Error banners
        successNotification?.let { msg ->
            Surface(
                color = Color(0xFFE6F7F0),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, B360Green.copy(0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = B360Green, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(msg, color = Color(0xFF065F46), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { successNotification = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF065F46), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        errorMessage?.let { err ->
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = B360Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(err, color = Color(0xFF991B1B), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF991B1B), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // KPI Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Total Purchases",
                value = "KES ${String.format("%,.0f", totalPurchasesAmount)}",
                change = "Total supplier expenditures",
                icon = Icons.Default.ReceiptLong,
                color = B360Green,
                bgColor = Color(0xFFE6F7F0)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Recorded Invoices",
                value = purchases.size.toString(),
                change = "Supplier tax & delivery bills",
                icon = Icons.Default.Description,
                color = B360Blue,
                bgColor = Color(0xFFE8F1FF)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Stock Influx",
                value = "$totalUnitsReceived units",
                change = "Augmented into inventory",
                icon = Icons.Default.Inventory,
                color = Color(0xFF7C3AED),
                bgColor = Color(0xFFF1EAFE)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Pending Due",
                value = "$pendingInvoicesCount invoices",
                change = "Outstanding supplier credit",
                icon = Icons.Default.PendingActions,
                color = if (pendingInvoicesCount > 0) B360Amber else B360Green,
                bgColor = if (pendingInvoicesCount > 0) Color(0xFFFFF3D6) else Color(0xFFE6F7F0)
            )
        }

        // Toolbar (Search & Filter Chips)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = activeSearch,
                onValueChange = { localSearchQuery = it },
                placeholder = { Text("Search by Invoice #, Supplier, or Product…") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF94A3B8)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = B360Green,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            listOf("ALL" to "All Invoices", "PAID" to "Paid", "PENDING" to "Pending Due").forEach { (key, label) ->
                FilterChip(
                    selected = statusFilter == key,
                    onClick = { statusFilter = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE6F7F0),
                        selectedLabelColor = B360Green
                    ),
                    border = BorderStroke(1.dp, if (statusFilter == key) B360Green else Color(0xFFE2E8F0))
                )
            }
        }

        // Purchases Table Card
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("INVOICE NUMBER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.8f))
                    Text("DATE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.2f))
                    Text("SUPPLIER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.8f))
                    Text("ITEMS RECEIVED", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(2.2f))
                    Text("TOTAL AMOUNT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.4f))
                    Text("PAYMENT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1.2f))
                    Text("ACTIONS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.weight(0.8f))
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = B360Green)
                    }
                } else if (filteredPurchases.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ReceiptLong, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(56.dp))
                            Text("No purchase invoices found", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                            Text("Record your first supplier purchase invoice to automatically augment inventory.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Record Purchase")
                            }
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredPurchases) { invoice ->
                            val isPaid = invoice.paymentStatus.equals("PAID", ignoreCase = true)
                            val statusBg = if (isPaid) Color(0xFFE6F7F0) else Color(0xFFFFF3D6)
                            val statusColor = if (isPaid) B360Green else B360Amber

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewingInvoice = invoice }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Invoice Number with badge
                                Row(
                                    modifier = Modifier.weight(1.8f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE8F1FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Receipt, null, tint = B360Blue, modifier = Modifier.size(16.dp))
                                    }
                                    Column {
                                        Text(
                                            invoice.invoiceNumber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text("Ref: #${invoice.id.take(8)}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }

                                // Date
                                Text(
                                    invoice.invoiceDate.take(10),
                                    modifier = Modifier.weight(1.2f),
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569)
                                )

                                // Supplier
                                Column(modifier = Modifier.weight(1.8f)) {
                                    Text(invoice.supplierName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1E293B))
                                    invoice.supplierPhone?.takeIf { it.isNotBlank() }?.let { phone ->
                                        Text(phone, fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }

                                // Items summary
                                Column(modifier = Modifier.weight(2.2f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val summaryText = invoice.items.joinToString(", ") { "${it.productName} (x${it.quantity})" }
                                    Text(
                                        summaryText.ifBlank { "0 items" },
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155),
                                        maxLines = 2
                                    )
                                    val totalUnits = invoice.items.sumOf { it.quantity }
                                    Text("$totalUnits total units added to stock", fontSize = 11.sp, color = B360Green, fontWeight = FontWeight.Medium)
                                }

                                // Total Amount
                                Text(
                                    "KES ${String.format("%,.0f", invoice.totalAmount)}",
                                    modifier = Modifier.weight(1.4f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )

                                // Payment status & method badge
                                Surface(
                                    color = statusBg,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(statusColor)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "${invoice.paymentMethod} • ${invoice.paymentStatus}",
                                            color = statusColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Actions
                                Row(modifier = Modifier.weight(0.8f)) {
                                    IconButton(
                                        onClick = { viewingInvoice = invoice },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, null, tint = B360Blue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }

    // Modal: Record Purchase Invoice
    if (showCreateDialog) {
        RecordPurchaseInvoiceDialog(
            products = inventoryState.products,
            onDismiss = { showCreateDialog = false },
            onSaved = { invoice ->
                showCreateDialog = false
                successNotification = "Purchase Invoice #${invoice.invoiceNumber} recorded! Inventory stock has been augmented."
                fetchPurchases()
                inventoryViewModel.loadProducts(businessId)
            }
        )
    }

    // Modal: View Invoice Details
    viewingInvoice?.let { invoice ->
        PurchaseInvoiceDetailsDialog(
            invoice = invoice,
            onDismiss = { viewingInvoice = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPurchaseInvoiceDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onSaved: (PurchaseInvoice) -> Unit,
    client: HttpClient = remember { inject() }
) {
    val scope = rememberCoroutineScope()
    var invoiceNumber by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf("") }
    var supplierPhone by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var paymentStatus by remember { mutableStateOf("PAID") }
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Line items being added to the purchase invoice
    data class DraftItem(
        val productId: String,
        val productName: String,
        val sku: String,
        var quantityStr: String,
        var unitCostStr: String
    )

    var draftItems by remember { mutableStateOf<List<DraftItem>>(emptyList()) }

    // Selector state for adding a product line
    var selectedProduct by remember { mutableStateOf<Product?>(products.firstOrNull()) }
    var addQtyStr by remember { mutableStateOf("1") }
    var addCostStr by remember { mutableStateOf(selectedProduct?.buyingPrice?.toInt()?.toString() ?: "0") }
    var productDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedProduct) {
        selectedProduct?.let { p ->
            addCostStr = if (p.buyingPrice > 0.0) p.buyingPrice.toInt().toString() else "0"
        }
    }

    val totalCalculated = remember(draftItems) {
        draftItems.sumOf { item ->
            val q = item.quantityStr.toIntOrNull() ?: 0
            val c = item.unitCostStr.toDoubleOrNull() ?: 0.0
            q * c
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(760.dp)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE6F7F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ReceiptLong, null, tint = B360Green, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text("Record Purchase Invoice", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Receive supplier stock and augment inventory counts", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                validationError?.let { err ->
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(err, color = Color(0xFF991B1B), fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                }

                // Section 1: Invoice Information
                Text("1. INVOICE & SUPPLIER DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Invoice Number (Requested prominent field)
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it; validationError = null },
                        label = { Text("Invoice Number *") },
                        placeholder = { Text("e.g. INV-2026-081, ETR-4412") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = { Icon(Icons.Default.Tag, null, tint = B360Green) }
                    )

                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it; validationError = null },
                        label = { Text("Supplier Name *") },
                        placeholder = { Text("e.g. Brookside Dairy, Farmer's Choice") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = { Icon(Icons.Default.Store, null, tint = Color.Gray) }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = supplierPhone,
                        onValueChange = { supplierPhone = it },
                        label = { Text("Supplier Phone (Optional)") },
                        placeholder = { Text("0712 345 678") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Payment Method
                    var methodExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = methodExpanded,
                        onExpandedChange = { methodExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = methodExpanded,
                            onDismissRequest = { methodExpanded = false }
                        ) {
                            listOf("CASH", "MPESA", "BANK_TRANSFER", "CREDIT").forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = { paymentMethod = method; methodExpanded = false }
                                )
                            }
                        }
                    }

                    // Payment Status
                    var statusExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = paymentStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            listOf("PAID", "PENDING", "PARTIAL").forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status) },
                                    onClick = { paymentStatus = status; statusExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Section 2: Items to Augment Inventory
                Text("2. LINE ITEMS (AUGMENTS INVENTORY STOCK)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                // Line item adder form
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Product Selector
                            ExposedDropdownMenuBox(
                                expanded = productDropdownExpanded,
                                onExpandedChange = { productDropdownExpanded = it },
                                modifier = Modifier.weight(2.5f)
                            ) {
                                OutlinedTextField(
                                    value = selectedProduct?.name ?: "Select Product from Inventory",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Product") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = productDropdownExpanded,
                                    onDismissRequest = { productDropdownExpanded = false }
                                ) {
                                    if (products.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No products found in inventory") },
                                            onClick = { productDropdownExpanded = false }
                                        )
                                    } else {
                                        products.forEach { p ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(p.name, fontWeight = FontWeight.SemiBold)
                                                        Text("SKU: ${p.sku} • Current Stock: ${p.currentStock}", fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                },
                                                onClick = {
                                                    selectedProduct = p
                                                    productDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Quantity
                            OutlinedTextField(
                                value = addQtyStr,
                                onValueChange = { addQtyStr = it.filter { c -> c.isDigit() } },
                                label = { Text("Qty Received") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Unit Cost
                            OutlinedTextField(
                                value = addCostStr,
                                onValueChange = { addCostStr = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Unit Cost (KES)") },
                                modifier = Modifier.weight(1.4f),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            Button(
                                onClick = {
                                    val p = selectedProduct
                                    val qty = addQtyStr.toIntOrNull()
                                    val cost = addCostStr.toDoubleOrNull()
                                    if (p == null) {
                                        validationError = "Please select a product from inventory."
                                    } else if (qty == null || qty <= 0) {
                                        validationError = "Enter a positive quantity received."
                                    } else if (cost == null || cost < 0.0) {
                                        validationError = "Enter a valid unit cost."
                                    } else {
                                        draftItems = draftItems + DraftItem(
                                            productId = p.id,
                                            productName = p.name,
                                            sku = p.sku,
                                            quantityStr = qty.toString(),
                                            unitCostStr = cost.toString()
                                        )
                                        addQtyStr = "1"
                                        validationError = null
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = B360Blue),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Item")
                            }
                        }
                    }
                }

                // Table of added items
                if (draftItems.isEmpty()) {
                    Text(
                        "No items added yet. Select a product above to add to this purchase invoice.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F5F9)).padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PRODUCT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(2.5f))
                                Text("QTY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                                Text("UNIT COST", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1.5f))
                                Text("LINE TOTAL", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1.5f))
                                Text("", modifier = Modifier.width(36.dp))
                            }
                            draftItems.forEachIndexed { index, item ->
                                val q = item.quantityStr.toIntOrNull() ?: 0
                                val c = item.unitCostStr.toDoubleOrNull() ?: 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2.5f)) {
                                        Text(item.productName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(item.sku, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("+${q} units", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = B360Green, fontSize = 13.sp)
                                    Text("KES ${String.format("%,.0f", c)}", modifier = Modifier.weight(1.5f), fontSize = 13.sp)
                                    Text("KES ${String.format("%,.0f", q * c)}", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    IconButton(
                                        onClick = { draftItems = draftItems.filterIndexed { i, _ -> i != index } },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = B360Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }

                // Total Summary Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE6F7F0))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Invoice Amount:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Text("KES ${String.format("%,.0f", totalCalculated)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = B360Green)
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Invoice Notes / Delivery Remarks (Optional)") },
                    placeholder = { Text("e.g. Received via delivery truck #KBZ-123A") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (invoiceNumber.trim().isBlank()) {
                                validationError = "Invoice Number is required."
                                return@Button
                            }
                            if (supplierName.trim().isBlank()) {
                                validationError = "Supplier Name is required."
                                return@Button
                            }
                            if (draftItems.isEmpty()) {
                                validationError = "Please add at least one line item to this invoice."
                                return@Button
                            }

                            val itemsToSend = draftItems.map {
                                PurchaseLineItem(
                                    productId = it.productId,
                                    productName = it.productName,
                                    sku = it.sku,
                                    quantity = it.quantityStr.toIntOrNull() ?: 1,
                                    unitCost = it.unitCostStr.toDoubleOrNull() ?: 0.0,
                                    lineTotal = (it.quantityStr.toIntOrNull() ?: 1) * (it.unitCostStr.toDoubleOrNull() ?: 0.0)
                                )
                            }

                            val request = CreatePurchaseInvoiceRequest(
                                invoiceNumber = invoiceNumber.trim(),
                                supplierName = supplierName.trim(),
                                supplierPhone = supplierPhone.trim().takeIf { it.isNotBlank() },
                                invoiceDate = Clock.System.now().toString(),
                                paymentStatus = paymentStatus,
                                paymentMethod = paymentMethod,
                                notes = notes.trim(),
                                items = itemsToSend
                            )

                            scope.launch {
                                isSubmitting = true
                                validationError = null
                                runCatching {
                                    client.post("$BASE_URL/purchases") {
                                        contentType(ContentType.Application.Json)
                                        setBody(request)
                                    }.body<ApiResponse<PurchaseInvoice>>()
                                }.onSuccess { res ->
                                    isSubmitting = false
                                    val savedInvoice = res.data
                                    if (res.success && savedInvoice != null) {
                                        onSaved(savedInvoice)
                                    } else {
                                        validationError = res.message.ifBlank { "Failed to record purchase invoice" }
                                    }
                                }.onFailure { err ->
                                    isSubmitting = false
                                    validationError = err.message ?: "Failed to save purchase invoice"
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Augmenting Inventory...")
                        } else {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Confirm & Augment Stock", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseInvoiceDetailsDialog(
    invoice: PurchaseInvoice,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(620.dp).wrapContentHeight().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PURCHASE INVOICE", fontSize = 12.sp, color = B360Green, fontWeight = FontWeight.Bold)
                        Text(invoice.invoiceNumber, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SUPPLIER", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(invoice.supplierName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        invoice.supplierPhone?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DATE RECORDED", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(invoice.invoiceDate.take(10), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${invoice.paymentMethod} • ${invoice.paymentStatus}", fontSize = 12.sp, color = B360Green, fontWeight = FontWeight.Bold)
                    }
                }

                Text("ITEMS RECEIVED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(8.dp)) {
                            Text("Item", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                            Text("Qty", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("Cost", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                        invoice.items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(item.productName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(item.sku, fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("+${item.quantity}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = B360Green, fontSize = 13.sp)
                                Text("KES ${String.format("%,.0f", item.unitCost)}", modifier = Modifier.weight(1f), fontSize = 13.sp)
                                Text("KES ${String.format("%,.0f", item.lineTotal)}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Invoice Amount:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("KES ${String.format("%,.0f", invoice.totalAmount)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = B360Green)
                }

                if (invoice.notes.isNotBlank()) {
                    Text("Notes: ${invoice.notes}", fontSize = 12.sp, color = Color.Gray)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
