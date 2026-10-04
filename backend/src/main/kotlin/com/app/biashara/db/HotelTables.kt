package com.app.biashara.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object HotelRoomTypes : Table("hotel_room_types") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val name = varchar("name", 100)
    val description = text("description").default("")
    val capacity = integer("capacity")
    val rate = long("nightly_rate_cents")
    val active = bool("is_active").default(true)
    override val primaryKey = PrimaryKey(id)
}
object HotelRooms : Table("hotel_rooms") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val typeId = varchar("type_id", 36).references(HotelRoomTypes.id)
    val number = varchar("room_number", 50)
    val floor = varchar("floor", 50).default("")
    val status = varchar("housekeeping_status", 30).default("CLEAN")
    val notes = text("notes").default("")
    val active = bool("is_active").default(true)
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(businessId, number) }
}
object HotelReservations : Table("hotel_reservations") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val roomId = varchar("room_id", 36).references(HotelRooms.id)
    val guestName = varchar("guest_name", 160)
    val phone = varchar("guest_phone", 30).default("")
    val email = varchar("guest_email", 160).default("")
    val guests = integer("guests")
    val arrival = date("arrival")
    val departure = date("departure")
    val rate = long("nightly_rate_cents")
    val status = varchar("status", 30).default("CONFIRMED")
    val bookingSource = varchar("source", 80).default("DIRECT")
    val reference = varchar("reference", 120).default("")
    val notes = text("notes").default("")
    val checkedInAt = timestamp("checked_in_at").nullable()
    val checkedOutAt = timestamp("checked_out_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
    init { index(false, businessId, roomId, arrival, departure) }
}
object HotelFolioEntries : Table("hotel_folio_entries") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val reservationId = varchar("reservation_id", 36).references(HotelReservations.id)
    val kind = varchar("kind", 30)
    val description = varchar("description", 255)
    val amount = long("amount_cents")
    val paymentId = varchar("payment_id", 36).references(PaymentsTable.id).nullable().uniqueIndex()
    val orderId = varchar("order_id", 36).references(OrdersTable.id).nullable().uniqueIndex()
    val requestId = varchar("request_id", 36)
    val actorId = varchar("actor_id", 36).references(UsersTable.id)
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(businessId, requestId) }
}
object HotelRoomBlocks : Table("hotel_room_blocks") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val roomId = varchar("room_id", 36).references(HotelRooms.id)
    val arrival = date("arrival")
    val departure = date("departure")
    val reason = varchar("reason", 255)
    val bookingSource = varchar("source", 80).default("MANUAL")
    val externalUid = varchar("external_uid", 255).default("")
    override val primaryKey = PrimaryKey(id)
}
object HotelPaymentRequests : Table("hotel_payment_requests") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val reservationId = varchar("reservation_id", 36).references(HotelReservations.id)
    val orderId = varchar("order_id", 36).references(OrdersTable.id).uniqueIndex()
    val requestId = varchar("request_id", 36)
    override val primaryKey = PrimaryKey(id)
    init { uniqueIndex(businessId, requestId) }
}
