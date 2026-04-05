package com.app.biashara.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(onAddExpense: () -> Unit) {
    val expenses = listOf(
        Triple("Facebook Ads", "ADVERTISING", 3500.0),
        Triple("Packaging materials", "PACKAGING", 1200.0),
        Triple("Rider delivery", "DELIVERY", 800.0),
        Triple("Monthly rent", "RENT", 15000.0),
        Triple("New stock purchase", "STOCK_PURCHASE", 45000.0)
    )

    val totalExpenses = expenses.sumOf { it.third }
    val categoryColors = mapOf(
        "ADVERTISING" to B360Blue,
        "PACKAGING" to Color(0xFF7B1FA2),
        "DELIVERY" to B360Amber,
        "RENT" to B360Red,
        "STOCK_PURCHASE" to B360Green
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses / Gharama", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense, containerColor = B360Green, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(B360Red.copy(0.08f))) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total Expenses – This Month", fontSize = 13.sp, color = Color.Gray)
                        Text("KES ${"%,.0f".format(totalExpenses)}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = B360Red)
                    }
                }
            }
            items(expenses) { (desc, cat, amount) ->
                val color = categoryColors[cat] ?: Color.Gray
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(color = color.copy(0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Receipt, null, tint = color, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text(desc, fontWeight = FontWeight.Medium)
                                Surface(color = color.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                                    Text(cat.replace("_", " "), color = color, fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text("KES ${"%,.0f".format(amount)}", fontWeight = FontWeight.Bold, color = B360Red)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onSaved, containerColor = B360Green) {
                Icon(Icons.Filled.Check, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Save Expense", color = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (KES)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
        }
    }
}
