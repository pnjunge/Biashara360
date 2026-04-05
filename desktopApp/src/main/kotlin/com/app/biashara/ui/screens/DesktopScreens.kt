package com.app.biashara.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.app.biashara.ui.theme.*

// ─── Dashboard ────────────────────────────────────────────────────────────────

@Composable
fun DesktopDashboardScreen() {
    val scrollState = rememberScrollState()
    Column(
        Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // KPI cards row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(Modifier.weight(1f), "Monthly Revenue", "KES 145,650", "+12%", Icons.Filled.TrendingUp, B360Green)
            KpiCard(Modifier.weight(1f), "Net Profit", "KES 38,200", "+8%", Icons.Filled.AccountBalance, B360Blue)
            KpiCard(Modifier.weight(1f), "Orders Today", "24", "+3", Icons.Filled.ShoppingCart, B360Amber)
            KpiCard(Modifier.weight(1f), "Pending Payments", "KES 12,300", "7 orders", Icons.Filled.Pending, B360Red)
        }

        // Charts + lists row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.height(340.dp)) {
            // Revenue chart placeholder
            Card(Modifier.weight(1.6f).fillMaxHeight(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Revenue Trend", fontWeight = FontWeight.Bold)
                    Text("Last 7 days", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    RevenueBarChart()
                }
            }
            // Quick stats
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AlertCard("3 products low stock", Icons.Filled.Warning, B360Amber)
                AlertCard("7 unpaid orders", Icons.Filled.PendingActions, B360Red)
                AlertCard("5 new customers this week", Icons.Filled.PersonAdd, B360Green)
                AlertCard("Mpesa: 2 unreconciled", Icons.Filled.SyncProblem, B360Blue)
            }
        }

        // Recent orders + top customers
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.weight(1.4f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Recent Orders", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        Triple("B360-0042", "Amina Hassan", "PAID"),
                        Triple("B360-0041", "Brian Otieno", "PENDING"),
                        Triple("B360-0040", "Grace Njeri", "COD"),
                        Triple("B360-0039", "David Kamau", "PAID"),
                        Triple("B360-0038", "Mary Akinyi", "PENDING")
                    ).forEach { (no, name, status) ->
                        DesktopOrderRow(no, name, status)
                        Divider(color = Color(0xFFF5F5F5))
                    }
                }
            }
            Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Top Customers", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        Triple("Amina Hassan", "12 orders", "KES 54,000"),
                        Triple("Grace Njeri", "8 orders", "KES 31,200"),
                        Triple("Brian Otieno", "5 orders", "KES 18,500"),
                        Triple("Mary Akinyi", "3 orders", "KES 9,800")
                    ).forEach { (name, orders, spent) ->
                        TopCustomerRow(name, orders, spent)
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(modifier: Modifier, title: String, value: String, change: String, icon: ImageVector, color: Color) {
    Card(modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontSize = 13.sp, color = Color.Gray)
                Box(Modifier.size(36.dp).background(color.copy(0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
            Text(change, fontSize = 12.sp, color = Color.Gray)
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
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { i, value ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                val heightFraction = value / max
                Box(
                    Modifier
                        .width(28.dp)
                        .fillMaxHeight(heightFraction * 0.8f)
                        .background(
                            Brush.verticalGradient(listOf(B360GreenLight, B360Green)),
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(days[i], fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AlertCard(message: String, icon: ImageVector, color: Color) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.08f))
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Text(message, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DesktopOrderRow(orderNo: String, customer: String, status: String) {
    val statusColor = when (status) {
        "PAID" -> B360Green; "PENDING" -> B360Amber; else -> B360Blue
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(orderNo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = B360Green)
        Text(customer, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        Surface(color = statusColor.copy(0.12f), shape = RoundedCornerShape(20.dp)) {
            Text(status, color = statusColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
        }
    }
}

@Composable
fun TopCustomerRow(name: String, orders: String, spent: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(B360Green.copy(0.1f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Text(name.first().toString(), color = B360Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column {
                Text(name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(orders, fontSize = 11.sp, color = Color.Gray)
            }
        }
        Text(spent, fontWeight = FontWeight.SemiBold, color = B360Green, fontSize = 13.sp)
    }
}

// ─── Inventory ────────────────────────────────────────────────────────────────

@Composable
fun DesktopInventoryScreen() {
    var searchQuery by remember { mutableStateOf("") }
    val products = listOf(
        listOf("Black Dress Size M", "SKU-001", "KES 800", "KES 1,500", "2", "LOW"),
        listOf("Ankara Print Fabric", "SKU-002", "KES 350", "KES 700", "12", "OK"),
        listOf("Gold Hoop Earrings", "SKU-003", "KES 150", "KES 450", "3", "LOW"),
        listOf("White Sneakers 38", "SKU-004", "KES 1,200", "KES 2,200", "0", "OUT"),
        listOf("Silk Blouse Pink", "SKU-005", "KES 600", "KES 1,200", "8", "OK"),
        listOf("Beaded Necklace", "SKU-006", "KES 200", "KES 600", "15", "OK")
    )

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
                Divider()
                products.filter { it[0].contains(searchQuery, ignoreCase = true) || it[1].contains(searchQuery, ignoreCase = true) }
                    .forEach { row ->
                        val statusColor = when (row[5]) { "LOW" -> B360Amber; "OUT" -> B360Red; else -> B360Green }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(row[0], modifier = Modifier.weight(2f), fontWeight = FontWeight.Medium)
                            Text(row[1], modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
                            Text(row[2], modifier = Modifier.weight(1f))
                            Text(row[3], modifier = Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold)
                            Text(row[4], modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = statusColor)
                            Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                Text(row[5], color = statusColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Edit, null, tint = B360Blue, modifier = Modifier.size(16.dp)) }
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.AddBox, null, tint = B360Green, modifier = Modifier.size(16.dp)) }
                            }
                        }
                        Divider(color = Color(0xFFF5F5F5))
                    }
            }
        }
    }
}

// ─── Orders ───────────────────────────────────────────────────────────────────

@Composable
fun DesktopOrdersScreen() {
    val orders = listOf(
        listOf("B360-0042", "Amina Hassan", "0712345678", "KES 4,500", "PAID", "DELIVERED", "Today 2:30PM"),
        listOf("B360-0041", "Brian Otieno", "0723456789", "KES 1,500", "PENDING", "PROCESSING", "Today 11:00AM"),
        listOf("B360-0040", "Grace Njeri", "0734567890", "KES 3,200", "COD", "SHIPPED", "Yesterday"),
        listOf("B360-0039", "David Kamau", "0745678901", "KES 6,800", "PAID", "DELIVERED", "Yesterday"),
        listOf("B360-0038", "Mary Akinyi", "0756789012", "KES 700", "PENDING", "PENDING", "Mon Mar 4")
    )

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Paid", "Pending", "COD").forEach { filter ->
                    FilterChip(selected = filter == "All", onClick = {}, label = { Text(filter) })
                }
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
                Divider()
                orders.forEach { row ->
                    val payColor = when (row[4]) { "PAID" -> B360Green; "PENDING" -> B360Amber; else -> B360Blue }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(row[0], modifier = Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(row[1], modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Medium)
                        Text(row[2], modifier = Modifier.weight(1.2f), color = Color.Gray, fontSize = 13.sp)
                        Text(row[3], modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
                        Surface(color = payColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1.2f)) {
                            Text(row[4], color = payColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Text(row[5], modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = Color.Gray)
                        Text(row[6], modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = Color.Gray)
                    }
                    Divider(color = Color(0xFFF5F5F5))
                }
            }
        }
    }
}

// ─── Customers ────────────────────────────────────────────────────────────────

@Composable
fun DesktopCustomersScreen() {
    val customers = listOf(
        listOf("Amina Hassan", "0712345678", "12", "KES 54,000", "540"),
        listOf("Grace Njeri", "0734567890", "8", "KES 31,200", "312"),
        listOf("Brian Otieno", "0723456789", "5", "KES 18,500", "185"),
        listOf("David Kamau", "0745678901", "4", "KES 16,800", "168"),
        listOf("Mary Akinyi", "0756789012", "3", "KES 9,800", "98")
    )

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Search customers...") },
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
                Divider()
                customers.forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1.5f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).background(B360Green.copy(0.1f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                                Text(row[0].first().toString(), color = B360Green, fontWeight = FontWeight.Bold)
                            }
                            Text(row[0], fontWeight = FontWeight.Medium)
                        }
                        Text(row[1], Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
                        Text(row[2], Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(row[3], Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, null, tint = B360Amber, modifier = Modifier.size(14.dp))
                            Text(row[4], fontWeight = FontWeight.Medium)
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Visibility, null, tint = B360Blue, modifier = Modifier.size(16.dp)) }
                            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Chat, null, tint = B360Green, modifier = Modifier.size(16.dp)) }
                        }
                    }
                    Divider(color = Color(0xFFF5F5F5))
                }
            }
        }
    }
}

// ─── Expenses ─────────────────────────────────────────────────────────────────

@Composable
fun DesktopExpensesScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStatCard(Modifier.weight(1f), "Total This Month", "KES 65,500", B360Red)
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
        Card(Modifier.fillWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(16.dp, 12.dp)) {
                    listOf("Description", "Category", "Amount", "Date", "Actions").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 0) 2f else 1f))
                    }
                }
                Divider()
                listOf(
                    listOf("Facebook & Instagram Ads", "ADVERTISING", "KES 5,000", "Mar 7"),
                    listOf("Packaging materials", "PACKAGING", "KES 1,200", "Mar 6"),
                    listOf("Rider delivery fees", "DELIVERY", "KES 800", "Mar 6"),
                    listOf("Monthly shop rent", "RENT", "KES 15,000", "Mar 1"),
                    listOf("Fabric & stock purchase", "STOCK_PURCHASE", "KES 45,000", "Mar 1")
                ).forEach { row ->
                    val catColor = when (row[1]) { "ADVERTISING" -> B360Blue; "RENT" -> B360Red; "STOCK_PURCHASE" -> B360Green; "DELIVERY" -> B360Amber; else -> Color.Gray }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(row[0], Modifier.weight(2f), fontWeight = FontWeight.Medium)
                        Surface(color = catColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                            Text(row[1].replace("_", " "), color = catColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Text(row[2], Modifier.weight(1f), color = B360Red, fontWeight = FontWeight.SemiBold)
                        Text(row[3], Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
                        IconButton(onClick = {}, Modifier.weight(1f).size(28.dp)) { Icon(Icons.Filled.Delete, null, tint = B360Red, modifier = Modifier.size(16.dp)) }
                    }
                    Divider(color = Color(0xFFF5F5F5))
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

private fun Modifier.fillWidth() = this.fillMaxWidth()

// ─── Payments ─────────────────────────────────────────────────────────────────

@Composable
fun DesktopPaymentsScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStatCard(Modifier.weight(1f), "Total Collected", "KES 133,350", B360Green)
            SummaryStatCard(Modifier.weight(1f), "Unreconciled", "KES 3,500", B360Amber)
            SummaryStatCard(Modifier.weight(1f), "Mpesa Transactions", "47", B360Blue)
            SummaryStatCard(Modifier.weight(1f), "Failed Payments", "2", B360Red)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(16.dp, 12.dp)) {
                    listOf("Mpesa Code", "Customer", "Phone", "Amount", "Channel", "Status", "Date", "").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 7) 0.6f else 1f))
                    }
                }
                Divider()
                listOf(
                    listOf("RGK71HXYZ", "Amina Hassan", "0712345678", "KES 4,500", "Mpesa", "RECONCILED", "Today 2:30PM"),
                    listOf("PLM23NQRS", "David Kamau", "0745678901", "KES 6,800", "Mpesa", "RECONCILED", "Yesterday"),
                    listOf("QWE45RTYU", "Sarah Wangui", "0767890123", "KES 1,200", "Airtel", "PENDING", "Yesterday"),
                    listOf("ZXC89VBNM", "Tom Mutua", "0778901234", "KES 2,300", "Mpesa", "PENDING", "Mon")
                ).forEach { row ->
                    val statusColor = if (row[5] == "RECONCILED") B360Green else B360Amber
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(row[0], Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(row[1], Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Text(row[2], Modifier.weight(1f), color = Color.Gray, fontSize = 12.sp)
                        Text(row[3], Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(row[4], Modifier.weight(1f), fontSize = 12.sp)
                        Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                            Text(row[5], color = statusColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Text(row[6], Modifier.weight(1f), color = Color.Gray, fontSize = 12.sp)
                        if (row[5] == "PENDING") {
                            TextButton(onClick = {}, modifier = Modifier.weight(0.6f)) { Text("Match", fontSize = 11.sp, color = B360Blue) }
                        } else Spacer(Modifier.weight(0.6f))
                    }
                    Divider(color = Color(0xFFF5F5F5))
                }
            }
        }
    }
}

// ─── Reports ──────────────────────────────────────────────────────────────────

@Composable
fun DesktopReportsScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // P&L Summary
            Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profit & Loss — March 2025", fontWeight = FontWeight.Bold)
                    Divider()
                    PnlRow("Total Revenue", "KES 145,650", B360Green)
                    PnlRow("Cost of Goods Sold", "KES 72,000", B360Red)
                    PnlRow("Gross Profit", "KES 73,650", B360Green, bold = true)
                    PnlRow("Total Expenses", "KES 35,450", B360Red)
                    Divider()
                    PnlRow("Net Profit", "KES 38,200", B360Green, bold = true, large = true)
                    PnlRow("Net Margin", "26.2%", B360Blue)
                }
            }
            // Expense breakdown
            Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Expense Breakdown", fontWeight = FontWeight.Bold)
                    Divider()
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
    Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Divider()
            content()
        }
    }
}

@Composable
fun SettingsField(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.Gray)
        OutlinedTextField(value = value, onValueChange = {}, modifier = Modifier.width(300.dp), shape = RoundedCornerShape(8.dp), singleLine = true)
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean) {
    var state by remember { mutableStateOf(checked) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                    Divider(color = Color(0xFFF0F0F0))
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
                    Divider(color = Color(0xFFF0F0F0))
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
            Divider()
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
        Divider(Modifier.fillMaxHeight().width(1.dp))
        // Chat panel placeholder
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Forum, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Select a conversation", color = Color.Gray, fontSize = 16.sp)
        }
    }
}
