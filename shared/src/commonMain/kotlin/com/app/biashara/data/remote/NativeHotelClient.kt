package com.app.biashara.data.remote

import kotlinx.serialization.Serializable
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType

@Serializable data class NativeHotelTypeRequest(val name: String, val description: String = "", val capacity: Int = 2, val nightlyRateCents: Long, val isActive: Boolean = true)
@Serializable data class NativeHotelTypeResponse(val id: String, val name: String, val description: String, val capacity: Int, val nightlyRateCents: Long, val isActive: Boolean)
@Serializable data class NativeHotelRoomRequest(val typeId: String, val number: String, val floor: String = "", val isActive: Boolean = true)
@Serializable data class NativeHotelRoomResponse(val id: String, val typeId: String, val number: String, val floor: String, val housekeepingStatus: String, val notes: String, val isActive: Boolean, val available: Boolean)
@Serializable data class NativeHotelHousekeepingRequest(val status: String, val notes: String = "")
@Serializable data class NativeHotelReservationRequest(val roomId: String, val guestName: String, val guestPhone: String = "", val guestEmail: String = "", val guests: Int = 1, val arrival: String, val departure: String, val source: String = "DIRECT", val reference: String = "", val notes: String = "")
@Serializable data class NativeHotelReservationResponse(val id: String, val roomId: String, val guestName: String, val guestPhone: String, val guestEmail: String, val guests: Int, val arrival: String, val departure: String, val nightlyRateCents: Long, val status: String, val source: String, val reference: String, val notes: String, val checkedInAt: String?, val checkedOutAt: String?, val balanceCents: Long)
@Serializable data class NativeHotelFolioRequest(val kind: String, val description: String = "", val amountCents: Long = 0, val paymentId: String? = null, val orderId: String? = null, val requestId: String)
@Serializable data class NativeHotelFolioEntryResponse(val id: String, val kind: String, val description: String, val amountCents: Long, val paymentId: String?, val orderId: String?, val createdAt: String)
@Serializable data class NativeHotelFolioResponse(val reservation: NativeHotelReservationResponse, val entries: List<NativeHotelFolioEntryResponse>, val roomChargeCents: Long, val totalChargeCents: Long, val totalPaidCents: Long, val balanceCents: Long)
@Serializable data class NativeHotelBlockRequest(val roomId: String, val arrival: String, val departure: String, val reason: String)
@Serializable data class NativeHotelBlockResponse(val id: String, val roomId: String, val arrival: String, val departure: String, val reason: String, val source: String)
@Serializable data class NativeHotelCalendarImportRequest(val source: String, val calendar: String)
@Serializable data class NativeHotelPaymentRequest(val amountCents: Long, val method: String, val requestId: String)
@Serializable data class NativeHotelPaymentResponse(val orderId: String, val paymentMethod: String, val paymentStatus: String, val amountCents: Long)
@Serializable data class NativeHotelDashboard(val roomTypes: List<NativeHotelTypeResponse>, val rooms: List<NativeHotelRoomResponse>, val reservations: List<NativeHotelReservationResponse>, val blocks: List<NativeHotelBlockResponse>)
@Serializable data class NativeHotelReportResponse(val startDate: String, val endDate: String, val roomNightsAvailable: Int, val roomNightsBooked: Int, val occupancyPercent: Double, val accommodationRevenueCents: Long, val averageDailyRateCents: Long, val revParCents: Long, val outstandingBalanceCents: Long, val arrivals: Int, val departures: Int, val collectedCents: Long, val refundableCreditCents: Long)

/** Online hotel workflow shared by native clients; mutations remain server-authoritative. */
class NativeHotelClient(private val client: io.ktor.client.HttpClient) {
    private fun encodeBody(body: Any): kotlinx.serialization.json.JsonElement = when (body) {
        is NativeHotelReservationRequest -> kotlinx.serialization.json.Json.encodeToJsonElement(NativeHotelReservationRequest.serializer(), body)
        is NativeHotelTypeRequest -> kotlinx.serialization.json.Json.encodeToJsonElement(NativeHotelTypeRequest.serializer(), body)
        is NativeHotelRoomRequest -> kotlinx.serialization.json.Json.encodeToJsonElement(NativeHotelRoomRequest.serializer(), body)
        is NativeHotelHousekeepingRequest -> kotlinx.serialization.json.Json.encodeToJsonElement(NativeHotelHousekeepingRequest.serializer(), body)
        is NativeHotelPaymentRequest -> kotlinx.serialization.json.Json.encodeToJsonElement(NativeHotelPaymentRequest.serializer(), body)
        is NativeHotelFolioRequest -> kotlinx.serialization.json.Json.encodeToJsonElement(NativeHotelFolioRequest.serializer(), body)
        is NativeHotelBlockRequest -> kotlinx.serialization.json.Json.encodeToJsonElement(NativeHotelBlockRequest.serializer(), body)
        is NativeHotelCalendarImportRequest -> kotlinx.serialization.json.Json.encodeToJsonElement(NativeHotelCalendarImportRequest.serializer(), body)
        else -> error("Unsupported hotel request")
    }
    private suspend inline fun <reified T> request(path: String, method: io.ktor.http.HttpMethod = io.ktor.http.HttpMethod.Get, body: Any? = null): T {
        val response: ApiResponse<T> = client.request("$BASE_URL/hotel$path") {
            this.method = method
            if (body != null) { contentType(io.ktor.http.ContentType.Application.Json); setBody(encodeBody(body)) }
        }.body()
        check(response.success) { response.message.ifBlank { "Hotel request failed" } }
        return requireNotNull(response.data) { "Hotel response was empty" }
    }
    suspend fun dashboard(arrival: String, departure: String): NativeHotelDashboard = request("?arrival=$arrival&departure=$departure")
    suspend fun folio(id: String): NativeHotelFolioResponse = request("/reservations/$id/folio")
    suspend fun payments(id: String): List<NativeHotelPaymentResponse> = request("/reservations/$id/payments")
    suspend fun transition(id: String, action: String): NativeHotelFolioResponse = request("/reservations/$id/$action", io.ktor.http.HttpMethod.Post)
    suspend fun saveReservation(id: String?, body: NativeHotelReservationRequest): NativeHotelReservationResponse = request("/reservations${id?.let { "/$it" }.orEmpty()}", if (id == null) io.ktor.http.HttpMethod.Post else io.ktor.http.HttpMethod.Put, body)
    suspend fun saveType(id: String?, body: NativeHotelTypeRequest): NativeHotelTypeResponse = request("/room-types${id?.let { "/$it" }.orEmpty()}", if (id == null) io.ktor.http.HttpMethod.Post else io.ktor.http.HttpMethod.Put, body)
    suspend fun saveRoom(id: String?, body: NativeHotelRoomRequest): NativeHotelRoomResponse = request("/rooms${id?.let { "/$it" }.orEmpty()}", if (id == null) io.ktor.http.HttpMethod.Post else io.ktor.http.HttpMethod.Put, body)
    suspend fun housekeeping(id: String, body: NativeHotelHousekeepingRequest): NativeHotelRoomResponse = request("/rooms/$id/housekeeping", io.ktor.http.HttpMethod.Put, body)
    suspend fun payment(id: String, body: NativeHotelPaymentRequest): NativeHotelPaymentResponse = request("/reservations/$id/payments", io.ktor.http.HttpMethod.Post, body)
    suspend fun entry(id: String, body: NativeHotelFolioRequest): NativeHotelFolioResponse = request("/reservations/$id/folio", io.ktor.http.HttpMethod.Post, body)
    suspend fun cancelPayment(id: String, order: String): Boolean = request("/reservations/$id/payments/$order", io.ktor.http.HttpMethod.Delete)
    suspend fun report(arrival: String, departure: String): NativeHotelReportResponse = request("/report?arrival=$arrival&departure=$departure")
    suspend fun block(body: NativeHotelBlockRequest): String = request("/blocks", io.ktor.http.HttpMethod.Post, body)
    suspend fun removeBlock(id: String): Boolean = request("/blocks/$id", io.ktor.http.HttpMethod.Delete)
    suspend fun importCalendar(room: String, body: NativeHotelCalendarImportRequest): Int = request("/rooms/$room/calendar", io.ktor.http.HttpMethod.Post, body)
    suspend fun exportCalendar(room: String): String {
        val result = client.get("$BASE_URL/hotel/rooms/$room/calendar")
        check(result.status.value in 200..299) { "Calendar export failed" }
        return result.bodyAsText()
    }
}
