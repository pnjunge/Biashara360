package com.app.biashara.ui.screens.pos

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import com.app.biashara.ui.LocalWindowWidthSizeClass
import com.app.biashara.ui.SecureScreen
import com.app.biashara.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL

@kotlinx.serialization.Serializable
private data class PosTable(
    val id: String,
    val name: String,
    val area: String = "",
    val capacity: Int = 1,
    val status: String = "AVAILABLE",
    val mergedIntoTableId: String? = null
)

@kotlinx.serialization.Serializable
private data class PosHospitalityDashboard(val tables: List<PosTable> = emptyList())

@kotlinx.serialization.Serializable
private data class PosTaxRate(
    val id: String,
    val name: String,
    val rate: Double,
    val taxType: String,
    val isInclusive: Boolean,
    val isActive: Boolean,
    val appliesTo: String
)

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
            lineTo(topCenter.x - w * 0.2f, topCenter.y + h * 0.8f)
            lineTo(topRight.x + w * 0.2f, topRight.y + h * 0.8f)
            close()
        }
        drawPath(frontRightFlap, color = Color(0xFF6EE7B7))

        // 8. Paper Airplane Coordinates (top right)
        val ax = cx + width * 0.22f
        val ay = cy - height * 0.32f

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
        val planeUnder = Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(leftWing.x, leftWing.y)
            lineTo(bottomFold.x, bottomFold.y)
            close()
        }
        drawPath(planeUnder, color = Color(0xFF047857))

        val planeMain = Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(rightWing.x, rightWing.y)
            lineTo(bottomFold.x, bottomFold.y)
            close()
        }
        drawPath(planeMain, color = Color(0xFF34D399))

        val planeOuter = Path().apply {
            moveTo(nose.x, nose.y)
            lineTo(rightWing.x, rightWing.y)
            lineTo(bottomFold.x + width * 0.03f, bottomFold.y + height * 0.02f)
            close()
        }
        drawPath(planeOuter, color = Color(0xFF6EE7B7))
    }
}

@Composable
private fun PosEmptyProductsView(
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PaperAirplaneBoxIllustration(modifier = Modifier.size(160.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            "No products match filter",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF0F172A)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Try adjusting your search or filter\nto find what you're looking for.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(28.dp))
        OutlinedButton(
            onClick = onClearFilter,
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

@Composable
private fun PosCatalogSearchFilterHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: PosFilter,
    onFilterChange: (PosFilter) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    hospitalityEnabled: Boolean,
    itemCount: Int,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Point of Sale",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = if (hospitalityEnabled) "Hospitality mode active · Unified POS interface" else "Find and select products to start a sale",
                    fontSize = 13.sp,
                    color = if (hospitalityEnabled) B360Green else Color(0xFF64748B)
                )
            }
            if (!isCompact) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F5EE)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(B360Green))
                        Text(
                            text = "$itemCount items",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = B360Green
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F5EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.FilterAlt, contentDescription = "Filter", tint = B360Green, modifier = Modifier.size(22.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name or SKU...", color = Color(0xFFB0BBC8)) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                } else {
                    Icon(Icons.Filled.QrCodeScanner, "Scan", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                }
            },
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

        if (!isCompact) {
            // ── Category Tabs for Split-Pane POS Screen ───────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All Items" Tab
                val isAllSelected = selectedFilter == PosFilter.ALL
                Surface(
                    onClick = {
                        onFilterChange(PosFilter.ALL)
                        onCategoryChange("All")
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isAllSelected) B360Green else Color.White,
                    border = if (isAllSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.GridView,
                            contentDescription = null,
                            tint = if (isAllSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "All Items",
                            fontSize = 13.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isAllSelected) Color.White else Color(0xFF64748B)
                        )
                    }
                }

                // Inventory Category Tabs
                categories.filter { it != "All" }.forEach { cat ->
                    val isCatSelected = selectedFilter == PosFilter.CATEGORIES && selectedCategory == cat
                    Surface(
                        onClick = {
                            onFilterChange(PosFilter.CATEGORIES)
                            onCategoryChange(cat)
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = if (isCatSelected) B360Green else Color.White,
                        border = if (isCatSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocalOffer,
                                contentDescription = null,
                                tint = if (isCatSelected) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                cat,
                                fontSize = 13.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCatSelected) Color.White else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Favorites Quick Filter Tab
                val isFavSelected = selectedFilter == PosFilter.FAVORITES
                Surface(
                    onClick = { onFilterChange(PosFilter.FAVORITES) },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isFavSelected) B360Green else Color.White,
                    border = if (isFavSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = if (isFavSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Favorites",
                            fontSize = 13.sp,
                            fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isFavSelected) Color.White else Color(0xFF64748B)
                        )
                    }
                }

                // Recent Quick Filter Tab
                val isRecentSelected = selectedFilter == PosFilter.RECENT
                Surface(
                    onClick = { onFilterChange(PosFilter.RECENT) },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isRecentSelected) B360Green else Color.White,
                    border = if (isRecentSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = if (isRecentSelected) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Recent",
                            fontSize = 13.sp,
                            fontWeight = if (isRecentSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRecentSelected) Color.White else Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            // Filter Tabs for Compact Mode
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PosFilter.values().forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { onFilterChange(filter) },
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

            // Secondary Category Chips for Compact Mode
            if (selectedFilter == PosFilter.CATEGORIES && categories.size > 1) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isCatSelected = selectedCategory == cat
                        Surface(
                            onClick = { onCategoryChange(cat) },
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
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PosProductListItem(
    product: Product,
    inCartQty: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.isOutOfStock
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onAddToCart,
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
                Text(
                    product.name.take(2).uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    color = B360Green,
                    fontSize = 16.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) B360Amber else Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onToggleFavorite)
                    )
                }
                Text("SKU: ${product.sku}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                Spacer(Modifier.height(4.dp))
                Text(
                    "KES ${"%,.0f".format(product.sellingPrice)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = B360Green,
                    fontSize = 14.sp
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val stockColor = if (isOutOfStock) B360Red else if (product.isLowStock) B360Amber else Color(0xFF10B981)
                Surface(shape = RoundedCornerShape(8.dp), color = stockColor.copy(alpha = 0.1f)) {
                    Text(
                        text = if (isOutOfStock) "Out" else "${product.currentStock} left",
                        color = stockColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (inCartQty > 0) {
                    Surface(shape = RoundedCornerShape(8.dp), color = B360Green.copy(alpha = 0.1f)) {
                        Text(
                            "$inCartQty in cart",
                            color = B360Green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosProductGridCard(
    product: Product,
    inCartQty: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.isOutOfStock
    val stockColor = if (isOutOfStock) B360Red else if (product.isLowStock) B360Amber else Color(0xFF10B981)

    val cardScale by animateFloatAsState(
        targetValue = if (inCartQty > 0) 1.01f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (inCartQty > 0) B360Green else Color(0xFFF1F5F9),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "cardBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(enabled = !isOutOfStock, onClick = onAddToCart),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            if (inCartQty > 0) 1.5.dp else 1.dp,
            cardBorderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (inCartQty > 0) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Initials Avatar + Stock Badge + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        product.name.take(2).uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        color = B360Green,
                        fontSize = 15.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = stockColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = if (isOutOfStock) "Out" else "${product.currentStock} left",
                            color = stockColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) B360Amber else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Product Name
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.height(36.dp)
            )

            // SKU
            Text(
                text = "SKU: ${product.sku}",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Bottom Row: Price and In-Cart Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KES ${"%,.0f".format(product.sellingPrice)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = B360Green,
                    fontSize = 14.sp
                )

                if (inCartQty > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = B360Green
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text(
                                text = "$inCartQty",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (!isOutOfStock) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosCartSummaryPane(
    cart: SnapshotStateList<MobileCartItem>,
    subtotal: Double,
    tax: Double,
    grandTotal: Double,
    hospitalityEnabled: Boolean,
    serviceType: String,
    onServiceTypeChange: (String) -> Unit,
    tables: List<PosTable>,
    selectedTable: PosTable?,
    onSelectTable: (PosTable?) -> Unit,
    tablesLoading: Boolean,
    tableError: String?,
    onRetryTables: () -> Unit,
    guestCount: String,
    onGuestCountChange: (String) -> Unit,
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onSelectCustomer: (Customer?) -> Unit,
    walkInName: String,
    onWalkInNameChange: (String) -> Unit,
    walkInPhone: String,
    onWalkInPhoneChange: (String) -> Unit,
    paymentMethod: PaymentMethod,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    mpesaConfigs: List<MpesaConfig>,
    mpesaAccountType: String?,
    onMpesaAccountTypeChange: (String?) -> Unit,
    taxesLoaded: Boolean,
    taxError: String?,
    taxRates: List<PosTaxRate>,
    selectedTaxId: String?,
    onSelectTaxId: (String?) -> Unit,
    onRetryTax: () -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    errorMessage: String?,
    onErrorMessage: (String?) -> Unit,
    isCheckingOut: Boolean,
    networkAvailable: Boolean,
    onOpenTab: () -> Unit,
    onCompleteCheckout: () -> Unit,
    onClearCart: (() -> Unit)? = null,
    onCloseSheet: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var tableMenuExpanded by remember { mutableStateOf(false) }
    var customerMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // ── 1. Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = B360Green,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = if (hospitalityEnabled) "Hospitality Order" else "Order Summary",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${cart.sumOf { it.qty }} item${if (cart.sumOf { it.qty } == 1) "" else "s"} in order",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onClearCart != null && cart.isNotEmpty()) {
                    TextButton(onClick = onClearCart) {
                        Text("Clear", color = B360Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (onCloseSheet != null) {
                    IconButton(onClick = onCloseSheet) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9))

        // ── 2. Scrollable Body ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cart Items
            if (cart.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.ShoppingCart,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Your cart is empty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Select products from the catalog to add them to this sale",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cart Items", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                        Text("${cart.sumOf { it.qty }} pcs", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    cart.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.product.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "KES ${"%,.0f".format(item.product.sellingPrice)} × ${item.qty} = KES ${"%,.0f".format(item.product.sellingPrice * item.qty)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val idx = cart.indexOf(item)
                                        if (item.qty > 1) cart[idx] = item.copy(qty = item.qty - 1) else cart.remove(item)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) { Icon(Icons.Filled.Remove, null, modifier = Modifier.size(16.dp)) }
                                AnimatedContent(
                                    targetState = item.qty,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                                        } else {
                                            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                                        }
                                    },
                                    label = "cartQtyTicker"
                                ) { targetQty ->
                                    Text(
                                        targetQty.toString(),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        fontSize = 13.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (item.qty < item.product.currentStock) {
                                            val idx = cart.indexOf(item)
                                            cart[idx] = item.copy(qty = item.qty + 1)
                                        } else {
                                            onErrorMessage("Only ${item.product.currentStock} in stock.")
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
            }

            // Hospitality Service & Table Selection
            if (hospitalityEnabled || tables.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🍽️ Hospitality Service & Table", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))

                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("DINE_IN" to "Dine In", "TAKEAWAY" to "Takeaway", "DELIVERY" to "Delivery").forEach { (type, label) ->
                            val isSel = serviceType == type
                            FilterChip(
                                selected = isSel,
                                onClick = { onServiceTypeChange(type) },
                                label = { Text(label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = B360Green,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (serviceType == "DINE_IN") {
                        if (tablesLoading) Text("Loading tables…", fontSize = 12.sp, color = Color.Gray)
                        tableError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { tableMenuExpanded = true },
                                enabled = !tablesLoading && tableError == null && tables.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                            ) {
                                Text(
                                    selectedTable?.let { "Table: ${it.name} (${it.area})" } ?: "Select table (required)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = tableMenuExpanded,
                                onDismissRequest = { tableMenuExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                tables.forEach { table ->
                                    DropdownMenuItem(
                                        text = { Text("${table.name} · ${table.area} · ${table.capacity} seats · ${table.status}", fontSize = 13.sp) },
                                        onClick = {
                                            onSelectTable(table)
                                            tableMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        if (!tablesLoading && tableError == null && tables.isEmpty()) {
                            Text("No tables configured. Add tables in Hospitality Operations.", fontSize = 12.sp, color = Color(0xFF92400E))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onRetryTables, enabled = !tablesLoading) {
                                Text("Refresh tables", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Guests: ", fontSize = 12.sp, color = Color.Gray)
                                OutlinedTextField(
                                    value = guestCount,
                                    onValueChange = onGuestCountChange,
                                    singleLine = true,
                                    modifier = Modifier.width(76.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Customer Selection
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "Walk-In Customer",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Customer") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = { IconButton(onClick = { customerMenuExpanded = true }) { Icon(Icons.Filled.ArrowDropDown, null) } },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green, unfocusedBorderColor = Color(0xFFE2E8F0))
                )
                DropdownMenu(expanded = customerMenuExpanded, onDismissRequest = { customerMenuExpanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                    DropdownMenuItem(text = { Text("Walk-In Customer") }, onClick = { onSelectCustomer(null); customerMenuExpanded = false })
                    customers.forEach { c ->
                        DropdownMenuItem(text = { Text("${c.name} (${c.phone})") }, onClick = { onSelectCustomer(c); customerMenuExpanded = false })
                    }
                }
            }

            if (selectedCustomer == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = walkInName,
                        onValueChange = onWalkInNameChange,
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green, unfocusedBorderColor = Color(0xFFE2E8F0))
                    )
                    OutlinedTextField(
                        value = walkInPhone,
                        onValueChange = onWalkInPhoneChange,
                        label = { Text("Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green, unfocusedBorderColor = Color(0xFFE2E8F0))
                    )
                }
            }

            // Payment Methods
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(PaymentMethod.CASH, PaymentMethod.MPESA, PaymentMethod.CARD).forEach { pm ->
                        val isSel = paymentMethod == pm
                        Button(
                            onClick = { onPaymentMethodChange(pm) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) B360Green else Color(0xFFF1F5F9),
                                contentColor = if (isSel) Color.White else Color(0xFF334155)
                            )
                        ) {
                            Text(pm.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (paymentMethod == PaymentMethod.MPESA && mpesaConfigs.size > 1) {
                Text("M-Pesa channel", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mpesaConfigs.forEach { config ->
                        val selected = mpesaAccountType == config.accountType
                        FilterChip(
                            selected = selected,
                            onClick = { onMpesaAccountTypeChange(config.accountType) },
                            label = { Text(config.accountType.displayMpesaChannel(), fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sales Tax
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sales tax", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                if (taxError != null) {
                    Text(taxError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    TextButton(onClick = onRetryTax) { Text("Retry tax rates") }
                } else if (!taxesLoaded) {
                    Text("Loading saved tax rates…", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedTaxId == null, onClick = { onSelectTaxId(null) }, label = { Text("No added tax", fontSize = 12.sp) })
                        taxRates.forEach { rate ->
                            FilterChip(
                                selected = selectedTaxId == rate.id,
                                onClick = { onSelectTaxId(rate.id) },
                                label = { Text("${rate.name} (${"%.2f".format(rate.rate * 100)}%)", fontSize = 12.sp) }
                            )
                        }
                    }
                    Text("Select a saved exclusive VAT rate, or no added tax for exempt or tax-inclusive prices.", fontSize = 11.sp, color = Color.Gray)
                }
                Text("Subtotal: KES ${"%,.2f".format(subtotal)} · Added tax: KES ${"%,.2f".format(tax)}", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Sale Notes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green, unfocusedBorderColor = Color(0xFFE2E8F0))
            )
        }

        // ── 3. Sticky Bottom Checkout Bar ──
        Surface(
            color = Color.White,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                errorMessage?.let { msg ->
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = B360Red, modifier = Modifier.size(18.dp))
                            Text(msg, color = B360Red, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Grand Total", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("KES ${"%,.2f".format(grandTotal)}", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = B360Green)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hospitalityEnabled) {
                            OutlinedButton(
                                onClick = onOpenTab,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                                modifier = Modifier.height(48.dp),
                                enabled = !isCheckingOut && cart.isNotEmpty() && networkAvailable
                            ) {
                                Text("Open Tab", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = onCompleteCheckout,
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isCheckingOut && cart.isNotEmpty() && networkAvailable
                        ) {
                            if (isCheckingOut) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("Processing...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                Text(
                                    if (hospitalityEnabled) "Pay & Settle" else if (networkAvailable) "Complete Checkout" else "Reconnect",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
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
fun PosScreen(
    windowWidthSizeClass: WindowWidthSizeClass = LocalWindowWidthSizeClass.current,
    inventoryViewModel: InventoryViewModel = kmpViewModel(),
    customersViewModel: CustomersViewModel = kmpViewModel(),
    businessViewModel: BusinessViewModel = kmpViewModel(),
    createOrderUseCase: CreateOrderUseCase = koinInject(),
    initiatePaymentUseCase: InitiatePaymentUseCase = koinInject()
) {
    SecureScreen()
    val isCompact = windowWidthSizeClass == WindowWidthSizeClass.Compact

    val coroutineScope = rememberCoroutineScope()
    val businessId = remember { UserSession.getBusinessId() }
    val networkAvailable = LocalNetworkAvailable.current
    val client: HttpClient = koinInject()
    var taxRates by remember(businessId) { mutableStateOf<List<PosTaxRate>>(emptyList()) }
    var selectedTaxId by remember(businessId) { mutableStateOf<String?>(null) }
    var taxesLoaded by remember(businessId) { mutableStateOf(false) }
    var taxError by remember(businessId) { mutableStateOf<String?>(null) }
    var taxRetry by remember { mutableIntStateOf(0) }
    LaunchedEffect(businessId, networkAvailable, taxRetry) {
        taxesLoaded = false
        taxError = null
        try {
            val response: ApiResponse<List<PosTaxRate>> = client.get("$BASE_URL/tax/rates").body()
            val taxData = response.data
            if (response.success && taxData != null) {
                taxRates = taxData.filter {
                    it.isActive && it.taxType == "VAT" && !it.isInclusive &&
                        it.appliesTo in listOf("ALL", "PRODUCTS") && it.rate.isFinite() && it.rate in 0.0..1.0
                }
            } else {
                taxRates = emptyList()
            }
            if (taxRates.none { it.id == selectedTaxId }) selectedTaxId = null
            taxesLoaded = true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            taxRates = emptyList()
            taxError = e.message ?: "Could not load tax rates"
            taxesLoaded = true
        }
    }
    val selectedTax = taxRates.find { it.id == selectedTaxId }
    val taxRate = selectedTax?.rate ?: 0.0

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

    val hospitalityEnabled = businessProfileState.profile?.hospitalityEnabled == true || businessProfileState.profile?.type == "HOSPITALITY"
    var serviceType by remember(businessId) { mutableStateOf("DINE_IN") }
    var selectedTableId by remember(businessId) { mutableStateOf<String?>(null) }
    var tables by remember(businessId) { mutableStateOf<List<PosTable>>(emptyList()) }
    var tableError by remember { mutableStateOf<String?>(null) }
    var tablesLoading by remember { mutableStateOf(false) }
    var tableRetry by remember { mutableIntStateOf(0) }
    var guestCount by remember { mutableStateOf("1") }

    LaunchedEffect(hospitalityEnabled, networkAvailable, tableRetry, businessId) {
        tablesLoading = true
        tableError = null
        try {
            val response: ApiResponse<PosHospitalityDashboard> = client.get("$BASE_URL/hospitality").body()
            val data = response.data
            if (response.success && data != null) {
                tables = data.tables.filter { it.mergedIntoTableId == null }
                if (tables.none { it.id == selectedTableId }) selectedTableId = null
            } else if (hospitalityEnabled) {
                tableError = response.message.ifBlank { "Could not load tables" }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (hospitalityEnabled) {
                tableError = e.message ?: "Could not load tables"
            }
        } finally { tablesLoading = false }
    }
    val selectedTable = tables.find { it.id == selectedTableId }

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
    val tax = kotlin.math.round(subtotal * taxRate * 100.0) / 100.0
    val grandTotal = kotlin.math.round((subtotal + tax) * 100.0) / 100.0

    val handleOpenTab: () -> Unit = {
        if (!networkAvailable) {
            errorMessage = "You’re offline. Reconnect before opening a tab."
        } else if (serviceType == "DINE_IN" && (selectedTable == null || tablesLoading || tableError != null)) {
            errorMessage = "Select a table before opening a tab."
        } else if (serviceType == "DINE_IN" && guestCount.toIntOrNull() !in 1..100) {
            errorMessage = "Enter a guest count from 1 to 100."
        } else {
            isCheckingOut = true
            errorMessage = null
            coroutineScope.launch {
                val order = Order(
                    id = generateId(),
                    orderNumber = "B360-TAB-${System.currentTimeMillis() % 10000}",
                    businessId = businessId,
                    customerId = selectedCustomer?.id,
                    customerName = walkInName.ifBlank { "Walk-In Guest" },
                    customerPhone = (selectedCustomer?.phone ?: walkInPhone).trim(),
                    deliveryLocation = if (serviceType == "DINE_IN") selectedTable!!.name else "Takeaway",
                    hospitalityTableId = if (serviceType == "DINE_IN") selectedTableId else null,
                    serviceType = serviceType,
                    guestCount = if (serviceType == "DINE_IN") guestCount.toInt() else 1,
                    items = cart.map { OrderItem(productId = it.product.id, productName = it.product.name, quantity = it.qty, unitPrice = it.product.sellingPrice, buyingPrice = it.product.buyingPrice) },
                    paymentStatus = PaymentStatus.PENDING,
                    deliveryStatus = DeliveryStatus.PENDING,
                    paymentMethod = paymentMethod,
                    notes = notes.ifBlank { "Hospitality Tab" },
                    includeTax = selectedTax != null,
                    taxRate = taxRate,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                createOrderUseCase(order)
                    .onSuccess { saved ->
                        checkoutResult = CheckoutResult(
                            saved.id,
                            saved.orderNumber,
                            paymentMethod,
                            saved.customerPhone.orEmpty(),
                            "Tab #${saved.orderNumber} opened successfully! Settle from Active Tabs.",
                            true,
                            mpesaAccountType
                        )
                        cart.clear()
                        notes = ""
                        selectedTableId = null
                        guestCount = "1"
                        tableRetry++
                        showCartSheet = false
                        inventoryViewModel.loadProducts(businessId)
                    }
                    .onFailure { err -> errorMessage = err.message ?: "Failed to open tab." }
                isCheckingOut = false
            }
        }
    }

    val handleCompleteCheckout: () -> Unit = {
        if (!networkAvailable) {
            errorMessage = "You’re offline. Reconnect before creating an order or collecting payment."
        } else if (hospitalityEnabled && serviceType == "DINE_IN" && (selectedTable == null || tablesLoading || tableError != null)) {
            errorMessage = "Select a table before completing a dine-in order."
        } else if (hospitalityEnabled && serviceType == "DINE_IN" && guestCount.toIntOrNull() !in 1..100) {
            errorMessage = "Enter a guest count from 1 to 100."
        } else {
            val phone = (selectedCustomer?.phone ?: walkInPhone).trim()
            if (paymentMethod == PaymentMethod.MPESA && phone.isBlank()) {
                errorMessage = "Enter the customer's M-Pesa phone number to send payment prompt."
            } else {
                isCheckingOut = true
                errorMessage = null
                coroutineScope.launch {
                    val order = Order(
                        id = generateId(),
                        orderNumber = "B360-POS-${System.currentTimeMillis() % 10000}",
                        businessId = businessId,
                        customerId = selectedCustomer?.id,
                        customerName = walkInName.ifBlank { "Walk-In Customer" },
                        customerPhone = phone,
                        deliveryLocation = if (hospitalityEnabled && serviceType == "DINE_IN") selectedTable!!.name else "In-Store POS",
                        hospitalityTableId = if (hospitalityEnabled && serviceType == "DINE_IN") selectedTableId else null,
                        serviceType = if (hospitalityEnabled) serviceType else "RETAIL",
                        guestCount = if (hospitalityEnabled && serviceType == "DINE_IN") guestCount.toInt() else 1,
                        items = cart.map { OrderItem(productId = it.product.id, productName = it.product.name, quantity = it.qty, unitPrice = it.product.sellingPrice, buyingPrice = it.product.buyingPrice) },
                        paymentStatus = if (paymentMethod == PaymentMethod.CASH) PaymentStatus.PAID else PaymentStatus.PENDING,
                        deliveryStatus = DeliveryStatus.DELIVERED,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        includeTax = selectedTax != null,
                        taxRate = taxRate,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                    createOrderUseCase(order)
                        .onSuccess { saved ->
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
                            selectedTableId = null
                            guestCount = "1"
                            tableRetry++
                            showCartSheet = false
                            inventoryViewModel.loadProducts(businessId)
                        }
                        .onFailure { err -> errorMessage = err.message ?: "Failed to save sale." }
                    isCheckingOut = false
                }
            }
        }
    }

    if (isCompact) {
        // ── Compact Flow: Single List + Bottom Sheet Cart ─────────────────────
        Scaffold(
            containerColor = Color(0xFFF8FAFB),
            topBar = {
                Surface(color = Color(0xFFF8FAFB), shadowElevation = 0.dp) {
                    PosCatalogSearchFilterHeader(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedFilter = selectedFilter,
                        onFilterChange = { selectedFilter = it },
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        hospitalityEnabled = hospitalityEnabled,
                        itemCount = filteredProducts.size,
                        isCompact = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp, bottom = 4.dp)
                    )
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
                        Text(
                            if (hospitalityEnabled) "Checkout / Order (${cart.sumOf { it.qty }}) • KES ${"%,.2f".format(grandTotal)}"
                            else "Checkout (${cart.sumOf { it.qty }}) • KES ${"%,.2f".format(grandTotal)}",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
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
                        PosEmptyProductsView(onClearFilter = {
                            searchQuery = ""
                            selectedFilter = PosFilter.ALL
                            selectedCategory = "All"
                        })
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { p ->
                                PosProductListItem(
                                    product = p,
                                    inCartQty = cart.find { it.product.id == p.id }?.qty ?: 0,
                                    isFavorite = favoriteProductIds.contains(p.id),
                                    onToggleFavorite = {
                                        if (favoriteProductIds.contains(p.id)) favoriteProductIds.remove(p.id)
                                        else favoriteProductIds.add(p.id)
                                    },
                                    onAddToCart = {
                                        if (!p.isOutOfStock) {
                                            val existing = cart.find { it.product.id == p.id }
                                            if (existing != null) {
                                                if (existing.qty < p.currentStock) {
                                                    val idx = cart.indexOf(existing)
                                                    cart[idx] = existing.copy(qty = existing.qty + 1)
                                                } else errorMessage = "Only ${p.currentStock} in stock."
                                            } else cart.add(MobileCartItem(p, 1))
                                        }
                                    }
                                )
                            }
                            item { Spacer(Modifier.height(84.dp)) }
                        }
                    }
                }
            }
        }

        // Cart Bottom Sheet for Compact
        if (showCartSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCartSheet = false },
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                PosCartSummaryPane(
                    cart = cart,
                    subtotal = subtotal,
                    tax = tax,
                    grandTotal = grandTotal,
                    hospitalityEnabled = hospitalityEnabled,
                    serviceType = serviceType,
                    onServiceTypeChange = { serviceType = it },
                    tables = tables,
                    selectedTable = selectedTable,
                    onSelectTable = { selectedTableId = it?.id },
                    tablesLoading = tablesLoading,
                    tableError = tableError,
                    onRetryTables = { tableRetry++ },
                    guestCount = guestCount,
                    onGuestCountChange = { guestCount = it.filter(Char::isDigit).take(3) },
                    customers = customersState.customers,
                    selectedCustomer = selectedCustomer,
                    onSelectCustomer = {
                        selectedCustomer = it
                        if (it != null) {
                            walkInName = it.name
                            walkInPhone = it.phone
                        } else {
                            walkInName = "Walk-In Customer"
                            walkInPhone = ""
                        }
                    },
                    walkInName = walkInName,
                    onWalkInNameChange = { walkInName = it },
                    walkInPhone = walkInPhone,
                    onWalkInPhoneChange = { walkInPhone = it },
                    paymentMethod = paymentMethod,
                    onPaymentMethodChange = { paymentMethod = it },
                    mpesaConfigs = mpesaState.configs,
                    mpesaAccountType = mpesaAccountType,
                    onMpesaAccountTypeChange = { mpesaAccountType = it },
                    taxesLoaded = taxesLoaded,
                    taxError = taxError,
                    taxRates = taxRates,
                    selectedTaxId = selectedTaxId,
                    onSelectTaxId = { selectedTaxId = it },
                    onRetryTax = { taxRetry++ },
                    notes = notes,
                    onNotesChange = { notes = it },
                    errorMessage = errorMessage,
                    onErrorMessage = { errorMessage = it },
                    isCheckingOut = isCheckingOut,
                    networkAvailable = networkAvailable,
                    onOpenTab = handleOpenTab,
                    onCompleteCheckout = handleCompleteCheckout,
                    onClearCart = { cart.clear() },
                    onCloseSheet = { showCartSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                )
            }
        }
    } else {
        // ── Medium / Expanded: Split-Pane POS Screen ─────────────────────────
        // Left 65%: Grid of product cards with search & category tabs.
        // Right 35%: Permanent cart summary with live totals, table selector, and checkout buttons.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFB))
        ) {
            // Left 65%: Grid of product cards with search & category tabs
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .background(Color(0xFFF8FAFB))
            ) {
                PosCatalogSearchFilterHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    hospitalityEnabled = hospitalityEnabled,
                    itemCount = filteredProducts.size,
                    isCompact = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when {
                        inventoryState.isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = B360Green)
                            }
                        }
                        filteredProducts.isEmpty() -> {
                            PosEmptyProductsView(onClearFilter = {
                                searchQuery = ""
                                selectedFilter = PosFilter.ALL
                                selectedCategory = "All"
                            })
                        }
                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredProducts, key = { it.id }) { product ->
                                    PosProductGridCard(
                                        product = product,
                                        inCartQty = cart.find { it.product.id == product.id }?.qty ?: 0,
                                        isFavorite = favoriteProductIds.contains(product.id),
                                        onToggleFavorite = {
                                            if (favoriteProductIds.contains(product.id)) favoriteProductIds.remove(product.id)
                                            else favoriteProductIds.add(product.id)
                                        },
                                        onAddToCart = {
                                            if (!product.isOutOfStock) {
                                                val existing = cart.find { it.product.id == product.id }
                                                if (existing != null) {
                                                    if (existing.qty < product.currentStock) {
                                                        val idx = cart.indexOf(existing)
                                                        cart[idx] = existing.copy(qty = existing.qty + 1)
                                                    } else errorMessage = "Only ${product.currentStock} in stock."
                                                } else cart.add(MobileCartItem(product, 1))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            VerticalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

            // Right 35%: Permanent cart summary with live totals, table selector, and checkout buttons
            Surface(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                PosCartSummaryPane(
                    cart = cart,
                    subtotal = subtotal,
                    tax = tax,
                    grandTotal = grandTotal,
                    hospitalityEnabled = hospitalityEnabled,
                    serviceType = serviceType,
                    onServiceTypeChange = { serviceType = it },
                    tables = tables,
                    selectedTable = selectedTable,
                    onSelectTable = { selectedTableId = it?.id },
                    tablesLoading = tablesLoading,
                    tableError = tableError,
                    onRetryTables = { tableRetry++ },
                    guestCount = guestCount,
                    onGuestCountChange = { guestCount = it.filter(Char::isDigit).take(3) },
                    customers = customersState.customers,
                    selectedCustomer = selectedCustomer,
                    onSelectCustomer = {
                        selectedCustomer = it
                        if (it != null) {
                            walkInName = it.name
                            walkInPhone = it.phone
                        } else {
                            walkInName = "Walk-In Customer"
                            walkInPhone = ""
                        }
                    },
                    walkInName = walkInName,
                    onWalkInNameChange = { walkInName = it },
                    walkInPhone = walkInPhone,
                    onWalkInPhoneChange = { walkInPhone = it },
                    paymentMethod = paymentMethod,
                    onPaymentMethodChange = { paymentMethod = it },
                    mpesaConfigs = mpesaState.configs,
                    mpesaAccountType = mpesaAccountType,
                    onMpesaAccountTypeChange = { mpesaAccountType = it },
                    taxesLoaded = taxesLoaded,
                    taxError = taxError,
                    taxRates = taxRates,
                    selectedTaxId = selectedTaxId,
                    onSelectTaxId = { selectedTaxId = it },
                    onRetryTax = { taxRetry++ },
                    notes = notes,
                    onNotesChange = { notes = it },
                    errorMessage = errorMessage,
                    onErrorMessage = { errorMessage = it },
                    isCheckingOut = isCheckingOut,
                    networkAvailable = networkAvailable,
                    onOpenTab = handleOpenTab,
                    onCompleteCheckout = handleCompleteCheckout,
                    onClearCart = { cart.clear() },
                    onCloseSheet = null,
                    modifier = Modifier.fillMaxSize()
                )
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
                val checkScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "checkScale"
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        if (result.paymentPromptAccepted || result.paymentMethod == PaymentMethod.CASH) Icons.Filled.CheckCircle else Icons.Filled.Pending,
                        null,
                        tint = if (result.paymentPromptAccepted || result.paymentMethod == PaymentMethod.CASH) B360Green else Color(0xFFF59E0B),
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer {
                                scaleX = checkScale
                                scaleY = checkScale
                            }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Order Number: ${result.orderNumber}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    Text(result.paymentMessage, textAlign = TextAlign.Center, color = Color(0xFF64748B))
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
}

private fun String.displayMpesaChannel(): String =
    lowercase().replaceFirstChar { it.uppercase() }
