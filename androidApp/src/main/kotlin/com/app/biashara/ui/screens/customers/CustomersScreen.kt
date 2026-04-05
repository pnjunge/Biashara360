package com.app.biashara.ui.screens.customers

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
fun CustomersScreen(onCustomerDetail: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    val customers = listOf(
        Triple("1", "Amina Hassan", Pair(12, 54_000.0)),
        Triple("2", "Grace Njeri", Pair(8, 31_200.0)),
        Triple("3", "Brian Otieno", Pair(5, 18_500.0)),
        Triple("4", "Mary Akinyi", Pair(3, 9_800.0)),
        Triple("5", "David Kamau", Pair(2, 4_200.0))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customers / Wateja", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {}, containerColor = B360Green, contentColor = Color.White) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search customers...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(customers.filter { it.second.contains(searchQuery, ignoreCase = true) }) { (id, name, stats) ->
                    CustomerCard(id, name, stats.first, stats.second, onClick = { onCustomerDetail(id) })
                }
            }
        }
    }
}

@Composable
fun CustomerCard(id: String, name: String, orders: Int, spent: Double, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick, shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = B360Green.copy(0.1f), shape = RoundedCornerShape(50), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.first().toString(), fontWeight = FontWeight.Bold, color = B360Green, fontSize = 20.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text("$orders orders · KES ${"%,.0f".format(spent)}", color = Color.Gray, fontSize = 12.sp)
            }
            if (orders > 1) {
                Icon(Icons.Filled.Repeat, contentDescription = "Repeat", tint = B360Blue, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(customerId: String, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer Info", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Orders"); Text("12", fontWeight = FontWeight.Bold) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Spent"); Text("KES 54,000", color = B360Green, fontWeight = FontWeight.Bold) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Outstanding"); Text("KES 0") }
                }
            }
        }
    }
}
