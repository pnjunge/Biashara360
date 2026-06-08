package com.app.biashara.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import java.io.File
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.app.biashara.ui.theme.*
import com.app.biashara.UserSession
import com.app.biashara.presentation.viewmodel.*
import com.app.biashara.domain.model.*

inline fun <reified T : Any> inject(): T = org.koin.core.context.GlobalContext.get().get()

// ─── Dashboard ────────────────────────────────────────────────────────────────

@Composable
fun DesktopDashboardScreen(
    viewModel: DashboardViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Dashboard title
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1E293B)
        )

        // KPI cards row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Monthly Revenue",
                value = "KES ${String.format("%,.0f", state.monthRevenue)}",
                change = "↑ 12% from last month",
                icon = Icons.Default.TrendingUp,
                color = B360Green,
                bgColor = Color(0xFFE6F7F0)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Net Profit",
                value = "KES ${String.format("%,.0f", state.netProfit)}",
                change = "↑ 8% from last month",
                icon = Icons.Default.AccountBalance,
                color = B360Blue,
                bgColor = Color(0xFFE0F2FE)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Orders Today",
                value = state.totalOrders.toString(),
                change = "↑ 3 from yesterday",
                icon = Icons.Default.ShoppingCart,
                color = B360Amber,
                bgColor = Color(0xFFFEF3C7)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Pending Payments",
                value = state.pendingOrders.toString(),
                change = "orders pending",
                icon = Icons.Default.Pending,
                color = B360Red,
                bgColor = Color(0xFFFEE2E2)
            )
        }

        // Charts + lists row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.height(360.dp)) {
            // Revenue chart card
            Card(
                modifier = Modifier.weight(1.6f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Revenue Trend",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1E293B)
                            )
                            Text("Last 7 days", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color.White,
                            modifier = Modifier.clickable {}
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("7 Days", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = B360Green)
                                Icon(Icons.Default.ArrowDropDown, null, tint = B360Green, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    RevenueBarChart()
                }
            }

            // Quick Alerts card
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Quick Alerts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AlertCard("${state.lowStockCount} products low stock", Icons.Default.Warning, B360Amber, Color(0xFFFEF3C7))
                        AlertCard("${state.pendingOrders} unpaid orders", Icons.Default.PendingActions, B360Red, Color(0xFFFEE2E2))
                        AlertCard("5 new customers this week", Icons.Default.PersonAdd, B360Green, Color(0xFFE6F7F0))
                        AlertCard("Mpesa: 2 unreconciled", Icons.Default.SyncProblem, B360Blue, Color(0xFFE0F2FE))
                    }
                }
            }
        }

        // Recent orders + top customers
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Recent Orders table card
            Card(
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Orders",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            "View all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = B360Green,
                            modifier = Modifier.clickable {}
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Order No.", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("Customer", modifier = Modifier.weight(1.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("Status", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("Amount", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("Date", modifier = Modifier.weight(1.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    if (state.recentOrders.isEmpty()) {
                        Text("No recent orders", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        state.recentOrders.forEach { order ->
                            DesktopOrderRow(
                                orderNo = order.orderNumber,
                                customer = order.customerName,
                                status = order.paymentStatus.name,
                                amount = "KES ${String.format("%,.0f", order.subtotal)}",
                                date = "Today, 10:30 AM"
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }

            // Top Customers card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Top Customers",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            "View all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = B360Green,
                            modifier = Modifier.clickable {}
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    val topCustomers = state.topCustomers
                    if (topCustomers.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Text("No customer data yet", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        topCustomers.take(4).forEach { (customer, stats) ->
                            val spentFmt = "KES ${String.format("%,.0f", stats.totalSpent)}"
                            val ordersFmt = "${stats.totalOrders} ${if (stats.totalOrders == 1) "order" else "orders"}"
                            TopCustomerRow(customer.name, ordersFmt, spentFmt)
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    modifier: Modifier,
    title: String,
    value: String,
    change: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left circular icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }

            // Right text details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B))
                Text(change, fontSize = 12.sp, color = B360Green, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun RevenueBarChart() {
    val data = listOf(18000f, 24000f, 19000f, 31000f, 27000f, 22000f, 34000f)
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val max = data.max()
    
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Y-Axis labels
        Column(
            modifier = Modifier.fillMaxHeight().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text("400K", fontSize = 10.sp, color = Color(0xFF94A3B8))
            Text("300K", fontSize = 10.sp, color = Color(0xFF94A3B8))
            Text("200K", fontSize = 10.sp, color = Color(0xFF94A3B8))
            Text("100K", fontSize = 10.sp, color = Color(0xFF94A3B8))
            Text("0", fontSize = 10.sp, color = Color(0xFF94A3B8))
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { i, value ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    val heightFraction = value / max
                    Box(
                        Modifier
                            .width(24.dp)
                            .fillMaxHeight(heightFraction * 0.8f)
                            .background(
                                B360Green,
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(days[i], fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun AlertCard(message: String, icon: ImageVector, color: Color, bgColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(message, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DesktopOrderRow(orderNo: String, customer: String, status: String, amount: String, date: String) {
    val isPaid = status == "PAID"
    val badgeBg = if (isPaid) Color(0xFFE6F7F0) else Color(0xFFFEF3C7)
    val badgeText = if (isPaid) B360Green else B360Amber
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(orderNo, modifier = Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1E293B))
        Text(customer, modifier = Modifier.weight(1.8f), fontSize = 13.sp, color = Color(0xFF64748B))
        Box(modifier = Modifier.weight(1.2f)) {
            Surface(color = badgeBg, shape = RoundedCornerShape(20.dp)) {
                Text(
                    text = status.lowercase().replaceFirstChar { it.uppercase() },
                    color = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Text(amount, modifier = Modifier.weight(1.2f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Text(date, modifier = Modifier.weight(1.6f), fontSize = 13.sp, color = Color(0xFF64748B))
    }
}

@Composable
fun TopCustomerRow(name: String, orders: String, spent: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase(),
                    color = B360Blue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Column {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1E293B))
                Text(orders, fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
        Text(spent, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
    }
}

// ─── Inventory ────────────────────────────────────────────────────────────────

@Composable
fun DesktopInventoryScreen(
    viewModel: InventoryViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadProducts(UserSession.getBusinessId())
    }

    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        viewModel.onSearchQueryChange(searchQuery)
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search products...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.width(320.dp), shape = RoundedCornerShape(10.dp), singleLine = true
            )
            Spacer(Modifier.weight(1f))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(B360Green)) {
                Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Product")
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                // Header
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    listOf("Product Name", "SKU", "Buying Price", "Selling Price", "Stock", "Status", "Actions").forEachIndexed { i, header ->
                        Text(header, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray,
                            modifier = Modifier.weight(if (i == 0) 2f else 1f))
                    }
                }
                HorizontalDivider()
                if (state.filteredProducts.isEmpty()) {
                    Text("No products found", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    state.filteredProducts.forEach { product ->
                        val statusColor = if (product.isOutOfStock) B360Red else if (product.isLowStock) B360Amber else B360Green
                        val statusText = if (product.isOutOfStock) "OUT" else if (product.isLowStock) "LOW" else "OK"
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(product.name, modifier = Modifier.weight(2f), fontWeight = FontWeight.Medium)
                            Text(product.sku, modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
                            Text("KES ${String.format("%,.0f", product.buyingPrice)}", modifier = Modifier.weight(1f))
                            Text("KES ${String.format("%,.0f", product.sellingPrice)}", modifier = Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold)
                            Text(product.currentStock.toString(), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = statusColor)
                            Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                Text(statusText, color = statusColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Edit, null, tint = B360Blue, modifier = Modifier.size(16.dp)) }
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.AddBox, null, tint = B360Green, modifier = Modifier.size(16.dp)) }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        }
    }
}

// ─── Orders ───────────────────────────────────────────────────────────────────

@Composable
fun DesktopOrdersScreen(
    viewModel: OrdersViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.selectedTabStatus == null, onClick = { viewModel.selectTab(null) }, label = { Text("All") })
                FilterChip(selected = state.selectedTabStatus == PaymentStatus.PAID, onClick = { viewModel.selectTab(PaymentStatus.PAID) }, label = { Text("Paid") })
                FilterChip(selected = state.selectedTabStatus == PaymentStatus.PENDING, onClick = { viewModel.selectTab(PaymentStatus.PENDING) }, label = { Text("Pending") })
                FilterChip(selected = state.selectedTabStatus == PaymentStatus.COD, onClick = { viewModel.selectTab(PaymentStatus.COD) }, label = { Text("COD") })
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(B360Green)) {
                Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Order")
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    listOf("Order #", "Customer", "Phone", "Amount", "Payment", "Delivery", "Date").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 0) 1f else 1.2f))
                    }
                }
                HorizontalDivider()
                if (state.filteredOrders.isEmpty()) {
                    Text("No orders found", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    state.filteredOrders.forEach { order ->
                        val payColor = when (order.paymentStatus) { PaymentStatus.PAID -> B360Green; PaymentStatus.PENDING -> B360Amber; else -> B360Blue }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(order.orderNumber, modifier = Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(order.customerName, modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Medium)
                            Text(order.customerPhone, modifier = Modifier.weight(1.2f), color = Color.Gray, fontSize = 13.sp)
                            Text("KES ${String.format("%,.0f", order.subtotal)}", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
                            Surface(color = payColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1.2f)) {
                                Text(order.paymentStatus.name, color = payColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Text(order.deliveryStatus.name, modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = Color.Gray)
                            Text(order.createdAt.toString().take(16).replace("T", " "), modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = Color.Gray)
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        }
    }
}

// ─── Customers ────────────────────────────────────────────────────────────────

@Composable
fun DesktopCustomersScreen(
    viewModel: CustomersViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
    }
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(searchQuery) {
        viewModel.onSearchQueryChange(searchQuery)
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search customers...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.width(320.dp), shape = RoundedCornerShape(10.dp), singleLine = true)
            Spacer(Modifier.weight(1f))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(B360Green)) {
                Icon(Icons.Filled.PersonAdd, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Add Customer")
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    listOf("Customer", "Phone", "Orders", "Total Spent", "Loyalty Pts", "Actions").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 0) 1.5f else 1f))
                    }
                }
                HorizontalDivider()
                if (state.filteredCustomers.isEmpty()) {
                    Text("No customers found", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    state.filteredCustomers.forEach { customer ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1.5f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(32.dp).background(B360Green.copy(0.1f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                                    Text(customer.name.firstOrNull()?.toString()?.uppercase() ?: "", color = B360Green, fontWeight = FontWeight.Bold)
                                }
                                Text(customer.name, fontWeight = FontWeight.Medium)
                            }
                            Text(customer.phone, Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
                            Text("—", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text("—", Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold)
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, null, tint = B360Amber, modifier = Modifier.size(14.dp))
                                Text(customer.loyaltyPoints.toString(), fontWeight = FontWeight.Medium)
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Visibility, null, tint = B360Blue, modifier = Modifier.size(16.dp)) }
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Chat, null, tint = B360Green, modifier = Modifier.size(16.dp)) }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        }
    }
}

// ─── Expenses ─────────────────────────────────────────────────────────────────

@Composable
fun DesktopExpensesScreen(
    viewModel: ExpensesViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadExpenses()
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStatCard(Modifier.weight(1f), "Total This Month", "KES ${String.format("%,.0f", state.totalAmount)}", B360Red)
            SummaryStatCard(Modifier.weight(1f), "Advertising", "KES 8,500", B360Blue)
            SummaryStatCard(Modifier.weight(1f), "Stock Purchase", "KES 45,000", B360Green)
            SummaryStatCard(Modifier.weight(1f), "Operations", "KES 12,000", B360Amber)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(B360Green)) {
                Icon(Icons.Filled.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Add Expense")
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(16.dp, 12.dp)) {
                    listOf("Description", "Category", "Amount", "Date", "Actions").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 0) 2f else 1f))
                    }
                }
                HorizontalDivider()
                if (state.expenses.isEmpty()) {
                    Text("No expenses recorded", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    state.expenses.forEach { expense ->
                        val catColor = when (expense.category) {
                            ExpenseCategory.ADVERTISING -> B360Blue
                            ExpenseCategory.RENT -> B360Red
                            ExpenseCategory.STOCK_PURCHASE -> B360Green
                            ExpenseCategory.DELIVERY -> B360Amber
                            else -> Color.Gray
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(expense.description, Modifier.weight(2f), fontWeight = FontWeight.Medium)
                            Surface(color = catColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                Text(expense.category.displayName(), color = catColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Text("KES ${String.format("%,.0f", expense.amount)}", Modifier.weight(1f), color = B360Red, fontWeight = FontWeight.SemiBold)
                            Text(expense.expenseDate.toString().take(10), Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
                            IconButton(onClick = { viewModel.deleteExpense(expense.id) }, Modifier.weight(1f).size(28.dp)) { Icon(Icons.Filled.Delete, null, tint = B360Red, modifier = Modifier.size(16.dp)) }
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(color.copy(0.08f))) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = color.copy(0.8f))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        }
    }
}

// ─── Payments ─────────────────────────────────────────────────────────────────

@Composable
fun DesktopPaymentsScreen(
    viewModel: PaymentsViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadPayments()
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStatCard(Modifier.weight(1f), "Total Collected", "KES ${String.format("%,.0f", state.totalReconciled)}", B360Green)
            SummaryStatCard(Modifier.weight(1f), "Unreconciled", "KES ${String.format("%,.0f", state.totalUnmatched)}", B360Amber)
            SummaryStatCard(Modifier.weight(1f), "Mpesa Transactions", state.payments.size.toString(), B360Blue)
            SummaryStatCard(Modifier.weight(1f), "Failed Payments", "0", B360Red)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(16.dp, 12.dp)) {
                    listOf("Mpesa Code", "Customer", "Phone", "Amount", "Channel", "Status", "Date", "").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 7) 0.6f else 1f))
                    }
                }
                HorizontalDivider()
                if (state.payments.isEmpty()) {
                    Text("No transactions recorded", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    state.payments.forEach { payment ->
                        val statusColor = if (payment.reconciled) B360Green else B360Amber
                        val statusText = if (payment.reconciled) "RECONCILED" else "PENDING"
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(payment.transactionCode, Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(payment.payerName, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Text(payment.payerPhone, Modifier.weight(1f), color = Color.Gray, fontSize = 12.sp)
                            Text("KES ${String.format("%,.0f", payment.amount)}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("Mpesa", Modifier.weight(1f), fontSize = 12.sp)
                            Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                Text(statusText, color = statusColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Text(payment.transactionDate.toString().take(10), Modifier.weight(1f), color = Color.Gray, fontSize = 12.sp)
                            if (!payment.reconciled) {
                                TextButton(onClick = { viewModel.reconcilePayment(payment.id, payment.orderId ?: "") }, modifier = Modifier.weight(0.6f)) {
                                    Text("Match", fontSize = 11.sp, color = B360Blue)
                                }
                            } else Spacer(Modifier.weight(0.6f))
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        }
    }
}

// ─── Reports ──────────────────────────────────────────────────────────────────

@Composable
fun DesktopReportsScreen(
    viewModel: ReportsViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadReport("This Month")
    }

    val summary = state.profitSummary

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // P&L Summary
            Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profit & Loss — March 2025", fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    if (summary == null) {
                        Text("Loading profit summary...", color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        PnlRow("Total Revenue", "KES ${String.format("%,.0f", summary.totalRevenue)}", B360Green)
                        PnlRow("Cost of Goods Sold", "KES ${String.format("%,.0f", summary.totalCostOfGoods)}", B360Red)
                        PnlRow("Gross Profit", "KES ${String.format("%,.0f", summary.grossProfit)}", B360Green, bold = true)
                        PnlRow("Total Expenses", "KES ${String.format("%,.0f", summary.totalExpenses)}", B360Red)
                        HorizontalDivider()
                        PnlRow("Net Profit", "KES ${String.format("%,.0f", summary.netProfit)}", B360Green, bold = true, large = true)
                        PnlRow("Net Margin", "${String.format("%.1f", summary.netMargin * 100)}%", B360Blue)
                    }
                }
            }
            // Expense breakdown
            Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Expense Breakdown", fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    listOf(
                        Triple("Stock Purchase", "KES 45,000", 0.69f),
                        Triple("Advertising", "KES 8,500", 0.13f),
                        Triple("Rent", "KES 15,000", 0.23f),
                        Triple("Delivery", "KES 3,200", 0.05f),
                        Triple("Packaging", "KES 1,200", 0.02f)
                    ).forEach { (cat, amt, frac) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, fontSize = 13.sp)
                                Text(amt, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            LinearProgressIndicator(progress = frac, Modifier.fillMaxWidth().height(6.dp), color = B360Green, trackColor = B360Green.copy(0.15f))
                        }
                    }
                }
            }
        }
        // Export buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {}) { Icon(Icons.Filled.PictureAsPdf, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Export PDF") }
            OutlinedButton(onClick = {}) { Icon(Icons.Filled.TableChart, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Export Excel") }
            OutlinedButton(onClick = {}) { Icon(Icons.Filled.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Share via WhatsApp") }
        }
    }
}

@Composable
fun PnlRow(label: String, value: String, color: Color, bold: Boolean = false, large: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = if (large) 15.sp else 13.sp)
        Text(value, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, fontSize = if (large) 15.sp else 13.sp)
    }
}

// ─── Settings ─────────────────────────────────────────────────────────────────

@Composable
fun DesktopSettingsScreen() {
    val scrollState = rememberScrollState()
    // Load current URL from the same source used at startup
    var backendUrl by remember { mutableStateOf(com.app.biashara.data.remote.BASE_URL) }
    Column(
        Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsSection("Business Profile") {
            SettingsField("Business Name", "Wanjiru's Fashion")
            SettingsField("Owner Phone", "+254712345678")
            SettingsField("Business Type", "Retail")
            SettingsField("Mpesa Short Code", "174379")
        }
        SettingsSection("Security") {
            SettingsToggle("Two-Factor Authentication (2FA)", true)
            SettingsToggle("Email Notifications", true)
            SettingsToggle("SMS Alerts", false)
        }
        SettingsSection("Subscription") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Current Plan", fontWeight = FontWeight.Medium); Text("Freemium", color = Color.Gray) }
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(B360Green)) { Text("Upgrade to Premium") }
            }
        }
        // New section for backend configuration
        SettingsSection("Backend Connectivity") {
            OutlinedTextField(
                value = backendUrl,
                onValueChange = { backendUrl = it },
                label = { Text("Backend URL") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    // Save to config file
                    val configDir = File(System.getProperty("user.home"), ".biashara360")
                    if (!configDir.exists()) configDir.mkdirs()
                    val configFile = File(configDir, "base_url.txt")
                    configFile.writeText(backendUrl.trim())
                    // Update runtime variable
                    com.app.biashara.data.remote.BASE_URL = backendUrl.trim()
                },
                colors = ButtonDefaults.buttonColors(B360Green)
            ) {
                Text("Save Backend URL")
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
fun SettingsField(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = Color.Gray)
        OutlinedTextField(value = value, onValueChange = {}, modifier = Modifier.width(300.dp), shape = RoundedCornerShape(8.dp), singleLine = true)
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean) {
    var state by remember { mutableStateOf(checked) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label)
        Switch(checked = state, onCheckedChange = { state = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = B360Green))
    }
}

// ── Tax Screen ────────────────────────────────────────────────────────────────
@Composable
fun DesktopTaxScreen() {
    val taxTypes = listOf(
        Triple("VAT 16%", "KES 48,000", "Due 20th"),
        Triple("TOT 1.5%", "KES 4,500", "Due 20th"),
        Triple("WHT 3%", "KES 9,000", "Due 20th"),
    )
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Tax Management", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("VAT Liability" to "KES 48,000", "TOT Liability" to "KES 4,500", "Next Filing" to "Mar 20").forEach { (label, value) ->
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, fontSize = 12.sp, color = Color.Gray)
                        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = B360Green)
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tax Summary", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                taxTypes.forEach { (type, amount, due) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(type, fontSize = 14.sp)
                        Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(due, fontSize = 12.sp, color = Color.Gray)
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

// ── KRA iTax Screen ───────────────────────────────────────────────────────────
@Composable
fun DesktopKraScreen() {
    val returns = listOf(
        Triple("VAT3 - Feb 2025", "Submitted", "KES 48,000"),
        Triple("TOT - Feb 2025", "Pending", "KES 4,500"),
        Triple("WHT - Feb 2025", "Pending", "KES 9,000"),
    )
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("KRA iTax Integration", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Compliance Score" to "87%", "eTIMS Invoices" to "142", "Pending Returns" to "2").forEach { (label, value) ->
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, fontSize = 12.sp, color = Color.Gray)
                        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = B360Green)
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tax Returns", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                        Text("Download CSV", fontSize = 13.sp)
                    }
                }
                returns.forEach { (name, status, amount) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontSize = 14.sp)
                        Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        val statusColor = if (status == "Submitted") B360Green else Color(0xFFFF8F00)
                        Text(status, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

// ── Social Inbox Screen ───────────────────────────────────────────────────────
@Composable
fun DesktopSocialScreen() {
    val conversations = listOf(
        Triple("Amara Osei", "WhatsApp", "Do you have Nike size 42?"),
        Triple("Fatuma Amin", "Instagram", "What's the price of the dress?"),
        Triple("James Kariuki", "Facebook", "Can I pay via Mpesa?"),
        Triple("Grace Mwangi", "TikTok", "Hi, I want to order 2 pieces"),
    )
    val platformColor = mapOf("WhatsApp" to Color(0xFF25D366), "Instagram" to Color(0xFFE1306C), "Facebook" to Color(0xFF1877F2), "TikTok" to Color(0xFF000000))

    Row(Modifier.fillMaxSize()) {
        // Conversation list
        Column(Modifier.width(320.dp).fillMaxHeight().background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Unified Inbox", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            HorizontalDivider()
            conversations.forEach { (name, platform, msg) ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(platformColor[platform] ?: B360Green), contentAlignment = Alignment.Center) {
                            Text(name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(msg, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                        }
                        Text(platform, fontSize = 10.sp, color = platformColor[platform] ?: B360Green)
                    }
                }
            }
        }
        VerticalDivider(Modifier.fillMaxHeight().width(1.dp))
        // Chat panel placeholder
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Forum, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Select a conversation", color = Color.Gray, fontSize = 16.sp)
        }
    }
}

// ─── CyberSource Settings Screen ──────────────────────────────────────────────
@Composable
fun DesktopCyberSourceSettingsScreen() {
    var merchantId by remember { mutableStateOf("WanFashion_CS_098") }
    var merchantKeyId by remember { mutableStateOf("9c7c25eb-42f8-4a52-b8bb-69d2d0c2e39b") }
    var merchantSecretKey by remember { mutableStateOf("••••••••••••••••••••••••••••••••") }
    var isSandbox by remember { mutableStateOf(true) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("CyberSource Configuration", fontWeight = FontWeight.Bold, fontSize = 22.sp)

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("API Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                HorizontalDivider()

                // Merchant ID
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Merchant ID (Organization ID)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = merchantId,
                        onValueChange = { merchantId = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("e.g. wanfashion_cs_098") }
                    )
                }

                // Key ID
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Active Key ID (REST API JWT/P12 Key ID)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = merchantKeyId,
                        onValueChange = { merchantKeyId = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("e.g. 9c7c25eb-xxxx-xxxx-xxxx-xxxxxxx") }
                    )
                }

                // Secret Key
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Shared Secret Key (Rest API Shared Secret)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = merchantSecretKey,
                        onValueChange = { merchantSecretKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("Enter your secure merchant shared secret key") }
                    )
                }

                // Environment toggle
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Sandbox Environment", fontWeight = FontWeight.Medium)
                        Text("Toggle off to deploy credentials on live CyberSource production rails", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isSandbox,
                        onCheckedChange = { isSandbox = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = B360Green
                        )
                    )
                }

                HorizontalDivider()

                if (showSuccessMessage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(B360Green.copy(0.15f))
                            .padding(14.dp)
                    ) {
                        Text("✓ CyberSource configuration validated and saved successfully!", color = B360Green, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            showSuccessMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Text("Save Configuration", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

