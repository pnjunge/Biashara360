package com.app.biashara.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Serializable
private data class IncomingPortalItem(val name: String, val quantity: Int)
@Serializable
private data class IncomingPortalOrder(
    val id: String, val orderNumber: String, val customerName: String,
    val location: String, val amount: Double, val paymentStatus: String,
    val claimedBy: String? = null, val items: List<IncomingPortalItem> = emptyList()
)
@Serializable
private data class IncomingPortalQueue(
    val waiting: List<IncomingPortalOrder> = emptyList(), val mine: List<IncomingPortalOrder> = emptyList()
)

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

        OutlinedButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)) {
            Text("Portal orders (${queue.waiting.size})${if (error != null) " !" else ""}", fontSize = 11.sp)
        }
        if (open) AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Portal orders") },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(min = 380.dp, max = 600.dp)) {
                    Text("Any staff member in this business can claim a waiting order.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !mine, onClick = { mine = false }, label = { Text("Waiting (${queue.waiting.size})") })
                        FilterChip(selected = mine, onClick = { mine = true }, label = { Text("My orders (${queue.mine.size})") })
                    }
                    message?.let { Text(it, color = Color(0xFF047857)) }
                    error?.let { Text(it, color = Color(0xFFB91C1C)) }
                    val orders = if (mine) queue.mine else queue.waiting
                    if (orders.isEmpty()) Text(if (mine) "You have no active claimed portal orders." else "No portal orders are waiting.")
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        orders.forEach { order ->
                            Surface(border = BorderStroke(1.dp, Color(0xFFDCE4E2)), shape = MaterialTheme.shapes.medium) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${order.orderNumber} · ${order.location}", fontWeight = FontWeight.Bold)
                                    Text("${order.customerName} · KES ${String.format("%,.2f", order.amount)}")
                                    Text("Payment: ${order.paymentStatus}")
                                    order.items.forEach { Text("${it.quantity} × ${it.name}") }
                                    if (mine) Text("Claimed by you", color = Color(0xFF047857))
                                    else Button(enabled = !busy, onClick = {
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
                                    }) { Text(if (busy) "Claiming…" else "Claim order") }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}
