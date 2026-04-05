package com.app.biashara.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.app.biashara.ui.theme.B360Green

// ── Platform Meta ─────────────────────────────────────────────────────────────
data class PlatformMeta(val label: String, val emoji: String, val color: Color, val bg: Color)

val PLATFORMS = mapOf(
    "WHATSAPP"  to PlatformMeta("WhatsApp",  "💬", Color(0xFF25D366), Color(0xFFE8FBF0)),
    "INSTAGRAM" to PlatformMeta("Instagram", "📸", Color(0xFFE1306C), Color(0xFFFDE8F0)),
    "FACEBOOK"  to PlatformMeta("Facebook",  "👥", Color(0xFF1877F2), Color(0xFFE7F0FE)),
    "TIKTOK"    to PlatformMeta("TikTok",    "🎵", Color(0xFF000000), Color(0xFFF0F0F0)),
)

// ── Data Models ───────────────────────────────────────────────────────────────
data class SocialConv(
    val id: String, val platform: String, val customerName: String,
    val customerPhone: String?, val status: String, val unreadCount: Int,
    val lastMessage: String, val lastMessageAt: String, val isAiHandled: Boolean
)

data class SocialMsg(
    val id: String, val direction: String, val senderType: String,
    val content: String, val messageType: String, val time: String, val isAiGenerated: Boolean
)

private val sampleConvs = listOf(
    SocialConv("c1","WHATSAPP","Amina Wanjiru","+254712345678","OPEN",3,"Hii unga inauzwa bei gani?","14:30",false),
    SocialConv("c2","INSTAGRAM","Kevin Omondi",null,"PENDING_PAYMENT",0,"Sawa, nitapeleka M-Pesa sasa","13:55",true),
    SocialConv("c3","FACEBOOK","Grace Muthoni","+254798765432","OPEN",1,"Do you do deliveries to Nakuru?","12:20",false),
    SocialConv("c4","TIKTOK","TikTok User 902",null,"COMPLETED",0,"Asante sana! Order imefika 🙏","10:00",true),
    SocialConv("c5","WHATSAPP","Peter Kamau","+254711223344","OPEN",2,"Mnafungua saa ngapi?","09:45",false),
)

private val sampleMsgs = mapOf(
    "c1" to listOf(
        SocialMsg("m1","INBOUND","CUSTOMER","Habari! Mnauza unga wa dhahabu?","TEXT","14:20",false),
        SocialMsg("m2","OUTBOUND","AI","Habari yako! Ndiyo, tunazo unga wa dhahabu. Bei ni KES 180 kwa kilo 2, KES 320 kwa kilo 5. Ungependa kuagiza kiasi gani? 😊","TEXT","14:21",true),
        SocialMsg("m3","INBOUND","CUSTOMER","Hii unga inauzwa bei gani kwa debe?","TEXT","14:30",false),
    ),
    "c2" to listOf(
        SocialMsg("m4","INBOUND","CUSTOMER","Ninaomba order ya 3 bottles za cooking oil","TEXT","13:30",false),
        SocialMsg("m5","OUTBOUND","AI","Asante Kevin! Cooking oil × 3 = KES 585 total. Nitakutumia maelekezo ya kulipa! 🛍️","TEXT","13:31",true),
        SocialMsg("m6","OUTBOUND","AGENT","Hujambo Kevin! 🛍️\n\nCooking Oil × 3\n💰 Jumla: KES 585\n\n💳 Lipa Mpesa:\nPaybill: 174379\nAccount: ORD-0122\nKiasi: KES 585\n\nAsante! 🙏","PAYMENT_REQUEST","13:32",false),
        SocialMsg("m7","INBOUND","CUSTOMER","Sawa, nitapeleka M-Pesa sasa","TEXT","13:55",false),
    ),
    "c3" to listOf(SocialMsg("m8","INBOUND","CUSTOMER","Do you do deliveries to Nakuru?","TEXT","12:20",false)),
    "c4" to listOf(
        SocialMsg("m9","INBOUND","CUSTOMER","Naomba order ya sugar 2kg na rice 5kg","TEXT","08:00",false),
        SocialMsg("m10","OUTBOUND","AI","Sawa! Sugar 2kg + Rice 5kg = KES 900. Nikusaidie? 😊","TEXT","08:01",true),
        SocialMsg("m11","INBOUND","CUSTOMER","Asante sana! Order imefika 🙏","TEXT","10:00",false),
    ),
    "c5" to listOf(
        SocialMsg("m12","INBOUND","CUSTOMER","Mnafungua saa ngapi?","TEXT","09:45",false),
        SocialMsg("m13","INBOUND","CUSTOMER","Na mnafunga saa ngapi jioni?","TEXT","09:46",false),
    ),
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen() {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Inbox", "Channels", "Analytics")
    val totalUnread = sampleConvs.sumOf { it.unreadCount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Social Inbox", fontWeight = FontWeight.ExtraBold)
                        Text("WhatsApp · Instagram · Facebook · TikTok", fontSize = 11.sp, color = Color(0xFF888888))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    Box(
                        modifier = Modifier.padding(end = 12.dp)
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("AI On", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = B360Green)
                    }
                }
            )
        },
        containerColor = Color(0xFFF4F7F5)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, containerColor = Color.White, contentColor = B360Green, edgePadding = 0.dp) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(title, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            if (i == 0 && totalUnread > 0) {
                                Box(
                                    modifier = Modifier.size(18.dp).background(B360Green, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { Text("$totalUnread", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }
                            }
                        }
                    })
                }
            }
            when (tab) {
                0 -> SocialInboxTab()
                1 -> SocialChannelsTab()
                2 -> SocialAnalyticsTab()
            }
        }
    }
}

// ── Inbox Tab ─────────────────────────────────────────────────────────────────
@Composable
fun SocialInboxTab() {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var filterPlatform by remember { mutableStateOf("ALL") }
    var conversations by remember { mutableStateOf(sampleConvs) }
    var messages by remember { mutableStateOf(sampleMsgs.toMutableMap()) }

    if (selectedId != null) {
        val conv = conversations.find { it.id == selectedId }!!
        ChatView(conv, messages[selectedId] ?: emptyList(),
            onBack = { selectedId = null },
            onSend = { text ->
                val newMsg = SocialMsg("m${System.currentTimeMillis()}", "OUTBOUND", "AGENT", text, "TEXT", "Now", false)
                messages = (messages + (selectedId!! to (messages[selectedId] ?: emptyList()) + newMsg)).toMutableMap()
            },
            onSendPayment = { amount, desc ->
                val payMsg = "Hujambo ${conv.customerName.split(" ")[0]}! 🛍️\n\n$desc\n💰 KES ${amount.toLong()}\n\n💳 Lipa Mpesa:\nPaybill: 174379\nKiasi: KES $amount\n\nAsante! 🙏"
                val newMsg = SocialMsg("m${System.currentTimeMillis()}", "OUTBOUND", "AGENT", payMsg, "PAYMENT_REQUEST", "Now", false)
                messages = (messages + (selectedId!! to (messages[selectedId] ?: emptyList()) + newMsg)).toMutableMap()
                conversations = conversations.map { if (it.id == selectedId) it.copy(status = "PENDING_PAYMENT") else it }
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Platform filter chips
        LazyRow(
            modifier = Modifier.background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf("ALL") + PLATFORMS.keys.toList()
            items(filters) { p ->
                val meta    = PLATFORMS[p]
                val isActive = filterPlatform == p
                FilterChip(
                    selected = isActive,
                    onClick  = { filterPlatform = p },
                    label    = { Text(if (meta != null) "${meta.emoji} ${meta.label}" else "All", fontSize = 12.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = (meta?.color ?: B360Green),
                        selectedLabelColor     = Color.White
                    )
                )
            }
        }
        Divider()

        val filtered = conversations.filter { filterPlatform == "ALL" || it.platform == filterPlatform }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { conv ->
                ConversationListItem(conv, onClick = { selectedId = conv.id })
            }
        }
    }
}

@Composable
fun ConversationListItem(conv: SocialConv, onClick: () -> Unit) {
    val p     = PLATFORMS[conv.platform]!!
    val statusColor = when (conv.status) {
        "OPEN"            -> B360Green
        "PENDING_PAYMENT" -> Color(0xFFFF8F00)
        "COMPLETED"       -> Color(0xFF1565C0)
        else              -> Color.Gray
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .background(Color.White).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(p.bg),
                contentAlignment = Alignment.Center
            ) { Text(conv.customerName.first().toString(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
            Text(
                text = p.emoji,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp),
                fontSize = 13.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(conv.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(conv.lastMessageAt, fontSize = 10.sp, color = Color(0xFFAAAAAA))
            }
            Text(conv.lastMessage, fontSize = 12.sp, color = Color(0xFF777777), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                Text(conv.status.replace("_", " "), fontSize = 10.sp, color = Color(0xFFAAAAAA))
                if (conv.isAiHandled) {
                    Box(modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(10.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text("AI", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = B360Green)
                    }
                }
            }
        }

        if (conv.unreadCount > 0) {
            Box(
                modifier = Modifier.size(20.dp).background(p.color, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("${conv.unreadCount}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
    Divider(color = Color(0xFFF5F5F5))
}

// ── Chat View ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatView(
    conv: SocialConv,
    messages: List<SocialMsg>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onSendPayment: (String, String) -> Unit
) {
    val p             = PLATFORMS[conv.platform]!!
    var draft         by remember { mutableStateOf("") }
    var aiSuggestion  by remember { mutableStateOf<String?>(null) }
    var aiLoading     by remember { mutableStateOf(false) }
    var showPaySheet  by remember { mutableStateOf(false) }
    var payAmt        by remember { mutableStateOf("") }
    var payDesc       by remember { mutableStateOf("") }
    val listState     = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun getAiReply() {
        aiLoading = true
        val last = messages.lastOrNull { it.direction == "INBOUND" }?.content ?: ""
        val reply = when {
            last.contains("bei", ignoreCase = true) || last.contains("price", ignoreCase = true) ->
                "Habari! Bei yetu:\n• Unga 2kg - KES 180\n• Unga 5kg - KES 320\n• Debe - KES 1,200\n\nUngependa kuagiza? 😊"
            last.contains("delivery", ignoreCase = true) || last.contains("deliver", ignoreCase = true) ->
                "Ndiyo! Tunafanya delivery Nairobi yote. Delivery fee ni KES 150. Unataka tuwasilishe wapi? 📦"
            last.contains("saa ngapi", ignoreCase = true) || last.contains("open", ignoreCase = true) ->
                "Tunafungua 7:00 asubuhi hadi 9:00 usiku kila siku! 🕖 Je, ungependa kuagiza kitu?"
            last.contains("payment", ignoreCase = true) || last.contains("kulipa", ignoreCase = true) ->
                "Tunakubali M-Pesa (Paybill 174379), Cash, na Card. Unataka kulipa kwa njia gani? 💳"
            else ->
                "Habari ${conv.customerName.split(" ")[0]}! Asante kwa kuwasiliana. Ninawezaje kukusaidia leo? 😊"
        }
        kotlinx.coroutines.GlobalScope.kotlinx.coroutines.launch {
            kotlinx.coroutines.delay(1200)
            aiSuggestion = reply
            aiLoading    = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7F5))) {
        // Header
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(p.bg), contentAlignment = Alignment.Center) {
                        Text(conv.customerName.first().toString(), fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text(conv.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${p.emoji} ${p.label}${conv.customerPhone?.let { " · $it" } ?: ""}", fontSize = 11.sp, color = Color(0xFF888888))
                    }
                }
            },
            actions = {
                TextButton(onClick = { showPaySheet = true }, colors = ButtonDefaults.textButtonColors(contentColor = B360Green)) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pay", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        // Messages
        LazyColumn(
            state     = listState,
            modifier  = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg)
            }
        }

        // AI Suggestion
        aiSuggestion?.let { sug ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = B360Green, modifier = Modifier.size(14.dp))
                        Text("AI Suggested Reply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = B360Green)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(sug, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick  = { draft = sug; aiSuggestion = null },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = B360Green),
                            shape    = RoundedCornerShape(8.dp)
                        ) { Text("Edit", fontSize = 12.sp) }
                        Button(
                            onClick  = { onSend(sug); aiSuggestion = null },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape    = RoundedCornerShape(8.dp)
                        ) { Text("Send", fontSize = 12.sp) }
                    }
                }
            }
        }

        // Compose bar
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(0.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick  = { if (!aiLoading) getAiReply() },
                    modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(10.dp)).size(44.dp)
                ) {
                    if (aiLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = B360Green, strokeWidth = 2.dp)
                    else Icon(Icons.Default.Bolt, contentDescription = "AI Reply", tint = B360Green, modifier = Modifier.size(20.dp))
                }
                OutlinedTextField(
                    value         = draft,
                    onValueChange = { draft = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = { Text("Reply to ${conv.customerName.split(" ")[0]}…", fontSize = 13.sp) },
                    maxLines      = 3,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = B360Green,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                IconButton(
                    onClick  = { if (draft.isNotBlank()) { onSend(draft); draft = "" } },
                    enabled  = draft.isNotBlank(),
                    modifier = Modifier.background(if (draft.isNotBlank()) B360Green else Color(0xFFE0E0E0), CircleShape).size(44.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Payment Bottom Sheet
    if (showPaySheet) {
        Dialog(onDismissRequest = { showPaySheet = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Request Payment", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))

                    Text("Customer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF666666))
                    Spacer(Modifier.height(5.dp))
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.emoji)
                        Text(conv.customerName, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Amount (KES)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF666666))
                    Spacer(Modifier.height(5.dp))
                    OutlinedTextField(value = payAmt, onValueChange = { payAmt = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 850") }, singleLine = true, shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green))

                    Spacer(Modifier.height(10.dp))
                    Text("Description", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF666666))
                    Spacer(Modifier.height(5.dp))
                    OutlinedTextField(value = payDesc, onValueChange = { payDesc = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Unga 2kg × 3") }, singleLine = true, shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = B360Green))

                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showPaySheet = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick  = { onSendPayment(payAmt, payDesc.ifBlank { "Order yako" }); payAmt = ""; payDesc = ""; showPaySheet = false },
                            enabled  = payAmt.isNotBlank(),
                            modifier = Modifier.weight(2f),
                            colors   = ButtonDefaults.buttonColors(containerColor = B360Green),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send + STK Push", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: SocialMsg) {
    val isOut     = msg.direction == "OUTBOUND"
    val isPay     = msg.messageType == "PAYMENT_REQUEST"
    val bubbleBg  = when {
        isPay -> Color(0xFFE8F5E9)
        isOut -> B360Green
        else  -> Color.White
    }
    val textColor = if (isOut && !isPay) Color.White else Color(0xFF1A1A1A)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape    = RoundedCornerShape(
                topStart = if (isOut) 16.dp else 4.dp,
                topEnd   = if (isOut) 4.dp  else 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp
            ),
            colors   = CardDefaults.cardColors(containerColor = bubbleBg),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isPay) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = B360Green, modifier = Modifier.size(13.dp))
                        Text("Payment Request", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = B360Green)
                    }
                }
                Text(msg.content, fontSize = 13.sp, color = textColor, lineHeight = 19.sp)
                Row(modifier = Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (msg.isAiGenerated) {
                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text("AI", fontSize = 8.sp, color = if (isOut) Color.White else B360Green, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(msg.time, fontSize = 10.sp, color = if (isOut) Color.White.copy(0.7f) else Color(0xFFAAAAAA))
                }
            }
        }
    }
}

// ── Channels Tab ──────────────────────────────────────────────────────────────
@Composable
fun SocialChannelsTab() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(PLATFORMS.entries.toList()) { (key, meta) ->
            val isConnected = key != "TIKTOK"
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(if (isConnected) 2.dp else 1.dp, if (isConnected) meta.color else Color(0xFFE0E0E0))) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(48.dp).background(meta.bg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(meta.emoji, fontSize = 26.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meta.label, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text(if (isConnected) "● Connected" else "○ Not connected", fontSize = 12.sp,
                                color = if (isConnected) meta.color else Color(0xFFCCCCCC), fontWeight = FontWeight.SemiBold)
                        }
                        if (isConnected) {
                            Box(modifier = Modifier.background(Color(0xFFE8F5E9), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("Active", color = B360Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = meta.color), shape = RoundedCornerShape(8.dp)) {
                                Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (isConnected) {
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAF8), RoundedCornerShape(8.dp)).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("AI Reply", fontSize = 10.sp, color = Color(0xFF888888))
                                Text("ON", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = B360Green)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Today", fontSize = 10.sp, color = Color(0xFF888888))
                                Text("${(5..20).random()} msgs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Orders", fontSize = 10.sp, color = Color(0xFF888888))
                                Text("${(1..5).random()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Analytics Tab ─────────────────────────────────────────────────────────────
@Composable
fun SocialAnalyticsTab() {
    val stats = listOf(
        Triple("WHATSAPP",  Triple(48, 12, 87600)),
        Triple("INSTAGRAM", Triple(31,  8, 52400)),
        Triple("FACEBOOK",  Triple(19,  4, 28800)),
        Triple("TIKTOK",    Triple(14,  3, 18900)),
    )
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KraKpiCard("Total Convs", "112", "All channels", B360Green, Modifier.weight(1f))
                KraKpiCard("Orders", "27", "From social", Color(0xFF1565C0), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KraKpiCard("Revenue", "KES 188K", "Social orders", Color(0xFF6A1B9A), Modifier.weight(1f))
                KraKpiCard("AI Handled", "74%", "Auto-replied", Color(0xFFE65100), Modifier.weight(1f))
            }
        }
        item { Text("Platform Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        items(stats) { (platform, data) ->
            val (convs, orders, revenue) = data
            val p   = PLATFORMS[platform]!!
            val cvr = (orders.toFloat() / convs * 100).toInt()
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(p.emoji, fontSize = 28.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatChip("$convs convs", Color(0xFF666666))
                            StatChip("$orders orders", p.color)
                            StatChip("KES ${revenue/1000}K", B360Green)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(progress = { cvr / 100f }, modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)), color = p.color, trackColor = Color(0xFFF0F0F0))
                            Text("$cvr%", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = p.color)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
}

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
