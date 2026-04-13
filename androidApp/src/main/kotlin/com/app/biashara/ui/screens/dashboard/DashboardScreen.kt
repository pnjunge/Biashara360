package com.app.biashara.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = koinInject()
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
            item {
                RevenueBannerCard(
                    monthRevenue = state.monthRevenue,
                    netProfit = state.netProfit,
                    pendingOrders = state.pendingOrders
                )
            }
            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { QuickActionChip(Icons.Filled.Add, "New Order", B360Green) { navController.navigate(Screen.CreateOrder.route) } }
                    item { QuickActionChip(Icons.Filled.Inventory, "Add Stock", B360Blue) { navController.navigate(Screen.Inventory.route) } }
                    item { QuickActionChip(Icons.Filled.Receipt, "Add Expense", B360Amber) { navController.navigate(Screen.AddExpense.route) } }
                    item { QuickActionChip(Icons.Filled.People, "New Customer", Color(0xFF7B1FA2)) { navController.navigate(Screen.Customers.route) } }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), "Total Orders", "${state.totalOrders}", Icons.Filled.ShoppingCart, B360Green)
                    StatCard(Modifier.weight(1f), "Low Stock", "${state.lowStockCount}", Icons.Filled.Warning, B360Amber)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), "Customers", "${state.topCustomers.size}", Icons.Filled.People, B360Blue)
                    StatCard(Modifier.weight(1f), "Unpaid Orders", "${state.pendingOrders}", Icons.Filled.Pending, B360Red)
                }
            }
            if (state.lowStockProducts.isNotEmpty()) {
                item {
                    LowStockAlertsSection(
                        products = state.lowStockProducts,
                        onViewAll = { navController.navigate(Screen.Inventory.route) }
                    )
                }
            }
            if (state.recentOrders.isNotEmpty()) {
                item {
                    RecentOrdersSection(
                        orders = state.recentOrders,
                        onViewAll = { navController.navigate(Screen.Orders.route) },
                        onOrderClick = { id -> navController.navigate(Screen.OrderDetail.createRoute(id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun RevenueBannerCard(monthRevenue: Double, netProfit: Double, pendingOrders: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(B360Green, B360GreenDark)))
                .padding(20.dp)
        ) {
            Column {
                Text("This Month's Revenue", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Text("KES ${"%,.0f".format(monthRevenue)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    MiniStat("Net Profit", "KES ${"%,.0f".format(netProfit)}", Color.White)
                    MiniStat("Pending Orders", "$pendingOrders", Color(0xFFFFD54F))
                }
            }
        }
    }
}

@Composable
fun MiniStat(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
fun StatCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun LowStockAlertsSection(products: List<Product>, onViewAll: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Warning, null, tint = B360Amber, modifier = Modifier.size(18.dp))
                    Text("Low Stock Alerts", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onViewAll) { Text("View All") }
            }
            products.take(5).forEachIndexed { index, product ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(product.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Badge(containerColor = if (product.isOutOfStock) B360Red else B360Amber) {
                        Text("${product.currentStock} left", color = Color.White, fontSize = 11.sp)
                    }
                }
                if (index < products.size - 1 && index < 4) Divider(color = Color(0xFFF0F0F0))
            }
        }
    }
}

@Composable
fun RecentOrdersSection(orders: List<Order>, onViewAll: () -> Unit, onOrderClick: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Orders", fontWeight = FontWeight.Bold)
                TextButton(onClick = onViewAll) { Text("View All") }
            }
            orders.take(5).forEachIndexed { index, order ->
                Surface(onClick = { onOrderClick(order.id) }, color = Color.Transparent) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(order.orderNumber, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(order.customerName, color = Color.Gray, fontSize = 12.sp)
                        }
                        Surface(color = paymentStatusColor(order.paymentStatus.name).copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                            Text(
                                order.paymentStatus.displayLabel(),
                                color = paymentStatusColor(order.paymentStatus.name),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                if (index < orders.size - 1 && index < 4) Divider(color = Color(0xFFF0F0F0))
            }
        }
    }
}
