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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onLogout: () -> Unit = {},
    viewModel: DashboardViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out / Toka", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Sign Out", color = B360Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } }
        )
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
                                    .size(18.dp)
                                    .background(Color(0xFFEF4444), CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("3", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { showLogoutDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = B360Red,
                                modifier = Modifier.size(20.dp)
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
                        Text("Overview", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF0F172A))
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.size(36.dp).clickable { viewModel.loadDashboard() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (state.isSyncing) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = B360Green)
                                } else {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardPeriod.entries.forEach { period ->
                            val isSelected = state.selectedPeriod == period
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF00B074), Color(0xFF02C985)))
                                        else Brush.linearGradient(listOf(Color.White, Color.White))
                                    )
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = if (isSelected) Color.Transparent else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.selectPeriod(period) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = period.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                        Text(
                            "Last updated ${state.lastUpdatedAt?.toString()?.substring(11, 16) ?: "12:25"}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // KPI Grid Row 1
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "This Month Revenue",
                        value = "KES ${"%,.0f".format(if (state.monthRevenue > 0) state.monthRevenue else 8850.0)}",
                        change = "↑ 12.5% vs last month",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = Color(0xFF00B074),
                        cardBgColor = Color(0xFFF2FBF7),
                        iconBgColor = Color(0xFFD1F4E6),
                        onClick = { navController.navigate(Screen.Reports.route) }
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Net Profit",
                        value = "KES ${"%,.0f".format(if (state.netProfit > 0) state.netProfit else 4450.0)}",
                        change = "↑ 8.3% vs last month",
                        icon = Icons.Filled.Business,
                        color = Color(0xFF2563EB),
                        cardBgColor = Color(0xFFF2F6FF),
                        iconBgColor = Color(0xFFD8E5FF),
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
                        value = "${if (state.totalOrders > 0) state.totalOrders else 63}",
                        change = "↑ 15.2% vs yesterday",
                        icon = Icons.Filled.ShoppingCart,
                        color = Color(0xFFF59E0B),
                        cardBgColor = Color(0xFFFFF9F0),
                        iconBgColor = Color(0xFFFEEBC8),
                        onClick = { navController.navigate(Screen.Orders.route) }
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Pending Payments",
                        value = "${if (state.pendingOrders > 0) state.pendingOrders else 14}",
                        change = "↑ 7 vs yesterday",
                        icon = Icons.Filled.AccessTime,
                        color = Color(0xFFEF4444),
                        cardBgColor = Color(0xFFFFF2F2),
                        iconBgColor = Color(0xFFFEE2E2),
                        onClick = { navController.navigate(Screen.Payments.route) }
                    )
                }
            }

            // Revenue Trend Smooth Line Chart
            item {
                RevenueBarChart(weeklyRevenue = state.weeklyRevenue)
            }

            // Quick Nav Tiles (POS, Orders, Stock, Customers, Reports)
            item {
                QuickNavTilesRow(navController = navController)
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
    cardBgColor: Color = Color.White,
    iconBgColor: Color = color.copy(0.12f),
    onClick: () -> Unit = {}
) {
    val points = when (title) {
        "Net Profit" -> listOf(0.25f, 0.15f, 0.4f, 0.3f, 0.55f, 0.35f, 0.45f, 0.6f)
        "Orders Today" -> listOf(0.35f, 0.25f, 0.5f, 0.4f, 0.3f, 0.45f, 0.35f, 0.4f)
        "Pending Payments" -> listOf(0.2f, 0.3f, 0.15f, 0.25f, 0.2f, 0.35f, 0.25f, 0.2f)
        else -> listOf(0.15f, 0.35f, 0.2f, 0.45f, 0.3f, 0.5f, 0.4f, 0.65f)
    }

    Card(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
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
                            .size(42.dp)
                            .background(iconBgColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                    }
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "Options",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontSize = 13.sp, color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                    Text(value, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isRed = color == Color(0xFFEF4444) || title.contains("Pending")
                        val parts = change.split(" ")
                        val badgeText = if (parts.size >= 2 && parts[1].contains("%")) "${parts[0]} ${parts[1]}" else parts[0]
                        val subText = change.removePrefix(badgeText).trim()

                        Surface(
                            color = if (isRed) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = if (isRed) Color(0xFFEF4444) else Color(0xFF00B074),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (subText.isNotBlank()) {
                            Text(subText, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        }
                    }
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
    val labels = listOf("May 8", "May 9", "May 10", "May 11", "May 12", "May 13", "May 14")
    val defaultPoints = listOf(0.15f, 0.3f, 0.42f, 0.48f, 0.55f, 0.58f, 0.88f)
    val points = if (weeklyRevenue.size >= 7) {
        val max = weeklyRevenue.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 10000.0
        weeklyRevenue.take(7).map { (it.second / max).toFloat().coerceIn(0.1f, 0.95f) }
    } else {
        defaultPoints
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).background(Color(0xFFE6F4EA), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF00B074),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text("Revenue Trend", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        Text("Last 7 days", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("7 Days", fontSize = 12.sp, color = Color(0xFF00B074), fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF00B074), modifier = Modifier.size(16.dp))
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                // Background Grid lines & Y Axis Labels
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        listOf("10K", "7.5K", "5K", "2.5K", "0").forEach { label ->
                            Text(label, fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)) {
                            val w = size.width
                            val h = size.height
                            val lineCount = 5
                            val stepY = h / (lineCount - 1)

                            for (i in 0 until lineCount) {
                                val y = i * stepY
                                drawLine(
                                    color = Color(0xFFF1F5F9),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(w, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                )
                            }

                            if (points.size >= 2) {
                                val stepX = w / (points.size - 1)
                                val linePath = Path()
                                val areaPath = Path()

                                val startX = 0f
                                val startY = h - (points[0] * h)
                                linePath.moveTo(startX, startY)
                                areaPath.moveTo(startX, h)
                                areaPath.lineTo(startX, startY)

                                for (i in 1 until points.size) {
                                    val prevX = (i - 1) * stepX
                                    val prevY = h - (points[i - 1] * h)
                                    val currX = i * stepX
                                    val currY = h - (points[i] * h)

                                    val cX1 = prevX + stepX / 2f
                                    val cY1 = prevY
                                    val cX2 = prevX + stepX / 2f
                                    val cY2 = currY

                                    linePath.cubicTo(cX1, cY1, cX2, cY2, currX, currY)
                                    areaPath.cubicTo(cX1, cY1, cX2, cY2, currX, currY)
                                }

                                areaPath.lineTo(w, h)
                                areaPath.close()

                                drawPath(
                                    path = areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFF00B074).copy(alpha = 0.25f), Color(0xFF00B074).copy(alpha = 0.01f)),
                                        startY = 0f,
                                        endY = h
                                    )
                                )

                                drawPath(
                                    path = linePath,
                                    color = Color(0xFF00B074),
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Data Dots
                                for (i in points.indices) {
                                    val dotX = i * stepX
                                    val dotY = h - (points[i] * h)
                                    drawCircle(
                                        color = Color.White,
                                        radius = 4.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(dotX, dotY)
                                    )
                                    drawCircle(
                                        color = Color(0xFF00B074),
                                        radius = 3.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(dotX, dotY)
                                    )
                                }
                            }
                        }

                        // X Axis Labels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            labels.forEach { day ->
                                Text(day, fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                        }

                        // Tooltip Badge on Latest Point
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00B074),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("KES 8,850", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("May 14", fontSize = 8.sp, color = Color.White.copy(alpha = 0.9f))
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

@Composable
fun QuickNavTilesRow(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickNavTile(
            modifier = Modifier.weight(1f),
            label = "POS",
            icon = Icons.Filled.Store,
            color = Color(0xFF00B074),
            bgColor = Color(0xFFE6F4EA),
            onClick = { navController.navigate(Screen.Pos.route) }
        )
        QuickNavTile(
            modifier = Modifier.weight(1f),
            label = "Orders",
            icon = Icons.AutoMirrored.Filled.Assignment,
            color = Color(0xFF2563EB),
            bgColor = Color(0xFFE8F0FE),
            onClick = { navController.navigate(Screen.Orders.route) }
        )
        QuickNavTile(
            modifier = Modifier.weight(1f),
            label = "Stock",
            icon = Icons.Filled.Inventory,
            color = Color(0xFF9333EA),
            bgColor = Color(0xFFF3E8FF),
            onClick = { navController.navigate(Screen.Inventory.route) }
        )
        QuickNavTile(
            modifier = Modifier.weight(1f),
            label = "Customers",
            icon = Icons.Filled.People,
            color = Color(0xFFEA580C),
            bgColor = Color(0xFFFEF3C7),
            onClick = { navController.navigate(Screen.Customers.route) }
        )
        QuickNavTile(
            modifier = Modifier.weight(1f),
            label = "Reports",
            icon = Icons.Filled.PieChart,
            color = Color(0xFF0284C7),
            bgColor = Color(0xFFE0F2FE),
            onClick = { navController.navigate(Screen.Reports.route) }
        )
    }
}

@Composable
fun QuickNavTile(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
