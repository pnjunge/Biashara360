package com.app.biashara.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import java.io.File
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.app.biashara.ui.theme.*
import com.app.biashara.UserSession
import com.app.biashara.presentation.viewmodel.*
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.generateId
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.app.biashara.ui.AppScreen
import com.app.biashara.ui.DesktopNavigationViewModel
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.input.key.*

inline fun <reified T : Any> inject(): T = org.koin.core.context.GlobalContext.get().get()

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String
) {
    var dateRangeLabel by remember { mutableStateOf("Mar 1 – Mar 31, 2025") }
    var showDatePickerDropdown by remember { mutableStateOf(false) }

    var filterLabel by remember { mutableStateOf("Filter") }
    var showFilterDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E293B)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Picker Card
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    color = Color.White,
                    modifier = Modifier.clickable { showDatePickerDropdown = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Date Range",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = dateRangeLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showDatePickerDropdown,
                    onDismissRequest = { showDatePickerDropdown = false }
                ) {
                    val ranges = listOf("Today", "Yesterday", "Last 7 Days", "Last 30 Days", "This Month", "Last Month")
                    ranges.forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range) },
                            onClick = {
                                dateRangeLabel = when (range) {
                                    "Today" -> "Today"
                                    "Yesterday" -> "Yesterday"
                                    "Last 7 Days" -> "Last 7 Days"
                                    "Last 30 Days" -> "Last 30 Days"
                                    "This Month" -> "This Month"
                                    "Last Month" -> "Last Month"
                                    else -> range
                                }
                                showDatePickerDropdown = false
                            }
                        )
                    }
                }
            }

            // Filter Card
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    color = Color.White,
                    modifier = Modifier.clickable { showFilterDropdown = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = filterLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = { showFilterDropdown = false }
                ) {
                    val filters = listOf("All Channels", "Online Store", "POS Terminal", "Mobile App", "Wholesale")
                    filters.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter) },
                            onClick = {
                                filterLabel = filter
                                showFilterDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomActionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color = B360Green,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE6F7F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun DonutChart(
    modifier: Modifier = Modifier,
    slices: List<Pair<Float, Color>>,
    centerText: String,
    centerSubtext: String
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val size = size.minDimension
            val strokeWidth = 24.dp.toPx()
            val radius = (size - strokeWidth) / 2
            
            var startAngle = -90f
            slices.forEach { (value, color) ->
                val sweepAngle = value * 360f
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset((this.size.width - radius * 2) / 2, (this.size.height - radius * 2) / 2),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
            Text(centerSubtext, fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun DonutLegendRow(
    color: Color,
    category: String,
    percentage: String,
    amount: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(category, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(percentage, fontSize = 13.sp, color = Color(0xFF64748B))
            Text(amount, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun RevenueExpenseLineChart(
    modifier: Modifier = Modifier
) {
    // Sample revenue data (rising trend) and expense data
    val revenuePoints = listOf(0.55f, 0.60f, 0.58f, 0.72f, 0.80f, 0.75f, 0.90f, 0.85f, 0.78f, 0.88f,
        0.92f, 0.87f, 0.95f, 0.90f, 0.82f, 0.78f, 0.72f, 0.80f, 0.76f, 0.70f,
        0.65f, 0.68f, 0.74f, 0.78f, 0.72f, 0.65f, 0.60f, 0.72f, 0.82f, 0.88f)
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 40.dp.toPx()
        val paddingBottom = 24.dp.toPx()
        val paddingTop = 12.dp.toPx()
        val paddingRight = 12.dp.toPx()
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        
        // Draw grid lines
        val yLines = 6
        for (i in 0 until yLines) {
            val y = paddingTop + chartHeight * i / (yLines - 1)
            drawLine(
                color = Color(0xFFF1F5F9),
                start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Data points (Expenses)
        val expensePoints = listOf(0.2f, 0.25f, 0.22f, 0.35f, 0.45f, 0.42f, 0.58f, 0.52f, 0.48f, 0.65f,
            0.72f, 0.68f, 0.92f, 0.85f, 0.78f, 0.65f, 0.55f, 0.60f, 0.52f, 0.48f,
            0.42f, 0.38f, 0.45f, 0.52f, 0.48f, 0.38f, 0.32f, 0.45f, 0.58f, 0.52f)
        val xStep = chartWidth / (expensePoints.size - 1)
        
        // ── Draw Revenue line (Green) ──
        val revPath = androidx.compose.ui.graphics.Path()
        val revFillPath = androidx.compose.ui.graphics.Path()
        revenuePoints.forEachIndexed { index, value ->
            val x = paddingLeft + index * xStep
            val y = paddingTop + chartHeight * (1f - value * 0.9f)
            if (index == 0) {
                revPath.moveTo(x, y)
                revFillPath.moveTo(x, paddingTop + chartHeight)
                revFillPath.lineTo(x, y)
            } else {
                revPath.lineTo(x, y)
                revFillPath.lineTo(x, y)
            }
            if (index == revenuePoints.size - 1) {
                revFillPath.lineTo(x, paddingTop + chartHeight)
                revFillPath.close()
            }
        }
        drawPath(
            path = revFillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(B360Green.copy(alpha = 0.12f), Color.Transparent),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )
        drawPath(
            path = revPath,
            color = B360Green,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
        // Revenue dot markers
        listOf(0, 5, 10, 15, 20, 25, 29).forEach { index ->
            val x = paddingLeft + index * xStep
            val y = paddingTop + chartHeight * (1f - revenuePoints[index] * 0.9f)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
            drawCircle(color = B360Green, radius = 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
        }
        
        // ── Draw Expenses line (Red) ──
        val path = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()
        
        expensePoints.forEachIndexed { index, value ->
            val x = paddingLeft + index * xStep
            val y = paddingTop + chartHeight * (1f - value * 0.9f)
            
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, paddingTop + chartHeight)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (index == expensePoints.size - 1) {
                fillPath.lineTo(x, paddingTop + chartHeight)
                fillPath.close()
            }
        }
        
        // Draw fill under Expenses
        drawPath(
            path = fillPath,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(B360Red.copy(alpha = 0.15f), Color.Transparent),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )
        
        // Draw expense line
        drawPath(
            path = path,
            color = B360Red,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
        
        // Expense dot markers (every 5th point)
        expensePoints.forEachIndexed { index, value ->
            if (index % 5 == 0 || index == expensePoints.size - 1) {
                val x = paddingLeft + index * xStep
                val y = paddingTop + chartHeight * (1f - value * 0.9f)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                drawCircle(color = B360Red, radius = 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
    }
}

// ─── Dashboard ────────────────────────────────────────────────────────────────


@Composable
fun DesktopDashboardScreen(
    viewModel: DashboardViewModel = remember { inject() },
    navigationViewModel: com.app.biashara.ui.DesktopNavigationViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 24.dp, top = 24.dp, end = 34.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // Screen Header
        ScreenHeader(
            title = "Dashboard",
            subtitle = "Welcome back! Here's what's happening with your business today."
        )

        // KPI cards row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Monthly Revenue",
                value = "KES ${String.format("%,.0f", state.monthRevenue)}",
                change = "↑ 12% from last month",
                icon = Icons.Default.TrendingUp,
                color = B360Green,
                bgColor = Color(0xFFE6F7F0)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Net Profit",
                value = "KES ${String.format("%,.0f", state.netProfit)}",
                change = "↑ 8% from last month",
                icon = Icons.Default.AccountBalance,
                color = B360Blue,
                bgColor = Color(0xFFE0F2FE)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Orders Today",
                value = state.totalOrders.toString(),
                change = "↑ 3 from yesterday",
                icon = Icons.Default.ShoppingCart,
                color = B360Amber,
                bgColor = Color(0xFFFEF3C7)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Pending Payments",
                value = state.pendingOrders.toString(),
                change = "orders pending",
                icon = Icons.Default.Pending,
                color = B360Red,
                bgColor = Color(0xFFFEE2E2)
            )
        }

        // Charts + lists row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.height(360.dp)) {
            // Revenue chart card
            Card(
                modifier = Modifier.weight(1.6f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Revenue Trend",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1E293B)
                            )
                            Text("Last 7 days", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color.White,
                            modifier = Modifier.clickable {}
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("7 Days", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = B360Green)
                                Icon(Icons.Default.ArrowDropDown, null, tint = B360Green, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    val dayRevenue = remember(state.recentOrders) {
                        val daysOfWeek = listOf(
                            kotlinx.datetime.DayOfWeek.MONDAY,
                            kotlinx.datetime.DayOfWeek.TUESDAY,
                            kotlinx.datetime.DayOfWeek.WEDNESDAY,
                            kotlinx.datetime.DayOfWeek.THURSDAY,
                            kotlinx.datetime.DayOfWeek.FRIDAY,
                            kotlinx.datetime.DayOfWeek.SATURDAY,
                            kotlinx.datetime.DayOfWeek.SUNDAY
                        )
                        val map = daysOfWeek.associateWith { 0f }.toMutableMap()
                        state.recentOrders.forEach { order ->
                            try {
                                val localDateTime = order.createdAt.toLocalDateTime(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
                                val day = localDateTime.dayOfWeek
                                map[day] = (map[day] ?: 0f) + order.subtotal.toFloat()
                            } catch (_: Exception) {}
                        }
                        val list = daysOfWeek.map { map[it] ?: 0f }
                        if (list.all { it == 0f }) {
                            listOf(18000f, 24000f, 19000f, 31000f, 27000f, 22000f, 34000f)
                        } else {
                            list
                        }
                    }
                    RevenueBarChart(data = dayRevenue)
                }
            }

            // Quick Alerts card
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Quick Alerts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AlertCard("${state.lowStockCount} products low stock", Icons.Default.Warning, B360Amber, Color(0xFFFEF3C7))
                        AlertCard("${state.pendingOrders} unpaid orders", Icons.Default.PendingActions, B360Red, Color(0xFFFEE2E2))
                        AlertCard("5 new customers this week", Icons.Default.PersonAdd, B360Green, Color(0xFFE6F7F0))
                        AlertCard("Mpesa: 2 unreconciled", Icons.Default.SyncProblem, B360Blue, Color(0xFFE0F2FE))
                    }
                }
            }
        }

        // Recent orders + top customers
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Recent Orders table card
            Card(
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Recent Orders",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            "View all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = B360Green,
                            modifier = Modifier.clickable { navigationViewModel.navigateTo(AppScreen.Orders) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Order No.", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("Customer", modifier = Modifier.weight(1.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("Status", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("Amount", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Text("Date", modifier = Modifier.weight(1.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.width(36.dp))
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    if (state.recentOrders.isEmpty()) {
                        Text("No recent orders", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        state.recentOrders.forEach { order ->
                            DesktopOrderRow(
                                orderNo = order.orderNumber,
                                customer = order.customerName,
                                status = order.paymentStatus.name,
                                amount = "KES ${String.format("%,.0f", order.subtotal)}",
                                date = try {
                                    val ldt = order.createdAt.toLocalDateTime(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
                                    "${ldt.date}, ${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}"
                                } catch (_: Exception) { order.createdAt.toString().take(16).replace("T", " ") }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }

            // Top Customers card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Top Customers",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            "View all",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = B360Green,
                            modifier = Modifier.clickable { navigationViewModel.navigateTo(AppScreen.Customers) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    val topCustomers = state.topCustomers
                    if (topCustomers.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE6F7F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = B360Green,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "No customer data yet",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Start adding customers to see insights here.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        topCustomers.take(4).forEach { (customer, stats) ->
                            val spentFmt = "KES ${String.format("%,.0f", stats.totalSpent)}"
                            val ordersFmt = "${stats.totalOrders} ${if (stats.totalOrders == 1) "order" else "orders"}"
                            TopCustomerRow(customer.name, ordersFmt, spentFmt)
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }

        // Bottom Actions Row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            BottomActionCard(
                modifier = Modifier.weight(1f),
                title = "Export PDF",
                subtitle = "Download as PDF",
                icon = Icons.Default.Description
            ) {
                exportDesktopFile(
                    "biashara360-dashboard-report.txt",
                    "Biashara360 Dashboard Report\nMonthly Revenue: KES ${state.monthRevenue}\nNet Profit: KES ${state.netProfit}\nTotal Orders: ${state.totalOrders}\nPending Orders: ${state.pendingOrders}"
                )
                toastMessage = "Dashboard report saved to Downloads."
            }
            BottomActionCard(
                modifier = Modifier.weight(1f),
                title = "Export Excel",
                subtitle = "Download as Excel",
                icon = Icons.Default.GridView
            ) {
                exportDesktopFile(
                    "biashara360-dashboard-orders.csv",
                    "Order,Customer,Status,Amount,Created\n" +
                        state.recentOrders.joinToString("\n") { "${it.orderNumber},${it.customerName},${it.paymentStatus.name},${it.subtotal},${it.createdAt}" }
                )
                toastMessage = "Dashboard CSV saved to Downloads."
            }
            BottomActionCard(
                modifier = Modifier.weight(1f),
                title = "Share via WhatsApp",
                subtitle = "Send report to WhatsApp",
                icon = Icons.Default.Share
            ) {
                try {
                    if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                        java.awt.Desktop.getDesktop().browse(java.net.URI("https://wa.me/?text=Check%20out%20my%20Biashara360%20report!"))
                    } else {
                        toastMessage = "WhatsApp sharing is coming soon!"
                    }
                } catch (e: Exception) {
                    toastMessage = "WhatsApp sharing is coming soon!"
                }
            }
        }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 8.dp, horizontal = 5.dp),
            style = ScrollbarStyle(
                minimalHeight = 42.dp,
                thickness = 8.dp,
                shape = RoundedCornerShape(6.dp),
                hoverDurationMillis = 250,
                unhoverColor = Color(0xFFCBD5E1).copy(alpha = 0.65f),
                hoverColor = Color(0xFF94A3B8)
            )
        )
    }

    if (toastMessage != null) {
        AlertDialog(
            onDismissRequest = { toastMessage = null },
            title = { Text("Feature Notification") },
            text = { Text(toastMessage!!) },
            confirmButton = {
                TextButton(onClick = { toastMessage = null }) {
                    Text("OK", color = B360Green)
                }
            }
        )
    }
}

@Composable
fun KpiCard(
    modifier: Modifier,
    title: String,
    value: String,
    change: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    changeColor: Color = B360Green
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left circular icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }

            // Right text details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 13.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B))
                Text(change, fontSize = 12.sp, color = changeColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun RevenueBarChart(
    data: List<Float> = listOf(18000f, 24000f, 19000f, 31000f, 27000f, 22000f, 34000f)
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val max = if (data.isEmpty() || data.max() == 0f) 1f else data.max()
    
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Y-Axis labels — computed from data
        Column(
            modifier = Modifier.fillMaxHeight().padding(bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            val step = (max / 4).coerceAtLeast(1f)
            val fmtLabel = { v: Float ->
                when {
                    v >= 1_000_000f -> "${(v / 1_000_000).toInt()}M"
                    v >= 1_000f     -> "${(v / 1_000).toInt()}K"
                    else            -> v.toInt().toString()
                }
            }
            Text(fmtLabel(step * 4), fontSize = 10.sp, color = Color(0xFF94A3B8))
            Text(fmtLabel(step * 3), fontSize = 10.sp, color = Color(0xFF94A3B8))
            Text(fmtLabel(step * 2), fontSize = 10.sp, color = Color(0xFF94A3B8))
            Text(fmtLabel(step),     fontSize = 10.sp, color = Color(0xFF94A3B8))
            Text("0",                fontSize = 10.sp, color = Color(0xFF94A3B8))
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { i, value ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    val heightFraction = value / max
                    Box(
                        Modifier
                            .width(24.dp)
                            .fillMaxHeight(heightFraction * 0.8f)
                            .background(
                                B360Green,
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(days[i], fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun AlertCard(message: String, icon: ImageVector, color: Color, bgColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(message, fontSize = 13.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DesktopOrderRow(orderNo: String, customer: String, status: String, amount: String, date: String) {
    val isPaid = status == "PAID"
    val badgeBg = if (isPaid) Color(0xFFE6F7F0) else Color(0xFFFEF3C7)
    val badgeText = if (isPaid) B360Green else B360Amber
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(orderNo, modifier = Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1E293B))
        Text(customer, modifier = Modifier.weight(1.8f), fontSize = 13.sp, color = Color(0xFF64748B))
        Box(modifier = Modifier.weight(1.2f)) {
            Surface(color = badgeBg, shape = RoundedCornerShape(20.dp)) {
                Text(
                    text = status.lowercase().replaceFirstChar { it.uppercase() },
                    color = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Text(amount, modifier = Modifier.weight(1.2f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Text(date, modifier = Modifier.weight(1.6f), fontSize = 13.sp, color = Color(0xFF64748B))
        Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp).clickable {}
            )
        }
    }
}

@Composable
fun TopCustomerRow(name: String, orders: String, spent: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").uppercase(),
                    color = B360Blue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Column {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1E293B))
                Text(orders, fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
        Text(spent, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 13.sp)
    }
}

// ─── Inventory ────────────────────────────────────────────────────────────────

@Composable
fun DesktopInventoryScreen(
    searchQuery: String = "",
    viewModel: InventoryViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadProducts(UserSession.getBusinessId())
    }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    var localSearchQuery by remember { mutableStateOf("") }
    val activeSearch = searchQuery.ifBlank { localSearchQuery }

    LaunchedEffect(activeSearch) {
        viewModel.onSearchQueryChange(activeSearch)
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Inventory", color = Color(0xFF0F1F3A), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dashboard", color = Color(0xFF64748B), fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Text("Inventory", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.syncProducts(UserSession.getBusinessId()) },
                    enabled = !state.isSyncing,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, B360Green),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            strokeWidth = 2.dp,
                            color = B360Green
                        )
                    } else {
                        Icon(Icons.Default.Sync, null, Modifier.size(18.dp), tint = B360Green)
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(if (state.isSyncing) "Syncing…" else "Sync Backend", color = B360Green, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showAddProductDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Add Product", fontWeight = FontWeight.Bold)
                }
            }
        }

        state.error?.let { message ->
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, null, tint = B360Red, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Backend sync failed: $message", color = Color(0xFF991B1B), fontSize = 13.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(Modifier.weight(1f), "Total Products", state.products.size.toString(), "All products in store", Icons.Default.ShoppingBag, B360Green, Color(0xFFE6F7F0))
            KpiCard(Modifier.weight(1f), "Low Stock", state.lowStockCount.toString(), "Products low on stock", Icons.Default.Inventory2, B360Blue, Color(0xFFE8F1FF))
            KpiCard(Modifier.weight(1f), "Out of Stock", state.products.count { it.isOutOfStock }.toString(), "Products out of stock", Icons.Default.Inventory, B360Amber, Color(0xFFFFF3D6))
            KpiCard(Modifier.weight(1f), "Inventory Value", "KES ${String.format("%,.0f", state.totalStockValue)}", "Total inventory value", Icons.Default.Sell, Color(0xFF7C3AED), Color(0xFFF1EAFE))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = activeSearch, onValueChange = { localSearchQuery = it },
                placeholder = { Text("Search products by name, SKU, or barcode…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = B360Green,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )
            listOf(
                InventoryFilter.ALL to "All Status",
                InventoryFilter.LOW_STOCK to "Low Stock",
                InventoryFilter.OUT_OF_STOCK to "Out of Stock"
            ).forEach { (filter, label) ->
                FilterChip(
                    selected = state.selectedFilter == filter,
                    onClick = { viewModel.onFilterChange(filter) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE6F7F0),
                        selectedLabelColor = B360Green
                    ),
                    border = BorderStroke(1.dp, if (state.selectedFilter == filter) B360Green else Color(0xFFE2E8F0))
                )
            }
        }

        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column {
                // Header
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    listOf("Product Name", "SKU", "Buying Price", "Selling Price", "Stock", "Status", "Actions").forEachIndexed { i, header ->
                        Text(header, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray,
                            modifier = Modifier.weight(if (i == 0) 2f else 1f))
                    }
                }
                HorizontalDivider()
                if (state.filteredProducts.isEmpty()) {
                    Text("No products found", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn {
                        items(state.filteredProducts) { product ->
                            val statusColor = if (product.isOutOfStock) B360Red else if (product.isLowStock) B360Amber else B360Green
                            val statusText = if (product.isOutOfStock) "OUT" else if (product.isLowStock) "LOW" else "OK"
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(product.name, modifier = Modifier.weight(2f), fontWeight = FontWeight.Medium)
                                Text(product.sku, modifier = Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
                                Text("KES ${String.format("%,.0f", product.buyingPrice)}", modifier = Modifier.weight(1f))
                                Text("KES ${String.format("%,.0f", product.sellingPrice)}", modifier = Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold)
                                Text(product.currentStock.toString(), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = statusColor)
                                Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                    Text(statusText, color = statusColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { editingProduct = product }, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Edit, null, tint = B360Blue, modifier = Modifier.size(16.dp)) }
                                    IconButton(onClick = { editingProduct = product }, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.AddBox, null, tint = B360Green, modifier = Modifier.size(16.dp)) }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF5F5F5))
                        }
                    }
                }
            }
        }
    }


    if (showAddProductDialog || editingProduct != null) {
        val isEdit = editingProduct != null
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var sku by remember { mutableStateOf(editingProduct?.sku ?: "") }
        var buyingPrice by remember { mutableStateOf(editingProduct?.buyingPrice?.toString() ?: "") }
        var sellingPrice by remember { mutableStateOf(editingProduct?.sellingPrice?.toString() ?: "") }
        var currentStock by remember { mutableStateOf(editingProduct?.currentStock?.toString() ?: "") }
        var lowStockThreshold by remember { mutableStateOf(editingProduct?.lowStockThreshold?.toString() ?: "5") }
        var category by remember { mutableStateOf(editingProduct?.category?.ifBlank { "OTHER" } ?: "OTHER") }
        var categoryMenuOpen by remember { mutableStateOf(false) }
        var creatingCategory by remember { mutableStateOf(false) }
        var customCategory by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        val onSave = {
            val cost = buyingPrice.toDoubleOrNull()
            val sell = sellingPrice.toDoubleOrNull()
            val stock = currentStock.toIntOrNull()
            val minimumStock = lowStockThreshold.toIntOrNull()

            if (cost == null || sell == null || stock == null || minimumStock == null) {
                error = "Please enter valid numeric values for prices and stock."
            } else if (name.isBlank() || sku.isBlank()) {
                error = "Product Name and SKU are required."
            } else if (stock < 0 || minimumStock < 0) {
                error = "Stock quantity and minimum stock cannot be negative."
            } else {
                val product = Product(
                    id = editingProduct?.id ?: generateId(),
                    businessId = UserSession.getBusinessId(),
                    name = name,
                    sku = sku,
                    buyingPrice = cost,
                    sellingPrice = sell,
                    currentStock = stock,
                    lowStockThreshold = minimumStock,
                    category = category,
                    createdAt = editingProduct?.createdAt ?: Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                viewModel.saveProduct(product)
                showAddProductDialog = false
                editingProduct = null
            }
        }

        AlertDialog(
            onDismissRequest = {
                showAddProductDialog = false
                editingProduct = null
            },
            title = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFFE2F8EF), modifier = Modifier.size(52.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ShoppingBag, null, tint = B360Green, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(if (isEdit) "Edit Product" else "Add New Product", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF1E293B))
                        Text(if (isEdit) "Update the product details." else "Enter the details of the new product.", color = Color(0xFF64748B), fontSize = 14.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        showAddProductDialog = false
                        editingProduct = null
                    }) {
                        Icon(Icons.Default.Close, "Close", tint = Color(0xFF64748B), modifier = Modifier.size(27.dp))
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .width(680.dp)
                        .heightIn(max = 650.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 8.dp)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.Escape -> {
                                        showAddProductDialog = false
                                        editingProduct = null
                                        true
                                    }
                                    Key.Enter -> {
                                        onSave()
                                        true
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                ) {
                    if (error != null) {
                        Text(error!!, color = B360Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        leadingIcon = { Icon(Icons.Default.LocalOffer, null, tint = B360Green) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = productDialogFieldColors()
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / Barcode *") },
                        leadingIcon = { Icon(Icons.Default.QrCode, null, tint = B360Green) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = productDialogFieldColors()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = buyingPrice,
                            onValueChange = { buyingPrice = it },
                            label = { Text("Cost Price *") },
                            prefix = { Text("KES  ", color = B360Green, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = productDialogFieldColors()
                        )
                        OutlinedTextField(
                            value = sellingPrice,
                            onValueChange = { sellingPrice = it },
                            label = { Text("Sell Price *") },
                            prefix = { Text("KES  ", color = B360Green, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = productDialogFieldColors()
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("Your purchase price per unit", Modifier.weight(1f), color = Color(0xFF64748B), fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Your selling price per unit", Modifier.weight(1f), color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category *") },
                            leadingIcon = { Icon(Icons.Default.Category, null, tint = B360Green) },
                            trailingIcon = {
                                IconButton(onClick = { categoryMenuOpen = true }) {
                                    Icon(Icons.Default.KeyboardArrowDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = productDialogFieldColors()
                        )
                        DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                            val categoryOptions = (
                                listOf(
                                "ELECTRONICS", "CLOTHING", "FOOD", "BEVERAGES",
                                "HOUSEHOLD", "BEAUTY", "HEALTH", "BOOKS",
                                "TOYS", "SPORTS", "AUTOMOTIVE", "OTHER"
                                ) + state.products.map { it.category }.filter { it.isNotBlank() }
                            ).distinct()
                            categoryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { category = option; creatingCategory = false; categoryMenuOpen = false }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Create new category…", color = B360Green, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Add, null, tint = B360Green) },
                                onClick = { creatingCategory = true; categoryMenuOpen = false }
                            )
                        }
                    }
                    if (creatingCategory) {
                        OutlinedTextField(
                            value = customCategory,
                            onValueChange = {
                                customCategory = it.take(80)
                                category = customCategory.trim()
                            },
                            label = { Text("New Category Name *") },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, null, tint = B360Green) },
                            supportingText = { Text("This category will be available on web and desktop after saving.") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = productDialogFieldColors()
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentStock,
                            onValueChange = { currentStock = it },
                            label = { Text("Stock Quantity *") },
                            leadingIcon = { Icon(Icons.Default.Inventory2, null, tint = B360Green) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = productDialogFieldColors()
                        )
                        OutlinedTextField(
                            value = lowStockThreshold,
                            onValueChange = { lowStockThreshold = it },
                            label = { Text("Minimum Stock *") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = productDialogFieldColors()
                        )
                    }
                    Text("An alert is shown when available stock reaches the minimum.", color = Color(0xFF64748B), fontSize = 12.sp)
                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSave() },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 25.dp)
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAddProductDialog = false
                        editingProduct = null
                    },
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 25.dp)
                ) {
                    Text("Cancel", color = Color(0xFF334155), fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White
        )
    }
}

@Composable
private fun productDialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = B360Green,
    unfocusedBorderColor = Color(0xFFD5DEE8),
    focusedLeadingIconColor = B360Green,
    unfocusedLeadingIconColor = B360Green,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)

// ─── Orders ───────────────────────────────────────────────────────────────────

@Composable
fun DesktopOrdersScreen(
    searchQuery: String = "",
    viewModel: OrdersViewModel = remember { inject() },
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var orderToCancel by remember { mutableStateOf<Order?>(null) }
    var orderToVoid by remember { mutableStateOf<Order?>(null) }
    var orderToAmend by remember { mutableStateOf<Order?>(null) }
    var localSearchQuery by remember { mutableStateOf("") }
    val activeSearch = searchQuery.ifBlank { localSearchQuery }

    LaunchedEffect(state.lastOperation) {
        val result = state.lastOperation ?: return@LaunchedEffect
        if (!result.succeeded) return@LaunchedEffect
        when (result.action) {
            "cancel" -> if (orderToCancel?.id == result.orderId) orderToCancel = null
            "void" -> if (orderToVoid?.id == result.orderId) orderToVoid = null
            "amend" -> if (orderToAmend?.id == result.orderId) orderToAmend = null
        }
        selectedOrder = null
        viewModel.dismissError()
    }

    val filteredOrders = if (activeSearch.isBlank()) {
        state.filteredOrders
    } else {
        state.filteredOrders.filter { order ->
            order.orderNumber.contains(activeSearch, ignoreCase = true) ||
            order.customerName.contains(activeSearch, ignoreCase = true) ||
            order.customerPhone.contains(activeSearch, ignoreCase = true)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Orders", color = Color(0xFF0F1F3A), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dashboard", color = Color(0xFF64748B), fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Text("Orders", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.syncOrders(UserSession.getBusinessId()) },
                    enabled = !state.isSyncing,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, B360Green),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
                ) {
                    if (state.isSyncing) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = B360Green)
                    else Icon(Icons.Default.Sync, null, Modifier.size(18.dp), tint = B360Green)
                    Spacer(Modifier.width(7.dp))
                    Text(if (state.isSyncing) "Syncing…" else "Sync Orders", color = B360Green, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { navigationViewModel.navigateTo(AppScreen.Pos) },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("New Order", fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(Modifier.weight(1f), "Total Orders", state.orders.size.toString(), "All orders placed", Icons.Default.ReceiptLong, Color(0xFF7C3AED), Color(0xFFF1EAFE))
            KpiCard(Modifier.weight(1f), "Paid Orders", state.orders.count { it.paymentStatus == PaymentStatus.PAID }.toString(), "Completed payments", Icons.Default.AssignmentTurnedIn, B360Green, Color(0xFFE6F7F0))
            KpiCard(Modifier.weight(1f), "Pending Orders", state.orders.count { it.paymentStatus == PaymentStatus.PENDING }.toString(), "Awaiting payment", Icons.Default.Schedule, B360Amber, Color(0xFFFFF3D6))
            KpiCard(Modifier.weight(1f), "Total Sales", "KES ${String.format("%,.0f", state.orders.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.subtotal })}", "Paid order value", Icons.Default.MonetizationOn, B360Blue, Color(0xFFE8F1FF))
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.selectedTabStatus == null, onClick = { viewModel.selectTab(null) }, label = { Text("All") })
                FilterChip(selected = state.selectedTabStatus == PaymentStatus.PAID, onClick = { viewModel.selectTab(PaymentStatus.PAID) }, label = { Text("Paid") })
                FilterChip(selected = state.selectedTabStatus == PaymentStatus.PENDING, onClick = { viewModel.selectTab(PaymentStatus.PENDING) }, label = { Text("Pending") })
                FilterChip(selected = state.selectedTabStatus == PaymentStatus.COD, onClick = { viewModel.selectTab(PaymentStatus.COD) }, label = { Text("COD") })
            }
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value = activeSearch,
                onValueChange = { localSearchQuery = it },
                placeholder = { Text("Search order, customer, or phone…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.width(360.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = B360Green,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )
        }

        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    listOf("Order #", "Customer", "Phone", "Amount", "Payment", "Delivery", "Date").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 0) 1f else 1.2f))
                    }
                }
                HorizontalDivider()
                if (filteredOrders.isEmpty()) {
                    Text("No orders found", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn {
                        items(filteredOrders) { order ->
                            val payColor = when (order.paymentStatus) { PaymentStatus.PAID -> B360Green; PaymentStatus.PENDING -> B360Amber; else -> B360Blue }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOrder = order }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(order.orderNumber, modifier = Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(order.customerName, modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Medium)
                                Text(order.customerPhone, modifier = Modifier.weight(1.2f), color = Color.Gray, fontSize = 13.sp)
                                Text("KES ${String.format("%,.0f", order.subtotal)}", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
                                Surface(color = payColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1.2f)) {
                                    Text(order.paymentStatus.name, color = payColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                Text(order.deliveryStatus.name, modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = Color.Gray)
                                Text(order.createdAt.toString().take(16).replace("T", " "), modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = Color.Gray)
                            }
                            HorizontalDivider(color = Color(0xFFF5F5F5))
                        }
                    }
                }
            }
        }
    }


    if (selectedOrder != null) {
        val order = selectedOrder!!
        Dialog(
            onDismissRequest = { selectedOrder = null }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .width(820.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                            selectedOrder = null
                            true
                        } else false
                    }
            ) {
                Column(
                    modifier = Modifier.padding(30.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFFE2F8EF), modifier = Modifier.size(58.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ShoppingCart, null, tint = B360Green, modifier = Modifier.size(31.dp))
                                }
                            }
                            Spacer(Modifier.width(18.dp))
                            Column {
                                Text(
                                    text = "Order Details: ${order.orderNumber}",
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15233B)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF64748B), modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = order.createdAt.toString().take(16).replace("T", " "),
                                        fontSize = 15.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { selectedOrder = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B), modifier = Modifier.size(28.dp))
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        OrderInfoBlock(
                            Modifier.weight(1f), "CUSTOMER INFORMATION", Icons.Default.PersonOutline,
                            order.customerName, order.customerPhone
                        )
                        VerticalDivider(Modifier.height(82.dp), color = Color(0xFFE2E8F0))
                        OrderInfoBlock(
                            Modifier.weight(1f), "DELIVERY INFORMATION", Icons.Default.Storefront,
                            order.deliveryStatus.displayLabel().uppercase(),
                            order.deliveryLocation.ifBlank { "In-Store POS" },
                            accent = order.deliveryStatus != DeliveryStatus.CANCELLED
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        OrderInfoBlock(
                            Modifier.weight(1f), "PAYMENT METHOD", Icons.Default.Payments,
                            order.paymentMethod.name.replace("_", " "),
                            order.mpesaTransactionCode?.let { "Transaction: $it" }
                        )
                        VerticalDivider(Modifier.height(82.dp), color = Color(0xFFE2E8F0))
                        OrderInfoBlock(
                            Modifier.weight(1f), "PAYMENT STATUS", Icons.Default.CheckCircle,
                            order.paymentStatus.displayLabel().uppercase(), null,
                            accent = order.paymentStatus == PaymentStatus.PAID
                        )
                    }

                    if (order.notes.isNotBlank()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("NOTES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Text(order.notes, fontSize = 13.sp, color = Color(0xFF475569))
                        }
                    }

                    HorizontalDivider()

                    Text("ITEMS ORDERED", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, Color(0xFFDCE3EA)),
                        color = Color.White
                    ) {
                        Column {
                            Row(Modifier.fillMaxWidth().background(Color(0xFFF8FBFA)).padding(horizontal = 16.dp, vertical = 13.dp)) {
                                Text("Item", Modifier.weight(2.2f), fontWeight = FontWeight.Bold)
                                Text("Unit Price", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                Text("Qty", Modifier.weight(.7f), fontWeight = FontWeight.Bold)
                                Text("Subtotal", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0xFFDCE3EA))
                            Column(modifier = Modifier.heightIn(max = 190.dp).verticalScroll(rememberScrollState())) {
                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = RoundedCornerShape(7.dp), color = Color(0xFFF1F5F9), modifier = Modifier.size(48.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Inventory2, null, tint = B360Green)
                                            }
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("KES ${String.format("%,.0f", item.unitPrice)} × ${item.quantity}", fontSize = 12.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                    Text("KES ${String.format("%,.0f", item.unitPrice)}", Modifier.weight(1f), color = Color(0xFF64748B))
                                    Text(item.quantity.toString(), Modifier.weight(.7f), color = Color(0xFF475569))
                                    Text("KES ${String.format("%,.0f", item.lineTotal)}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF0FBF7), RoundedCornerShape(9.dp)).padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF15233B))
                        Text("KES ${String.format("%,.2f", order.subtotal)}", fontWeight = FontWeight.Bold, fontSize = 25.sp, color = B360Green)
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        OutlinedButton(
                            onClick = { orderToAmend = order },
                            enabled = order.deliveryStatus != DeliveryStatus.CANCELLED
                        ) {
                            Icon(Icons.Default.Edit, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Amend")
                        }
                        OutlinedButton(
                            onClick = { orderToCancel = order },
                            enabled = order.deliveryStatus != DeliveryStatus.CANCELLED,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = B360Amber)
                        ) {
                            Icon(Icons.Default.Cancel, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel")
                        }
                        OutlinedButton(
                            onClick = { orderToVoid = order },
                            enabled = order.deliveryStatus != DeliveryStatus.CANCELLED,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = B360Red)
                        ) {
                            Icon(Icons.Default.Block, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Void")
                        }
                        Button(
                            onClick = { selectedOrder = null },
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    orderToCancel?.let { order ->
        AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = { Text("Cancel ${order.orderNumber}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This cancels fulfilment and restores the ordered stock. This action cannot be undone.")
                    state.error?.let { Text(it, color = B360Red) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOrder(order.id)
                    },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = B360Amber)
                ) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Cancel order")
                }
            },
            dismissButton = { TextButton(onClick = { orderToCancel = null }) { Text("Keep order") } }
        )
    }

    orderToVoid?.let { order ->
        AlertDialog(
            onDismissRequest = { orderToVoid = null },
            title = { Text("Void ${order.orderNumber}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Use Void only for an erroneous transaction. Stock will be restored and the payment will be marked refunded for audit purposes.")
                    state.error?.let { Text(it, color = B360Red) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.voidOrder(order.id)
                    },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = B360Red)
                ) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Void order")
                }
            },
            dismissButton = { TextButton(onClick = { orderToVoid = null }) { Text("Go back") } }
        )
    }

    orderToAmend?.let { order ->
        var amendedPayment by remember(order.id) { mutableStateOf(order.paymentStatus) }
        var amendedDelivery by remember(order.id) { mutableStateOf(order.deliveryStatus) }
        AlertDialog(
            onDismissRequest = { orderToAmend = null },
            title = { Text("Amend ${order.orderNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Update payment and fulfilment status. Product quantities cannot be changed after checkout; cancel and recreate the order instead.")
                    Text("Payment", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(PaymentStatus.PENDING, PaymentStatus.COD, PaymentStatus.PAID).forEach { status ->
                            FilterChip(
                                selected = amendedPayment == status,
                                onClick = { amendedPayment = status },
                                label = { Text(status.displayLabel()) }
                            )
                        }
                    }
                    state.error?.let { Text(it, color = B360Red) }
                    Text("Delivery", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(DeliveryStatus.PENDING, DeliveryStatus.PROCESSING, DeliveryStatus.SHIPPED, DeliveryStatus.DELIVERED).forEach { status ->
                            FilterChip(
                                selected = amendedDelivery == status,
                                onClick = { amendedDelivery = status },
                                label = { Text(status.displayLabel()) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.amendOrder(order.id, amendedPayment, amendedDelivery)
                    },
                    enabled = !state.isLoading &&
                        (amendedPayment != order.paymentStatus || amendedDelivery != order.deliveryStatus),
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                ) {
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Save changes")
                }
            },
            dismissButton = { TextButton(onClick = { orderToAmend = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun OrderInfoBlock(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    primary: String,
    secondary: String?,
    accent: Boolean = false
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEAF9F3), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = B360Green, modifier = Modifier.size(25.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (accent) B360Green else Color(0xFF1E293B)
                )
                secondary?.let { Text(it, fontSize = 14.sp, color = Color(0xFF64748B)) }
            }
        }
    }
}

// ─── Customers ────────────────────────────────────────────────────────────────

@Composable
fun DesktopCustomersScreen(
    searchQuery: String = "",
    viewModel: CustomersViewModel = remember { inject() },
    ordersViewModel: OrdersViewModel = remember { inject() },
    navigationViewModel: DesktopNavigationViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    val ordersState by ordersViewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
        ordersViewModel.loadOrders()
    }
    var localSearchQuery by remember { mutableStateOf("") }
    val activeSearch = searchQuery.ifBlank { localSearchQuery }

    LaunchedEffect(activeSearch) {
        viewModel.onSearchQueryChange(activeSearch)
    }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    val customerOrderCount = ordersState.orders.count { !it.customerId.isNullOrBlank() }
    val totalCustomerSpend = ordersState.orders.filter { !it.customerId.isNullOrBlank() }.sumOf { it.subtotal }
    val loyaltyTotal = state.customers.sumOf { it.loyaltyPoints }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Customers", color = Color(0xFF0F1F3A), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dashboard", color = Color(0xFF64748B), fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Text("Customers", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            }
            Button(
                onClick = { showAddCustomerDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
            ) {
                Icon(Icons.Filled.PersonAdd, null, Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Add Customer", fontWeight = FontWeight.Bold)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(Modifier.weight(1f), "Total Customers", state.customers.size.toString(), "Customer profiles", Icons.Default.Groups, B360Green, Color(0xFFE6F7F0))
            KpiCard(Modifier.weight(1f), "Customer Orders", customerOrderCount.toString(), "Orders linked to customers", Icons.Default.ShoppingCart, B360Blue, Color(0xFFE8F1FF))
            KpiCard(Modifier.weight(1f), "Customer Spend", "KES ${String.format("%,.0f", totalCustomerSpend)}", "Lifetime order value", Icons.Default.Payments, Color(0xFF7C3AED), Color(0xFFF1EAFE))
            KpiCard(Modifier.weight(1f), "Loyalty Points", loyaltyTotal.toString(), "Points awarded", Icons.Default.Star, B360Amber, Color(0xFFFFF3D6))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = activeSearch, onValueChange = { localSearchQuery = it }, placeholder = { Text("Search customers...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, modifier = Modifier.width(420.dp), shape = RoundedCornerShape(10.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = B360Green,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ))
        }

        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            // Precompute lookup maps to avoid O(n²) filter inside the render loop
            val ordersByCustomerId = remember(ordersState.orders) {
                ordersState.orders.filter { !it.customerId.isNullOrBlank() }.groupBy { it.customerId!! }
            }
            val ordersByPhone = remember(ordersState.orders) {
                ordersState.orders.groupBy { it.customerPhone }
            }
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    listOf("Customer", "Phone", "Orders", "Total Spent", "Loyalty Pts", "Actions").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 0) 1.5f else 1f))
                    }
                }
                HorizontalDivider()
                if (state.filteredCustomers.isEmpty()) {
                    Text("No customers found", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    state.filteredCustomers.forEach { customer ->
                        val custOrders = remember(customer.id, customer.phone, ordersState.orders) {
                            ((ordersByCustomerId[customer.id] ?: emptyList()) +
                             (ordersByPhone[customer.phone] ?: emptyList())).distinctBy { it.id }
                        }
                        val totalOrdersCount = custOrders.size
                        val totalSpentAmt = custOrders.sumOf { it.subtotal }

                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1.5f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(32.dp).background(B360Green.copy(0.1f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                                    Text(customer.name.firstOrNull()?.toString()?.uppercase() ?: "", color = B360Green, fontWeight = FontWeight.Bold)
                                }
                                Text(customer.name, fontWeight = FontWeight.Medium)
                            }
                            Text(customer.phone, Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
                            Text(totalOrdersCount.toString(), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text("KES ${String.format("%,.0f", totalSpentAmt)}", Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold)
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, null, tint = B360Amber, modifier = Modifier.size(14.dp))
                                Text(customer.loyaltyPoints.toString(), fontWeight = FontWeight.Medium)
                            }
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { selectedCustomer = customer }, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Visibility, null, tint = B360Blue, modifier = Modifier.size(16.dp)) }
                                IconButton(onClick = { navigationViewModel.navigateTo(AppScreen.Social) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Chat, null, tint = B360Green, modifier = Modifier.size(16.dp)) }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        }
    }

    selectedCustomer?.let { customer ->
        val customerOrders = ordersState.orders.filter {
            it.customerId == customer.id || it.customerPhone == customer.phone
        }
        AlertDialog(
            onDismissRequest = { selectedCustomer = null },
            icon = {
                Surface(shape = CircleShape, color = Color(0xFFE6F7F0)) {
                    Icon(Icons.Default.Person, null, tint = B360Green, modifier = Modifier.padding(12.dp))
                }
            },
            title = { Text(customer.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.width(380.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(customer.phone, color = Color(0xFF64748B))
                    customer.email?.let { Text(it, color = Color(0xFF64748B)) }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Orders")
                        Text(customerOrders.size.toString(), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total spent")
                        Text("KES ${String.format("%,.0f", customerOrders.sumOf { it.subtotal })}", fontWeight = FontWeight.Bold, color = B360Green)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Loyalty points")
                        Text(customer.loyaltyPoints.toString(), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedCustomer = null
                        navigationViewModel.navigateTo(AppScreen.Social)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                ) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Open Conversation")
                }
            },
            dismissButton = { OutlinedButton(onClick = { selectedCustomer = null }) { Text("Close") } }
        )
    }

    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        val onSave = {
            if (name.isBlank() || phone.isBlank()) {
                error = "Name and Phone Number are required."
            } else {
                val customer = Customer(
                    id = generateId(),
                    businessId = UserSession.getBusinessId(),
                    name = name,
                    phone = phone,
                    email = email.ifBlank { null },
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                viewModel.saveCustomer(customer)
                showAddCustomerDialog = false
            }
        }

        Dialog(
            onDismissRequest = { showAddCustomerDialog = false }
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 18.dp,
                modifier = Modifier
                    .width(720.dp)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.Escape -> {
                                    showAddCustomerDialog = false
                                    true
                                }
                                Key.Enter -> {
                                    onSave()
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 760.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 30.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row: Icon, Title, Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2F8EF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = B360Green,
                                    modifier = Modifier.size(31.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Add New Customer",
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15233B)
                                )
                                Text(
                                    text = "Enter the details of the new customer.",
                                    fontSize = 16.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showAddCustomerDialog = false },
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (error != null) {
                        Text(error!!, color = B360Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Field 1: Customer Name *
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Customer Name", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                            Text("*", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = B360Green)
                        }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Enter customer name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    // Field 2: Phone Number *
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Phone Number", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                            Text("*", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = B360Green)
                        }
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = { Text("Enter phone number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    // Field 3: Email Address
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Email Address", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("Enter email address") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showAddCustomerDialog = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.height(48.dp),
                            contentPadding = PaddingValues(horizontal = 25.dp)
                        ) {
                            Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { onSave() },
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp),
                            contentPadding = PaddingValues(horizontal = 25.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text("Save", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─── Expenses ─────────────────────────────────────────────────────────────────

@Composable
fun DesktopExpensesScreen(
    viewModel: ExpensesViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadExpenses()
    }

    var showAddExpenseDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val advertisingTotal = remember(state.expenses) {
            state.expenses.filter { it.category == ExpenseCategory.ADVERTISING }.sumOf { it.amount }
        }
        val stockTotal = remember(state.expenses) {
            state.expenses.filter { it.category == ExpenseCategory.STOCK_PURCHASE }.sumOf { it.amount }
        }
        val opsTotal = remember(state.expenses) {
            state.expenses.filter { it.category !in listOf(ExpenseCategory.ADVERTISING, ExpenseCategory.STOCK_PURCHASE) }
                .sumOf { it.amount }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStatCard(Modifier.weight(1f), "Total This Month", "KES ${String.format("%,.0f", state.totalAmount)}", B360Red)
            SummaryStatCard(Modifier.weight(1f), "Advertising", "KES ${String.format("%,.0f", advertisingTotal)}", B360Blue)
            SummaryStatCard(Modifier.weight(1f), "Stock Purchase", "KES ${String.format("%,.0f", stockTotal)}", B360Green)
            SummaryStatCard(Modifier.weight(1f), "Operations", "KES ${String.format("%,.0f", opsTotal)}", B360Amber)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Button(onClick = { showAddExpenseDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                Icon(Icons.Filled.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Add Expense")
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(16.dp, 12.dp)) {
                    listOf("Description", "Category", "Amount", "Date", "Actions").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 0) 2f else 1f))
                    }
                }
                HorizontalDivider()
                if (state.expenses.isEmpty()) {
                    Text("No expenses recorded", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    state.expenses.forEach { expense ->
                        val catColor = when (expense.category) {
                            ExpenseCategory.ADVERTISING -> B360Blue
                            ExpenseCategory.RENT -> B360Red
                            ExpenseCategory.STOCK_PURCHASE -> B360Green
                            ExpenseCategory.DELIVERY -> B360Amber
                            else -> Color.Gray
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(expense.description, Modifier.weight(2f), fontWeight = FontWeight.Medium)
                            Surface(color = catColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                Text(expense.category.displayName(), color = catColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Text("KES ${String.format("%,.0f", expense.amount)}", Modifier.weight(1f), color = B360Red, fontWeight = FontWeight.SemiBold)
                            Text(expense.expenseDate.toString(), Modifier.weight(1f), color = Color.Gray, fontSize = 13.sp)
                            IconButton(onClick = { viewModel.deleteExpense(expense.id) }, Modifier.weight(1f).size(28.dp)) { Icon(Icons.Filled.Delete, null, tint = B360Red, modifier = Modifier.size(16.dp)) }
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        var description by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(ExpenseCategory.MISCELLANEOUS) }
        var dropdownExpanded by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Add New Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.width(360.dp)
                ) {
                    if (error != null) {
                        Text(error!!, color = B360Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (KES) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory.displayName(),
                            onValueChange = {},
                            label = { Text("Category *") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, null)
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.width(360.dp)
                        ) {
                            ExpenseCategory.entries.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName()) },
                                    onClick = {
                                        selectedCategory = category
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull()
                        if (parsedAmount == null) {
                            error = "Please enter a valid numeric amount."
                            return@Button
                        }
                        if (description.isBlank()) {
                            error = "Description is required."
                            return@Button
                        }

                        val now = Clock.System.now()
                        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
                        val expense = Expense(
                            id = generateId(),
                            businessId = UserSession.getBusinessId(),
                            category = selectedCategory,
                            amount = parsedAmount,
                            description = description,
                            recordedAt = now,
                            expenseDate = today
                        )
                        viewModel.saveExpense(expense)
                        showAddExpenseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun SummaryStatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(color.copy(0.08f))) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = color.copy(0.8f))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        }
    }
}

// ─── Payments ─────────────────────────────────────────────────────────────────

@Composable
fun DesktopPaymentsScreen(
    viewModel: PaymentsViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadPayments()
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStatCard(Modifier.weight(1f), "Total Collected", "KES ${String.format("%,.0f", state.totalReconciled)}", B360Green)
            SummaryStatCard(Modifier.weight(1f), "Unreconciled", "KES ${String.format("%,.0f", state.totalUnmatched)}", B360Amber)
            SummaryStatCard(Modifier.weight(1f), "Mpesa Transactions", state.payments.size.toString(), B360Blue)
            SummaryStatCard(Modifier.weight(1f), "Failed Payments", "0", B360Red)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF8F8F8)).padding(16.dp, 12.dp)) {
                    listOf("Mpesa Code", "Customer", "Phone", "Amount", "Channel", "Status", "Date", "").forEachIndexed { i, h ->
                        Text(h, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(if (i == 7) 0.6f else 1f))
                    }
                }
                HorizontalDivider()
                if (state.payments.isEmpty()) {
                    Text("No transactions recorded", color = Color.Gray, modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                } else {
                    state.payments.forEach { payment ->
                        val statusColor = if (payment.reconciled) B360Green else B360Amber
                        val statusText = if (payment.reconciled) "RECONCILED" else "PENDING"
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(payment.transactionCode, Modifier.weight(1f), color = B360Green, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(payment.payerName, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Text(payment.payerPhone, Modifier.weight(1f), color = Color.Gray, fontSize = 12.sp)
                            Text("KES ${String.format("%,.0f", payment.amount)}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("Mpesa", Modifier.weight(1f), fontSize = 12.sp)
                            Surface(color = statusColor.copy(0.1f), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                                Text(statusText, color = statusColor, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Text(payment.transactionDate.toString().take(10), Modifier.weight(1f), color = Color.Gray, fontSize = 12.sp)
                            if (!payment.reconciled) {
                                TextButton(onClick = { viewModel.reconcilePayment(payment.id, payment.orderId ?: "") }, modifier = Modifier.weight(0.6f)) {
                                    Text("Match", fontSize = 11.sp, color = B360Blue)
                                }
                            } else Spacer(Modifier.weight(0.6f))
                        }
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        }
    }
}

// ─── Reports ──────────────────────────────────────────────────────────────────

@Composable
fun DesktopReportsScreen(
    viewModel: ReportsViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadReport("This Month")
    }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val summary = state.profitSummary
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Screen Header
        ScreenHeader(
            title = "Reports",
            subtitle = "Overview of your business performance and insights."
        )

        // KPI cards row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Total Revenue",
                value = "KES ${String.format("%,.0f", summary?.totalRevenue ?: 0.0)}",
                change = "↑ 12% from last month",
                icon = Icons.Default.MonetizationOn,
                color = B360Green,
                bgColor = Color(0xFFE6F7F0)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Total Expenses",
                value = "KES ${String.format("%,.0f", summary?.totalExpenses ?: 100.0)}",
                change = "↓ 8% from last month",
                icon = Icons.Default.ShoppingBag,
                color = B360Red,
                bgColor = Color(0xFFFEE2E2),
                changeColor = B360Red
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Net Profit",
                value = "KES ${String.format("%,.0f", summary?.netProfit ?: -100.0)}",
                change = "↑ 15% from last month",
                icon = Icons.Default.TrendingUp,
                color = B360Blue,
                bgColor = Color(0xFFE0F2FE)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Gross Profit Margin",
                value = "${String.format("%.1f", (summary?.netMargin ?: 0.0) * 100)}%",
                change = "↑ 3% from last month",
                icon = Icons.Default.PieChart,
                color = B360Amber,
                bgColor = Color(0xFFFEF3C7)
            )
        }

        // Charts + lists row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.height(380.dp)) {
            // Revenue vs Expenses chart card
            Card(
                modifier = Modifier.weight(1.6f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Revenue vs Expenses",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color.White,
                            modifier = Modifier.clickable {}
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Daily", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(B360Green))
                            Text("Revenue (KES)", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(B360Red))
                            Text("Expenses (KES)", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Line Chart component
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        // Y-Axis labels
                        Column(
                            modifier = Modifier.fillMaxHeight().padding(bottom = 20.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Text("2.5K", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("2K", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("1.5K", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("1K", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("500", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text("0", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            RevenueExpenseLineChart(modifier = Modifier.weight(1f).fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mar 1", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("Mar 6", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("Mar 11", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("Mar 16", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("Mar 21", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("Mar 26", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                Text("Mar 31", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }

            // Expense Breakdown card
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Expense Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E293B)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Chart on the left
                        DonutChart(
                            modifier = Modifier.size(140.dp),
                            slices = listOf(
                                0.617f to Color(0xFF10B981),
                                0.206f to Color(0xFF3B82F6),
                                0.117f to Color(0xFF8B5CF6),
                                0.044f to Color(0xFFF59E0B),
                                0.016f to Color(0xFF14B8A6)
                            ),
                            centerText = "KES 100",
                            centerSubtext = "Total Expenses"
                        )
                        
                        // Legend List on the right
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DonutLegendRow(Color(0xFF10B981), "Stock Purchase", "45%", "KES 45,000")
                            DonutLegendRow(Color(0xFF3B82F6), "Rent", "15%", "KES 15,000")
                            DonutLegendRow(Color(0xFF8B5CF6), "Advertising", "8.5%", "KES 8,500")
                            DonutLegendRow(Color(0xFFF59E0B), "Delivery", "3.2%", "KES 3,200")
                            DonutLegendRow(Color(0xFF14B8A6), "Packaging", "1.2%", "KES 1,200")
                        }
                    }
                }
            }
        }

        // Bottom Actions Row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            BottomActionCard(
                modifier = Modifier.weight(1f),
                title = "Export PDF",
                subtitle = "Download as PDF",
                icon = Icons.Default.Description
            ) {
                exportDesktopFile(
                    "biashara360-profit-report.txt",
                    "Biashara360 Profit Report\nRevenue: KES ${summary?.totalRevenue ?: 0.0}\nExpenses: KES ${summary?.totalExpenses ?: 0.0}\nNet Profit: KES ${summary?.netProfit ?: 0.0}\nNet Margin: ${summary?.netMargin ?: 0.0}%"
                )
                toastMessage = "Profit report saved to Downloads."
            }
            BottomActionCard(
                modifier = Modifier.weight(1f),
                title = "Export Excel",
                subtitle = "Download as Excel",
                icon = Icons.Default.GridView
            ) {
                exportDesktopFile(
                    "biashara360-profit-report.csv",
                    "Metric,Value\nRevenue,${summary?.totalRevenue ?: 0.0}\nExpenses,${summary?.totalExpenses ?: 0.0}\nNet Profit,${summary?.netProfit ?: 0.0}\nNet Margin,${summary?.netMargin ?: 0.0}"
                )
                toastMessage = "Profit CSV saved to Downloads."
            }
            BottomActionCard(
                modifier = Modifier.weight(1f),
                title = "Share via WhatsApp",
                subtitle = "Send report to WhatsApp",
                icon = Icons.Default.Share
            ) {
                try {
                    if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                        java.awt.Desktop.getDesktop().browse(java.net.URI("https://wa.me/?text=Check%20out%20my%20Biashara360%20report!"))
                    } else {
                        toastMessage = "WhatsApp sharing is coming soon!"
                    }
                } catch (e: Exception) {
                    toastMessage = "WhatsApp sharing is coming soon!"
                }
            }
        }
    }

    if (toastMessage != null) {
        AlertDialog(
            onDismissRequest = { toastMessage = null },
            title = { Text("Feature Notification") },
            text = { Text(toastMessage!!) },
            confirmButton = {
                TextButton(onClick = { toastMessage = null }) {
                    Text("OK", color = B360Green)
                }
            }
        )
    }
}

// ─── Settings ─────────────────────────────────────────────────────────────────

enum class SettingsTab(val title: String, val icon: ImageVector) {
    General("General Settings", Icons.Default.Settings),
    Mpesa("M-Pesa Config", Icons.Default.Phone),
    CyberSource("CyberSource Config", Icons.Default.CreditCard),
    Receipt("Receipt Customization", Icons.Default.ReceiptLong)
}

@Composable
fun SettingsTabChip(
    tab: SettingsTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFFE6F7F0) else Color.White
    val border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
    val contentColor = if (isSelected) B360Green else Color(0xFF64748B)

    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = tab.title,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun DesktopSettingsScreen(
    viewModel: BusinessViewModel = remember { inject() }
) {
    var activeTab by remember { mutableStateOf(SettingsTab.Receipt) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Screen Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Manage your account, payment gateways, and application preferences",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }

        // Sub-tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsTab.values().forEach { tab ->
                SettingsTabChip(
                    tab = tab,
                    isSelected = activeTab == tab,
                    onClick = { activeTab = tab }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Active Tab Content
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (activeTab) {
                SettingsTab.General -> {
                    val scrollState = rememberScrollState()
                    val profileState by viewModel.profileState.collectAsState()
                    val mpesaState by viewModel.mpesaState.collectAsState()

                    LaunchedEffect(Unit) {
                        viewModel.loadProfile()
                        viewModel.loadMpesaConfig()
                    }

                    val profile = profileState.profile
                    val mpesaConfig = mpesaState.config

                    var nameInput by remember { mutableStateOf("") }
                    var phoneInput by remember { mutableStateOf("") }
                    var typeInput by remember { mutableStateOf("") }
                    var shortCodeInput by remember { mutableStateOf("") }

                    // Inline editing states
                    var isEditingName by remember { mutableStateOf(false) }
                    var isEditingPhone by remember { mutableStateOf(false) }
                    var isEditingType by remember { mutableStateOf(false) }
                    var isEditingShortCode by remember { mutableStateOf(false) }

                    LaunchedEffect(profile) {
                        if (profile != null) {
                            nameInput = profile.name
                            phoneInput = profile.phone
                            typeInput = profile.type
                        } else {
                            nameInput = "Wanjiru's Fashion"
                            phoneInput = "+254712345678"
                            typeInput = "Retail"
                        }
                    }

                    LaunchedEffect(mpesaConfig) {
                        if (mpesaConfig != null) {
                            shortCodeInput = mpesaConfig.shortCode
                        } else {
                            shortCodeInput = "174379"
                        }
                    }

                    var backendUrl by remember { mutableStateOf(com.app.biashara.data.remote.BASE_URL) }

                    fun saveField(fieldName: String, value: String) {
                        val currentProfile = profile ?: BusinessProfile(
                            id = "default",
                            name = nameInput,
                            owner = profile?.owner ?: "Wanjiru",
                            phone = phoneInput,
                            email = profile?.email ?: "wanjiru@fashion.com",
                            type = typeInput,
                            county = profile?.county ?: "Nairobi",
                            address = profile?.address ?: "Kenyatta Ave",
                            kraPin = profile?.kraPin ?: "",
                            paybillNumber = shortCodeInput,
                            accountNumber = profile?.accountNumber ?: "",
                            subscriptionTier = profile?.subscriptionTier ?: "Freemium"
                        )
                        when (fieldName) {
                            "name" -> {
                                viewModel.updateProfile(currentProfile.copy(name = value))
                                isEditingName = false
                            }
                            "phone" -> {
                                viewModel.updateProfile(currentProfile.copy(phone = value))
                                isEditingPhone = false
                            }
                            "type" -> {
                                viewModel.updateProfile(currentProfile.copy(type = value))
                                isEditingType = false
                            }
                            "shortCode" -> {
                                val configReq = MpesaConfigRequest(
                                    shortCode = value,
                                    callbackUrl = mpesaConfig?.callbackUrl ?: ""
                                )
                                viewModel.saveMpesaConfig(configReq)
                                isEditingShortCode = false
                            }
                        }
                    }

                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Section 1: Business Profile
                        SettingsSection(
                            title = "Business Profile",
                            subtitle = "View and update your business information",
                            icon = Icons.Default.Storefront
                        ) {
                            SettingsField(
                                label = "Business Name",
                                value = nameInput,
                                icon = Icons.Default.Business,
                                isEditing = isEditingName,
                                onEditToggle = { isEditingName = it },
                                onValueChange = { nameInput = it },
                                onSave = { saveField("name", nameInput) }
                            )
                            SettingsField(
                                label = "Owner Phone",
                                value = phoneInput,
                                icon = Icons.Default.Phone,
                                isEditing = isEditingPhone,
                                onEditToggle = { isEditingPhone = it },
                                onValueChange = { phoneInput = it },
                                onSave = { saveField("phone", phoneInput) }
                            )
                            SettingsField(
                                label = "Business Type",
                                value = typeInput,
                                icon = Icons.Default.LocalOffer,
                                isEditing = isEditingType,
                                onEditToggle = { isEditingType = it },
                                onValueChange = { typeInput = it },
                                onSave = { saveField("type", typeInput) }
                            )
                            SettingsField(
                                label = "Mpesa Short Code",
                                value = shortCodeInput,
                                icon = Icons.Default.PhoneAndroid,
                                isEditing = isEditingShortCode,
                                onEditToggle = { isEditingShortCode = it },
                                onValueChange = { shortCodeInput = it },
                                onSave = { saveField("shortCode", shortCodeInput) }
                            )
                        }

                        // Section 2: Security
                        SettingsSection(
                            title = "Security",
                            subtitle = "Manage your account security and notification preferences",
                            icon = Icons.Default.Shield
                        ) {
                            SettingsToggle("Two-Factor Authentication (2FA)", true, icon = Icons.Default.Lock)
                            SettingsToggle("Email Notifications", true, icon = Icons.Default.Email)
                            SettingsToggle("SMS Alerts", false, icon = Icons.Default.Message)
                        }

                        // Section 3: Subscription
                        SettingsSection(
                            title = "Subscription",
                            subtitle = "Manage your subscription plan and billing",
                            icon = Icons.Default.Star
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Current Plan",
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Freemium",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        runCatching {
                                            if (java.awt.Desktop.isDesktopSupported() &&
                                                java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)
                                            ) {
                                                java.awt.Desktop.getDesktop().browse(
                                                    java.net.URI("https://enw9p7mvty.us-east-1.awsapprunner.com/settings")
                                                )
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(B360Green),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Upgrade to Premium")
                                }
                            }
                        }

                        // Section 4: Backend Connectivity
                        SettingsSection(
                            title = "Backend Connectivity",
                            subtitle = "Configure your backend servers and environment endpoints",
                            icon = Icons.Default.Link
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = backendUrl,
                                    onValueChange = { backendUrl = it },
                                    label = { Text("Backend URL") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = B360Green,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )
                                Button(
                                    onClick = {
                                        val configDir = File(System.getProperty("user.home"), ".biashara360")
                                        if (!configDir.exists()) configDir.mkdirs()
                                        val configFile = File(configDir, "base_url.txt")
                                        configFile.writeText(backendUrl.trim())
                                        com.app.biashara.data.remote.BASE_URL = backendUrl.trim()
                                    },
                                    colors = ButtonDefaults.buttonColors(B360Green),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save Backend URL")
                                }
                            }
                        }
                    }
                }
                SettingsTab.Mpesa -> {
                    DesktopPaymentConfigurationScreen()
                }
                SettingsTab.CyberSource -> {
                    DesktopPaymentConfigurationScreen()
                }
                SettingsTab.Receipt -> {
                    DesktopReceiptTemplateScreen()
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    subtitle: String = "",
    icon: ImageVector = Icons.Default.Settings,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFECFDF5), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = B360Green,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))
            content()
        }
    }
}

@Composable
fun SettingsField(
    label: String,
    value: String,
    icon: ImageVector,
    isEditing: Boolean,
    onEditToggle: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.2f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
                fontSize = 14.sp
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.End
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.width(260.dp).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = B360Green,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = B360Green
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(44.dp)
                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFF1F5F9), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = value,
                        color = Color(0xFF1E293B),
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onEditToggle(true) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = B360Green
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggle(
    label: String,
    checked: Boolean,
    icon: ImageVector = Icons.Default.Settings,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    var state by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) { state = checked }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
                fontSize = 14.sp
            )
        }
        Switch(
            checked = state,
            onCheckedChange = {
                state = it
                onCheckedChange?.invoke(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = B360Green
            )
        )
    }
}

// ── Tax Screen ────────────────────────────────────────────────────────────────
@Composable
fun DesktopTaxScreen() {
    val taxTypes = listOf(
        Triple("VAT 16%", "KES 48,000", "Due 20th"),
        Triple("TOT 1.5%", "KES 4,500", "Due 20th"),
        Triple("WHT 3%", "KES 9,000", "Due 20th"),
    )
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Tax Management", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("VAT Liability" to "KES 48,000", "TOT Liability" to "KES 4,500", "Next Filing" to "Mar 20").forEach { (label, value) ->
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, fontSize = 12.sp, color = Color.Gray)
                        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = B360Green)
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tax Summary", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                taxTypes.forEach { (type, amount, due) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(type, fontSize = 14.sp)
                        Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(due, fontSize = 12.sp, color = Color.Gray)
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

// ── KRA iTax Screen ───────────────────────────────────────────────────────────
@Composable
fun DesktopKraScreen() {
    val returns = listOf(
        Triple("VAT3 - Feb 2025", "Submitted", "KES 48,000"),
        Triple("TOT - Feb 2025", "Pending", "KES 4,500"),
        Triple("WHT - Feb 2025", "Pending", "KES 9,000"),
    )
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("KRA iTax Integration", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Compliance Score" to "87%", "eTIMS Invoices" to "142", "Pending Returns" to "2").forEach { (label, value) ->
                Card(Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, fontSize = 12.sp, color = Color.Gray)
                        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = B360Green)
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tax Returns", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                        Text("Download CSV", fontSize = 13.sp)
                    }
                }
                returns.forEach { (name, status, amount) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontSize = 14.sp)
                        Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        val statusColor = if (status == "Submitted") B360Green else Color(0xFFFF8F00)
                        Text(status, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

// ── Social Inbox Screen ───────────────────────────────────────────────────────
@Composable
fun DesktopSocialScreen() {
    val conversations = listOf(
        Triple("Amara Osei", "WhatsApp", "Do you have Nike size 42?"),
        Triple("Fatuma Amin", "Instagram", "What's the price of the dress?"),
        Triple("James Kariuki", "Facebook", "Can I pay via Mpesa?"),
        Triple("Grace Mwangi", "TikTok", "Hi, I want to order 2 pieces"),
    )
    val platformColor = mapOf("WhatsApp" to Color(0xFF25D366), "Instagram" to Color(0xFFE1306C), "Facebook" to Color(0xFF1877F2), "TikTok" to Color(0xFF000000))

    Row(Modifier.fillMaxSize()) {
        // Conversation list
        Column(Modifier.width(320.dp).fillMaxHeight().background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Unified Inbox", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            HorizontalDivider()
            conversations.forEach { (name, platform, msg) ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(platformColor[platform] ?: B360Green), contentAlignment = Alignment.Center) {
                            Text(name.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(msg, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                        }
                        Text(platform, fontSize = 10.sp, color = platformColor[platform] ?: B360Green)
                    }
                }
            }
        }
        VerticalDivider(Modifier.fillMaxHeight().width(1.dp))
        // Chat panel placeholder
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Forum, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Select a conversation", color = Color.Gray, fontSize = 16.sp)
        }
    }
}

// ─── CyberSource Settings Screen ──────────────────────────────────────────────
@Composable
fun DesktopCyberSourceSettingsScreen() {
    var merchantId by remember { mutableStateOf("WanFashion_CS_098") }
    var merchantKeyId by remember { mutableStateOf("9c7c25eb-42f8-4a52-b8bb-69d2d0c2e39b") }
    var merchantSecretKey by remember { mutableStateOf("••••••••••••••••••••••••••••••••") }
    var isSandbox by remember { mutableStateOf(true) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("CyberSource Configuration", fontWeight = FontWeight.Bold, fontSize = 22.sp)

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("API Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                HorizontalDivider()

                // Merchant ID
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Merchant ID (Organization ID)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = merchantId,
                        onValueChange = { merchantId = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("e.g. wanfashion_cs_098") }
                    )
                }

                // Key ID
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Active Key ID (REST API JWT/P12 Key ID)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = merchantKeyId,
                        onValueChange = { merchantKeyId = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("e.g. 9c7c25eb-xxxx-xxxx-xxxx-xxxxxxx") }
                    )
                }

                // Secret Key
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Shared Secret Key (Rest API Shared Secret)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = merchantSecretKey,
                        onValueChange = { merchantSecretKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        placeholder = { Text("Enter your secure merchant shared secret key") }
                    )
                }

                // Environment toggle
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Sandbox Environment", fontWeight = FontWeight.Medium)
                        Text("Toggle off to deploy credentials on live CyberSource production rails", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = isSandbox,
                        onCheckedChange = { isSandbox = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = B360Green
                        )
                    )
                }

                HorizontalDivider()

                if (showSuccessMessage) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(B360Green.copy(0.15f))
                            .padding(14.dp)
                    ) {
                        Text("✓ CyberSource configuration validated and saved successfully!", color = B360Green, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            try {
                                val configDir = java.io.File(System.getProperty("user.home"), ".biashara360")
                                if (!configDir.exists()) configDir.mkdirs()
                                val configFile = java.io.File(configDir, "cybersource_config.json")
                                configFile.writeText(
                                    "{\"merchantId\":\"$merchantId\",\"merchantKeyId\":\"$merchantKeyId\",\"sandbox\":$isSandbox}"
                                )
                            } catch (_: Exception) { /* best-effort */ }
                            showSuccessMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(180.dp)
                    ) {
                        Text("Save Configuration", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopMpesaScreen(
    viewModel: BusinessViewModel = remember { inject() }
) {
    val state by viewModel.mpesaState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadMpesaConfig()
    }

    var shortCode by remember { mutableStateOf("") }
    var callbackUrl by remember { mutableStateOf("") }
    var environment by remember { mutableStateOf("sandbox") }
    var accountType by remember { mutableStateOf("paybill") }

    LaunchedEffect(state.config) {
        state.config?.let { cfg ->
            shortCode = cfg.shortCode
            callbackUrl = cfg.callbackUrl
            environment = cfg.environment
            accountType = cfg.accountType
        }
    }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "M-Pesa Integration Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Configure your Safaricom Daraja API keys for real-time mobile checkout",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
        } else {
            // Error Card
            state.error?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = Color(0xFFB91C1C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Success Card
            if (state.saveSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ M-Pesa configuration saved successfully!",
                        color = Color(0xFF065F46),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(
                        text = "Daraja API Configurations",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E293B)
                    )
                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text("Consumer credentials and the Lipa na M-Pesa passkey are managed globally by the backend.", color = Color(0xFF64748B), fontSize = 12.sp)

                    // Shortcode
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Business Shortcode (Paybill / Till) *", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF64748B))
                        OutlinedTextField(
                            value = shortCode,
                            onValueChange = { shortCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            placeholder = { Text("e.g. 174379") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    // Callback URL
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Callback URL *", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF64748B))
                        OutlinedTextField(
                            value = callbackUrl,
                            onValueChange = { callbackUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            placeholder = { Text("https://api.yourdomain.com/v1/payments/mpesa/callback") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = B360Green,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    // Select Dropdowns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Environment Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            var envExpanded by remember { mutableStateOf(false) }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Environment", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF64748B))
                                Box {
                                    OutlinedTextField(
                                        value = if (environment == "sandbox") "Sandbox" else "Production",
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            IconButton(onClick = { envExpanded = true }) {
                                                Icon(Icons.Filled.ArrowDropDown, null)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = B360Green,
                                            unfocusedBorderColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                    DropdownMenu(expanded = envExpanded, onDismissRequest = { envExpanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Sandbox") },
                                            onClick = { environment = "sandbox"; envExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Production") },
                                            onClick = { environment = "production"; envExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        // Account Type Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            var typeExpanded by remember { mutableStateOf(false) }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Account Type", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF64748B))
                                Box {
                                    OutlinedTextField(
                                        value = if (accountType == "paybill") "Paybill (C2B / LNM)" else "Buy Goods Till",
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            IconButton(onClick = { typeExpanded = true }) {
                                                Icon(Icons.Filled.ArrowDropDown, null)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = B360Green,
                                            unfocusedBorderColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                        DropdownMenuItem(
                                            text = { Text("Paybill (C2B / LNM)") },
                                            onClick = { accountType = "paybill"; typeExpanded = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Buy Goods Till") },
                                            onClick = { accountType = "till"; typeExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Save Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveMpesaConfig(
                                    MpesaConfigRequest(
                                        shortCode = shortCode,
                                        callbackUrl = callbackUrl,
                                        environment = environment,
                                        accountType = accountType
                                    )
                                )
                            },
                            enabled = !state.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(180.dp)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text("Save Config", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopReceiptTemplateScreen(
    viewModel: BusinessViewModel = remember { inject() }
) {
    val state by viewModel.profileState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    var header by remember { mutableStateOf("Welcome to our store!") }
    var footer by remember { mutableStateOf("Thank you for shopping with us!") }
    var showTax by remember { mutableStateOf(true) }
    var showCustomer by remember { mutableStateOf(true) }

    LaunchedEffect(state.profile) {
        state.profile?.let { prof ->
            header = prof.receiptHeader.take(60)
            footer = prof.receiptFooter.take(60)
            showTax = prof.receiptShowTax
            showCustomer = prof.receiptShowCustomer
        }
    }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Receipt Template Customization",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Personalize the layout and details of your thermal receipts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }

            Button(
                onClick = {
                    state.profile?.let { prof ->
                        viewModel.updateProfile(
                            prof.copy(
                                receiptHeader = header,
                                receiptFooter = footer,
                                receiptShowTax = showTax,
                                receiptShowCustomer = showCustomer
                            )
                        )
                    }
                },
                enabled = !state.isSaving && state.profile != null,
                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(180.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Save Template", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
        } else {
            // Error Card
            state.error?.let { err ->
                val (title, description) = if (err.lowercase() == "unauthorized" || err.contains("Admin") || err.contains("Unauthorized")) {
                    "Admin access required" to "Only administrators can customize the receipt template."
                } else {
                    "Error" to err
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = title,
                                color = Color(0xFF991B1B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = description,
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Success Card
            if (state.saveSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Receipt template customized successfully!",
                        color = Color(0xFF065F46),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Side: Editor Form
                Card(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(
                            text = "Receipt Parameters",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1E293B)
                        )
                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Header Message
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Header Message", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                            Text("Message displayed at the top of the receipt", fontSize = 11.sp, color = Color(0xFF64748B))
                            Spacer(Modifier.height(2.dp))
                            OutlinedTextField(
                                value = header,
                                onValueChange = { if (it.length <= 60) header = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                trailingIcon = {
                                    Text("${header.length} / 60", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = B360Green,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                        }

                        // Footer Note
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Footer Note", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                            Text("Message displayed at the bottom of the receipt", fontSize = 11.sp, color = Color(0xFF64748B))
                            Spacer(Modifier.height(2.dp))
                            OutlinedTextField(
                                value = footer,
                                onValueChange = { if (it.length <= 60) footer = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                trailingIcon = {
                                    Text("${footer.length} / 60", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = B360Green,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Show Tax Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show KRA Tax Breakdown", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1E293B))
                                Text("Include VAT (16%) details on thermal receipts", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            Switch(
                                checked = showTax,
                                onCheckedChange = { showTax = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = B360Green,
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFFE2E8F0)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        // Show Customer Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Show Customer Details", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1E293B))
                                Text("Include buyer name and phone on receipt header", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            Switch(
                                checked = showCustomer,
                                onCheckedChange = { showCustomer = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = B360Green,
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFFE2E8F0)
                                )
                            )
                        }
                    }
                }

                // Right Side: Live Thermal Receipt Preview
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE RECEIPT PREVIEW",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color.White,
                            modifier = Modifier.clickable {}
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = B360Green,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Preview Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = B360Green)
                            }
                        }
                    }

                    val profile = state.profile
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFF0)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Header business details
                            Text(
                                text = profile?.name?.uppercase() ?: "BIASHARA STORE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Black
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = profile?.address ?: "123 Tom Mboya St",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "${profile?.county ?: "Nairobi"}, Kenya",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Tel: ${profile?.phone ?: "+254 700 000 000"}",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                                if (!profile?.kraPin.isNullOrBlank()) {
                                    Text(
                                        text = "PIN: ${profile?.kraPin}",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            // Dashed Divider
                            Text(
                                text = "------------------------------------------",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Gray
                            )

                            // Customer Section if enabled
                            if (showCustomer) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "CUSTOMER: John Doe",
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "PHONE: +254 712 222 333",
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.DarkGray
                                    )
                                }
                                Text(
                                    text = "------------------------------------------",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }

                            // Items Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ITEM",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black
                                )
                                Text(
                                    text = "QTY  •  PRICE  •  TOTAL",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black
                                )
                            }

                            Text(
                                text = "------------------------------------------",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Gray
                            )

                            // Sample Items
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Men's Slim Fit Jeans",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "1  •  2,500.00  •  2,500.00",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Casual Cotton Shirt",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "2  •  1,200.00  •  2,400.00",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                            }

                            Text(
                                text = "------------------------------------------",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Gray
                            )

                            // Calculation totals
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "SUBTOTAL:",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "KES 4,900.00",
                                        fontSize = 11.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }

                                val subtotalVal = 4900.0
                                val vatVal = subtotalVal * 0.16
                                val totalVal = if (showTax) subtotalVal + vatVal else subtotalVal

                                if (showTax) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "VAT (16%):",
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = "KES ${String.format("%,.2f", vatVal)}",
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = Color.DarkGray
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "TOTAL:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "KES ${String.format("%,.2f", totalVal)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.Black
                                    )
                                }
                            }

                            Text(
                                text = "------------------------------------------",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Gray
                            )

                            // Custom message header and footer
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = header,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Text(
                                    text = footer,
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "eTIMS Invoice: #INV-2026-0091",
                                    fontSize = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Powered by Biashara360",
                                    fontSize = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
