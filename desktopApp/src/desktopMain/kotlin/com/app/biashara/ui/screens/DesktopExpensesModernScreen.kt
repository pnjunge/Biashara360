package com.app.biashara.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.biashara.UserSession
import com.app.biashara.domain.model.Expense
import com.app.biashara.domain.model.ExpenseCategory
import com.app.biashara.domain.usecase.generateId
import com.app.biashara.presentation.viewmodel.ExpensesViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val ExpensesGreen = Color(0xFF00B874)
private val ExpensesNavy = Color(0xFF0F172A)
private val ExpensesMuted = Color(0xFF64748B)
private val ExpensesLightMuted = Color(0xFF94A3B8)
private val ExpensesBorder = Color(0xFFE2E8F0)
private val ExpensesBg = Color(0xFFF8FAFC)
private val ExpensesCardBg = Color.White
private val ExpensesRed = Color(0xFFFF4D4D)
private val ExpensesCoral = Color(0xFFFF5252)

@Composable
fun DesktopExpensesModernScreen(
    viewModel: ExpensesViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    val currentUser by UserSession.currentUser.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToView by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    var selectedPeriod by remember { mutableStateOf("This Month") }
    var periodDropdownOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var categoryFilterMenuOpen by remember { mutableStateOf(false) }
    var moreMenuOpen by remember { mutableStateOf(false) }

    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val currentYear = now.year
    val currentMonth = now.monthNumber

    val currentUserName = remember(currentUser) {
        currentUser?.name?.ifBlank { null }
            ?: UserSession.getUserName().ifBlank { null }
            ?: "Admin"
    }
    val userInitial = remember(currentUserName) {
        currentUserName.firstOrNull()?.uppercase()?.toString() ?: "A"
    }
    val userRole = remember(currentUser) {
        currentUser?.role?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Admin"
    }

    // Default sample expenses if the database has 0 expenses initially
    val sampleExpenses = remember(currentYear, currentMonth) {
        listOf(
            Expense(
                id = "demo-1",
                businessId = UserSession.getBusinessId(),
                category = ExpenseCategory.SALARIES,
                amount = 100000.0,
                description = "August [Cash]",
                recordedAt = Clock.System.now(),
                expenseDate = LocalDate(currentYear, currentMonth, 27.coerceAtMost(28))
            ),
            Expense(
                id = "demo-2",
                businessId = UserSession.getBusinessId(),
                category = ExpenseCategory.STOCK_PURCHASE,
                amount = 10000.0,
                description = "Tusker",
                recordedAt = Clock.System.now(),
                expenseDate = LocalDate(currentYear, currentMonth, 27.coerceAtMost(28))
            ),
            Expense(
                id = "demo-3",
                businessId = UserSession.getBusinessId(),
                category = ExpenseCategory.RENT,
                amount = 10000.0,
                description = "Rent July",
                recordedAt = Clock.System.now(),
                expenseDate = LocalDate(currentYear, currentMonth, 26.coerceAtMost(28))
            ),
            Expense(
                id = "demo-4",
                businessId = UserSession.getBusinessId(),
                category = ExpenseCategory.ADVERTISING,
                amount = 10000.0,
                description = "Facebook",
                recordedAt = Clock.System.now(),
                expenseDate = LocalDate(currentYear, currentMonth, 26.coerceAtMost(28))
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadExpenses()
        while (true) {
            kotlinx.coroutines.delay(30_000)
            viewModel.syncExpenses(UserSession.getBusinessId())
        }
    }

    // Dynamic date range string computed from system clock
    val dateRangeText = remember(selectedPeriod, currentYear, currentMonth) {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        when (selectedPeriod) {
            "This Month" -> {
                val mName = monthNames.getOrElse(currentMonth - 1) { "Sep" }
                val lastDay = when (currentMonth) {
                    2 -> if (currentYear % 4 == 0) 29 else 28
                    4, 6, 9, 11 -> 30
                    else -> 31
                }
                "$mName 1, $currentYear - $mName $lastDay, $currentYear"
            }
            "Last Month" -> {
                val lastMonthNum = if (currentMonth == 1) 12 else currentMonth - 1
                val lastMonthYear = if (currentMonth == 1) currentYear - 1 else currentYear
                val mName = monthNames.getOrElse(lastMonthNum - 1) { "Aug" }
                val lastDay = when (lastMonthNum) {
                    2 -> if (lastMonthYear % 4 == 0) 29 else 28
                    4, 6, 9, 11 -> 30
                    else -> 31
                }
                "$mName 1, $lastMonthYear - $mName $lastDay, $lastMonthYear"
            }
            "This Year" -> "Jan 1, $currentYear - Dec 31, $currentYear"
            else -> "All Recorded Expenses"
        }
    }

    // Dynamic expenses: use DB state if populated, otherwise sampleExpenses
    val activeExpenses = if (state.expenses.isNotEmpty()) state.expenses else sampleExpenses

    // Filter by period dynamically
    val periodExpenses = remember(activeExpenses, selectedPeriod, currentYear, currentMonth) {
        when (selectedPeriod) {
            "This Month" -> activeExpenses.filter { it.expenseDate.year == currentYear && it.expenseDate.monthNumber == currentMonth }
            "Last Month" -> {
                val lmNum = if (currentMonth == 1) 12 else currentMonth - 1
                val lmYear = if (currentMonth == 1) currentYear - 1 else currentYear
                activeExpenses.filter { it.expenseDate.year == lmYear && it.expenseDate.monthNumber == lmNum }
            }
            "This Year" -> activeExpenses.filter { it.expenseDate.year == currentYear }
            else -> activeExpenses
        }.ifEmpty { activeExpenses }
    }

    // Filter by search & category
    val filteredExpenses = remember(periodExpenses, searchQuery, selectedCategoryFilter) {
        periodExpenses.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.category.name.contains(searchQuery, ignoreCase = true) ||
                item.category.displayName().contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == null || item.category == selectedCategoryFilter
            matchesQuery && matchesCategory
        }
    }

    // Dynamic calculations from periodExpenses
    val totalThisMonth = periodExpenses.sumOf { it.amount }
    val stockPurchaseTotal = periodExpenses.filter { it.category == ExpenseCategory.STOCK_PURCHASE }.sumOf { it.amount }
    val advertisingTotal = periodExpenses.filter { it.category == ExpenseCategory.ADVERTISING }.sumOf { it.amount }
    val operationsTotal = periodExpenses.filter {
        it.category == ExpenseCategory.RENT ||
            it.category == ExpenseCategory.UTILITIES ||
            it.category == ExpenseCategory.PACKAGING ||
            it.category == ExpenseCategory.DELIVERY ||
            it.category == ExpenseCategory.TRANSPORT
    }.sumOf { it.amount }

    fun computePercentageShare(catAmount: Double): String {
        if (totalThisMonth <= 0.0) return "0%"
        val pct = ((catAmount / totalThisMonth) * 100).toInt()
        return "$pct%"
    }

    // Dynamic Breakdown items (grouping top categories or top expense items)
    val breakdownItems = remember(periodExpenses, totalThisMonth) {
        if (periodExpenses.isEmpty()) emptyList()
        else {
            periodExpenses
                .groupBy { it.category }
                .map { (cat, list) ->
                    val catSum = list.sumOf { it.amount }
                    val pct = if (totalThisMonth > 0) ((catSum / totalThisMonth) * 100).toInt() else 0
                    val topItem = list.maxByOrNull { it.amount }
                    val title = topItem?.description ?: cat.displayName()
                    val (bg, color, icon) = getCategoryBadgeMeta(cat)
                    ExpenseBreakdownItem(
                        title = title,
                        subtitle = cat.displayName().uppercase(),
                        amount = catSum,
                        percentage = pct,
                        barColor = color,
                        icon = icon,
                        iconColor = color,
                        iconBg = bg
                    )
                }
                .sortedByDescending { it.amount }
                .take(4)
        }
    }

    // Dynamic Monthly Chart items (calculated from actual spending per category)
    val chartItems = remember(periodExpenses) {
        val categoriesWithSpend = periodExpenses
            .groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(4)

        val finalCategories = if (categoriesWithSpend.isNotEmpty()) {
            categoriesWithSpend
        } else {
            listOf(
                ExpenseCategory.SALARIES to 0.0,
                ExpenseCategory.STOCK_PURCHASE to 0.0,
                ExpenseCategory.RENT to 0.0,
                ExpenseCategory.ADVERTISING to 0.0
            )
        }

        finalCategories.map { (cat, amount) ->
            val (_, color, icon) = getCategoryBadgeMeta(cat)
            BarData(
                label = cat.displayName().take(14),
                amount = amount,
                color = color,
                icon = icon,
                iconColor = color
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ExpensesBg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. Top Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEDE9FE),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Expenses & Profit",
                        color = ExpensesNavy,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dashboard",
                            color = ExpensesLightMuted,
                            fontSize = 13.sp
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = ExpensesLightMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Expenses & Profit",
                            color = ExpensesMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dynamic Date Range Button with Dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, ExpensesBorder),
                        modifier = Modifier.clickable { periodDropdownOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = ExpensesMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = selectedPeriod,
                                    color = ExpensesNavy,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateRangeText,
                                    color = ExpensesLightMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = ExpensesLightMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = periodDropdownOpen,
                        onDismissRequest = { periodDropdownOpen = false }
                    ) {
                        listOf("This Month", "Last Month", "This Year", "All Time").forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period) },
                                onClick = {
                                    selectedPeriod = period
                                    periodDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Add Expense Button
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpensesGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Add Expense",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // --- 2. Four KPI Metric Cards (Computed Dynamically) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExpensesKpiCard(
                modifier = Modifier.weight(1f),
                title = "Total This Month",
                amount = totalThisMonth,
                subtitle = "All categories",
                trend = "↑ ${periodExpenses.size} items",
                isTrendUp = true,
                icon = Icons.Default.Description,
                iconColor = Color(0xFFFF4D6D),
                iconBg = Color(0xFFFFEEEE)
            )

            ExpensesKpiCard(
                modifier = Modifier.weight(1f),
                title = "Stock Purchase",
                amount = stockPurchaseTotal,
                subtitle = "Stock purchases",
                trend = computePercentageShare(stockPurchaseTotal),
                isTrendUp = false,
                icon = Icons.Default.ShoppingCart,
                iconColor = ExpensesGreen,
                iconBg = Color(0xFFE8FAF2)
            )

            ExpensesKpiCard(
                modifier = Modifier.weight(1f),
                title = "Advertising",
                amount = advertisingTotal,
                subtitle = "Marketing spend",
                trend = computePercentageShare(advertisingTotal),
                isTrendUp = true,
                icon = Icons.Default.Campaign,
                iconColor = Color(0xFF0284C7),
                iconBg = Color(0xFFE0F2FE)
            )

            ExpensesKpiCard(
                modifier = Modifier.weight(1f),
                title = "Operations",
                amount = operationsTotal,
                subtitle = "Rent + Ops",
                trend = computePercentageShare(operationsTotal),
                isTrendUp = true,
                icon = Icons.Default.Settings,
                iconColor = Color(0xFFD97706),
                iconBg = Color(0xFFFEF3C7)
            )
        }

        // --- 3. Middle Row: Expense Breakdown + Monthly Expense Chart ---
        Row(
            modifier = Modifier.fillMaxWidth().height(330.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Expense Breakdown Card (Dynamic)
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ExpensesCardBg),
                border = BorderStroke(1.dp, ExpensesBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
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
                                Icons.Default.PieChart,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Expense Breakdown",
                                color = ExpensesNavy,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, ExpensesBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedPeriod,
                                    color = ExpensesMuted,
                                    fontSize = 12.sp
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = ExpensesLightMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (breakdownItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No expense breakdown available.", color = ExpensesMuted, fontSize = 13.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            breakdownItems.forEach { item ->
                                ExpenseBreakdownRow(
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    amount = item.amount,
                                    percentage = item.percentage,
                                    barColor = item.barColor,
                                    icon = item.icon,
                                    iconColor = item.iconColor,
                                    iconBg = item.iconBg
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Expense Chart Card (Dynamic)
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ExpensesCardBg),
                border = BorderStroke(1.dp, ExpensesBorder)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp)
                ) {
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
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Monthly Expense Chart",
                                color = ExpensesNavy,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, ExpensesBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedPeriod,
                                    color = ExpensesMuted,
                                    fontSize = 12.sp
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = ExpensesLightMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Dynamic Bar Chart with Scaled Axes
                    MonthlyBarChart(
                        items = chartItems,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            }
        }

        // --- 4. Bottom Card: Expenses List ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ExpensesCardBg),
            border = BorderStroke(1.dp, ExpensesBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with Search, Filter & More options
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Expenses List",
                            color = ExpensesNavy,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Search Box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search expense, category...", fontSize = 12.sp, color = ExpensesLightMuted) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = ExpensesLightMuted, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.width(260.dp).height(44.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ExpensesGreen,
                                unfocusedBorderColor = ExpensesBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // Filter Button
                        Box {
                            OutlinedButton(
                                onClick = { categoryFilterMenuOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ExpensesBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpensesNavy),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = selectedCategoryFilter?.displayName() ?: "Filter",
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = categoryFilterMenuOpen,
                                onDismissRequest = { categoryFilterMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Categories") },
                                    onClick = {
                                        selectedCategoryFilter = null
                                        categoryFilterMenuOpen = false
                                    }
                                )
                                ExpenseCategory.entries.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.displayName()) },
                                        onClick = {
                                            selectedCategoryFilter = cat
                                            categoryFilterMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // More Button
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, ExpensesBorder),
                                modifier = Modifier.size(40.dp).clickable { moreMenuOpen = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "More options",
                                        tint = ExpensesMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = moreMenuOpen,
                                onDismissRequest = { moreMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export to CSV") },
                                    onClick = { moreMenuOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to PDF") },
                                    onClick = { moreMenuOpen = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Refresh") },
                                    onClick = {
                                        viewModel.loadExpenses()
                                        moreMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = ExpensesBorder)

                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", modifier = Modifier.weight(0.4f), color = ExpensesLightMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TableHeaderCell("Description", 1.8f)
                    TableHeaderCell("Category", 1.4f)
                    Text("Amount", modifier = Modifier.weight(1.1f), color = ExpensesLightMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    TableHeaderCell("Date", 1.2f)
                    TableHeaderCell("Added By", 1.3f)
                    TableHeaderCell("Actions", 1.1f)
                }

                HorizontalDivider(color = ExpensesBorder)

                // Table Rows
                if (filteredExpenses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses found matching the criteria.",
                            color = ExpensesMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    filteredExpenses.forEachIndexed { index, expense ->
                        ExpenseTableRowItem(
                            index = index + 1,
                            expense = expense,
                            userName = currentUserName,
                            userInitial = userInitial,
                            onEdit = { expenseToEdit = expense },
                            onDelete = { expenseToDelete = expense },
                            onView = { expenseToView = expense }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }

                HorizontalDivider(color = ExpensesBorder)

                // Table Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredExpenses.size} of ${activeExpenses.size} expenses",
                        color = ExpensesLightMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "All matching expenses loaded",
                        color = ExpensesLightMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // --- Add Expense Dialog ---
    if (showAddDialog) {
        AddOrEditExpenseDialog(
            initialExpense = null,
            onDismiss = { showAddDialog = false },
            onSave = { description, amount, category, date ->
                val nowInstant = Clock.System.now()
                viewModel.saveExpense(
                    Expense(
                        id = generateId(),
                        businessId = UserSession.getBusinessId(),
                        category = category,
                        amount = amount,
                        description = description,
                        recordedAt = nowInstant,
                        expenseDate = date
                    )
                )
                showAddDialog = false
            }
        )
    }

    // --- Edit Expense Dialog ---
    expenseToEdit?.let { item ->
        AddOrEditExpenseDialog(
            initialExpense = item,
            onDismiss = { expenseToEdit = null },
            onSave = { description, amount, category, date ->
                viewModel.saveExpense(
                    item.copy(
                        description = description,
                        amount = amount,
                        category = category,
                        expenseDate = date
                    )
                )
                expenseToEdit = null
            }
        )
    }

    // --- View Expense Dialog ---
    expenseToView?.let { item ->
        ViewExpenseDialog(
            expense = item,
            addedBy = "$currentUserName ($userRole)",
            onDismiss = { expenseToView = null }
        )
    }

    // --- Delete Confirmation Dialog ---
    expenseToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold, color = ExpensesNavy) },
            text = { Text("Are you sure you want to delete “${item.description}”? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(item.id)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel", color = ExpensesNavy)
                }
            }
        )
    }
}

// --- KPI Card Component ---
@Composable
private fun ExpensesKpiCard(
    modifier: Modifier,
    title: String,
    amount: Double,
    subtitle: String,
    trend: String,
    isTrendUp: Boolean,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ExpensesCardBg),
        border = BorderStroke(1.dp, ExpensesBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBg,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = ExpensesMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "KES ${String.format("%,.0f", amount)}",
                        color = ExpensesNavy,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val trendBg = if (isTrendUp) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    val trendColor = if (isTrendUp) Color(0xFF16A34A) else Color(0xFFEF4444)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = trendBg
                    ) {
                        Text(
                            text = trend,
                            color = trendColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = subtitle,
                    color = ExpensesLightMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private data class ExpenseBreakdownItem(
    val title: String,
    val subtitle: String,
    val amount: Double,
    val percentage: Int,
    val barColor: Color,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBg: Color
)

// --- Breakdown Row Component ---
@Composable
private fun ExpenseBreakdownRow(
    title: String,
    subtitle: String,
    amount: Double,
    percentage: Int,
    barColor: Color,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = iconBg,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
        }

        Column(modifier = Modifier.width(130.dp)) {
            Text(
                text = title,
                color = ExpensesNavy,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = ExpensesLightMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (percentage / 100f).coerceIn(0.04f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }

        Text(
            text = "KES ${String.format("%,.0f", amount)}",
            color = ExpensesCoral,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp),
            textAlign = TextAlign.End
        )

        Text(
            text = "$percentage%",
            color = ExpensesLightMuted,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}

// --- Dynamic Monthly Bar Chart Component ---
@Composable
private fun MonthlyBarChart(
    items: List<BarData>,
    modifier: Modifier = Modifier
) {
    val highest = items.maxOfOrNull { it.amount } ?: 1000.0
    val maxVal = maxOf(1000.0, highest * 1.25)
    val step = maxVal / 4.0

    fun formatK(v: Double): String {
        return when {
            v >= 1_000_000 -> "${String.format("%.1f", v / 1_000_000)}M"
            v >= 1_000 -> "${(v / 1_000).toInt()}K"
            else -> "${v.toInt()}"
        }
    }

    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.Bottom
    ) {
        // Y-Axis
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Amount (KES)",
                color = ExpensesLightMuted,
                fontSize = 10.sp,
                modifier = Modifier.rotate(-90f).padding(end = 4.dp)
            )

            Column(
                modifier = Modifier.fillMaxHeight().padding(bottom = 44.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(formatK(maxVal), color = ExpensesLightMuted, fontSize = 10.sp)
                Text(formatK(step * 3), color = ExpensesLightMuted, fontSize = 10.sp)
                Text(formatK(step * 2), color = ExpensesLightMuted, fontSize = 10.sp)
                Text(formatK(step * 1), color = ExpensesLightMuted, fontSize = 10.sp)
                Text("0", color = ExpensesLightMuted, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Chart Bars Area
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            // Horizontal Grid Lines
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = 44.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(5) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }

            // Bars
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { item ->
                    val barHeightFraction = if (maxVal > 0) (item.amount / maxVal).toFloat().coerceIn(0.04f, 0.95f) else 0.04f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        // Label above bar
                        Text(
                            text = "KES ${String.format("%,.0f", item.amount)}",
                            color = item.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Bar
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .fillMaxHeight(fraction = barHeightFraction * 0.72f)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(item.color)
                        )

                        Spacer(Modifier.height(8.dp))

                        // Category Icon & Label Below
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = item.iconColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.label,
                                color = ExpensesNavy,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private data class BarData(
    val label: String,
    val amount: Double,
    val color: Color,
    val icon: ImageVector,
    val iconColor: Color
)

// --- Table Row Item Component ---
@Composable
private fun ExpenseTableRowItem(
    index: Int,
    expense: Expense,
    userName: String,
    userInitial: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // #
        Text(
            text = "$index",
            modifier = Modifier.weight(0.4f),
            color = ExpensesNavy,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        // Description
        Text(
            text = expense.description,
            modifier = Modifier.weight(1.8f),
            color = ExpensesNavy,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Category Badge
        Box(modifier = Modifier.weight(1.4f)) {
            val (badgeBg, badgeColor, badgeIcon) = getCategoryBadgeMeta(expense.category)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = badgeBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(13.dp))
                    Text(
                        text = expense.category.displayName(),
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Amount
        Text(
            text = "KES ${String.format("%,.0f", expense.amount)}",
            modifier = Modifier.weight(1.1f),
            color = ExpensesRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        // Date with calendar icon
        Row(
            modifier = Modifier.weight(1.2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = null,
                tint = ExpensesLightMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = expense.expenseDate.toString(),
                color = ExpensesMuted,
                fontSize = 12.sp
            )
        }

        // Added By: circular avatar + user name (Dynamic)
        Row(
            modifier = Modifier.weight(1.3f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFDCFCE7),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userInitial,
                        color = Color(0xFF16A34A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = userName,
                color = ExpensesNavy,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Actions: Edit, Delete, View
        Row(
            modifier = Modifier.weight(1.1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFEFF6FF),
                modifier = Modifier.size(28.dp).clickable { onEdit() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Delete
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFEF2F2),
                modifier = Modifier.size(28.dp).clickable { onDelete() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // View
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(28.dp).clickable { onView() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "View Details",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableHeaderCell(title: String, weight: Float) {
    Row(
        modifier = Modifier.weight(weight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = ExpensesLightMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(
            Icons.Default.UnfoldMore,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(14.dp)
        )
    }
}

private fun getCategoryBadgeMeta(category: ExpenseCategory): Triple<Color, Color, ImageVector> = when (category) {
    ExpenseCategory.SALARIES -> Triple(Color(0xFFFEE2E2), Color(0xFFEF4444), Icons.Default.Payments)
    ExpenseCategory.STOCK_PURCHASE -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), Icons.Default.Inventory2)
    ExpenseCategory.RENT -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.Home)
    ExpenseCategory.ADVERTISING -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), Icons.Default.Campaign)
    ExpenseCategory.UTILITIES -> Triple(Color(0xFFCFFAFE), Color(0xFF0891B2), Icons.Default.Settings)
    ExpenseCategory.DELIVERY, ExpenseCategory.TRANSPORT -> Triple(Color(0xFFFFEDD5), Color(0xFFEA580C), Icons.Default.LocalShipping)
    ExpenseCategory.PACKAGING -> Triple(Color(0xFFF3E8FF), Color(0xFF7C3AED), Icons.Default.Inventory2)
    else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), Icons.Default.Category)
}

// --- Add / Edit Expense Dialog ---
@Composable
private fun AddOrEditExpenseDialog(
    initialExpense: Expense?,
    onDismiss: () -> Unit,
    onSave: (String, Double, ExpenseCategory, LocalDate) -> Unit
) {
    var description by remember { mutableStateOf(initialExpense?.description ?: "") }
    var amount by remember { mutableStateOf(initialExpense?.let { String.format("%.0f", it.amount) } ?: "") }
    var category by remember { mutableStateOf(initialExpense?.category ?: ExpenseCategory.MISCELLANEOUS) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("M-Pesa") }
    var paymentMenuOpen by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(620.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color(0xFFE2F8EF), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = ExpensesGreen, modifier = Modifier.size(24.dp))
                            }
                        }
                        Column {
                            Text(
                                text = if (initialExpense == null) "Add New Expense" else "Edit Expense",
                                color = ExpensesNavy,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Record business spending accurately.",
                                color = ExpensesMuted,
                                fontSize = 13.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ExpensesMuted)
                    }
                }

                error?.let {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEE2E2), modifier = Modifier.fillMaxWidth()) {
                        Text(it, color = Color(0xFFEF4444), fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Description *", color = ExpensesNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it; error = null },
                        placeholder = { Text("e.g. August [Cash] or Facebook Ads", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Amount (KES) *", color = ExpensesNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }; error = null },
                        placeholder = { Text("0.00", fontSize = 13.sp) },
                        prefix = { Text("KES ", color = ExpensesGreen, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category *", color = ExpensesNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { categoryMenuOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { categoryMenuOpen = true },
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(
                            expanded = categoryMenuOpen,
                            onDismissRequest = { categoryMenuOpen = false },
                            modifier = Modifier.width(300.dp)
                        ) {
                            ExpenseCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName()) },
                                    onClick = {
                                        category = cat
                                        categoryMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Payment Method", color = ExpensesNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { paymentMenuOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { paymentMenuOpen = true },
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(
                            expanded = paymentMenuOpen,
                            onDismissRequest = { paymentMenuOpen = false }
                        ) {
                            listOf("M-Pesa", "Cash", "Card", "Bank Transfer").forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        paymentMethod = method
                                        paymentMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Notes (Optional)", color = ExpensesNavy, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Add any notes or transaction references...", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                HorizontalDivider(color = ExpensesBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = ExpensesNavy)
                    }

                    Button(
                        onClick = {
                            val parsed = amount.toDoubleOrNull()
                            when {
                                description.isBlank() -> error = "Description is required."
                                parsed == null || parsed <= 0 -> error = "Enter a valid amount greater than zero."
                                else -> {
                                    val finalDesc = if (notes.isNotBlank()) "${description.trim()} — ${notes.trim()}" else description.trim()
                                    onSave(finalDesc, parsed, category, initialExpense?.expenseDate ?: today)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpensesGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Expense", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- View Expense Dialog ---
@Composable
private fun ViewExpenseDialog(
    expense: Expense,
    addedBy: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(480.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expense Details",
                        color = ExpensesNavy,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ExpensesMuted)
                    }
                }

                HorizontalDivider(color = ExpensesBorder)

                ExpenseDetailItem("Description", expense.description)
                ExpenseDetailItem("Category", expense.category.displayName())
                ExpenseDetailItem("Amount", "KES ${String.format("%,.0f", expense.amount)}")
                ExpenseDetailItem("Date", expense.expenseDate.toString())
                ExpenseDetailItem("Added By", addedBy)
                ExpenseDetailItem("Expense ID", expense.id)

                HorizontalDivider(color = ExpensesBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ExpensesGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = ExpensesLightMuted, fontSize = 13.sp)
        Text(value, color = ExpensesNavy, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
