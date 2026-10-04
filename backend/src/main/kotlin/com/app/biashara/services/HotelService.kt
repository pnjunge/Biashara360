package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.math.BigDecimal
import java.math.RoundingMode

class HotelService(private val audit: AuditLogService? = null) {
    private val live = listOf("CONFIRMED", "CHECKED_IN")
    private fun today() = Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
    private fun range(a: String, d: String): Pair<LocalDate, LocalDate> {
        val start = LocalDate.parse(a); val end = LocalDate.parse(d)
        require(end > start && start.daysUntil(end) <= 366) { "Departure must be after arrival, with a stay of at most 366 nights" }
        return start to end
    }
    private fun money(value: Long) { require(value in 1..100_000_000_00L) { "Amount must be positive and at most KES 100 million" } }
    private fun requestId(value: String) { require(runCatching { java.util.UUID.fromString(value) }.isSuccess) { "A UUID requestId is required" } }
    // All hotel mutations serialize on the tenant row; READ COMMITTED sees the preceding booking after waiting.
    private fun <T> mutate(business: String, actor: String, action: String, block: () -> T): T = transaction(Connection.TRANSACTION_READ_COMMITTED) {
        require(BusinessesTable.select { BusinessesTable.id eq business }.forUpdate().singleOrNull() != null) { "Business not found" }
        val result = block()
        if (!action.endsWith("_VIEWED")) {
            val resource = when (result) {
                is HotelFolioResponse -> result.reservation.id
                is HotelReservationResponse -> result.id
                is HotelRoomResponse -> result.id
                is HotelTypeResponse -> result.id
                is HotelPaymentResponse -> result.orderId
                else -> null
            }
            audit?.logEvent(business, actor, action = action, resourceType = "HOTEL", resourceId = resource, details = action)
        }
        result
    }
    fun enabled(business: String) = transaction { BusinessesTable.select { BusinessesTable.id eq business }.single()[BusinessesTable.hotelEnabled] }
    fun setEnabled(business: String, actor: String, enabled: Boolean) = mutate(business, actor, "HOTEL_ENABLED") {
        if (!enabled) require(!HotelReservations.select { (HotelReservations.businessId eq business) and (HotelReservations.status inList live) }.any()) { "Finish or cancel active stays before disabling Hotels" }
        val b = BusinessesTable.select { BusinessesTable.id eq business }.single()
        val menus = b[BusinessesTable.enabledMenus].split(',').filter { it.isNotBlank() }.toMutableSet()
        if (enabled) menus += "HOTEL" else menus -= "HOTEL"
        BusinessesTable.update({ BusinessesTable.id eq business }) { it[hotelEnabled] = enabled; it[enabledMenus] = menus.joinToString(",") }
        enabled
    }
    private fun room(b: String, id: String) = HotelRooms.select { (HotelRooms.businessId eq b) and (HotelRooms.id eq id) }.singleOrNull() ?: error("Room not found")
    private fun reservation(b: String, id: String) = HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.id eq id) }.singleOrNull() ?: error("Reservation not found")
    private fun type(b: String, id: String) = HotelRoomTypes.select { (HotelRoomTypes.businessId eq b) and (HotelRoomTypes.id eq id) }.singleOrNull() ?: error("Room type not found")
    private fun available(b: String, id: String, a: LocalDate, d: LocalDate, excluding: String = ""): Boolean {
        val r = room(b, id)
        return r[HotelRooms.active] && r[HotelRooms.status] != "OUT_OF_SERVICE" && type(b, r[HotelRooms.typeId])[HotelRoomTypes.active] &&
            !HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.roomId eq id) and
                (HotelReservations.id neq excluding) and (HotelReservations.status inList live) and
                (HotelReservations.arrival less d) and (HotelReservations.departure greater a) }.any() &&
            !HotelRoomBlocks.select { (HotelRoomBlocks.businessId eq b) and (HotelRoomBlocks.roomId eq id) and
                (HotelRoomBlocks.arrival less d) and (HotelRoomBlocks.departure greater a) }.any()
    }
    fun saveType(b: String, actor: String, id: String?, r: HotelTypeRequest) = mutate(b, actor, "HOTEL_ROOM_TYPE_SAVED") {
        require(r.name.trim().length in 2..100 && r.description.length <= 2000 && r.capacity in 1..30) { "Enter a room type name, capacity of 1–30, and description up to 2000 characters" }; money(r.nightlyRateCents)
        require(!HotelRoomTypes.select { (HotelRoomTypes.businessId eq b) and (HotelRoomTypes.id neq (id ?: "")) }.any { it[HotelRoomTypes.name].equals(r.name.trim(), true) }) { "Room type already exists" }
        if (id != null) type(b, id)
        if (id != null) require(!HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.status inList live) and (HotelReservations.guests greater r.capacity) }.any { room(b, it[HotelReservations.roomId])[HotelRooms.typeId] == id }) { "The capacity is below the guest count of an active reservation" }
        if (!r.isActive && id != null) require(!HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.status inList live) }.any { room(b, it[HotelReservations.roomId])[HotelRooms.typeId] == id }) { "Room type has active reservations" }
        val key = id ?: generateId()
        fun values(s: org.jetbrains.exposed.sql.statements.UpdateBuilder<*>) { s[HotelRoomTypes.name] = r.name.trim(); s[HotelRoomTypes.description] = r.description.trim(); s[HotelRoomTypes.capacity] = r.capacity; s[HotelRoomTypes.rate] = r.nightlyRateCents; s[HotelRoomTypes.active] = r.isActive }
        if (id == null) HotelRoomTypes.insert { it[HotelRoomTypes.id] = key; it[businessId] = b; values(it) }
        else HotelRoomTypes.update({ HotelRoomTypes.id eq key }) { values(it) }
        typeResponse(type(b, key))
    }
    fun saveRoom(b: String, actor: String, id: String?, r: HotelRoomRequest) = mutate(b, actor, "HOTEL_ROOM_SAVED") {
        require(r.number.trim().length in 1..50 && r.floor.length <= 50) { "Enter a room number up to 50 characters" }; type(b, r.typeId)
        require(!HotelRooms.select { (HotelRooms.businessId eq b) and (HotelRooms.id neq (id ?: "")) }.any { it[HotelRooms.number].equals(r.number.trim(), true) }) { "Room number already exists" }
        if (id != null) {
            val old = room(b, id)
            if (!r.isActive || old[HotelRooms.typeId] != r.typeId) require(!HotelReservations.select { (HotelReservations.roomId eq id) and (HotelReservations.status inList live) }.any()) { "Room has active reservations" }
        }
        val key = id ?: generateId()
        fun values(s: org.jetbrains.exposed.sql.statements.UpdateBuilder<*>) { s[HotelRooms.typeId] = r.typeId; s[HotelRooms.number] = r.number.trim(); s[HotelRooms.floor] = r.floor.trim(); s[HotelRooms.active] = r.isActive; s[HotelRooms.updatedAt] = Clock.System.now() }
        if (id == null) HotelRooms.insert { it[HotelRooms.id] = key; it[businessId] = b; values(it) }
        else HotelRooms.update({ HotelRooms.id eq key }) { values(it) }
        roomResponse(room(b, key), true)
    }
    fun housekeeping(b: String, actor: String, id: String, r: HotelHousekeepingRequest) = mutate(b, actor, "HOTEL_HOUSEKEEPING_UPDATED") {
        room(b, id); require(r.status in setOf("CLEAN", "DIRTY", "IN_PROGRESS", "INSPECTED", "OUT_OF_SERVICE") && r.notes.length <= 2000) { "Invalid housekeeping status or notes" }
        if (r.status == "OUT_OF_SERVICE") require(!HotelReservations.select { (HotelReservations.roomId eq id) and (HotelReservations.status inList live) }.any()) { "Move or cancel active reservations before taking the room out of service" }
        HotelRooms.update({ HotelRooms.id eq id }) { it[status] = r.status; it[notes] = r.notes.trim(); it[updatedAt] = Clock.System.now() }
        roomResponse(room(b, id), true)
    }
    fun saveReservation(b: String, actor: String, id: String?, r: HotelReservationRequest) = mutate(b, actor, "HOTEL_RESERVATION_SAVED") {
        val (a, d) = range(r.arrival, r.departure)
        require(r.guestName.trim().length in 2..160 && r.guestPhone.length <= 20 && r.guestEmail.length <= 160 && r.notes.length <= 4000 && r.reference.length <= 120 && r.source.length in 1..80) { "Invalid guest or reservation details" }
        val rm = room(b, r.roomId); val rt = type(b, rm[HotelRooms.typeId])
        require(r.guests in 1..rt[HotelRoomTypes.capacity]) { "Guest count exceeds the room capacity" }
        val old = id?.let { reservation(b, it) }
        if (old != null) require(!pendingPayments(b, old[HotelReservations.id])) { "Resolve pending payments before changing a stay" }
        require(old == null || old[HotelReservations.status] in live) { "Only active reservations can be edited" }
        if (old == null || old[HotelReservations.status] != "CHECKED_IN") require(a >= today()) { "Arrival cannot be in the past" }
        else {
            require(a == old[HotelReservations.arrival] && d >= today()) { "Keep the checked-in arrival date; departure must be today or later" }
            if (old[HotelReservations.roomId] != r.roomId) require(rm[HotelRooms.status] in setOf("CLEAN", "INSPECTED")) { "The destination room must be clean" }
        }
        require(available(b, r.roomId, a, d, id ?: "")) { "Room is unavailable for those dates" }
        val key = id ?: generateId(); val now = Clock.System.now()
        fun values(s: org.jetbrains.exposed.sql.statements.UpdateBuilder<*>) {
            s[HotelReservations.roomId] = r.roomId; s[HotelReservations.guestName] = r.guestName.trim(); s[HotelReservations.phone] = r.guestPhone.trim(); s[HotelReservations.email] = r.guestEmail.trim()
            s[HotelReservations.guests] = r.guests; s[HotelReservations.arrival] = a; s[HotelReservations.departure] = d; s[HotelReservations.bookingSource] = r.source.trim(); s[HotelReservations.reference] = r.reference.trim(); s[HotelReservations.notes] = r.notes.trim(); s[HotelReservations.updatedAt] = now
        }
        if (id == null) HotelReservations.insert { it[HotelReservations.id] = key; it[businessId] = b; it[rate] = rt[HotelRoomTypes.rate]; it[createdAt] = now; values(it) }
        else {
            HotelReservations.update({ HotelReservations.id eq key }) { values(it) }
            if (old!![HotelReservations.status] == "CHECKED_IN" && old[HotelReservations.roomId] != r.roomId) HotelRooms.update({ HotelRooms.id eq old[HotelReservations.roomId] }) { it[status] = "DIRTY"; it[updatedAt] = now }
        }
        folioInternal(b, key).reservation
    }
    fun transition(b: String, actor: String, id: String, action: String) = mutate(b, actor, "HOTEL_$action") {
        syncPayments(b, id)
        val r = reservation(b, id); val status = r[HotelReservations.status]; val now = Clock.System.now()
        val next = when (action) {
            "CHECK_IN" -> {
                require(status == "CONFIRMED") { "Reservation must be confirmed" }
                require(today() >= r[HotelReservations.arrival] && today() < r[HotelReservations.departure]) { "Check-in must be during the booked stay" }
                val rm = room(b, r[HotelReservations.roomId]); require(rm[HotelRooms.status] in setOf("CLEAN", "INSPECTED")) { "Room must be clean or inspected before check-in" }
                require(!HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.roomId eq r[HotelReservations.roomId]) and (HotelReservations.status eq "CHECKED_IN") }.any()) { "Another guest is still checked in to this room" }
                require(available(b, r[HotelReservations.roomId], r[HotelReservations.arrival], r[HotelReservations.departure], id)) { "Room is unavailable" }
                "CHECKED_IN"
            }
            "CHECK_OUT" -> {
                require(status == "CHECKED_IN") { "Guest is not checked in" }
                require(folioInternal(b, id).balanceCents == 0L) { "Settle the folio balance or refund its credit before checkout" }
                HotelRooms.update({ HotelRooms.id eq r[HotelReservations.roomId] }) { it[HotelRooms.status] = "DIRTY"; it[updatedAt] = now }
                "CHECKED_OUT"
            }
            "CANCEL", "NO_SHOW" -> { require(status == "CONFIRMED") { "Only confirmed reservations can be cancelled or marked no-show" }; if (action == "NO_SHOW") require(today() >= r[HotelReservations.arrival]) { "Arrival date has not been reached" }; if (action == "CANCEL") "CANCELLED" else "NO_SHOW" }
            else -> error("Unknown reservation action")
        }
        if (action != "CHECK_IN") require(!pendingPayments(b, id)) { "Complete or cancel pending payment requests before changing reservation status" }
        HotelReservations.update({ HotelReservations.id eq id }) {
            it[HotelReservations.status] = next; it[updatedAt] = now
            if (next == "CHECKED_IN") it[checkedInAt] = now
            if (next == "CHECKED_OUT") it[checkedOutAt] = now
        }
        folioInternal(b, id)
    }
    private fun cents(amount: Double) = BigDecimal.valueOf(amount).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    private fun pendingPayments(b: String, id: String) = (HotelPaymentRequests innerJoin OrdersTable).select {
        (HotelPaymentRequests.businessId eq b) and (HotelPaymentRequests.reservationId eq id) and (OrdersTable.paymentStatus eq "PENDING")
    }.any()
    private fun syncPayments(b: String, id: String) {
        val requests = HotelPaymentRequests.select { (HotelPaymentRequests.businessId eq b) and (HotelPaymentRequests.reservationId eq id) }.toList()
        requests.forEach { req ->
            val order = OrdersTable.select { OrdersTable.id eq req[HotelPaymentRequests.orderId] }.single()
            if (order[OrdersTable.paymentStatus] == "PAID" && !HotelFolioEntries.select { (HotelFolioEntries.businessId eq b) and (HotelFolioEntries.requestId eq req[HotelPaymentRequests.requestId]) }.any()) {
                val payments = PaymentsTable.select { (PaymentsTable.businessId eq b) and (PaymentsTable.orderId eq order[OrdersTable.id]) and (PaymentsTable.status inList listOf("SUCCESS", "COMPLETED", "PAID")) }.toList()
                // Payment providers may return repeated callbacks; only the settlement recorded on this hotel order is credited.
                val payment = payments.firstOrNull { cents(it[PaymentsTable.amount]) == cents(order[OrdersTable.subtotal]) }
                if (payment != null && !HotelFolioEntries.select { HotelFolioEntries.paymentId eq payment[PaymentsTable.id] }.any()) {
                    HotelFolioEntries.insert {
                        it[HotelFolioEntries.id] = generateId(); it[businessId] = b; it[reservationId] = id; it[kind] = "PAYMENT"; it[description] = "${payment[PaymentsTable.method]} · ${payment[PaymentsTable.transactionCode]}"
                        it[amount] = -cents(payment[PaymentsTable.amount]); it[paymentId] = payment[PaymentsTable.id]; it[requestId] = req[HotelPaymentRequests.requestId]; it[actorId] = order[OrdersTable.serverUserId]!!; it[createdAt] = payment[PaymentsTable.transactionDate]
                    }
                }
            }
        }
    }
    fun folio(b: String, actor: String, id: String) = mutate(b, actor, "HOTEL_FOLIO_VIEWED") { reservation(b, id); syncPayments(b, id); folioInternal(b, id) }
    private fun roomCharge(r: ResultRow) = if (r[HotelReservations.status] in setOf("CANCELLED", "NO_SHOW")) 0L else Math.multiplyExact(r[HotelReservations.rate], r[HotelReservations.arrival].daysUntil(r[HotelReservations.departure]).toLong())
    private fun folioInternal(b: String, id: String): HotelFolioResponse {
        val r = reservation(b, id)
        val entries = HotelFolioEntries.select { (HotelFolioEntries.businessId eq b) and (HotelFolioEntries.reservationId eq id) }.orderBy(HotelFolioEntries.createdAt).toList()
        val roomTotal = roomCharge(r)
        val charges = (roomTotal + entries.filter { it[HotelFolioEntries.kind] in setOf("CHARGE", "CREDIT") }.sumOf { it[HotelFolioEntries.amount] }).coerceAtLeast(0)
        val paid = -entries.filter { it[HotelFolioEntries.kind] in setOf("PAYMENT", "CASH_REFUND") }.sumOf { it[HotelFolioEntries.amount] }
        return HotelFolioResponse(reservationResponse(r, charges - paid), entries.map { HotelFolioEntryResponse(it[HotelFolioEntries.id], it[HotelFolioEntries.kind], it[HotelFolioEntries.description], it[HotelFolioEntries.amount], it[HotelFolioEntries.paymentId], it[HotelFolioEntries.orderId], it[HotelFolioEntries.createdAt].toString()) }, roomTotal, charges, paid, charges - paid)
    }
    fun postEntry(b: String, actor: String, id: String, r: HotelFolioRequest) = mutate(b, actor, "HOTEL_FOLIO_ENTRY") {
        reservation(b, id); requestId(r.requestId); money(r.amountCents)
        val duplicate = HotelFolioEntries.select { (HotelFolioEntries.businessId eq b) and (HotelFolioEntries.requestId eq r.requestId) }.singleOrNull()
        if (duplicate != null) { require(duplicate[HotelFolioEntries.reservationId] == id && duplicate[HotelFolioEntries.kind] == r.kind && kotlin.math.abs(duplicate[HotelFolioEntries.amount]) == r.amountCents && duplicate[HotelFolioEntries.description] == r.description.trim()) { "requestId was already used for another entry" }; return@mutate folioInternal(b, id) }
        require(!HotelPaymentRequests.select { (HotelPaymentRequests.businessId eq b) and (HotelPaymentRequests.requestId eq r.requestId) }.any()) { "requestId is already used for a payment request" }
        syncPayments(b, id)
        require(r.description.trim().length in 2..255 && r.kind in setOf("CHARGE", "CREDIT", "CASH_REFUND")) { "Choose charge, credit, or cash refund and supply a description" }
        val reservation = reservation(b, id); val folio = folioInternal(b, id)
        require(reservation[HotelReservations.status] != "CHECKED_OUT") { "Checked-out folios are closed" }
        if (r.kind == "CHARGE") require(reservation[HotelReservations.status] in live) { "Cannot add charges to a cancelled stay" }
        if (r.kind == "CREDIT") require(r.amountCents <= folio.totalChargeCents) { "Credit cannot exceed the total charges" }
        if (r.kind == "CASH_REFUND") { require(!pendingPayments(b, id)) { "Resolve pending payments before refunding" }; require(folio.balanceCents < 0 && r.amountCents <= -folio.balanceCents) { "Refund cannot exceed the guest's credit balance" } }
        require(r.paymentId == null && r.orderId == null) { "Use hotel payment requests for payments" }
        val refundPaymentId = if (r.kind == "CASH_REFUND") generateId() else null
        if (refundPaymentId != null) PaymentsTable.insert {
            it[PaymentsTable.id] = refundPaymentId; it[businessId] = b; it[transactionCode] = "HRF-${refundPaymentId.replace("-", "")}"; it[PaymentsTable.amount] = r.amountCents / 100.0
            it[payerName] = reservation[HotelReservations.guestName]; it[payerPhone] = reservation[HotelReservations.phone]
            it[method] = "CASH"; it[status] = "REFUNDED"; it[channel] = "HOTEL_REFUND"; it[reconciled] = true; it[notes] = r.description.trim(); it[transactionDate] = Clock.System.now()
        }
        HotelFolioEntries.insert {
            it[HotelFolioEntries.id] = generateId(); it[businessId] = b; it[reservationId] = id; it[kind] = r.kind; it[description] = r.description.trim()
            it[amount] = if (r.kind == "CREDIT") -r.amountCents else r.amountCents; it[paymentId] = refundPaymentId; it[requestId] = r.requestId; it[actorId] = actor; it[createdAt] = Clock.System.now()
        }
        folioInternal(b, id)
    }
    fun payment(b: String, actor: String, id: String, r: HotelPaymentRequest) = mutate(b, actor, "HOTEL_PAYMENT_REQUESTED") {
        val stay = reservation(b, id); requestId(r.requestId); money(r.amountCents)
        require(r.method in setOf("CASH", "MPESA", "CARD")) { "Choose Cash, M-Pesa or Card" }
        require(r.method != "MPESA" || r.amountCents % 100L == 0L) { "M-Pesa payments must use whole KES amounts; use Cash or Card for cents" }
        val existing = HotelPaymentRequests.select { (HotelPaymentRequests.businessId eq b) and (HotelPaymentRequests.requestId eq r.requestId) }.singleOrNull()
        if (existing != null) {
            require(existing[HotelPaymentRequests.reservationId] == id) { "requestId belongs to another reservation" }
            val o = OrdersTable.select { OrdersTable.id eq existing[HotelPaymentRequests.orderId] }.single()
            require(cents(o[OrdersTable.subtotal]) == r.amountCents && o[OrdersTable.paymentMethod] == r.method) { "requestId was already used for another payment" }
            return@mutate HotelPaymentResponse(o[OrdersTable.id], r.method, o[OrdersTable.paymentStatus], r.amountCents)
        }
        require(!HotelFolioEntries.select { (HotelFolioEntries.businessId eq b) and (HotelFolioEntries.requestId eq r.requestId) }.any()) { "requestId is already used for a folio entry" }
        syncPayments(b, id); require(stay[HotelReservations.status] in live) { "Payments require an active reservation" }
        require(!pendingPayments(b, id)) { "Complete or cancel the existing payment request first" }
        require(r.amountCents <= folioInternal(b, id).balanceCents) { "Payment cannot exceed the outstanding balance" }
        val key = generateId(); val now = Clock.System.now(); val number = "HTL-${key.replace("-", "").take(12).uppercase()}"; val amount = r.amountCents / 100.0
        OrdersTable.insert {
            it[OrdersTable.id] = key; it[orderNumber] = number; it[businessId] = b; it[clientReference] = "hotel:${r.requestId}"; it[customerName] = stay[HotelReservations.guestName]; it[customerPhone] = stay[HotelReservations.phone]
            it[paymentStatus] = if (r.method == "CASH") "PAID" else "PENDING"; it[paymentMethod] = r.method; it[deliveryStatus] = "DELIVERED"; it[serviceType] = "HOTEL"; it[serverUserId] = actor
            it[baseAmount] = amount; it[subtotal] = amount; it[notes] = "Hotel reservation $id"; it[createdAt] = now; it[updatedAt] = now
        }
        HotelPaymentRequests.insert { it[HotelPaymentRequests.id] = generateId(); it[businessId] = b; it[reservationId] = id; it[orderId] = key; it[requestId] = r.requestId }
        if (r.method == "CASH") PaymentsTable.insert {
            it[PaymentsTable.id] = generateId(); it[businessId] = b; it[orderId] = key; it[transactionCode] = "CASH-$number"; it[PaymentsTable.amount] = amount
            it[payerPhone] = stay[HotelReservations.phone]; it[payerName] = stay[HotelReservations.guestName]; it[method] = "CASH"; it[status] = "SUCCESS"; it[channel] = "HOTEL"; it[reconciled] = true; it[transactionDate] = now
        }
        syncPayments(b, id)
        HotelPaymentResponse(key, r.method, if (r.method == "CASH") "PAID" else "PENDING", r.amountCents)
    }
    fun cancelPayment(b: String, actor: String, id: String, orderId: String) = mutate(b, actor, "HOTEL_PAYMENT_CANCELLED") {
        reservation(b, id)
        require(HotelPaymentRequests.select { (HotelPaymentRequests.businessId eq b) and (HotelPaymentRequests.reservationId eq id) and (HotelPaymentRequests.orderId eq orderId) }.any()) { "Payment request not found" }
        val o = OrdersTable.select { OrdersTable.id eq orderId }.forUpdate().single()
        require(o[OrdersTable.paymentStatus] == "PENDING" && o[OrdersTable.stkCheckoutRequestId] == null && !PaymentsTable.select { PaymentsTable.orderId eq orderId }.any() && !CyberSourceTransactionsTable.select { CyberSourceTransactionsTable.orderId eq orderId }.any()) { "Only an uninitiated payment request may be cancelled; resolve initiated payments with the payment provider" }
        OrdersTable.update({ OrdersTable.id eq orderId }) { it[paymentStatus] = "CANCELLED"; it[updatedAt] = Clock.System.now() }; true
    }
    fun paymentRequests(b: String, id: String) = transaction {
        reservation(b, id)
        (HotelPaymentRequests innerJoin OrdersTable).select { (HotelPaymentRequests.businessId eq b) and (HotelPaymentRequests.reservationId eq id) }.map { HotelPaymentResponse(it[OrdersTable.id], it[OrdersTable.paymentMethod], it[OrdersTable.paymentStatus], cents(it[OrdersTable.subtotal])) }
    }
    fun block(b: String, actor: String, r: HotelBlockRequest) = mutate(b, actor, "HOTEL_ROOM_BLOCKED") {
        val (a, d) = range(r.arrival, r.departure); room(b, r.roomId); require(r.reason.trim().length in 2..255) { "Supply a reason up to 255 characters" }
        require(!HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.roomId eq r.roomId) and (HotelReservations.status inList live) and (HotelReservations.arrival less d) and (HotelReservations.departure greater a) }.any()) { "Block overlaps an active reservation" }
        val id = generateId(); HotelRoomBlocks.insert { it[HotelRoomBlocks.id] = id; it[businessId] = b; it[roomId] = r.roomId; it[arrival] = a; it[departure] = d; it[reason] = r.reason.trim() }; id
    }
    fun removeBlock(b: String, actor: String, id: String) = mutate(b, actor, "HOTEL_ROOM_BLOCK_REMOVED") { require(HotelRoomBlocks.deleteWhere { (HotelRoomBlocks.businessId eq b) and (HotelRoomBlocks.id eq id) } == 1) { "Block not found" }; true }
    fun dashboard(b: String, actor: String, a: String, d: String) = mutate(b, actor, "HOTEL_DASHBOARD_VIEWED") {
        val (start, end) = range(a, d)
        val stays = HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.arrival less end) and (HotelReservations.departure greater start) or ((HotelReservations.businessId eq b) and (HotelReservations.status eq "CHECKED_IN")) }.orderBy(HotelReservations.arrival).toList()
        stays.forEach { syncPayments(b, it[HotelReservations.id]) }
        HotelDashboard(HotelRoomTypes.select { HotelRoomTypes.businessId eq b }.map(::typeResponse), HotelRooms.select { HotelRooms.businessId eq b }.orderBy(HotelRooms.number).map { roomResponse(it, available(b, it[HotelRooms.id], start, end)) }, stays.map { folioInternal(b, it[HotelReservations.id]).reservation }, HotelRoomBlocks.select { (HotelRoomBlocks.businessId eq b) and (HotelRoomBlocks.arrival less end) and (HotelRoomBlocks.departure greater start) }.map { HotelBlockResponse(it[HotelRoomBlocks.id], it[HotelRoomBlocks.roomId], it[HotelRoomBlocks.arrival].toString(), it[HotelRoomBlocks.departure].toString(), it[HotelRoomBlocks.reason], it[HotelRoomBlocks.bookingSource]) })
    }
    fun report(b: String, actor: String, a: String, d: String) = mutate(b, actor, "HOTEL_REPORT_VIEWED") {
        val (start, end) = range(a, d)
        val rooms = HotelRooms.select { (HotelRooms.businessId eq b) and (HotelRooms.active eq true) and (HotelRooms.status neq "OUT_OF_SERVICE") }.filter { type(b, it[HotelRooms.typeId])[HotelRoomTypes.active] }.map { it[HotelRooms.id] }
        val stays = HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.status inList listOf("CONFIRMED", "CHECKED_IN", "CHECKED_OUT")) }.toList()
        var booked = 0; var revenue = 0L
        stays.forEach { r -> val nights = maxOf(start, r[HotelReservations.arrival]).daysUntil(minOf(end, r[HotelReservations.departure])).coerceAtLeast(0); booked += nights; revenue += nights * r[HotelReservations.rate]; syncPayments(b, r[HotelReservations.id]) }
        val blocks = HotelRoomBlocks.select { (HotelRoomBlocks.businessId eq b) and (HotelRoomBlocks.arrival less end) and (HotelRoomBlocks.departure greater start) }.toList()
        val days = generateSequence(start) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it < end }.toList()
        val available = rooms.sumOf { roomId -> days.count { day -> blocks.none { it[HotelRoomBlocks.roomId] == roomId && it[HotelRoomBlocks.arrival] <= day && it[HotelRoomBlocks.departure] > day } } }
        val allStays = HotelReservations.select { HotelReservations.businessId eq b }.toList()
        allStays.forEach { syncPayments(b, it[HotelReservations.id]) }
        val balances = allStays.map { folioInternal(b, it[HotelReservations.id]).balanceCents }
        val zone = TimeZone.of("Africa/Nairobi")
        val collected = -HotelFolioEntries.select { (HotelFolioEntries.businessId eq b) and (HotelFolioEntries.kind inList listOf("PAYMENT", "CASH_REFUND")) and (HotelFolioEntries.createdAt greaterEq start.atStartOfDayIn(zone)) and (HotelFolioEntries.createdAt less end.atStartOfDayIn(zone)) }.sumOf { it[HotelFolioEntries.amount] }
        HotelReportResponse(a, d, available, booked, if (available == 0) 0.0 else booked * 100.0 / available, revenue, if (booked == 0) 0 else revenue / booked, if (available == 0) 0 else revenue / available, balances.sumOf { it.coerceAtLeast(0) }, stays.count { it[HotelReservations.arrival] >= start && it[HotelReservations.arrival] < end }, stays.count { it[HotelReservations.departure] >= start && it[HotelReservations.departure] < end }, collected, -balances.filter { it < 0 }.sum())
    }
    private fun typeResponse(r: ResultRow) = HotelTypeResponse(r[HotelRoomTypes.id], r[HotelRoomTypes.name], r[HotelRoomTypes.description], r[HotelRoomTypes.capacity], r[HotelRoomTypes.rate], r[HotelRoomTypes.active])
    private fun roomResponse(r: ResultRow, free: Boolean) = HotelRoomResponse(r[HotelRooms.id], r[HotelRooms.typeId], r[HotelRooms.number], r[HotelRooms.floor], r[HotelRooms.status], r[HotelRooms.notes], r[HotelRooms.active], free)
    private fun reservationResponse(r: ResultRow, balance: Long) = HotelReservationResponse(r[HotelReservations.id], r[HotelReservations.roomId], r[HotelReservations.guestName], r[HotelReservations.phone], r[HotelReservations.email], r[HotelReservations.guests], r[HotelReservations.arrival].toString(), r[HotelReservations.departure].toString(), r[HotelReservations.rate], r[HotelReservations.status], r[HotelReservations.bookingSource], r[HotelReservations.reference], r[HotelReservations.notes], r[HotelReservations.checkedInAt]?.toString(), r[HotelReservations.checkedOutAt]?.toString(), balance)
    fun exportCalendar(b: String, id: String) = transaction {
        room(b, id)
        val events = HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.roomId eq id) and (HotelReservations.status inList live) }.map { Triple(it[HotelReservations.id], it[HotelReservations.arrival], it[HotelReservations.departure]) } + HotelRoomBlocks.select { (HotelRoomBlocks.businessId eq b) and (HotelRoomBlocks.roomId eq id) }.map { Triple(it[HotelRoomBlocks.id], it[HotelRoomBlocks.arrival], it[HotelRoomBlocks.departure]) }
        "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Biashara360//Hotel//EN\r\nCALSCALE:GREGORIAN\r\n" + events.joinToString("") { (uid, a, d) -> "BEGIN:VEVENT\r\nUID:$uid@biashara360\r\nDTSTAMP:${Clock.System.now().toString().substringBefore('.').replace("-", "").replace(":", "").let { if (it.endsWith("Z")) it else it + "Z" }}\r\nDTSTART;VALUE=DATE:${a.toString().replace("-", "")}\r\nDTEND;VALUE=DATE:${d.toString().replace("-", "")}\r\nSUMMARY:Unavailable\r\nEND:VEVENT\r\n" } + "END:VCALENDAR\r\n"
    }
    fun importCalendar(b: String, actor: String, id: String, r: HotelCalendarImportRequest) = mutate(b, actor, "HOTEL_CALENDAR_IMPORTED") {
        room(b, id); require(r.source.trim().length in 2..80 && r.source.trim() != "MANUAL" && r.calendar.length <= 1_000_000) { "Supply a channel name and calendar up to 1 MB" }
        val events = HotelCalendar.parse(r.calendar)
        require(events.size <= 1000) { "Calendar has more than 1000 events" }
        events.forEach { event -> require(!HotelReservations.select { (HotelReservations.businessId eq b) and (HotelReservations.roomId eq id) and (HotelReservations.status inList live) and (HotelReservations.arrival less event.departure) and (HotelReservations.departure greater event.arrival) }.any()) { "Calendar conflicts with an existing reservation; resolve it before importing" } }
        HotelRoomBlocks.deleteWhere { (HotelRoomBlocks.businessId eq b) and (HotelRoomBlocks.roomId eq id) and (HotelRoomBlocks.bookingSource eq r.source.trim()) }
        events.forEach { e -> HotelRoomBlocks.insert { it[HotelRoomBlocks.id] = generateId(); it[businessId] = b; it[roomId] = id; it[arrival] = e.arrival; it[departure] = e.departure; it[reason] = "External booking"; it[bookingSource] = r.source.trim(); it[externalUid] = e.uid } }
        events.size
    }
}
