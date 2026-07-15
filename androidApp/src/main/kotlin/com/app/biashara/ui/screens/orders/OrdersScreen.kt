package com.app.biashara.ui.screens.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.app.biashara.ui.kmpViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOrderDetail: (String) -> Unit,
    onCreateOrder: () -> Unit,
    viewModel: OrdersViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadOrders() }

    val tabs = listOf("All" to null, "Paid" to PaymentStatus.PAID, "Pending" to PaymentStatus.PENDING, "COD" to PaymentStatus.COD)
    val selectedTabIndex = tabs.indexOfFirst { it.second == state.selectedTabStatus }.takeIf { it >= 0 } ?: 0

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Orders / Maagizo", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = B360Surface,
                    contentColor = B360Green,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = B360Green
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, (label, status) ->
                        val isSelected = selectedTabIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(status) },
                            text = { Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            selectedContentColor = B360Green,
                            unselectedContentColor = Color(0xFF64748B)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateOrder,
                containerColor = B360Green,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Order")
            }
        }
    ) { padding ->
        if (state.isLoading && state.orders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).background(B360Surface), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        if (state.filteredOrders.isEmpty() && !state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding).background(B360Surface), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.ShoppingCart, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Text("No orders yet", color = Color.Gray)
                    Button(onClick = onCreateOrder, colors = ButtonDefaults.buttonColors(containerColor = B360Green), shape = RoundedCornerShape(20.dp)) {
                        Text("Create First Order")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(B360Surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.orderNumber, fontWeight = FontWeight.Bold, color = B360Green, fontSize = 15.sp)
                Text(order.createdAt.toString().substring(0, 10), fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Person, null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B))
                Text(order.customerName, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Phone, null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                Text(order.customerPhone, fontSize = 13.sp, color = Color(0xFF64748B))
            }
            if (!order.mpesaTransactionCode.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(14.dp), tint = B360Green)
                    Text("Mpesa: ${order.mpesaTransactionCode}", fontSize = 12.sp, color = B360Green, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("KES ${"%,.0f".format(order.subtotal)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
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
            label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrdersViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()
    val order = state.orders.find { it.id == orderId }

    LaunchedEffect(Unit) {
        if (state.orders.isEmpty()) viewModel.loadOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order ${order?.orderNumber ?: "#${orderId.take(8).uppercase()}"}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color(0xFF0F172A)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        }
    ) { padding ->
        if (order == null) {
            Box(Modifier.fillMaxSize().padding(padding).background(B360Surface), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).background(B360Surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Order Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Customer", color = Color(0xFF64748B)); Text(order.customerName, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Phone", color = Color(0xFF64748B)); Text(order.customerPhone, color = Color(0xFF0F172A))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payment Status", color = Color(0xFF64748B))
                        Text(order.paymentStatus.displayLabel(), color = paymentStatusColor(order.paymentStatus.name), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Status", color = Color(0xFF64748B)); Text(order.deliveryStatus.displayLabel(), color = Color(0xFF0F172A))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", color = Color(0xFF64748B)); Text("KES ${"%,.0f".format(order.subtotal)}", fontWeight = FontWeight.Bold, color = B360Green)
                    }
                    if (!order.mpesaTransactionCode.isNullOrEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("M-Pesa Code", color = Color(0xFF64748B)); Text(order.mpesaTransactionCode!!, color = B360Green, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (order.items.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Items (${order.totalItems})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        order.items.forEach { item ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.productName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                    Text("Qty: ${item.quantity} × KES ${"%,.0f".format(item.unitPrice)}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("KES ${"%,.0f".format(item.lineTotal)}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
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
                title = { Text("New Order", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color(0xFF0F172A)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOrderCreated,
                containerColor = B360Green,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Filled.Check, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Create Order", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).background(B360Surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add items to create an order", color = Color.Gray)
        }
    }
}
