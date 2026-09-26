package com.app.biashara.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.UserSession
import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

@Serializable
private data class IncomingPortalItem(val name: String, val quantity: Int)
@Serializable
private data class IncomingPortalOrder(
    val id: String, val orderNumber: String, val customerName: String,
    val location: String, val amount: Double, val paymentStatus: String,
    val createdAt: String = "", val claimedBy: String? = null,
    val items: List<IncomingPortalItem> = emptyList()
)
@Serializable
private data class IncomingPortalQueue(
    val waiting: List<IncomingPortalOrder> = emptyList(), val mine: List<IncomingPortalOrder> = emptyList()
)

private fun portalOrderAge(createdAt: String): String {
    val instant = runCatching { Instant.parse(createdAt) }.getOrNull() ?: return ""
    val minutes = Duration.between(instant, Instant.now()).toMinutes().coerceAtLeast(0)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60} hr ago"
        else -> "${minutes / 1440} d ago"
    }
}

@Composable
private fun PortalMeta(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(15.dp))
        Text(text, color = Color(0xFF475569), fontSize = 12.sp)
    }
}

@Composable
fun PortalOrdersButton(client: HttpClient) {
    val user by UserSession.currentUser.collectAsState()
    if (user?.businessId.isNullOrBlank()) return
    key(user?.id, user?.businessId) {
        val scope = rememberCoroutineScope()
        val mutex = remember { Mutex() }
        val known = remember { mutableSetOf<String>() }
        var queue by remember { mutableStateOf(IncomingPortalQueue()) }
        var open by remember { mutableStateOf(false) }
        var mine by remember { mutableStateOf(false) }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var message by remember { mutableStateOf<String?>(null) }

        suspend fun refresh() {
            val result = client.get("$BASE_URL/portal-orders").body<ApiResponse<IncomingPortalQueue>>()
            val data = result.data
            check(result.success && data != null) { result.message.ifBlank { "Could not load portal orders." } }
            if (data.waiting.any { it.id !in known }) {
                open = true; mine = false; message = "New portal orders are waiting to be claimed."
            }
            known.clear(); known.addAll(data.waiting.map { it.id })
            queue = data
        }

        LaunchedEffect(Unit) {
            while (isActive) {
                try { mutex.withLock { refresh(); error = null } }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "Portal orders could not refresh." }
                delay(5000)
            }
        }

        OutlinedButton(
            onClick = { open = true },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF047857))
        ) {
            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text("Portal orders (${queue.waiting.size})${if (error != null) " !" else ""}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        if (open) AlertDialog(
            onDismissRequest = { open = false },
            title = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Surface(shape = MaterialTheme.shapes.large, color = Color(0xFFE9FBF1)) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                    Column {
                        Text("Portal orders", fontWeight = FontWeight.Bold)
                        Text("Any staff member can claim a waiting order.", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Normal)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.widthIn(min = 420.dp, max = 640.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !mine, onClick = { mine = false }, label = { Text("Waiting (${queue.waiting.size})") })
                        FilterChip(selected = mine, onClick = { mine = true }, label = { Text("My orders (${queue.mine.size})") })
                    }
                    Surface(color = Color(0xFFE9FBF1), shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(18.dp))
                            Text(message ?: "New portal orders are waiting to be claimed.", color = Color(0xFF047857), fontSize = 12.sp)
                        }
                    }
                    error?.let { Text(it, color = Color(0xFFB91C1C)) }
                    val orders = if (mine) queue.mine else queue.waiting
                    if (orders.isEmpty()) Text(if (mine) "You have no active claimed portal orders." else "No portal orders are waiting.")
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        orders.forEach { order ->
                            Surface(border = BorderStroke(1.dp, Color(0xFFDCE4E2)), shape = MaterialTheme.shapes.medium, color = Color.White) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(order.orderNumber, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            Text(order.customerName, fontSize = 12.sp, color = Color(0xFF64748B))
                                        }
                                        Surface(color = Color(0xFFFFF1DB), shape = MaterialTheme.shapes.small) {
                                            Text(order.location.ifBlank { "ONLINE" }.uppercase(), color = Color(0xFFB45309), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        PortalMeta(Icons.Default.Inventory2, "KES ${String.format("%,.2f", order.amount)}")
                                        PortalMeta(Icons.Default.CreditCard, "Payment: ${order.paymentStatus.ifBlank { "Pending" }}")
                                        portalOrderAge(order.createdAt).takeIf { it.isNotBlank() }?.let { PortalMeta(Icons.Default.AccessTime, it) }
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        order.items.forEach { Text("${it.quantity} × ${it.name}", fontSize = 12.sp, color = Color(0xFF334155)) }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        if (mine) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(17.dp))
                                                Text("Claimed by you", color = Color(0xFF047857), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        } else Button(enabled = !busy, onClick = {
                                            busy = true; error = null; message = null
                                            scope.launch {
                                                mutex.withLock {
                                                    try {
                                                        val result = client.post("$BASE_URL/portal-orders/${order.id}/claim").body<ApiResponse<IncomingPortalOrder>>()
                                                        check(result.success && result.data != null) { result.message.ifBlank { "Order already claimed or unavailable." } }
                                                        refresh()
                                                        mine = true
                                                        message = "You claimed ${order.orderNumber}."
                                                    } catch (e: CancellationException) { throw e }
                                                    catch (e: Exception) {
                                                        runCatching { refresh() }
                                                        error = e.message ?: "Could not claim order."
                                                    } finally { busy = false }
                                                }
                                            }
                                        }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)) {
                                            Text(if (busy) "Claiming…" else "Claim order")
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                    }
                }
            }
            }
        )
    }
}
