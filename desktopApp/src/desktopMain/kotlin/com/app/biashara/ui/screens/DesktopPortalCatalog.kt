package com.app.biashara.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.biashara.data.remote.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable private data class PortalCatalogItem(val id: String, val name: String, val sku: String, val category: String, val onlineVisible: Boolean, val onlineGroup: String, val isActive: Boolean)
@Serializable private data class PortalCatalogUpdate(val onlineVisible: Boolean, val onlineGroup: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable fun DesktopPortalCatalog(client: HttpClient = remember { inject() }) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<PortalCatalogItem>>(emptyList()) }
    val groups = remember { mutableStateMapOf<String, String>() }
    var search by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launch { try { block() } catch (e: Exception) { error = e.message ?: "Portal items could not load" } finally { busy = false } }
    }
    suspend fun load() {
        val response = client.get("$BASE_URL/storefront/items").body<ApiResponse<List<PortalCatalogItem>>>()
        check(response.success && response.data != null) { response.message.ifBlank { "Could not load portal items" } }
        items = requireNotNull(response.data)
        groups.clear(); items.forEach { groups[it.id] = it.onlineGroup }
    }
    fun save(item: PortalCatalogItem, visible: Boolean = item.onlineVisible) = run {
        val response = client.put("$BASE_URL/storefront/items/${item.id}") { setBody(PortalCatalogUpdate(visible, groups[item.id].orEmpty())) }.body<ApiResponse<PortalCatalogItem>>()
        check(response.success && response.data != null) { response.message.ifBlank { "Could not save portal item" } }
        val updated = requireNotNull(response.data)
        items = items.map { if (it.id == updated.id) updated else it }; groups[updated.id] = updated.onlineGroup
    }
    LaunchedEffect(Unit) { run { load() } }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Portal Items & Groups", fontWeight = FontWeight.Bold)
        Text("Remove items from the customer portal without deleting inventory. Leave the group blank to use the inventory category.")
        OutlinedTextField(search, { search = it }, label = { Text("Search name, SKU or group") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedButton(onClick = { run { load() } }, enabled = !busy) { Text("Refresh") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        DesktopScrollColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.filter { "${it.name} ${it.sku} ${it.category} ${it.onlineGroup}".contains(search, true) }.forEach { item ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    Text("${item.sku} · ${if (!item.isActive) "Inactive" else if (item.onlineVisible) "Shown online" else "Removed from portal"}")
                    OutlinedTextField(groups[item.id].orEmpty(), { if (it.length <= 100) groups[item.id] = it }, label = { Text("Portal group") }, placeholder = { Text(item.category.ifBlank { "Other" }) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { save(item) }, enabled = !busy && groups[item.id] != item.onlineGroup) { Text("Save Group") }
                        Button(onClick = { save(item, !item.onlineVisible) }, enabled = !busy) { Text(if (item.onlineVisible) "Remove from Portal" else "Show on Portal") }
                    }
                } }
            }
            if (!busy && items.isEmpty()) Text("No inventory items yet.")
        }
    }
}
