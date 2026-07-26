package com.app.biashara.ui.screens.tax

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.app.biashara.ui.theme.*

// ── Data ──────────────────────────────────────────────────────────────────────

data class TaxRateUi(
    val id: String,
    val taxType: String,
    val name: String,
    val ratePercent: Double,
    val isActive: Boolean,
    val appliesTo: String,
    val isInclusive: Boolean,
    val description: String
)

data class RemittanceUi(
    val id: String,
    val taxType: String,
    val period: String,
    val taxableAmount: Double,
    val taxAmount: Double,
    val status: String,
    val receiptNumber: String?
)

data class TaxLineUi(val name: String, val type: String, val ratePercent: Double, val amount: Double)

private val TYPE_COLORS = mapOf(
    "VAT"    to Color(0xFF1B8B34),
    "TOT"    to Color(0xFF1565C0),
    "WHT"    to Color(0xFF6A1B9A),
    "EXCISE" to Color(0xFFE65100),
    "CUSTOM" to Color(0xFF37474F)
)

private val KENYA_DEFAULT_TAX_RATES = listOf(
    TaxRateUi("vat-default", "VAT", "Value Added Tax", 16.0, true, "Sales", false, "Kenya standard VAT rate"),
    TaxRateUi("tot-default", "TOT", "Turnover Tax", 1.5, true, "Turnover", false, "Kenya turnover tax rate"),
    TaxRateUi("wht-default", "WHT", "Withholding Tax", 3.0, true, "Services", false, "Standard resident withholding rate"),
    TaxRateUi("excise-default", "EXCISE", "Excise Duty", 20.0, true, "Selected goods", false, "Default excise duty rate")
)




// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxScreen(onConfigureKra: () -> Unit = {}) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Summary", "Tax Rates", "Calculator", "Remittances")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tax Management", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = B360Surface),
                actions = {}
            )
        },
        containerColor = B360Surface
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().background(B360Surface)) {
            // Tabs
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
                0 -> TaxSummaryTab()
                1 -> TaxRatesTab()
                2 -> TaxCalculatorTab()
                3 -> RemittancesTab(onConfigureKra)
            }
        }
    }
}

// ── Summary Tab ───────────────────────────────────────────────────────────────

@Composable
fun TaxSummaryTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(B360Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("February 2026 Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 4.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TaxKpiCard("VAT Collected", "KES 67,200", "Feb 2026", Color(0xFF1B8B34), Modifier.weight(1f))
                TaxKpiCard("WHT Collected", "KES 1,800",  "Feb 2026", Color(0xFF6A1B9A), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TaxKpiCard("Total Liability", "KES 69,000", "All types",       Color(0xFFE65100), Modifier.weight(1f))
                TaxKpiCard("Pending Returns", "1",          "Mar VAT due Apr 20", Color(0xFFFF8F00), Modifier.weight(1f))
            }
        }
        item {
            // Effective rate bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Effective Tax Rate", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 8.dp))
                    Text("KES 69,000 tax on KES 465,000 revenue", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 10.dp))
                    LinearProgressIndicator(
                        progress = { 0.148f },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = B360Green,
                        trackColor = Color(0xFFF1F5F9)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("14.8% effective rate", fontWeight = FontWeight.Bold, color = B360Green, fontSize = 13.sp)
                }
            }
        }
        item {
            Text("KRA Filing Deadlines", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
        }
        items(listOf(
            Triple("VAT",  "20th of following month", Color(0xFF1B8B34)),
            Triple("TOT",  "20th of following month", Color(0xFF1565C0)),
            Triple("WHT",  "20th of following month", Color(0xFF6A1B9A)),
            Triple("PAYE", "9th of following month",  Color(0xFF37474F)),
        )) { (type, deadline, color) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(type, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column {
                            Text("Monthly Filing", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text(deadline, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        }
                    }
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Tax Rates Tab ─────────────────────────────────────────────────────────────

@Composable
fun TaxRatesTab() {
    var rates by remember { mutableStateOf(KENYA_DEFAULT_TAX_RATES) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(B360Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Kenya defaults seed banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = B360GreenBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, B360Green.copy(0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Kenya Tax Defaults", fontWeight = FontWeight.Bold, color = B360Green, fontSize = 13.sp)
                        Text("VAT 16% · TOT 1.5% · WHT 3% · Excise 20%", fontSize = 11.sp, color = B360Green.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("Defaults loaded", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        items(rates, key = { it.id }) { rate ->
            val typeColor = TYPE_COLORS[rate.taxType] ?: Color.Gray
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(rate.taxType, color = typeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Text(rate.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Text("${rate.ratePercent}%", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = typeColor)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(rate.description, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("Applies to: ${rate.appliesTo}")
                            Chip(if (rate.isInclusive) "Inclusive" else "Exclusive")
                        }
                        Switch(
                            checked = rate.isActive,
                            onCheckedChange = { checked ->
                                rates = rates.map { if (it.id == rate.id) it.copy(isActive = checked) else it }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = B360Green)
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = B360Green),
                border = BorderStroke(1.dp, B360Green.copy(0.3f))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add custom rates on web", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Tax Calculator Tab ────────────────────────────────────────────────────────

@Composable
fun TaxCalculatorTab() {
    var amount by remember { mutableStateOf("10000") }
    var selectedIds by remember { mutableStateOf(setOf("vat-default")) }

    val numAmount = amount.toDoubleOrNull() ?: 0.0
    val activeRates = KENYA_DEFAULT_TAX_RATES.filter { it.isActive }
    val selectedRates = activeRates.filter { it.id in selectedIds }
    val lines = selectedRates.map { r ->
        TaxLineUi(r.name, r.taxType, r.ratePercent, Math.round(numAmount * r.ratePercent / 100 * 100.0) / 100.0)
    }
    val totalTax   = lines.sumOf { it.amount }
    val grandTotal = numAmount + totalTax

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(B360Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Amount (KES)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green, unfocusedBorderColor = Color(0xFFE2E8F0))
                    )
                }
            }
        }

        item {
            Text("Select Tax Rates to Apply", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
        }

        items(activeRates, key = { it.id }) { rate ->
            val typeColor = TYPE_COLORS[rate.taxType] ?: Color.Gray
            val isSelected = rate.id in selectedIds
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    selectedIds = if (isSelected) selectedIds - rate.id else selectedIds + rate.id
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) typeColor.copy(alpha = 0.08f) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) typeColor else Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = typeColor)
                        )
                        Column {
                            Text(rate.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text(rate.taxType, fontSize = 11.sp, color = typeColor, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("${rate.ratePercent}%", fontWeight = FontWeight.ExtraBold, color = typeColor, fontSize = 16.sp)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tax Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        Text("KES ${"%,.0f".format(numAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                    lines.forEach { line ->
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${line.name} (${line.ratePercent}%)", color = TYPE_COLORS[line.type] ?: Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("+ KES ${"%,.0f".format(line.amount)}", color = TYPE_COLORS[line.type] ?: Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Tax", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("KES ${"%,.0f".format(totalTax)}", fontWeight = FontWeight.ExtraBold, color = B360Green, fontSize = 16.sp)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = B360Green),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Grand Total", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text("KES ${"%,.0f".format(grandTotal)}", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 24.sp)
                }
            }
        }
    }
}

// ── Remittances Tab ───────────────────────────────────────────────────────────

@Composable
fun RemittancesTab(onConfigureKra: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(B360Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = onConfigureKra,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Configure KRA to File", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

         items(emptyList<RemittanceUi>(), key = { it.id }) { r ->
            val (statusColor, statusBg) = when (r.status) {
                "PAID"    -> Pair(B360Green, B360GreenBg)
                "FILED"   -> Pair(Color(0xFF1565C0), Color(0xFFE3F2FD))
                else      -> Pair(Color(0xFFFF8F00), Color(0xFFFFF8E1))
            }
            val typeColor = TYPE_COLORS[r.taxType] ?: Color.Gray

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(r.taxType, color = typeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Text(r.period, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Box(modifier = Modifier.background(statusBg, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text(r.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Taxable Amount", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Text("KES ${"%,.0f".format(r.taxableAmount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Tax Due", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Text("KES ${"%,.0f".format(r.taxAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = B360Green)
                        }
                    }
                    r.receiptNumber?.let { receipt ->
                        Spacer(Modifier.height(8.dp))
                        Text("KRA Receipt: $receipt", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    }
                    if (r.status == "PENDING") {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "This remittance must be filed through KRA first", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0)),
                                border = BorderStroke(1.dp, Color(0xFF1565C0).copy(0.3f))
                            ) { Text("Mark Filed", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Enter the KRA payment receipt before marking paid", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                                shape = RoundedCornerShape(20.dp)
                            ) { Text("Mark Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

// ── Shared Composables ────────────────────────────────────────────────────────

@Composable
private fun TaxKpiCard(title: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
            Text(sub, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun Chip(label: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
    }
}
