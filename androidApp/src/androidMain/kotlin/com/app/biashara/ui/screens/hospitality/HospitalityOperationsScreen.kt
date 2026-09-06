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
import androidx.compose.ui.graphics.vector.ImageVector
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
    val id: String = "",
    val tableId: String? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val guestCount: Int = 1,
    val reservedAt: String = "",
    val durationMinutes: Int = 90,
    val status: String = "BOOKED",
    val notes: String = ""
)

@Serializable
private data class AndroidIngredient(
    val id: String = "",
    val name: String = "",
    val unit: String = "",
    val quantity: Double = 0.0,
    val reorderLevel: Double = 0.0,
    val isLowStock: Boolean = false
)

@Serializable private data class AndroidMenuOption(val name: String = "", val priceDelta: Double = 0.0)
@Serializable private data class AndroidMenuProfile(val productId: String = "", val preparationStation: String? = null, val mealPeriods: List<String> = emptyList(), val sizes: List<AndroidMenuOption> = emptyList(), val extras: List<AndroidMenuOption> = emptyList(), val variants: List<AndroidMenuOption> = emptyList(), val comboProductIds: List<String> = emptyList(), val soldOut: Boolean = false, val happyHourPrice: Double? = null, val happyHourStart: String? = null, val happyHourEnd: String? = null, val ageRestricted: Boolean = false, val minimumAge: Int? = null)
@Serializable private data class AndroidShift(val id: String = "", val openedBy: String = "", val openedAt: String = "", val closedAt: String? = null, val openingFloat: Double = 0.0, val expectedCash: Double? = null, val actualCash: Double? = null, val mpesaTotal: Double? = null, val cardTotal: Double? = null, val tipsTotal: Double = 0.0, val expensesTotal: Double = 0.0, val status: String = "OPEN", val variance: Double? = null, val actualMpesa: Double? = null, val actualCard: Double? = null, val mpesaVariance: Double? = null, val cardVariance: Double? = null, val totalVariance: Double? = null)
@Serializable private data class AndroidSupplier(val id: String = "", val name: String = "", val phone: String = "", val email: String? = null, val address: String? = null, val isActive: Boolean = true)
@Serializable private data class AndroidPurchaseOrder(val id: String = "", val orderNumber: String = "", val supplierId: String = "", val status: String = "ORDERED", val totalCost: Double = 0.0, val orderedAt: String = "", val receivedAt: String? = null)
@Serializable private data class AndroidApproval(val id: String = "", val actionType: String = "", val entityType: String = "", val entityId: String = "", val requestedBy: String = "", val approvedBy: String? = null, val status: String = "PENDING", val reason: String = "", val requestedAt: String = "")
@Serializable private data class AndroidProduct(val id: String = "", val name: String = "", val category: String = "", val sellingPrice: Double = 0.0)
@Serializable private data class AndroidReportBreakdown(val label: String = "", val count: Int = 0, val amount: Double = 0.0)
@Serializable private data class AndroidReport(val byLocation: List<AndroidReportBreakdown> = emptyList(), val byChannel: List<AndroidReportBreakdown> = emptyList(), val foodCost: Double = 0.0, val beverageCost: Double = 0.0, val wastageCost: Double = 0.0, val grossMargin: Double = 0.0, val averageTableTurnoverMinutes: Double = 0.0)
@Serializable
private data class AndroidOperationsData(
    val reservations: List<AndroidReservation> = emptyList(),
    val menuProfiles: List<AndroidMenuProfile> = emptyList(),
    val ingredients: List<AndroidIngredient> = emptyList(),
    val shifts: List<AndroidShift> = emptyList(),
    val suppliers: List<AndroidSupplier> = emptyList(),
    val purchaseOrders: List<AndroidPurchaseOrder> = emptyList(),
    val approvals: List<AndroidApproval> = emptyList()
)

@Serializable private data class AndroidPortalOrderItem(val name: String = "", val quantity: Int = 1)
@Serializable private data class AndroidPortalOrder(val id: String = "", val orderNumber: String = "", val customerName: String = "", val location: String = "", val amount: Double = 0.0, val paymentStatus: String = "", val createdAt: String = "", val claimedBy: String? = null, val items: List<AndroidPortalOrderItem> = emptyList())
@Serializable private data class AndroidPortalQueue(val waiting: List<AndroidPortalOrder> = emptyList(), val mine: List<AndroidPortalOrder> = emptyList())

private fun androidPortalOrderAge(createdAt: String): String {
    val instant = runCatching { java.time.Instant.parse(createdAt) }.getOrNull() ?: return ""
    val minutes = java.time.Duration.between(instant, java.time.Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60} hr ago"
        else -> "${minutes / 1440} d ago"
    }
}

@Composable
private fun AndroidPortalMeta(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
        Text(text, color = Color(0xFF475569), fontSize = 11.sp)
    }
}

@Serializable private data class UpdateTicketReq(val status: String)
@Serializable private data class CreateTableReq(val name: String, val area: String = "Main Floor", val capacity: Int = 4)
@Serializable private data class UpdateTableReq(val name: String, val area: String = "Main Floor", val capacity: Int = 4)
@Serializable private data class TransferTabReq(val tableId: String)
@Serializable private data class CloseTabReq(val paymentMethod: String)
@Serializable private data class AndroidShiftOpenReq(val openingFloat: Double, val notes: String = "")
@Serializable private data class AndroidShiftCloseReq(val actualCash: Double, val actualMpesa: Double, val actualCard: Double, val tipsTotal: Double = 0.0, val expensesTotal: Double = 0.0, val notes: String = "")
@Serializable private data class AndroidApprovalDecisionReq(val approved: Boolean)
@Serializable private data class AndroidMenuProfileReq(val preparationStation: String? = null, val mealPeriods: List<String> = emptyList(), val sizes: List<AndroidMenuOption> = emptyList(), val extras: List<AndroidMenuOption> = emptyList(), val variants: List<AndroidMenuOption> = emptyList(), val comboProductIds: List<String> = emptyList(), val soldOut: Boolean = false, val happyHourPrice: Double? = null, val happyHourStart: String? = null, val happyHourEnd: String? = null, val ageRestricted: Boolean = false, val minimumAge: Int? = null)
@Serializable private data class AndroidSupplierReq(val name: String, val phone: String = "", val email: String? = null, val address: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalityOperationsScreen(client: HttpClient = koinInject()) {
    val scope = rememberCoroutineScope()
    var selectedSegment by remember { mutableStateOf(0) }
    var dashboard by remember { mutableStateOf<AndroidFullDashboard?>(null) }
    var operations by remember { mutableStateOf<AndroidOperationsData?>(null) }
    var products by remember { mutableStateOf<List<AndroidProduct>>(emptyList()) }
    var report by remember { mutableStateOf<AndroidReport?>(null) }
    var portalQueue by remember { mutableStateOf(AndroidPortalQueue()) }
    var portalOpen by remember { mutableStateOf(false) }
    var portalMine by remember { mutableStateOf(false) }
    var portalBusy by remember { mutableStateOf<String?>(null) }
    var portalMessage by remember { mutableStateOf<String?>(null) }
    var knownPortalIds by remember { mutableStateOf<Set<String>>(emptySet()) }
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
                val portalRes = runCatching { client.get("$BASE_URL/portal-orders").body<ApiResponse<AndroidPortalQueue>>() }.getOrNull()
                val productsRes = runCatching { client.get("$BASE_URL/products").body<ApiResponse<List<AndroidProduct>>>() }.getOrNull()
                listOf(dashRes, opsRes, portalRes, productsRes)
            }.onSuccess { results ->
                val dashRes = results[0] as ApiResponse<AndroidFullDashboard>
                val opsRes = results[1] as ApiResponse<AndroidOperationsData>
                @Suppress("UNCHECKED_CAST") val portalRes = results[2] as ApiResponse<AndroidPortalQueue>?
                @Suppress("UNCHECKED_CAST") val productsRes = results[3] as ApiResponse<List<AndroidProduct>>?
                if (dashRes.success && dashRes.data != null) dashboard = dashRes.data
                else error = dashRes.message.ifBlank { "Could not load hospitality operations." }
                if (opsRes.success && opsRes.data != null) operations = opsRes.data
                val portalData = portalRes?.data
                if (portalRes?.success == true && portalData != null) {
                    val waiting = portalData.waiting
                    if (waiting.any { it.id !in knownPortalIds }) portalOpen = true
                    knownPortalIds = waiting.map { it.id }.toSet()
                    portalQueue = portalData
                }
                val productData = productsRes?.data
                if (productsRes?.success == true && productData != null) products = productData
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

    fun operationAction(successText: String, block: suspend () -> ApiResponse<*>) {
        scope.launch {
            runCatching { block() }.onSuccess {
                if (it.success) { successMsg = successText; loadData() } else error = it.message
            }.onFailure { error = it.message ?: "Hospitality operation failed." }
        }
    }

    fun updateReservationStatus(id: String, status: String) = operationAction("Reservation updated.") { client.patch("$BASE_URL/hospitality/operations/reservations/$id/$status").body<ApiResponse<AndroidReservation>>() }
    fun receivePurchaseOrder(id: String) = operationAction("Purchase order received.") { client.post("$BASE_URL/hospitality/operations/purchase-orders/$id/receive").body<ApiResponse<AndroidPurchaseOrder>>() }
    fun decideApproval(id: String, approved: Boolean) = operationAction(if (approved) "Approval granted." else "Approval rejected.") { client.post("$BASE_URL/hospitality/operations/approvals/$id/decision") { contentType(ContentType.Application.Json); setBody(AndroidApprovalDecisionReq(approved)) }.body<ApiResponse<AndroidApproval>>() }
    fun openShift(openingFloat: Double) = operationAction("Shift opened.") { client.post("$BASE_URL/hospitality/operations/shifts/open") { contentType(ContentType.Application.Json); setBody(AndroidShiftOpenReq(openingFloat)) }.body<ApiResponse<AndroidShift>>() }
    fun closeShift(id: String, actualCash: Double, actualMpesa: Double, actualCard: Double, tips: Double, expenses: Double) = operationAction("Day closed and payments reconciled.") { client.post("$BASE_URL/hospitality/operations/shifts/$id/close") { contentType(ContentType.Application.Json); setBody(AndroidShiftCloseReq(actualCash, actualMpesa, actualCard, tips, expenses)) }.body<ApiResponse<AndroidShift>>() }
    fun saveMenuProfile(profile: AndroidMenuProfile, station: String, soldOut: Boolean) = operationAction("Menu controls saved.") { client.put("$BASE_URL/hospitality/operations/menu/${profile.productId}") { contentType(ContentType.Application.Json); setBody(AndroidMenuProfileReq(station.takeUnless { it == "NONE" }, profile.mealPeriods, profile.sizes, profile.extras, profile.variants, profile.comboProductIds, soldOut, profile.happyHourPrice, profile.happyHourStart, profile.happyHourEnd, profile.ageRestricted, profile.minimumAge)) }.body<ApiResponse<AndroidMenuProfile>>() }
    fun createSupplier(name: String, phone: String, email: String) = operationAction("Supplier created.") { client.post("$BASE_URL/hospitality/operations/suppliers") { contentType(ContentType.Application.Json); setBody(AndroidSupplierReq(name, phone, email.takeIf { it.isNotBlank() })) }.body<ApiResponse<AndroidSupplier>>() }
    fun loadReport(startDate: String, endDate: String) {
        scope.launch {
            runCatching { client.get("$BASE_URL/hospitality/operations/report?startDate=$startDate&endDate=$endDate").body<ApiResponse<AndroidReport>>() }
                .onSuccess { response -> if (response.success && response.data != null) { report = response.data; successMsg = "Operations report loaded." } else error = response.message }
                .onFailure { error = it.message ?: "Could not load report." }
        }
    }

    fun claimPortalOrder(order: AndroidPortalOrder) {
        if (portalBusy != null) return
        portalBusy = order.id
        portalMessage = null
        scope.launch {
            runCatching { client.post("$BASE_URL/portal-orders/${order.id}/claim").body<ApiResponse<AndroidPortalOrder>>() }.onSuccess { response ->
                val claimed = response.data
                if (response.success && claimed != null) {
                    portalQueue = portalQueue.copy(waiting = portalQueue.waiting.filterNot { it.id == order.id }, mine = listOf(claimed) + portalQueue.mine.filterNot { it.id == order.id })
                    portalMine = true
                    portalMessage = "Claimed ${order.orderNumber}."
                } else error = response.message
            }.onFailure { error = it.message ?: "Could not claim portal order." }
            portalBusy = null
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
                OutlinedButton(
                    onClick = { portalOpen = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF047857))
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Portal orders${if (portalQueue.waiting.isNotEmpty()) " (${portalQueue.waiting.size})" else ""}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
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

        if (operations != null && operations?.shifts?.none { it.status == "OPEN" } == true) {
            Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFCD34D))) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start of day required: open a shift before taking hospitality orders or settling payments.", color = Color(0xFF92400E), fontSize = 12.sp)
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
                    operations = operations,
                    products = products,
                    report = report,
                    onReservationStatus = ::updateReservationStatus,
                    onReceivePurchaseOrder = ::receivePurchaseOrder,
                    onDecideApproval = ::decideApproval,
                    onOpenShift = ::openShift,
                    onCloseShift = ::closeShift,
                    onSaveMenuProfile = ::saveMenuProfile,
                    onCreateSupplier = ::createSupplier,
                    onRunReport = ::loadReport
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

    if (portalOpen) {
        AndroidPortalOrdersDialog(
            queue = portalQueue,
            showingMine = portalMine,
            busyId = portalBusy,
            message = portalMessage,
            onShowingMine = { portalMine = it },
            onClaim = ::claimPortalOrder,
            onDismiss = { portalOpen = false }
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
private fun AndroidOperationsSection(
    operations: AndroidOperationsData?,
    products: List<AndroidProduct>,
    report: AndroidReport?,
    onReservationStatus: (String, String) -> Unit,
    onReceivePurchaseOrder: (String) -> Unit,
    onDecideApproval: (String, Boolean) -> Unit,
    onOpenShift: (Double) -> Unit,
    onCloseShift: (String, Double, Double, Double, Double, Double) -> Unit,
    onSaveMenuProfile: (AndroidMenuProfile, String, Boolean) -> Unit,
    onCreateSupplier: (String, String, String) -> Unit,
    onRunReport: (String, String) -> Unit
) {
    var section by remember { mutableStateOf("RESERVATIONS") }
    val sections = listOf("RESERVATIONS", "MENU", "SHIFTS", "PURCHASING", "APPROVALS", "REPORTS")
    val data = operations ?: AndroidOperationsData()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MobileSummary("Bookings", data.reservations.count { it.status == "BOOKED" }, Modifier.weight(1f))
                MobileSummary("Low Stock", data.ingredients.count { it.isLowStock }, Modifier.weight(1f))
                MobileSummary("Approvals", data.approvals.count { it.status == "PENDING" }, Modifier.weight(1f))
            }
        }
        item {
            ScrollableTabRow(selectedTabIndex = sections.indexOf(section), edgePadding = 0.dp, containerColor = Color.Transparent, divider = {}) {
                sections.forEach { name ->
                    Tab(selected = section == name, onClick = { section = name }, text = { Text(name.lowercase().replaceFirstChar { it.titlecase() }, fontSize = 11.sp) })
                }
            }
        }

        when (section) {
            "RESERVATIONS" -> {
                if (data.reservations.isEmpty()) item { AndroidEmptyCard("No active reservations.") }
                items(data.reservations, key = { it.id }) { reservation ->
                    AndroidReservationCard(reservation, onReservationStatus)
                }
            }
            "MENU" -> {
                item { AndroidMenuProfilesCard(data.menuProfiles, products, onSaveMenuProfile) }
            }
            "SHIFTS" -> {
                item { AndroidShiftsCard(data.shifts, onOpenShift, onCloseShift) }
            }
            "PURCHASING" -> {
                item { AndroidPurchasingCard(data, onReceivePurchaseOrder, onCreateSupplier) }
            }
            "APPROVALS" -> {
                if (data.approvals.none { it.status == "PENDING" }) item { AndroidEmptyCard("No pending approvals.") }
                items(data.approvals.filter { it.status == "PENDING" }, key = { it.id }) { approval ->
                    AndroidApprovalCard(approval, onDecideApproval)
                }
            }
            "REPORTS" -> {
                item { AndroidReportsCard(report, onRunReport) }
            }
        }

        item {
            Text("Ingredient stock", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F1F3A))
        }
        if (data.ingredients.isEmpty()) item { AndroidEmptyCard("No ingredient items found.") }
        items(data.ingredients, key = { it.id }) { ingredient ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (ingredient.isLowStock) Color(0xFFFFF7ED) else Color.White)) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column {
                        Text(ingredient.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Reorder at ${ingredient.reorderLevel} ${ingredient.unit}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("${ingredient.quantity} ${ingredient.unit}", fontWeight = FontWeight.Bold, color = if (ingredient.isLowStock) Color(0xFFD97706) else Color(0xFF0F1F3A), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun AndroidEmptyCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Text(message, Modifier.padding(14.dp), color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun AndroidReservationCard(reservation: AndroidReservation, onStatus: (String, String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(reservation.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${reservation.guestCount} guests · ${reservation.reservedAt}", fontSize = 11.sp, color = Color.Gray)
                }
                Text(reservation.status, fontWeight = FontWeight.Bold, color = Color(0xFF00B874), fontSize = 12.sp)
            }
            if (reservation.notes.isNotBlank()) Text(reservation.notes, fontSize = 11.sp, color = Color(0xFF475569))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (reservation.status) {
                    "BOOKED" -> {
                        Button(onClick = { onStatus(reservation.id, "SEATED") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 5.dp)) { Text("Seat", fontSize = 11.sp) }
                        OutlinedButton(onClick = { onStatus(reservation.id, "CANCELLED") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 5.dp)) { Text("Cancel", fontSize = 11.sp) }
                    }
                    "SEATED" -> Button(onClick = { onStatus(reservation.id, "COMPLETED") }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 5.dp)) { Text("Complete", fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun AndroidMenuProfilesCard(profiles: List<AndroidMenuProfile>, products: List<AndroidProduct>, onSave: (AndroidMenuProfile, String, Boolean) -> Unit) {
    var selectedId by remember(profiles) { mutableStateOf(profiles.firstOrNull()?.productId) }
    var station by remember(selectedId, profiles) { mutableStateOf(profiles.firstOrNull { it.productId == selectedId }?.preparationStation ?: "NONE") }
    var soldOut by remember(selectedId, profiles) { mutableStateOf(profiles.firstOrNull { it.productId == selectedId }?.soldOut ?: false) }
    val profile = profiles.firstOrNull { it.productId == selectedId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Menu profiles & routing", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F1F3A))
        if (profiles.isEmpty()) AndroidEmptyCard("No menu profiles configured.") else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(profiles, key = { it.productId }) { item ->
                    val productName = products.firstOrNull { it.id == item.productId }?.name ?: item.productId.take(8)
                    FilterChip(selected = selectedId == item.productId, onClick = { selectedId = item.productId }, label = { Text(productName, fontSize = 11.sp) })
                }
            }
            if (profile != null) {
                Text("Preparation station", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("NONE", "KITCHEN", "BAR").forEach { value ->
                        FilterChip(selected = station == value, onClick = { station = value }, label = { Text(value, fontSize = 10.sp) })
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Sold out", fontSize = 12.sp)
                    Switch(checked = soldOut, onCheckedChange = { soldOut = it })
                }
                Button(onClick = { onSave(profile, station, soldOut) }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp)) { Text("Save menu controls") }
            }
        }
    }
}

@Composable
private fun AndroidShiftsCard(shifts: List<AndroidShift>, onOpen: (Double) -> Unit, onClose: (String, Double, Double, Double, Double, Double) -> Unit) {
    var openingFloat by remember { mutableStateOf("") }
    var actualCash by remember { mutableStateOf("") }
    var actualMpesa by remember { mutableStateOf("") }
    var actualCard by remember { mutableStateOf("") }
    var tips by remember { mutableStateOf("") }
    var expenses by remember { mutableStateOf("") }
    val openShift = shifts.firstOrNull { it.status == "OPEN" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Start and end of day", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F1F3A))
        if (openShift == null) {
            Text("Open a shift before taking hospitality orders or settling payments.", fontSize = 11.sp, color = Color(0xFF64748B))
            OutlinedTextField(openingFloat, { openingFloat = it }, label = { Text("Opening float") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { onOpen(openingFloat.toDoubleOrNull() ?: 0.0) }, modifier = Modifier.fillMaxWidth()) { Text("Open shift") }
        } else {
            Text("End of day · Open since ${openShift.openedAt} · Float KES ${String.format("%,.0f", openShift.openingFloat)}", fontSize = 12.sp, color = Color(0xFF475569))
            Text("Settle all open tabs and reconcile successful payments before closing.", fontSize = 11.sp, color = Color(0xFF64748B))
            OutlinedTextField(actualCash, { actualCash = it }, label = { Text("Actual cash") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(actualMpesa, { actualMpesa = it }, label = { Text("M-Pesa received") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(actualCard, { actualCard = it }, label = { Text("Card receipts") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(tips, { tips = it }, label = { Text("Tips") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(expenses, { expenses = it }, label = { Text("Expenses") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Button(onClick = { onClose(openShift.id, actualCash.toDoubleOrNull() ?: 0.0, actualMpesa.toDoubleOrNull() ?: 0.0, actualCard.toDoubleOrNull() ?: 0.0, tips.toDoubleOrNull() ?: 0.0, expenses.toDoubleOrNull() ?: 0.0) }, modifier = Modifier.fillMaxWidth()) { Text("Close day") }
        }
        shifts.filter { it.status == "CLOSED" }.take(5).forEach { shift -> Text("${shift.openedAt} · Cash variance ${shift.variance?.let { String.format("%,.0f", it) } ?: "—"} · M-Pesa variance ${shift.mpesaVariance?.let { String.format("%,.0f", it) } ?: "—"} · Card variance ${shift.cardVariance?.let { String.format("%,.0f", it) } ?: "—"}", fontSize = 11.sp, color = Color(0xFF64748B)) }
    }
}

@Composable
private fun AndroidPurchasingCard(data: AndroidOperationsData, onReceive: (String) -> Unit, onCreateSupplier: (String, String, String) -> Unit) {
    var supplierName by remember { mutableStateOf("") }
    var supplierPhone by remember { mutableStateOf("") }
    var supplierEmail by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Suppliers & purchase orders", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F1F3A))
        if (data.suppliers.isEmpty()) Text("No suppliers configured.", fontSize = 12.sp, color = Color.Gray) else data.suppliers.forEach { supplier ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp).fillMaxWidth(), Arrangement.SpaceBetween) {
                    Column { Text(supplier.name, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(supplier.phone.ifBlank { supplier.email.orEmpty() }, fontSize = 11.sp, color = Color.Gray) }
                    Text(if (supplier.isActive) "ACTIVE" else "INACTIVE", fontSize = 10.sp, color = if (supplier.isActive) Color(0xFF059669) else Color.Gray)
                }
            }
        }
        Text("Add supplier", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(supplierName, { supplierName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(supplierPhone, { supplierPhone = it }, label = { Text("Phone") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(supplierEmail, { supplierEmail = it }, label = { Text("Email") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Button(onClick = { onCreateSupplier(supplierName.trim(), supplierPhone.trim(), supplierEmail.trim()); supplierName = ""; supplierPhone = ""; supplierEmail = "" }, enabled = supplierName.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save supplier") }
        Text("Purchase orders", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        if (data.purchaseOrders.isEmpty()) AndroidEmptyCard("No purchase orders.") else data.purchaseOrders.forEach { order ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp).fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column { Text(order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("KES ${String.format("%,.0f", order.totalCost)} · ${order.status}", fontSize = 11.sp, color = Color.Gray) }
                    if (order.status !in setOf("RECEIVED", "CANCELLED")) TextButton(onClick = { onReceive(order.id) }) { Text("Receive", fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun AndroidApprovalCard(approval: AndroidApproval, onDecision: (String, Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${approval.actionType} · ${approval.entityType}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(approval.reason.ifBlank { "Approval requested by ${approval.requestedBy}" }, fontSize = 11.sp, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { onDecision(approval.id, true) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 5.dp)) { Text("Approve", fontSize = 11.sp) }
                OutlinedButton(onClick = { onDecision(approval.id, false) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 5.dp)) { Text("Reject", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun AndroidReportsCard(report: AndroidReport?, onRun: (String, String) -> Unit) {
    var startDate by remember { mutableStateOf(java.time.LocalDate.now().minusDays(6).toString()) }
    var endDate by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Operations report", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F1F3A))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(startDate, { startDate = it }, label = { Text("Start") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(endDate, { endDate = it }, label = { Text("End") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Button(onClick = { onRun(startDate, endDate) }, modifier = Modifier.fillMaxWidth()) { Text("Run report") }
        report?.let {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MobileSummary("Food cost", it.foodCost.toInt(), Modifier.weight(1f))
                MobileSummary("Beverage", it.beverageCost.toInt(), Modifier.weight(1f))
                MobileSummary("Margin", it.grossMargin.toInt(), Modifier.weight(1f))
            }
            if (it.byLocation.isNotEmpty()) Text("By location: ${it.byLocation.joinToString { row -> "${row.label} KES ${String.format("%,.0f", row.amount)}" }}", fontSize = 11.sp, color = Color(0xFF475569))
            if (it.byChannel.isNotEmpty()) Text("By channel: ${it.byChannel.joinToString { row -> "${row.label} KES ${String.format("%,.0f", row.amount)}" }}", fontSize = 11.sp, color = Color(0xFF475569))
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

@Composable
private fun AndroidPortalOrdersDialog(
    queue: AndroidPortalQueue,
    showingMine: Boolean,
    busyId: String?,
    message: String?,
    onShowingMine: (Boolean) -> Unit,
    onClaim: (AndroidPortalOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val orders = if (showingMine) queue.mine else queue.waiting
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE9FBF1)) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.padding(10.dp).size(22.dp))
                        }
                        Column {
                            Text("Portal orders", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color(0xFF0F1F3A))
                            Text("Any staff member can claim a waiting order.", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = !showingMine, onClick = { onShowingMine(false) }, label = { Text("Waiting (${queue.waiting.size})", fontSize = 11.sp) })
                    FilterChip(selected = showingMine, onClick = { onShowingMine(true) }, label = { Text("My orders (${queue.mine.size})", fontSize = 11.sp) })
                }
                Surface(color = Color(0xFFE9FBF1), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(18.dp))
                        Text(message ?: "New portal orders are waiting to be claimed.", color = Color(0xFF047857), fontSize = 12.sp)
                    }
                }
                if (orders.isEmpty()) {
                    AndroidEmptyCard(if (showingMine) "You have not claimed any portal orders." else "No unclaimed portal orders.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(orders, key = { it.id }) { order ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFDCE4E2))) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                            Text(order.customerName, fontSize = 11.sp, color = Color(0xFF64748B))
                                        }
                                        Surface(color = Color(0xFFFFF1DB), shape = RoundedCornerShape(6.dp)) {
                                            Text(order.location.ifBlank { "ONLINE" }.uppercase(), color = Color(0xFFB45309), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        AndroidPortalMeta(Icons.Default.Inventory2, "KES ${String.format("%,.2f", order.amount)}")
                                        AndroidPortalMeta(Icons.Default.CreditCard, "Payment: ${order.paymentStatus.ifBlank { "Pending" }}")
                                        androidPortalOrderAge(order.createdAt).takeIf { it.isNotBlank() }?.let { AndroidPortalMeta(Icons.Default.AccessTime, it) }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        order.items.forEach { Text("${it.quantity} × ${it.name}", fontSize = 12.sp, color = Color(0xFF334155)) }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                        if (!showingMine) Button(onClick = { onClaim(order) }, enabled = busyId == null, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)) {
                                            Text(if (busyId == order.id) "Claiming…" else "Claim order", fontSize = 11.sp)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        } else Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(17.dp))
                                            Text("Claimed by you", color = Color(0xFF047857), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
