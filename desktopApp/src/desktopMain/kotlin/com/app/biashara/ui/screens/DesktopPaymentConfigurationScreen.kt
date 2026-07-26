package com.app.biashara.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.presentation.viewmodel.BusinessViewModel

private val PaymentGreen = Color(0xFF00B874)
private val PaymentNavy = Color(0xFF0F1F3A)
private val PaymentMuted = Color(0xFF64748B)
private val PaymentBorder = Color(0xFFE2E8F0)
private val PaymentSurface = Color(0xFFF8FAFC)

@Composable
fun DesktopPaymentConfigurationScreen(
    viewModel: BusinessViewModel = remember { inject() }
) {
    val mpesa by viewModel.mpesaState.collectAsState()
    val cyberSource by viewModel.cyberSourceState.collectAsState()

    fun refresh() {
        viewModel.loadMpesaConfig()
        viewModel.loadCyberSourceConfig()
    }

    LaunchedEffect(Unit) { refresh() }

    val configuredCount = listOf(mpesa.config, cyberSource.config).count { it != null }
    val productionCount = listOf(
        mpesa.config?.environment,
        cyberSource.config?.environment
    ).count { it.equals("production", ignoreCase = true) }
    val credentialsReady = listOf(
        mpesa.config?.passkeyConfigured == true,
        cyberSource.config?.secretConfigured == true
    ).count { it }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PaymentSurface),
        contentPadding = PaddingValues(28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Payment Configuration", color = PaymentNavy, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Settings", color = PaymentMuted, fontSize = 14.sp)
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        Text("Payment Configuration", color = PaymentMuted, fontSize = 14.sp)
                    }
                }
                Button(
                    onClick = ::refresh,
                    enabled = !mpesa.isLoading && !cyberSource.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PaymentGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
                ) {
                    Icon(Icons.Default.Refresh, "Refresh", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh Config", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryCard(
                    Modifier.weight(1f),
                    "Payment Services",
                    "$configuredCount / 2",
                    "Configured for this business",
                    Icons.Default.AccountBalanceWallet,
                    PaymentGreen,
                    Color(0xFFE1F8EF)
                )
                SummaryCard(
                    Modifier.weight(1f),
                    "Production Services",
                    productionCount.toString(),
                    "Currently using live rails",
                    Icons.Default.VerifiedUser,
                    Color(0xFF2563EB),
                    Color(0xFFE8F1FF)
                )
                SummaryCard(
                    Modifier.weight(1f),
                    "Credentials Ready",
                    "$credentialsReady / 2",
                    "Required secrets configured",
                    Icons.Default.Key,
                    Color(0xFF7C3AED),
                    Color(0xFFF1EAFE)
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFEAF8F2),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFBDEBD8))
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(shape = CircleShape, color = PaymentGreen.copy(alpha = 0.14f)) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            tint = PaymentGreen,
                            modifier = Modifier.padding(9.dp).size(18.dp)
                        )
                    }
                    Column {
                        Text("Managed securely on the web", color = PaymentNavy, fontWeight = FontWeight.Bold)
                        Text(
                            "Desktop settings are read-only and synchronize automatically. Secrets are never downloaded to this device.",
                            color = PaymentMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                PaymentServiceCard(
                    modifier = Modifier.weight(1f),
                    title = "M-Pesa",
                    subtitle = "Safaricom Daraja",
                    icon = Icons.Default.PhoneAndroid,
                    iconColor = PaymentGreen,
                    configured = mpesa.config != null,
                    loading = mpesa.isLoading,
                    error = mpesa.error,
                    rows = listOf(
                        "Business Shortcode" to (mpesa.config?.shortCode ?: "—"),
                        "Account Type" to mpesa.config?.accountType.displayValue(),
                        "Environment" to mpesa.config?.environment.displayValue(),
                        "Passkey" to if (mpesa.config?.passkeyConfigured == true) "Configured" else "Not configured",
                        "Callback URL" to (mpesa.config?.callbackUrl ?: "—")
                    )
                )
                PaymentServiceCard(
                    modifier = Modifier.weight(1f),
                    title = "CyberSource",
                    subtitle = "Card payments",
                    icon = Icons.Default.CreditCard,
                    iconColor = Color(0xFF7C3AED),
                    configured = cyberSource.config != null,
                    loading = cyberSource.isLoading,
                    error = cyberSource.error,
                    rows = listOf(
                        "Merchant ID" to (cyberSource.config?.merchantId ?: "—"),
                        "Active Key ID" to (cyberSource.config?.merchantKeyId ?: "—"),
                        "Environment" to cyberSource.config?.environment.displayValue(),
                        "Shared Secret" to if (cyberSource.config?.secretConfigured == true) "Configured" else "Not configured",
                        "Last Updated" to (cyberSource.config?.updatedAt?.take(19)?.replace('T', ' ') ?: "—")
                    )
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    label: String,
    value: String,
    caption: String,
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color
) {
    Card(
        modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PaymentBorder)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Surface(shape = CircleShape, color = iconBackground) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.padding(13.dp).size(25.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, color = PaymentMuted, fontSize = 13.sp)
                Text(value, color = PaymentNavy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(caption, color = PaymentMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PaymentServiceCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    configured: Boolean,
    loading: Boolean,
    error: String?,
    rows: List<Pair<String, String>>
) {
    Card(
        modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, PaymentBorder)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(11.dp), color = iconColor.copy(alpha = 0.11f)) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.padding(11.dp).size(24.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = PaymentNavy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = PaymentMuted, fontSize = 12.sp)
                }
                StatusPill(configured)
            }
            HorizontalDivider(color = PaymentBorder)

            if (loading) {
                Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PaymentGreen)
                }
            } else {
                rows.forEachIndexed { index, (label, value) ->
                    ConfigRow(label, value)
                    if (index < rows.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Color(0xFFF1F5F9))
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(20.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusPill(configured: Boolean) {
    val foreground = if (configured) PaymentGreen else Color(0xFFD97706)
    val background = if (configured) Color(0xFFDCF8EC) else Color(0xFFFFF3D6)
    Surface(shape = RoundedCornerShape(7.dp), color = background) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(6.dp).background(foreground, CircleShape))
            Text(if (configured) "CONFIGURED" else "NOT SET", color = foreground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ConfigRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PaymentMuted, fontSize = 13.sp)
        Text(value, color = PaymentNavy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun String?.displayValue(): String =
    this?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: "—"
