package com.app.biashara.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.app.biashara.ui.navigation.Screen
import com.app.biashara.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    // In full impl, collect from DashboardViewModel via koin
    val businessName = "Wanjiru's Fashion"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Habari, Wanjiru! 👋", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(businessName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(B360Surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Revenue Banner
            item {
                RevenueBannerCard(
                    monthRevenue = 145_650.0,
                    netProfit = 38_200.0,
                    pendingPayments = 12_300.0
                )
            }
            // Quick Actions
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
            // Stats Row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), "Orders Today", "24", Icons.Filled.ShoppingCart, B360Green)
                    StatCard(Modifier.weight(1f), "Low Stock", "3", Icons.Filled.Warning, B360Amber)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), "Customers", "186", Icons.Filled.People, B360Blue)
                    StatCard(Modifier.weight(1f), "Unpaid Orders", "7", Icons.Filled.Pending, B360Red)
                }
            }
            // Low Stock Alerts
            item {
                LowStockAlertsSection(
                    onViewAll = { navController.navigate(Screen.Inventory.route) }
                )
            }
            // Recent Orders
            item {
                RecentOrdersSection(
                    onViewAll = { navController.navigate(Screen.Orders.route) }
                )
            }
        }
    }
}

@Composable
fun RevenueBannerCard(monthRevenue: Double, netProfit: Double, pendingPayments: Double) {
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
                    MiniStat("Pending", "KES ${"%,.0f".format(pendingPayments)}", Color(0xFFFFD54F))
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
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    ) {
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
fun LowStockAlertsSection(onViewAll: () -> Unit) {
    // Sample data
    val lowStockItems = listOf(
        Pair("Black Dress Size M", 2),
        Pair("Ankara Print Fabric", 1),
        Pair("Gold Hoop Earrings", 3)
    )
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = B360Amber, modifier = Modifier.size(18.dp))
                    Text("Low Stock Alerts", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onViewAll) { Text("View All") }
            }
            lowStockItems.forEach { (name, qty) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    Badge(containerColor = if (qty <= 1) B360Red else B360Amber) {
                        Text("$qty left", color = Color.White, fontSize = 11.sp)
                    }
                }
                if (name != lowStockItems.last().first) Divider(color = Color(0xFFF0F0F0))
            }
        }
    }
}

@Composable
fun RecentOrdersSection(onViewAll: () -> Unit) {
    val orders = listOf(
        Triple("B360-0042", "Amina Hassan", "PAID"),
        Triple("B360-0041", "Brian Otieno", "PENDING"),
        Triple("B360-0040", "Grace Njeri", "COD")
    )
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Orders", fontWeight = FontWeight.Bold)
                TextButton(onClick = onViewAll) { Text("View All") }
            }
            orders.forEach { (orderNo, customer, status) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(orderNo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(customer, color = Color.Gray, fontSize = 12.sp)
                    }
                    Surface(color = paymentStatusColor(status).copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                        Text(status, color = paymentStatusColor(status), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                if (orderNo != orders.last().first) Divider(color = Color(0xFFF0F0F0))
            }
        }
    }
}
