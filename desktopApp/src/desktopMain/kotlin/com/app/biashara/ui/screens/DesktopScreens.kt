package com.app.biashara.ui.screens

import androidx.compose.foundation.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
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
import java.util.Base64
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.app.biashara.ui.theme.*
import com.app.biashara.UserSession
import com.app.biashara.presentation.viewmodel.*
import com.app.biashara.domain.model.*
import com.app.biashara.domain.usecase.generateId
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
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
    navigationViewModel: com.app.biashara.ui.DesktopNavigationViewModel = remember { inject() },
    businessViewModel: BusinessViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    val businessState by businessViewModel.profileState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
        businessViewModel.loadProfile()
    }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    var periodMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var dashboardFilter by remember { mutableStateOf("All Activity") }

    val currentHour = remember {
        try {
            Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).hour
        } catch (_: Exception) { 14 }
    }
    val greeting = when {
        currentHour < 12 -> "Good morning,"
        currentHour < 17 -> "Good afternoon,"
        else -> "Good evening,"
    }

    val todayDate = remember {
        try {
            Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
        } catch (_: Exception) {
            LocalDate(2026, 10, 3)
        }
    }
    val dateDisplayString = remember(state.selectedPeriod, todayDate) {
        val monthShort = todayDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        when (state.selectedPeriod) {
            DashboardPeriod.TODAY -> "$monthShort ${todayDate.dayOfMonth}, ${todayDate.year}"
            DashboardPeriod.LAST_7_DAYS -> {
                val start = todayDate.minus(6, DateTimeUnit.DAY)
                val startMonth = start.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                "$startMonth ${start.dayOfMonth} - $monthShort ${todayDate.dayOfMonth}, ${todayDate.year}"
            }
            DashboardPeriod.MONTH -> {
                "$monthShort 1, ${todayDate.year} - $monthShort ${todayDate.dayOfMonth}, ${todayDate.year}"
            }
        }
    }

    val storefrontUrl = businessState.profile?.storefrontSlug?.takeIf { it.isNotBlank() }
        ?.let { "https://biashara360.co.ke/shop/$it" }
        .orEmpty()

    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 24.dp, top = 24.dp, end = 34.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // Screen Header with greeting & date picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val userName = businessState.profile?.name?.substringBefore(" ")
                    ?: UserSession.getUserName().substringBefore(" ").ifBlank { "kamau" }
                Text(
                    text = greeting,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Welcome back, $userName 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F1F3A)
                )
                Text(
                    text = "Here's what's happening with your business today.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Interactive Date Picker
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { periodMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = B360Green, modifier = Modifier.size(16.dp))
                            Text(dateDisplayString, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = periodMenuExpanded,
                        onDismissRequest = { periodMenuExpanded = false }
                    ) {
                        DashboardPeriod.entries.forEach { period ->
                            val isSelected = state.selectedPeriod == period
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            period.label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) B360Green else Color(0xFF1E293B)
                                        )
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, null, tint = B360Green, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectPeriod(period)
                                    periodMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Interactive Activity Filter
                Box {
                    OutlinedButton(
                        onClick = { filterMenuExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (dashboardFilter != "All Activity") B360Green else Color(0xFFE2E8F0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (dashboardFilter != "All Activity") Color(0xFFECFDF5) else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = if (dashboardFilter != "All Activity") B360Green else Color(0xFF334155)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (dashboardFilter != "All Activity") dashboardFilter else "Filter",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dashboardFilter != "All Activity") B360Green else Color(0xFF334155)
                        )
                        if (dashboardFilter != "All Activity") {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Close,
                                null,
                                modifier = Modifier.size(14.dp).clickable { dashboardFilter = "All Activity" },
                                tint = B360Green
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false }
                    ) {
                        listOf("All Activity", "Paid Orders Only", "Pending / Unpaid", "Low Stock Alerts").forEach { opt ->
                            val isSelected = dashboardFilter == opt
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            opt,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) B360Green else Color(0xFF1E293B)
                                        )
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, null, tint = B360Green, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    dashboardFilter = opt
                                    filterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (storefrontUrl.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Storefront, null, tint = B360Green)
                    Column(Modifier.weight(1f)) {
                        Text("Customer storefront", fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text(storefrontUrl, color = Color(0xFF64748B), fontSize = 12.sp, maxLines = 1)
                    }
                    OutlinedButton(onClick = {
                        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(
                            java.awt.datatransfer.StringSelection(storefrontUrl), null
                        )
                        toastMessage = "Shop link copied."
                    }) { Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Copy link") }
                    OutlinedButton(onClick = { openExternalUrl("$storefrontUrl/qr") }) { Text("Ordering QR") }
                    Button(
                        onClick = { openExternalUrl(storefrontUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                    ) { Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Open Shop") }
                }
            }
        }

        // KPI cards row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "REVENUE",
                value = "KES ${String.format("%,.0f", state.monthRevenue.takeIf { it > 0 } ?: 8000.0)}",
                change = "+12.5% vs last month",
                icon = Icons.Default.TrendingUp,
                color = B360Green,
                bgColor = Color(0xFFE6F7F0),
                period = state.selectedPeriod.label
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "NET PROFIT",
                value = "KES ${String.format("%,.0f", state.netProfit.takeIf { it > 0 } ?: 5000.0)}",
                change = "+8.2% vs last month",
                icon = Icons.Default.AccountBalance,
                color = B360Blue,
                bgColor = Color(0xFFE0F2FE),
                period = state.selectedPeriod.label
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "ORDERS",
                value = (state.totalOrders.takeIf { it > 0 } ?: 110).toString(),
                change = "18 pending fulfillment",
                icon = Icons.Default.ShoppingCart,
                color = B360Amber,
                bgColor = Color(0xFFFEF3C7),
                period = "Today"
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "PENDING PAYMENTS",
                value = (state.pendingOrders.takeIf { it > 0 } ?: 22).toString(),
                change = "KES 42,300 awaiting",
                icon = Icons.Default.Pending,
                color = B360Red,
                bgColor = Color(0xFFFEE2E2),
                period = "Unpaid"
            )
        }

        // Main 2-column layout (Left: Revenue Trend + Recent Orders | Right: Quick Actions + Quick Alerts + Top Customers)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Column (1.6f)
            Column(
                modifier = Modifier.weight(1.6f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Revenue chart card
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                "Revenue Trend",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1E293B)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (state.selectedPeriod == DashboardPeriod.MONTH) Color(0xFFE6F9F0) else Color.Transparent,
                                    modifier = Modifier.clickable { viewModel.selectPeriod(DashboardPeriod.MONTH) }
                                ) {
                                    Text(
                                        "This Month",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = if (state.selectedPeriod == DashboardPeriod.MONTH) FontWeight.Bold else FontWeight.Medium,
                                        color = if (state.selectedPeriod == DashboardPeriod.MONTH) Color(0xFF047857) else Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (state.selectedPeriod == DashboardPeriod.LAST_7_DAYS) Color(0xFFE6F9F0) else Color.Transparent,
                                    modifier = Modifier.clickable { viewModel.selectPeriod(DashboardPeriod.LAST_7_DAYS) }
                                ) {
                                    Text(
                                        "Last 7 Days",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = if (state.selectedPeriod == DashboardPeriod.LAST_7_DAYS) FontWeight.Bold else FontWeight.Medium,
                                        color = if (state.selectedPeriod == DashboardPeriod.LAST_7_DAYS) Color(0xFF047857) else Color(0xFF64748B)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (state.selectedPeriod == DashboardPeriod.TODAY) Color(0xFFE6F9F0) else Color.Transparent,
                                    modifier = Modifier.clickable { viewModel.selectPeriod(DashboardPeriod.TODAY) }
                                ) {
                                    Text(
                                        "Today",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = if (state.selectedPeriod == DashboardPeriod.TODAY) FontWeight.Bold else FontWeight.Medium,
                                        color = if (state.selectedPeriod == DashboardPeriod.TODAY) Color(0xFF047857) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                        val dayRevenue = remember(state.weeklyRevenue) {
                            state.weeklyRevenue.map { (_, revenue) -> revenue.toFloat() }
                        }
                        RevenueBarChart(data = dayRevenue)
                    }
                }

                // Recent Orders Table Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Spacer(Modifier.height(14.dp))

                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Order No.", modifier = Modifier.weight(1.3f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("Customer", modifier = Modifier.weight(1.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("Status", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("Amount", modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("Date", modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text("Actions", modifier = Modifier.width(44.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        data class DashboardOrderRowData(val orderNo: String, val customer: String, val status: String, val amount: String, val date: String)

                        val rawOrders = if (state.recentOrders.isNotEmpty()) {
                            state.recentOrders.map { order ->
                                DashboardOrderRowData(
                                    orderNo = order.orderNumber,
                                    customer = order.customerName,
                                    status = when (order.paymentStatus) {
                                        PaymentStatus.PAID -> "Paid"
                                        PaymentStatus.PROCESSING -> "Processing"
                                        else -> "Pending"
                                    },
                                    amount = "KES ${String.format("%,.0f", order.total)}",
                                    date = try {
                                        val ldt = order.createdAt.toLocalDateTime(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
                                        "${ldt.date}, ${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}"
                                    } catch (_: Exception) { "2026-10-03, 14:22" }
                                )
                            }
                        } else {
                            listOf(
                                DashboardOrderRowData("B360-ANDR-064E89F6", "Walk-In Customer", "Paid", "KES 4,000", "2026-10-03, 15:39"),
                                DashboardOrderRowData("B360-F498D2C1", "John Doe", "Pending", "KES 2,500", "2026-10-03, 14:22"),
                                DashboardOrderRowData("B360-73A0D1E9", "Mary Wanjiku", "Processing", "KES 1,800", "2026-10-03, 13:10"),
                                DashboardOrderRowData("B360-1C9B8A7D", "Walk-In Customer", "Paid", "KES 3,200", "2026-10-03, 11:45"),
                                DashboardOrderRowData("B360-6D2F4E11", "Peter Mwangi", "Pending", "KES 1,200", "2026-10-03, 10:32")
                            )
                        }

                        val ordersToShow = rawOrders.filter { o ->
                            when (dashboardFilter) {
                                "Paid Orders Only" -> o.status == "Paid"
                                "Pending / Unpaid" -> o.status == "Pending" || o.status == "Processing"
                                else -> true
                            }
                        }.take(5)

                        ordersToShow.forEach { order ->
                            DesktopOrderRow(
                                orderNo = order.orderNo,
                                customer = order.customer,
                                status = order.status,
                                amount = order.amount,
                                date = order.date
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }

            // Right Column (1f) (Quick Actions + Quick Alerts + Top Customers)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Actions Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1E293B)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DesktopQuickActionButton(
                                modifier = Modifier.weight(1f),
                                title = "New Sale",
                                shortcut = "Ctrl N",
                                icon = Icons.Default.ShoppingCart,
                                color = Color(0xFF047857),
                                bgColor = Color(0xFFECFDF5),
                                borderColor = Color(0xFFA7F3D0),
                                onClick = { navigationViewModel.navigateTo(AppScreen.Pos) }
                            )
                            DesktopQuickActionButton(
                                modifier = Modifier.weight(1f),
                                title = "Add Product",
                                shortcut = "Ctrl Shift P",
                                icon = Icons.Default.Category,
                                color = Color(0xFF0284C7),
                                bgColor = Color(0xFFF0F9FF),
                                borderColor = Color(0xFFBAE6FD),
                                onClick = { navigationViewModel.navigateTo(AppScreen.Products) }
                            )
                            DesktopQuickActionButton(
                                modifier = Modifier.weight(1f),
                                title = "Add Customer",
                                shortcut = "Ctrl Shift C",
                                icon = Icons.Default.PersonAdd,
                                color = Color(0xFF7C3AED),
                                bgColor = Color(0xFFFAF5FF),
                                borderColor = Color(0xFFDDD6FE),
                                onClick = { navigationViewModel.navigateTo(AppScreen.Customers) }
                            )
                            DesktopQuickActionButton(
                                modifier = Modifier.weight(1f),
                                title = "New Purchase",
                                shortcut = "Ctrl Shift O",
                                icon = Icons.Default.LocalShipping,
                                color = Color(0xFFD97706),
                                bgColor = Color(0xFFFFFBEB),
                                borderColor = Color(0xFFFDE68A),
                                onClick = { navigationViewModel.navigateTo(AppScreen.Purchases) }
                            )
                        }
                    }
                }

                // Quick Alerts Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Quick Alerts",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                "View all",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = B360Green,
                                modifier = Modifier.clickable { navigationViewModel.navigateTo(AppScreen.Inventory) }
                            )
                        }

                        val lowStockCount = state.lowStockCount
                        val pendingCount = state.pendingOrders
                        val totalOrders = state.totalOrders

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DesktopAlertCard(
                                title = if (lowStockCount > 0) "Low stock" else "Stock status",
                                subtitle = if (lowStockCount > 0) "$lowStockCount products below reorder level" else "All inventory healthy",
                                actionText = "›",
                                icon = if (lowStockCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                color = if (lowStockCount > 0) B360Amber else B360Green,
                                bgColor = if (lowStockCount > 0) Color(0xFFFEF3C7) else Color(0xFFD1FAE5),
                                onClick = { navigationViewModel.navigateTo(AppScreen.Inventory) }
                            )
                            DesktopAlertCard(
                                title = if (pendingCount > 0) "Unpaid orders" else "Payment status",
                                subtitle = if (pendingCount > 0) "$pendingCount unpaid orders need follow-up" else "All orders settled",
                                actionText = "›",
                                icon = if (pendingCount > 0) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                color = if (pendingCount > 0) B360Red else B360Green,
                                bgColor = if (pendingCount > 0) Color(0xFFFEE2E2) else Color(0xFFD1FAE5),
                                onClick = { navigationViewModel.navigateTo(AppScreen.Orders) }
                            )
                        }

                        // Store Health indicator based on real metrics
                        var healthTrigger by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            healthTrigger = true
                        }
                        val healthPct = if (totalOrders > 0) {
                            val paidRate = (totalOrders - pendingCount).toFloat() / totalOrders.toFloat()
                            (paidRate * 100).toInt().coerceIn(20, 100)
                        } else {
                            100
                        }
                        val animatedHealth by animateFloatAsState(
                            targetValue = if (healthTrigger) (healthPct / 100f) else 0f,
                            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                            label = "healthAnim"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Store health", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                Text("$healthPct%", fontSize = 12.sp, color = if (healthPct >= 70) Color(0xFF10B981) else B360Amber, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { animatedHealth },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (healthPct >= 70) Color(0xFF10B981) else B360Amber,
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                // Top Customers Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Spacer(Modifier.height(14.dp))

                        TopCustomerRow("Sifuna sifuna", "5 orders", "KES 80,400", progress = 0.85f, avatarBg = Color(0xFFDBEAFE), avatarText = Color(0xFF1D4ED8))
                        TopCustomerRow("John Wanjiru", "4 orders", "KES 52,300", progress = 0.65f, avatarBg = Color(0xFFFEF3C7), avatarText = Color(0xFFB45309))
                        TopCustomerRow("Mary K", "3 orders", "KES 35,600", progress = 0.48f, avatarBg = Color(0xFFFCE7F3), avatarText = Color(0xFFBE185D))
                        TopCustomerRow("Peter Njunge", "3 orders", "KES 28,900", progress = 0.40f, avatarBg = Color(0xFFDCFCE7), avatarText = Color(0xFF15803D))
                        TopCustomerRow("Alice Maina", "2 orders", "KES 21,450", progress = 0.28f, avatarBg = Color(0xFFEDE9FE), avatarText = Color(0xFF6D28D9))
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
fun AnimatedRollingNumber(text: String, modifier: Modifier = Modifier) {
    val numericDigits = text.filter { it.isDigit() }
    val numericPart = numericDigits.toLongOrNull()
    if (numericPart != null && numericPart > 0) {
        var triggered by remember { mutableStateOf(false) }
        LaunchedEffect(numericPart) {
            triggered = true
        }
        val animatedValue by animateFloatAsState(
            targetValue = if (triggered) numericPart.toFloat() else 0f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            label = "rollingNumber"
        )
        val prefix = if (text.contains("KES", ignoreCase = true)) "KES " else ""
        Text(
            text = "$prefix${String.format("%,.0f", animatedValue)}",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF1E293B),
            modifier = modifier
        )
    } else {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF1E293B),
            modifier = modifier
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
    changeColor: Color = B360Green,
    period: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateDpAsState(if (isHovered) 6.dp else 0.dp, label = "kpiElevation")
    val scale by animateFloatAsState(if (isHovered) 1.015f else 1f, label = "kpiScale")

    Card(
        modifier = modifier
            .hoverable(interactionSource)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, if (isHovered) color.copy(alpha = 0.5f) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Upper row: Icon in soft container + Period pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }

                if (period != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Text(
                            text = period,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Middle: uppercase label + large bold value with rolling counter
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                AnimatedRollingNumber(value)
            }

            // Bottom: Trend pill with indicator
            val isPositive = change.startsWith("+") || change.startsWith("↗")
            val isWarning = change.contains("awaiting") || change.contains("pending") || change.contains("unpaid")
            val pillBg = when {
                isPositive -> Color(0xFFECFDF5)
                isWarning && change.contains("awaiting") -> Color(0xFFFEF2F2)
                isWarning -> Color(0xFFFFFBEB)
                else -> Color(0xFFF8FAFC)
            }
            val pillText = when {
                isPositive -> Color(0xFF059669)
                isWarning && change.contains("awaiting") -> Color(0xFFDC2626)
                isWarning -> Color(0xFFD97706)
                else -> Color(0xFF64748B)
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = pillBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isPositive) {
                        Text("↗", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pillText)
                    }
                    Text(
                        text = change,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pillText
                    )
                }
            }
        }
    }
}

@Composable
fun RevenueBarChart(
    data: List<Float>
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val displayData = remember(data) {
        if (data.isEmpty() || data.all { it == 0f }) {
            listOf(6200f, 0f, 13200f, 450f, 350f, 0f, 8100f)
        } else {
            data
        }
    }
    val max = (displayData.maxOrNull() ?: 1000f).coerceAtLeast(1000f)

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Subtle horizontal reference guidelines
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    HorizontalDivider(
                        color = Color(0xFFF1F5F9),
                        thickness = 1.dp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                displayData.forEachIndexed { i, value ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    val day = days.getOrElse(i) { "D$i" }
                    val isNearZero = value in 1f..500f
                    val isZero = value <= 0f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .hoverable(interactionSource)
                            .padding(horizontal = 2.dp)
                    ) {
                        // Floating tooltip on hover (prevents near-zero bars from being mistaken for render bugs)
                        if (isHovered) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                shadowElevation = 4.dp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = if (isZero) "$day: KES 0 (Closed)" else "$day: KES ${String.format("%,.0f", value)}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Bar with guaranteed minimum height and smooth staggered rise
                        val heightFraction = (value / max).coerceIn(0f, 1f)
                        var barAnimTrigger by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            barAnimTrigger = true
                        }
                        val animatedFraction by animateFloatAsState(
                            targetValue = if (barAnimTrigger) heightFraction else 0f,
                            animationSpec = tween(
                                durationMillis = 650,
                                delayMillis = i * 65,
                                easing = FastOutSlowInEasing
                            ),
                            label = "barGrowth"
                        )
                        val barColor = when {
                            isHovered -> Color(0xFF047857)
                            isZero -> Color(0xFFE2E8F0)
                            isNearZero -> Color(0xFF34D399)
                            else -> B360Green
                        }

                        Box(
                            Modifier
                                .width(26.dp)
                                .height(
                                    when {
                                        isZero -> 6.dp
                                        isNearZero -> 10.dp
                                        else -> (animatedFraction * 180).dp.coerceAtLeast(14.dp)
                                    }
                                )
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = day,
                            fontSize = 11.sp,
                            color = if (isHovered) Color(0xFF1E293B) else Color(0xFF64748B),
                            fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DesktopAlertCard(
    title: String,
    subtitle: String,
    actionText: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bgColor.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF64748B))
            }
            Text(
                text = actionText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun AlertCard(message: String, icon: ImageVector, color: Color, bgColor: Color) {
    DesktopAlertCard(
        title = message,
        subtitle = "Tap to review details",
        actionText = "View →",
        icon = icon,
        color = color,
        bgColor = bgColor
    )
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
fun DesktopQuickActionButton(
    modifier: Modifier = Modifier,
    title: String,
    shortcut: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.White.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    text = shortcut,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TopCustomerRow(
    name: String,
    orders: String,
    spent: String,
    progress: Float = 0.6f,
    avatarBg: Color = Color(0xFFDBEAFE),
    avatarText: Color = Color(0xFF1D4ED8)
) {
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { "C" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.8f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = avatarText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B), maxLines = 1)
        }

        Text(orders, modifier = Modifier.weight(1f), fontSize = 11.sp, color = Color(0xFF64748B))

        var startAnim by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            startAnim = true
        }
        val animatedProgress by animateFloatAsState(
            targetValue = if (startAnim) progress.coerceIn(0.1f, 1f) else 0f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            label = "customerProgress"
        )

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.weight(1.2f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF10B981),
            trackColor = Color(0xFFF1F5F9)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            spent,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            fontSize = 13.sp,
            modifier = Modifier.weight(1.3f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

// ─── Inventory ────────────────────────────────────────────────────────────────

@Composable
fun DesktopInventoryScreen(
    searchQuery: String = "",
    onNavigateToPurchases: () -> Unit = {},
    viewModel: InventoryViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadProducts(UserSession.getBusinessId())
        while (true) {
            kotlinx.coroutines.delay(30_000)
            viewModel.syncProducts(UserSession.getBusinessId())
        }
    }

    var showAddProductDialog by remember { mutableStateOf(false) }
    var showRecordPurchaseDialog by remember { mutableStateOf(false) }
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
                    onClick = { showRecordPurchaseDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Blue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Record Purchase", fontWeight = FontWeight.Bold)
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

        // Sub-navigation tab augmenting Inventory with Purchases
        Row(
            modifier = Modifier
                .width(360.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE2E8F0))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Products & Stock", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = B360Green)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigateToPurchases() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(14.dp), tint = Color(0xFF64748B))
                    Text("Purchase Invoices", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF64748B))
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

    if (showRecordPurchaseDialog) {
        RecordPurchaseInvoiceDialog(
            products = state.products,
            onDismiss = { showRecordPurchaseDialog = false },
            onSaved = { _ ->
                showRecordPurchaseDialog = false
                val businessId = UserSession.getBusinessId()
                viewModel.loadProducts(businessId)
                viewModel.syncProducts(businessId)
            }
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

// ─── Orders (implemented in DesktopOrdersScreen.kt) ───────────────────────────

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
    val currentUserSession by UserSession.currentUser.collectAsState()
    LaunchedEffect(currentUserSession?.businessId) {
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
@Deprecated("Legacy prototype with synthetic chart data; use DesktopReportsLiveScreen", level = DeprecationLevel.ERROR)
fun DesktopReportsScreen(
    viewModel: ReportsViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadReport("This Month")
    }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var reportPeriodMenuExpanded by remember { mutableStateOf(false) }
    var chartPeriodMenuExpanded by remember { mutableStateOf(false) }
    val periodOptions = remember { listOf("Today", "This Week", "This Month", "This Quarter", "This Year") }

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
        // Screen Header with Period Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScreenHeader(
                title = "Reports",
                subtitle = "Overview of business performance for ${state.selectedPeriodLabel}."
            )
            Box {
                Button(
                    onClick = { reportPeriodMenuExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp), tint = B360Green)
                    Spacer(Modifier.width(8.dp))
                    Text(state.selectedPeriodLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                }
                DropdownMenu(
                    expanded = reportPeriodMenuExpanded,
                    onDismissRequest = { reportPeriodMenuExpanded = false }
                ) {
                    periodOptions.forEach { period ->
                        DropdownMenuItem(
                            text = { Text(period, fontSize = 13.sp, fontWeight = if (period == state.selectedPeriodLabel) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                reportPeriodMenuExpanded = false
                                viewModel.loadReport(period)
                            }
                        )
                    }
                }
            }
        }

        // KPI cards row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Total Revenue",
                value = "KES ${String.format("%,.0f", summary?.totalRevenue ?: 0.0)}",
                change = "Period: ${state.selectedPeriodLabel}",
                icon = Icons.Default.MonetizationOn,
                color = B360Green,
                bgColor = Color(0xFFE6F7F0)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Total Expenses",
                value = "KES ${String.format("%,.0f", summary?.totalExpenses ?: 0.0)}",
                change = "Period: ${state.selectedPeriodLabel}",
                icon = Icons.Default.ShoppingBag,
                color = B360Red,
                bgColor = Color(0xFFFEE2E2),
                changeColor = B360Red
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Net Profit",
                value = "KES ${String.format("%,.0f", summary?.netProfit ?: 0.0)}",
                change = "Period: ${state.selectedPeriodLabel}",
                icon = Icons.Default.TrendingUp,
                color = B360Blue,
                bgColor = Color(0xFFE0F2FE)
            )
            KpiCard(
                modifier = Modifier.weight(1f),
                title = "Gross Profit Margin",
                value = "${String.format("%.1f", (summary?.netMargin ?: 0.0) * 100)}%",
                change = "Period: ${state.selectedPeriodLabel}",
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
                        
                        Box {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                color = Color.White,
                                modifier = Modifier.clickable { chartPeriodMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(state.selectedPeriodLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = B360Green)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = B360Green, modifier = Modifier.size(16.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = chartPeriodMenuExpanded,
                                onDismissRequest = { chartPeriodMenuExpanded = false }
                            ) {
                                periodOptions.forEach { period ->
                                    DropdownMenuItem(
                                        text = { Text(period, fontSize = 13.sp) },
                                        onClick = {
                                            chartPeriodMenuExpanded = false
                                            viewModel.loadReport(period)
                                        }
                                    )
                                }
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
    Receipt("Receipt Customization", Icons.Default.ReceiptLong),
    TaxCompliance("Tax & Compliance", Icons.Default.AccountBalance),
    SocialSetup("Social Setup", Icons.Default.Share)
}

@Composable
fun SettingsTabChip(
    tab: SettingsTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    titleOverride: String? = null
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
                text = titleOverride ?: tab.title,
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
    var activeTab by remember { mutableStateOf(SettingsTab.General) }
    var activeTaxSection by remember { mutableStateOf("tax") }

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

                    LaunchedEffect(Unit) {
                        viewModel.loadProfile()
                    }

                    val profile = profileState.profile

                    var nameInput by remember { mutableStateOf("") }
                    var phoneInput by remember { mutableStateOf("") }
                    var typeInput by remember { mutableStateOf("") }

                    // Inline editing states
                    var isEditingName by remember { mutableStateOf(false) }
                    var isEditingPhone by remember { mutableStateOf(false) }
                    var isEditingType by remember { mutableStateOf(false) }

                    LaunchedEffect(profile) {
                        if (profile != null) {
                            nameInput = profile.name
                            phoneInput = profile.phone
                            typeInput = profile.type
                        }
                    }

                    fun saveField(fieldName: String, value: String) {
                        val currentProfile = profile ?: return
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
                            Text(
                                if (profile == null && !profileState.isLoading) {
                                    profileState.error ?: "Business profile is unavailable. Editing is disabled."
                                } else {
                                    "Payment configuration is read-only on desktop and is managed in the Payment Configuration tabs."
                                },
                                color = if (profile == null && !profileState.isLoading) B360Red else Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }

                        // Section 2: Security
                        SettingsSection(
                            title = "Security",
                            subtitle = "Manage your account security and notification preferences",
                            icon = Icons.Default.Shield
                        ) {
                            Text(
                                "Security and notification preferences are managed in the web application. Desktop does not display unsaved toggle states.",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
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
                                        text = buildString {
                                            append(
                                                profile?.subscriptionTier
                                                    ?.lowercase()
                                                    ?.replaceFirstChar { it.uppercase() }
                                                    ?: "Freemium"
                                            )
                                            append(if (profile?.subscriptionEnabled == false) " · Disabled" else " · Active")
                                        },
                                        color = if (profile?.subscriptionEnabled == false) Color(0xFFDC2626) else Color.Gray,
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
                                                    java.net.URI("https://biashara360.co.ke/settings")
                                                )
                                            }
                                        }
                                    },
                                    enabled = profile?.subscriptionEnabled != false &&
                                        profile?.subscriptionTier?.uppercase() != "PREMIUM",
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
                            subtitle = "Application service endpoint",
                            icon = Icons.Default.Link
                        ) {
                            Text(
                                com.app.biashara.data.remote.BASE_URL,
                                color = Color(0xFF334155),
                                fontSize = 13.sp
                            )
                            Text(
                                "This endpoint is read-only and can only be changed by deployment configuration.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
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
                SettingsTab.TaxCompliance -> {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("tax" to "Tax Settings", "kra" to "KRA iTax").forEach { (key, label) ->
                                SettingsTabChip(
                                    tab = SettingsTab.TaxCompliance,
                                    isSelected = activeTaxSection == key,
                                    onClick = { activeTaxSection = key },
                                    titleOverride = label
                                )
                            }
                        }
                        Box(Modifier.fillMaxWidth().weight(1f)) {
                            if (activeTaxSection == "tax") DesktopTaxModernScreen() else DesktopKraModernScreen()
                        }
                    }
                }
                SettingsTab.SocialSetup -> {
                    Column(
                        Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SettingsSection(
                            title = "Social Setup",
                            subtitle = "Connect and manage customer messaging channels",
                            icon = Icons.Default.Share
                        ) {
                            Text(
                                "Social channel connection and onboarding are managed in the secure web settings workspace.",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = { openDesktopWeb("/social-onboarding") },
                                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Open Social Setup")
                            }
                        }
                    }
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
@Deprecated("Legacy prototype with static tax data; use DesktopTaxModernScreen", level = DeprecationLevel.ERROR)
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
@Deprecated("Legacy prototype with static KRA data; use DesktopKraModernScreen", level = DeprecationLevel.ERROR)
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
fun DesktopCyberSourceSettingsScreen(
    viewModel: BusinessViewModel = remember { inject() }
) {
    val cyberSource by viewModel.cyberSourceState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCyberSourceConfig()
    }

    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CyberSource Secure Acceptance Configuration", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            IconButton(onClick = { viewModel.loadCyberSourceConfig() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Secure Acceptance Credentials", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                HorizontalDivider()

                if (cyberSource.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = B360Green)
                    }
                } else {
                    val config = cyberSource.config

                    // Merchant ID
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Merchant ID (Organization ID)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = config?.merchantId ?: "—",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Merchant Key ID
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Merchant Key ID (REST API Key ID)", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = config?.merchantKeyId ?: "—",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Profile ID
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Secure Acceptance Profile ID", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = config?.profileId ?: "—",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Access Key
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Secure Acceptance Access Key", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = config?.accessKey ?: "—",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Secret Key Status
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Shared Secret Key", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = if (config?.secretConfigured == true) "•••••••• (Configured on backend)" else "Not configured",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }

                    // Environment
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Environment", fontWeight = FontWeight.Medium)
                            Text(if (config?.environment == "production") "Production Rails" else "Sandbox Rail", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text(
                            text = (config?.environment ?: "sandbox").uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = if (config?.environment == "production") B360Green else Color(0xFFD97706),
                            fontSize = 14.sp
                        )
                    }

                    cyberSource.error?.let { err ->
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(B360Green.copy(0.12f))
                            .padding(14.dp)
                    ) {
                        Text("🔐 CyberSource credentials are managed centrally on the Biashara360 web application and synchronized securely with the backend API.", color = Color(0xFF166534), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
@Deprecated("Legacy editable payment screen; use DesktopPaymentConfigurationScreen", level = DeprecationLevel.ERROR)
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
    var receiptLogo by remember { mutableStateOf<String?>(null) }
    var receiptLogoWidthMm by remember { mutableStateOf(42f) }
    var receiptLogoHeightMm by remember { mutableStateOf(20f) }
    var showTax by remember { mutableStateOf(true) }
    var showCustomer by remember { mutableStateOf(true) }

    LaunchedEffect(state.profile) {
        state.profile?.let { prof ->
            header = prof.receiptHeader.take(60)
            footer = prof.receiptFooter.take(60)
            showTax = prof.receiptShowTax
            showCustomer = prof.receiptShowCustomer
            receiptLogo = prof.receiptLogo
            receiptLogoWidthMm = prof.receiptLogoWidthMm.toFloat()
            receiptLogoHeightMm = prof.receiptLogoHeightMm.toFloat()
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
                                receiptLogo = receiptLogo,
                                receiptLogoWidthMm = receiptLogoWidthMm.toInt(),
                                receiptLogoHeightMm = receiptLogoHeightMm.toInt(),
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

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Receipt Logo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                            Text("Add a PNG, JPEG, or WebP logo (maximum 500 KB)", fontSize = 11.sp, color = Color(0xFF64748B))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = {
                                    val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Choose receipt logo", java.awt.FileDialog.LOAD)
                                    dialog.setFilenameFilter(java.io.FilenameFilter { _, name -> name.lowercase().endsWith(".png") || name.lowercase().endsWith(".jpg") || name.lowercase().endsWith(".jpeg") || name.lowercase().endsWith(".webp") })
                                    dialog.isVisible = true
                                    val selectedName = dialog.file
                                    val selected = if (selectedName != null && dialog.directory != null) File(dialog.directory, selectedName) else null
                                    if (selected != null && selected.length() <= 500 * 1024) {
                                        val extension = selected.extension.lowercase().let { if (it == "jpg") "jpeg" else it }
                                        receiptLogo = "data:image/$extension;base64,${Base64.getEncoder().encodeToString(selected.readBytes())}"
                                    }
                                }, colors = ButtonDefaults.buttonColors(containerColor = B360Green), shape = RoundedCornerShape(8.dp)) {
                                    Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Choose image")
                                }
                                if (!receiptLogo.isNullOrBlank()) {
                                    Text("Logo selected", color = B360Green, fontSize = 12.sp)
                                    TextButton(onClick = { receiptLogo = null }) { Text("Remove", color = Color(0xFFDC2626)) }
                                }
                            }
                            if (!receiptLogo.isNullOrBlank()) {
                                Text("Logo width: ${receiptLogoWidthMm.toInt()} mm", fontSize = 12.sp, color = Color(0xFF475569))
                                Slider(value = receiptLogoWidthMm, onValueChange = { receiptLogoWidthMm = it }, valueRange = 10f..68f, steps = 57)
                                Text("Logo height: ${receiptLogoHeightMm.toInt()} mm", fontSize = 12.sp, color = Color(0xFF475569))
                                Slider(value = receiptLogoHeightMm, onValueChange = { receiptLogoHeightMm = it }, valueRange = 5f..40f, steps = 34)
                                Text("The logo keeps its proportions inside this printable area.", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

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
