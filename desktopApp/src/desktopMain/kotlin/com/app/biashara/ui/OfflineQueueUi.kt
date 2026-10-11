package com.app.biashara.ui

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.biashara.data.offline.NativeOffline

@Composable
fun DesktopOfflineQueueButton() {
    val state by NativeOffline.state.collectAsState()
    var open by remember { mutableStateOf(false) }
    if (state.pending > 0) TextButton(onClick = { open = true }) { Text("Pending sync (${state.pending})") }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Pending offline actions") }, text = {
        Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NativeOffline.owner()?.let { owner -> NativeOffline.store?.queued(owner)?.forEach { request ->
                Text("${request.method} · ${request.url.substringAfter("/v1/")}\n${request.error ?: "Waiting to sync"}")
            } }
            Text("Server permissions, stock and shift checks run again during sync. Rejected actions stay here for review.")
        }
    }, confirmButton = {
        TextButton(onClick = {
            NativeOffline.owner()?.let { owner -> NativeOffline.store?.let { store ->
                store.queued(owner).forEach { store.enqueue(owner, it.copy(error = null)) }
            } }
            NativeOffline.update(); open = false
        }) { Text("Retry sync") }
    }, dismissButton = { TextButton(onClick = { open = false }) { Text("Close") } })
}
