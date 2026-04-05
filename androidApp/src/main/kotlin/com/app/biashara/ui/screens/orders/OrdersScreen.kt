package com.app.biashara.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.*

data class OrderUi(
    val id: String, val orderNumber: String,
    val customerName: String, val customerPhone: String,
    val items: Int, val total: Double,
    val paymentStatus: String, val deliveryStatus: String,
    val date: String, val txCode: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOrderDetail: (String) -> Unit,
    onCreateOrder: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Paid", "Pending", "COD")

    val sampleOrders = listOf(
        OrderUi("1", "B360-0042", "Amina Hassan", "0712345678", 3, 4500.0, "PAID", "DELIVERED", "Today, 2:30PM", "RGK71HXYZ"),
        OrderUi("2", "B360-0041", "Brian Otieno", "0723456789", 1, 1500.0, "PENDING", "PROCESSING", "Today, 11:00AM"),
        OrderUi("3", "B360-0040", "Grace Njeri", "0734567890", 2, 3200.0, "COD", "SHIPPED", "Yesterday"),
        OrderUi("4", "B360-0039", "David Kamau", "0745678901", 4, 6800.0, "PAID", "DELIVERED", "Yesterday", "PLM23NQRS"),
        OrderUi("5", "B360-0038", "Mary Akinyi", "0756789012", 1, 700.0, "PENDING", "PENDING", "Mon, Mar 4")
    )

    val filteredOrders = when (selectedTab) {
        1 -> sampleOrders.filter { it.paymentStatus == "PAID" }
        2 -> sampleOrders.filter { it.paymentStatus == "PENDING" }
        3 -> sampleOrders.filter { it.paymentStatus == "COD" }
        else -> sampleOrders
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Orders / Maagizo", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 13.sp) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateOrder,
                containerColor = B360Green,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Order")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredOrders) { order ->
                OrderCard(order = order, onClick = { onOrderDetail(order.id) })
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun OrderCard(order: OrderUi, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.orderNumber, fontWeight = FontWeight.Bold, color = B360Green)
                Text(order.date, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Text(order.customerName, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Text(order.customerPhone, fontSize = 13.sp, color = Color.Gray)
            }
            if (!order.txCode.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = B360Green)
                    Text("Mpesa: ${order.txCode}", fontSize = 12.sp, color = B360Green)
                }
            }
            Spacer(Modifier.height(10.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("KES ${"%,.0f".format(order.total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(order.paymentStatus, paymentStatusColor(order.paymentStatus))
                    StatusBadge(order.deliveryStatus, Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(orderId: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order #${orderId.take(8).uppercase()}", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Status"); Text("Paid", color = B360Green, fontWeight = FontWeight.SemiBold) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total"); Text("KES 4,500", fontWeight = FontWeight.Bold) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Payment"); Text("M-Pesa") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(onBack: () -> Unit, onOrderCreated: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Order", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onOrderCreated, containerColor = B360Green) {
                Icon(Icons.Filled.Check, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Create Order", color = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Add items to create an order", color = Color.Gray)
        }
    }
}
