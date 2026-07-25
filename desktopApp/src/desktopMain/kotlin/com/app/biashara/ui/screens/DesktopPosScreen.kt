package com.app.biashara.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.app.biashara.domain.usecase.InitiatePaymentUseCase
import com.app.biashara.domain.usecase.generateId
import com.app.biashara.presentation.viewmodel.CustomersViewModel
import com.app.biashara.presentation.viewmodel.InventoryViewModel
import com.app.biashara.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class DesktopCartItem(
    val product: Product,
    val qty: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopPosScreen(
    inventoryViewModel: InventoryViewModel = remember { inject() },
    customersViewModel: CustomersViewModel = remember { inject() },
    createOrderUseCase: CreateOrderUseCase = remember { inject() },
    initiatePaymentUseCase: InitiatePaymentUseCase = remember { inject() }
) {
    val coroutineScope = rememberCoroutineScope()
    val businessId = remember { UserSession.getBusinessId() }

    // Load inventory and customers
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
            val matchesCategory = when {
                selectedCategory == "All" -> true
                selectedCategory == "Favorites" -> true
                selectedCategory == "Recent" -> true
                selectedCategory == "Categories" -> true
                selectedCategory.startsWith("Category:") -> {
                    val catName = selectedCategory.substringAfter("Category:")
                    p.category == catName
                }
                else -> true
            }
            matchesSearch && matchesCategory && p.isActive
        }
    }

    // Cart and Transaction State
    val cart = remember { mutableStateListOf<DesktopCartItem>() }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var walkInName by remember { mutableStateOf("Walk-In Customer") }
    var walkInPhone by remember { mutableStateOf("+254000000000") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var notes by remember { mutableStateOf("") }

    var isCheckingOut by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successOrderNumber by remember { mutableStateOf<String?>(null) }
    var paymentFeedback by remember { mutableStateOf<String?>(null) }
    var showCartMenu by remember { mutableStateOf(false) }

    val subtotal = cart.sumOf { it.product.sellingPrice * it.qty }
    val tax = subtotal * 0.16
    val grandTotal = subtotal + tax

    Row(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))
    ) {
        // Left Side: Product Selection Grid
        Column(
            modifier = Modifier.weight(1.3f).fillMaxHeight().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 1: Search and Filter Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search product name or SKU...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = B360Green,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                // Filter Button
                OutlinedIconButton(
                    onClick = { searchQuery = ""; selectedCategory = "All" },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Filter",
                        tint = B360Green
                    )
                }

                // Green All button
                Button(
                    onClick = { searchQuery = ""; selectedCategory = "All" },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("All", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Row 2: Tabs/Chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple("All", "All", Icons.Default.Home),
                    Triple("Favorites", "Favorites", Icons.Default.Star),
                    Triple("Categories", "Categories", Icons.Default.Menu),
                    Triple("Recent", "Recent", Icons.Default.Refresh)
                )

                tabs.forEach { (tabId, label, icon) ->
                    val isSelected = selectedCategory == tabId || (tabId == "Categories" && selectedCategory.startsWith("Category:"))
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (tabId == "Categories") {
                                selectedCategory = if (categories.size > 1) "Category:${categories[1]}" else "Categories"
                            } else {
                                selectedCategory = tabId
                            }
                        },
                        label = { Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) Color.White else Color.Gray
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = B360Green,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF334155)
                        ),
                        border = BorderStroke(1.dp, if (isSelected) B360Green else Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Sub-row for Categories if Categories is selected
            if (selectedCategory == "Categories" || selectedCategory.startsWith("Category:")) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val actualCategories = categories.filter { it != "All" }
                    if (actualCategories.isEmpty()) {
                        Text("No categories found", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                    } else {
                        actualCategories.forEach { cat ->
                            val catValue = "Category:$cat"
                            val isSelected = selectedCategory == catValue
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = catValue },
                                label = { Text(cat, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = B360Green.copy(alpha = 0.85f),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF475569)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) B360Green else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }
                }
            }

            // Products Catalog
            if (inventoryState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = B360Green)
                }
            } else if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(Color(0xFFE6F7F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = B360Green,
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "No products match filters.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Try adjusting your search or filters to find what you're looking for.",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                selectedCategory = "All"
                            },
                            border = BorderStroke(1.dp, B360Green),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = B360Green)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Clear filters", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts) { p ->
                        val isOutOfStock = p.isOutOfStock
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isOutOfStock) {
                                    errorMessage = null
                                    val existing = cart.find { it.product.id == p.id }
                                    if (existing != null) {
                                        if (existing.qty < p.currentStock) {
                                            val idx = cart.indexOf(existing)
                                            cart[idx] = existing.copy(qty = existing.qty + 1)
                                        } else {
                                            errorMessage = "Cannot add more. Only ${p.currentStock} items in stock."
                                        }
                                    } else {
                                        cart.add(DesktopCartItem(p, 1))
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Product Logo block
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.name.take(2).uppercase(),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = B360Green
                                    )
                                }

                                Column {
                                    Text(
                                        text = p.sku,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = p.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "KES ${String.format("%,.0f", p.sellingPrice)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = B360Green,
                                        fontSize = 13.sp
                                    )

                                    val stockColor = if (isOutOfStock) B360Red else if (p.isLowStock) B360Amber else B360Green
                                    Text(
                                        text = if (isOutOfStock) "Out" else "${p.currentStock} left",
                                        color = stockColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(stockColor.copy(0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right Side: Shopping Cart Panel
        Card(
            modifier = Modifier.weight(0.8f).fillMaxHeight(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            val cartScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(cartScrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cart Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = B360Green,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "POS Cart",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (cart.isNotEmpty()) {
                            TextButton(onClick = { cart.clear(); errorMessage = null }) {
                                Text("Clear All", color = B360Red, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box {
                            IconButton(onClick = { showCartMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.Gray)
                            }
                            DropdownMenu(expanded = showCartMenu, onDismissRequest = { showCartMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Clear cart") },
                                    onClick = {
                                        cart.clear()
                                        errorMessage = null
                                        showCartMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.DeleteSweep, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Reset checkout") },
                                    onClick = {
                                        selectedCustomer = null
                                        walkInName = "Walk-In Customer"
                                        walkInPhone = "+254000000000"
                                        paymentMethod = PaymentMethod.CASH
                                        notes = ""
                                        paymentFeedback = null
                                        errorMessage = null
                                        showCartMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.RestartAlt, null) }
                                )
                            }
                        }
                    }
                }

                // Error Message block
                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFFB91C1C),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Success Message block
                successOrderNumber?.let { num ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CheckCircle, null, tint = B360Green, modifier = Modifier.size(36.dp))
                            Text("Checkout Successful!", fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            Text("Order Number: $num", fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp)
                            Button(
                                onClick = { successOrderNumber = null },
                                colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                            ) {
                                Text("New Session", color = Color.White)
                            }
                        }
                    }
                }

                if (successOrderNumber == null) {
                    // Cart Line Items List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Text("Shopping cart is empty", color = Color(0xFF64748B), fontSize = 14.sp)
                                }
                            }
                        } else {
                            cart.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            "KES ${String.format("%,.0f", item.product.sellingPrice)}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    // Counter triggers
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                errorMessage = null
                                                val idx = cart.indexOf(item)
                                                if (item.qty > 1) {
                                                    cart[idx] = item.copy(qty = item.qty - 1)
                                                } else {
                                                    cart.remove(item)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Remove, null, modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            item.qty.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.widthIn(min = 20.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )

                                        IconButton(
                                            onClick = {
                                                errorMessage = null
                                                val idx = cart.indexOf(item)
                                                if (item.qty < item.product.currentStock) {
                                                    cart[idx] = item.copy(qty = item.qty + 1)
                                                } else {
                                                    errorMessage = "Insufficient stock for ${item.product.name}."
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Text(
                                        "KES ${String.format("%,.0f", item.product.sellingPrice * item.qty)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.widthIn(min = 60.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                    )

                                    IconButton(onClick = { cart.remove(item) }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            null,
                                            tint = B360Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Customer Selection & Checkout Detail Card
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Customer dropdown
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: "Walk-In / Anonymous Customer",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Select Customer") },
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
                                    text = { Text("Walk-In / Anonymous Customer") },
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = walkInName,
                                    onValueChange = { walkInName = it },
                                    label = { Text("Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = walkInPhone,
                                    onValueChange = { walkInPhone = it },
                                    label = { Text("Phone") },
                                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Payment Methods Custom Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Payment Method", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple(PaymentMethod.CASH, "CASH", Icons.Default.ShoppingCart),
                                    Triple(PaymentMethod.COD, "COD", Icons.Default.LocalShipping),
                                    Triple(PaymentMethod.MPESA, "MPESA", Icons.Default.Phone),
                                    Triple(PaymentMethod.CARD, "CARD", Icons.Default.CheckCircle)
                                ).forEach { (pm, label, icon) ->
                                    val isSelected = paymentMethod == pm
                                    Button(
                                        onClick = {
                                            paymentMethod = pm
                                            errorMessage = null
                                            paymentFeedback = when (pm) {
                                                PaymentMethod.CASH -> "Cash payment will mark the order as paid immediately."
                                                PaymentMethod.COD -> "Cash on Delivery will keep payment due until the order is delivered."
                                                PaymentMethod.MPESA -> "M-Pesa will send an STK prompt to $walkInPhone after the order is created."
                                                PaymentMethod.CARD -> "Card payment will open the secure CyberSource checkout after the order is created."
                                                else -> null
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) B360Green else Color(0xFFF1F5F9),
                                            contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isSelected) Color.White else Color(0xFF64748B)
                                            )
                                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        paymentFeedback?.let { feedback ->
                            Surface(
                                color = B360Green.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, B360Green.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    feedback,
                                    color = Color(0xFF166534),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Notes TextField
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Add sale notes (optional)") },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(Modifier.height(4.dp))

                        // Checkout calculations
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)).padding(12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", fontSize = 12.sp, color = Color.Gray)
                                Text("KES ${String.format("%,.0f", subtotal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("VAT (16%)", fontSize = 12.sp, color = Color.Gray)
                                Text("KES ${String.format("%,.0f", tax)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Grand Total", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "KES ${String.format("%,.0f", grandTotal)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = B360Green
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (cart.isEmpty()) {
                                    errorMessage = "Please add items to cart."
                                    return@Button
                                }
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
                                        paymentStatus = when (paymentMethod) {
                                            PaymentMethod.CASH -> PaymentStatus.PAID
                                            PaymentMethod.COD -> PaymentStatus.COD
                                            else -> PaymentStatus.PENDING
                                        },
                                        deliveryStatus = DeliveryStatus.DELIVERED,
                                        paymentMethod = paymentMethod,
                                        notes = notes,
                                        createdAt = Clock.System.now(),
                                        updatedAt = Clock.System.now()
                                    )

                                    createOrderUseCase(order)
                                        .onSuccess { savedOrder ->
                                            when (paymentMethod) {
                                                PaymentMethod.MPESA -> {
                                                    initiatePaymentUseCase(savedOrder.id, walkInPhone)
                                                        .onSuccess {
                                                            paymentFeedback = "M-Pesa STK prompt sent to $walkInPhone."
                                                            successOrderNumber = savedOrder.orderNumber
                                                            cart.clear()
                                                            notes = ""
                                                        }
                                                        .onFailure { paymentError ->
                                                            errorMessage = "Order ${savedOrder.orderNumber} was saved, but M-Pesa could not start: ${paymentError.message ?: "STK push failed"}"
                                                        }
                                                }
                                                PaymentMethod.CARD -> {
                                                    try {
                                                        val checkoutUrl = "https://enw9p7mvty.us-east-1.awsapprunner.com/card-payments?orderId=${savedOrder.id}"
                                                        if (java.awt.Desktop.isDesktopSupported() &&
                                                            java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)
                                                        ) {
                                                            java.awt.Desktop.getDesktop().browse(java.net.URI(checkoutUrl))
                                                            paymentFeedback = "Secure CyberSource checkout opened in your browser."
                                                            successOrderNumber = savedOrder.orderNumber
                                                            cart.clear()
                                                            notes = ""
                                                        } else {
                                                            errorMessage = "Order ${savedOrder.orderNumber} was saved. Open $checkoutUrl to complete card payment."
                                                        }
                                                    } catch (paymentError: Exception) {
                                                        errorMessage = "Order ${savedOrder.orderNumber} was saved, but card checkout could not open: ${paymentError.message}"
                                                    }
                                                }
                                                else -> {
                                                    successOrderNumber = savedOrder.orderNumber
                                                    cart.clear()
                                                    notes = ""
                                                }
                                            }
                                            inventoryViewModel.loadProducts(businessId)
                                        }
                                        .onFailure { err ->
                                            errorMessage = err.message ?: "Failed to record checkout sale."
                                        }
                                    isCheckingOut = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isCheckingOut && cart.isNotEmpty()
                        ) {
                            if (isCheckingOut) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Complete Sale", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
