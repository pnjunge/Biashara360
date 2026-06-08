package com.app.biashara.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.navigation.NavController
import com.app.biashara.domain.model.Order
import com.app.biashara.domain.model.Product
import com.app.biashara.presentation.viewmodel.DashboardViewModel
import com.app.biashara.ui.navigation.Screen
import com.app.biashara.ui.theme.*
import com.app.biashara.ui.kmpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    val greeting = if (state.userName.isNotBlank()) "Habari, ${state.userName.split(" ").first()}! 👋"
    else "Habari! 👋"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(greeting, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (state.businessName.isNotBlank()) {
                            Text(
                                state.businessName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Payments.route) }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (state.isLoading && state.recentOrders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = B360Red, modifier = Modifier.size(48.dp))
                    Text(state.error!!, color = Color.Gray, fontSize = 14.sp)
                    Button(onClick = { viewModel.dismissError(); viewModel.loadDashboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                        Text("Retry")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(B360Surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Grid Row 1
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Monthly Revenue",
                        value = "KES ${"%,.0f".format(state.monthRevenue)}",
                        change = "↑ 12% from last month",
                        icon = Icons.Filled.TrendingUp,
                        color = B360Green,
                        bgColor = B360Green.copy(0.12f)
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Net Profit",
                        value = "KES ${"%,.0f".format(state.netProfit)}",
                        change = "↑ 8% from last month",
                        icon = Icons.Filled.Business,
                        color = B360Blue,
                        bgColor = B360Blue.copy(0.12f)
                    )
                }
            }

            // KPI Grid Row 2
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Orders Today",
                        value = "${state.totalOrders}",
                        change = "↑ 3 from yesterday",
                        icon = Icons.Filled.ShoppingCart,
                        color = B360Amber,
                        bgColor = B360Amber.copy(0.12f)
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending Payments",
                        value = "${state.pendingOrders}",
                        change = "orders pending",
                        icon = Icons.Filled.AccessTime,
                        color = B360Red,
                        bgColor = B360Red.copy(0.12f)
                    )
                }
            }

            // Revenue Trend Bar Chart
            item {
                RevenueBarChart()
            }

            // Quick Actions
            item {
                Column {
                    Text("Quick Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { QuickActionChip(Icons.Filled.Add, "New Order", B360Green) { navController.navigate(Screen.CreateOrder.route) } }
                        item { QuickActionChip(Icons.Filled.Inventory, "Add Stock", B360Blue) { navController.navigate(Screen.Inventory.route) } }
                        item { QuickActionChip(Icons.Filled.Receipt, "Add Expense", B360Amber) { navController.navigate(Screen.AddExpense.route) } }
                        item { QuickActionChip(Icons.Filled.People, "New Customer", Color(0xFF7B1FA2)) { navController.navigate(Screen.Customers.route) } }
                    }
                }
            }

            // Quick Alerts Section
            item {
                QuickAlertsSection(
                    lowStockCount = state.lowStockCount,
                    pendingOrdersCount = state.pendingOrders
                )
            }

            // Recent Orders Section
            if (state.recentOrders.isNotEmpty()) {
                item {
                    RecentOrdersSection(
                        orders = state.recentOrders,
                        onViewAll = { navController.navigate(Screen.Orders.route) },
                        onOrderClick = { id -> navController.navigate(Screen.OrderDetail.createRoute(id)) }
                    )
                }
            }

            // Top Customers Section
            item {
                TopCustomersSection(
                    customers = state.topCustomers,
                    onViewAll = { navController.navigate(Screen.Customers.route) }
                )
            }
        }
    }
}

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    change: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(bgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(change, fontSize = 10.sp, color = B360Green, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun RevenueBarChart(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Revenue Trend", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Last 7 days", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("7 Days", fontSize = 12.sp, color = B360Green, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ArrowDropDown, null, tint = B360Green, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Y Axis
                Column(
                    modifier = Modifier.fillMaxHeight().padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf("400K", "300K", "200K", "100K", "0").forEach { label ->
                        Text(label, fontSize = 9.sp, color = Color.Gray)
                    }
                }
                
                // Bars
                val data = listOf(
                    Pair("Mon", 0.45f),
                    Pair("Tue", 0.65f),
                    Pair("Wed", 0.5f),
                    Pair("Thu", 0.85f),
                    Pair("Fri", 0.7f),
                    Pair("Sat", 0.6f),
                    Pair("Sun", 0.95f)
                )
                
                data.forEach { (day, fraction) ->
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight(fraction)
                                .background(B360Green, RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(day, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertCard(
    message: String,
    icon: ImageVector,
    tint: Color,
    bgColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Text(
            message,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
    }
}

@Composable
fun QuickAlertsSection(
    lowStockCount: Int,
    pendingOrdersCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Quick Alerts", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            
            AlertCard(
                message = "$lowStockCount products low stock",
                icon = Icons.Filled.Warning,
                tint = B360Amber,
                bgColor = B360Amber.copy(0.12f)
            )
            AlertCard(
                message = "$pendingOrdersCount unpaid orders",
                icon = Icons.Filled.AccessTime,
                tint = B360Red,
                bgColor = B360Red.copy(0.12f)
            )
            AlertCard(
                message = "5 new customers this week",
                icon = Icons.Filled.People,
                tint = B360Green,
                bgColor = B360Green.copy(0.12f)
            )
            AlertCard(
                message = "Mpesa: 2 unreconciled",
                icon = Icons.Filled.Sync,
                tint = B360Blue,
                bgColor = B360Blue.copy(0.12f)
            )
        }
    }
}

@Composable
fun QuickActionChip(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun RecentOrdersSection(orders: List<Order>, onViewAll: () -> Unit, onOrderClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Orders", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                TextButton(onClick = onViewAll) { Text("View All", color = B360Green) }
            }
            orders.take(5).forEachIndexed { index, order ->
                Surface(onClick = { onOrderClick(order.id) }, color = Color.Transparent) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(order.orderNumber, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(order.customerName, color = Color.Gray, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("KES ${"%,.0f".format(order.subtotal)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(color = paymentStatusColor(order.paymentStatus.name).copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                                Text(
                                    order.paymentStatus.displayLabel(),
                                    color = paymentStatusColor(order.paymentStatus.name),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                if (index < orders.size - 1 && index < 4) Divider(color = Color(0xFFF1F5F9))
            }
        }
    }
}

@Composable
fun TopCustomersSection(
    customers: List<Pair<com.app.biashara.domain.model.Customer, com.app.biashara.domain.model.CustomerStats>>,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Top Customers", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                TextButton(onClick = onViewAll) {
                    Text("View All", color = B360Green)
                }
            }
            
            if (customers.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No customer data yet", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                customers.take(4).forEachIndexed { index, (customer, stats) ->
                    val initials = customer.name.split(" ").map { it.firstOrNull()?.toString() ?: "" }.joinToString("").uppercase()
                    val spent = stats.totalSpent
                    val orders = stats.totalOrders
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).background(B360Blue.copy(0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initials, color = B360Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text(customer.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$orders ${if (orders == 1) "order" else "orders"}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Text("KES ${"%,.0f".format(spent)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (index < customers.size - 1 && index < 3) {
                        Divider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}
