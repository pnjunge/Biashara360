package com.app.biashara.ui.screens.payments

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.biashara.ui.theme.*

// ── Data classes ──────────────────────────────────────────────────────────────
data class CsTransactionUi(
    val id: String, val orderId: String, val csId: String,
    val txnType: String, val cardLast4: String, val cardType: String,
    val status: String, val approvalCode: String, val amount: Double, val date: String
)

data class SavedCardUi(
    val id: String, val last4: String, val type: String,
    val expiry: String, val holder: String, val isDefault: Boolean
)

// ── Mock data ─────────────────────────────────────────────────────────────────
private val mockTransactions = listOf(
    CsTransactionUi("t1", "B360-0042", "7285900622826740503954", "CAPTURE",       "4242", "VISA",       "CAPTURED",   "HH8765", 4500.0, "Today 14:32"),
    CsTransactionUi("t2", "B360-0041", "7285900622826740503955", "AUTHORIZATION", "5555", "MASTERCARD", "AUTHORIZED", "AB1234", 1500.0, "Today 11:05"),
    CsTransactionUi("t3", "B360-0039", "7285900622826740503956", "REFUND",        "4242", "VISA",       "REFUNDED",   "",       6800.0, "Yesterday"),
    CsTransactionUi("t4", "B360-0037", "7285900622826740503957", "AUTHORIZATION", "4111", "VISA",       "DECLINED",   "",       2200.0, "Mon"),
)

private val mockSavedCards = listOf(
    SavedCardUi("1", "4242", "VISA",       "12/27", "Amina Hassan", true),
    SavedCardUi("2", "5555", "MASTERCARD", "06/26", "Brian Otieno", false),
)

// ── CyberSource Screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberSourceScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Charge Card", "History", "Saved Cards")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Payments", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = B360Surface
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // KPI Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CsKpiChip("Captured", "KES 4,500", B360Green, modifier = Modifier.weight(1f))
                CsKpiChip("Authorized", "KES 1,500", B360Blue, modifier = Modifier.weight(1f))
                CsKpiChip("Declined", "1 txn", B360Red, modifier = Modifier.weight(1f))
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = B360Green,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { i, tab ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(tab, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            when (selectedTab) {
                0 -> ChargeCardTab()
                1 -> TransactionHistoryTab()
                2 -> SavedCardsTab()
            }
        }
    }
}

// ── Charge Card Tab ───────────────────────────────────────────────────────────
@Composable
fun ChargeCardTab() {
    var orderId by remember { mutableStateOf("B360-0042") }
    var amount by remember { mutableStateOf("4500") }
    var showCheckout by remember { mutableStateOf(false) }
    var useType by remember { mutableStateOf(0) } // 0=saved, 1=new

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Order Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedTextField(value = orderId, onValueChange = { orderId = it },
                        label = { Text("Order ID") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp), singleLine = true)

                    OutlinedTextField(value = amount, onValueChange = { amount = it },
                        label = { Text("Amount (KES)") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))

                    // Payment method picker
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("🔐 Saved Card", "💳 New Card").forEachIndexed { i, label ->
                            OutlinedButton(
                                onClick = { useType = i },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(2.dp, if (useType == i) B360Green else B360Border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (useType == i) B360GreenBg else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text(label, fontSize = 12.sp, color = if (useType == i) B360Green else B360TextSecondary) }
                        }
                    }
                }
            }
        }

        // Saved cards
        if (useType == 0) {
            item { Text("Saved Cards", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            items(mockSavedCards) { card -> SavedCardItem(card) }
        }

        item {
            Button(
                onClick = { showCheckout = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (useType == 0) "Charge Saved Card" else "Open Secure Checkout",
                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔐 PCI DSS Level 1 · CyberSource Unified Checkout",
                    fontSize = 11.sp, color = B360TextSecondary)
            }
        }
    }

    if (showCheckout) {
        CardPaymentDialog(
            orderId = orderId,
            amount = amount.toDoubleOrNull() ?: 0.0,
            onDismiss = { showCheckout = false }
        )
    }
}

@Composable
fun SavedCardItem(card: SavedCardUi) {
    val brandColor = if (card.type == "VISA") B360Blue else B360Red
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = brandColor.copy(alpha = 0.1f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(card.type.take(1), fontWeight = FontWeight.ExtraBold, color = brandColor, fontSize = 16.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(3.dp), color = brandColor.copy(alpha = 0.1f)) {
                        Text(card.type, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = brandColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                    Text("•••• ${card.last4}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (card.isDefault) {
                        Surface(shape = RoundedCornerShape(8.dp), color = B360GreenBg) {
                            Text("DEFAULT", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = B360Green,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }
                }
                Text("${card.holder} · Exp ${card.expiry}", fontSize = 12.sp, color = B360TextSecondary)
            }
        }
    }
}

// ── Card Payment Dialog ───────────────────────────────────────────────────────
@Composable
fun CardPaymentDialog(orderId: String, amount: Double, onDismiss: () -> Void) {
    var cardNum by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var saveCard by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    fun fmtCard(v: String): String {
        val d = v.filter { it.isDigit() }.take(16)
        return d.chunked(4).joinToString(" ")
    }

    Dialog(onDismissRequest = { if (!processing) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (result != null) {
                // Result screen
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (result == "success") "✅" else "❌", fontSize = 52.sp)
                    Text(
                        if (result == "success") "Payment Captured!" else "Card Declined",
                        fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                        color = if (result == "success") B360Green else B360Red
                    )
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
                return@Card
            }

            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = B360GreenBg, modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("🔐", fontSize = 18.sp) }
                    }
                    Column {
                        Text("Secure Payment", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text("$orderId · KES ${amount.toLong().toString().reversed().chunked(3).joinToString(",").reversed()}",
                            fontSize = 12.sp, color = B360TextSecondary)
                    }
                }

                HorizontalDivider(color = B360Border)

                // Card number
                OutlinedTextField(
                    value = cardNum,
                    onValueChange = { cardNum = fmtCard(it) },
                    label = { Text("Card Number") },
                    placeholder = { Text("1234 5678 9012 3456", color = B360TextSecondary) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospaced, fontSize = 16.sp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = expiry, onValueChange = { expiry = it.filter { c -> c.isDigit() || c == '/' }.take(5) },
                        label = { Text("MM/YY") }, modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(10.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = cvv, onValueChange = { cvv = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("CVV") }, modifier = Modifier.weight(1f), singleLine = true,
                        shape = RoundedCornerShape(10.dp), visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                OutlinedTextField(value = name, onValueChange = { name = it.uppercase() },
                    label = { Text("Cardholder Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveCard, onCheckedChange = { saveCard = it }, colors = CheckboxDefaults.colors(checkedColor = B360Green))
                    Text("Save card for future payments", fontSize = 13.sp)
                }

                if (processing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().background(B360GreenBg, RoundedCornerShape(8.dp)).padding(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = B360Green, strokeWidth = 2.dp)
                        Text("Processing with CyberSource...", fontSize = 12.sp, color = B360Green)
                    }
                }

                // Test hint
                Surface(shape = RoundedCornerShape(8.dp), color = B360AmberBg, modifier = Modifier.fillMaxWidth()) {
                    Text("🧪 Sandbox: 4111 1111 1111 1111 = decline. Any other number = approve.",
                        fontSize = 11.sp, color = B360Amber, modifier = Modifier.padding(10.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            processing = true
                            // Simulate API call
                            val raw = cardNum.replace(" ", "")
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                result = if (raw == "4111111111111111") "decline" else "success"
                                processing = false
                            }, 1800)
                        },
                        modifier = Modifier.weight(2f), enabled = !processing && cardNum.length >= 19 && expiry.isNotBlank() && cvv.isNotBlank() && name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = B360Green),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pay KES ${amount.toLong()}", fontWeight = FontWeight.Bold)
                    }
                }

                Text("🔐 PCI DSS Level 1 · CyberSource", fontSize = 10.sp, color = B360TextSecondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

// ── Transaction History Tab ───────────────────────────────────────────────────
@Composable
fun TransactionHistoryTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(mockTransactions) { txn ->
            CsTransactionRow(txn)
        }
    }
}

@Composable
fun CsTransactionRow(txn: CsTransactionUi) {
    val (statusColor, statusBg) = when (txn.status) {
        "CAPTURED"   -> B360Green to B360GreenBg
        "AUTHORIZED" -> B360Blue to B360BlueBg
        "REFUNDED"   -> B360Amber to B360AmberBg
        "DECLINED"   -> B360Red to B360RedBg
        else         -> B360TextSecondary to B360Surface
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = statusBg, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(when (txn.txnType) { "CAPTURE" -> "✓"; "REFUND" -> "↺"; "VOID" -> "✕"; else -> "◎" }, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(txn.orderId, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = B360Green)
                    Text("· ${txn.txnType}", fontSize = 11.sp, color = B360TextSecondary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(3.dp), color = B360Blue.copy(0.1f)) {
                        Text(txn.cardType, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = B360Blue,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                    }
                    Text("••${txn.cardLast4}", fontSize = 12.sp, color = B360TextSecondary)
                    if (txn.approvalCode.isNotEmpty()) Text("✓ ${txn.approvalCode}", fontSize = 11.sp, color = B360Green, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("KES ${txn.amount.toLong().toString().reversed().chunked(3).joinToString(",").reversed()}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(shape = RoundedCornerShape(20.dp), color = statusBg) {
                    Text(txn.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                }
                Text(txn.date, fontSize = 11.sp, color = B360TextSecondary)
            }
        }
    }
}

// ── Saved Cards Tab ───────────────────────────────────────────────────────────
@Composable
fun SavedCardsTab() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(mockSavedCards) { card -> SavedCardItem(card) }
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = B360GreenBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🔐 Zero PCI Scope", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = B360Green)
                    Text("Card numbers are stored in CyberSource Token Management Service. Your servers never handle raw card data.", fontSize = 12.sp, color = B360Green.copy(alpha = 0.8f), lineHeight = 18.sp)
                }
            }
        }
    }
}

// ── Mini KPI chip ─────────────────────────────────────────────────────────────
@Composable
fun CsKpiChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(1.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = color)
            Text(label, fontSize = 10.sp, color = B360TextSecondary)
        }
    }
}

// Add missing color references
private val B360GreenBg = Color(0xFFEBF7EE)
private val B360AmberBg = Color(0xFFFFF4E0)
private val B360RedBg   = Color(0xFFFDECEA)
private val B360BlueBg  = Color(0xFFE8F0FE)
private val B360TextSecondary = Color(0xFF6B7280)
private val B360Border  = Color(0xFFE5E7EB)
private val B360Surface = Color(0xFFF4F7F5)
private val B360Amber   = Color(0xFFFF8C00)
