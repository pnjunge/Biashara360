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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.app.biashara.ui.hasNativePermission
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
import com.app.biashara.presentation.viewmodel.BusinessViewModel
import com.app.biashara.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL

data class DesktopCartItem(
    val product: Product,
    val qty: Int
)

/**
 * POS menu groups are backed by the product category field. Products without
 * a category still need a visible group so they are never hidden from the
 * catalogue.
 */
private fun Product.posGroup(): String = category.trim().ifBlank { "Other" }

@Serializable
private data class PosHospitalityTableInfo(
    val id: String,
    val name: String,
    val area: String,
    val capacity: Int,
    val status: String
)

@Serializable
private data class PosHospitalityOrderItemInfo(
    val id: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val lineTotal: Double = 0.0
)

@Serializable
private data class PosHospitalityOrderInfo(
    val id: String,
    val orderNumber: String,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val deliveryLocation: String? = null,
    val serviceType: String? = null,
    val hospitalityTableId: String? = null,
    val tabStatus: String = "OPEN",
    val subtotal: Double = 0.0,
    val items: List<PosHospitalityOrderItemInfo> = emptyList(),
    val createdAt: String = ""
)

@Serializable
private data class PosKitchenTicket(val orderId: String, val station: String, val status: String)

@Serializable
private data class PosHospitalityDashboardResponse(
    val enabled: Boolean,
    val tables: List<PosHospitalityTableInfo> = emptyList(),
    val openTabs: List<PosHospitalityOrderInfo> = emptyList(),
    val tickets: List<PosKitchenTicket> = emptyList()
)

@Serializable
private data class PosSplitPaymentReq(
    val method: String,
    val amount: Double,
    val phone: String? = null
)

@Serializable
internal data class PosCreateHospitalityOrderReq(
    val tableId: String? = null,
    val guestCount: Int = 1,
    val serviceType: String = "DINE_IN",
    val customerName: String = "Walk-In Customer",
    val customerPhone: String = "",
    val items: List<PosHospitalityOrderItemReq> = emptyList(),
    val notes: String = ""
)

@Serializable
internal data class PosHospitalityOrderItemReq(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val notes: String = ""
)

@Serializable
internal data class PosHospitalityOrderRes(
    val id: String,
    val orderNumber: String
)

fun printReceiptDesktop(
    orderNumber: String,
    tableName: String? = null,
    items: List<Pair<String, Pair<Int, Double>>>,
    subtotal: Double,
    tax: Double,
    grandTotal: Double,
    isSettled: Boolean,
    paymentMethod: String = "CASH",
    customerName: String = "Walk-In Customer",
    customerPhone: String = "",
    businessProfile: BusinessProfile? = null
) {
    try {
        val tempFile = java.io.File.createTempFile("receipt_$orderNumber", ".html")
        tempFile.deleteOnExit()

        val statusText = if (!isSettled) "<span style='color:#d97706; font-weight:bold;'>PENDING SETTLEMENT</span>" else "<span style='color:#16a34a; font-weight:bold;'>SETTLED ($paymentMethod)</span>"
        val tableInfo = if (!tableName.isNullOrBlank()) "<div class='line'><span>TABLE:</span><strong>$tableName</strong></div>" else ""
        fun escapeHtml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        val logo = businessProfile?.receiptLogo?.takeIf { it.startsWith("data:image/") || it.startsWith("https://") }
            ?.let { "<img class='logo' src=\"${escapeHtml(it)}\" alt=\"Business logo\">" }.orEmpty()
        val businessName = escapeHtml(businessProfile?.name?.ifBlank { "BIASHARA360 POS" } ?: "BIASHARA360 POS")
        val businessAddress = businessProfile?.address?.takeIf { it.isNotBlank() }?.let(::escapeHtml).orEmpty()
        val businessCounty = businessProfile?.county?.takeIf { it.isNotBlank() }?.let { "${escapeHtml(it)}, Kenya" }.orEmpty()
        val businessPhone = businessProfile?.phone?.takeIf { it.isNotBlank() }?.let(::escapeHtml).orEmpty()
        val businessPin = businessProfile?.kraPin?.takeIf { it.isNotBlank() }?.let { "<div>PIN: ${escapeHtml(it)}</div>" }.orEmpty()
        val showCustomer = businessProfile?.receiptShowCustomer != false
        val showTax = businessProfile?.receiptShowTax != false
        val receiptHeader = escapeHtml(businessProfile?.receiptHeader?.ifBlank { if (!isSettled) "PLEASE PRESENT BILL TO CASHIER FOR SETTLEMENT" else "THANK YOU FOR YOUR VISIT!" } ?: if (!isSettled) "PLEASE PRESENT BILL TO CASHIER FOR SETTLEMENT" else "THANK YOU FOR YOUR VISIT!")
        val receiptFooter = escapeHtml(businessProfile?.receiptFooter.orEmpty())
        val logoWidth = businessProfile?.receiptLogoWidthMm?.coerceIn(10, 68) ?: 42
        val logoHeight = businessProfile?.receiptLogoHeightMm?.coerceIn(5, 40) ?: 20

        val itemsHtml = items.joinToString("") { (name, qtyPrice) ->
            val (qty, price) = qtyPrice
            """<div style="margin: 5px 0;">
                <div style="font-weight:bold;">${name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</div>
                <div style="display:flex; justify-content:space-between;">
                    <span>$qty × KES ${String.format("%,.2f", price)}</span>
                    <span>KES ${String.format("%,.2f", qty * price)}</span>
                </div>
            </div>"""
        }

        val htmlContent = """<!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>Receipt $orderNumber</title>
    <style>
        @page { size: 80mm auto; margin: 4mm; }
        body { width: 72mm; margin: 0 auto; color: #111; font-family: monospace; font-size: 11px; line-height: 1.35; }
        .center { text-align: center; }
        .logo { display: block; width: ${logoWidth}mm; height: ${logoHeight}mm; max-width: 100%; object-fit: contain; margin: 0 auto 5px; }
        .rule { border-top: 1px dashed #111; margin: 7px 0; }
        .line { display: flex; justify-content: space-between; }
        .total { font-size: 14px; font-weight: bold; margin-top: 4px; }
        @media print { .no-print { display: none; } }
    </style>
</head>
<body>
    <div class="center">
        $logo
        <h2 style="margin:2px 0;">$businessName</h2>
        <div style="font-size:10px; color:#444;">$businessAddress</div>
        <div style="font-size:10px; color:#444;">$businessCounty</div>
        <div style="font-size:10px; color:#444;">$businessPhone</div>
        $businessPin
    </div>
    <div class="rule"></div>
    <div class="line"><span>REF / ORDER:</span><strong>$orderNumber</strong></div>
    $tableInfo
    <div class="line"><span>DATE:</span><span>${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))}</span></div>
    <div class="line"><span>STATUS:</span>$statusText</div>
    ${if (showCustomer) "<div class='line'><span>CUSTOMER:</span><span>${escapeHtml(customerName)}</span></div>" else ""}
    ${if (showCustomer && customerPhone.isNotBlank()) "<div class='line'><span>PHONE:</span><span>${escapeHtml(customerPhone)}</span></div>" else ""}
    <div class="rule"></div>
    $itemsHtml
    <div class="rule"></div>
    <div class="line"><span>SUBTOTAL:</span><span>KES ${String.format("%,.2f", subtotal)}</span></div>
    ${if (showTax) "<div class=\"line\"><span>VAT (16%):</span><span>KES ${String.format("%,.2f", tax)}</span></div>" else ""}
    <div class="line total"><span>TOTAL:</span><span>KES ${String.format("%,.2f", grandTotal)}</span></div>
    <div class="rule"></div>
    <div class="center" style="margin-top:8px;">
        <strong>$receiptHeader</strong><br>$receiptFooter
    </div>
    <div class="center no-print" style="margin-top:16px;">
        <button onclick="window.print()">Print Receipt</button>
    </div>
    <script>window.addEventListener('load', () => setTimeout(() => window.print(), 200))</script>
</body>
</html>"""

        tempFile.writeText(htmlContent)
        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
            java.awt.Desktop.getDesktop().browse(tempFile.toURI())
        }
    } catch (_: Exception) {
        System.err.println("Could not open receipt for printing")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopPosScreen(
    inventoryViewModel: InventoryViewModel = remember { inject() },
    customersViewModel: CustomersViewModel = remember { inject() },
    businessViewModel: BusinessViewModel = remember { inject() },
    createOrderUseCase: CreateOrderUseCase = remember { inject() },
    initiatePaymentUseCase: InitiatePaymentUseCase = remember { inject() },
    client: HttpClient = remember { inject() },
    navigationViewModel: com.app.biashara.ui.DesktopNavigationViewModel = remember { inject() }
) {
    val canCreateOrder = hasNativePermission("orders.create")
    val coroutineScope = rememberCoroutineScope()
    val currentUserSession by UserSession.currentUser.collectAsState()
    val businessId = currentUserSession?.businessId ?: UserSession.getBusinessId()

    // Load inventory and customers
    LaunchedEffect(currentUserSession?.businessId) {
        inventoryViewModel.loadProducts(businessId)
        customersViewModel.loadCustomers()
        businessViewModel.loadMpesaConfig()
        businessViewModel.loadProfile()
    }

    val inventoryState by inventoryViewModel.state.collectAsState()
    val customersState by customersViewModel.state.collectAsState()
    val mpesaState by businessViewModel.mpesaState.collectAsState()
    val businessProfileState by businessViewModel.profileState.collectAsState()

    val isHospitalityActive = businessProfileState.profile?.hospitalityEnabled == true || businessProfileState.profile?.type == "HOSPITALITY"
    var tablesList by remember { mutableStateOf<List<PosHospitalityTableInfo>>(emptyList()) }
    var kitchenTickets by remember { mutableStateOf<List<PosKitchenTicket>>(emptyList()) }
    var openTabsList by remember { mutableStateOf<List<PosHospitalityOrderInfo>>(emptyList()) }
    var serviceType by remember { mutableStateOf("DINE_IN") }
    var selectedTable by remember { mutableStateOf<PosHospitalityTableInfo?>(null) }
    var guestCount by remember { mutableStateOf(1) }
    var isOpeningTab by remember { mutableStateOf(false) }
    val canOpenTabs = hasNativePermission("hospitality.orders")
    var tabActionError by remember { mutableStateOf<String?>(null) }
    var openedTabNumber by remember { mutableStateOf<String?>(null) }
    var shiftRecovery by remember { mutableStateOf(0) }

    var splitModalOrder by remember { mutableStateOf<PosHospitalityOrderInfo?>(null) }
    var settleModalOrder by remember { mutableStateOf<PosHospitalityOrderInfo?>(null) }

    val reloadHospitalityData: suspend () -> Unit = {
        if (isHospitalityActive) {
            runCatching {
                client.get("$BASE_URL/hospitality").body<ApiResponse<PosHospitalityDashboardResponse>>()
            }.onSuccess { resp ->
                val dashData = resp.data
                if (resp.success && dashData != null) {
                    tablesList = dashData.tables
                    openTabsList = dashData.openTabs
                    kitchenTickets = dashData.tickets.filter { ticket -> dashData.openTabs.any { it.id == ticket.orderId } }
                }
            }
        }
    }

    LaunchedEffect(isHospitalityActive) {
        if (!isHospitalityActive) return@LaunchedEffect
        while (isActive) {
            reloadHospitalityData()
            delay(5000)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val activeProducts = remember(inventoryState.products) {
        inventoryState.products.filter { it.isActive }
    }
    val groupCategories = remember(activeProducts) {
        activeProducts.map { it.posGroup() }.distinct().sortedBy { it.lowercase() }
    }
    val categories = remember(groupCategories) {
        listOf("All") + groupCategories
    }

    val filteredProducts = remember(activeProducts, searchQuery, selectedCategory) {
        activeProducts.filter { p ->
            val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) || p.sku.contains(searchQuery, ignoreCase = true) || p.category.contains(searchQuery, ignoreCase = true) || p.barcode?.contains(searchQuery, ignoreCase = true) == true
            val matchesCategory = when {
                selectedCategory == "All" -> true
                selectedCategory == "Popular" -> true
                selectedCategory == "Recent" -> true
                else -> p.posGroup() == selectedCategory
            }
            matchesSearch && matchesCategory
        }
    }

    // Cart and Transaction State
    val cart = remember { mutableStateListOf<DesktopCartItem>() }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var walkInName by remember { mutableStateOf("Walk-In Customer") }
    var walkInPhone by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var includeTax by remember { mutableStateOf(true) }
    var mpesaAccountType by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    var isCheckingOut by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successOrderNumber by remember { mutableStateOf<String?>(null) }
    var paymentFeedback by remember { mutableStateOf<String?>(null) }
    var showCartMenu by remember { mutableStateOf(false) }

    LaunchedEffect(mpesaState.configs) {
        val availableTypes = mpesaState.configs.map { it.accountType }
        if (mpesaAccountType !in availableTypes) {
            mpesaAccountType = availableTypes.firstOrNull()
        }
    }

    val subtotal = cart.sumOf { it.product.sellingPrice * it.qty }
    val tax = if (includeTax) kotlin.math.round(subtotal * 0.16 * 100.0) / 100.0 else 0.0
    val grandTotal = subtotal + tax

    Row(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7FB)).padding(20.dp)
    ) {
        // Left Side: Product Selection Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column {
                    Text("Point of Sale", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text("Fast checkout · Touch and keyboard ready", fontSize = 12.sp, color = Color(0xFF64748B))
                }

            }
            if (isHospitalityActive) {
                Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFFA7F3D0))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hospitality mode", color = B360Green, fontWeight = FontWeight.SemiBold)
                        Text(selectedTable?.let { "${it.name} · $guestCount guests" } ?: "Select a table for dine in", color = Color(0xFF477269), fontSize = 12.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                categories.forEach { category ->
                    val selected = selectedCategory == category
                    Surface(
                        onClick = { selectedCategory = category },
                        modifier = Modifier.widthIn(min = 128.dp).height(82.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) B360Green else Color.White,
                        border = BorderStroke(1.dp, if (selected) B360Green else Color(0xFFE4EBF3))
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(if (category == "All") Icons.Default.GridView else Icons.Default.RestaurantMenu, null, tint = if (selected) Color.White else Color(0xFF526580), modifier = Modifier.size(25.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(if (category == "All") "All Items" else category, color = if (selected) Color.White else Color(0xFF526580), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Search items, categories or barcode…", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF526580)) },
                    trailingIcon = { Icon(Icons.Default.QrCodeScanner, "Barcode search", tint = Color(0xFF526580)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, unfocusedBorderColor = Color(0xFFDCE5EF))
                )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(if (selectedCategory == "All") "All Items" else selectedCategory, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("Quick add items to the order", color = Color(0xFF64748B), fontSize = 13.sp)
                }
                Text("${filteredProducts.size} items", color = Color(0xFF64748B), fontSize = 12.sp)
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
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(filteredProducts) { p ->
                        val isOutOfStock = p.isOutOfStock
                        val cartItem = cart.firstOrNull { it.product.id == p.id }
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
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(10.dp))) {
                                    PosProductImage(p, Modifier.fillMaxSize())
                                    if (isOutOfStock) {
                                        Surface(Modifier.align(Alignment.TopStart).padding(8.dp), color = Color(0xFF64748B), shape = RoundedCornerShape(8.dp)) {
                                            Text("Out of stock", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    if (cartItem != null) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                                            color = B360Green,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                "${cartItem.qty} in order",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Column {
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

                                    FilledIconButton(
                                        enabled = !isOutOfStock && (cartItem?.qty ?: 0) < p.currentStock,
                                        onClick = {
                                            val existing = cart.firstOrNull { it.product.id == p.id }
                                            if (existing == null) cart.add(DesktopCartItem(p, 1))
                                            else if (existing.qty < p.currentStock) cart[cart.indexOf(existing)] = existing.copy(qty = existing.qty + 1)
                                        },
                                        modifier = Modifier.size(36.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFDDF7EE), contentColor = B360Green)
                                    ) { Icon(Icons.Default.Add, "Add ${p.name}", Modifier.size(20.dp)) }
                                }

                                if (cartItem != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val idx = cart.indexOf(cartItem)
                                                if (cartItem.qty > 1) cart[idx] = cartItem.copy(qty = cartItem.qty - 1)
                                                else cart.remove(cartItem)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) { Icon(Icons.Default.Remove, "Remove one", modifier = Modifier.size(15.dp)) }
                                        Text(cartItem.qty.toString(), fontWeight = FontWeight.ExtraBold, color = B360Green)
                                        IconButton(
                                            onClick = {
                                                if (cartItem.qty < p.currentStock) {
                                                    val idx = cart.indexOf(cartItem)
                                                    cart[idx] = cartItem.copy(qty = cartItem.qty + 1)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp),
                                            enabled = cartItem.qty < p.currentStock
                                        ) { Icon(Icons.Default.Add, "Add one", modifier = Modifier.size(15.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right Side: Shopping Cart Panel
        Card(
            modifier = Modifier.widthIn(min = 400.dp, max = 480.dp).fillMaxHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE4EBF3))
        ) {
            val cartScrollState = rememberScrollState()
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(cartScrollState)
                        .padding(22.dp),
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
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = "Current Order",
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
                                Text("Clear", color = B360Red, fontWeight = FontWeight.Bold)
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
                                        walkInPhone = ""
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


if (isHospitalityActive) {
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Hospitality Order Details", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = B360Green)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("DINE_IN" to "Dine In", "TAKEAWAY" to "Takeaway", "DELIVERY" to "Delivery").forEach { (type, label) ->
                                            FilterChip(
                                                modifier = Modifier.weight(1f),
                                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = B360Green, selectedLabelColor = Color.White, containerColor = Color(0xFFF5F8FC)),
                                                selected = serviceType == type,
                                                onClick = { serviceType = type; if (type != "DINE_IN") selectedTable = null },
                                                label = { Text(label, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                    if (serviceType == "DINE_IN") {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            var tableDropdownExpanded by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.weight(1f)) {
                                                OutlinedTextField(
                                                    value = selectedTable?.let { "${it.name} (${it.area})" } ?: "Select a table (required)",
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Table *", fontSize = 11.sp) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    trailingIcon = { IconButton(onClick = { tableDropdownExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                                                )
                                                DropdownMenu(
                                                    expanded = tableDropdownExpanded,
                                                    onDismissRequest = { tableDropdownExpanded = false }
                                                ) {
                                                    tablesList.forEach { tbl ->
                                                        DropdownMenuItem(
                                                            text = { Text("${tbl.name} · ${tbl.area} (${tbl.capacity} seats)") },
                                                            onClick = { selectedTable = tbl; tableDropdownExpanded = false }
                                                        )
                                                    }
                                                }
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("Guests", fontSize = 11.sp, color = Color(0xFF64748B))
                                                Surface(color = Color(0xFFF5F8FC), shape = RoundedCornerShape(10.dp)) {
                                                    Row(Modifier.height(48.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(onClick = { guestCount-- }, enabled = guestCount > 1, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Remove, "Fewer guests", Modifier.size(18.dp)) }
                                                        Text(guestCount.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                                        IconButton(onClick = { guestCount++ }, enabled = guestCount < 100, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Add, "More guests", Modifier.size(18.dp)) }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val activeTab = remember(openTabsList, selectedTable) {
                                        openTabsList.find { selectedTable != null && it.hospitalityTableId == selectedTable?.id && (it.tabStatus == "OPEN" || it.tabStatus == "AWAITING_PAYMENT") }
                                    }
                                    activeTab?.let { tab ->
                                        Surface(
                                            color = Color(0xFFFEF3C7),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Active Tab #${tab.orderNumber}", fontWeight = FontWeight.Bold, color = Color(0xFF92400E), fontSize = 12.sp)
                                                    Text("KES ${String.format("%,.2f", tab.subtotal)}", fontWeight = FontWeight.Bold, color = B360Green, fontSize = 13.sp)
                                                }
                                                Text("Guest: ${tab.customerName ?: "Walk-In Guest"} · ${tab.items.size} item(s)", fontSize = 11.sp, color = Color(0xFF78350F))
                                                if (kitchenTickets.any { it.orderId == tab.id && it.station == "KITCHEN" && it.status != "SERVED" }) Text("Serve kitchen items before settlement", color = Color(0xFF92400E), fontSize = 11.sp)
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Button(
                                                        onClick = {
                                                            val itemsList = tab.items.map { it.productName to (it.quantity to it.unitPrice) }
                                                            printReceiptDesktop(
                                                                orderNumber = tab.orderNumber,
                                                                tableName = selectedTable?.name,
                                                                items = itemsList,
                                                                subtotal = tab.subtotal,
                                                                tax = 0.0,
                                                                grandTotal = tab.subtotal,
                                                                isSettled = false,
                                                                customerName = tab.customerName ?: "Walk-In Customer",
                                                                businessProfile = businessProfileState.profile
                                                            )
                                                        },
                                                        modifier = Modifier.weight(1f).height(32.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Print Bill", fontSize = 11.sp, color = Color.White)
                                                    }

                                                    Button(
                                                        enabled = hasNativePermission("hospitality.billing") && kitchenTickets.none { it.orderId == tab.id && it.station == "KITCHEN" && it.status != "SERVED" },
                                                        onClick = { splitModalOrder = tab },
                                                        modifier = Modifier.weight(1f).height(32.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Split Bill", fontSize = 11.sp, color = Color.White)
                                                    }

                                                    Button(
                                                        enabled = hasNativePermission("hospitality.billing") && kitchenTickets.none { it.orderId == tab.id && it.station == "KITCHEN" && it.status != "SERVED" },
                                                        onClick = { settleModalOrder = tab },
                                                        modifier = Modifier.weight(1f).height(32.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Settle Tab", fontSize = 11.sp, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                                    PosProductImage(item.product, Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)))
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
                                        walkInPhone = ""
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

                        if (paymentMethod == PaymentMethod.MPESA && mpesaState.configs.size > 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("M-Pesa Channel", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    mpesaState.configs.forEach { config ->
                                        FilterChip(
                                            selected = mpesaAccountType == config.accountType,
                                            onClick = { mpesaAccountType = config.accountType },
                                            label = { Text(config.accountType.lowercase().replaceFirstChar { it.uppercase() }) },
                                            modifier = Modifier.weight(1f)
                                        )
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
                            label = { Text("Sale notes") },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(if (includeTax) Color(0xFFE1F8EF) else Color.White, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Include VAT in payment", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Apply 16% VAT to this sale", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(checked = includeTax, onCheckedChange = { includeTax = it })
                        }

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
                                Text(if (includeTax) "VAT (16%)" else "VAT (16%) — excluded", fontSize = 12.sp, color = Color.Gray)
                                Text("KES ${String.format("%,.0f", tax)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                if (!com.app.biashara.data.remote.NativeAccessSession.permits("orders.create")) {
                                    errorMessage = "Create order permission required"
                                    return@Button
                                }
                                if (cart.isEmpty()) {
                                    errorMessage = "Please add items to cart."
                                    return@Button
                                }
                                try {
                                    requireDesktopTable(isHospitalityActive, serviceType, selectedTable?.id)
                                    if (paymentMethod == PaymentMethod.MPESA) desktopMpesaPhone(walkInPhone)
                                } catch (e: IllegalArgumentException) {
                                    errorMessage = e.message
                                    return@Button
                                }
                                isCheckingOut = true
                                errorMessage = null
                                coroutineScope.launch {
                                if (isHospitalityActive) {
                                        try {
                                            val response = client.post("$BASE_URL/hospitality/orders") {
                                                contentType(ContentType.Application.Json)
                                                setBody(PosCreateHospitalityOrderReq(
                                                    tableId = selectedTable?.id,
                                                    guestCount = guestCount,
                                                    serviceType = serviceType,
                                                    customerName = walkInName,
                                                    customerPhone = walkInPhone,
                                                    items = cart.map { PosHospitalityOrderItemReq(it.product.id, it.product.name, it.qty, it.product.sellingPrice) },
                                                    notes = notes
                                                ))
                                            }.body<ApiResponse<PosHospitalityOrderInfo>>()
                                            check(response.success && response.data != null) {
                                                response.message.ifBlank { "Could not place hospitality order." }
                                            }
                                            cart.clear()
                                            notes = ""
                                            reloadHospitalityData()
                                            settleModalOrder = response.data
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Could not place hospitality order."
                                        } finally {
                                            isCheckingOut = false
                                        }
                                        return@launch
                                    }

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
                                        includeTax = includeTax,
                                        taxRate = 0.16,
                                        notes = notes,
                                        createdAt = Clock.System.now(),
                                        updatedAt = Clock.System.now()
                                    )

                                    createOrderUseCase(order)
                                        .onSuccess { savedOrder ->
                                            when (paymentMethod) {
                                                PaymentMethod.MPESA -> {
                                                    initiatePaymentUseCase(savedOrder.id, desktopMpesaPhone(walkInPhone), mpesaAccountType)
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
                                                        val checkoutUrl = "https://biashara360.co.ke/card-payments?orderId=${savedOrder.id}"
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
                                                    val itemsList = cart.map { it.product.name to (it.qty to it.product.sellingPrice) }
                                                    printReceiptDesktop(
                                                        orderNumber = savedOrder.orderNumber,
                                                        tableName = selectedTable?.name,
                                                        items = itemsList,
                                                        subtotal = subtotal,
                                                        tax = tax,
                                                        grandTotal = grandTotal,
                                                        isSettled = true,
                                                        paymentMethod = paymentMethod.name,
                                                        customerName = selectedCustomer?.name ?: walkInName,
                                                        customerPhone = selectedCustomer?.phone ?: walkInPhone,
                                                        businessProfile = businessProfileState.profile
                                                    )
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
                            enabled = canCreateOrder && !isCheckingOut && !isOpeningTab && cart.isNotEmpty()
                        ) {
                            if (isCheckingOut) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Complete Order (KES ${String.format("%,.0f", grandTotal)})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }

                        if (isHospitalityActive) {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (isOpeningTab || isCheckingOut) return@Button
                                    try {
                                        check(com.app.biashara.data.remote.NativeAccessSession.permits("hospitality.orders")) { "Hospitality orders permission required" }
                                        require(guestCount in 1..100) { "Guest count must be between 1 and 100" }
                                        requireDesktopTable(true, serviceType, selectedTable?.id)
                                    } catch (e: Exception) {
                                        tabActionError = e.message ?: "Unable to open tab"
                                        return@Button
                                    }
                                    isOpeningTab = true
                                    tabActionError = null
                                    coroutineScope.launch {
                                        errorMessage = null
                                        runCatching {
                                            val req = PosCreateHospitalityOrderReq(
                                                tableId = selectedTable?.id,
                                                guestCount = guestCount,
                                                serviceType = serviceType,
                                                customerName = selectedCustomer?.name ?: walkInName,
                                                customerPhone = selectedCustomer?.phone ?: walkInPhone,
                                                items = cart.map {
                                                    PosHospitalityOrderItemReq(
                                                        productId = it.product.id,
                                                        productName = it.product.name,
                                                        quantity = it.qty,
                                                        unitPrice = it.product.sellingPrice
                                                    )
                                                },
                                                notes = notes
                                            )
                                            openDesktopTab(client, req)
                                        }.onSuccess { resp ->
                                            val orderRes = resp.data
                                            if (resp.success && orderRes != null) {
                                                openedTabNumber = orderRes.orderNumber
                                                paymentFeedback = "Tab #${orderRes.orderNumber} opened."
                                                val itemsList = cart.map { it.product.name to (it.qty to it.product.sellingPrice) }
                                                printReceiptDesktop(
                                                    orderNumber = orderRes.orderNumber,
                                                    tableName = selectedTable?.name,
                                                    items = itemsList,
                                                    subtotal = subtotal,
                                                    tax = tax,
                                                    grandTotal = grandTotal,
                                                    isSettled = false,
                                                    customerName = selectedCustomer?.name ?: walkInName,
                                                    customerPhone = selectedCustomer?.phone ?: walkInPhone,
                                                    businessProfile = businessProfileState.profile
                                                )
                                                cart.clear()
                                                notes = ""
                                                reloadHospitalityData()
                                            } else {
                                                tabActionError = resp.message.ifBlank { "Could not open customer tab." }
                                            }
                                        }.onFailure { err ->
                                            tabActionError = err.message ?: "Failed to open hospitality tab."
                                        }
                                        isOpeningTab = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp),
                                enabled = canOpenTabs && !isOpeningTab && !isCheckingOut && cart.isNotEmpty()
                            ) {
                                if (isOpeningTab) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Restaurant, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("Open Tab & Send Food to Kitchen", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(cartScrollState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }

        tabActionError?.let { message ->
            AlertDialog(
                onDismissRequest = { tabActionError = null },
                title = { Text("Could not open tab") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = {
                        tabActionError = null
                        if (message.contains("shift", ignoreCase = true)) shiftRecovery++
                    }) { Text(if (message.contains("shift", ignoreCase = true)) "Check shift" else "OK") }
                }
            )
        }
        if (shiftRecovery > 0) {
            val access by com.app.biashara.data.remote.NativeAccessSession.access.collectAsState()
            key(shiftRecovery) { com.app.biashara.ui.NativeLoginShiftPrompt(client, access) }
        }
        openedTabNumber?.let { number ->
            com.app.biashara.ui.DesktopTabOpenedDialog(number,
                onContinue = { openedTabNumber = null },
                onViewTabs = { openedTabNumber = null; navigationViewModel.navigateTo(com.app.biashara.ui.AppScreen.OpenTabs) })
        }

        splitModalOrder?.let { order ->
            SplitBillDialogOverlay(
                order = order,
                onDismiss = { splitModalOrder = null },
                onSplitComplete = { feedback ->
                    splitModalOrder = null
                    paymentFeedback = feedback
                    openTabsList = openTabsList.filterNot { it.id == order.id }
                    kitchenTickets = kitchenTickets.filterNot { it.orderId == order.id }
                    coroutineScope.launch { reloadHospitalityData() }
                },
                client = client,
                coroutineScope = coroutineScope,
                businessProfile = businessProfileState.profile,
                kitchenServed = kitchenTickets.none { it.orderId == order.id && it.station == "KITCHEN" && it.status != "SERVED" }
            )
        }

        settleModalOrder?.let { order ->
            SettleTabDialogOverlay(
                order = order,
                onDismiss = { settleModalOrder = null },
                onSettleComplete = { feedback ->
                    settleModalOrder = null
                    paymentFeedback = feedback
                    if (feedback.contains("settled and closed", ignoreCase = true)) {
                        openTabsList = openTabsList.filterNot { it.id == order.id }
                        kitchenTickets = kitchenTickets.filterNot { it.orderId == order.id }
                    }
                    coroutineScope.launch { reloadHospitalityData() }
                },
                client = client,
                coroutineScope = coroutineScope,
                initialPaymentMethod = if (paymentMethod == PaymentMethod.COD) "CASH" else paymentMethod.name,
                mpesaAccountType = mpesaAccountType,
                businessProfile = businessProfileState.profile,
                kitchenServed = kitchenTickets.none { it.orderId == order.id && it.station == "KITCHEN" && it.status != "SERVED" }
            )
        }
    }
}

@Composable
private fun SplitBillDialogOverlay(
    order: PosHospitalityOrderInfo,
    onDismiss: () -> Unit,
    onSplitComplete: (String) -> Unit,
    client: HttpClient,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    businessProfile: BusinessProfile?,
    kitchenServed: Boolean = true
) {
    var splitMode by remember { mutableStateOf("EQUAL") }
    var splitGuests by remember { mutableStateOf(2) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(kitchenServed) { if (!kitchenServed) errorMsg = "Serve all kitchen orders before settling this tab" else if (errorMsg == "Serve all kitchen orders before settling this tab") errorMsg = null }

    val half = order.subtotal / 2.0
    var customLine1Method by remember { mutableStateOf("CASH") }
    var customLine1Amount by remember { mutableStateOf(half.toString()) }
    var customLine1Phone by remember { mutableStateOf("") }

    var customLine2Method by remember { mutableStateOf("MPESA") }
    var customLine2Amount by remember { mutableStateOf((order.subtotal - half).toString()) }
    var customLine2Phone by remember { mutableStateOf(order.customerPhone ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .width(440.dp)
                .clickable(enabled = false) {}
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Split Bill Request", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Order #${order.orderNumber}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Total: KES ${String.format("%,.2f", order.subtotal)}", fontWeight = FontWeight.Bold, color = B360Green, fontSize = 13.sp)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = splitMode == "EQUAL", onClick = { splitMode = "EQUAL" }, label = { Text("Equal Split") })
                    FilterChip(selected = splitMode == "CUSTOM", onClick = { splitMode = "CUSTOM" }, label = { Text("By Payment Method") })
                }

                if (splitMode == "EQUAL") {
                    Text("Select number of split guests:", fontSize = 12.sp, color = Color(0xFF64748B))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 3, 4, 5).forEach { num ->
                            FilterChip(
                                selected = splitGuests == num,
                                onClick = { splitGuests = num },
                                label = { Text("$num Ways (${String.format("%,.0f", order.subtotal / num)} ea)", fontSize = 11.sp) }
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Split Payment Allocations:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = customLine1Amount, onValueChange = { customLine1Amount = it }, label = { Text("Line 1 (KES)") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = customLine1Method, onValueChange = { customLine1Method = it }, label = { Text("Method") }, modifier = Modifier.width(90.dp), singleLine = true)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = customLine2Amount, onValueChange = { customLine2Amount = it }, label = { Text("Line 2 (KES)") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = customLine2Method, onValueChange = { customLine2Method = it }, label = { Text("Method") }, modifier = Modifier.width(90.dp), singleLine = true)
                        }
                    }
                }

                errorMsg?.let {
                    Text(it, color = B360Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSubmitting = true
                                errorMsg = null
                                runCatching {
                                    val splitItems = if (splitMode == "EQUAL") {
                                        val perGuest = order.subtotal / splitGuests
                                        (1..splitGuests).map { PosSplitPaymentReq(method = "CASH", amount = perGuest) }
                                    } else {
                                        val amt1 = customLine1Amount.toDoubleOrNull() ?: 0.0
                                        val amt2 = customLine2Amount.toDoubleOrNull() ?: 0.0
                                        if (kotlin.math.abs((amt1 + amt2) - order.subtotal) > 1.0) {
                                            throw IllegalArgumentException("Split sum (KES ${amt1 + amt2}) must equal total (KES ${order.subtotal})")
                                        }
                                        listOf(
                                            PosSplitPaymentReq(method = customLine1Method.uppercase(), amount = amt1, phone = customLine1Phone),
                                            PosSplitPaymentReq(method = customLine2Method.uppercase(), amount = amt2, phone = customLine2Phone)
                                        )
                                    }

                                    val resp = client.post("$BASE_URL/hospitality/operations/tabs/${order.id}/split") {
                                        contentType(ContentType.Application.Json)
                                        setBody(splitItems)
                                    }.body<ApiResponse<Unit>>()

                                    if (resp.success) {
                                        splitItems.forEachIndexed { idx, item ->
                                            printReceiptDesktop(
                                                orderNumber = "${order.orderNumber}-SPLIT-${idx + 1}",
                                                tableName = order.deliveryLocation,
                                                items = order.items.map { it.productName to (it.quantity to it.unitPrice) },
                                                subtotal = item.amount,
                                                tax = 0.0,
                                                grandTotal = item.amount,
                                                isSettled = false,
                                                paymentMethod = item.method,
                                                customerName = "Guest ${idx + 1} (${order.customerName ?: "Walk-in"})",
                                                businessProfile = businessProfile
                                            )
                                        }
                                        onSplitComplete("Bill for Order #${order.orderNumber} split successfully into ${splitItems.size} payments!")
                                    } else {
                                        errorMsg = resp.message.ifBlank { "Could not split bill" }
                                    }
                                }.onFailure {
                                    errorMsg = it.message ?: "Failed to split bill"
                                }
                                isSubmitting = false
                            }
                        },
                        enabled = !isSubmitting && kitchenServed,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        else Text("Confirm & Split", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettleTabDialogOverlay(
    order: PosHospitalityOrderInfo,
    onDismiss: () -> Unit,
    onSettleComplete: (String) -> Unit,
    client: HttpClient,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    initialPaymentMethod: String,
    mpesaAccountType: String?,
    businessProfile: BusinessProfile?,
    kitchenServed: Boolean = true
) {
    var paymentMethod by remember { mutableStateOf(initialPaymentMethod) }
    var mpesaPhone by remember { mutableStateOf(order.customerPhone ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(kitchenServed) { if (!kitchenServed) errorMsg = "Serve all kitchen orders before settling this tab" else if (errorMsg == "Serve all kitchen orders before settling this tab") errorMsg = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = { if (!isSubmitting) onDismiss() }),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .width(420.dp)
                .clickable(enabled = false) {}
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Settle & Close Tab", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    IconButton(onClick = { if (!isSubmitting) onDismiss() }) { Icon(Icons.Default.Close, null) }
                }

                Surface(color = Color(0xFFF0FDF4), border = BorderStroke(1.dp, Color(0xFFBBF7D0)), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tab #${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                            Text("KES ${String.format("%,.2f", order.subtotal)}", fontWeight = FontWeight.Bold, color = B360Green, fontSize = 14.sp)
                        }
                        Text("Customer: ${order.customerName ?: "Walk-in Customer"}", fontSize = 12.sp, color = Color(0xFF15803D))
                    }
                }

                Text("Select Settlement Payment Method:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CASH" to "Cash", "MPESA" to "M-Pesa", "CARD" to "Card").forEach { (method, label) ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            enabled = !isSubmitting && kitchenServed,
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                if (paymentMethod == "MPESA") {
                    OutlinedTextField(
                        value = mpesaPhone,
                        onValueChange = { mpesaPhone = it },
                        label = { Text("Customer M-Pesa Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                errorMsg?.let {
                    Text(it, color = B360Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (!isSubmitting) onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSubmitting = true
                                errorMsg = null
                                runCatching {
                                    val result = settleDesktopTab(client, order.id, paymentMethod, mpesaPhone, mpesaAccountType)
                                    if (result.paid) {
                                        val itemsList = order.items.map { it.productName to (it.quantity to it.unitPrice) }
                                        printReceiptDesktop(
                                            orderNumber = order.orderNumber,
                                            tableName = order.deliveryLocation,
                                            items = itemsList,
                                            subtotal = order.subtotal,
                                            tax = 0.0,
                                            grandTotal = order.subtotal,
                                            isSettled = true,
                                            paymentMethod = paymentMethod,
                                            customerName = order.customerName ?: "Walk-in Customer",
                                            customerPhone = mpesaPhone,
                                            businessProfile = businessProfile
                                        )
                                    }
                                    if (paymentMethod == "CARD") {
                                        java.awt.Desktop.getDesktop().browse(java.net.URI("https://biashara360.co.ke/card-payments?orderId=${order.id}"))
                                    }
                                    onSettleComplete("Tab #${order.orderNumber}: ${result.message}")
                                }.onFailure {
                                    errorMsg = it.message ?: "Failed to settle tab"
                                }
                                isSubmitting = false
                            }
                        },
                        enabled = !isSubmitting && kitchenServed,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                    ) {
                        if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        else Text(if (paymentMethod == "MPESA") "Send M-Pesa Prompt" else "Complete Settlement", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
private fun PosProductImage(product: Product, modifier: Modifier = Modifier) {
    var bitmap by remember(product.imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(product.imageUrl) {
        val path = product.imageUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val url = java.net.URI(BASE_URL).resolve(path).toURL()
                require(url.protocol in setOf("https", "http"))
                val connection = url.openConnection()
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.getInputStream().use { loadImageBitmap(it) }
            }.getOrNull()
        }
    }
    Box(modifier.background(Color(0xFFEAF4F0)), contentAlignment = Alignment.Center) {
        val photo = bitmap
        if (photo != null) Image(photo, product.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(product.name.take(2).uppercase(), color = B360Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
