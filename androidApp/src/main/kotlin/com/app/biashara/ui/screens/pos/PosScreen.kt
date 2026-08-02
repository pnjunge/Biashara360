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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.geometry.Offset
import com.app.biashara.UserSession
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.CreateOrderUseCase
import com.app.biashara.domain.usecase.InitiatePaymentUseCase
import com.app.biashara.domain.usecase.generateId
import com.app.biashara.presentation.viewmodel.CustomersViewModel
import com.app.biashara.presentation.viewmodel.InventoryViewModel
import com.app.biashara.presentation.viewmodel.BusinessViewModel
import com.app.biashara.ui.kmpViewModel
import com.app.biashara.ui.LocalNetworkAvailable
import com.app.biashara.ui.SecureScreen
import com.app.biashara.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

/** Kenya standard VAT rate. TODO: Source from business profile KRA config. */
private const val VAT_RATE = 0.16

data class MobileCartItem(val product: Product, var qty: Int)

private data class CheckoutResult(
    val orderId: String,
    val orderNumber: String,
    val paymentMethod: PaymentMethod,
    val phoneNumber: String,
    val paymentMessage: String,
    val paymentPromptAccepted: Boolean,
    val mpesaAccountType: String? = null
)

private fun friendlyPaymentError(message: String): String =
    if (message.contains("Invalid TransactionType", ignoreCase = true)) {
        "This shortcode may not be provisioned as the selected Paybill or Till type."
    } else {
        message.ifBlank { "The M-Pesa prompt could not be sent." }
    }

// ─── Filter Tab Model ─────────────────────────────────────────────────────────
private enum class PosFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Filled.GridView),
    FAVORITES("Favorites", Icons.Filled.StarBorder),
    CATEGORIES("Categories", Icons.Filled.LocalOffer),
    RECENT("Recent", Icons.Filled.AccessTime)
}

@Composable
fun PaperAirplaneBoxIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height * 0.58f

        // Draw the background circle
        drawCircle(
            color = Color(0xFFE8F5EE),
            radius = width / 2f,
            center = Offset(cx, cy - height * 0.08f)
        )

        // Box size parameters
        val w = width * 0.22f
        val h = height * 0.12f

        // Box Coordinates
        val bottomCenter = Offset(cx, cy + h)
        val bottomLeft = Offset(cx - w, cy + h * 0.4f)
        val bottomRight = Offset(cx + w, cy + h * 0.4f)
        val topCenter = Offset(cx, cy)
        val topLeft = Offset(cx - w, cy - h * 0.6f)
        val topRight = Offset(cx + w, cy - h * 0.6f)
        val backCenter = Offset(cx, cy - h * 1.2f)

        // 1. Inside Back shadow
        val innerPath = Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(backCenter.x, backCenter.y)
            lineTo(topRight.x, topRight.y)
            lineTo(topCenter.x, topCenter.y)
            close()
        }
        drawPath(innerPath, color = Color(0xFF047857).copy(alpha = 0.25f))

        // 2. Left side/flap (folded out)
        val leftFlap = Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(backCenter.x, backCenter.y)
            lineTo(backCenter.x - w * 0.7f, backCenter.y - h * 0.2f)
            lineTo(topLeft.x - w * 0.7f, topLeft.y - h * 0.2f)
            close()
        }
        drawPath(leftFlap, color = Color(0xFFD1FAE5))

        // 3. Right side/flap (folded out)
        val rightFlap = Path().apply {
            moveTo(topRight.x, topRight.y)
            lineTo(backCenter.x, backCenter.y)
            lineTo(backCenter.x + w * 0.7f, backCenter.y - h * 0.2f)
            lineTo(topRight.x + w * 0.7f, topRight.y - h * 0.2f)
            close()
        }
        drawPath(rightFlap, color = Color(0xFFD1FAE5))

        // 4. Front Left wall
        val frontLeftPath = Path().apply {
            moveTo(bottomCenter.x, bottomCenter.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            lineTo(topLeft.x, topLeft.y)
            lineTo(topCenter.x, topCenter.y)
            close()
        }
        drawPath(frontLeftPath, color = Color(0xFFA7F3D0))

        // 5. Front Right wall
        val frontRightPath = Path().apply {
            moveTo(bottomCenter.x, bottomCenter.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(topRight.x, topRight.y)
            lineTo(topCenter.x, topCenter.y)
            close()
        }
        drawPath(frontRightPath, color = Color(0xFF6EE7B7))

        // 6. Front Left flap (folded down)
        val frontLeftFlap = Path().apply {
            moveTo(topLeft.x, topLeft.y)
            lineTo(topCenter.x, topCenter.y)
            lineTo(topCenter.x - w * 0.2f, topCenter.y + h * 0.8f)
            lineTo(topLeft.x - w * 0.2f, topLeft.y + h * 0.8f)
            close()
        }
        drawPath(frontLeftFlap, color = Color(0xFFA7F3D0))

        // 7. Front Right flap (folded down)
        val frontRightFlap = Path().apply {
            moveTo(topRight.x, topRight.y)
            lineTo(topCenter.x, topCenter.y)
            lineTo(topCenter.x + w * 0.2f, topCenter.y + h * 0.8f)
            lineTo(topRight.x + w * 0.2f, topRight.y + h * 0.8f)
            close()
        }
        drawPath(frontRightFlap, color = Color(0xFF6EE7B7))

        // 8. Paper Airplane Coordinates (top right)
        val ax = cx + width * 0.22f
        val ay = cy - height * 0.32f

        // Airplane Wings/Body
        val nose = Offset(ax + width * 0.12f, ay - height * 0.12f)
        val leftWing = Offset(ax - width * 0.1f, ay + height * 0.04f)
        val rightWing = Offset(ax + width * 0.02f, ay + height * 0.08f)
        val bottomFold = Offset(ax - width * 0.02f, ay + height * 0.03f)

        // Dotted Trail path (curved bezier)
        val trailPath = Path().apply {
            moveTo(cx, cy - h * 0.4f)
            cubicTo(
                cx - width * 0.22f, cy - height * 0.15f,
                cx - width * 0.1f, cy - height * 0.35f,
                leftWing.x, leftWing.y
            )
        }
        drawPath(
            path = trailPath,
            color = Color(0xFF10B981).copy(alpha = 0.5f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f),
                cap = StrokeCap.Round
            )
        )

        // Draw Paper Airplane parts
        // Left underwing (darker)
        val planeUnder = Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(leftWing.x, leftWing.y)
            lineTo(bottomFold.x, bottomFold.y)
            close()
        }
        drawPath(planeUnder, color = Color(0xFF047857))

        // Main wing/body (medium green)
        val planeMain = Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(rightWing.x, rightWing.y)
            lineTo(bottomFold.x, bottomFold.y)
            close()
        }
        drawPath(planeMain, color = Color(0xFF34D399))

        // Outer flap (lighter green)
        val planeOuter = Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(rightWing.x, rightWing.y)
            lineTo(bottomFold.x + width * 0.03f, bottomFold.y + height * 0.02f)
            close()
        }
        drawPath(planeOuter, color = Color(0xFF6EE7B7))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    inventoryViewModel: InventoryViewModel = kmpViewModel(),
    customersViewModel: CustomersViewModel = kmpViewModel(),
    businessViewModel: BusinessViewModel = kmpViewModel(),
    createOrderUseCase: CreateOrderUseCase = koinInject(),
    initiatePaymentUseCase: InitiatePaymentUseCase = koinInject()
) {
    SecureScreen()
    val coroutineScope = rememberCoroutineScope()
    val businessId = remember { UserSession.getBusinessId() }
    val networkAvailable = LocalNetworkAvailable.current

    LaunchedEffect(Unit) {
        inventoryViewModel.loadProducts(businessId)
        customersViewModel.loadCustomers()
        businessViewModel.loadMpesaConfig()
        businessViewModel.loadProfile()
    }

    val inventoryState by inventoryViewModel.state.collectAsState()
    val customersState by customersViewModel.state.collectAsState()
    val mpesaState by businessViewModel.mpesaState.collectAsState()
    val businessProfileState by businessViewModel.profileState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PosFilter.ALL) }
    var selectedCategory by remember { mutableStateOf("All") }
    val favoriteProductIds = remember { mutableStateListOf<String>() }

    val recentProductIds = remember(inventoryState.products) {
        inventoryState.products.sortedByDescending { it.createdAt }.take(5).map { it.id }.toSet()
    }

    val categories = remember(inventoryState.products) {
        listOf("All") + inventoryState.products.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    val filteredProducts = remember(inventoryState.products, searchQuery, selectedFilter, selectedCategory, favoriteProductIds) {
        inventoryState.products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.sku.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedFilter) {
                PosFilter.ALL -> true
                PosFilter.FAVORITES -> p.id in favoriteProductIds
                PosFilter.CATEGORIES -> selectedCategory == "All" || p.category == selectedCategory
                PosFilter.RECENT -> p.id in recentProductIds
            }
            matchesSearch && matchesFilter && p.isActive
        }
    }

    val cart = remember { mutableStateListOf<MobileCartItem>() }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var walkInName by remember { mutableStateOf("Walk-In Customer") }
    var walkInPhone by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var mpesaAccountType by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var isCheckingOut by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var checkoutResult by remember { mutableStateOf<CheckoutResult?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }

    LaunchedEffect(mpesaState.configs) {
        val availableTypes = mpesaState.configs.map { it.accountType }
        if (mpesaAccountType !in availableTypes) {
            mpesaAccountType = availableTypes.firstOrNull()
        }
    }

    val subtotal = cart.sumOf { it.product.sellingPrice * it.qty }
    val tax = subtotal * VAT_RATE  // 16% Kenya standard VAT
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
                            val isHospitalityActive = businessProfileState.profile?.hospitalityEnabled == true || businessProfileState.profile?.type == "HOSPITALITY"
                            Text("Point of Sale", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF0F172A))
                            Text(
                                if (isHospitalityActive) "Hospitality mode active · Unified POS interface" else "Find and select products to start a sale",
                                fontSize = 13.sp,
                                color = if (isHospitalityActive) B360Green else Color(0xFF64748B)
                            )
                        }
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F5EE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.FilterAlt, contentDescription = "Filter", tint = B360Green, modifier = Modifier.size(22.dp))
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

                    // Secondary Category Chips
                    if (selectedFilter == PosFilter.CATEGORIES && categories.size > 1) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                val isCatSelected = selectedCategory == cat
                                Surface(
                                    onClick = { selectedCategory = cat },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isCatSelected) B360Green.copy(alpha = 0.12f) else Color.White,
                                    border = BorderStroke(1.dp, if (isCatSelected) B360Green else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCatSelected) B360Green else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
            if (cart.isNotEmpty() && checkoutResult == null) {
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
                        // Paper Airplane and Open Box illustration
                        PaperAirplaneBoxIllustration(
                            modifier = Modifier.size(160.dp)
                        )

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
                            onClick = { searchQuery = ""; selectedFilter = PosFilter.ALL; selectedCategory = "All" },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, B360Green),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = B360Green)
                        ) {
                            Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                p.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            val isFav = favoriteProductIds.contains(p.id)
                                            Icon(
                                                imageVector = if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                contentDescription = "Favorite",
                                                tint = if (isFav) B360Amber else Color(0xFF94A3B8),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clickable {
                                                        if (isFav) favoriteProductIds.remove(p.id) else favoriteProductIds.add(p.id)
                                                    }
                                            )
                                        }
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
        checkoutResult?.let { result ->
            AlertDialog(
                onDismissRequest = { if (!isCheckingOut) checkoutResult = null },
                title = {
                    Text(
                        if (result.paymentPromptAccepted || result.paymentMethod == PaymentMethod.CASH) {
                            "Checkout Successful"
                        } else {
                            "Order Created — Payment Pending"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            if (result.paymentPromptAccepted || result.paymentMethod == PaymentMethod.CASH) Icons.Filled.CheckCircle else Icons.Filled.Pending,
                            null,
                            tint = if (result.paymentPromptAccepted || result.paymentMethod == PaymentMethod.CASH) B360Green else Color(0xFFF59E0B),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Order Number: ${result.orderNumber}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        Text(result.paymentMessage, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color(0xFF64748B))
                    }
                },
                confirmButton = {
                    Button(onClick = { checkoutResult = null }, enabled = !isCheckingOut, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                        Text("New Sale", color = Color.White)
                    }
                },
                dismissButton = {
                    if (result.paymentMethod == PaymentMethod.MPESA && !result.paymentPromptAccepted) {
                        OutlinedButton(
                            enabled = !isCheckingOut,
                            onClick = {
                                isCheckingOut = true
                                coroutineScope.launch {
                                    initiatePaymentUseCase(
                                        result.orderId,
                                        result.phoneNumber,
                                        result.mpesaAccountType
                                    )
                                        .onSuccess {
                                            checkoutResult = result.copy(
                                                paymentMessage = it.customerMessage,
                                                paymentPromptAccepted = true
                                            )
                                        }
                                        .onFailure {
                                            checkoutResult = result.copy(
                                                paymentMessage = friendlyPaymentError(it.message.orEmpty())
                                            )
                                        }
                                    isCheckingOut = false
                                }
                            }
                        ) {
                            if (isCheckingOut) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Retry M-Pesa")
                            }
                        }
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
                            DropdownMenuItem(text = { Text("Walk-In Customer") }, onClick = { selectedCustomer = null; walkInName = "Walk-In Customer"; walkInPhone = ""; expanded = false })
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

                    if (paymentMethod == PaymentMethod.MPESA && mpesaState.configs.size > 1) {
                        Text("M-Pesa channel", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            mpesaState.configs.forEach { config ->
                                val selected = mpesaAccountType == config.accountType
                                FilterChip(
                                    selected = selected,
                                    onClick = { mpesaAccountType = config.accountType },
                                    label = { Text(config.accountType.displayMpesaChannel()) },
                                    modifier = Modifier.weight(1f)
                                )
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
                            if (!networkAvailable) {
                                errorMessage = "You’re offline. Reconnect before creating an order or collecting payment."
                                return@Button
                            }
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
                                    .onSuccess { saved ->
                                        val phone = selectedCustomer?.phone ?: walkInPhone
                                        val result = if (paymentMethod == PaymentMethod.MPESA) {
                                            initiatePaymentUseCase(saved.id, phone, mpesaAccountType).fold(
                                                onSuccess = {
                                                    CheckoutResult(saved.id, saved.orderNumber, paymentMethod, phone, it.customerMessage, true, mpesaAccountType)
                                                },
                                                onFailure = {
                                                    CheckoutResult(
                                                        saved.id,
                                                        saved.orderNumber,
                                                        paymentMethod,
                                                        phone,
                                                        friendlyPaymentError(it.message.orEmpty()),
                                                        false,
                                                        mpesaAccountType
                                                    )
                                                }
                                            )
                                        } else {
                                            CheckoutResult(
                                                saved.id,
                                                saved.orderNumber,
                                                paymentMethod,
                                                phone,
                                                if (paymentMethod == PaymentMethod.CASH) "Cash payment recorded."
                                                else "Order saved. Collect or reconcile the card payment from Payments.",
                                                paymentMethod == PaymentMethod.CASH
                                            )
                                        }
                                        checkoutResult = result
                                        cart.clear()
                                        notes = ""
                                        showCartSheet = false
                                        inventoryViewModel.loadProducts(businessId)
                                    }
                                    .onFailure { err -> errorMessage = err.message ?: "Failed to save sale." }
                                isCheckingOut = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isCheckingOut && cart.isNotEmpty() && networkAvailable
                    ) {
                        if (isCheckingOut) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text(
                            if (networkAvailable) "Complete POS Checkout" else "Reconnect to Checkout",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun String.displayMpesaChannel(): String =
    lowercase().replaceFirstChar { it.uppercase() }
