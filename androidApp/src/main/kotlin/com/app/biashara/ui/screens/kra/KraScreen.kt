package com.app.biashara.ui.screens.kra

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KraScreen() {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Compliance", "eTIMS", "Returns", "Setup")

    Scaffold(
        containerColor = B360Surface,
        topBar = {
            TopAppBar(
                title = { Text("KRA & eTIMS", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = B360Green,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.White) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }

            val content = when (tab) {
                0 -> "Compliance data unavailable" to
                    "No compliance score or taxpayer details are shown until live KRA data is connected."
                1 -> "No eTIMS invoices loaded" to
                    "Live eTIMS invoice synchronization is not connected in the Android app."
                2 -> "No KRA returns loaded" to
                    "Tax returns will appear after the KRA integration is connected."
                else -> "KRA setup is unavailable here" to
                    "Configure KRA securely in the Biashara360 web application. Android does not store KRA credentials locally."
            }
            KraUnavailable(content.first, content.second)
        }
    }
}

@Composable
private fun KraUnavailable(title: String, message: String) {
    Box(Modifier.fillMaxSize().background(B360Surface).padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = B360Green)
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(message, color = Color(0xFF64748B), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
