package com.app.biashara.ui.screens.inventory

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

data class ProductUi(
    val id: String, val name: String, val sku: String,
    val buyingPrice: Double, val sellingPrice: Double,
    val stock: Int, val lowStockThreshold: Int = 5
) {
    val profit get() = sellingPrice - buyingPrice
    val isLowStock get() = stock in 1..lowStockThreshold
    val isOutOfStock get() = stock == 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    // Sample data
    val sampleProducts = listOf(
        ProductUi("1", "Black Dress Size M", "SKU-001", 800.0, 1500.0, 2),
        ProductUi("2", "Ankara Print Fabric (2m)", "SKU-002", 350.0, 700.0, 12),
        ProductUi("3", "Gold Hoop Earrings", "SKU-003", 150.0, 450.0, 3),
        ProductUi("4", "White Sneakers Size 38", "SKU-004", 1200.0, 2200.0, 0),
        ProductUi("5", "Silk Blouse Pink", "SKU-005", 600.0, 1200.0, 8)
    )

    val filters = listOf("All", "Low Stock", "Out of Stock")

    val filteredProducts = sampleProducts.filter { product ->
        val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                product.sku.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Low Stock" -> product.isLowStock
            "Out of Stock" -> product.isOutOfStock
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory / Hifadhi", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Product") },
                containerColor = B360Green,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search products...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton({ searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = null)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            // Filter chips
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
            // Summary row
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryChip(Modifier.weight(1f), "${sampleProducts.size}", "Products", B360Blue)
                SummaryChip(Modifier.weight(1f), "${sampleProducts.count { it.isLowStock }}", "Low Stock", B360Amber)
                SummaryChip(Modifier.weight(1f), "${sampleProducts.count { it.isOutOfStock }}", "Out of Stock", B360Red)
            }
            // Product list
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductCard(product = product, onEdit = { onEditProduct(product.id) })
                }
                item { Spacer(Modifier.height(80.dp)) } // FAB space
            }
        }
    }
}

@Composable
fun SummaryChip(modifier: Modifier, value: String, label: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 11.sp, color = color.copy(0.8f))
        }
    }
}

@Composable
fun ProductCard(product: ProductUi, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                    LabeledValue("Profit", "KES ${"%,.0f".format(product.profit)}", B360Blue)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${product.stock}",
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
fun AddProductScreen(productId: String? = null, onBack: () -> Unit) {
    val isEdit = productId != null
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Product" else "Add Product", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onBack, containerColor = B360Green) {
                Icon(Icons.Filled.Check, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(if (isEdit) "Update" else "Save Product", color = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU / Barcode") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (KES)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock Qty") }, modifier = Modifier.weight(1f))
            }
        }
    }
}
