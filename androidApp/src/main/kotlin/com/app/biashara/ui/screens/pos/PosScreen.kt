package com.app.biashara.ui.screens.pos

import androidx.compose.foundation.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.UserSession
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.CreateOrderUseCase
import com.app.biashara.domain.usecase.generateId
import com.app.biashara.presentation.viewmodel.CustomersViewModel
import com.app.biashara.presentation.viewmodel.InventoryViewModel
import com.app.biashara.ui.kmpViewModel
import com.app.biashara.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

data class MobileCartItem(val product: Product, var qty: Int)

// ─── Filter Tab Model ─────────────────────────────────────────────────────────
private enum class PosFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Filled.GridView),
    FAVORITES("Favorites", Icons.Filled.StarBorder),
    CATEGORIES("Categories", Icons.Filled.LocalOffer),
    RECENT("Recent", Icons.Filled.AccessTime)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    inventoryViewModel: InventoryViewModel = kmpViewModel(),
    customersViewModel: CustomersViewModel = kmpViewModel(),
    createOrderUseCase: CreateOrderUseCase = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val businessId = remember { UserSession.getBusinessId() }

    LaunchedEffect(Unit) {
        inventoryViewModel.loadProducts(businessId)
        customersViewModel.loadCustomers()
    }

    val inventoryState by inventoryViewModel.state.collectAsState()
    val customersState by customersViewModel.state.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PosFilter.ALL) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(inventoryState.products) {
        listOf("All") + inventoryState.products.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val filteredProducts = remember(inventoryState.products, searchQuery, selectedFilter) {
        inventoryState.products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.sku.contains(searchQuery, ignoreCase = true)
            matchesSearch && p.isActive
        }
    }

    val cart = remember { mutableStateListOf<MobileCartItem>() }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var walkInName by remember { mutableStateOf("Walk-In Customer") }
    var walkInPhone by remember { mutableStateOf("+254000000000") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }
    var isCheckingOut by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successOrderNumber by remember { mutableStateOf<String?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }

    val subtotal = cart.sumOf { it.product.sellingPrice * it.qty }
    val tax = subtotal * 0.16
    val grandTotal = subtotal + tax

    Scaffold(
        containerColor = Color(0xFFF8FAFB),
        topBar = {
            Surface(color = Color(0xFFF8FAFB), shadowElevation = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("Point of Sale", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF0F172A))
                            Text("Search and select products to start a sale", fontSize = 13.sp, color = Color(0xFF64748B))
                        }
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F5EE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = B360Green, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // ── Search Bar ────────────────────────────────────────────
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name or SKU...", color = Color(0xFFB0BBC8)) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp)) },
                        trailingIcon = { Icon(Icons.Filled.QrCodeScanner, "Scan", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = B360Green,
                            unfocusedBorderColor = Color(0xFFE9EFF6),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Filter Tabs ───────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosFilter.values().forEach { filter ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                onClick = { selectedFilter = filter },
                                shape = RoundedCornerShape(24.dp),
                                color = if (isSelected) B360Green else Color.White,
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        filter.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        filter.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        floatingActionButton = {
            if (cart.isNotEmpty() && successOrderNumber == null) {
                ExtendedFloatingActionButton(
                    onClick = { showCartSheet = true },
                    containerColor = B360Green,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Filled.ShoppingCart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cart (${cart.sumOf { it.qty }}) • KES ${"%,.0f".format(grandTotal)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFB))) {
            when {
                inventoryState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = B360Green)
                    }
                }
                filteredProducts.isEmpty() -> {
                    // ── Empty State ───────────────────────────────────────────
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Illustration circle
                        Box(
                            modifier = Modifier.size(160.dp).clip(CircleShape).background(Color(0xFFE8F5EE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Inventory2,
                                contentDescription = null,
                                tint = B360Green.copy(alpha = 0.55f),
                                modifier = Modifier.size(80.dp)
                            )
                            // Magnifier overlay (top-right)
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier.padding(12.dp).size(36.dp).clip(CircleShape).background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Search, null, tint = B360Green, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Text("No products match filter", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Try adjusting your search or filter\nto find what you're looking for.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(28.dp))
                        OutlinedButton(
                            onClick = { searchQuery = ""; selectedFilter = PosFilter.ALL },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, B360Green),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = B360Green)
                        ) {
                            Icon(Icons.Filled.FilterListOff, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clear filters", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts) { p ->
                            val isOutOfStock = p.isOutOfStock
                            val currentCartQty = cart.find { it.product.id == p.id }?.qty ?: 0
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (!isOutOfStock) {
                                        val existing = cart.find { it.product.id == p.id }
                                        if (existing != null) {
                                            if (existing.qty < p.currentStock) {
                                                val idx = cart.indexOf(existing)
                                                cart[idx] = existing.copy(qty = existing.qty + 1)
                                            } else errorMessage = "Only ${p.currentStock} in stock."
                                        } else cart.add(MobileCartItem(p, 1))
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F5EE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(p.name.take(2).uppercase(), fontWeight = FontWeight.ExtraBold, color = B360Green, fontSize = 16.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                        Text("SKU: ${p.sku}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        Spacer(Modifier.height(4.dp))
                                        Text("KES ${"%,.0f".format(p.sellingPrice)}", fontWeight = FontWeight.ExtraBold, color = B360Green, fontSize = 14.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val stockColor = if (isOutOfStock) B360Red else if (p.isLowStock) B360Amber else Color(0xFF10B981)
                                        Surface(shape = RoundedCornerShape(8.dp), color = stockColor.copy(alpha = 0.1f)) {
                                            Text(
                                                text = if (isOutOfStock) "Out" else "${p.currentStock} left",
                                                color = stockColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (currentCartQty > 0) {
                                            Surface(shape = RoundedCornerShape(8.dp), color = B360Green.copy(alpha = 0.1f)) {
                                                Text("$currentCartQty in cart", color = B360Green, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(84.dp)) }
                    }
                }
            }
        }

        // ── Success Dialog ────────────────────────────────────────────────────
        successOrderNumber?.let { orderNo ->
            AlertDialog(
                onDismissRequest = { successOrderNumber = null },
                title = { Text("Checkout Successful", fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CheckCircle, null, tint = B360Green, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Order Number: $orderNo", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                },
                confirmButton = {
                    Button(onClick = { successOrderNumber = null }, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                        Text("New Sale", color = Color.White)
                    }
                }
            )
        }

        // ── Cart Bottom Sheet ─────────────────────────────────────────────────
        if (showCartSheet) {
            ModalBottomSheet(onDismissRequest = { showCartSheet = false }, containerColor = Color.White) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Checkout Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    errorMessage?.let { Text(it, color = B360Red, fontSize = 12.sp) }

                    LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cart) { item ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("KES ${"%,.0f".format(item.product.sellingPrice)}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        val idx = cart.indexOf(item)
                                        if (item.qty > 1) cart[idx] = item.copy(qty = item.qty - 1) else cart.remove(item)
                                    }) { Icon(Icons.Filled.Remove, null, modifier = Modifier.size(16.dp)) }
                                    Text(item.qty.toString(), fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        if (item.qty < item.product.currentStock) {
                                            val idx = cart.indexOf(item); cart[idx] = item.copy(qty = item.qty + 1)
                                        }
                                    }) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp)) }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Walk-In Customer",
                            onValueChange = {}, readOnly = true, label = { Text("Customer") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.ArrowDropDown, null) } },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green, unfocusedBorderColor = Color(0xFFE2E8F0))
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                            DropdownMenuItem(text = { Text("Walk-In Customer") }, onClick = { selectedCustomer = null; walkInName = "Walk-In Customer"; walkInPhone = "+254000000000"; expanded = false })
                            customersState.customers.forEach { c ->
                                DropdownMenuItem(text = { Text("${c.name} (${c.phone})") }, onClick = { selectedCustomer = c; walkInName = c.name; walkInPhone = c.phone; expanded = false })
                            }
                        }
                    }

                    if (selectedCustomer == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = walkInName, onValueChange = { walkInName = it }, label = { Text("Name") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green, unfocusedBorderColor = Color(0xFFE2E8F0)))
                            OutlinedTextField(value = walkInPhone, onValueChange = { walkInPhone = it }, label = { Text("Phone") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green, unfocusedBorderColor = Color(0xFFE2E8F0)))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(PaymentMethod.CASH, PaymentMethod.MPESA, PaymentMethod.CARD).forEach { pm ->
                            val isSel = paymentMethod == pm
                            Button(onClick = { paymentMethod = pm }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isSel) B360Green else Color(0xFFF1F5F9), contentColor = if (isSel) Color.White else Color(0xFF334155))) {
                                Text(pm.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Sale Notes") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green, unfocusedBorderColor = Color(0xFFE2E8F0)))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total amount due", color = Color.Gray, fontSize = 13.sp)
                        Text("KES ${"%,.0f".format(grandTotal)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = B360Green)
                    }

                    Button(
                        onClick = {
                            isCheckingOut = true; errorMessage = null
                            coroutineScope.launch {
                                val order = Order(
                                    id = generateId(), orderNumber = "B360-POS-${System.currentTimeMillis() % 10000}",
                                    businessId = businessId, customerId = selectedCustomer?.id,
                                    customerName = walkInName, customerPhone = walkInPhone,
                                    deliveryLocation = "In-Store POS",
                                    items = cart.map { OrderItem(productId = it.product.id, productName = it.product.name, quantity = it.qty, unitPrice = it.product.sellingPrice, buyingPrice = it.product.buyingPrice) },
                                    paymentStatus = if (paymentMethod == PaymentMethod.CASH) PaymentStatus.PAID else PaymentStatus.PENDING,
                                    deliveryStatus = DeliveryStatus.DELIVERED, paymentMethod = paymentMethod, notes = notes,
                                    createdAt = Clock.System.now(), updatedAt = Clock.System.now()
                                )
                                createOrderUseCase(order)
                                    .onSuccess { saved -> successOrderNumber = saved.orderNumber; cart.clear(); notes = ""; showCartSheet = false; inventoryViewModel.loadProducts(businessId) }
                                    .onFailure { err -> errorMessage = err.message ?: "Failed to save sale." }
                                isCheckingOut = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isCheckingOut && cart.isNotEmpty()
                    ) {
                        if (isCheckingOut) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Complete POS Checkout", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
