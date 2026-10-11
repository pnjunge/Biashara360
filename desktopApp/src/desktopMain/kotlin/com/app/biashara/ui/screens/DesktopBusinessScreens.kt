package com.app.biashara.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.window.Dialog
import com.app.biashara.UserSession
import com.app.biashara.domain.model.Product
import com.app.biashara.presentation.viewmodel.InventoryViewModel
import com.app.biashara.ui.AppScreen
import com.app.biashara.ui.DesktopNavigationViewModel
import com.app.biashara.ui.theme.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

// ─────────────────────────────────────────────────────────────────────────────
// 1. PRODUCTS SCREEN (Dedicated catalog view with category filters & pricing)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DesktopProductsScreen(
    searchQuery: String = "",
    onNavigateToInventory: () -> Unit = {},
    viewModel: InventoryViewModel = remember { inject() },
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadProducts(UserSession.getBusinessId())
    }

    var selectedCategory by remember { mutableStateOf("ALL") }
    var localSearch by remember { mutableStateOf("") }
    val effectiveSearch = searchQuery.ifBlank { localSearch }.trim().lowercase()

    val categories = remember(state.products) {
        listOf("ALL") + state.products.map { it.category.ifBlank { "General" } }.distinct().sorted()
    }

    val filteredProducts = remember(state.products, selectedCategory, effectiveSearch) {
        state.products.filter { p ->
            val matchesCategory = selectedCategory == "ALL" || p.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = effectiveSearch.isEmpty() ||
                p.name.lowercase().contains(effectiveSearch) ||
                p.sku.lowercase().contains(effectiveSearch) ||
                p.barcode?.lowercase()?.contains(effectiveSearch) == true
            matchesCategory && matchesSearch
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Products Catalog", color = Color(0xFF0F1F3A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Manage retail pricing, barcodes, categories & eTIMS tax classification", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onNavigateToInventory,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Icon(Icons.Default.Inventory2, null, Modifier.size(16.dp), tint = Color(0xFF475569))
                    Spacer(Modifier.width(6.dp))
                    Text("Stock Balances", color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Product", fontWeight = FontWeight.Bold)
                }
            }
        }

        // KPIs
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard(Modifier.weight(1f), "Total Products", state.products.size.toString(), "In catalog", Icons.Default.Category, B360Green, Color(0xFFE6F7F0))
            KpiCard(Modifier.weight(1f), "Categories", categories.size.minus(1).coerceAtLeast(1).toString(), "Departments", Icons.Default.Folder, B360Blue, Color(0xFFE0F2FE))
            KpiCard(Modifier.weight(1f), "Low Stock", state.products.count { it.isLowStock }.toString(), "Under reorder limit", Icons.Default.Warning, B360Amber, Color(0xFFFEF3C7))
            KpiCard(Modifier.weight(1f), "Catalog Value", "KES ${String.format("%,.0f", state.products.sumOf { it.sellingPrice * it.currentStock.toDouble() })}", "Total retail value", Icons.Default.Sell, Color(0xFF7C3AED), Color(0xFFF3E8FF))
        }

        // Category pills & search bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) B360Green else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) B360Green else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = if (cat == "ALL") "All Categories (${state.products.size})" else cat,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            OutlinedTextField(
                value = localSearch,
                onValueChange = { localSearch = it },
                leadingIcon = { Icon(Icons.Default.Search, "Filter products", Modifier.size(16.dp), tint = Color(0xFF94A3B8)) },
                modifier = Modifier.width(240.dp).height(44.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
        }

        // Table
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.fillMaxSize()) {
                // Table header
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Product Name", modifier = Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Category", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("SKU / Barcode", modifier = Modifier.weight(1.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Selling Price", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Cost Price", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Stock Level", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Status", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Spacer(Modifier.width(40.dp))
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))

                if (filteredProducts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No products found in this category", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                } else {
                    DesktopLazyColumn(Modifier.fillMaxSize()) {
                        items(filteredProducts) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
                                    if (p.description.isNotBlank()) {
                                        Text(p.description, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        p.category.ifBlank { "General" },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Column(modifier = Modifier.weight(1.4f)) {
                                    Text(p.sku.ifBlank { "—" }, fontSize = 12.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                                    p.barcode?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                                Text("KES ${String.format("%,.2f", p.sellingPrice)}", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
                                Text("KES ${String.format("%,.2f", p.buyingPrice)}", modifier = Modifier.weight(1.2f), color = Color(0xFF64748B), fontSize = 12.sp)
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${p.currentStock}", fontWeight = FontWeight.Bold, color = if (p.isLowStock) B360Red else Color(0xFF1E293B), fontSize = 13.sp)
                                    Text(" pcs", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (p.currentStock > 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                                ) {
                                    Text(
                                        if (p.currentStock > 0) "In Stock" else "Out of Stock",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (p.currentStock > 0) Color(0xFF047857) else Color(0xFFB91C1C)
                                    )
                                }
                                IconButton(
                                    onClick = { onNavigateToInventory() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        DesktopAddProductModal(
            onDismiss = { showAddDialog = false },
            onProductAdded = {
                viewModel.loadProducts(UserSession.getBusinessId())
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun DesktopAddProductModal(onDismiss: () -> Unit, onProductAdded: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("10") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(480.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add New Product", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF64748B))
                    }
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU Code") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = sellingPrice, onValueChange = { sellingPrice = it }, label = { Text("Selling Price (KES) *") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Cost Price (KES)") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Opening Stock") }, modifier = Modifier.weight(1f))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF64748B)) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onProductAdded() },
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        enabled = name.isNotBlank() && sellingPrice.isNotBlank()
                    ) {
                        Text("Save Product")
                    }
                }
            }
        }
    }
}

// Supplier records are loaded and saved through the backend.
@kotlinx.serialization.Serializable
internal data class DesktopSupplierRecord(
    val id: String,
    val name: String,
    val phone: String = "",
    val email: String? = null,
    val address: String? = null,
    val isActive: Boolean = true
)

@kotlinx.serialization.Serializable
private data class DesktopSupplierCreateRequest(
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null
)

@Composable
fun DesktopSuppliersScreen(
    searchQuery: String = "",
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    val client: HttpClient = remember { inject() }
    val scope = rememberCoroutineScope()
    var suppliers by remember { mutableStateOf<List<DesktopSupplierRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var localSearch by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    val effectiveSearch = searchQuery.ifBlank { localSearch }.trim()
    val filtered = suppliers.filter {
        effectiveSearch.isBlank() || it.name.contains(effectiveSearch, true) ||
            it.phone.contains(effectiveSearch) || it.email.orEmpty().contains(effectiveSearch, true)
    }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            val response = client.get("${com.app.biashara.data.remote.BASE_URL}/suppliers")
                .body<com.app.biashara.data.remote.ApiResponse<List<DesktopSupplierRecord>>>()
            check(response.success && response.data != null) { response.message.ifBlank { "Could not load suppliers" } }
            suppliers = response.data!!
        } catch (e: Exception) {
            error = e.message ?: "Could not load suppliers"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader("Suppliers & Vendors", "Manage your supplier directory")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(localSearch, { localSearch = it }, label = { Text("Search suppliers") }, modifier = Modifier.weight(1f))
            Button(onClick = { showAdd = true }) { Text("Add Supplier") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(Modifier.weight(1f), "Active Suppliers", suppliers.count { it.isActive }.toString(), "Supplier directory", Icons.Default.LocalShipping, B360Green, Color(0xFFE6F7F0))
            OutlinedButton(onClick = { navigationViewModel.navigateTo(AppScreen.Purchases) }) { Text("Purchases") }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { reload++ }) { Text("Retry") }
        }
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (filtered.isEmpty() && error == null) {
            Text(if (effectiveSearch.isBlank()) "No suppliers recorded" else "No matching suppliers", color = Color(0xFF64748B))
        }
        DesktopLazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { supplier ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(supplier.name, fontWeight = FontWeight.Bold)
                        Text(if (supplier.isActive) "Active" else "Inactive", color = Color(0xFF64748B))
                        if (supplier.phone.isNotBlank()) Text(supplier.phone)
                        supplier.email?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        supplier.address?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var saveError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            onDismissRequest = { if (!saving) showAdd = false },
            title = { Text("Add Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Company name *") })
                    OutlinedTextField(phone, { phone = it }, label = { Text("Phone") })
                    OutlinedTextField(email, { email = it }, label = { Text("Email") })
                    OutlinedTextField(address, { address = it }, label = { Text("Address") })
                    saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(enabled = name.isNotBlank() && !saving, onClick = {
                    scope.launch {
                        saving = true
                        saveError = null
                        try {
                            val response = client.post("${com.app.biashara.data.remote.BASE_URL}/suppliers") {
                                contentType(ContentType.Application.Json)
                                setBody(DesktopSupplierCreateRequest(name.trim(), phone.trim(), email.trim().ifBlank { null }, address.trim().ifBlank { null }))
                            }.body<com.app.biashara.data.remote.ApiResponse<DesktopSupplierRecord>>()
                            check(response.success && response.data != null) { response.message.ifBlank { "Could not save supplier" } }
                            showAdd = false
                            reload++
                        } catch (e: Exception) {
                            saveError = e.message ?: "Could not save supplier"
                        } finally {
                            saving = false
                        }
                    }
                }) { Text(if (saving) "Saving…" else "Save Supplier") }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DesktopWebWorkflow(title: String, description: String, path: String) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(title, description)
        Text("Manage these records in the web app. This desktop view is not connected yet.", color = Color(0xFF64748B))
        Button(onClick = { openExternalUrl("https://biashara360.co.ke/$path") }) { Text("Open web app") }
    }
}

@Composable
fun DesktopHotelRoomsScreen(navigationViewModel: DesktopNavigationViewModel = remember { inject() }) {
    DesktopWebWorkflow("Hotel & Rooms", "Rooms, reservations and guest stays", "hotel")
}

@Composable
fun DesktopAppointmentsScreen(navigationViewModel: DesktopNavigationViewModel = remember { inject() }) {
    DesktopWebWorkflow("Appointments & Services", "Manage your service appointments", "services")
}

@Composable
fun DesktopBookingsScreen(navigationViewModel: DesktopNavigationViewModel = remember { inject() }) {
    DesktopWebWorkflow("Table & Event Bookings", "Manage dining reservations", "hospitality")
}
