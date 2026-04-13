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
import com.app.biashara.domain.model.Order
import com.app.biashara.domain.model.PaymentStatus
import com.app.biashara.presentation.viewmodel.OrdersViewModel
import com.app.biashara.ui.theme.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOrderDetail: (String) -> Unit,
    onCreateOrder: () -> Unit,
    viewModel: OrdersViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadOrders() }

    val tabs = listOf("All" to null, "Paid" to PaymentStatus.PAID, "Pending" to PaymentStatus.PENDING, "COD" to PaymentStatus.COD)
    val selectedTabIndex = tabs.indexOfFirst { it.second == state.selectedTabStatus }.takeIf { it >= 0 } ?: 0

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Orders / Maagizo", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                TabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.White) {
                    tabs.forEachIndexed { index, (label, status) ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { viewModel.selectTab(status) },
                            text = { Text(label, fontSize = 13.sp) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateOrder, containerColor = B360Green, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "New Order")
            }
        }
    ) { padding ->
        if (state.isLoading && state.orders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        if (state.filteredOrders.isEmpty() && !state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.ShoppingCart, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Text("No orders yet", color = Color.Gray)
                    Button(onClick = onCreateOrder, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                        Text("Create First Order")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.filteredOrders) { order ->
                OrderCard(order = order, onClick = { onOrderDetail(order.id) })
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
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
                Text(order.createdAt.toString().substring(0, 10), fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Text(order.customerName, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Phone, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Text(order.customerPhone, fontSize = 13.sp, color = Color.Gray)
            }
            if (!order.mpesaTransactionCode.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(14.dp), tint = B360Green)
                    Text("Mpesa: ${order.mpesaTransactionCode}", fontSize = 12.sp, color = B360Green)
                }
            }
            Spacer(Modifier.height(10.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("KES ${"%,.0f".format(order.subtotal)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(order.paymentStatus.displayLabel(), paymentStatusColor(order.paymentStatus.name))
                    StatusBadge(order.deliveryStatus.displayLabel(), Color.Gray)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Text(
            label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrdersViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val order = state.orders.find { it.id == orderId }

    LaunchedEffect(Unit) {
        if (state.orders.isEmpty()) viewModel.loadOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order ${order?.orderNumber ?: "#${orderId.take(8).uppercase()}"}", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (order == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Customer"); Text(order.customerName, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Phone"); Text(order.customerPhone)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payment Status")
                        Text(order.paymentStatus.displayLabel(), color = paymentStatusColor(order.paymentStatus.name), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Status"); Text(order.deliveryStatus.displayLabel())
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total"); Text("KES ${"%,.0f".format(order.subtotal)}", fontWeight = FontWeight.Bold)
                    }
                    if (!order.mpesaTransactionCode.isNullOrEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("M-Pesa Code"); Text(order.mpesaTransactionCode!!, color = B360Green)
                        }
                    }
                }
            }
            if (order.items.isNotEmpty()) {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Items (${order.totalItems})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        order.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text("Qty: ${item.quantity} × KES ${"%,.0f".format(item.unitPrice)}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("KES ${"%,.0f".format(item.lineTotal)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
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
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add items to create an order", color = Color.Gray)
        }
    }
}
