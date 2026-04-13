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
import com.app.biashara.UserSession
import com.app.biashara.domain.model.Customer
import com.app.biashara.domain.usecase.generateId
import com.app.biashara.presentation.viewmodel.CustomersViewModel
import com.app.biashara.ui.theme.*
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    onCustomerDetail: (String) -> Unit,
    viewModel: CustomersViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newLocation by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadCustomers() }

    val saveResult by viewModel.saveResult.collectAsState(initial = null)
    LaunchedEffect(saveResult) {
        saveResult?.let {
            saving = false
            if (it.isSuccess) {
                showAddSheet = false
                newName = ""; newPhone = ""; newEmail = ""; newLocation = ""
            }
        }
    }

    if (showAddSheet) {
        AlertDialog(
            onDismissRequest = { showAddSheet = false },
            title = { Text("Add Customer / Ongeza Mteja") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it },
                        label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, enabled = !saving)
                    OutlinedTextField(value = newPhone, onValueChange = { newPhone = it },
                        label = { Text("Phone (07XX) *") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, enabled = !saving)
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it },
                        label = { Text("Email (optional)") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, enabled = !saving)
                    OutlinedTextField(value = newLocation, onValueChange = { newLocation = it },
                        label = { Text("Location (optional)") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, enabled = !saving)
                    if (state.error != null) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank()) {
                            saving = true
                            val now = Clock.System.now()
                            viewModel.saveCustomer(
                                Customer(
                                    id = generateId(),
                                    businessId = UserSession.getBusinessId(),
                                    name = newName,
                                    phone = newPhone,
                                    email = newEmail.ifBlank { null },
                                    location = newLocation,
                                    createdAt = now,
                                    updatedAt = now
                                )
                            )
                        }
                    },
                    enabled = !saving && newName.isNotBlank() && newPhone.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = B360Green)
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSheet = false; viewModel.dismissError() }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customers / Wateja", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true; viewModel.dismissError() },
                containerColor = B360Green,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "Add Customer")
            }
        }
    ) { padding ->
        if (state.isLoading && state.customers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search customers...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton({ viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, null)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (state.filteredCustomers.isEmpty() && !state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.People, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Text("No customers yet", color = Color.Gray)
                        Button(onClick = { showAddSheet = true }, colors = ButtonDefaults.buttonColors(containerColor = B360Green)) {
                            Text("Add first customer")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredCustomers) { customer ->
                        CustomerCard(customer = customer, onClick = { onCustomerDetail(customer.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun CustomerCard(customer: Customer, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = B360Green.copy(0.1f), shape = RoundedCornerShape(50), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(customer.name.first().uppercase(), fontWeight = FontWeight.Bold, color = B360Green, fontSize = 20.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.SemiBold)
                Text(customer.phone, color = Color.Gray, fontSize = 12.sp)
                if (customer.location.isNotBlank()) {
                    Text(customer.location, color = Color.Gray, fontSize = 12.sp)
                }
            }
            if (customer.loyaltyPoints > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐ ${customer.loyaltyPoints}", fontSize = 12.sp, color = B360Amber, fontWeight = FontWeight.Bold)
                    Text("pts", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: String,
    onBack: () -> Unit,
    viewModel: CustomersViewModel = koinInject()
) {
    val detailState by viewModel.detailState.collectAsState()

    LaunchedEffect(customerId) { viewModel.loadCustomerDetail(customerId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detailState.customer?.name ?: "Customer Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        if (detailState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = B360Green)
            }
            return@Scaffold
        }

        val customer = detailState.customer ?: return@Scaffold
        val stats = detailState.stats

        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(color = B360Green.copy(0.1f), shape = RoundedCornerShape(50), modifier = Modifier.size(64.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(customer.name.first().uppercase(), fontWeight = FontWeight.Bold, color = B360Green, fontSize = 28.sp)
                        }
                    }
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(customer.phone, color = Color.Gray)
                        if (!customer.email.isNullOrBlank()) Text(customer.email!!, color = Color.Gray, fontSize = 13.sp)
                        if (customer.loyaltyPoints > 0) {
                            Text("⭐ ${customer.loyaltyPoints} pts", color = B360Amber, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            if (stats != null) {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Purchase History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Orders")
                            Text("${stats.totalOrders}", fontWeight = FontWeight.Bold)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Spent")
                            Text("KES ${"%,.0f".format(stats.totalSpent)}", color = B360Green, fontWeight = FontWeight.Bold)
                        }
                        if (stats.averageOrderValue > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Avg Order Value")
                                Text("KES ${"%,.0f".format(stats.averageOrderValue)}")
                            }
                        }
                    }
                }
            }
        }
    }
}
