package com.app.biashara.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.app.biashara.UserSession
import com.app.biashara.data.remote.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable private data class AlertOrder(val id: String, val orderNumber: String, val createdAt: String)
@Serializable private data class AlertFeed(val merchantName: String, val orders: List<AlertOrder>)
@Serializable private data class LinkRequest(val name: String)
@Serializable private data class LinkedWorkstation(val id: String, val name: String, val merchantName: String, val token: String, val expiresAt: String)
@Serializable private data class RegisteredWorkstation(val id: String, val name: String, val expiresAt: String, val revoked: Boolean)
private object AlertUi { val settings = MutableStateFlow(false) }
@Composable fun WorkstationAlertSettingsButton() {
    TextButton(onClick = { AlertUi.settings.value=true }) { Text("Workstation alerts") }
}
@Composable fun BoxScope.DesktopWorkstationAlerts(client: HttpClient) {
    val user by UserSession.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val showSettings by AlertUi.settings.collectAsState()
    var revision by remember { mutableStateOf(0) }
    var popup by remember { mutableStateOf<List<AlertOrder>>(emptyList()) }
    var merchant by remember { mutableStateOf(WorkstationAlertPreferences.merchant) }
    var status by remember { mutableStateOf<String?>(null) }
    var speechIssue by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(revision) {
        popup=emptyList()
        WorkstationAlertPreferences.connectionActive=false
        val key = WorkstationAlertPreferences.token
        if (key.isBlank() || WorkstationAlertPreferences.endpoint != BASE_URL) return@LaunchedEffect
        while (isActive) {
            try {
                val response = client.get("$BASE_URL/workstation-alerts/feed") { header("X-Workstation-Key",key) }
                if (response.status == HttpStatusCode.Unauthorized) { popup=emptyList(); WorkstationAlertPreferences.connectionActive=false; status="Workstation link expired or revoked. Relink in Workstation alerts."; break }
                val result = response.body<ApiResponse<AlertFeed>>()
                val feed = result.data
                check(result.success && feed != null) { result.message.ifBlank { "Could not load workstation alerts" } }
                WorkstationAlertPreferences.connectionActive=true
                status=null; merchant=feed.merchantName
                val fresh = feed.orders.filter { it.id !in WorkstationAlertPreferences.seen }
                if (fresh.isNotEmpty()) {
                    popup=if (UserSession.currentUser.value == null) (popup+fresh).distinctBy { it.id }.takeLast(100) else emptyList()
                    WorkstationAlertPreferences.seen=WorkstationAlertPreferences.seen+fresh.map { it.id }
                    speechIssue=DesktopAlertSound.play(fresh.size, fresh.first().orderNumber)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { WorkstationAlertPreferences.connectionActive=false; status="Workstation alerts disconnected; retrying automatically." }
            delay(5000)
        }
    }
    val access by NativeAccessSession.access.collectAsState()
    LaunchedEffect(user?.id, access?.enabledMenus) {
        if (user == null || access?.allows("HOSPITALITY") != true || access?.permits("hospitality.view") != true) return@LaunchedEffect
        var known: Set<String>? = null
        while (isActive) {
            try {
                val response=client.get("$BASE_URL/hospitality").body<ApiResponse<JsonObject>>()
                val tickets=response.data?.get("tickets")?.jsonArray.orEmpty().filter { it.jsonObject["status"]?.jsonPrimitive?.content !in setOf("SERVED","CANCELLED") }
                if (response.success) {
                    val ids=tickets.map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet()
                    val fresh=known?.let { ids-it }.orEmpty()
                    if(fresh.isNotEmpty()) speechIssue=DesktopAlertSound.play(fresh.size, tickets.first { it.jsonObject["id"]!!.jsonPrimitive.content in fresh }.jsonObject["orderNumber"]?.jsonPrimitive?.content.orEmpty(), "kitchen order")
                    known=ids
                }
            } catch (e:CancellationException){throw e} catch (_:Exception) { }
            delay(5000)
        }
    }
    if (user == null) {
        Column(Modifier.align(Alignment.BottomEnd).padding(20.dp),horizontalAlignment=Alignment.End) {
            status?.let { Text(it, style=MaterialTheme.typography.bodySmall) }
            WorkstationAlertSettingsButton()
        }
    }
    LaunchedEffect(user?.id) { if(user!=null) popup=emptyList() }
    if (popup.isNotEmpty() && !showSettings && user==null) AlertDialog(
        onDismissRequest={popup=emptyList()}, title={Text("Online orders · $merchant")},
        text={Column(Modifier.heightIn(max=280.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(if(user==null) "Sign in to claim and process these orders." else "Open Portal orders to claim and process these orders.")
            popup.forEach { Text("Order ${it.orderNumber}") }
            speechIssue?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
        }}, confirmButton={TextButton(onClick={popup=emptyList()}){Text("Dismiss")}}
    )
    if(showSettings) {
        var name by remember { mutableStateOf(System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME") ?: "POS workstation") }
        var keyInput by remember { mutableStateOf("") }
        var speech by remember { mutableStateOf(WorkstationAlertPreferences.speech) }
        var sound by remember { mutableStateOf(WorkstationAlertPreferences.sound) }
        var message by remember { mutableStateOf(status) }
        var busy by remember { mutableStateOf(false) }
        var registered by remember { mutableStateOf<List<RegisteredWorkstation>>(emptyList()) }
        val admin = user?.role?.name in setOf("ADMIN","BUSINESS_ADMIN","SUPERADMIN")
        suspend fun loadLinks() {
            if(admin) runCatching { client.get("$BASE_URL/workstation-alerts").body<ApiResponse<List<RegisteredWorkstation>>>() }.getOrNull()?.data?.let { registered=it }
        }
        LaunchedEffect(user?.id) { loadLinks() }
        fun action(block: suspend () -> Unit) { scope.launch { busy=true; try { block() } catch(e:CancellationException){throw e} catch(e:Exception){message=e.message ?: "Could not update workstation"} finally {busy=false} } }
        AlertDialog(onDismissRequest={if(!busy)AlertUi.settings.value=false},title={Text("Workstation alerts")},text={
            Column(Modifier.widthIn(min=320.dp,max=460.dp).heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(if(WorkstationAlertPreferences.token.isBlank()) "This workstation is not linked." else "Linked merchant: ${WorkstationAlertPreferences.merchant}")
                Text("Alerts continue after logout while the desktop app is open. Sign-in is required to handle orders.",style=MaterialTheme.typography.bodySmall)
                Row(verticalAlignment=Alignment.CenterVertically){Switch(sound,{sound=it;WorkstationAlertPreferences.sound=it});Text("Sound alerts")}
                OutlinedTextField(speech,{speech=it.take(200);WorkstationAlertPreferences.speech=speech},label={Text("Spoken alert text")},supportingText={Text("Use {type}, {count} or {order}. Leave blank for tone only.")},modifier=Modifier.fillMaxWidth())
                TextButton(enabled=!busy,onClick={action { message=DesktopAlertSound.play(1,"B360-TEST") ?: "Test alert played" }}) {Text("Test sound")}
                if(admin) {
                    OutlinedTextField(name,{name=it.take(100)},label={Text("Workstation name")},singleLine=true,modifier=Modifier.fillMaxWidth())
                    Button(enabled=!busy && name.isNotBlank(),onClick={action {
                        val result=client.post("$BASE_URL/workstation-alerts"){contentType(ContentType.Application.Json);setBody(LinkRequest(name))}.body<ApiResponse<LinkedWorkstation>>()
                        val link=result.data;check(result.success&&link!=null){result.message.ifBlank{"Could not link workstation"}}
                        WorkstationAlertPreferences.unlink();WorkstationAlertPreferences.token=link.token;WorkstationAlertPreferences.endpoint=BASE_URL;WorkstationAlertPreferences.merchant=link.merchantName
                        merchant=link.merchantName;revision++;message="Workstation linked to ${link.merchantName}";loadLinks()
                    }}){Text("Link to signed-in merchant")}
                    if(WorkstationAlertPreferences.token.isNotBlank()) TextButton(onClick={
                        runCatching { java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(WorkstationAlertPreferences.token),null) }.onSuccess { message="Workstation key copied. Paste it only on a workstation you want to link." }.onFailure { message="Could not access clipboard" }
                    }){Text("Copy workstation key")}
                    registered.filter { !it.revoked }.forEach { device -> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(device.name,Modifier.weight(1f));TextButton(enabled=!busy,onClick={action {
                            val response=client.delete("$BASE_URL/workstation-alerts/${device.id}").body<ApiResponse<Unit>>()
                            check(response.success){response.message};loadLinks();message="Link revoked"
                        }}){Text("Revoke")}
                    }}
                } else Text("Ask a merchant administrator to sign in once to link this workstation, or provide a workstation key.",style=MaterialTheme.typography.bodySmall)
                OutlinedTextField(keyInput,{keyInput=it.trim()},label={Text("Workstation key")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedButton(enabled=!busy&&keyInput.isNotBlank(),onClick={action {
                    val result=client.get("$BASE_URL/workstation-alerts/feed"){header("X-Workstation-Key",keyInput)}.body<ApiResponse<AlertFeed>>()
                    val feed=result.data;check(result.success&&feed!=null){result.message.ifBlank{"Invalid workstation key"}}
                    WorkstationAlertPreferences.unlink();WorkstationAlertPreferences.token=keyInput;WorkstationAlertPreferences.endpoint=BASE_URL;WorkstationAlertPreferences.merchant=feed.merchantName
                    keyInput="";merchant=feed.merchantName;revision++;message="Workstation linked"
                }}){Text("Link with key")}
                if(WorkstationAlertPreferences.token.isNotBlank()) TextButton(enabled=!busy,onClick={WorkstationAlertPreferences.unlink();revision++;merchant="";message="Local workstation link removed"}){Text("Unlink this workstation")}
                message?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
            }
        },confirmButton={TextButton(enabled=!busy,onClick={AlertUi.settings.value=false}){Text("Done")}})
    }
}
