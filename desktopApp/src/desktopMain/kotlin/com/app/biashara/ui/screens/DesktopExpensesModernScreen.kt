package com.app.biashara.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import com.app.biashara.UserSession
import com.app.biashara.domain.model.Expense
import com.app.biashara.domain.model.ExpenseCategory
import com.app.biashara.domain.usecase.generateId
import com.app.biashara.presentation.viewmodel.ExpensesViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val ExpensesGreen = Color(0xFF00B874)
private val ExpensesNavy = Color(0xFF0F1F3A)
private val ExpensesMuted = Color(0xFF64748B)
private val ExpensesBorder = Color(0xFFE2E8F0)

@Composable
fun DesktopExpensesModernScreen(
    viewModel: ExpensesViewModel = remember { inject() }
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadExpenses() }

    val visibleExpenses = remember(state.expenses, selectedCategory) {
        selectedCategory?.let { category -> state.expenses.filter { it.category == category } } ?: state.expenses
    }
    val advertising = state.expenses.filter { it.category == ExpenseCategory.ADVERTISING }
    val stock = state.expenses.filter { it.category == ExpenseCategory.STOCK_PURCHASE }
    val operations = state.expenses.filter {
        it.category != ExpenseCategory.ADVERTISING && it.category != ExpenseCategory.STOCK_PURCHASE
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Expenses", color = ExpensesNavy, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dashboard", color = ExpensesMuted, fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Text("Expenses", color = ExpensesMuted, fontSize = 14.sp)
                }
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ExpensesGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Expense", fontWeight = FontWeight.Bold)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ExpenseSummaryCard(
                Modifier.weight(1f), "Total Expenses (This Month)", state.totalAmount, state.expenses.size,
                Icons.Default.AccountBalanceWallet, Color(0xFFEF4444), Color(0xFFFEECEC)
            )
            ExpenseSummaryCard(
                Modifier.weight(1f), "Advertising", advertising.sumOf { it.amount }, advertising.size,
                Icons.Default.Campaign, Color(0xFF2563EB), Color(0xFFE8F1FF)
            )
            ExpenseSummaryCard(
                Modifier.weight(1f), "Stock Purchase", stock.sumOf { it.amount }, stock.size,
                Icons.Default.ShoppingCart, ExpensesGreen, Color(0xFFE1F8EF)
            )
            ExpenseSummaryCard(
                Modifier.weight(1f), "Operations", operations.sumOf { it.amount }, operations.size,
                Icons.Default.Groups, Color(0xFFF97316), Color(0xFFFFF0E5)
            )
        }

        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ExpensesBorder)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box {
                            ExpenseFilterButton(
                                selectedCategory?.displayName() ?: "All Categories",
                                Icons.Default.Category
                            ) { categoryMenuOpen = true }
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
                    OutlinedButton(
                        onClick = { selectedCategory = null },
                        enabled = selectedCategory != null,
                        shape = RoundedCornerShape(9.dp),
                        border = BorderStroke(1.dp, ExpensesBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpensesNavy)
                    ) {
                        Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Clear category")
                    }
                }

                HorizontalDivider(color = ExpensesBorder)
                ExpenseTableHeader()
                HorizontalDivider(color = ExpensesBorder)

                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when {
                        state.isLoading -> CircularProgressIndicator(
                            color = ExpensesGreen,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        visibleExpenses.isEmpty() -> ExpenseEmptyState(
                            onAdd = { showAddDialog = true },
                            modifier = Modifier.align(Alignment.Center)
                        )
                        else -> Column(Modifier.fillMaxWidth()) {
                            visibleExpenses.forEach { expense ->
                                ExpenseTableRow(expense, onDelete = { viewModel.deleteExpense(expense.id) })
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }

                HorizontalDivider(color = ExpensesBorder)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Showing ${visibleExpenses.size} of ${state.expenses.size} expenses",
                        color = ExpensesMuted,
                        fontSize = 13.sp
                    )
                    Text("All matching expenses shown", color = ExpensesMuted, fontSize = 12.sp)
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onSave = { description, amount, category ->
                val now = Clock.System.now()
                viewModel.saveExpense(
                    Expense(
                        id = generateId(),
                        businessId = UserSession.getBusinessId(),
                        category = category,
                        amount = amount,
                        description = description,
                        recordedAt = now,
                        expenseDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ExpenseSummaryCard(
    modifier: Modifier,
    label: String,
    amount: Double,
    count: Int,
    icon: ImageVector,
    color: Color,
    background: Color
) {
    Card(
        modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background.copy(alpha = 0.52f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
                Icon(icon, null, tint = color, modifier = Modifier.padding(13.dp).size(26.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, color = ExpensesMuted, fontSize = 12.sp)
                Text("KES ${String.format("%,.0f", amount)}", color = color, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("$count transactions", color = ExpensesMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ExpenseFilterButton(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit = {}) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, ExpensesBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpensesNavy),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 11.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
        Spacer(Modifier.width(16.dp))
        Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ExpenseTableHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFFCFDFE)).padding(horizontal = 24.dp, vertical = 16.dp)) {
        ExpenseCell("Description", 1.7f, true)
        ExpenseCell("Category", 1.2f, true)
        ExpenseCell("Amount", 1f, true)
        ExpenseCell("Payment Method", 1.25f, true)
        ExpenseCell("Date", 1f, true)
        ExpenseCell("Actions", 0.65f, true)
    }
}

@Composable
private fun ExpenseTableRow(expense: Expense, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExpenseCell(expense.description, 1.7f)
        Box(Modifier.weight(1.2f)) {
            val color = expense.category.categoryColor()
            Surface(shape = RoundedCornerShape(7.dp), color = color.copy(alpha = 0.1f)) {
                Text(
                    expense.category.displayName(),
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }
        ExpenseCell("KES ${String.format("%,.0f", expense.amount)}", 1f, bold = true)
        ExpenseCell("Not recorded", 1.25f)
        ExpenseCell(expense.expenseDate.toString(), 1f)
        Row(Modifier.weight(0.65f), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.DeleteOutline, "Delete expense", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete expense?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete “${expense.description}”.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RowScope.ExpenseCell(text: String, weight: Float, header: Boolean = false, bold: Boolean = false) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        color = if (header) ExpensesNavy else ExpensesMuted,
        fontSize = if (header) 12.sp else 13.sp,
        fontWeight = if (header || bold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1
    )
}

@Composable
private fun ExpenseEmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(shape = CircleShape, color = Color(0xFFF1F3FA)) {
            Icon(Icons.Default.Inbox, null, tint = Color(0xFFA7ACC4), modifier = Modifier.padding(18.dp).size(42.dp))
        }
        Text("No expenses recorded", color = ExpensesNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("Start by adding your first expense to track your business spending.", color = ExpensesMuted, fontSize = 13.sp)
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = ExpensesGreen), shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("Add Expense", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, ExpenseCategory) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.MISCELLANEOUS) }
    var menuOpen by remember { mutableStateOf(false) }
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
                .width(720.dp)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 18.dp
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 30.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Surface(shape = CircleShape, color = Color(0xFFE2F8EF), modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.RequestQuote, null, tint = ExpensesGreen, modifier = Modifier.size(31.dp))
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Add New Expense", color = ExpensesNavy, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("Record a business expense.", color = ExpensesMuted, fontSize = 16.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = ExpensesMuted, modifier = Modifier.size(28.dp))
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }

                ExpenseFieldLabel("Description", required = true)
                ExpenseIconField(description, { description = it; error = null }, "Enter expense description", Icons.Default.Edit)

                ExpenseFieldLabel("Amount (KES)", required = true)
                ExpenseIconField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' }; error = null }, "0.00", null, prefix = "KSh")

                ExpenseFieldLabel("Category", required = true)
                Box(Modifier.fillMaxWidth()) {
                    ExpenseIconField(category.displayName(), {}, "", Icons.Default.LocalOffer, readOnly = true) {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.width(300.dp)) {
                        ExpenseCategory.entries.forEach {
                            DropdownMenuItem(text = { Text(it.displayName()) }, onClick = { category = it; menuOpen = false })
                        }
                    }
                }

                ExpenseFieldLabel("Date", required = true)
                ExpenseIconField(today.toString(), {}, "", Icons.Default.CalendarToday, readOnly = true) {
                    Icon(Icons.Default.CalendarMonth, null, tint = ExpensesMuted, modifier = Modifier.padding(end = 16.dp))
                }

                ExpenseFieldLabel("Payment Method")
                Box(Modifier.fillMaxWidth()) {
                    ExpenseIconField(paymentMethod, {}, "", Icons.Default.CreditCard, readOnly = true) {
                        IconButton(onClick = { paymentMenuOpen = true }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                    }
                    DropdownMenu(expanded = paymentMenuOpen, onDismissRequest = { paymentMenuOpen = false }) {
                        listOf("M-Pesa", "Cash", "Card", "Bank Transfer").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { paymentMethod = it; paymentMenuOpen = false })
                        }
                    }
                }

                ExpenseFieldLabel("Notes (Optional)")
                ExpenseIconField(notes, { notes = it }, "Add any additional notes…", Icons.Default.ChatBubbleOutline)

                HorizontalDivider(color = ExpensesBorder, modifier = Modifier.padding(top = 8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.End)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(9.dp), modifier = Modifier.height(48.dp)) {
                        Text("Cancel", color = ExpensesNavy)
                    }
                    Button(
                        onClick = {
                            val parsed = amount.toDoubleOrNull()
                            when {
                                description.isBlank() -> error = "Description is required."
                                parsed == null || parsed <= 0 -> error = "Enter a valid amount greater than zero."
                                else -> onSave(
                                    buildString {
                                        append(description.trim())
                                        if (notes.isNotBlank()) append(" — ${notes.trim()}")
                                        append(" [$paymentMethod]")
                                    },
                                    parsed,
                                    category
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpensesGreen),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.height(48.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Expense", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseFieldLabel(text: String, required: Boolean = false) {
    Text(
        buildString { append(text); if (required) append("  *") },
        color = ExpensesNavy,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ExpenseIconField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector?,
    prefix: String? = null,
    readOnly: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = ExpensesMuted) },
        leadingIcon = {
            Box(
                Modifier.fillMaxHeight().width(58.dp).background(Color(0xFFECFAF5)),
                contentAlignment = Alignment.Center
            ) {
                if (prefix != null) Text(prefix, color = ExpensesGreen, fontWeight = FontWeight.Bold)
                else if (icon != null) Icon(icon, null, tint = Color(0xFF334A68), modifier = Modifier.size(22.dp))
            }
        },
        trailingIcon = trailingIcon,
        readOnly = readOnly,
        singleLine = true,
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.fillMaxWidth().height(58.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ExpensesGreen,
            unfocusedBorderColor = ExpensesBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

private fun ExpenseCategory.categoryColor(): Color = when (this) {
    ExpenseCategory.ADVERTISING -> Color(0xFF2563EB)
    ExpenseCategory.STOCK_PURCHASE -> ExpensesGreen
    ExpenseCategory.RENT, ExpenseCategory.UTILITIES -> Color(0xFFEF4444)
    ExpenseCategory.DELIVERY, ExpenseCategory.TRANSPORT -> Color(0xFFF97316)
    else -> Color(0xFF7C3AED)
}
