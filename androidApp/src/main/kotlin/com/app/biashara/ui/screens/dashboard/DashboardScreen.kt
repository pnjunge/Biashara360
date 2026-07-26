package com.app.biashara.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.app.biashara.domain.model.Order
import com.app.biashara.domain.model.Product
import com.app.biashara.presentation.viewmodel.DashboardViewModel
import com.app.biashara.presentation.viewmodel.DashboardPeriod
import com.app.biashara.ui.navigation.Screen
import com.app.biashara.ui.theme.*
import com.app.biashara.ui.kmpViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.app.biashara.UserSession
import com.app.biashara.domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    val currentUser by UserSession.currentUser.collectAsState()
    val greeting = if (state.userName.isNotBlank()) "Habari, ${state.userName.split(" ").first()}! 👋"
    else "Habari! 👋"
    val roleDisplay = when (currentUser?.role) {
        UserRole.SUPERADMIN -> "Super Administrator"
        UserRole.ADMIN -> "System Administrator"
        UserRole.STAFF -> "Staff Member"
        UserRole.VIEWER -> "Viewer"
        null -> "System Administrator"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greeting,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = roleDisplay,
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { navController.navigate(Screen.Orders.route) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(22.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(B360Green, CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-10).dp, y = 10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { navController.navigate(Screen.Settings.route) }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        }
    ) { padding ->
        if (state.isLoading && state.recentOrders.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        if (state.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = B360Red, modifier = Modifier.size(48.dp))
                    Text(state.error!!, color = Color.Gray, fontSize = 14.sp)
                    Button(onClick = { viewModel.dismissError(); viewModel.loadDashboard() },
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                        Text("Retry")
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(B360Surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(
                            onClick = { viewModel.loadDashboard() },
                            enabled = !state.isSyncing
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh dashboard")
                            }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DashboardPeriod.entries.size) { index ->
                            val period = DashboardPeriod.entries[index]
                            FilterChip(
                                selected = state.selectedPeriod == period,
                                onClick = { viewModel.selectPeriod(period) },
                                label = { Text(period.label) }
                            )
                        }
                    }
                    state.lastUpdatedAt?.let {
                        Text(
                            "Last updated ${it.toString().substring(11, 16)}",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // KPI Grid Row 1
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "${state.selectedPeriod.label} Revenue",
                        value = "KES ${"%,.0f".format(state.monthRevenue)}",
                        change = "↑ 12% from last month",
                        icon = Icons.Filled.TrendingUp,
                        color = B360Green,
                        bgColor = B360Green.copy(0.12f),
                        onClick = { navController.navigate(Screen.Reports.route) }
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Net Profit",
                        value = "KES ${"%,.0f".format(state.netProfit)}",
                        change = "↑ 8% from last month",
                        icon = Icons.Filled.Business,
                        color = B360Blue,
                        bgColor = B360Blue.copy(0.12f),
                        onClick = { navController.navigate(Screen.Reports.route) }
                    )
                }
            }

            // KPI Grid Row 2
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Orders Today",
                        value = "${state.totalOrders}",
                        change = "↑ 3 from yesterday",
                        icon = Icons.Filled.ShoppingCart,
                        color = B360Amber,
                        bgColor = B360Amber.copy(0.12f),
                        onClick = { navController.navigate(Screen.Orders.route) }
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending Payments",
                        value = "${state.pendingOrders}",
                        change = "orders pending",
                        icon = Icons.Filled.AccessTime,
                        color = B360Red,
                        bgColor = B360Red.copy(0.12f),
                        onClick = { navController.navigate(Screen.Payments.route) }
                    )
                }
            }

            // Revenue Trend Bar Chart
            item {
                RevenueBarChart(weeklyRevenue = state.weeklyRevenue)
            }

            // Quick Actions
            item {
                Column {
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Filled.Add,
                            label = "New Order",
                            iconColor = B360Green,
                            style = QuickActionStyle.FILLED,
                            modifier = Modifier.weight(1f)
                        ) {
                            navController.navigate(Screen.CreateOrder.route)
                        }
                        QuickActionCard(
                            icon = Icons.Filled.LocalOffer,
                            label = "Add Product",
                            iconColor = B360Blue,
                            modifier = Modifier.weight(1f)
                        ) {
                            navController.navigate(Screen.AddProduct.createRoute())
                        }
                        QuickActionCard(
                            icon = Icons.Filled.MoveToInbox,
                            label = "Stock In",
                            iconColor = B360Amber,
                            modifier = Modifier.weight(1f)
                        ) {
                            navController.navigate(Screen.Inventory.route)
                        }
                        QuickActionCard(
                            icon = Icons.Filled.BarChart,
                            label = "Sales Report",
                            iconColor = Color(0xFF7B1FA2),
                            modifier = Modifier.weight(1f)
                        ) {
                            navController.navigate(Screen.Reports.route)
                        }
                    }
                }
            }

            // Quick Alerts Section
            item {
                QuickAlertsSection(
                    lowStockCount = state.lowStockCount,
                    pendingOrdersCount = state.pendingOrders
                )
            }

            // Recent Orders Section
            if (state.recentOrders.isNotEmpty()) {
                item {
                    RecentOrdersSection(
                        orders = state.recentOrders,
                        onViewAll = { navController.navigate(Screen.Orders.route) },
                        onOrderClick = { id -> navController.navigate(Screen.OrderDetail.createRoute(id)) }
                    )
                }
            }

            // Top Customers Section
            item {
                TopCustomersSection(
                    customers = state.topCustomers,
                    onViewAll = { navController.navigate(Screen.Customers.route) }
                )
            }
        }
    }
}

@Composable
fun WavyLineChart(
    color: Color,
    points: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width == 0f || height == 0f || points.size < 2) return@Canvas

        val path = Path()
        val fillPath = Path()

        val stepX = width / (points.size - 1)
        
        val startX = 0f
        val startY = height - (points[0] * height)
        path.moveTo(startX, startY)
        fillPath.moveTo(startX, height)
        fillPath.lineTo(startX, startY)

        for (i in 1 until points.size) {
            val prevX = (i - 1) * stepX
            val prevY = height - (points[i - 1] * height)
            val currentX = i * stepX
            val currentY = height - (points[i] * height)

            val controlX1 = prevX + stepX / 2f
            val controlY1 = prevY
            val controlX2 = prevX + stepX / 2f
            val controlY2 = currentY

            path.cubicTo(
                controlX1, controlY1,
                controlX2, controlY2,
                currentX, currentY
            )
            fillPath.cubicTo(
                controlX1, controlY1,
                controlX2, controlY2,
                currentX, currentY
            )
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0.0f)),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    change: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit = {}
) {
    val points = when (title) {
        "Net Profit" -> listOf(0.25f, 0.15f, 0.4f, 0.3f, 0.55f, 0.35f, 0.45f, 0.6f)
        "Orders Today" -> listOf(0.35f, 0.25f, 0.5f, 0.4f, 0.3f, 0.45f, 0.35f, 0.4f)
        "Pending Payments" -> listOf(0.2f, 0.3f, 0.15f, 0.25f, 0.2f, 0.35f, 0.25f, 0.2f)
        else -> listOf(0.15f, 0.35f, 0.2f, 0.45f, 0.3f, 0.5f, 0.4f, 0.65f)
    }

    Card(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            WavyLineChart(
                color = color,
                points = points,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(48.dp)
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(bgColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    
                    val annotatedChange = buildAnnotatedString {
                        val parts = change.split(" ")
                        if (parts.isNotEmpty()) {
                            val firstPart = parts[0]
                            if (firstPart.startsWith("↑") || firstPart.startsWith("↓") || firstPart.any { it.isDigit() }) {
                                val percentPart = if (parts.size > 1 && parts[1].contains("%")) "${parts[0]} ${parts[1]}" else parts[0]
                                withStyle(SpanStyle(color = B360Green, fontWeight = FontWeight.Bold)) {
                                    append(percentPart)
                                }
                                append(" ")
                                val rest = parts.drop(if (parts.size > 1 && parts[1].contains("%")) 2 else 1).joinToString(" ")
                                withStyle(SpanStyle(color = Color(0xFF64748B))) {
                                    append(rest)
                                }
                            } else if (change == "orders pending") {
                                withStyle(SpanStyle(color = B360Green, fontWeight = FontWeight.Bold)) {
                                    append("orders pending")
                                }
                            } else {
                                withStyle(SpanStyle(color = B360Green, fontWeight = FontWeight.Bold)) {
                                    append(change)
                                }
                            }
                        }
                    }
                    Text(annotatedChange, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun RevenueBarChart(
    modifier: Modifier = Modifier,
    weeklyRevenue: List<Pair<String, Double>> = emptyList()
) {
    // Normalise data to [0,1] fractions for bar heights
    val maxRevenue = weeklyRevenue.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    val barData = if (weeklyRevenue.isEmpty()) {
        // Fallback pattern when no data yet — shows subtle empty state
        listOf("Mon" to 0f, "Tue" to 0f, "Wed" to 0f, "Thu" to 0f, "Fri" to 0f, "Sat" to 0f, "Sun" to 0f)
    } else {
        weeklyRevenue.map { (day, rev) -> day to (rev / maxRevenue).toFloat().coerceIn(0.02f, 1f) }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = B360Green,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("Revenue Trend", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("Last 7 days", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("7 Days", fontSize = 12.sp, color = B360Green, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ArrowDropDown, null, tint = B360Green, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Background grid lines Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 40.dp, bottom = 20.dp)
                ) {
                    val height = size.height
                    val width = size.width
                    val lineCount = 6
                    val stepY = height / (lineCount - 1)
                    
                    for (i in 0 until lineCount) {
                        val y = i * stepY
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Y Axis
                    Column(
                        modifier = Modifier
                            .width(40.dp)
                            .fillMaxHeight()
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        listOf("500K", "400K", "300K", "200K", "100K", "0").forEach { label ->
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }

                    // Bars & Labels
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        barData.forEach { (day, fraction) ->
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .fillMaxWidth(0.4f)
                                        .fillMaxHeight(fraction * 0.9f)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(B360Green, B360Green.copy(alpha = 0.2f))
                                            ),
                                            shape = RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(day, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertCard(
    message: String,
    icon: ImageVector,
    tint: Color,
    bgColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        }
        Text(
            message,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
        )
    }
}

@Composable
fun QuickAlertsSection(
    lowStockCount: Int,
    pendingOrdersCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Quick Alerts", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            
            AlertCard(
                message = "$lowStockCount products low stock",
                icon = Icons.Filled.Warning,
                tint = B360Amber,
                bgColor = B360Amber.copy(0.12f)
            )
            AlertCard(
                message = "$pendingOrdersCount unpaid orders",
                icon = Icons.Filled.AccessTime,
                tint = B360Red,
                bgColor = B360Red.copy(0.12f)
            )
            AlertCard(
                message = "5 new customers this week",
                icon = Icons.Filled.People,
                tint = B360Green,
                bgColor = B360Green.copy(0.12f)
            )
            AlertCard(
                message = "Mpesa: 2 unreconciled",
                icon = Icons.Filled.Sync,
                tint = B360Blue,
                bgColor = B360Blue.copy(0.12f)
            )
        }
    }
}

enum class QuickActionStyle { PLAIN, FILLED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier,
    style: QuickActionStyle = QuickActionStyle.PLAIN,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (style == QuickActionStyle.FILLED) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
fun RecentOrdersSection(orders: List<Order>, onViewAll: () -> Unit, onOrderClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Orders", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                TextButton(onClick = onViewAll) { Text("View All", color = B360Green) }
            }
            orders.take(5).forEachIndexed { index, order ->
                Surface(onClick = { onOrderClick(order.id) }, color = Color.Transparent) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(order.orderNumber, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(order.customerName, color = Color.Gray, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("KES ${"%,.0f".format(order.subtotal)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(color = paymentStatusColor(order.paymentStatus.name).copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                                Text(
                                    order.paymentStatus.displayLabel(),
                                    color = paymentStatusColor(order.paymentStatus.name),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                if (index < orders.size - 1 && index < 4) Divider(color = Color(0xFFF1F5F9))
            }
        }
    }
}

@Composable
fun TopCustomersSection(
    customers: List<Pair<com.app.biashara.domain.model.Customer, com.app.biashara.domain.model.CustomerStats>>,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Top Customers", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                TextButton(onClick = onViewAll) {
                    Text("View All", color = B360Green)
                }
            }
            
            if (customers.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No customer data yet", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                customers.take(4).forEachIndexed { index, (customer, stats) ->
                    val initials = customer.name.split(" ").map { it.firstOrNull()?.toString() ?: "" }.joinToString("").uppercase()
                    val spent = stats.totalSpent
                    val orders = stats.totalOrders
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).background(B360Blue.copy(0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initials, color = B360Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text(customer.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$orders ${if (orders == 1) "order" else "orders"}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Text("KES ${"%,.0f".format(spent)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (index < customers.size - 1 && index < 3) {
                        Divider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}
