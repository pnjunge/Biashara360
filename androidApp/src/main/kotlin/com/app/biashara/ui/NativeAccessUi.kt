package com.app.biashara.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.biashara.data.remote.*
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NativeLoginShiftPrompt(client: HttpClient, access: NativeAccess?) {
    var dismissed by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<LoginShiftStatus?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var openingFloat by remember { mutableStateOf("0") }
    val api = remember(client) { NativeShiftClient(client) }
    val scope = rememberCoroutineScope()
    suspend fun refresh() {
        if (access == null) return
        runCatching { api.check(access) }.onSuccess { status = it; error = null; if (it?.needsPrompt != true) dismissed = true }
            .onFailure { error = it.message ?: "Unable to check your shift" }
    }
    LaunchedEffect(access, dismissed) {
        if (access != null && !dismissed) {
            while (true) {
                if (!busy) {
                    busy = true
                    try { refresh() } finally { busy = false }
                }
                delay(5_000)
            }
        }
    }
    if (!dismissed && access != null && (status?.needsPrompt == true || error != null)) {
        AlertDialog(
            onDismissRequest = { if (!busy) dismissed = true },
            title = { Text("Start your shift") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(when {
                        status == null -> "Your shift status could not be checked."
                        status?.dayOpen == false -> "The trading day is closed. Start your shift to begin work."
                        else -> "The trading day is open. Start your personal shift to begin work."
                    })
                    if (status?.dayOpen == false && status?.canOpenDay == true) {
                        OutlinedTextField(value = openingFloat, onValueChange = { openingFloat = it }, label = { Text("Opening float") }, singleLine = true)
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Column {
                    if (status?.dayOpen == false && status?.canOpenDay == true) {
                        val amount = openingFloat.toDoubleOrNull()
                        TextButton(enabled = !busy && amount != null && amount.isFinite() && amount >= 0, onClick = {
                            scope.launch {
                                busy = true
                                runCatching { api.openDay(amount!!); refresh() }.onFailure { error = it.message }
                                busy = false
                            }
                        }) { Text("Open trading day") }
                    }
                    if (status?.personalOpen == false && status?.canStartPersonal == true) {
                        TextButton(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                runCatching { api.startPersonal(); refresh() }.onFailure { error = it.message }
                                busy = false
                            }
                        }) { Text("Start my shift") }
                    }
                    TextButton(enabled = !busy, onClick = { scope.launch { busy = true; refresh(); busy = false } }) { Text("Check again") }
                }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { dismissed = true }) { Text("Later") } }
        )
    }
}

@Composable
fun NativeAccessGate(allowed: Boolean, loading: Boolean, error: String?, onRetry: () -> Unit, onBack: () -> Unit, content: @Composable () -> Unit) {
    if (allowed) content() else {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) CircularProgressIndicator()
            Text(if (loading) "Loading your access rights…" else error ?: "You do not have access to this screen.")
            if (!loading) {
                TextButton(onClick = onRetry) { Text("Reload access rights") }
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
    }
}

@Composable
fun hasNativePermission(permission: String): Boolean {
    val access by NativeAccessSession.access.collectAsState()
    return access?.permits(permission) == true
}
