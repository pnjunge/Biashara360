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
                placeholder = { Text("Filter products...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = Color(0xFF94A3B8)) },
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
                    LazyColumn(Modifier.fillMaxSize()) {
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

// ─────────────────────────────────────────────────────────────────────────────
// 2. SUPPLIERS SCREEN
// ─────────────────────────────────────────────────────────────────────────────

data class DesktopSupplierItem(
    val id: String,
    val name: String,
    val contactPerson: String,
    val phone: String,
    val email: String,
    val address: String,
    val totalOrders: Int,
    val balanceDue: Double,
    val status: String = "ACTIVE"
)

@Composable
fun DesktopSuppliersScreen(
    searchQuery: String = "",
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    var suppliers by remember {
        mutableStateOf(
            listOf(
                DesktopSupplierItem("1", "Kenya Breweries Ltd", "Maina Mwangi", "0722 000 111", "orders@kbl.co.ke", "Ruaraka, Nairobi", 42, 0.0),
                DesktopSupplierItem("2", "Farmer's Choice Produce", "Jane Wanjiku", "0733 111 222", "sales@farmerschoice.co.ke", "Kahawa West, Nairobi", 28, 14500.0),
                DesktopSupplierItem("3", "Brookside Dairy Ltd", "David Kiprono", "0711 222 333", "dispatch@brookside.co.ke", "Ruiru, Kiambu", 64, 8200.0),
                DesktopSupplierItem("4", "Unga Farm Care Ltd", "Sarah Njeri", "0700 333 444", "orders@unga.com", "Industrial Area, Nairobi", 19, 0.0),
                DesktopSupplierItem("5", "Bidco Africa Distributors", "Hassan Omar", "0720 444 555", "supplies@bidco.com", "Thika, Kiambu", 33, 22300.0)
            )
        )
    }

    var localSearch by remember { mutableStateOf("") }
    val effectiveSearch = searchQuery.ifBlank { localSearch }.lowercase()
    val filtered = remember(suppliers, effectiveSearch) {
        suppliers.filter { s ->
            effectiveSearch.isEmpty() ||
                s.name.lowercase().contains(effectiveSearch) ||
                s.contactPerson.lowercase().contains(effectiveSearch) ||
                s.phone.contains(effectiveSearch)
        }
    }

    var showAddSupplierModal by remember { mutableStateOf(false) }

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
                Text("Suppliers & Vendors", color = Color(0xFF0F1F3A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Manage vendor directory, purchase orders & payment reconciliation", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { navigationViewModel.navigateTo(AppScreen.Purchases) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Icon(Icons.Default.ReceiptLong, null, Modifier.size(16.dp), tint = Color(0xFF475569))
                    Spacer(Modifier.width(6.dp))
                    Text("Purchase Orders", color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { showAddSupplierModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Supplier", fontWeight = FontWeight.Bold)
                }
            }
        }

        // KPIs
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard(Modifier.weight(1f), "Active Suppliers", suppliers.size.toString(), "Verified vendors", Icons.Default.LocalShipping, B360Green, Color(0xFFE6F7F0))
            KpiCard(Modifier.weight(1f), "Purchase Orders", suppliers.sumOf { it.totalOrders }.toString(), "Total POs raised", Icons.Default.Receipt, B360Blue, Color(0xFFE0F2FE))
            KpiCard(Modifier.weight(1f), "Payables Balance", "KES ${String.format("%,.0f", suppliers.sumOf { it.balanceDue })}", "Awaiting settlement", Icons.Default.Payments, B360Red, Color(0xFFFEE2E2))
            KpiCard(Modifier.weight(1f), "Deliveries This Month", "18 orders", "On schedule", Icons.Default.CheckCircle, Color(0xFF7C3AED), Color(0xFFF3E8FF))
        }

        // Table
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Supplier Company", modifier = Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Contact Person", modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Phone / Email", modifier = Modifier.weight(1.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Location", modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("POs Count", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Balance Due", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Spacer(Modifier.width(60.dp))
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))

                LazyColumn(Modifier.fillMaxSize()) {
                    items(filtered) { s ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(2f)) {
                                Text(s.name, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFECFDF5)) {
                                    Text("ACTIVE VENDOR", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                }
                            }
                            Text(s.contactPerson, modifier = Modifier.weight(1.5f), fontSize = 12.sp, color = Color(0xFF334155))
                            Column(modifier = Modifier.weight(1.8f)) {
                                Text(s.phone, fontSize = 12.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                Text(s.email, fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            Text(s.address, modifier = Modifier.weight(1.5f), fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("${s.totalOrders} POs", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Text(
                                if (s.balanceDue > 0) "KES ${String.format("%,.0f", s.balanceDue)}" else "Cleared",
                                modifier = Modifier.weight(1.2f),
                                fontWeight = FontWeight.Bold,
                                color = if (s.balanceDue > 0) B360Red else Color(0xFF059669),
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { navigationViewModel.navigateTo(AppScreen.Purchases) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.AddShoppingCart, "New PO", tint = B360Green, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }

    if (showAddSupplierModal) {
        Dialog(onDismissRequest = { showAddSupplierModal = false }) {
            var name by remember { mutableStateOf("") }
            var person by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var address by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.width(460.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Add New Supplier", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Company Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = person, onValueChange = { person = it }, label = { Text("Contact Person") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Physical Address / City") }, modifier = Modifier.fillMaxWidth())

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddSupplierModal = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                suppliers = suppliers + DesktopSupplierItem(
                                    id = (suppliers.size + 1).toString(),
                                    name = name,
                                    contactPerson = person.ifBlank { name },
                                    phone = phone,
                                    email = email,
                                    address = address,
                                    totalOrders = 0,
                                    balanceDue = 0.0
                                )
                                showAddSupplierModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            enabled = name.isNotBlank() && phone.isNotBlank()
                        ) {
                            Text("Save Supplier")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. HOTEL & ROOMS SCREEN
// ─────────────────────────────────────────────────────────────────────────────

enum class DesktopRoomStatus { VACANT, OCCUPIED, CLEANING, MAINTENANCE }

data class DesktopRoom(
    val number: String,
    val type: String,
    val floor: Int,
    val rate: Double,
    val guestName: String? = null,
    val checkOutDate: String? = null,
    val status: DesktopRoomStatus = DesktopRoomStatus.VACANT
)

@Composable
fun DesktopHotelRoomsScreen(
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    var rooms by remember {
        mutableStateOf(
            listOf(
                DesktopRoom("101", "Standard Single", 1, 3500.0, "John Mutua", "Tomorrow 10:00 AM", DesktopRoomStatus.OCCUPIED),
                DesktopRoom("102", "Standard Single", 1, 3500.0, null, null, DesktopRoomStatus.VACANT),
                DesktopRoom("103", "Standard Double", 1, 4500.0, null, null, DesktopRoomStatus.CLEANING),
                DesktopRoom("201", "Deluxe Double", 2, 5500.0, "Dr. Alice Njoki", "Oct 5, 2026", DesktopRoomStatus.OCCUPIED),
                DesktopRoom("202", "Deluxe Double", 2, 5500.0, null, null, DesktopRoomStatus.VACANT),
                DesktopRoom("203", "Executive Suite", 2, 8500.0, "Mark Kipchoge", "Oct 4, 2026", DesktopRoomStatus.OCCUPIED),
                DesktopRoom("301", "Executive Suite", 3, 8500.0, null, null, DesktopRoomStatus.VACANT),
                DesktopRoom("302", "Presidential Penthouse", 3, 15000.0, null, null, DesktopRoomStatus.VACANT),
                DesktopRoom("303", "Family Suite", 3, 10000.0, null, null, DesktopRoomStatus.MAINTENANCE)
            )
        )
    }

    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var showCheckInModal by remember { mutableStateOf(false) }
    var selectedRoomForCheckIn by remember { mutableStateOf<DesktopRoom?>(null) }

    val filteredRooms = remember(rooms, selectedStatusFilter) {
        if (selectedStatusFilter == "ALL") rooms
        else rooms.filter { it.status.name == selectedStatusFilter }
    }

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
                Text("Hotel & Rooms Management", color = Color(0xFF0F1F3A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Room occupancy, housekeeping, reservations & guest check-in desk", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        selectedRoomForCheckIn = rooms.firstOrNull { it.status == DesktopRoomStatus.VACANT }
                        showCheckInModal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Hotel, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Guest Check-In", fontWeight = FontWeight.Bold)
                }
            }
        }

        // KPIs
        val totalCount = rooms.size
        val occupiedCount = rooms.count { it.status == DesktopRoomStatus.OCCUPIED }
        val occupancyRate = (occupiedCount.toDouble() / totalCount * 100).toInt()

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard(Modifier.weight(1f), "Total Rooms", totalCount.toString(), "In property", Icons.Default.MeetingRoom, Color(0xFF475569), Color(0xFFF1F5F9))
            KpiCard(Modifier.weight(1f), "Occupancy Rate", "$occupancyRate%", "$occupiedCount occupied", Icons.Default.Bed, B360Blue, Color(0xFFE0F2FE))
            KpiCard(Modifier.weight(1f), "Vacant & Clean", rooms.count { it.status == DesktopRoomStatus.VACANT }.toString(), "Ready for guest", Icons.Default.CheckCircle, B360Green, Color(0xFFE6F7F0))
            KpiCard(Modifier.weight(1f), "Housekeeping", rooms.count { it.status == DesktopRoomStatus.CLEANING }.toString(), "Need cleaning", Icons.Default.CleaningServices, B360Amber, Color(0xFFFEF3C7))
        }

        // Filter pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL", "VACANT", "OCCUPIED", "CLEANING", "MAINTENANCE").forEach { s ->
                val isSelected = selectedStatusFilter == s
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) B360Green else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) B360Green else Color(0xFFE2E8F0)),
                    modifier = Modifier.clickable { selectedStatusFilter = s }
                ) {
                    Text(
                        text = if (s == "ALL") "All Rooms ($totalCount)" else s.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF475569)
                    )
                }
            }
        }

        // Room Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredRooms) { room ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, when (room.status) {
                        DesktopRoomStatus.VACANT -> Color(0xFFA7F3D0)
                        DesktopRoomStatus.OCCUPIED -> Color(0xFFBAE6FD)
                        DesktopRoomStatus.CLEANING -> Color(0xFFFDE68A)
                        DesktopRoomStatus.MAINTENANCE -> Color(0xFFFECACA)
                    })
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Room ${room.number}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F1F3A))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (room.status) {
                                    DesktopRoomStatus.VACANT -> Color(0xFFECFDF5)
                                    DesktopRoomStatus.OCCUPIED -> Color(0xFFE0F2FE)
                                    DesktopRoomStatus.CLEANING -> Color(0xFFFEF3C7)
                                    DesktopRoomStatus.MAINTENANCE -> Color(0xFFFEE2E2)
                                }
                            ) {
                                Text(
                                    room.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (room.status) {
                                        DesktopRoomStatus.VACANT -> Color(0xFF047857)
                                        DesktopRoomStatus.OCCUPIED -> Color(0xFF0369A1)
                                        DesktopRoomStatus.CLEANING -> Color(0xFFB45309)
                                        DesktopRoomStatus.MAINTENANCE -> Color(0xFFB91C1C)
                                    }
                                )
                            }
                        }

                        Text(room.type, fontSize = 13.sp, color = Color(0xFF475569))
                        Text("KES ${String.format("%,.0f", room.rate)} / night", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        if (room.status == DesktopRoomStatus.OCCUPIED) {
                            Column {
                                Text("Guest: ${room.guestName.orEmpty()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text("Check-out: ${room.checkOutDate.orEmpty()}", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            Button(
                                onClick = {
                                    rooms = rooms.map {
                                        if (it.number == room.number) it.copy(status = DesktopRoomStatus.CLEANING, guestName = null, checkOutDate = null)
                                        else it
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Check Out Guest", color = Color(0xFF1E293B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (room.status == DesktopRoomStatus.VACANT) {
                            Button(
                                onClick = {
                                    selectedRoomForCheckIn = room
                                    showCheckInModal = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Check In Guest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (room.status == DesktopRoomStatus.CLEANING) {
                            Button(
                                onClick = {
                                    rooms = rooms.map {
                                        if (it.number == room.number) it.copy(status = DesktopRoomStatus.VACANT)
                                        else it
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mark Clean & Ready", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    rooms = rooms.map {
                                        if (it.number == room.number) it.copy(status = DesktopRoomStatus.VACANT)
                                        else it
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Complete Maintenance", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCheckInModal) {
        Dialog(onDismissRequest = { showCheckInModal = false }) {
            var guestName by remember { mutableStateOf("") }
            var guestPhone by remember { mutableStateOf("") }
            var nights by remember { mutableStateOf("1") }

            Card(
                modifier = Modifier.width(440.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Check-In Room ${selectedRoomForCheckIn?.number.orEmpty()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                    Text("Daily Rate: KES ${selectedRoomForCheckIn?.rate?.let { String.format("%,.0f", it) }}", fontSize = 13.sp, color = B360Green, fontWeight = FontWeight.Bold)

                    OutlinedTextField(value = guestName, onValueChange = { guestName = it }, label = { Text("Guest Full Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = guestPhone, onValueChange = { guestPhone = it }, label = { Text("Phone Number / ID *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nights, onValueChange = { nights = it }, label = { Text("Number of Nights") }, modifier = Modifier.fillMaxWidth())

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCheckInModal = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val n = nights.toIntOrNull() ?: 1
                                rooms = rooms.map {
                                    if (it.number == selectedRoomForCheckIn?.number) {
                                        it.copy(
                                            status = DesktopRoomStatus.OCCUPIED,
                                            guestName = guestName,
                                            checkOutDate = "In $n days (10:00 AM)"
                                        )
                                    } else it
                                }
                                showCheckInModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            enabled = guestName.isNotBlank()
                        ) {
                            Text("Confirm Check-In")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. APPOINTMENTS & SERVICES SCREEN
// ─────────────────────────────────────────────────────────────────────────────

data class DesktopAppointment(
    val id: String,
    val time: String,
    val customerName: String,
    val customerPhone: String,
    val serviceName: String,
    val staffName: String,
    val durationMin: Int,
    val price: Double,
    val status: String = "CONFIRMED"
)

@Composable
fun DesktopAppointmentsScreen(
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    var appointments by remember {
        mutableStateOf(
            listOf(
                DesktopAppointment("1", "09:00 AM", "Lucy Muthoni", "0722 123 456", "Hair Styling & Treatment", "Sarah (Stylist)", 60, 2500.0, "COMPLETED"),
                DesktopAppointment("2", "10:30 AM", "Karanja Kamau", "0733 987 654", "Full Vehicle Detailing", "Dennis (Detailing)", 90, 4500.0, "IN_PROGRESS"),
                DesktopAppointment("3", "01:00 PM", "Mercy Chebet", "0711 456 789", "Deep Tissue Massage", "Faith (Therapist)", 60, 3500.0, "CONFIRMED"),
                DesktopAppointment("4", "02:30 PM", "Brian Otieno", "0700 888 999", "Executive Consultation", "Dr. Kimani", 45, 5000.0, "CONFIRMED"),
                DesktopAppointment("5", "04:00 PM", "Grace Wambui", "0721 555 444", "Nail Gel & Pedicure", "Alice (Tech)", 45, 1800.0, "CONFIRMED")
            )
        )
    }

    var showBookingModal by remember { mutableStateOf(false) }

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
                Text("Appointments & Services", color = Color(0xFF0F1F3A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Salons, clinics, spas, auto detailing & service bookings schedule", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showBookingModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Book Appointment", fontWeight = FontWeight.Bold)
                }
            }
        }

        // KPIs
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard(Modifier.weight(1f), "Today's Schedule", appointments.size.toString(), "Booked sessions", Icons.Default.CalendarToday, B360Green, Color(0xFFE6F7F0))
            KpiCard(Modifier.weight(1f), "In Progress", appointments.count { it.status == "IN_PROGRESS" }.toString(), "Being served now", Icons.Default.HourglassTop, B360Blue, Color(0xFFE0F2FE))
            KpiCard(Modifier.weight(1f), "Expected Revenue", "KES ${String.format("%,.0f", appointments.sumOf { it.price })}", "Scheduled appointments", Icons.Default.Payments, Color(0xFF7C3AED), Color(0xFFF3E8FF))
            KpiCard(Modifier.weight(1f), "Staff on Duty", "4 Specialists", "Available slots", Icons.Default.Groups, B360Amber, Color(0xFFFEF3C7))
        }

        // Schedule Table
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Client Name", modifier = Modifier.weight(1.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Service Selected", modifier = Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Assigned Staff", modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Duration", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Fee", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Status", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Spacer(Modifier.width(80.dp))
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))

                LazyColumn(Modifier.fillMaxSize()) {
                    items(appointments) { a ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(a.time, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
                            Column(modifier = Modifier.weight(1.8f)) {
                                Text(a.customerName, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
                                Text(a.customerPhone, color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                            Text(a.serviceName, modifier = Modifier.weight(2f), fontSize = 12.sp, color = Color(0xFF334155))
                            Text(a.staffName, modifier = Modifier.weight(1.5f), fontSize = 12.sp, color = Color(0xFF475569))
                            Text("${a.durationMin} mins", modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF64748B))
                            Text("KES ${String.format("%,.0f", a.price)}", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
                            Surface(
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                color = when (a.status) {
                                    "COMPLETED" -> Color(0xFFECFDF5)
                                    "IN_PROGRESS" -> Color(0xFFE0F2FE)
                                    else -> Color(0xFFFEF3C7)
                                }
                            ) {
                                Text(
                                    a.status.replace("_", " "),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (a.status) {
                                        "COMPLETED" -> Color(0xFF047857)
                                        "IN_PROGRESS" -> Color(0xFF0369A1)
                                        else -> Color(0xFFB45309)
                                    }
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (a.status != "COMPLETED") {
                                    IconButton(
                                        onClick = {
                                            appointments = appointments.map {
                                                if (it.id == a.id) it.copy(status = "COMPLETED") else it
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Check, "Complete", tint = B360Green, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }

    if (showBookingModal) {
        Dialog(onDismissRequest = { showBookingModal = false }) {
            var customer by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var service by remember { mutableStateOf("Hair Styling & Treatment") }
            var time by remember { mutableStateOf("11:00 AM") }
            var staff by remember { mutableStateOf("Sarah (Stylist)") }

            Card(
                modifier = Modifier.width(460.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Book New Appointment", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                    OutlinedTextField(value = customer, onValueChange = { customer = it }, label = { Text("Customer Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = service, onValueChange = { service = it }, label = { Text("Service Requested") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time Slot") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = staff, onValueChange = { staff = it }, label = { Text("Assigned Specialist") }, modifier = Modifier.fillMaxWidth())

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showBookingModal = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                appointments = appointments + DesktopAppointment(
                                    id = (appointments.size + 1).toString(),
                                    time = time,
                                    customerName = customer,
                                    customerPhone = phone,
                                    serviceName = service,
                                    staffName = staff,
                                    durationMin = 60,
                                    price = 2500.0,
                                    status = "CONFIRMED"
                                )
                                showBookingModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            enabled = customer.isNotBlank() && phone.isNotBlank()
                        ) {
                            Text("Save Booking")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. BOOKINGS & RESERVATIONS SCREEN
// ─────────────────────────────────────────────────────────────────────────────

data class DesktopBookingItem(
    val id: String,
    val time: String,
    val guestName: String,
    val phone: String,
    val partySize: Int,
    val area: String,
    val notes: String = "",
    val status: String = "BOOKED"
)

@Composable
fun DesktopBookingsScreen(
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    var bookings by remember {
        mutableStateOf(
            listOf(
                DesktopBookingItem("1", "12:30 PM", "James Mwangi", "0722 999 111", 4, "Table 04 (Main Dining)", "Anniversary celebration", "SEATED"),
                DesktopBookingItem("2", "01:00 PM", "Claire Wairimu", "0733 888 222", 2, "Table 02 (Window)", "Quiet table requested", "BOOKED"),
                DesktopBookingItem("3", "02:00 PM", "Eng. Daniel Rotich", "0711 777 333", 6, "Patio Lounge 01", "Business lunch", "BOOKED"),
                DesktopBookingItem("4", "07:30 PM", "Ambassador Mutisya", "0700 666 444", 8, "VIP Private Room", "Pre-set wine & tasting menu", "BOOKED")
            )
        )
    }

    var showNewBookingDialog by remember { mutableStateOf(false) }

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
                Text("Table & Event Bookings", color = Color(0xFF0F1F3A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Restaurant tables, VIP lounges, private dining & event reservations", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showNewBookingDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ConfirmationNumber, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Reservation", fontWeight = FontWeight.Bold)
                }
            }
        }

        // KPIs
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            KpiCard(Modifier.weight(1f), "Total Bookings", bookings.size.toString(), "Today's ledger", Icons.Default.Event, B360Green, Color(0xFFE6F7F0))
            KpiCard(Modifier.weight(1f), "Expected Guests", bookings.sumOf { it.partySize }.toString(), "Confirmed covers", Icons.Default.People, B360Blue, Color(0xFFE0F2FE))
            KpiCard(Modifier.weight(1f), "Seated Now", bookings.count { it.status == "SEATED" }.toString(), "Dining active", Icons.Default.TableRestaurant, B360Amber, Color(0xFFFEF3C7))
            KpiCard(Modifier.weight(1f), "VIP Lounges", "2 Booked", "Private suites", Icons.Default.Star, Color(0xFF7C3AED), Color(0xFFF3E8FF))
        }

        // Bookings Table
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Guest Name", modifier = Modifier.weight(1.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Party Size", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Table / Area", modifier = Modifier.weight(1.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Special Notes", modifier = Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text("Status", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Spacer(Modifier.width(80.dp))
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))

                LazyColumn(Modifier.fillMaxSize()) {
                    items(bookings) { b ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(b.time, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
                            Column(modifier = Modifier.weight(1.8f)) {
                                Text(b.guestName, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
                                Text(b.phone, color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                            Text("${b.partySize} guests", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Color(0xFF334155), fontSize = 12.sp)
                            Text(b.area, modifier = Modifier.weight(1.8f), fontSize = 12.sp, color = Color(0xFF475569))
                            Text(b.notes.ifBlank { "—" }, modifier = Modifier.weight(2f), fontSize = 11.sp, color = Color(0xFF64748B))
                            Surface(
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (b.status == "SEATED") Color(0xFFE0F2FE) else Color(0xFFECFDF5)
                            ) {
                                Text(
                                    b.status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (b.status == "SEATED") Color(0xFF0369A1) else Color(0xFF047857)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (b.status == "BOOKED") {
                                    Button(
                                        onClick = {
                                            bookings = bookings.map {
                                                if (it.id == b.id) it.copy(status = "SEATED") else it
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Seat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }

    if (showNewBookingDialog) {
        Dialog(onDismissRequest = { showNewBookingDialog = false }) {
            var guest by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var guestsCount by remember { mutableStateOf("2") }
            var time by remember { mutableStateOf("07:00 PM") }
            var area by remember { mutableStateOf("Table 05 (Main Dining)") }
            var notes by remember { mutableStateOf("") }

            Card(
                modifier = Modifier.width(460.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("New Table Reservation", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                    OutlinedTextField(value = guest, onValueChange = { guest = it }, label = { Text("Guest Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number *") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = guestsCount, onValueChange = { guestsCount = it }, label = { Text("Party Size") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Reservation Time") }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Table / Space Selection") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Special Requests / Dietary Notes") }, modifier = Modifier.fillMaxWidth())

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showNewBookingDialog = false }) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                bookings = bookings + DesktopBookingItem(
                                    id = (bookings.size + 1).toString(),
                                    time = time,
                                    guestName = guest,
                                    phone = phone,
                                    partySize = guestsCount.toIntOrNull() ?: 2,
                                    area = area,
                                    notes = notes,
                                    status = "BOOKED"
                                )
                                showNewBookingDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            enabled = guest.isNotBlank() && phone.isNotBlank()
                        ) {
                            Text("Confirm Reservation")
                        }
                    }
                }
            }
        }
    }
}
