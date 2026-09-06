package com.app.biashara.ui.screens.hospitality

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
private data class AndroidTabItem(
    val id: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val notes: String = ""
)

@Serializable
private data class AndroidTabOrder(
    val id: String,
    val orderNumber: String,
    val customerName: String? = "Walk-in Guest",
    val customerPhone: String? = "",
    val deliveryLocation: String? = "Dine In",
    val serviceType: String? = "DINE_IN",
    val hospitalityTableId: String? = null,
    val tabStatus: String = "OPEN",
    val subtotal: Double = 0.0,
    val items: List<AndroidTabItem> = emptyList(),
    val createdAt: String = ""
)

@Serializable
private data class AndroidKitchenTicketItem(
    val productName: String = "",
    val quantity: Int = 1,
    val notes: String = ""
)

@Serializable
private data class AndroidKitchenTicket(
    val id: String,
    val orderId: String = "",
    val orderNumber: String,
    val tableName: String? = null,
    val station: String,
    val status: String,
    val notes: String = "",
    val items: List<AndroidKitchenTicketItem> = emptyList(),
    val createdAt: String = ""
)

@Serializable
private data class AndroidHospitalityTable(
    val id: String,
    val name: String,
    val area: String = "Main Floor",
    val capacity: Int = 4,
    val status: String = "AVAILABLE",
    val openOrderId: String? = null,
    val openAmount: Double = 0.0,
    val openOrderCount: Int = 0
)

@Serializable
private data class AndroidFullDashboard(
    val enabled: Boolean = true,
    val tables: List<AndroidHospitalityTable> = emptyList(),
    val openTabs: List<AndroidTabOrder> = emptyList(),
    val tickets: List<AndroidKitchenTicket> = emptyList()
)

@Serializable
private data class AndroidReservation(
    val id: String,
    val customerName: String,
    val guestCount: Int,
    val reservedAt: String,
    val status: String
)

@Serializable
private data class AndroidIngredient(
    val id: String,
    val name: String,
    val unit: String = "",
    val quantity: Double = 0.0,
    val reorderLevel: Double = 0.0,
    val isLowStock: Boolean = false
)

@Serializable
private data class AndroidOperationsData(
    val reservations: List<AndroidReservation> = emptyList(),
    val ingredients: List<AndroidIngredient> = emptyList()
)

@Serializable private data class UpdateTicketReq(val status: String)
@Serializable private data class CreateTableReq(val name: String, val area: String = "Main Floor", val capacity: Int = 4)
@Serializable private data class UpdateTableReq(val name: String, val area: String = "Main Floor", val capacity: Int = 4)
@Serializable private data class TransferTabReq(val tableId: String)
@Serializable private data class CloseTabReq(val paymentMethod: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalityOperationsScreen(client: HttpClient = koinInject()) {
    val scope = rememberCoroutineScope()
    var selectedSegment by remember { mutableStateOf(0) }
    var dashboard by remember { mutableStateOf<AndroidFullDashboard?>(null) }
    var operations by remember { mutableStateOf<AndroidOperationsData?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showAddTableDialog by remember { mutableStateOf(false) }
    var tableToEdit by remember { mutableStateOf<AndroidHospitalityTable?>(null) }
    var tabToTransfer by remember { mutableStateOf<AndroidTabOrder?>(null) }
    var tabToSettle by remember { mutableStateOf<AndroidTabOrder?>(null) }
    var tabForProForma by remember { mutableStateOf<AndroidTabOrder?>(null) }

    fun loadData() {
        scope.launch {
            loading = true
            error = null
            runCatching {
                val dashRes = client.get("$BASE_URL/hospitality").body<ApiResponse<AndroidFullDashboard>>()
                val opsRes = client.get("$BASE_URL/hospitality/operations").body<ApiResponse<AndroidOperationsData>>()
                dashRes to opsRes
            }.onSuccess { (dashRes, opsRes) ->
                if (dashRes.success && dashRes.data != null) dashboard = dashRes.data
                else error = dashRes.message.ifBlank { "Could not load hospitality operations." }
                if (opsRes.success && opsRes.data != null) operations = opsRes.data
            }.onFailure {
                error = it.message ?: "Failed to connect to backend service."
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
        while (isActive) {
            delay(5000)
            loadData()
        }
    }

    fun updateTicketStatus(ticketId: String, newStatus: String) {
        scope.launch {
            runCatching {
                client.patch("$BASE_URL/hospitality/tickets/$ticketId") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateTicketReq(newStatus))
                }.body<ApiResponse<AndroidKitchenTicket>>()
            }.onSuccess {
                if (it.success) {
                    successMsg = "Ticket updated to $newStatus"
                    loadData()
                } else error = it.message
            }.onFailure { error = it.message }
        }
    }

    fun handleSaveTable(name: String, area: String, capacity: Int, id: String? = null) {
        scope.launch {
            runCatching {
                if (id == null) {
                    client.post("$BASE_URL/hospitality/tables") {
                        contentType(ContentType.Application.Json)
                        setBody(CreateTableReq(name, area, capacity))
                    }.body<ApiResponse<AndroidHospitalityTable>>()
                } else {
                    client.put("$BASE_URL/hospitality/tables/$id") {
                        contentType(ContentType.Application.Json)
                        setBody(UpdateTableReq(name, area, capacity))
                    }.body<ApiResponse<AndroidHospitalityTable>>()
                }
            }.onSuccess {
                if (it.success) {
                    successMsg = if (id == null) "Table created!" else "Table updated!"
                    showAddTableDialog = false
                    tableToEdit = null
                    loadData()
                } else error = it.message
            }.onFailure { error = it.message }
        }
    }

    fun handleTransferTab(orderId: String, targetTableId: String) {
        scope.launch {
            runCatching {
                client.post("$BASE_URL/hospitality/tabs/$orderId/transfer") {
                    contentType(ContentType.Application.Json)
                    setBody(TransferTabReq(targetTableId))
                }.body<ApiResponse<AndroidTabOrder>>()
            }.onSuccess {
                if (it.success) {
                    successMsg = "Tab transferred!"
                    tabToTransfer = null
                    loadData()
                } else error = it.message
            }.onFailure { error = it.message }
        }
    }

    fun handleSettleTab(orderId: String, method: String) {
        scope.launch {
            runCatching {
                client.post("$BASE_URL/hospitality/tabs/$orderId/close") {
                    contentType(ContentType.Application.Json)
                    setBody(CloseTabReq(method))
                }.body<ApiResponse<AndroidTabOrder>>()
            }.onSuccess {
                if (it.success) {
                    successMsg = "Tab settled successfully!"
                    tabToSettle = null
                    loadData()
                } else error = it.message
            }.onFailure { error = it.message }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Hospitality Operations", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                Text("KDS, floor plan, open tabs and inventory", fontSize = 12.sp, color = Color(0xFF64748B))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { showAddTableDialog = true }) {
                    Icon(Icons.Default.AddCircle, "Add Table", tint = Color(0xFF0F1F3A))
                }
                IconButton(onClick = ::loadData, enabled = !loading) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = Color(0xFF00B874))
                }
            }
        }

        // Notification Badges
        error?.let { msg ->
            Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFCA5A5))) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = Color(0xFFB91C1C), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { error = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFFB91C1C))
                    }
                }
            }
        }

        successMsg?.let { msg ->
            Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFF86EFAC))) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = Color(0xFF15803D), fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { successMsg = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF15803D))
                    }
                }
            }
        }

        // Segmented Tab Selector Row
        ScrollableTabRow(
            selectedTabIndex = selectedSegment,
            containerColor = Color.White,
            contentColor = Color(0xFF00B874),
            edgePadding = 0.dp,
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
        ) {
            val tabs = listOf(
                "KDS Display (${dashboard?.tickets?.count { it.status in setOf("NEW", "PREPARING", "READY") } ?: 0})" to Icons.Default.Restaurant,
                "Floor Plan (${dashboard?.tables?.size ?: 0})" to Icons.Default.TableRestaurant,
                "Open Tabs (${dashboard?.openTabs?.size ?: 0})" to Icons.Default.Receipt,
                "Stock & Bookings" to Icons.Default.Inventory
            )
            tabs.forEachIndexed { index, (label, icon) ->
                Tab(
                    selected = selectedSegment == index,
                    onClick = { selectedSegment = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(label, fontSize = 12.sp, fontWeight = if (selectedSegment == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }
        }

        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFF00B874))

        dashboard?.let { dash ->
            when (selectedSegment) {
                0 -> AndroidKDSScreen(
                    tickets = dash.tickets,
                    onUpdateStatus = ::updateTicketStatus
                )
                1 -> AndroidFloorPlanScreen(
                    tables = dash.tables,
                    onEditTable = { tableToEdit = it },
                    onTransferTab = { table ->
                        val active = dash.openTabs.firstOrNull { it.hospitalityTableId == table.id }
                        if (active != null) tabToTransfer = active
                    }
                )
                2 -> AndroidOpenTabsScreen(
                    openTabs = dash.openTabs,
                    onSettle = { tabToSettle = it },
                    onTransfer = { tabToTransfer = it },
                    onProForma = { tabForProForma = it }
                )
                3 -> AndroidOperationsSection(
                    operations = operations
                )
            }
        }
    }

    if (showAddTableDialog || tableToEdit != null) {
        AndroidTableFormModal(
            table = tableToEdit,
            onDismiss = { showAddTableDialog = false; tableToEdit = null },
            onSave = { name, area, capacity -> handleSaveTable(name, area, capacity, tableToEdit?.id) }
        )
    }

    tabToTransfer?.let { tab ->
        AndroidTransferTabModal(
            tab = tab,
            tables = dashboard?.tables.orEmpty(),
            onDismiss = { tabToTransfer = null },
            onConfirm = { targetTableId -> handleTransferTab(tab.id, targetTableId) }
        )
    }

    tabToSettle?.let { tab ->
        AndroidSettleTabModal(
            tab = tab,
            onDismiss = { tabToSettle = null },
            onConfirm = { method -> handleSettleTab(tab.id, method) }
        )
    }

    tabForProForma?.let { tab ->
        AndroidProFormaModal(
            tab = tab,
            onDismiss = { tabForProForma = null }
        )
    }
}

@Composable
private fun AndroidKDSScreen(
    tickets: List<AndroidKitchenTicket>,
    onUpdateStatus: (ticketId: String, status: String) -> Unit
) {
    var selectedStation by remember { mutableStateOf("ALL") }
    val filtered = when (selectedStation) {
        "KITCHEN" -> tickets.filter { it.station == "KITCHEN" }
        "BAR" -> tickets.filter { it.station == "BAR" }
        else -> tickets
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Station:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            listOf("ALL", "KITCHEN", "BAR").forEach { station ->
                FilterChip(
                    selected = selectedStation == station,
                    onClick = { selectedStation = station },
                    label = { Text(station, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00B874), selectedLabelColor = Color.White)
                )
            }
        }

        if (filtered.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Box(Modifier.padding(30.dp), contentAlignment = Alignment.Center) {
                    Text("No active kitchen or bar order tickets.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { ticket ->
                    val statusBg = when (ticket.status) {
                        "NEW" -> Color(0xFFEFF6FF)
                        "PREPARING" -> Color(0xFFFEF3C7)
                        "READY" -> Color(0xFFDCFCE7)
                        else -> Color(0xFFF1F5F9)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = statusBg),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("Order #${ticket.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F1F3A))
                                    Text("Table: ${ticket.tableName ?: "Takeaway"}", fontSize = 12.sp, color = Color(0xFF475569))
                                }
                                Surface(color = if (ticket.station == "KITCHEN") Color(0xFF0F1F3A) else Color(0xFF2563EB), shape = RoundedCornerShape(10.dp)) {
                                    Text(ticket.station, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.5f))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ticket.items.forEach { item ->
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("${item.quantity}x ${item.productName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        if (item.notes.isNotBlank()) Text("(${item.notes})", fontSize = 11.sp, color = Color(0xFFDC2626))
                                    }
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                when (ticket.status) {
                                    "NEW" -> Button(onClick = { onUpdateStatus(ticket.id, "PREPARING") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))) { Text("Start Prep", fontSize = 12.sp) }
                                    "PREPARING" -> Button(onClick = { onUpdateStatus(ticket.id, "READY") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))) { Text("Mark Ready", fontSize = 12.sp) }
                                    "READY" -> Button(onClick = { onUpdateStatus(ticket.id, "SERVED") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))) { Text("Mark Served", fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidFloorPlanScreen(
    tables: List<AndroidHospitalityTable>,
    onEditTable: (AndroidHospitalityTable) -> Unit,
    onTransferTab: (AndroidHospitalityTable) -> Unit
) {
    var selectedArea by remember { mutableStateOf("ALL") }
    val areas = remember(tables) { listOf("ALL") + tables.map { it.area }.distinct() }
    val filtered = if (selectedArea == "ALL") tables else tables.filter { it.area == selectedArea }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(areas) { area ->
                FilterChip(
                    selected = selectedArea == area,
                    onClick = { selectedArea = area },
                    label = { Text(if (area == "ALL") "All Areas" else area, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF0F1F3A), selectedLabelColor = Color.White)
                )
            }
        }

        if (filtered.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Box(Modifier.padding(30.dp), contentAlignment = Alignment.Center) { Text("No tables found in area.", color = Color(0xFF64748B)) }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { table ->
                    val isOccupied = table.status == "OCCUPIED" || table.openOrderCount > 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (isOccupied) Color(0xFFFECDD3) else Color(0xFFE2E8F0))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text(table.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F1F3A))
                                    Text("${table.area} · ${table.capacity} Seats", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                Text(
                                    table.status,
                                    color = if (isOccupied) Color(0xFFE11D48) else Color(0xFF059669),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("${table.openOrderCount} Tabs Open", fontSize = 12.sp, color = Color(0xFF64748B))
                                Text("KES ${String.format("%,.0f", table.openAmount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF00B874))
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onEditTable(table) }, modifier = Modifier.weight(1f)) {
                                    Text("Edit", fontSize = 11.sp)
                                }
                                if (isOccupied) {
                                    Button(onClick = { onTransferTab(table) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))) {
                                        Text("Transfer", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidOpenTabsScreen(
    openTabs: List<AndroidTabOrder>,
    onSettle: (AndroidTabOrder) -> Unit,
    onTransfer: (AndroidTabOrder) -> Unit,
    onProForma: (AndroidTabOrder) -> Unit
) {
    if (openTabs.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Box(Modifier.padding(40.dp), contentAlignment = Alignment.Center) { Text("No open customer tabs.", color = Color(0xFF64748B)) }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(openTabs, key = { it.id }) { tab ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column {
                                Text("Tab #${tab.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${tab.deliveryLocation} · ${tab.customerName}", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                            Text("KES ${String.format("%,.0f", tab.subtotal)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF00B874))
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { onProForma(tab) }, modifier = Modifier.weight(1f)) { Text("Bill", fontSize = 11.sp) }
                            OutlinedButton(onClick = { onTransfer(tab) }, modifier = Modifier.weight(1f)) { Text("Transfer", fontSize = 11.sp) }
                            Button(onClick = { onSettle(tab) }, modifier = Modifier.weight(1.2f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B874))) { Text("Settle", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidOperationsSection(operations: AndroidOperationsData?) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MobileSummary("Bookings", operations?.reservations?.count { it.status == "BOOKED" } ?: 0, Modifier.weight(1f))
                MobileSummary("Low Stock", operations?.ingredients?.count { it.isLowStock } ?: 0, Modifier.weight(1f))
            }
        }

        item { Text("Upcoming Reservations", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F1F3A)) }

        val resList = operations?.reservations.orEmpty()
        if (resList.isEmpty()) {
            item { Card(modifier = Modifier.fillMaxWidth()) { Text("No active reservations.", Modifier.padding(14.dp), color = Color.Gray, fontSize = 12.sp) } }
        } else {
            items(resList) { res ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column {
                            Text(res.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${res.guestCount} guests · ${res.reservedAt}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(res.status, fontWeight = FontWeight.Bold, color = Color(0xFF00B874), fontSize = 12.sp)
                    }
                }
            }
        }

        item { Text("Ingredient Stock Levels", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F1F3A)) }

        val ingList = operations?.ingredients.orEmpty()
        if (ingList.isEmpty()) {
            item { Card(modifier = Modifier.fillMaxWidth()) { Text("No ingredient items found.", Modifier.padding(14.dp), color = Color.Gray, fontSize = 12.sp) } }
        } else {
            items(ingList) { ing ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (ing.isLowStock) Color(0xFFFFF7ED) else Color.White)) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                        Column {
                            Text(ing.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Reorder at ${ing.reorderLevel} ${ing.unit}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text("${ing.quantity} ${ing.unit}", fontWeight = FontWeight.Bold, color = if (ing.isLowStock) Color(0xFFD97706) else Color(0xFF0F1F3A), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileSummary(label: String, value: Int, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// Android Modals
@Composable
private fun AndroidTableFormModal(
    table: AndroidHospitalityTable?,
    onDismiss: () -> Unit,
    onSave: (name: String, area: String, capacity: Int) -> Unit
) {
    var name by remember { mutableStateOf(table?.name.orEmpty()) }
    var area by remember { mutableStateOf(table?.area ?: "Main Floor") }
    var capacityText by remember { mutableStateOf((table?.capacity ?: 4).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (table == null) "Add Table" else "Edit Table #${table.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Table Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = capacityText, onValueChange = { capacityText = it.filter { c -> c.isDigit() } }, label = { Text("Capacity") }, modifier = Modifier.fillMaxWidth())

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { if (name.isNotBlank()) onSave(name.trim(), area.trim(), capacityText.toIntOrNull() ?: 4) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun AndroidTransferTabModal(
    tab: AndroidTabOrder,
    tables: List<AndroidHospitalityTable>,
    onDismiss: () -> Unit,
    onConfirm: (targetTableId: String) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val available = tables.filter { it.id != tab.hospitalityTableId }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Transfer Tab #${tab.orderNumber}", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                LazyColumn(Modifier.height(160.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(available) { t ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { selectedId = t.id },
                            color = if (selectedId == t.id) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (selectedId == t.id) Color(0xFF00B874) else Color(0xFFE2E8F0))
                        ) {
                            Row(Modifier.padding(10.dp), Arrangement.SpaceBetween) {
                                Text(t.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${t.area} · ${t.capacity} seats", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { selectedId?.let(onConfirm) }, enabled = selectedId != null) { Text("Transfer") }
                }
            }
        }
    }
}

@Composable
private fun AndroidSettleTabModal(
    tab: AndroidTabOrder,
    onDismiss: () -> Unit,
    onConfirm: (paymentMethod: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("CASH") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Settle Tab #${tab.orderNumber}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Amount Due: KES ${String.format("%,.2f", tab.subtotal)}", fontWeight = FontWeight.Bold, color = Color(0xFF00B874))

                listOf("CASH" to "Cash", "MPESA" to "M-Pesa", "CARD" to "Card").forEach { (code, label) ->
                    Row(Modifier.fillMaxWidth().clickable { selectedMethod = code }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedMethod == code, onClick = { selectedMethod = code })
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(selectedMethod) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B874))) { Text("Settle") }
                }
            }
        }
    }
}

@Composable
private fun AndroidProFormaModal(
    tab: AndroidTabOrder,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PRO-FORMA BILL", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Tab #${tab.orderNumber} · Table: ${tab.deliveryLocation}", fontSize = 12.sp, color = Color.Gray)

                HorizontalDivider()

                LazyColumn(Modifier.height(140.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tab.items) { item ->
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("${item.quantity}x ${item.productName}", fontSize = 12.sp)
                            Text("KES ${String.format("%,.2f", item.unitPrice * item.quantity)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider()

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("TOTAL DUE:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("KES ${String.format("%,.2f", tab.subtotal)}", fontWeight = FontWeight.Bold, color = Color(0xFF00B874), fontSize = 15.sp)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
