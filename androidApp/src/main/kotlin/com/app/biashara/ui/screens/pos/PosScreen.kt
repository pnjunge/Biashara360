package com.app.biashara.ui.screens.pos

import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

data class MobileCartItem(
    val product: Product,
    var qty: Int
)

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
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(inventoryState.products) {
        listOf("All") + inventoryState.products.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val filteredProducts = remember(inventoryState.products, searchQuery, selectedCategory) {
        inventoryState.products.filter { p ->
            val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) || p.sku.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || p.category == selectedCategory
            matchesSearch && matchesCategory && p.isActive
        }
    }

    // Cart and Transaction State
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
        topBar = {
            TopAppBar(
                title = { Text("Point of Sale", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (cart.isNotEmpty() && successOrderNumber == null) {
                ExtendedFloatingActionButton(
                    onClick = { showCartSheet = true },
                    containerColor = B360Green,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.ShoppingCart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cart (${cart.sumOf { it.qty }}) • KES ${"%,.0f".format(grandTotal)}")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC))
        ) {
            // Search Bar & Categories
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or SKU...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = B360Green,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Horizontal Category filter chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = B360Green,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Products list
            if (inventoryState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = B360Green)
                }
            } else if (filteredProducts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No products match filter", color = Color.Gray)
                }
            } else {
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
                                            existing.qty += 1
                                            val idx = cart.indexOf(existing)
                                            cart[idx] = existing.copy()
                                        } else {
                                            errorMessage = "Cannot add more. Only ${p.currentStock} in stock."
                                        }
                                    } else {
                                        cart.add(MobileCartItem(p, 1))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Product Logo Image Placeholder
                                Box(
                                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(p.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = B360Green, fontSize = 16.sp)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("SKU: ${p.sku}", fontSize = 11.sp, color = Color.Gray)
                                    Text("KES ${"%,.0f".format(p.sellingPrice)}", fontWeight = FontWeight.ExtraBold, color = B360Green, fontSize = 13.sp)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val stockColor = if (isOutOfStock) B360Red else if (p.isLowStock) B360Amber else B360Green
                                    Text(
                                        text = if (isOutOfStock) "Out" else "${p.currentStock} left",
                                        color = stockColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.background(stockColor.copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                    )

                                    if (currentCartQty > 0) {
                                        Text(
                                            text = "$currentCartQty in cart",
                                            color = B360Green,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(84.dp)) }
                }
            }
        }

        // Checkout success alert dialog
        successOrderNumber?.let { orderNo ->
            AlertDialog(
                onDismissRequest = { successOrderNumber = null },
                title = { Text("Checkout Successful") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CheckCircle, null, tint = B360Green, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Transaction saved correctly.", color = Color.Gray)
                        Text("Order Number: $orderNo", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { successOrderNumber = null },
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                    ) {
                        Text("New Sale", color = Color.White)
                    }
                }
            )
        }

        // Cart Drawer / Bottom Sheet
        if (showCartSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCartSheet = false },
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Checkout Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                    // Error text
                    errorMessage?.let { msg ->
                        Text(msg, color = B360Red, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    // Cart item list
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false).heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cart) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("KES ${"%,.0f".format(item.product.sellingPrice)}", fontSize = 11.sp, color = Color.Gray)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        if (item.qty > 1) {
                                            item.qty -= 1
                                            val idx = cart.indexOf(item)
                                            cart[idx] = item.copy()
                                        } else {
                                            cart.remove(item)
                                        }
                                    }) {
                                        Icon(Icons.Filled.Remove, null, modifier = Modifier.size(16.dp))
                                    }
                                    Text(item.qty.toString(), fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        if (item.qty < item.product.currentStock) {
                                            item.qty += 1
                                            val idx = cart.indexOf(item)
                                            cart[idx] = item.copy()
                                        }
                                    }) {
                                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    Divider()

                    // Customer Selection dropdown
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Walk-In Customer",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Walk-In Customer") },
                                onClick = {
                                    selectedCustomer = null
                                    walkInName = "Walk-In Customer"
                                    walkInPhone = "+254000000000"
                                    expanded = false
                                }
                            )
                            customersState.customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (${c.phone})") },
                                    onClick = {
                                        selectedCustomer = c
                                        walkInName = c.name
                                        walkInPhone = c.phone
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedCustomer == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = walkInName,
                                onValueChange = { walkInName = it },
                                label = { Text("Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = walkInPhone,
                                onValueChange = { walkInPhone = it },
                                label = { Text("Phone") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    // Payment Method selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(PaymentMethod.CASH, PaymentMethod.MPESA, PaymentMethod.CARD).forEach { pm ->
                            val isSelected = paymentMethod == pm
                            Button(
                                onClick = { paymentMethod = pm },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) B360Green else Color(0xFFF1F5F9),
                                    contentColor = if (isSelected) Color.White else Color(0xFF334155)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(pm.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Sale Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Grand Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total amount due", color = Color.Gray, fontSize = 13.sp)
                        Text("KES ${"%,.0f".format(grandTotal)}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = B360Green)
                    }

                    Button(
                        onClick = {
                            isCheckingOut = true
                            errorMessage = null
                            coroutineScope.launch {
                                val order = Order(
                                    id = generateId(),
                                    orderNumber = "B360-POS-${System.currentTimeMillis() % 10000}",
                                    businessId = businessId,
                                    customerId = selectedCustomer?.id,
                                    customerName = walkInName,
                                    customerPhone = walkInPhone,
                                    deliveryLocation = "In-Store POS",
                                    items = cart.map {
                                        OrderItem(
                                            productId = it.product.id,
                                            productName = it.product.name,
                                            quantity = it.qty,
                                            unitPrice = it.product.sellingPrice,
                                            buyingPrice = it.product.buyingPrice
                                        )
                                    },
                                    paymentStatus = if (paymentMethod == PaymentMethod.CASH) PaymentStatus.PAID else PaymentStatus.PENDING,
                                    deliveryStatus = DeliveryStatus.DELIVERED,
                                    paymentMethod = paymentMethod,
                                    notes = notes,
                                    createdAt = Clock.System.now(),
                                    updatedAt = Clock.System.now()
                                )

                                createOrderUseCase(order)
                                    .onSuccess { savedOrder ->
                                        successOrderNumber = savedOrder.orderNumber
                                        cart.clear()
                                        notes = ""
                                        showCartSheet = false
                                        inventoryViewModel.loadProducts(businessId)
                                    }
                                    .onFailure { err ->
                                        errorMessage = err.message ?: "Failed to save sale."
                                    }
                                isCheckingOut = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        enabled = !isCheckingOut && cart.isNotEmpty()
                    ) {
                        if (isCheckingOut) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Complete POS Checkout", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
