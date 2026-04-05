package com.app.biashara.ui.screens.kra

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.B360Green

// ── Data ──────────────────────────────────────────────────────────────────────

data class EtimsInvoiceUi(
    val id: String, val invoiceNumber: String, val etimsNumber: String?,
    val status: String, val taxableAmount: Double, val taxAmount: Double,
    val totalAmount: Double, val submittedAt: String?
)

data class TaxReturnUi(
    val id: String, val returnType: String, val periodLabel: String,
    val dueDate: String, val status: String, val taxAmount: Double,
    val ackNo: String?
)

private val sampleEtims = listOf(
    EtimsInvoiceUi("1","INV-2026-0147","NS00000147","TRANSMITTED",12500.0,2000.0,14500.0,"Today 14:22"),
    EtimsInvoiceUi("2","INV-2026-0146","NS00000146","TRANSMITTED",8600.0,1376.0,9976.0,"Today 11:05"),
    EtimsInvoiceUi("3","INV-2026-0145",null,"ERROR",3200.0,512.0,3712.0,null),
    EtimsInvoiceUi("4","INV-2026-0144","NS00000144","TRANSMITTED",21000.0,3360.0,24360.0,"Yesterday 10:15"),
    EtimsInvoiceUi("5","INV-2026-0143",null,"PENDING",5500.0,880.0,6380.0,null),
)
private val sampleReturns = listOf(
    TaxReturnUi("1","VAT3","Mar 2026","2026-04-20","GENERATED",71200.0,null),
    TaxReturnUi("2","VAT3","Feb 2026","2026-03-20","SUBMITTED",67200.0,"ACK202602VAT001"),
    TaxReturnUi("3","VAT3","Jan 2026","2026-02-20","ACKNOWLEDGED",60800.0,"ACK202601VAT001"),
    TaxReturnUi("4","WHT","Feb 2026","2026-03-20","SUBMITTED",1350.0,"ACK202602WHT001"),
)

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KraScreen() {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Compliance", "eTIMS", "Returns", "Setup")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KRA iTax", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    TextButton(onClick = {}) {
                        Text("iTax Portal", color = B360Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = B360Green, modifier = Modifier.size(16.dp).padding(start = 2.dp))
                    }
                }
            )
        },
        containerColor = Color(0xFFF4F7F5)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, containerColor = Color.White, contentColor = B360Green, edgePadding = 0.dp) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i },
                        text = { Text(title, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) })
                }
            }
            when (tab) {
                0 -> KraComplianceTab()
                1 -> KraEtimsTab()
                2 -> KraReturnsTab()
                3 -> KraSetupTab()
            }
        }
    }
}

// ── Compliance Tab ────────────────────────────────────────────────────────────

@Composable
fun KraComplianceTab() {
    val score = 82
    val scoreColor = if (score >= 80) B360Green else if (score >= 50) Color(0xFFFF8F00) else Color(0xFFC62828)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Circular score indicator
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { score / 100f },
                            modifier = Modifier.size(80.dp),
                            color = scoreColor,
                            trackColor = Color(0xFFE0E0E0),
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$score", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = scoreColor)
                            Text("/100", fontSize = 9.sp, color = Color(0xFF888888))
                        }
                    }
                    Column {
                        Text("Compliance Score", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("✅ Good standing", fontSize = 12.sp, color = Color(0xFF666666), modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KraBadge("VAT Registered", B360Green)
                            KraBadge("eTIMS Active", Color(0xFF1565C0))
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KraKpiCard("PIN","P051234567X","KRA Taxpayer ID", B360Green, Modifier.weight(1f))
                KraKpiCard("eTIMS Rate","94%","Transmitted",Color(0xFF1565C0), Modifier.weight(1f))
            }
        }

        item {
            Text("Action Items", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        items(listOf(
            "📅 March 2026 VAT3 return due 20 Apr. Generate and upload now.",
            "📊 94% of invoices transmitted. Retry 3 failed invoices."
        )) { rec ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(10.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(rec, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF999999))
                }
            }
        }

        item {
            // iTax filing guide card
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF)), shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC5CAE9))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 iTax Filing Guide", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1565C0), modifier = Modifier.padding(bottom = 12.dp))
                    listOf(
                        "1." to "Generate return (VAT3 / TOT / WHT tab)",
                        "2." to "Download the KRA-format CSV",
                        "3." to "Log in at itax.kra.go.ke → Returns → File Returns",
                        "4." to "Upload CSV and submit",
                        "5." to "Paste acknowledgement number back here",
                    ).forEach { (num, step) ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(num, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0), fontSize = 13.sp, modifier = Modifier.width(24.dp))
                            Text(step, fontSize = 13.sp, color = Color(0xFF333333))
                        }
                    }
                }
            }
        }
    }
}

// ── eTIMS Tab ─────────────────────────────────────────────────────────────────

@Composable
fun KraEtimsTab() {
    var invoices by remember { mutableStateOf(sampleEtims) }

    val transmitted = invoices.count { it.status == "TRANSMITTED" }
    val errors      = invoices.count { it.status == "ERROR" || it.status == "PENDING" }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KraKpiCard("Transmitted","$transmitted","Signed by KRA", B360Green, Modifier.weight(1f))
                KraKpiCard("Failed","$errors","Need retry", if (errors > 0) Color(0xFFC62828) else Color(0xFF999999), Modifier.weight(1f))
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("What is KRA eTIMS?", fontWeight = FontWeight.Bold, color = B360Green, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Every sale must be transmitted to KRA in real-time. Each invoice gets a unique KRA number and QR code printed on the receipt, verifiable at etims.kra.go.ke", fontSize = 12.sp, color = Color(0xFF388E3C), lineHeight = 18.sp)
                }
            }
        }

        if (errors > 0) {
            item {
                Button(onClick = {
                    invoices = invoices.map { if (it.status == "ERROR" || it.status == "PENDING") it.copy(status = "TRANSMITTED", etimsNumber = "NS00000${it.id}X") else it }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Retry $errors Failed Invoice${if (errors > 1) "s" else ""}")
                }
            }
        }

        items(invoices, key = { it.id }) { inv ->
            val (statusColor, statusBg, statusLabel) = when (inv.status) {
                "TRANSMITTED" -> Triple(B360Green, Color(0xFFE8F5E9), "Transmitted")
                "PENDING"     -> Triple(Color(0xFFFF8F00), Color(0xFFFFF8E1), "Pending")
                else          -> Triple(Color(0xFFC62828), Color(0xFFFFEBEE), "Error")
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                            Column {
                                Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    if (inv.etimsNumber != null) "KRA: ${inv.etimsNumber}" else "Awaiting KRA number",
                                    fontSize = 11.sp, color = Color(0xFF888888)
                                )
                            }
                        }
                        Box(modifier = Modifier.background(statusBg, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("VAT", fontSize = 11.sp, color = Color(0xFF999999))
                            Text("KES ${"%,.0f".format(inv.taxAmount)}", fontWeight = FontWeight.SemiBold, color = B360Green)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total", fontSize = 11.sp, color = Color(0xFF999999))
                            Text("KES ${"%,.0f".format(inv.totalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                    inv.submittedAt?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("Transmitted: $it", fontSize = 11.sp, color = Color(0xFF888888))
                    }
                }
            }
        }
    }
}

// ── Returns Tab ───────────────────────────────────────────────────────────────

@Composable
fun KraReturnsTab() {
    var returns by remember { mutableStateOf(sampleReturns) }
    val typeColor = mapOf("VAT3" to B360Green, "TOT" to Color(0xFF1565C0), "WHT" to Color(0xFF6A1B9A))
    val typeBg    = mapOf("VAT3" to Color(0xFFE8F5E9), "TOT" to Color(0xFFE3F2FD), "WHT" to Color(0xFFF3E5F5))
    val statusColor = mapOf("DRAFT" to Color(0xFF757575), "GENERATED" to Color(0xFF1565C0), "SUBMITTED" to Color(0xFFFF8F00), "ACKNOWLEDGED" to B360Green)
    val statusBg    = mapOf("DRAFT" to Color(0xFFF5F5F5), "GENERATED" to Color(0xFFE3F2FD), "SUBMITTED" to Color(0xFFFFF8E1), "ACKNOWLEDGED" to Color(0xFFE8F5E9))

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Generate New Return", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text("Use the controls below to generate a VAT3, TOT, or WHT return for any past period.", fontSize = 12.sp, color = Color(0xFF666666))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = B360Green), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Generate Return")
                    }
                }
            }
        }

        items(returns, key = { it.id }) { r ->
            val tc = typeColor[r.returnType] ?: Color.Gray
            val tb = typeBg[r.returnType] ?: Color(0xFFF5F5F5)
            val sc = statusColor[r.status] ?: Color.Gray
            val sb = statusBg[r.status] ?: Color(0xFFF5F5F5)

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.background(tb, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(r.returnType, color = tc, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                            Column {
                                Text(r.periodLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Due ${r.dueDate}", fontSize = 11.sp, color = Color(0xFF888888))
                            }
                        }
                        Box(modifier = Modifier.background(sb, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(r.status, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tax Payable", fontSize = 12.sp, color = Color(0xFF666666))
                        Text("KES ${"%,.0f".format(r.taxAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = tc)
                    }
                    r.ackNo?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("iTax Ack: $it", fontSize = 11.sp, color = Color(0xFF888888), fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = tc)) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CSV", fontSize = 12.sp)
                        }
                        Button(onClick = {}, modifier = Modifier.weight(2f), colors = ButtonDefaults.buttonColors(containerColor = tc), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Upload on iTax", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Setup Tab ─────────────────────────────────────────────────────────────────

@Composable
fun KraSetupTab() {
    var pin by remember { mutableStateOf("P051234567X") }
    var sdcId by remember { mutableStateOf("SDCK2024001") }
    var env by remember { mutableStateOf("sandbox") }
    var saved by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("KRA Taxpayer Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 14.dp))
                    Text("KRA PIN *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF666666), modifier = Modifier.padding(bottom = 5.dp))
                    OutlinedTextField(value = pin, onValueChange = { pin = it.uppercase() }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("P051234567X") }, singleLine = true, shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp))
                    Spacer(Modifier.height(6.dp))
                    Text("Format: letter + 9 digits + letter (e.g. P051234567X)", fontSize = 11.sp, color = Color(0xFF888888))
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("eTIMS Virtual Device", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
                    Text("Register at etims.kra.go.ke to get your SDC ID.", fontSize = 12.sp, color = Color(0xFF666666), modifier = Modifier.padding(bottom = 14.dp))
                    Text("SDC ID", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF666666), modifier = Modifier.padding(bottom = 5.dp))
                    OutlinedTextField(value = sdcId, onValueChange = { sdcId = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("From KRA eTIMS portal") }, singleLine = true, shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.FlashOn, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Initialise Device with KRA")
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Environment", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("sandbox" to "Sandbox (Test)", "production" to "Production (Live)").forEach { (value, label) ->
                            val selected = env == value
                            val color    = if (value == "production") Color(0xFFE65100) else B360Green
                            OutlinedButton(
                                onClick = { env = value }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) color.copy(alpha = 0.1f) else Color.Transparent,
                                    contentColor   = if (selected) color else Color(0xFF666666)
                                ),
                                border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) color else Color(0xFFE0E0E0))
                            ) { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                        }
                    }
                }
            }
        }

        item {
            Button(onClick = { saved = true }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = B360Green), shape = RoundedCornerShape(12.dp)) {
                Icon(if (saved) Icons.Default.CheckCircle else Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (saved) "Saved!" else "Save KRA Profile", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Shared Composables ────────────────────────────────────────────────────────

@Composable
private fun KraKpiCard(title: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = Color(0xFF888888), modifier = Modifier.padding(bottom = 5.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
            Text(sub, fontSize = 11.sp, color = Color(0xFFAAAAAA))
        }
    }
}

@Composable
private fun KraBadge(label: String, color: Color) {
    Box(modifier = Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
