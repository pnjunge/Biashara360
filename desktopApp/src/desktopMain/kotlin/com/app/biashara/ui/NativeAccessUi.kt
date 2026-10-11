package com.app.biashara.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
        var currentTime by remember { mutableStateOf(ZonedDateTime.now(ZoneId.of("Africa/Nairobi"))) }
        LaunchedEffect(Unit) {
            while (true) {
                currentTime = ZonedDateTime.now(ZoneId.of("Africa/Nairobi"))
                delay(1_000)
            }
        }
        val green = Color(0xFF009E7B)
        val ink = Color(0xFF10223E)
        val amount = openingFloat.toDoubleOrNull()
        val canOpenDay = status?.dayOpen == false && status?.canOpenDay == true
        val canStart = status?.personalOpen == false && status?.canStartPersonal == true
        Dialog(
            onDismissRequest = { if (!busy) dismissed = true },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(20.dp),
                shape = RoundedCornerShape(24.dp), color = Color.White,
                shadowElevation = 24.dp
            ) {
                Column(Modifier.heightIn(max = 650.dp).verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val wide = maxWidth >= 520.dp
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (wide) ShiftPosIllustration(Modifier.size(170.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                    Text("Start your shift", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = ink, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { dismissed = true }, enabled = !busy, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, "Close shift prompt", tint = ink, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Text(when {
                                    status == null -> "Your shift status could not be checked. Try again to continue."
                                    status?.dayOpen == false -> "The trading day is closed. Start your shift to begin work."
                                    else -> "The trading day is open. Start your personal shift to begin work."
                                }, color = Color(0xFF415574), fontSize = 15.sp, lineHeight = 24.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShiftInfoCard("Date", currentTime.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)), Icons.Default.CalendarToday, Modifier.weight(1.3f))
                                    ShiftInfoCard("Current time", currentTime.format(DateTimeFormatter.ofPattern("HH:mm")), Icons.Default.Schedule, Modifier.weight(1f))
                                    ShiftInfoCard("Device", "Desktop", Icons.Default.Computer, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    if (canOpenDay) {
                        OutlinedTextField(
                            value = openingFloat, onValueChange = { openingFloat = it },
                            label = { Text("Opening float (KES)") }, singleLine = true,
                            enabled = !busy, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            isError = amount == null || !amount.isFinite() || amount < 0
                        )
                    }
                    error?.let {
                        Surface(color = Color(0xFFFFF1F2), shape = RoundedCornerShape(10.dp)) {
                            Text(it, color = Color(0xFFB42338), modifier = Modifier.fillMaxWidth().padding(12.dp), fontSize = 13.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            enabled = !busy, onClick = { dismissed = true },
                            modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(9.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDF1F6), contentColor = ink)
                        ) { Text("Later", fontWeight = FontWeight.SemiBold) }
                        OutlinedButton(
                            enabled = !busy,
                            onClick = { scope.launch { busy = true; try { refresh() } finally { busy = false } } },
                            modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(9.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = green)
                        ) { Icon(Icons.Default.Refresh, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Check again", fontWeight = FontWeight.SemiBold) }
                        if (canOpenDay || canStart) {
                            Button(
                                enabled = !busy && (!canOpenDay || (amount != null && amount.isFinite() && amount >= 0)),
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        try {
                                            runCatching {
                                                if (canOpenDay) api.openDay(requireNotNull(amount)) else api.startPersonal()
                                                refresh()
                                            }.onFailure { error = it.message ?: "Unable to start shift" }
                                        } finally { busy = false }
                                    }
                                },
                                modifier = Modifier.weight(1.2f).height(46.dp), shape = RoundedCornerShape(9.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                                Text(if (canOpenDay) "Open trading day" else "Start my shift", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
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


@Composable
private fun ShiftInfoCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Surface(modifier, color = Color(0xFFF5F8FC), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Color(0xFF315071), modifier = Modifier.size(18.dp))
            Text(label, color = Color(0xFF526580), fontSize = 10.sp)
            Text(value, color = Color(0xFF213A58), fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 2)
        }
    }
}

@Composable
private fun ShiftPosIllustration(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Color(0xFFDDF4EE), radius = w * .46f, center = Offset(w * .48f, h * .5f))
            drawRoundRect(Color(0xFF0B4347), Offset(w * .17f, h * .49f), Size(w * .5f, h * .32f), CornerRadius(w * .05f))
            drawRoundRect(Color(0xFF086A60), Offset(w * .2f, h * .53f), Size(w * .44f, h * .19f), CornerRadius(w * .025f))
            drawRoundRect(Color(0xFF07383F), Offset(w * .32f, h * .3f), Size(w * .27f, h * .23f), CornerRadius(w * .035f))
            drawRect(Color.White, Offset(w * .36f, h * .36f), Size(w * .19f, h * .18f))
            drawLine(Color(0xFFBCDCD5), Offset(w * .39f, h * .39f), Offset(w * .52f, h * .39f), strokeWidth = w * .015f)
            for (row in 0..1) for (col in 0..2) {
                drawRoundRect(if (col == 2) Color(0xFF27CF9C) else Color.White, Offset(w * (.25f + col * .1f), h * (.57f + row * .075f)), Size(w * .065f, h * .045f), CornerRadius(w * .006f))
            }
            drawLine(Color(0xFFB0CFD0), Offset(w * .12f, h * .82f), Offset(w * .78f, h * .82f), strokeWidth = w * .017f)
        }
        Surface(Modifier.align(Alignment.TopEnd).padding(top = 8.dp).size(62.dp), shape = RoundedCornerShape(50), color = Color.White) {
            Icon(Icons.Default.Schedule, null, tint = Color(0xFF008966), modifier = Modifier.padding(3.dp))
        }
    }
}

@Composable
fun DesktopTabOpenedDialog(number: String, onContinue: () -> Unit, onViewTabs: () -> Unit) {
    Dialog(onDismissRequest = onContinue, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 670.dp).fillMaxWidth().padding(20.dp), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 24.dp) {
            Column(Modifier.padding(30.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                    Box(Modifier.size(150.dp).background(Color(0xFFE2F7EF), RoundedCornerShape(75.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ReceiptLong, null, Modifier.size(92.dp), tint = Color(0xFF123943))
                        Icon(Icons.Default.CheckCircle, null, Modifier.align(Alignment.TopEnd).padding(8.dp).size(58.dp).background(Color.White, RoundedCornerShape(50)), tint = Color(0xFF009E7B))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tab opened", Modifier.weight(1f), fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10223E))
                            IconButton(onClick = onContinue) { Icon(Icons.Default.Close, "Close confirmation") }
                        }
                        Text("Tab #$number is open.\nYou can find it under Open Tabs.", color = Color(0xFF415574), fontSize = 15.sp, lineHeight = 24.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    OutlinedButton(onClick = onViewTabs, shape = RoundedCornerShape(9.dp)) {
                        Icon(Icons.Default.ReceiptLong, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("View Open Tabs")
                    }
                    Button(onClick = onContinue, shape = RoundedCornerShape(9.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009E7B))) {
                        Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Continue")
                    }
                }
            }
        }
    }
}
