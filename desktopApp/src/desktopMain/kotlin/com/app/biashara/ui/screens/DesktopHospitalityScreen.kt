package com.app.biashara.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable

@Serializable
private data class DesktopTabItem(
    val id: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val lineTotal: Double = 0.0,
    val notes: String = ""
)

@Serializable
private data class DesktopTabOrder(
    val id: String,
    val orderNumber: String,
    val customerName: String? = "Walk-in Guest",
    val customerPhone: String? = "",
    val deliveryLocation: String? = "Dine In",
    val serviceType: String? = "DINE_IN",
    val hospitalityTableId: String? = null,
    val tabStatus: String = "OPEN",
    val subtotal: Double = 0.0,
    val items: List<DesktopTabItem> = emptyList(),
    val createdAt: String = ""
)

@Serializable
private data class DesktopKitchenTicketItem(
    val productName: String = "",
    val quantity: Int = 1,
    val notes: String = ""
)

@Serializable
private data class DesktopKitchenTicket(
    val id: String,
    val orderId: String = "",
    val orderNumber: String,
    val tableName: String? = null,
    val station: String,
    val status: String,
    val notes: String = "",
    val items: List<DesktopKitchenTicketItem> = emptyList(),
    val createdAt: String = ""
)

@Serializable
private data class DesktopHospitalityTable(
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
private data class DesktopFullDashboard(
    val enabled: Boolean = true,
    val tables: List<DesktopHospitalityTable> = emptyList(),
    val openTabs: List<DesktopTabOrder> = emptyList(),
    val tickets: List<DesktopKitchenTicket> = emptyList()
)

@Serializable
private data class DesktopReservation(
    val id: String,
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
private data class DesktopIngredient(
    val id: String = "",
    val name: String = "",
    val unit: String = "",
    val quantity: Double = 0.0,
    val reorderLevel: Double = 0.0,
    val unitCost: Double = 0.0,
    val isLowStock: Boolean = false
)

@Serializable
private data class DesktopMenuOption(val name: String = "", val priceDelta: Double = 0.0)
@Serializable private data class DesktopMenuProfile(val productId: String = "", val preparationStation: String? = null, val mealPeriods: List<String> = emptyList(), val sizes: List<DesktopMenuOption> = emptyList(), val extras: List<DesktopMenuOption> = emptyList(), val variants: List<DesktopMenuOption> = emptyList(), val comboProductIds: List<String> = emptyList(), val soldOut: Boolean = false, val happyHourPrice: Double? = null, val happyHourStart: String? = null, val happyHourEnd: String? = null, val ageRestricted: Boolean = false, val minimumAge: Int? = null)
@Serializable private data class DesktopShift(val id: String = "", val openedBy: String = "", val openedAt: String = "", val closedAt: String? = null, val openingFloat: Double = 0.0, val expectedCash: Double? = null, val actualCash: Double? = null, val mpesaTotal: Double? = null, val cardTotal: Double? = null, val tipsTotal: Double = 0.0, val expensesTotal: Double = 0.0, val status: String = "OPEN", val variance: Double? = null, val actualMpesa: Double? = null, val actualCard: Double? = null, val mpesaVariance: Double? = null, val cardVariance: Double? = null, val totalVariance: Double? = null)
@Serializable private data class DesktopSupplier(val id: String = "", val name: String = "", val phone: String = "", val email: String? = null, val address: String? = null, val isActive: Boolean = true)
@Serializable private data class DesktopPurchaseOrder(val id: String = "", val orderNumber: String = "", val supplierId: String = "", val status: String = "ORDERED", val totalCost: Double = 0.0, val orderedAt: String = "", val receivedAt: String? = null)
@Serializable private data class DesktopApproval(val id: String = "", val actionType: String = "", val entityType: String = "", val entityId: String = "", val requestedBy: String = "", val approvedBy: String? = null, val status: String = "PENDING", val reason: String = "", val requestedAt: String = "")
@Serializable private data class DesktopProduct(val id: String = "", val name: String = "", val category: String = "", val sellingPrice: Double = 0.0)
@Serializable private data class DesktopReportBreakdown(val label: String = "", val count: Int = 0, val amount: Double = 0.0)
@Serializable private data class DesktopReport(val byWaiter: List<DesktopReportBreakdown> = emptyList(), val byTable: List<DesktopReportBreakdown> = emptyList(), val byItem: List<DesktopReportBreakdown> = emptyList(), val byCategory: List<DesktopReportBreakdown> = emptyList(), val byHour: List<DesktopReportBreakdown> = emptyList(), val byPaymentMethod: List<DesktopReportBreakdown> = emptyList(), val byChannel: List<DesktopReportBreakdown> = emptyList(), val byShift: List<DesktopReportBreakdown> = emptyList(), val byLocation: List<DesktopReportBreakdown> = emptyList(), val foodCost: Double = 0.0, val beverageCost: Double = 0.0, val wastageCost: Double = 0.0, val grossMargin: Double = 0.0, val averageTableTurnoverMinutes: Double = 0.0)
@Serializable private data class DesktopOperationsData(val reservations: List<DesktopReservation> = emptyList(), val menuProfiles: List<DesktopMenuProfile> = emptyList(), val ingredients: List<DesktopIngredient> = emptyList(), val shifts: List<DesktopShift> = emptyList(), val suppliers: List<DesktopSupplier> = emptyList(), val purchaseOrders: List<DesktopPurchaseOrder> = emptyList(), val approvals: List<DesktopApproval> = emptyList())

@Serializable private data class UpdateTicketReq(val status: String)
@Serializable private data class CreateTableReq(val name: String, val area: String = "Main Floor", val capacity: Int = 4)
@Serializable private data class UpdateTableReq(val name: String, val area: String = "Main Floor", val capacity: Int = 4)
@Serializable private data class TransferTabReq(val tableId: String)
@Serializable private data class DesktopShiftOpenReq(val openingFloat: Double, val notes: String = "")
@Serializable private data class DesktopShiftCloseReq(val actualCash: Double, val actualMpesa: Double, val actualCard: Double, val tipsTotal: Double = 0.0, val expensesTotal: Double = 0.0, val notes: String = "")
@Serializable private data class DesktopApprovalDecisionReq(val approved: Boolean)
@Serializable private data class DesktopMenuProfileReq(val preparationStation: String? = null, val mealPeriods: List<String> = emptyList(), val sizes: List<DesktopMenuOption> = emptyList(), val extras: List<DesktopMenuOption> = emptyList(), val variants: List<DesktopMenuOption> = emptyList(), val comboProductIds: List<String> = emptyList(), val soldOut: Boolean = false, val happyHourPrice: Double? = null, val happyHourStart: String? = null, val happyHourEnd: String? = null, val ageRestricted: Boolean = false, val minimumAge: Int? = null)
@Serializable private data class DesktopSupplierReq(val name: String, val phone: String = "", val email: String? = null, val address: String? = null)
@Serializable private data class DesktopPurchaseOrderLineReq(val ingredientId: String, val quantity: Double, val unitCost: Double)
@Serializable private data class DesktopPurchaseOrderReq(val supplierId: String, val notes: String = "", val items: List<DesktopPurchaseOrderLineReq>)

@Composable
fun DesktopHospitalityScreen(client: HttpClient = remember { inject() }) {
    DesktopHospitalityMainLayout(client, initialTab = 0)
}

@Composable
fun DesktopOpenTabsScreen(client: HttpClient = remember { inject() }) {
    DesktopHospitalityMainLayout(client, initialTab = 1)
}

@Composable
private fun DesktopHospitalityMainLayout(client: HttpClient, initialTab: Int) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(initialTab) }
    var dashboard by remember { mutableStateOf<DesktopFullDashboard?>(null) }
    var operations by remember { mutableStateOf<DesktopOperationsData?>(null) }
    var products by remember { mutableStateOf<List<DesktopProduct>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    // Dialog States
    var showAddTableDialog by remember { mutableStateOf(false) }
    var tableToEdit by remember { mutableStateOf<DesktopHospitalityTable?>(null) }
    var tabToTransfer by remember { mutableStateOf<DesktopTabOrder?>(null) }
    var tabToSettle by remember { mutableStateOf<DesktopTabOrder?>(null) }
    var settlingTab by remember { mutableStateOf(false) }
    var settlementError by remember { mutableStateOf<String?>(null) }
    var tabForProForma by remember { mutableStateOf<DesktopTabOrder?>(null) }

    fun loadData() {
        scope.launch {
            loading = true
            error = null
            runCatching {
                val dashRes = client.get("$BASE_URL/hospitality").body<ApiResponse<DesktopFullDashboard>>()
                val opsRes = client.get("$BASE_URL/hospitality/operations").body<ApiResponse<DesktopOperationsData>>()
                val productsRes = runCatching { client.get("$BASE_URL/products").body<ApiResponse<List<DesktopProduct>>>() }.getOrNull()
                Triple(dashRes, opsRes, productsRes)
            }.onSuccess { (dashRes, opsRes, productsRes) ->
                if (dashRes.success && dashRes.data != null) dashboard = dashRes.data
                else error = dashRes.message.ifBlank { "Could not load hospitality status." }
                if (opsRes.success && opsRes.data != null) operations = opsRes.data
                val productData = productsRes?.data
                if (productsRes?.success == true && productData != null) products = productData
            }.onFailure {
                error = it.message ?: "Failed to connect to backend server."
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            loadData()
            delay(5000)
        }
    }

    fun updateTicketStatus(ticketId: String, newStatus: String) {
        scope.launch {
            runCatching {
                client.patch("$BASE_URL/hospitality/tickets/$ticketId") {
                    contentType(ContentType.Application.Json)
                    setBody(UpdateTicketReq(newStatus))
                }.body<ApiResponse<DesktopKitchenTicket>>()
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
                    }.body<ApiResponse<DesktopHospitalityTable>>()
                } else {
                    client.put("$BASE_URL/hospitality/tables/$id") {
                        contentType(ContentType.Application.Json)
                        setBody(UpdateTableReq(name, area, capacity))
                    }.body<ApiResponse<DesktopHospitalityTable>>()
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
                }.body<ApiResponse<DesktopTabOrder>>()
            }.onSuccess {
                if (it.success) {
                    successMsg = "Tab transferred successfully!"
                    tabToTransfer = null
                    loadData()
                } else error = it.message
            }.onFailure { error = it.message }
        }
    }

    fun handleSettleTab(orderId: String, method: String, phone: String) {
        if (settlingTab) return
        settlingTab = true
        settlementError = null
        scope.launch {
            try {
                val result = settleDesktopTab(client, orderId, method, phone)
                if (method == "CARD") {
                    java.awt.Desktop.getDesktop().browse(java.net.URI("https://biashara360.co.ke/card-payments?orderId=$orderId"))
                }
                successMsg = result.message
                tabToSettle = null
                loadData()
            } catch (e: Exception) {
                settlementError = e.message ?: "Could not start settlement."
            } finally {
                settlingTab = false
            }
        }
    }

    fun operationAction(successText: String, block: suspend () -> ApiResponse<*>) {
        scope.launch {
            runCatching { block() }.onSuccess {
                if (it.success) { successMsg = successText; loadData() } else error = it.message
            }.onFailure { error = it.message ?: "Hospitality operation failed." }
        }
    }

    fun updateReservationStatus(id: String, status: String) = operationAction("Reservation updated.") {
        client.patch("$BASE_URL/hospitality/operations/reservations/$id/$status").body()
    }

    fun receivePurchaseOrder(id: String) = operationAction("Purchase order received.") {
        client.post("$BASE_URL/hospitality/operations/purchase-orders/$id/receive").body()
    }

    fun decideApproval(id: String, approved: Boolean) = operationAction(if (approved) "Approval granted." else "Approval rejected.") {
        client.post("$BASE_URL/hospitality/operations/approvals/$id/decision") {
            contentType(ContentType.Application.Json)
            setBody(DesktopApprovalDecisionReq(approved))
        }.body()
    }

    fun openShift(openingFloat: Double) = operationAction("Shift opened.") {
        client.post("$BASE_URL/hospitality/operations/shifts/open") {
            contentType(ContentType.Application.Json)
            setBody(DesktopShiftOpenReq(openingFloat))
        }.body()
    }

    fun closeShift(id: String, actualCash: Double, actualMpesa: Double, actualCard: Double, tips: Double, expenses: Double) = operationAction("Day closed and payments reconciled.") {
        client.post("$BASE_URL/hospitality/operations/shifts/$id/close") {
            contentType(ContentType.Application.Json)
            setBody(DesktopShiftCloseReq(actualCash, actualMpesa, actualCard, tips, expenses))
        }.body()
    }

    fun saveMenuProfile(productId: String, profile: DesktopMenuProfile, station: String, soldOut: Boolean) = operationAction("Menu controls saved.") {
        client.put("$BASE_URL/hospitality/operations/menu/$productId") {
            contentType(ContentType.Application.Json)
            setBody(DesktopMenuProfileReq(station.takeUnless { it == "NONE" }, profile.mealPeriods, profile.sizes, profile.extras, profile.variants, profile.comboProductIds, soldOut, profile.happyHourPrice, profile.happyHourStart, profile.happyHourEnd, profile.ageRestricted, profile.minimumAge))
        }.body()
    }

    fun createSupplier(name: String, phone: String) = operationAction("Supplier added.") {
        client.post("$BASE_URL/hospitality/operations/suppliers") {
            contentType(ContentType.Application.Json)
            setBody(DesktopSupplierReq(name, phone))
        }.body()
    }

    fun createPurchaseOrder(supplierId: String, ingredientId: String, quantity: Double, unitCost: Double) = operationAction("Purchase order created.") {
        client.post("$BASE_URL/hospitality/operations/purchase-orders") {
            contentType(ContentType.Application.Json)
            setBody(DesktopPurchaseOrderReq(supplierId, items = listOf(DesktopPurchaseOrderLineReq(ingredientId, quantity, unitCost))))
        }.body()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hospitality & Restaurant Hub",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F1F3A)
                )
                Text(
                    text = "Live floor plan, kitchen display system (KDS), open tabs and settlement controls",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showAddTableDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1F3A))
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Table")
                }

                Button(
                    onClick = ::loadData,
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B874))
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else Icon(Icons.Default.Refresh, "Refresh", Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (loading) "Updating..." else "Refresh")
                }
            }
        }

        // Notifications
        error?.let { msg ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = Color(0xFFB91C1C))
                    Spacer(Modifier.width(10.dp))
                    Text(msg, color = Color(0xFFB91C1C), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { error = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Dismiss", tint = Color(0xFFB91C1C))
                    }
                }
            }
        }

        successMsg?.let { msg ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF86EFAC))
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF15803D))
                    Spacer(Modifier.width(10.dp))
                    Text(msg, color = Color(0xFF15803D), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { successMsg = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Dismiss", tint = Color(0xFF15803D))
                    }
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF00B874),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            val tabs = listOf(
                "Floor Plan & Tables (${dashboard?.tables?.size ?: 0})" to Icons.Default.TableRestaurant,
                "Open Tabs (${dashboard?.openTabs?.size ?: 0})" to Icons.Default.ReceiptLong,
                "Kitchen KDS (${dashboard?.tickets?.count { it.station == "KITCHEN" && it.status in setOf("NEW", "PREPARING", "READY", "DELAYED") } ?: 0})" to Icons.Default.Restaurant,
                "Operations & Stock" to Icons.Default.Inventory
            )
            tabs.forEachIndexed { index, (label, icon) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }
        }

        if (operations != null && operations?.shifts?.none { it.status == "OPEN" } == true) {
            Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFCD34D))) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = Color(0xFFB45309), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start of day required: open a shift before taking hospitality orders or settling payments.", color = Color(0xFF92400E), fontSize = 12.sp)
                }
            }
        }

        dashboard?.let { dash ->
            when (selectedTab) {
                0 -> DesktopFloorPlanTab(
                    tables = dash.tables,
                    onEditTable = { tableToEdit = it },
                    onTransferTab = { table ->
                        val activeTab = dash.openTabs.firstOrNull { it.hospitalityTableId == table.id }
                        if (activeTab != null) tabToTransfer = activeTab
                    }
                )
                1 -> DesktopOpenTabsTab(
                    openTabs = dash.openTabs,
                    onSettle = { settlementError = null; tabToSettle = it },
                    onTransfer = { tabToTransfer = it },
                    onProForma = { tabForProForma = it }
                )
                2 -> DesktopKDSTab(
                    tickets = dash.tickets,
                    onUpdateStatus = ::updateTicketStatus
                )
                3 -> DesktopOperationsTab(
                    operations = operations,
                    tablesCount = dash.tables.size,
                    openTabsCount = dash.openTabs.size,
                    products = products,
                    onReservationStatus = ::updateReservationStatus,
                    onReceivePurchaseOrder = ::receivePurchaseOrder,
                    onDecideApproval = ::decideApproval,
                    onOpenShift = ::openShift,
                    onCloseShift = ::closeShift,
                    onSaveMenuProfile = ::saveMenuProfile,
                    onCreateSupplier = ::createSupplier,
                    onCreatePurchaseOrder = ::createPurchaseOrder,
                    client = client
                )
            }
        } ?: run {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00B874))
                }
            }
        }
    }

    if (showAddTableDialog || tableToEdit != null) {
        DesktopTableFormModal(
            table = tableToEdit,
            onDismiss = {
                showAddTableDialog = false
                tableToEdit = null
            },
            onSave = { name, area, capacity ->
                handleSaveTable(name, area, capacity, tableToEdit?.id)
            }
        )
    }

    tabToTransfer?.let { tab ->
        DesktopTransferTabModal(
            tab = tab,
            tables = dashboard?.tables.orEmpty(),
            onDismiss = { tabToTransfer = null },
            onConfirm = { targetTableId -> handleTransferTab(tab.id, targetTableId) }
        )
    }

    tabToSettle?.let { tab ->
        DesktopSettleModal(
            tab = tab,
            onDismiss = { if (!settlingTab) tabToSettle = null },
            isSubmitting = settlingTab,
            errorMessage = settlementError,
            onConfirmSettle = { method, phone -> handleSettleTab(tab.id, method, phone) }
        )
    }

    tabForProForma?.let { tab ->
        DesktopProFormaModal(
            tab = tab,
            onDismiss = { tabForProForma = null }
        )
    }
}

@Composable
private fun DesktopFloorPlanTab(
    tables: List<DesktopHospitalityTable>,
    onEditTable: (DesktopHospitalityTable) -> Unit,
    onTransferTab: (DesktopHospitalityTable) -> Unit
) {
    var selectedArea by remember { mutableStateOf("ALL") }
    val areas = remember(tables) { listOf("ALL") + tables.map { it.area }.distinct() }
    val filteredTables = if (selectedArea == "ALL") tables else tables.filter { it.area == selectedArea }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filter Area:", fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), fontSize = 14.sp)
            areas.forEach { area ->
                FilterChip(
                    selected = selectedArea == area,
                    onClick = { selectedArea = area },
                    label = { Text(if (area == "ALL") "All Areas" else area) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0F1F3A),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredTables.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(Modifier.padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No tables configured for this area. Click 'Add Table' above to create one.", color = Color(0xFF64748B))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTables, key = { it.id }) { table ->
                    val isOccupied = table.status == "OCCUPIED" || table.openOrderCount > 0
                    val statusColor = when (table.status) {
                        "OCCUPIED" -> Color(0xFFE11D48)
                        "RESERVED" -> Color(0xFFD97706)
                        "CLEANING" -> Color(0xFF2563EB)
                        else -> Color(0xFF059669)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (isOccupied) Color(0xFFFECDD3) else Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text(table.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F1F3A))
                                    Text("${table.area} · ${table.capacity} Seats", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = table.status,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Divider(color = Color(0xFFF1F5F9))

                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("Active Orders", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = "${table.openOrderCount} Tabs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F1F3A)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Unsettled Total", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(
                                        text = "KES ${String.format("%,.0f", table.openAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF00B874)
                                    )
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onEditTable(table) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit", fontSize = 12.sp)
                                }

                                if (isOccupied) {
                                    Button(
                                        onClick = { onTransferTab(table) },
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, null, Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Transfer", fontSize = 12.sp)
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
private fun DesktopOpenTabsTab(
    openTabs: List<DesktopTabOrder>,
    onSettle: (DesktopTabOrder) -> Unit,
    onTransfer: (DesktopTabOrder) -> Unit,
    onProForma: (DesktopTabOrder) -> Unit
) {
    if (openTabs.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Box(Modifier.padding(50.dp), contentAlignment = Alignment.Center) {
                Text("No open customer tabs currently awaiting settlement.", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(openTabs, key = { it.id }) { tab ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF0F1F3A),
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(tab.orderNumber.takeLast(3), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Tab #${tab.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F1F3A))
                                    Text("${tab.deliveryLocation} · Guest: ${tab.customerName}", fontSize = 13.sp, color = Color(0xFF64748B))
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Amount", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("KES ${String.format("%,.0f", tab.subtotal)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF00B874))
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Items:", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                            val summary = tab.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
                            Text(
                                text = if (summary.length > 80) summary.take(80) + "..." else summary,
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                maxLines = 1
                            )
                        }

                        Divider(color = Color(0xFFF1F5F9))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = { onProForma(tab) }) {
                                Icon(Icons.Default.Print, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pro-Forma Bill")
                            }
                            Spacer(Modifier.width(10.dp))
                            OutlinedButton(onClick = { onTransfer(tab) }) {
                                Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Transfer")
                            }
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = { onSettle(tab) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B874))
                            ) {
                                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Settle Tab")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopKDSTab(
    tickets: List<DesktopKitchenTicket>,
    onUpdateStatus: (ticketId: String, status: String) -> Unit
) {
    val filteredTickets = tickets.filter { it.station == "KITCHEN" }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (filteredTickets.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(Modifier.padding(50.dp), contentAlignment = Alignment.Center) {
                    Text("No active kitchen order tickets.", color = Color(0xFF64748B))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTickets, key = { it.id }) { ticket ->
                    val statusBg = when (ticket.status) {
                        "NEW" -> Color(0xFFEFF6FF)
                        "PREPARING" -> Color(0xFFFEF3C7)
                        "READY" -> Color(0xFFDCFCE7)
                        else -> Color(0xFFF1F5F9)
                    }
                    val statusBorder = when (ticket.status) {
                        "NEW" -> Color(0xFF93C5FD)
                        "PREPARING" -> Color(0xFDFCD34D)
                        "READY" -> Color(0xFF86EFAC)
                        else -> Color(0xFFCBD5E1)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = statusBg),
                        border = BorderStroke(1.5.dp, statusBorder),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Column {
                                    Text("Order #${ticket.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F1F3A))
                                    Text("Table: ${ticket.tableName ?: "Takeaway"}", fontSize = 13.sp, color = Color(0xFF475569))
                                }
                                Surface(
                                    color = if (ticket.station == "KITCHEN") Color(0xFF0F1F3A) else Color(0xFF2563EB),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = ticket.station,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Divider(color = statusBorder.copy(alpha = 0.6f))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ticket.items.forEach { item ->
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text("${item.quantity}x ${item.productName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1E293B))
                                        if (item.notes.isNotBlank()) {
                                            Text("(${item.notes})", fontSize = 11.sp, color = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }

                            if (ticket.notes.isNotBlank()) {
                                Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(6.dp)) {
                                    Text("Note: ${ticket.notes}", color = Color(0xFFB91C1C), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                when (ticket.status) {
                                    "NEW" -> Button(
                                        onClick = { onUpdateStatus(ticket.id, "PREPARING") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                    ) {
                                        Text("Start Preparing")
                                    }
                                    "PREPARING" -> Button(
                                        onClick = { onUpdateStatus(ticket.id, "READY") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Text("Mark Ready")
                                    }
                                    "READY" -> Button(
                                        onClick = { onUpdateStatus(ticket.id, "SERVED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                                    ) {
                                        Text("Mark Served")
                                    }
                                    else -> Text("Status: ${ticket.status}", fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
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
private fun DesktopOperationsTab(
    operations: DesktopOperationsData?,
    tablesCount: Int,
    openTabsCount: Int,
    products: List<DesktopProduct>,
    onReservationStatus: (String, String) -> Unit,
    onReceivePurchaseOrder: (String) -> Unit,
    onDecideApproval: (String, Boolean) -> Unit,
    onOpenShift: (Double) -> Unit,
    onCloseShift: (String, Double, Double, Double, Double, Double) -> Unit,
    onSaveMenuProfile: (String, DesktopMenuProfile, String, Boolean) -> Unit,
    onCreateSupplier: (String, String) -> Unit,
    onCreatePurchaseOrder: (String, String, Double, Double) -> Unit,
    client: HttpClient
) {
    val data = operations ?: DesktopOperationsData()
    var section by remember { mutableStateOf("RESERVATIONS") }
    val sections = listOf("RESERVATIONS", "MENU", "SHIFTS", "PURCHASING", "APPROVALS", "REPORTS")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OperationsSummaryCard(Modifier.weight(1f), "Total Tables", tablesCount.toString(), Icons.Default.TableRestaurant)
            OperationsSummaryCard(Modifier.weight(1f), "Open Tabs", openTabsCount.toString(), Icons.Default.ReceiptLong)
            OperationsSummaryCard(Modifier.weight(1f), "Bookings", data.reservations.count { it.status == "BOOKED" }.toString(), Icons.Default.Event)
            OperationsSummaryCard(Modifier.weight(1f), "Low Stock Items", data.ingredients.count { it.isLowStock }.toString(), Icons.Default.Warning)
        }
        ScrollableTabRow(selectedTabIndex = sections.indexOf(section), containerColor = Color.White, contentColor = Color(0xFF00B874)) {
            sections.forEach { value -> Tab(selected = section == value, onClick = { section = value }, text = { Text(value.replace('_', ' '), fontWeight = if (section == value) FontWeight.Bold else FontWeight.Normal) }) }
        }
        when (section) {
            "RESERVATIONS" -> DesktopReservationsSection(data.reservations, onReservationStatus)
            "MENU" -> DesktopMenuSection(data.menuProfiles, products, onSaveMenuProfile)
            "SHIFTS" -> DesktopShiftsSection(data.shifts, onOpenShift, onCloseShift)
            "PURCHASING" -> DesktopPurchasingSection(data.suppliers, data.purchaseOrders, data.ingredients, onCreateSupplier, onCreatePurchaseOrder, onReceivePurchaseOrder)
            "APPROVALS" -> DesktopApprovalsSection(data.approvals, onDecideApproval)
            "REPORTS" -> DesktopReportsSection(client)
        }
    }
}

@Composable
private fun DesktopReservationsSection(reservations: List<DesktopReservation>, onStatus: (String, String) -> Unit) {
    if (reservations.isEmpty()) Text("No reservations recorded.", color = Color(0xFF64748B))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(reservations, key = { it.id }) { reservation ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(reservation.customerName, fontWeight = FontWeight.Bold)
                        Text("${reservation.guestCount} guests · ${reservation.reservedAt} · ${reservation.customerPhone}", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Text(reservation.status, fontWeight = FontWeight.Bold, color = Color(0xFF00B874))
                    if (reservation.status == "BOOKED") {
                        Spacer(Modifier.width(8.dp)); Button(onClick = { onStatus(reservation.id, "SEATED") }) { Text("Seat") }
                        Spacer(Modifier.width(6.dp)); OutlinedButton(onClick = { onStatus(reservation.id, "CANCELLED") }) { Text("Cancel") }
                    } else if (reservation.status == "SEATED") {
                        Spacer(Modifier.width(8.dp)); Button(onClick = { onStatus(reservation.id, "COMPLETED") }) { Text("Complete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopMenuSection(profiles: List<DesktopMenuProfile>, products: List<DesktopProduct>, onSave: (String, DesktopMenuProfile, String, Boolean) -> Unit) {
    var selectedId by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var station by remember { mutableStateOf("KITCHEN") }
    var soldOut by remember { mutableStateOf(false) }
    val profile = profiles.firstOrNull { it.productId == selectedId } ?: DesktopMenuProfile(productId = selectedId)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Menu availability and preparation routing", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = products.isNotEmpty()) { Text(products.firstOrNull { it.id == selectedId }?.name ?: "Select a menu item") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { products.forEach { product -> DropdownMenuItem(text = { Text(product.name) }, onClick = { selectedId = product.id; val current = profiles.firstOrNull { it.productId == product.id }; station = current?.preparationStation ?: "KITCHEN"; soldOut = current?.soldOut ?: false; expanded = false }) } }
        }
        if (selectedId.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Station:")
                listOf("KITCHEN", "BAR", "NONE").forEach { value -> FilterChip(selected = station == value, onClick = { station = value }, label = { Text(value) }) }
                FilterChip(selected = soldOut, onClick = { soldOut = !soldOut }, label = { Text("Sold out") })
                Button(onClick = { onSave(selectedId, profile, station, soldOut) }) { Text("Save") }
            }
        } else Text("Select a product to manage its hospitality menu profile.", color = Color(0xFF64748B))
        profiles.forEach { item -> Text("${products.firstOrNull { it.id == item.productId }?.name ?: item.productId}: ${item.preparationStation ?: "NONE"}${if (item.soldOut) " · SOLD OUT" else ""}", fontSize = 13.sp) }
    }
}

@Composable
private fun DesktopShiftsSection(shifts: List<DesktopShift>, onOpen: (Double) -> Unit, onClose: (String, Double, Double, Double, Double, Double) -> Unit) {
    val open = shifts.firstOrNull { it.status == "OPEN" }
    var opening by remember { mutableStateOf("0") }
    var actual by remember { mutableStateOf("0") }
    var actualMpesa by remember { mutableStateOf("0") }
    var actualCard by remember { mutableStateOf("0") }
    var tips by remember { mutableStateOf("0") }
    var expenses by remember { mutableStateOf("0") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (open == null) { Text("Start of day · no shift is open.", fontWeight = FontWeight.Bold); Text("Open a shift before taking hospitality orders or settling payments.", fontSize = 12.sp, color = Color(0xFF64748B)); Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(opening, { opening = it }, label = { Text("Opening float") }, modifier = Modifier.width(180.dp)); Spacer(Modifier.width(8.dp)); Button(onClick = { onOpen(opening.toDoubleOrNull() ?: 0.0) }) { Text("Open shift") } } }
        else { Text("End of day · shift opened ${open.openedAt}", fontWeight = FontWeight.Bold); Text("Settle all open tabs and reconcile every successful payment before closing.", fontSize = 12.sp, color = Color(0xFF64748B)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(actual, { actual = it }, label = { Text("Cash counted") }, modifier = Modifier.width(145.dp)); OutlinedTextField(actualMpesa, { actualMpesa = it }, label = { Text("M-Pesa counted") }, modifier = Modifier.width(145.dp)); OutlinedTextField(actualCard, { actualCard = it }, label = { Text("Card counted") }, modifier = Modifier.width(135.dp)); OutlinedTextField(tips, { tips = it }, label = { Text("Tips") }, modifier = Modifier.width(110.dp)); OutlinedTextField(expenses, { expenses = it }, label = { Text("Expenses") }, modifier = Modifier.width(120.dp)); Button(onClick = { onClose(open.id, actual.toDoubleOrNull() ?: 0.0, actualMpesa.toDoubleOrNull() ?: 0.0, actualCard.toDoubleOrNull() ?: 0.0, tips.toDoubleOrNull() ?: 0.0, expenses.toDoubleOrNull() ?: 0.0) }) { Text("Close day") } } }
        shifts.forEach { shift -> Text("${shift.openedAt} · ${shift.status} · Expected cash ${shift.expectedCash?.let { String.format("%,.0f", it) } ?: "—"} · M-Pesa ${shift.mpesaTotal?.let { String.format("%,.0f", it) } ?: "—"}/${shift.actualMpesa?.let { String.format("%,.0f", it) } ?: "—"} · Card ${shift.cardTotal?.let { String.format("%,.0f", it) } ?: "—"}/${shift.actualCard?.let { String.format("%,.0f", it) } ?: "—"} · Total variance ${shift.totalVariance?.let { String.format("%,.0f", it) } ?: "—"}", fontSize = 12.sp, color = Color(0xFF64748B)) }
    }
}

@Composable
private fun DesktopPurchasingSection(suppliers: List<DesktopSupplier>, orders: List<DesktopPurchaseOrder>, ingredients: List<DesktopIngredient>, onSupplier: (String, String) -> Unit, onCreateOrder: (String, String, Double, Double) -> Unit, onReceive: (String) -> Unit) {
    var supplierName by remember { mutableStateOf("") }
    var supplierPhone by remember { mutableStateOf("") }
    var supplierExpanded by remember { mutableStateOf(false) }
    var ingredientExpanded by remember { mutableStateOf(false) }
    var supplierId by remember { mutableStateOf("") }
    var ingredientId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitCost by remember { mutableStateOf("0") }
    val chosenSupplier = suppliers.firstOrNull { it.id == supplierId } ?: suppliers.firstOrNull()
    val chosenIngredient = ingredients.firstOrNull { it.id == ingredientId } ?: ingredients.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Suppliers and purchase orders", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(supplierName, { supplierName = it }, label = { Text("Supplier") }, modifier = Modifier.width(180.dp)); OutlinedTextField(supplierPhone, { supplierPhone = it }, label = { Text("Phone") }, modifier = Modifier.width(150.dp)); Button(onClick = { if (supplierName.isNotBlank()) { onSupplier(supplierName.trim(), supplierPhone.trim()); supplierName = ""; supplierPhone = "" } }) { Text("Add supplier") } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box { OutlinedButton(onClick = { supplierExpanded = true }, enabled = suppliers.isNotEmpty()) { Text(chosenSupplier?.name ?: "Select supplier") }; DropdownMenu(supplierExpanded, { supplierExpanded = false }) { suppliers.forEach { supplier -> DropdownMenuItem(text = { Text(supplier.name) }, onClick = { supplierId = supplier.id; supplierExpanded = false }) } } }
            Box { OutlinedButton(onClick = { ingredientExpanded = true }, enabled = ingredients.isNotEmpty()) { Text(chosenIngredient?.name ?: "Select ingredient") }; DropdownMenu(ingredientExpanded, { ingredientExpanded = false }) { ingredients.forEach { ingredient -> DropdownMenuItem(text = { Text(ingredient.name) }, onClick = { ingredientId = ingredient.id; ingredientExpanded = false }) } } }
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Qty") }, modifier = Modifier.width(90.dp)); OutlinedTextField(unitCost, { unitCost = it }, label = { Text("Unit cost") }, modifier = Modifier.width(110.dp)); Button(onClick = { if (chosenSupplier != null && chosenIngredient != null) onCreateOrder(chosenSupplier.id, chosenIngredient.id, quantity.toDoubleOrNull() ?: 0.0, unitCost.toDoubleOrNull() ?: 0.0) }) { Text("Create PO") }
        }
        orders.forEach { order -> Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("${order.orderNumber} · ${suppliers.firstOrNull { it.id == order.supplierId }?.name ?: order.supplierId} · KES ${String.format("%,.0f", order.totalCost)} · ${order.status}", fontSize = 12.sp); if (order.status != "RECEIVED") Button(onClick = { onReceive(order.id) }) { Text("Receive") } } }
    }
}

@Composable
private fun DesktopApprovalsSection(approvals: List<DesktopApproval>, onDecision: (String, Boolean) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(approvals, key = { it.id }) { approval -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White)) { Row(Modifier.fillMaxWidth().padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${approval.actionType} · ${approval.entityType}", fontWeight = FontWeight.Bold); Text("${approval.reason} · ${approval.requestedAt}", fontSize = 12.sp, color = Color(0xFF64748B)) }; if (approval.status == "PENDING") { Button(onClick = { onDecision(approval.id, true) }) { Text("Approve") }; Spacer(Modifier.width(6.dp)); OutlinedButton(onClick = { onDecision(approval.id, false) }) { Text("Reject") } } else Text(approval.status, fontWeight = FontWeight.Bold) } } } }
}

@Composable
private fun DesktopReportsSection(client: HttpClient) {
    val scope = rememberCoroutineScope()
    var start by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var end by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var report by remember { mutableStateOf<DesktopReport?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(start, { start = it }, label = { Text("From (YYYY-MM-DD)") }, modifier = Modifier.width(190.dp)); OutlinedTextField(end, { end = it }, label = { Text("To (YYYY-MM-DD)") }, modifier = Modifier.width(190.dp)); Button(onClick = { scope.launch { runCatching { client.get("$BASE_URL/hospitality/operations/report?startDate=$start&endDate=$end").body<ApiResponse<DesktopReport>>() }.onSuccess { if (it.success) report = it.data else error = it.message }.onFailure { error = it.message } } }) { Text("Run report") } }
    error?.let { Text(it, color = Color(0xFFB91C1C)) }
    report?.let { value -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { OperationsSummaryCard(Modifier.weight(1f), "Food cost", "KES ${String.format("%,.0f", value.foodCost)}", Icons.Default.Restaurant); OperationsSummaryCard(Modifier.weight(1f), "Beverage cost", "KES ${String.format("%,.0f", value.beverageCost)}", Icons.Default.LocalBar); OperationsSummaryCard(Modifier.weight(1f), "Margin", "${String.format("%.1f", value.grossMargin)}%", Icons.Default.TrendingUp) } }
}

@Composable
private fun OperationsSummaryCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier, colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF00B874), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, color = Color(0xFF64748B), fontSize = 12.sp)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color(0xFF0F1F3A))
            }
        }
    }
}

@Composable
private fun DesktopTableFormModal(
    table: DesktopHospitalityTable?,
    onDismiss: () -> Unit,
    onSave: (name: String, area: String, capacity: Int) -> Unit
) {
    var name by remember { mutableStateOf(table?.name.orEmpty()) }
    var area by remember { mutableStateOf(table?.area ?: "Main Floor") }
    var capacityText by remember { mutableStateOf((table?.capacity ?: 4).toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            modifier = Modifier.width(420.dp)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = if (table == null) "Add New Table" else "Edit Table #${table.name}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F1F3A)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Table Name / Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Area (e.g. Main Floor, VIP, Terrace, Bar)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it.filter { c -> c.isDigit() } },
                    label = { Text("Seating Capacity (Guests)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val cap = capacityText.toIntOrNull() ?: 4
                            if (name.isNotBlank()) onSave(name.trim(), area.trim(), cap)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F1F3A))
                    ) {
                        Text("Save Table")
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopTransferTabModal(
    tab: DesktopTabOrder,
    tables: List<DesktopHospitalityTable>,
    onDismiss: () -> Unit,
    onConfirm: (targetTableId: String) -> Unit
) {
    var selectedTableId by remember { mutableStateOf<String?>(null) }
    val availableTables = tables.filter { it.id != tab.hospitalityTableId }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            modifier = Modifier.width(450.dp)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Transfer Tab #${tab.orderNumber}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                Text("Select destination table for this customer's active tab:", fontSize = 13.sp, color = Color(0xFF64748B))

                LazyColumn(Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableTables, key = { it.id }) { table ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTableId = table.id },
                            color = if (selectedTableId == table.id) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (selectedTableId == table.id) Color(0xFF00B874) else Color(0xFFE2E8F0))
                        ) {
                            Row(Modifier.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(table.name, fontWeight = FontWeight.Bold)
                                Text("${table.area} · ${table.capacity} seats", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { selectedTableId?.let { onConfirm(it) } },
                        enabled = selectedTableId != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B874))
                    ) {
                        Text("Confirm Transfer")
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopSettleModal(
    tab: DesktopTabOrder,
    onDismiss: () -> Unit,
    isSubmitting: Boolean,
    errorMessage: String?,
    onConfirmSettle: (paymentMethod: String, phone: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("CASH") }
    var phone by remember { mutableStateOf(tab.customerPhone.orEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            modifier = Modifier.width(420.dp)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Settle Tab #${tab.orderNumber}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                Text("Total Amount Due: KES ${String.format("%,.2f", tab.subtotal)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B874))

                Text("Select Settlement Payment Method:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                listOf("CASH" to "Cash Payment", "MPESA" to "M-Pesa Mobile Money", "CARD" to "Credit / Debit Card").forEach { (code, label) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitting) { selectedMethod = code },
                        color = if (selectedMethod == code) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (selectedMethod == code) Color(0xFF00B874) else Color(0xFFE2E8F0))
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(enabled = !isSubmitting, selected = selectedMethod == code, onClick = { selectedMethod = code })
                            Spacer(Modifier.width(10.dp))
                            Text(label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (selectedMethod == "MPESA") {
                    OutlinedTextField(value = phone, onValueChange = { phone = it },
                        enabled = !isSubmitting, label = { Text("M-Pesa phone number") },
                        placeholder = { Text("0712345678") }, singleLine = true)
                }
                errorMessage?.let { Text(it, color = Color(0xFFDC2626)) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirmSettle(selectedMethod, phone) },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B874))
                    ) {
                        Text(if (isSubmitting) "Processing…" else if (selectedMethod == "MPESA") "Send M-Pesa Prompt" else "Confirm Settlement")
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopProFormaModal(
    tab: DesktopTabOrder,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            modifier = Modifier.width(420.dp)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("BILL PREVIEW", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                Text("Tab #${tab.orderNumber} · Table: ${tab.deliveryLocation}", fontSize = 13.sp, color = Color(0xFF64748B))

                Divider()

                LazyColumn(Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tab.items) { item ->
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("${item.quantity}x ${item.productName}", fontSize = 13.sp)
                            Text("KES ${String.format("%,.2f", item.unitPrice * item.quantity)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider()

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("TOTAL PAYABLE:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("KES ${String.format("%,.2f", tab.subtotal)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF00B874))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
