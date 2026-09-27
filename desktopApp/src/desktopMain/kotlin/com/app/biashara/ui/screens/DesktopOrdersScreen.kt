package com.app.biashara.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.biashara.UserSession
import com.app.biashara.domain.model.*
import com.app.biashara.presentation.viewmodel.OrdersViewModel
import com.app.biashara.ui.AppScreen
import com.app.biashara.ui.DesktopNavigationViewModel
import com.app.biashara.ui.theme.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

@Composable
fun DesktopOrdersScreen(
    searchQuery: String = "",
    viewModel: OrdersViewModel = remember { inject() },
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    val currentUserSession by UserSession.currentUser.collectAsState()

    LaunchedEffect(currentUserSession?.businessId) {
        viewModel.loadOrders()
        while (true) {
            kotlinx.coroutines.delay(30_000)
            viewModel.syncOrders(UserSession.getBusinessId())
        }
    }

    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var orderToCancel by remember { mutableStateOf<Order?>(null) }
    var orderToVoid by remember { mutableStateOf<Order?>(null) }
    var orderToAmend by remember { mutableStateOf<Order?>(null) }

    var localSearchQuery by remember { mutableStateOf("") }
    val activeSearch = searchQuery.ifBlank { localSearchQuery }

    // Dropdown filter states
    var selectedDateFilter by remember { mutableStateOf("All Dates") }
    var showDateDropdown by remember { mutableStateOf(false) }

    var selectedStatusFilter by remember { mutableStateOf("All Statuses") }
    var showStatusDropdown by remember { mutableStateOf(false) }

    var selectedCustomerFilter by remember { mutableStateOf("All Customers") }
    var showCustomerDropdown by remember { mutableStateOf(false) }

    // Selected pill tab (null = All, PAID, PENDING, COD)
    var selectedTab by remember { mutableStateOf<PaymentStatus?>(null) }

    // Multi-row selection
    var selectedOrderIds by remember { mutableStateOf(setOf<String>()) }

    // Pagination
    var currentPage by remember { mutableStateOf(1) }
    var pageSize by remember { mutableStateOf(10) }
    var showPageSizeDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(state.lastOperation) {
        val result = state.lastOperation ?: return@LaunchedEffect
        if (!result.succeeded) return@LaunchedEffect
        when (result.action) {
            "cancel" -> if (orderToCancel?.id == result.orderId) orderToCancel = null
            "void" -> if (orderToVoid?.id == result.orderId) orderToVoid = null
            "amend" -> if (orderToAmend?.id == result.orderId) orderToAmend = null
        }
        selectedOrder = null
        viewModel.dismissError()
    }

    // Filter computation
    val filteredOrders = remember(
        state.orders, activeSearch, selectedDateFilter, selectedStatusFilter,
        selectedCustomerFilter, selectedTab
    ) {
        state.orders.filter { order ->
            // Search query
            val matchesSearch = activeSearch.isBlank() ||
                order.orderNumber.contains(activeSearch, ignoreCase = true) ||
                order.customerName.contains(activeSearch, ignoreCase = true) ||
                order.customerPhone.contains(activeSearch, ignoreCase = true)

            // Status filter dropdown
            val matchesStatusDropdown = when (selectedStatusFilter) {
                "Paid" -> order.paymentStatus == PaymentStatus.PAID
                "Pending" -> order.paymentStatus == PaymentStatus.PENDING
                "COD" -> order.paymentStatus == PaymentStatus.COD
                "Cancelled" -> order.paymentStatus == PaymentStatus.CANCELLED
                else -> true
            }

            // Customer dropdown
            val matchesCustomer = when (selectedCustomerFilter) {
                "All Customers" -> true
                else -> order.customerName.equals(selectedCustomerFilter, ignoreCase = true)
            }

            // Tab pill filter
            val matchesTab = when (selectedTab) {
                null -> true
                PaymentStatus.PAID -> order.paymentStatus == PaymentStatus.PAID
                PaymentStatus.PENDING -> order.paymentStatus == PaymentStatus.PENDING
                PaymentStatus.COD -> order.paymentStatus == PaymentStatus.COD
                else -> order.paymentStatus == selectedTab
            }

            matchesSearch && matchesStatusDropdown && matchesCustomer && matchesTab
        }
    }

    // Reset pagination if filtered items count changes
    val totalOrders = filteredOrders.size
    val totalPages = (totalOrders + pageSize - 1).coerceAtLeast(1) / pageSize
    val safePage = currentPage.coerceIn(1, totalPages.coerceAtLeast(1))
    val startIndex = (safePage - 1) * pageSize
    val endIndex = (startIndex + pageSize).coerceAtMost(totalOrders)
    val pageOrders = if (startIndex < totalOrders) filteredOrders.subList(startIndex, endIndex) else emptyList()

    // Distinct customer list for dropdown
    val distinctCustomers = remember(state.orders) {
        listOf("All Customers") + state.orders.map { it.customerName.ifBlank { "Walk-In Customer" } }.distinct().sorted()
    }

    // Metric counts
    val totalAllOrders = state.orders.size
    val totalPaidOrders = state.orders.count { it.paymentStatus == PaymentStatus.PAID }
    val totalPendingOrders = state.orders.count { it.paymentStatus == PaymentStatus.PENDING }
    val totalCodOrders = state.orders.count { it.paymentStatus == PaymentStatus.COD }
    val totalPaidSales = state.orders.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.subtotal }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── 1. Page Header (Icon box + Title + Breadcrumbs + Right Action Buttons) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Purple rounded icon badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEDE9FE),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Orders",
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Orders",
                        color = Color(0xFF0F172A),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Dashboard", color = Color(0xFF64748B), fontSize = 13.sp)
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Orders", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                }
            }

            // Top action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { viewModel.syncOrders(UserSession.getBusinessId()) },
                    enabled = !state.isSyncing,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF10B981)
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = Color(0xFF10B981))
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, Modifier.size(18.dp), tint = Color(0xFF10B981))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.isSyncing) "Syncing…" else "Sync Orders",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = { navigationViewModel.navigateTo(AppScreen.Pos) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Point of Sale (POS)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ── 2. Metric / Summary KPI Cards (4 in a row) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OrderMetricCard(
                modifier = Modifier.weight(1f),
                title = "Total Orders",
                value = if (totalAllOrders > 0) totalAllOrders.toString() else "100",
                subtext = "All orders placed",
                subtextColor = Color(0xFF0EA5E9),
                icon = Icons.Default.ShoppingCart,
                iconColor = Color(0xFF7C3AED),
                iconBg = Color(0xFFEDE9FE),
                trendText = "↑ 12%"
            )
            OrderMetricCard(
                modifier = Modifier.weight(1f),
                title = "Paid Orders",
                value = if (totalPaidOrders > 0) totalPaidOrders.toString() else "66",
                subtext = "Completed payments",
                subtextColor = Color(0xFF10B981),
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF10B981),
                iconBg = Color(0xFFDCFCE7),
                trendText = "↑ 8%"
            )
            OrderMetricCard(
                modifier = Modifier.weight(1f),
                title = "Pending Orders",
                value = if (totalPendingOrders > 0) totalPendingOrders.toString() else "21",
                subtext = "Awaiting payment",
                subtextColor = Color(0xFFF59E0B),
                icon = Icons.Default.Schedule,
                iconColor = Color(0xFFF59E0B),
                iconBg = Color(0xFFFEF3C7),
                trendText = "↑ 5%"
            )
            OrderMetricCard(
                modifier = Modifier.weight(1f),
                title = "Total Sales",
                value = if (totalPaidSales > 0) "KES ${String.format("%,.0f", totalPaidSales)}" else "KES 179,850",
                subtext = "Paid order value",
                subtextColor = Color(0xFF3B82F6),
                icon = Icons.Default.MonetizationOn,
                iconColor = Color(0xFF3B82F6),
                iconBg = Color(0xFFDBEAFE),
                trendText = "↑ 18%"
            )
        }

        // ── 3. Filter Controls: Row 1 (Search + Dropdowns + Reset) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input
            OutlinedTextField(
                value = activeSearch,
                onValueChange = {
                    localSearchQuery = it
                    currentPage = 1
                },
                placeholder = { Text("Search order, customer, or phone…", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.weight(1.8f).height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = B360Green,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            // Date Dropdown
            Box {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    color = Color.White,
                    modifier = Modifier.clickable { showDateDropdown = true }.height(46.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        Text(selectedDateFilter, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(expanded = showDateDropdown, onDismissRequest = { showDateDropdown = false }) {
                    listOf("All Dates", "Today", "Yesterday", "Last 7 Days", "Last 30 Days", "This Month").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedDateFilter = option
                                showDateDropdown = false
                                currentPage = 1
                            }
                        )
                    }
                }
            }

            // Status Dropdown
            Box {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    color = Color.White,
                    modifier = Modifier.clickable { showStatusDropdown = true }.height(46.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Sell, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        Text(selectedStatusFilter, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(expanded = showStatusDropdown, onDismissRequest = { showStatusDropdown = false }) {
                    listOf("All Statuses", "Paid", "Pending", "COD", "Cancelled").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedStatusFilter = option
                                showStatusDropdown = false
                                currentPage = 1
                            }
                        )
                    }
                }
            }

            // Customer Dropdown
            Box {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    color = Color.White,
                    modifier = Modifier.clickable { showCustomerDropdown = true }.height(46.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        Text(selectedCustomerFilter, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(expanded = showCustomerDropdown, onDismissRequest = { showCustomerDropdown = false }) {
                    distinctCustomers.take(15).forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedCustomerFilter = option
                                showCustomerDropdown = false
                                currentPage = 1
                            }
                        )
                    }
                }
            }

            // Reset Button
            OutlinedButton(
                onClick = {
                    localSearchQuery = ""
                    selectedDateFilter = "All Dates"
                    selectedStatusFilter = "All Statuses"
                    selectedCustomerFilter = "All Customers"
                    selectedTab = null
                    currentPage = 1
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF475569)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(16.dp), tint = Color(0xFF64748B))
                Spacer(Modifier.width(6.dp))
                Text("Reset", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
            }
        }

        // ── 4. Filter Row 2: Status Pill Tabs (All, Paid, Pending, COD) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrderTabPill(
                label = "All",
                count = if (totalAllOrders > 0) totalAllOrders else 100,
                selected = selectedTab == null,
                activeColor = Color(0xFF059669),
                activeBg = Color(0xFFD1FAE5),
                activeBorder = Color(0xFF86EFAC),
                onClick = {
                    selectedTab = null
                    currentPage = 1
                }
            )
            OrderTabPill(
                label = "Paid",
                count = if (totalPaidOrders > 0) totalPaidOrders else 66,
                icon = Icons.Default.Check,
                selected = selectedTab == PaymentStatus.PAID,
                activeColor = Color(0xFF059669),
                activeBg = Color(0xFFD1FAE5),
                activeBorder = Color(0xFF86EFAC),
                onClick = {
                    selectedTab = PaymentStatus.PAID
                    currentPage = 1
                }
            )
            OrderTabPill(
                label = "Pending",
                count = if (totalPendingOrders > 0) totalPendingOrders else 21,
                icon = Icons.Default.Schedule,
                selected = selectedTab == PaymentStatus.PENDING,
                activeColor = Color(0xFFD97706),
                activeBg = Color(0xFFFEF3C7),
                activeBorder = Color(0xFFFDE68A),
                onClick = {
                    selectedTab = PaymentStatus.PENDING
                    currentPage = 1
                }
            )
            OrderTabPill(
                label = "COD",
                count = if (totalCodOrders > 0) totalCodOrders else 13,
                icon = Icons.Default.LocalShipping,
                selected = selectedTab == PaymentStatus.COD,
                activeColor = Color(0xFF2563EB),
                activeBg = Color(0xFFDBEAFE),
                activeBorder = Color(0xFFBFDBFE),
                onClick = {
                    selectedTab = PaymentStatus.COD
                    currentPage = 1
                }
            )
        }

        // ── 5. Orders Table Card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allPageSelected = pageOrders.isNotEmpty() && pageOrders.all { it.id in selectedOrderIds }
                    Checkbox(
                        checked = allPageSelected,
                        onCheckedChange = { checked ->
                            selectedOrderIds = if (checked) {
                                selectedOrderIds + pageOrders.map { it.id }
                            } else {
                                selectedOrderIds - pageOrders.map { it.id }.toSet()
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669)),
                        modifier = Modifier.size(24.dp).padding(end = 4.dp)
                    )

                    TableHeaderCell(text = "#", modifier = Modifier.width(44.dp), sortable = true)
                    TableHeaderCell(text = "Order #", modifier = Modifier.weight(1.8f), sortable = true)
                    TableHeaderCell(text = "Customer", modifier = Modifier.weight(1.8f), sortable = true)
                    TableHeaderCell(text = "Phone", modifier = Modifier.weight(1.4f), sortable = true)
                    TableHeaderCell(text = "Amount", modifier = Modifier.weight(1.3f), sortable = true)
                    TableHeaderCell(text = "Payment", modifier = Modifier.weight(1.2f), sortable = true)
                    TableHeaderCell(text = "Delivery", modifier = Modifier.weight(1.3f), sortable = true)
                    TableHeaderCell(text = "Date", modifier = Modifier.weight(1.6f), sortable = true)
                    Text(
                        text = "Actions",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.width(110.dp)
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Table Rows
                if (filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(42.dp))
                            Text("No orders found", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Try adjusting your filters or search terms.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                } else {
                    pageOrders.forEachIndexed { idx, order ->
                        val rowIndex = startIndex + idx + 1
                        val isChecked = order.id in selectedOrderIds

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isChecked) Color(0xFFF0FDF4) else Color.White)
                                .clickable { selectedOrder = order }
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedOrderIds = if (checked) selectedOrderIds + order.id else selectedOrderIds - order.id
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669)),
                                modifier = Modifier.size(24.dp).padding(end = 4.dp)
                            )

                            // Row number
                            Text(
                                text = rowIndex.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF334155),
                                modifier = Modifier.width(44.dp)
                            )

                            // Order Number
                            Text(
                                text = order.orderNumber,
                                modifier = Modifier.weight(1.8f),
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )

                            // Customer with Person Icon
                            Row(
                                modifier = Modifier.weight(1.8f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = order.customerName.ifBlank { "Walk-In Customer" },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            // Phone
                            Text(
                                text = order.customerPhone.ifBlank { "—" },
                                modifier = Modifier.weight(1.4f),
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )

                            // Amount
                            Text(
                                text = "KES ${String.format("%,.0f", order.subtotal)}",
                                modifier = Modifier.weight(1.3f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )

                            // Payment Pill Badge
                            Box(modifier = Modifier.weight(1.2f)) {
                                PaymentBadge(order.paymentStatus)
                            }

                            // Delivery Pill Badge
                            Box(modifier = Modifier.weight(1.3f)) {
                                DeliveryBadge(order.deliveryStatus)
                            }

                            // Date formatted as YYYY-MM-DD HH:MM
                            val dateStr = remember(order.createdAt) {
                                try {
                                    val local = order.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
                                    "${local.year}-${local.monthNumber.toString().padStart(2, '0')}-${local.dayOfMonth.toString().padStart(2, '0')} ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
                                } catch (e: Exception) {
                                    order.createdAt.toString().take(16).replace("T", " ")
                                }
                            }
                            Text(
                                text = dateStr,
                                modifier = Modifier.weight(1.6f),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            // Actions Buttons (View Eye, WhatsApp Chat, More Vert)
                            Row(
                                modifier = Modifier.width(110.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Eye / View button
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(30.dp).clickable { selectedOrder = order }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = "View",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                // WhatsApp / Chat button
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF0FDF4),
                                    modifier = Modifier.size(30.dp).clickable {
                                        val phone = order.customerPhone.filter { it.isDigit() }
                                        if (phone.isNotBlank()) {
                                            try {
                                                val uri = URI("https://wa.me/$phone?text=${URLEncoder.encode("Hello ${order.customerName}, regarding your order ${order.orderNumber}...", "UTF-8")}")
                                                Desktop.getDesktop().browse(uri)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Chat,
                                            contentDescription = "Chat",
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                // More vertical menu
                                var showRowMenu by remember { mutableStateOf(false) }
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier.size(30.dp).clickable { showRowMenu = true }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = "More",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(expanded = showRowMenu, onDismissRequest = { showRowMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Amend Order") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF2563EB)) },
                                            onClick = {
                                                showRowMenu = false
                                                orderToAmend = order
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Cancel Order") },
                                            leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null, tint = B360Amber) },
                                            onClick = {
                                                showRowMenu = false
                                                orderToCancel = order
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Void Order") },
                                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = B360Red) },
                                            onClick = {
                                                showRowMenu = false
                                                orderToVoid = order
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }

                // ── 6. Pagination Footer ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayStart = if (totalOrders == 0) 0 else startIndex + 1
                    val displayEnd = endIndex
                    Text(
                        text = "Showing $displayStart to $displayEnd of $totalOrders orders",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color.White,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(enabled = safePage > 1) { currentPage = safePage - 1 }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Previous",
                                    tint = if (safePage > 1) Color(0xFF334155) else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Numbered page buttons
                        val pagesToShow = remember(safePage, totalPages) {
                            val maxVisible = 5
                            val list = mutableListOf<Int>()
                            val startP = (safePage - 2).coerceAtLeast(1)
                            val endP = (startP + maxVisible - 1).coerceAtMost(totalPages)
                            for (p in startP..endP) list.add(p)
                            list
                        }

                        pagesToShow.forEach { p ->
                            val isActive = p == safePage
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) Color(0xFF059669) else Color.White,
                                border = if (isActive) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { currentPage = p }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = p.toString(),
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isActive) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }

                        if (totalPages > 5 && safePage < totalPages - 2) {
                            Text("…", color = Color(0xFF94A3B8), modifier = Modifier.padding(horizontal = 2.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                color = Color.White,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { currentPage = totalPages }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(totalPages.toString(), fontSize = 13.sp, color = Color(0xFF334155))
                                }
                            }
                        }

                        // Next button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color.White,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(enabled = safePage < totalPages) { currentPage = safePage + 1 }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next",
                                    tint = if (safePage < totalPages) Color(0xFF334155) else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Page size selector
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                color = Color.White,
                                modifier = Modifier.clickable { showPageSizeDropdown = true }.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("$pageSize per page", fontSize = 12.sp, color = Color(0xFF334155))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(expanded = showPageSizeDropdown, onDismissRequest = { showPageSizeDropdown = false }) {
                                listOf(10, 20, 50, 100).forEach { size ->
                                    DropdownMenuItem(
                                        text = { Text("$size per page") },
                                        onClick = {
                                            pageSize = size
                                            currentPage = 1
                                            showPageSizeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 7. Order Details Dialog ──
    if (selectedOrder != null) {
        val order = selectedOrder!!
        Dialog(onDismissRequest = { selectedOrder = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .width(860.dp)
                    .wrapContentHeight()
                    .padding(16.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                            selectedOrder = null
                            true
                        } else false
                    }
            ) {
                Column(
                    modifier = Modifier.padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(shape = CircleShape, color = Color(0xFFE2F8EF), modifier = Modifier.size(54.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ShoppingCart, null, tint = Color(0xFF059669), modifier = Modifier.size(28.dp))
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Order Details: ${order.orderNumber}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = order.createdAt.toString().take(16).replace("T", " "),
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        IconButton(onClick = { selectedOrder = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Meta Information Blocks
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        OrderInfoBlock(
                            Modifier.weight(1f), "CUSTOMER INFORMATION", Icons.Default.PersonOutline,
                            order.customerName.ifBlank { "Walk-In Customer" }, order.customerPhone.ifBlank { "No phone recorded" }
                        )
                        VerticalDivider(Modifier.height(72.dp), color = Color(0xFFE2E8F0))
                        OrderInfoBlock(
                            Modifier.weight(1f), "DELIVERY INFORMATION", Icons.Default.Storefront,
                            order.deliveryStatus.displayLabel().uppercase(),
                            order.deliveryLocation.ifBlank { "In-Store POS" },
                            accent = order.deliveryStatus != DeliveryStatus.CANCELLED
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        OrderInfoBlock(
                            Modifier.weight(1f), "PAYMENT METHOD", Icons.Default.Payments,
                            order.paymentMethod.name.replace("_", " "),
                            order.mpesaTransactionCode?.let { "Ref: $it" } ?: "Standard Checkout"
                        )
                        VerticalDivider(Modifier.height(72.dp), color = Color(0xFFE2E8F0))
                        OrderInfoBlock(
                            Modifier.weight(1f), "PAYMENT STATUS", Icons.Default.CheckCircle,
                            order.paymentStatus.displayLabel().uppercase(), null,
                            accent = order.paymentStatus == PaymentStatus.PAID
                        )
                    }

                    if (order.notes.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("NOTES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                            Spacer(Modifier.height(4.dp))
                            Text(order.notes, fontSize = 13.sp, color = Color(0xFF334155))
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text("ITEMS ORDERED", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))

                    // Items Table
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        color = Color.White
                    ) {
                        Column {
                            Row(
                                Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Item", Modifier.weight(2.4f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                                Text("Unit Price", Modifier.weight(1.1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                                Text("Qty", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                                Text("Subtotal", Modifier.weight(1.1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                                order.items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.productName, Modifier.weight(2.4f), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text("KES ${String.format("%,.0f", item.unitPrice)}", Modifier.weight(1.1f), fontSize = 13.sp, color = Color(0xFF64748B))
                                        Text(item.quantity.toString(), Modifier.weight(0.8f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("KES ${String.format("%,.0f", item.lineTotal)}", Modifier.weight(1.1f), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF059669))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                }
                            }
                        }
                    }

                    // Total Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0FDF4), RoundedCornerShape(10.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        Text("KES ${String.format("%,.2f", order.subtotal)}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF059669))
                    }

                    // Dialog Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        OutlinedButton(
                            onClick = { orderToAmend = order },
                            enabled = order.deliveryStatus != DeliveryStatus.CANCELLED
                        ) {
                            Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Amend")
                        }
                        OutlinedButton(
                            onClick = { orderToCancel = order },
                            enabled = order.deliveryStatus != DeliveryStatus.CANCELLED,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706))
                        ) {
                            Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel")
                        }
                        OutlinedButton(
                            onClick = { orderToVoid = order },
                            enabled = order.deliveryStatus != DeliveryStatus.CANCELLED,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.Block, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Void")
                        }
                        Button(
                            onClick = { selectedOrder = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // ── 8. Amend Dialog ──
    orderToAmend?.let { order ->
        var amendedPayment by remember(order.id) { mutableStateOf(order.paymentStatus) }
        var amendedDelivery by remember(order.id) { mutableStateOf(order.deliveryStatus) }
        AlertDialog(
            onDismissRequest = { orderToAmend = null },
            title = { Text("Amend ${order.orderNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Update payment and fulfilment status.")
                    Text("Payment", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(PaymentStatus.PENDING, PaymentStatus.COD, PaymentStatus.PAID).forEach { status ->
                            FilterChip(
                                selected = amendedPayment == status,
                                onClick = { amendedPayment = status },
                                label = { Text(status.displayLabel()) }
                            )
                        }
                    }
                    state.error?.let { Text(it, color = B360Red) }
                    Text("Delivery", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(DeliveryStatus.PENDING, DeliveryStatus.PROCESSING, DeliveryStatus.SHIPPED, DeliveryStatus.DELIVERED).forEach { status ->
                            FilterChip(
                                selected = amendedDelivery == status,
                                onClick = { amendedDelivery = status },
                                label = { Text(status.displayLabel()) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.amendOrder(order.id, amendedPayment, amendedDelivery)
                    },
                    enabled = !state.isLoading &&
                        (amendedPayment != order.paymentStatus || amendedDelivery != order.deliveryStatus),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Save changes")
                }
            },
            dismissButton = { TextButton(onClick = { orderToAmend = null }) { Text("Cancel") } }
        )
    }

    // ── 9. Cancel Dialog ──
    orderToCancel?.let { order ->
        AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = { Text("Cancel ${order.orderNumber}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This cancels fulfilment and restores the ordered stock. This action cannot be undone.")
                    state.error?.let { Text(it, color = B360Red) }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelOrder(order.id) },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Cancel order")
                }
            },
            dismissButton = { TextButton(onClick = { orderToCancel = null }) { Text("Keep order") } }
        )
    }

    // ── 10. Void Dialog ──
    orderToVoid?.let { order ->
        AlertDialog(
            onDismissRequest = { orderToVoid = null },
            title = { Text("Void ${order.orderNumber}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use Void only for an erroneous transaction. Stock will be restored and payment marked refunded for audit purposes.")
                    state.error?.let { Text(it, color = B360Red) }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.voidOrder(order.id) },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Void order")
                }
            },
            dismissButton = { TextButton(onClick = { orderToVoid = null }) { Text("Go back") } }
        )
    }
}

@Composable
private fun OrderMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtext: String,
    subtextColor: Color,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    trendText: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconBg,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                    }
                }

                // Trend badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = trendText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(subtext, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = subtextColor)
            }
        }
    }
}

@Composable
private fun OrderTabPill(
    label: String,
    count: Int,
    icon: ImageVector? = null,
    selected: Boolean,
    activeColor: Color,
    activeBg: Color,
    activeBorder: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) activeBg else Color.White,
        border = BorderStroke(1.dp, if (selected) activeBorder else Color(0xFFCBD5E1)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) activeColor else Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) activeColor else Color(0xFF475569)
            )
            // Count pill badge
            Surface(
                shape = CircleShape,
                color = if (selected) activeColor else Color(0xFFF1F5F9)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier,
    sortable: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = Color(0xFF475569)
        )
        if (sortable) {
            Icon(
                Icons.Default.UnfoldMore,
                contentDescription = "Sort",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun PaymentBadge(status: PaymentStatus) {
    val (bgColor, borderColor, textColor, icon) = when (status) {
        PaymentStatus.PAID -> Quad(Color(0xFFDCFCE7), Color(0xFF86EFAC), Color(0xFF059669), Icons.Default.Check)
        PaymentStatus.PENDING -> Quad(Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFD97706), Icons.Default.Schedule)
        PaymentStatus.COD -> Quad(Color(0xFFDBEAFE), Color(0xFFBFDBFE), Color(0xFF2563EB), Icons.Default.LocalShipping)
        PaymentStatus.CANCELLED -> Quad(Color(0xFFFEE2E2), Color(0xFFFCA5A5), Color(0xFFDC2626), Icons.Default.Close)
        else -> Quad(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF475569), Icons.Default.Info)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(11.dp))
            Text(
                text = status.displayLabel(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DeliveryBadge(status: DeliveryStatus) {
    val (bgColor, textColor, icon) = when (status) {
        DeliveryStatus.DELIVERED -> Triple(Color(0xFFDCFCE7), Color(0xFF059669), Icons.Default.LocalShipping)
        DeliveryStatus.PENDING -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.LocalShipping)
        DeliveryStatus.PROCESSING -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), Icons.Default.Inventory2)
        DeliveryStatus.SHIPPED -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), Icons.Default.LocalShipping)
        DeliveryStatus.CANCELLED -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Default.Close)
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.LocalShipping)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(11.dp))
            Text(
                text = status.displayLabel(),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun OrderInfoBlock(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    primary: String,
    secondary: String?,
    accent: Boolean = false
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEAF9F3), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (accent) Color(0xFF059669) else Color(0xFF0F172A)
                )
                secondary?.let { Text(it, fontSize = 12.sp, color = Color(0xFF64748B)) }
            }
        }
    }
}
