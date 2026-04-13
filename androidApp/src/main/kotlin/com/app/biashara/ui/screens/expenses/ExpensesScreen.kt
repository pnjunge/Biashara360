package com.app.biashara.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    viewModel: ExpensesViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadExpenses() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses / Gharama", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense, containerColor = B360Green, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        if (state.isLoading && state.expenses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(B360Red.copy(0.08f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total Expenses – This Month", fontSize = 13.sp, color = Color.Gray)
                        Text(
                            "KES ${"%,.0f".format(state.totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = B360Red
                        )
                    }
                }
            }

            if (state.expenses.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Receipt, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Text("No expenses recorded", color = Color.Gray)
                            Button(onClick = onAddExpense, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                                Text("Add First Expense")
                            }
                        }
                    }
                }
            } else {
                items(state.expenses) { expense ->
                    val color = categoryColors[expense.category] ?: Color.Gray
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
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
                                    Text(expense.description, fontWeight = FontWeight.Medium)
                                    Surface(color = color.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                                        Text(
                                            expense.category.displayName(),
                                            color = color, fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(expense.expenseDate.toString(), fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("KES ${"%,.0f".format(expense.amount)}", fontWeight = FontWeight.Bold, color = B360Red)
                                IconButton(
                                    onClick = { viewModel.deleteExpense(expense.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, null, tint = B360Red.copy(0.7f), modifier = Modifier.size(16.dp))
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
    viewModel: ExpensesViewModel = koinInject()
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
                title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
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
                expanded = !saving
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                else {
                    Icon(Icons.Filled.Check, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Expense", color = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (error.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp))
                }
            }
            OutlinedTextField(
                value = description, onValueChange = { description = it; error = "" },
                label = { Text("Description *") }, modifier = Modifier.fillMaxWidth(),
                enabled = !saving
            )
            OutlinedTextField(
                value = amount, onValueChange = { amount = it; error = "" },
                label = { Text("Amount (KES) *") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !saving
            )
            Text("Category", fontWeight = FontWeight.Medium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName(), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = (categoryColors[cat] ?: Color.Gray).copy(0.2f)
                                ),
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
