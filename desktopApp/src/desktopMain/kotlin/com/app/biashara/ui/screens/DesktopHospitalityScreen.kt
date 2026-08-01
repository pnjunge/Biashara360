package com.app.biashara.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class DesktopHospitalityTable(
    val id: String,
    val name: String,
    val area: String,
    val capacity: Int,
    val status: String,
    val openAmount: Double = 0.0,
    val openOrderCount: Int = 0,
)

@Serializable
private data class DesktopHospitalityOrder(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val deliveryLocation: String,
    val serviceType: String,
    val tabStatus: String,
    val subtotal: Double,
    val createdAt: String,
)

@Serializable
private data class DesktopKitchenTicket(
    val id: String,
    val orderNumber: String,
    val tableName: String? = null,
    val station: String,
    val status: String,
)

@Serializable
private data class DesktopHospitalityDashboard(
    val enabled: Boolean,
    val tables: List<DesktopHospitalityTable> = emptyList(),
    val openTabs: List<DesktopHospitalityOrder> = emptyList(),
    val tickets: List<DesktopKitchenTicket> = emptyList(),
)
@Serializable private data class DesktopReservation(val id:String,val status:String)
@Serializable private data class DesktopIngredient(val id:String,val isLowStock:Boolean)
@Serializable private data class DesktopShift(val id:String,val status:String)
@Serializable private data class DesktopHospitalityOperations(val reservations:List<DesktopReservation> = emptyList(),val ingredients:List<DesktopIngredient> = emptyList(),val shifts:List<DesktopShift> = emptyList())

@Composable
fun DesktopHospitalityScreen(client: HttpClient = remember { inject() }) {
    DesktopHospitalityContent(client, tabsOnly = false)
}

@Composable
fun DesktopOpenTabsScreen(client: HttpClient = remember { inject() }) {
    DesktopHospitalityContent(client, tabsOnly = true)
}

@Composable
private fun DesktopHospitalityContent(client: HttpClient, tabsOnly: Boolean) {
    val scope = rememberCoroutineScope()
    var dashboard by remember { mutableStateOf<DesktopHospitalityDashboard?>(null) }
    var operations by remember { mutableStateOf<DesktopHospitalityOperations?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            runCatching {
                client.get("$BASE_URL/hospitality").body<ApiResponse<DesktopHospitalityDashboard>>() to client.get("$BASE_URL/hospitality/operations").body<ApiResponse<DesktopHospitalityOperations>>()
            }.onSuccess { (response,operationsResponse) ->
                if (response.success && response.data != null) dashboard = response.data
                else error = response.message.ifBlank { "Could not load hospitality operations." }
                if(operationsResponse.success) operations=operationsResponse.data
            }.onFailure { error = it.message ?: "Could not load hospitality operations." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(if (tabsOnly) "Open Tabs" else "Bar & Restaurant", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A))
                    Text(if (tabsOnly) "Separate customer receipts awaiting settlement" else "Tables, customer tabs, kitchen and bar tickets", color = Color(0xFF64748B))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { openDesktopWeb("/hospitality-operations") }) {
                        Icon(Icons.Default.OpenInBrowser, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Manage")
                    }
                    Button(onClick = ::load, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B874))) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        else Icon(Icons.Default.Refresh, "Refresh", Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text(if (loading) "Loading…" else "Refresh")
                    }
                }
            }
        }

        error?.let { message ->
            item { Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(10.dp)) { Text(message, color = Color(0xFFB91C1C), modifier = Modifier.padding(14.dp)) } }
        }

        dashboard?.let { data ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HospitalitySummary(Modifier.weight(1f), "Tables", data.tables.size.toString(), Icons.Default.TableRestaurant)
                    HospitalitySummary(Modifier.weight(1f), "Open tabs", data.openTabs.size.toString(), Icons.Default.Restaurant)
                    HospitalitySummary(Modifier.weight(1f), "Active tickets", data.tickets.count { it.status in setOf("NEW", "PREPARING", "READY") }.toString(), Icons.Default.Restaurant)
                    HospitalitySummary(Modifier.weight(1f), "Open amount", "KES ${String.format("%,.0f", data.openTabs.sumOf { it.subtotal })}", Icons.Default.Restaurant)
                }
            }
            if(!tabsOnly) item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    HospitalitySummary(Modifier.weight(1f),"Reservations",(operations?.reservations?.count{it.status=="BOOKED"}?:0).toString(),Icons.Default.TableRestaurant)
                    HospitalitySummary(Modifier.weight(1f),"Low ingredients",(operations?.ingredients?.count{it.isLowStock}?:0).toString(),Icons.Default.Restaurant)
                    HospitalitySummary(Modifier.weight(1f),"Shift",if(operations?.shifts?.any{it.status=="OPEN"}==true)"Open" else "Closed",Icons.Default.Restaurant)
                }
            }
            if (!tabsOnly) {
                item { Text("Tables", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A)) }
                if (data.tables.isEmpty()) item { HospitalityEmpty("No tables configured. Use Manage to create your first table.") }
                items(data.tables, key = { it.id }) { table ->
                    HospitalityRow(table.name, "${table.area} · ${table.capacity} seats", table.status, "${table.openOrderCount} tabs · KES ${String.format("%,.0f", table.openAmount)}")
                }
            }
            item { Text("Open tabs", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F1F3A), modifier = Modifier.padding(top = 8.dp)) }
            if (data.openTabs.isEmpty()) item { HospitalityEmpty("No open customer tabs.") }
            items(data.openTabs, key = { it.id }) { order ->
                HospitalityRow(order.orderNumber, "${order.deliveryLocation} · ${order.customerName}", order.tabStatus, "KES ${String.format("%,.0f", order.subtotal)}")
            }
        }
    }
}

@Composable
private fun HospitalitySummary(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier, colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF00B874)); Spacer(Modifier.width(12.dp)); Column { Text(label, color = Color(0xFF64748B), fontSize = 12.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
        }
    }
}

@Composable
private fun HospitalityRow(title: String, subtitle: String, status: String, trailing: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color(0xFF64748B), fontSize = 13.sp) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text(status.replace('_', ' '), color = Color(0xFF00A868), fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Spacer(Modifier.width(28.dp)); Text(trailing, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun HospitalityEmpty(message: String) {
    Surface(Modifier.fillMaxWidth(), color = Color.White, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) { Text(message, color = Color(0xFF64748B), modifier = Modifier.padding(20.dp)) }
}
