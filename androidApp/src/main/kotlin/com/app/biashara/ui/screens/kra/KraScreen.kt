package com.app.biashara.ui.screens.kra

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.app.biashara.ui.theme.*

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


// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KraScreen() {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Compliance", "eTIMS", "Returns", "Setup")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KRA iTax", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface),
                actions = {
                    TextButton(onClick = {}) {
                        Text("iTax Portal", color = B360Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = B360Green, modifier = Modifier.size(16.dp).padding(start = 2.dp))
                    }
                }
            )
        },
        containerColor = B360Surface
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(B360Surface)) {
            ScrollableTabRow(
                selectedTabIndex = tab,
                containerColor = Color.White,
                contentColor = B360Green,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tab]),
                        color = B360Green
                    )
                }
            ) {
                tabs.forEachIndexed { i, title ->
                    val isSelected = tab == i
                    Tab(
                        selected = isSelected,
                        onClick = { tab = i },
                        text = { Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, color = if (isSelected) B360Green else Color(0xFF64748B)) }
                    )
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

    LazyColumn(modifier = Modifier.fillMaxSize().background(B360Surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Circular score indicator
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { score / 100f },
                            modifier = Modifier.size(80.dp),
                            color = scoreColor,
                            trackColor = Color(0xFFE2E8F0),
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$score", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = scoreColor)
                            Text("/100", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                    }
                    Column {
                        Text("Compliance Score", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        Text("✅ Good standing", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
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
            Text("Action Items", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
        }

        items(listOf(
            "📅 March 2026 VAT3 return due 20 Apr. Generate and upload now.",
            "📊 94% of invoices transmitted. Retry 3 failed invoices."
        )) { rec ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(rec, fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                }
            }
        }

        item {
            // iTax filing guide card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFC5CAE9).copy(alpha = 0.5f))
            ) {
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
                            Text(step, fontSize = 13.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
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
    var invoices by remember { mutableStateOf(emptyList<EtimsInvoiceUi>()) }

    val transmitted = invoices.count { it.status == "TRANSMITTED" }
    val errors      = invoices.count { it.status == "ERROR" || it.status == "PENDING" }

    LazyColumn(modifier = Modifier.fillMaxSize().background(B360Surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KraKpiCard("Transmitted","$transmitted","Signed by KRA", B360Green, Modifier.weight(1f))
                KraKpiCard("Failed","$errors","Need retry", if (errors > 0) Color(0xFFC62828) else Color(0xFF64748B), Modifier.weight(1f))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = B360GreenBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, B360Green.copy(0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("What is KRA eTIMS?", fontWeight = FontWeight.Bold, color = B360Green, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Every sale must be transmitted to KRA in real-time. Each invoice gets a unique KRA number and QR code printed on the receipt, verifiable at etims.kra.go.ke", fontSize = 12.sp, color = B360Green.copy(alpha = 0.85f), lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        if (errors > 0) {
            item {
                Button(
                    onClick = {
                        invoices = invoices.map { if (it.status == "ERROR" || it.status == "PENDING") it.copy(status = "TRANSMITTED", etimsNumber = "NS00000${it.id}X") else it }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Retry $errors Failed Invoice${if (errors > 1) "s" else ""}", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        items(invoices, key = { it.id }) { inv ->
            val (statusColor, statusBg, statusLabel) = when (inv.status) {
                "TRANSMITTED" -> Triple(B360Green, B360GreenBg, "Transmitted")
                "PENDING"     -> Triple(Color(0xFFFF8F00), Color(0xFFFFF8E1), "Pending")
                else          -> Triple(Color(0xFFC62828), Color(0xFFFFEBEE), "Error")
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                            Column {
                                Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A))
                                Text(
                                    if (inv.etimsNumber != null) "KRA: ${inv.etimsNumber}" else "Awaiting KRA number",
                                    fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium
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
                            Text("VAT", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Text("KES ${"%,.0f".format(inv.taxAmount)}", fontWeight = FontWeight.Bold, color = B360Green)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Text("KES ${"%,.0f".format(inv.totalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                    }
                    inv.submittedAt?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("Transmitted: $it", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ── Returns Tab ───────────────────────────────────────────────────────────────

@Composable
fun KraReturnsTab() {
    var returns by remember { mutableStateOf(emptyList<TaxReturnUi>()) }
    val typeColor = mapOf("VAT3" to B360Green, "TOT" to Color(0xFF1565C0), "WHT" to Color(0xFF6A1B9A))
    val typeBg    = mapOf("VAT3" to B360GreenBg, "TOT" to Color(0xFFE3F2FD), "WHT" to Color(0xFFF3E5F5))
    val statusColor = mapOf("DRAFT" to Color(0xFF64748B), "GENERATED" to Color(0xFF1565C0), "SUBMITTED" to Color(0xFFFF8F00), "ACKNOWLEDGED" to B360Green)
    val statusBg    = mapOf("DRAFT" to Color(0xFFF1F5F9), "GENERATED" to Color(0xFFE3F2FD), "SUBMITTED" to Color(0xFFFFF8E1), "ACKNOWLEDGED" to B360GreenBg)

    LazyColumn(modifier = Modifier.fillMaxSize().background(B360Surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Generate New Return", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 8.dp))
                    Text("Use the controls below to generate a VAT3, TOT, or WHT return for any past period.", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Generate Return", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        items(returns, key = { it.id }) { r ->
            val tc = typeColor[r.returnType] ?: Color.Gray
            val tb = typeBg[r.returnType] ?: Color(0xFFF1F5F9)
            val sc = statusColor[r.status] ?: Color.Gray
            val sb = statusBg[r.status] ?: Color(0xFFF1F5F9)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.background(tb, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(r.returnType, color = tc, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                            Column {
                                Text(r.periodLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("Due ${r.dueDate}", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                        }
                        Box(modifier = Modifier.background(sb, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(r.status, color = sc, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tax Payable", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Text("KES ${"%,.0f".format(r.taxAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = tc)
                    }
                    r.ackNo?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("iTax Ack: $it", fontSize = 11.sp, color = Color(0xFF64748B), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = tc),
                            border = BorderStroke(1.dp, tc.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(2f),
                            colors = ButtonDefaults.buttonColors(containerColor = tc),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Upload on iTax", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    var pin by remember { mutableStateOf("") }
    var sdcId by remember { mutableStateOf("") }
    var env by remember { mutableStateOf("sandbox") }
    var saved by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(B360Surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("KRA Taxpayer Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 14.dp))
                    Text("KRA PIN *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 5.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.uppercase() },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("P051234567X") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = B360Green,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Format: letter + 9 digits + letter (e.g. P051234567X)", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("eTIMS Virtual Device", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 6.dp))
                    Text("Register at etims.kra.go.ke to get your SDC ID.", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 14.dp))
                    Text("SDC ID", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 5.dp))
                    OutlinedTextField(
                        value = sdcId,
                        onValueChange = { sdcId = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("From KRA eTIMS portal") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = B360Green,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Initialise Device with KRA", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Environment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("sandbox" to "Sandbox (Test)", "production" to "Production (Live)").forEach { (value, label) ->
                            val selected = env == value
                            val color    = if (value == "production") Color(0xFFE65100) else B360Green
                            OutlinedButton(
                                onClick = { env = value },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) color.copy(alpha = 0.1f) else Color.Transparent,
                                    contentColor   = if (selected) color else Color(0xFF64748B)
                                ),
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) color else Color(0xFFE2E8F0))
                            ) { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { saved = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(if (saved) Icons.Default.CheckCircle else Icons.Default.Save, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(if (saved) "Saved!" else "Save KRA Profile", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ── Shared Composables ────────────────────────────────────────────────────────

@Composable
private fun KraKpiCard(title: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
            Text(sub, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun KraBadge(label: String, color: Color) {
    Box(modifier = Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
