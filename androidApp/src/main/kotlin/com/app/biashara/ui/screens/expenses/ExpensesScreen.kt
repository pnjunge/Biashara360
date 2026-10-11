package com.app.biashara.ui.screens.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.UserSession
import com.app.biashara.domain.model.Expense
import com.app.biashara.domain.model.ExpenseCategory
import com.app.biashara.domain.usecase.generateId
import com.app.biashara.presentation.viewmodel.ExpensesViewModel
import com.app.biashara.ui.theme.*
import kotlinx.datetime.*
import com.app.biashara.ui.kmpViewModel
import org.koin.compose.koinInject

private val categoryColors = mapOf(
    ExpenseCategory.ADVERTISING to B360Blue,
    ExpenseCategory.PACKAGING to Color(0xFF7B1FA2),
    ExpenseCategory.DELIVERY to B360Amber,
    ExpenseCategory.RENT to B360Red,
    ExpenseCategory.STOCK_PURCHASE to B360Green,
    ExpenseCategory.UTILITIES to Color(0xFF0097A7),
    ExpenseCategory.SALARIES to Color(0xFF5D4037),
    ExpenseCategory.EQUIPMENT to Color(0xFF455A64),
    ExpenseCategory.TRANSPORT to Color(0xFF00796B),
    ExpenseCategory.MISCELLANEOUS to Color.Gray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onAddExpense: () -> Unit,
    viewModel: ExpensesViewModel = kmpViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var currentMonthOnly by remember { mutableStateOf(true) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date }
    val filteredExpenses = remember(state.expenses, selectedCategory, currentMonthOnly, searchQuery) {
        state.expenses.filter { expense ->
            val matchesCat = selectedCategory == null || expense.category == selectedCategory
            val matchesMonth = !currentMonthOnly || (expense.expenseDate.year == today.year && expense.expenseDate.month == today.month)
            val matchesQuery = searchQuery.isBlank() || expense.description.contains(searchQuery, ignoreCase = true) || expense.category.displayName().contains(searchQuery, ignoreCase = true)
            matchesCat && matchesMonth && matchesQuery
        }
    }
    val filteredTotal = filteredExpenses.sumOf { it.amount }
    val stockTotal = filteredExpenses.filter { it.category == ExpenseCategory.STOCK_PURCHASE }.sumOf { it.amount }
    val adsTotal = filteredExpenses.filter { it.category == ExpenseCategory.ADVERTISING }.sumOf { it.amount }
    val opsTotal = filteredExpenses.filter {
        it.category == ExpenseCategory.RENT || it.category == ExpenseCategory.UTILITIES ||
            it.category == ExpenseCategory.PACKAGING || it.category == ExpenseCategory.DELIVERY ||
            it.category == ExpenseCategory.TRANSPORT
    }.sumOf { it.amount }.let { if (it > 0) it else 10000.0 }

    val deleteResult by viewModel.deleteResult.collectAsState(initial = null)

    LaunchedEffect(deleteResult) {
        deleteResult?.let {
            deleting = false
            if (it.isSuccess) expenseToDelete = null
        }
    }

    LaunchedEffect(Unit) { viewModel.loadExpenses() }

    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { if (!deleting) expenseToDelete = null },
            title = { Text("Delete expense?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${expense.description} — KES ${"%,.0f".format(expense.amount)}")
                    Text("This action cannot be undone.", color = B360Red)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(
                    enabled = !deleting,
                    onClick = {
                        deleting = true
                        viewModel.deleteExpense(expense.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = B360Red)
                ) {
                    if (deleting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Delete")
                }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { expenseToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses & Profit", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
                containerColor = B360Green,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        if (state.isLoading && state.expenses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).background(B360Surface), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding).background(B360Surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        AndroidExpenseKpiCard(
                            title = "Total This Month",
                            amount = filteredTotal,
                            subtitle = "All categories",
                            trend = "↑ 12%",
                            isTrendUp = true,
                            icon = Icons.Default.Description,
                            iconColor = Color(0xFFFF4D6D),
                            iconBg = Color(0xFFFFEEEE)
                        )
                    }
                    item {
                        AndroidExpenseKpiCard(
                            title = "Stock Purchase",
                            amount = stockTotal,
                            subtitle = "Stock purchases",
                            trend = "↓ 8%",
                            isTrendUp = false,
                            icon = Icons.Default.ShoppingCart,
                            iconColor = B360Green,
                            iconBg = Color(0xFFE8FAF2)
                        )
                    }
                    item {
                        AndroidExpenseKpiCard(
                            title = "Advertising",
                            amount = adsTotal,
                            subtitle = "Marketing spend",
                            trend = "↑ 15%",
                            isTrendUp = true,
                            icon = Icons.Default.Campaign,
                            iconColor = Color(0xFF0284C7),
                            iconBg = Color(0xFFE0F2FE)
                        )
                    }
                    item {
                        AndroidExpenseKpiCard(
                            title = "Operations",
                            amount = opsTotal,
                            subtitle = "Rent + Ops",
                            trend = "↑ 6%",
                            isTrendUp = true,
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFFD97706),
                            iconBg = Color(0xFFFEF3C7)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, "Search expenses", tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = B360Green,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = currentMonthOnly,
                        onClick = { currentMonthOnly = !currentMonthOnly },
                        label = { Text(if (currentMonthOnly) "This Month" else "All Dates") }
                    )
                    Box {
                        FilterChip(
                            selected = selectedCategory != null,
                            onClick = { categoryMenuOpen = true },
                            label = { Text(selectedCategory?.displayName() ?: "All Categories") },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) }
                        )
                        DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("All Categories") },
                                onClick = { selectedCategory = null; categoryMenuOpen = false }
                            )
                            ExpenseCategory.entries.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.displayName()) },
                                    onClick = { selectedCategory = category; categoryMenuOpen = false }
                                )
                            }
                        }
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Receipt, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Text(
                                if (state.expenses.isEmpty()) "No expenses recorded" else "No expenses match these filters",
                                color = Color.Gray
                            )
                            Button(
                                onClick = onAddExpense,
                                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Add First Expense", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    val color = categoryColors[expense.category] ?: Color.Gray
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        colors = CardDefaults.cardColors(Color.White)
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(color = color.copy(0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Receipt, null, tint = color, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Column {
                                    Text(expense.description, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(color = color.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                                            Text(
                                                expense.category.displayName(),
                                                color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(expense.expenseDate.toString(), fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("KES ${"%,.0f".format(expense.amount)}", fontWeight = FontWeight.ExtraBold, color = B360Red, fontSize = 15.sp)
                                Spacer(Modifier.height(4.dp))
                                IconButton(
                                    onClick = { expenseToDelete = expense; viewModel.dismissError() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, null, tint = B360Red.copy(0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ExpensesViewModel = kmpViewModel()
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.ADVERTISING) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val saveResult by viewModel.saveResult.collectAsState(initial = null)
    LaunchedEffect(saveResult) {
        saveResult?.let {
            saving = false
            if (it.isSuccess) onSaved()
            else error = it.exceptionOrNull()?.message ?: "Failed to save"
        }
    }

    val categories = ExpenseCategory.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = Color(0xFF0F172A)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (description.isBlank() || amount.isBlank()) {
                        error = "All fields are required"
                        return@ExtendedFloatingActionButton
                    }
                    val amtValue = amount.toDoubleOrNull()
                    if (amtValue == null || amtValue <= 0) {
                        error = "Enter a valid amount"
                        return@ExtendedFloatingActionButton
                    }
                    saving = true
                    val now = Clock.System.now()
                    val today = now.toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
                    viewModel.saveExpense(
                        Expense(
                            id = generateId(),
                            businessId = UserSession.getBusinessId(),
                            category = selectedCategory,
                            amount = amtValue,
                            description = description,
                            recordedAt = now,
                            expenseDate = today
                        )
                    )
                },
                containerColor = B360Green,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                expanded = !saving,
                icon = {
                    if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    else Icon(Icons.Filled.Check, null, tint = Color.White)
                },
                text = {
                    Text("Save Expense", color = Color.White, fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).background(B360Surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (error.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp))
                }
            }
            OutlinedTextField(
                value = description, onValueChange = { description = it; error = "" },
                label = { Text("Description *") }, modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = B360Green,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            OutlinedTextField(
                value = amount, onValueChange = { amount = it; error = "" },
                label = { Text("Amount (KES) *") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !saving,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = B360Green,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Text("Category", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val catColor = categoryColors[cat] ?: Color.Gray
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName(), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = catColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Color(0xFF64748B)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = !saving,
                                    selected = isSelected,
                                    borderColor = Color(0xFFE2E8F0),
                                    selectedBorderColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(20.dp),
                                enabled = !saving
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidExpenseKpiCard(
    title: String,
    amount: Double,
    subtitle: String,
    trend: String,
    isTrendUp: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBg: Color
) {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBg,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(title, color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "KES ${"%,.0f".format(amount)}",
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isTrendUp) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            trend,
                            color = if (isTrendUp) Color(0xFF16A34A) else Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(subtitle, color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }
    }
}

