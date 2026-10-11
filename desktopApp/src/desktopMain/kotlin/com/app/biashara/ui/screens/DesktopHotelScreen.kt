package com.app.biashara.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biashara.UserSession
import com.app.biashara.data.remote.*
import com.app.biashara.ui.theme.B360Green
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import java.math.BigDecimal
import java.util.UUID
import javax.swing.JFileChooser

internal fun hotelAmountCents(value: String): Long {
    val cents = BigDecimal(value.trim()).movePointRight(2).longValueExact()
    require(cents > 0) { "Enter a positive amount with at most two decimal places" }
    return cents
}
private fun hotelMoney(cents: Long) = "KES ${"%,.2f".format(java.util.Locale.US, cents / 100.0)}"
private fun hotelToday() = Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
internal fun desktopHotelTabs(access: NativeAccess): List<String> = buildList {
    if (!access.allows("HOTEL") || !access.permits("hotel.view")) return@buildList
    addAll(listOf("Reservations", "Calendar", "Rooms"))
    if (access.permits("hotel.housekeeping")) add("Housekeeping")
    if (access.permits("hotel.reports")) add("Reports")
    if (access.permits("hotel.channels")) add("Channels")
}

@Composable
fun DesktopHotelScreen(client: HttpClient = remember { inject() }) {
    val access by NativeAccessSession.access.collectAsState()
    val api = remember(client) { NativeHotelClient(client) }
    val scope = rememberCoroutineScope()
    val tabs = desktopHotelTabs(access ?: NativeAccess())
    fun can(key: String) = access?.permits("hotel.$key") == true
    var tab by remember { mutableStateOf("Reservations") }
    var dates by remember { mutableStateOf(DesktopDateRange(hotelToday().toString(), hotelToday().plus(30, DateTimeUnit.DAY).toString())) }
    var data by remember { mutableStateOf<NativeHotelDashboard?>(null) }
    var report by remember { mutableStateOf<NativeHotelReportResponse?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var modal by remember { mutableStateOf<String?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var selectedStay by remember { mutableStateOf<NativeHotelReservationResponse?>(null) }
    var selectedRoom by remember { mutableStateOf<NativeHotelRoomResponse?>(null) }
    var action by remember { mutableStateOf("") }
    var folio by remember { mutableStateOf<NativeHotelFolioResponse?>(null) }
    var payments by remember { mutableStateOf<List<NativeHotelPaymentResponse>>(emptyList()) }
    val draft = remember { mutableStateMapOf<String, String>() }
    suspend fun load() {
        require(dates.start.isNotBlank() && dates.end.isNotBlank()) { "Select start and departure dates" }
        val start = LocalDate.parse(dates.start); val end = LocalDate.parse(dates.end)
        require(end > start) { "Select an end date after the start date (departure date is excluded)" }
        data = api.dashboard(start.toString(), end.toString())
        if (tab == "Reports" && can("reports")) report = api.report(start.toString(), end.toString())
    }
    fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null; message = null
        scope.launch { try { block() } catch (e: Exception) { error = e.message ?: "Hotel request failed" } finally { busy = false } }
    }
    fun form(kind: String, values: Map<String, String> = emptyMap(), id: String? = null) {
        draft.clear(); draft.putAll(values); editingId = id; error = null; modal = kind
    }
    fun editStay(stay: NativeHotelReservationResponse? = null) {
        selectedStay = stay
        form("Reservation", mapOf("room" to (stay?.roomId ?: data?.rooms?.firstOrNull()?.id.orEmpty()),
            "name" to stay?.guestName.orEmpty(), "phone" to stay?.guestPhone.orEmpty(), "email" to stay?.guestEmail.orEmpty(),
            "guests" to (stay?.guests ?: 1).toString(), "arrival" to (stay?.arrival ?: hotelToday().toString()),
            "departure" to (stay?.departure ?: hotelToday().plus(1, DateTimeUnit.DAY).toString()),
            "source" to (stay?.source ?: "DIRECT"), "reference" to stay?.reference.orEmpty(), "notes" to stay?.notes.orEmpty()), stay?.id)
    }
    fun openFolio(stay: NativeHotelReservationResponse) {
        selectedStay = stay
        run { folio = api.folio(stay.id); payments = api.payments(stay.id); error = null; modal = "Folio" }
    }
    fun confirm(stay: NativeHotelReservationResponse, next: String) { selectedStay = stay; action = next; error = null; modal = "Confirm" }
    LaunchedEffect(Unit) { run { load() } }
    LaunchedEffect(tabs) { if (tab !in tabs && tabs.isNotEmpty()) tab = tabs.first() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Hotel & Accommodation", fontSize = 26.sp, fontWeight = FontWeight.Bold); Text("Rooms, guest stays, check-in, payments and housekeeping", color = Color(0xFF64748B)) }
            OutlinedButton(onClick = { run { load() } }, enabled = !busy) { Text("Refresh") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it, color = B360Green) }
        if (tabs.isEmpty()) Text("Hotel view access is required.")
        else {
            ScrollableTabRow(selectedTabIndex = tabs.indexOf(tab).coerceAtLeast(0), edgePadding = 0.dp) {
                tabs.forEach { name -> Tab(selected = tab == name, onClick = { tab = name; if (name == "Reports") run { load() } }, text = { Text(name) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DesktopDateFilter(dates) { dates = it }
                Button(onClick = { run { load() } }, enabled = !busy) { Text("Apply dates") }
            }
            Text("End date is the departure date and is excluded from the stay.", fontSize = 12.sp, color = Color(0xFF64748B))
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(search, { search = it }, label = { Text("Search guest, room or reference") }, singleLine = true, modifier = Modifier.weight(1f))
                if (tab == "Reservations" && can("reservations")) Button(onClick = { editStay() }, enabled = !busy) { Text("New Reservation") }
                if (tab == "Rooms" && can("manage")) {
                    OutlinedButton(onClick = { form("Room Type", mapOf("capacity" to "2", "rate" to "", "active" to "true")) }) { Text("Add Room Type") }
                    Button(onClick = { form("Room", mapOf("type" to data?.roomTypes?.firstOrNull()?.id.orEmpty(), "active" to "true")) }) { Text("Add Room") }
                }
                if (tab == "Calendar" && can("manage")) Button(onClick = { form("Block Dates", mapOf("room" to data?.rooms?.firstOrNull()?.id.orEmpty(), "arrival" to dates.start, "departure" to dates.end)) }) { Text("Block Room") }
            }
            val dashboard = data
            if (dashboard == null && !busy) Text("No hotel data loaded. Use Refresh to try again.")
            else if (dashboard != null) DesktopScrollColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                fun roomName(id: String) = dashboard.rooms.firstOrNull { it.id == id }?.number ?: id
                val filteredStays = dashboard.reservations.filter { search.isBlank() || "${it.guestName} ${it.reference} ${roomName(it.roomId)}".contains(search, true) }
                when (tab) {
                    "Reservations" -> {
                        if (filteredStays.isEmpty()) Text("No reservations in the selected date range.")
                        filteredStays.forEach { stay -> HotelCard {
                            Text("${stay.guestName} · Room ${roomName(stay.roomId)}", fontWeight = FontWeight.Bold)
                            Text("${stay.arrival} → ${stay.departure} · ${stay.guests} guests · ${stay.status.replace('_', ' ')}")
                            Text("Balance: ${hotelMoney(stay.balanceCents)} · ${stay.reference}")
                            HotelActions {
                                if (can("billing")) OutlinedButton(onClick = { openFolio(stay) }, enabled = !busy) { Text("Guest Folio") }
                                if (can("reservations") && stay.status in listOf("CONFIRMED", "CHECKED_IN")) OutlinedButton(onClick = { editStay(stay) }, enabled = !busy) { Text("Edit / Move / Extend") }
                                if (can("frontdesk") && stay.status in listOf("CONFIRMED", "CHECKED_IN")) Button(onClick = {
                                    if (can("billing")) openFolio(stay) else confirm(stay, if (stay.status == "CONFIRMED") "check-in" else "check-out")
                                }, enabled = !busy) { Text(if (stay.status == "CONFIRMED") "Check In / Payment" else "Check Out") }
                                if (can("reservations") && stay.status == "CONFIRMED") {
                                    TextButton(onClick = { confirm(stay, "cancel") }) { Text("Cancel") }
                                    TextButton(onClick = { confirm(stay, "no-show") }) { Text("No Show") }
                                }
                            }
                        } }
                    }
                    "Calendar" -> {
                        dashboard.rooms.filter { search.isBlank() || it.number.contains(search, true) }.forEach { room -> HotelCard {
                            Text("Room ${room.number} · ${if (room.available) "Available" else "Booked / blocked"}", fontWeight = FontWeight.Bold)
                            filteredStays.filter { it.roomId == room.id && it.status in listOf("CONFIRMED", "CHECKED_IN") }.forEach { Text("${it.arrival} → ${it.departure} · ${it.guestName} · ${it.status}") }
                            dashboard.blocks.filter { it.roomId == room.id }.forEach { block ->
                                Text("${block.arrival} → ${block.departure} · ${block.source}: ${block.reason}")
                                if (can("manage") && block.source == "MANUAL") TextButton(onClick = { run { api.removeBlock(block.id); load() } }, enabled = !busy) { Text("Remove Block") }
                            }
                        } }
                    }
                    "Rooms", "Housekeeping" -> {
                        dashboard.rooms.filter { search.isBlank() || "${it.number} ${it.floor}".contains(search, true) }.forEach { room -> HotelCard {
                            val type = dashboard.roomTypes.firstOrNull { it.id == room.typeId }
                            Text("Room ${room.number} · ${type?.name.orEmpty()}", fontWeight = FontWeight.Bold)
                            Text("Floor ${room.floor} · ${room.housekeepingStatus} · ${if (room.isActive) "Active" else "Inactive"} · ${hotelMoney(type?.nightlyRateCents ?: 0)} / night")
                            if (room.notes.isNotBlank()) Text(room.notes)
                            HotelActions {
                                if (can("manage")) OutlinedButton(onClick = { form("Room", mapOf("number" to room.number, "floor" to room.floor, "type" to room.typeId, "active" to room.isActive.toString()), room.id) }) { Text("Edit Room") }
                                if (can("housekeeping")) Button(onClick = { selectedRoom = room; form("Housekeeping", mapOf("status" to room.housekeepingStatus, "notes" to room.notes)) }) { Text("Update Housekeeping") }
                            }
                        } }
                        if (tab == "Rooms" && can("manage")) dashboard.roomTypes.forEach { type -> HotelCard {
                            Text("${type.name} · ${type.capacity} guests · ${hotelMoney(type.nightlyRateCents)} / night")
                            TextButton(onClick = { form("Room Type", mapOf("name" to type.name, "description" to type.description, "capacity" to type.capacity.toString(), "rate" to BigDecimal(type.nightlyRateCents).movePointLeft(2).toPlainString(), "active" to type.isActive.toString()), type.id) }) { Text("Edit Room Type") }
                        } }
                    }
                    "Reports" -> report?.let { r -> HotelCard {
                        Text("Hotel report · ${r.startDate} → ${r.endDate}", fontWeight = FontWeight.Bold)
                        listOf("Occupancy" to "${"%.1f".format(r.occupancyPercent)}%", "Room nights available" to r.roomNightsAvailable.toString(), "Booked nights" to r.roomNightsBooked.toString(),
                            "Accommodation revenue" to hotelMoney(r.accommodationRevenueCents), "Collected payments" to hotelMoney(r.collectedCents),
                            "Average nightly rate" to hotelMoney(r.averageDailyRateCents), "Revenue per available room" to hotelMoney(r.revParCents),
                            "Outstanding balance" to hotelMoney(r.outstandingBalanceCents), "Refundable credit" to hotelMoney(r.refundableCreditCents), "Arrivals" to r.arrivals.toString(), "Departures" to r.departures.toString()).forEach { (label, value) -> Text("$label: $value") }
                    } } ?: Text("Apply the selected dates to load the report.")
                    "Channels" -> {
                        Text("Import channel availability as iCalendar, or export a room calendar.")
                        dashboard.rooms.forEach { room -> HotelCard {
                            Text("Room ${room.number}", fontWeight = FontWeight.Bold)
                            HotelActions {
                                Button(onClick = { selectedRoom = room; form("Import Calendar", mapOf("source" to "", "calendar" to "")) }) { Text("Import Calendar") }
                                OutlinedButton(onClick = { run { saveHotelDocument("Room-${room.number}.ics", api.exportCalendar(room.id)) } }, enabled = !busy) { Text("Export Calendar") }
                            }
                        } }
                    }
                }
            }
        }
    }
    if (modal == "Folio" && folio != null) DesktopHotelFolio(client, api, requireNotNull(folio), payments, ::can, onClose = { modal = null }, onUpdated = { run { load() } })
    else if (modal != null) HotelDialog(requireNotNull(modal), busy, error, onClose = { if (!busy) modal = null }, onSave = {
        run {
            fun value(key: String) = draft[key].orEmpty().trim()
            when (modal) {
                "Reservation" -> api.saveReservation(editingId, NativeHotelReservationRequest(value("room"), value("name"), value("phone"), value("email"), value("guests").toInt(), value("arrival"), value("departure"), value("source"), value("reference"), value("notes")))
                "Room Type" -> api.saveType(editingId, NativeHotelTypeRequest(value("name"), value("description"), value("capacity").toInt(), hotelAmountCents(value("rate")), value("active") == "true"))
                "Room" -> api.saveRoom(editingId, NativeHotelRoomRequest(value("type"), value("number"), value("floor"), value("active") == "true"))
                "Housekeeping" -> api.housekeeping(requireNotNull(selectedRoom).id, NativeHotelHousekeepingRequest(value("status"), value("notes")))
                "Block Dates" -> api.block(NativeHotelBlockRequest(value("room"), value("arrival"), value("departure"), value("reason")))
                "Import Calendar" -> api.importCalendar(requireNotNull(selectedRoom).id, NativeHotelCalendarImportRequest(value("source"), value("calendar")))
                "Confirm" -> api.transition(requireNotNull(selectedStay).id, action)
            }
            modal = null; load(); message = "Hotel records updated."
        }
    }) {
        val rooms = data?.rooms.orEmpty().map { it.id to "Room ${it.number}" }
        val types = data?.roomTypes.orEmpty().map { it.id to it.name }
        @Composable fun input(key: String, label: String, multiline: Boolean = false) { HotelInput(label, draft[key].orEmpty(), { draft[key] = it }, !busy, multiline) }
        @Composable fun choice(key: String, label: String, choices: List<Pair<String, String>>) { HotelChoice(label, draft[key].orEmpty(), choices, !busy) { draft[key] = it } }
        @Composable fun stayDates() {
            Text("Arrival / departure (exclusive)")
            DesktopDateFilter(DesktopDateRange(draft["arrival"].orEmpty(), draft["departure"].orEmpty())) { draft["arrival"] = it.start; draft["departure"] = it.end }
        }
        when (modal) {
            "Reservation" -> { choice("room", "Room", rooms); input("name", "Guest name"); input("phone", "Phone"); input("email", "Email"); input("guests", "Guests"); stayDates(); input("source", "Booking source"); input("reference", "Reference"); input("notes", "Notes", true) }
            "Room Type" -> { input("name", "Name"); input("description", "Description", true); input("capacity", "Capacity"); input("rate", "Nightly rate (KES)"); choice("active", "Status", listOf("true" to "Active", "false" to "Inactive")) }
            "Room" -> { input("number", "Room number"); input("floor", "Floor"); choice("type", "Room type", types); choice("active", "Status", listOf("true" to "Active", "false" to "Inactive")) }
            "Housekeeping" -> { choice("status", "Status", listOf("CLEAN", "DIRTY", "IN_PROGRESS", "INSPECTED", "OUT_OF_SERVICE").map { it to it.replace('_', ' ') }); input("notes", "Notes", true) }
            "Block Dates" -> { choice("room", "Room", rooms); stayDates(); input("reason", "Reason") }
            "Import Calendar" -> { input("source", "Channel name"); input("calendar", "Paste iCalendar content", true) }
            "Confirm" -> { Text("${action.replace('-', ' ')} · ${selectedStay?.guestName}"); Text(if (action == "check-out") "The guest folio must be fully settled. Checkout marks the room dirty." else if (action in listOf("cancel", "no-show")) "Accommodation charges will be removed and any payment becomes refundable guest credit." else "Confirm the guest's arrival.") }
        }
    }
}

@Composable private fun HotelCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable private fun HotelActions(content: @Composable FlowRowScope.() -> Unit) { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content) }
@Composable private fun HotelInput(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean = true, multiline: Boolean = false) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = enabled, singleLine = !multiline, minLines = if (multiline) 3 else 1, maxLines = if (multiline) 7 else 1, modifier = Modifier.fillMaxWidth())
}
@Composable private fun HotelChoice(label: String, value: String, choices: List<Pair<String, String>>, enabled: Boolean = true, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("$label: ${choices.firstOrNull { it.first == value }?.second ?: "Select"}") }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) { choices.forEach { (key, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onChange(key); expanded = false }) } }
    }
}
@Composable private fun HotelDialog(title: String, busy: Boolean, error: String?, onClose: () -> Unit, onSave: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = onClose, shape = RoundedCornerShape(24.dp), containerColor = Color.White,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Column(Modifier.width(640.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            DesktopScrollColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        } },
        confirmButton = { if (onSave != null) Button(onClick = onSave, enabled = !busy) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text("Close") } })
}
private fun saveHotelDocument(name: String, text: String) {
    val picker = JFileChooser().apply { selectedFile = java.io.File(name) }
    if (picker.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) picker.selectedFile.writeText(text)
}

@Composable
private fun DesktopHotelFolio(
    client: HttpClient, api: NativeHotelClient, initial: NativeHotelFolioResponse,
    initialPayments: List<NativeHotelPaymentResponse>, can: (String) -> Boolean,
    onClose: () -> Unit, onUpdated: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var folio by remember { mutableStateOf(initial) }
    var payments by remember { mutableStateOf(initialPayments) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var method by remember { mutableStateOf("CASH") }
    var amount by remember { mutableStateOf(if (initial.balanceCents > 0) BigDecimal(initial.balanceCents).movePointLeft(2).toPlainString() else "") }
    var phone by remember { mutableStateOf(initial.reservation.guestPhone) }
    var kind by remember { mutableStateOf("CHARGE") }
    var description by remember { mutableStateOf("") }
    var entryAmount by remember { mutableStateOf("") }
    var paymentRequestId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var entryRequestId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    val pending = payments.any { it.paymentStatus == "PENDING" }
    val id = initial.reservation.id
    suspend fun refresh() { folio = api.folio(id); payments = api.payments(id) }
    suspend fun mpesa(order: String) {
        val result: ApiResponse<kotlinx.serialization.json.JsonElement> = client.post("$BASE_URL/payments/initiate") {
            contentType(ContentType.Application.Json); setBody(mapOf("orderId" to order, "phoneNumber" to phone))
        }.body()
        check(result.success) { result.message.ifBlank { "M-Pesa request could not be sent; retry from Payment Requests" } }
    }
    fun run(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null; message = null
        scope.launch { try { block() } catch (e: Exception) { error = e.message ?: "Hotel request failed" } finally { busy = false } }
    }
    HotelDialog("Guest Folio · ${folio.reservation.guestName}", busy, error, onClose = { if (!busy) onClose() }, onSave = null) {
        val stay = folio.reservation
        Text("${stay.arrival} → ${stay.departure} · ${stay.status.replace('_', ' ')}")
        Text("Charges: ${hotelMoney(folio.totalChargeCents)} · Paid: ${hotelMoney(folio.totalPaidCents)}", fontWeight = FontWeight.Bold)
        Text("Balance: ${hotelMoney(folio.balanceCents)}", color = B360Green, fontWeight = FontWeight.Bold)
        message?.let { Text(it, color = B360Green) }
        HotelActions {
            OutlinedButton(onClick = { run { refresh(); onUpdated() } }, enabled = !busy) { Text("Refresh Payment Status") }
            OutlinedButton(onClick = { run {
                fun cell(s: String) = "\"${s.replace("\"", "\"\"")}\""
                val rows = listOf("Guest,${cell(stay.guestName)}", "Stay,${stay.arrival} to ${stay.departure}", "Description,Kind,Amount (KES)",
                    "Accommodation,CHARGE,${BigDecimal(folio.roomChargeCents).movePointLeft(2)}") + folio.entries.map { "${cell(it.description)},${it.kind},${BigDecimal(it.amountCents).movePointLeft(2)}" } + "Balance,,${BigDecimal(folio.balanceCents).movePointLeft(2)}"
                saveHotelDocument("Guest-Folio-$id.csv", rows.joinToString("\n"))
            } }, enabled = !busy) { Text("Save Folio") }
            if (can("frontdesk") && stay.status == "CONFIRMED") Button(onClick = { run {
                api.transition(id, "check-in"); onUpdated(); onClose()
            } }, enabled = !busy) { Text("Complete Check-In") }
            if (can("frontdesk") && stay.status == "CHECKED_IN") Button(onClick = { run {
                api.transition(id, "check-out"); onUpdated(); onClose()
            } }, enabled = !busy && folio.balanceCents == 0L && !pending) { Text("Complete Checkout") }
        }
        if (stay.status == "CONFIRMED") Text("Collect the full balance or a deposit before check-in, or collect payment later. Pending electronic requests have not been paid.")
        if (stay.status == "CHECKED_IN" && (folio.balanceCents != 0L || pending)) Text("Settle the balance, refund any guest credit and resolve pending requests before checkout.")
        if (can("billing") && stay.status in listOf("CONFIRMED", "CHECKED_IN")) {
            Text("Collect Payment / Deposit", fontWeight = FontWeight.Bold)
            if (pending) Text("A payment is pending. Complete it or change an uninitiated request below before collecting another payment.")
            HotelChoice("Payment method", method, listOf("CASH" to "Cash", "MPESA" to "M-Pesa", "CARD" to "Card"), !busy) { method = it; paymentRequestId = UUID.randomUUID().toString() }
            HotelInput("Amount (KES)", amount, { amount = it; paymentRequestId = UUID.randomUUID().toString() }, !busy)
            if (method == "MPESA" || payments.any { it.paymentMethod == "MPESA" && it.paymentStatus == "PENDING" }) HotelInput("M-Pesa phone", phone, { phone = it }, !busy)
            Button(onClick = { run {
                val result = api.payment(id, NativeHotelPaymentRequest(hotelAmountCents(amount), method, paymentRequestId))
                paymentRequestId = UUID.randomUUID().toString(); amount = ""
                refresh(); onUpdated()
                if (result.paymentMethod == "MPESA" && result.paymentStatus == "PENDING") mpesa(result.orderId)
                message = if (result.paymentStatus == "PAID") "Cash payment recorded." else "Payment request created. Confirm provider settlement using Refresh Payment Status."
            } }, enabled = !busy && !pending && amount.isNotBlank() && folio.balanceCents > 0) {
                Text(if (method == "CASH") "Record Cash Received" else "Request Payment")
            }
        }
        if (payments.isNotEmpty()) {
            Text("Payment Requests", fontWeight = FontWeight.Bold)
            payments.forEach { payment -> HotelCard {
                Text("${payment.paymentMethod} · ${hotelMoney(payment.amountCents)} · ${payment.paymentStatus}")
                if (payment.paymentStatus == "PENDING") {
                    var replacement by remember(payment.orderId) { mutableStateOf(if (payment.paymentMethod == "CASH") "MPESA" else "CASH") }
                    HotelChoice("New payment method", replacement, listOf("CASH", "MPESA", "CARD").filter { it != payment.paymentMethod }.map { it to it }, !busy) { replacement = it }
                    HotelActions {
                        OutlinedButton(onClick = { run {
                            api.cancelPayment(id, payment.orderId)
                            method = replacement; amount = BigDecimal(payment.amountCents).movePointLeft(2).toPlainString(); paymentRequestId = UUID.randomUUID().toString()
                            refresh(); message = "Ready to collect by $method. Confirm receipt or send the new request using the payment form above."
                        } }, enabled = !busy) { Text("Change Payment Method") }
                        OutlinedButton(onClick = { run { api.cancelPayment(id, payment.orderId); refresh(); onUpdated() } }, enabled = !busy) { Text("Cancel Uninitiated Request") }
                        if (payment.paymentMethod == "MPESA") Button(onClick = { run { mpesa(payment.orderId); message = "M-Pesa prompt sent. Refresh after the guest pays." } }, enabled = !busy) { Text("Send M-Pesa Prompt") }
                        if (payment.paymentMethod == "CARD") Button(onClick = { openExternalUrl("https://biashara360.co.ke/pay/card?orderId=${payment.orderId}&businessId=${UserSession.getBusinessId()}") }, enabled = !busy) { Text("Open Card Checkout") }
                    }
                    Text("Requests already sent to a provider must be resolved before their method can change.", fontSize = 12.sp)
                }
            } }
        }
        Text("Folio Entries", fontWeight = FontWeight.Bold)
        Text("Accommodation · ${hotelMoney(folio.roomChargeCents)}")
        folio.entries.forEach { Text("${it.kind} · ${it.description} · ${hotelMoney(it.amountCents)}") }
        if (can("billing") && stay.status != "CHECKED_OUT") {
            Text("Charges & Adjustments", fontWeight = FontWeight.Bold)
            HotelChoice("Entry type", kind, buildList {
                add("CHARGE" to "Additional charge")
                if (can("refunds")) { add("CREDIT" to "Credit / discount"); add("CASH_REFUND" to "Cash refund paid to guest") }
            }, !busy) { kind = it; entryRequestId = UUID.randomUUID().toString() }
            HotelInput("Description", description, { description = it; entryRequestId = UUID.randomUUID().toString() }, !busy)
            HotelInput("Amount (KES)", entryAmount, { entryAmount = it; entryRequestId = UUID.randomUUID().toString() }, !busy)
            if (kind == "CASH_REFUND") Text("Records cash actually returned to the guest; it does not reverse a provider transaction.")
            Button(onClick = { run {
                api.entry(id, NativeHotelFolioRequest(kind, description, hotelAmountCents(entryAmount), requestId = entryRequestId))
                entryRequestId = UUID.randomUUID().toString(); entryAmount = ""; description = ""; refresh(); onUpdated()
            } }, enabled = !busy && entryAmount.isNotBlank() && description.trim().length >= 2) { Text("Post Entry") }
        }
    }
}
