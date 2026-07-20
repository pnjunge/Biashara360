package com.app.biashara.ui.screens.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.domain.model.Product
import com.app.biashara.presentation.viewmodel.InventoryFilter
import com.app.biashara.presentation.viewmodel.InventoryViewModel
import com.app.biashara.ui.theme.*
import com.app.biashara.ui.kmpViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    viewModel: InventoryViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadProducts(com.app.biashara.UserSession.getBusinessId()) }

    val filterLabels = mapOf(
        InventoryFilter.ALL to "All",
        InventoryFilter.LOW_STOCK to "Low Stock",
        InventoryFilter.OUT_OF_STOCK to "Out of Stock"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory / Hifadhi", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Product", fontWeight = FontWeight.Bold) },
                containerColor = B360Green,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
        }
    ) { padding ->
        if (state.isLoading && state.products.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding).background(B360Surface)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search products...", color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton({ viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = B360Green,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterLabels.forEach { (filter, label) ->
                    val isSelected = state.selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = B360Green,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF64748B)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFE2E8F0),
                            selectedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryChip(Modifier.weight(1f), "${state.products.size}", "Products", B360Blue)
                SummaryChip(Modifier.weight(1f), "${state.lowStockCount}", "Low Stock", B360Amber)
                SummaryChip(Modifier.weight(1f), "${state.products.count { it.isOutOfStock }}", "Out of Stock", B360Red)
            }

            if (state.error != null) {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (state.filteredProducts.isEmpty() && !state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Inventory2, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Text("No products found", color = Color.Gray)
                        if (state.searchQuery.isBlank()) {
                            Button(onClick = onAddProduct, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                                Text("Add your first product")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredProducts) { product ->
                        ProductCard(product = product, onEdit = { onEditProduct(product.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun SummaryChip(modifier: Modifier, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProductCard(product: Product, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    if (product.isOutOfStock) {
                        Badge(containerColor = B360Red) { Text("Out", color = Color.White, fontSize = 10.sp) }
                    } else if (product.isLowStock) {
                        Badge(containerColor = B360Amber) { Text("Low", color = Color.White, fontSize = 10.sp) }
                    }
                }
                Text(product.sku, color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LabeledValue("Buy", "KES ${"%,.0f".format(product.buyingPrice)}", Color.Gray)
                    LabeledValue("Sell", "KES ${"%,.0f".format(product.sellingPrice)}", B360Green)
                    LabeledValue("Profit", "KES ${"%,.0f".format(product.profitPerItem)}", B360Blue)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${product.currentStock}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        product.isOutOfStock -> B360Red
                        product.isLowStock -> B360Amber
                        else -> B360Green
                    }
                )
                Text("in stock", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = B360Green, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun LabeledValue(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    productId: String? = null,
    onBack: () -> Unit,
    viewModel: InventoryViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isEdit = productId != null

    // Pre-populate from existing product when editing
    val existingProduct = remember(productId, state.products) {
        productId?.let { id -> state.products.find { it.id == id } }
    }

    var name by remember(existingProduct) { mutableStateOf(existingProduct?.name ?: "") }
    var sku by remember(existingProduct) { mutableStateOf(existingProduct?.sku ?: "") }
    var description by remember(existingProduct) { mutableStateOf(existingProduct?.description ?: "") }
    var category by remember(existingProduct) { mutableStateOf(existingProduct?.category ?: "") }
    var price by remember(existingProduct) { mutableStateOf(existingProduct?.sellingPrice?.toString() ?: "") }
    var buyingPrice by remember(existingProduct) { mutableStateOf(existingProduct?.buyingPrice?.toString() ?: "") }
    var stock by remember(existingProduct) { mutableStateOf(existingProduct?.currentStock?.toString() ?: "") }
    var lowStockThreshold by remember(existingProduct) { mutableStateOf(existingProduct?.lowStockThreshold?.toString() ?: "5") }
    var isActive by remember(existingProduct) { mutableStateOf(existingProduct?.isActive ?: true) }
    var formError by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = B360Green,
        unfocusedBorderColor = Color(0xFFE2E8F0),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Product" else "Add Product", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color(0xFF0F172A)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when {
                        name.isBlank() -> formError = "Product name is required"
                        price.toDoubleOrNull() == null -> formError = "Enter a valid selling price"
                        else -> {
                            formError = null
                            val businessId = com.app.biashara.UserSession.getBusinessId()
                            val now = kotlinx.datetime.Clock.System.now()
                            val product = com.app.biashara.domain.model.Product(
                                id = existingProduct?.id ?: com.app.biashara.domain.usecase.generateId(),
                                businessId = businessId,
                                name = name.trim(),
                                sku = sku.trim(),
                                description = description.trim(),
                                category = category.trim(),
                                sellingPrice = price.toDoubleOrNull() ?: 0.0,
                                buyingPrice = buyingPrice.toDoubleOrNull() ?: 0.0,
                                currentStock = stock.toIntOrNull() ?: 0,
                                lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 5,
                                isActive = isActive,
                                createdAt = existingProduct?.createdAt ?: now,
                                updatedAt = now
                            )
                            viewModel.saveProduct(product)
                            onBack()
                        }
                    }
                },
                containerColor = B360Green,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Filled.Check, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(if (isEdit) "Update" else "Save Product", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).background(B360Surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; formError = null },
                    label = { Text("Product Name *") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors
                )
            }
            item {
                OutlinedTextField(
                    value = sku, onValueChange = { sku = it },
                    label = { Text("SKU / Barcode") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors
                )
            }
            item {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors, maxLines = 3
                )
            }
            item {
                OutlinedTextField(
                    value = category, onValueChange = { category = it },
                    label = { Text("Category") }, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp), colors = fieldColors
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = buyingPrice, onValueChange = { buyingPrice = it },
                        label = { Text("Cost Price (KES)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp), colors = fieldColors
                    )
                    OutlinedTextField(
                        value = price, onValueChange = { price = it; formError = null },
                        label = { Text("Sell Price (KES) *") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp), colors = fieldColors
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = stock, onValueChange = { stock = it },
                        label = { Text("Stock Qty") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp), colors = fieldColors
                    )
                    OutlinedTextField(
                        value = lowStockThreshold, onValueChange = { lowStockThreshold = it },
                        label = { Text("Low Stock Alert") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp), colors = fieldColors
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active / Listed for Sale", fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                    Switch(
                        checked = isActive, onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = B360Green)
                    )
                }
            }
            if (formError != null) {
                item {
                    Text(formError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
            if (state.error != null) {
                item {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
