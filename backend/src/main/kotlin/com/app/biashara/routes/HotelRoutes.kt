package com.app.biashara.routes

import com.app.biashara.models.*
import com.app.biashara.services.HotelService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.*
import org.koin.ktor.ext.inject

fun Route.hotelRoutes() {
    val service: HotelService by inject()
    route("/hotel") {
        intercept(ApplicationCallPipeline.Call) {
            val setup = call.request.path().endsWith("/hotel/status") || call.request.path().endsWith("/hotel/enabled")
            if (!setup && (!service.enabled(call.businessId()) || !call.hasAnyMenu("HOTEL"))) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Hotel access is disabled or not assigned")); finish(); return@intercept
            }
            if (!setup) {
                val path = call.request.path(); val method = call.request.httpMethod
                val permission = when {
                    path.endsWith("/report") -> "hotel.reports"
                    path.contains("/calendar") -> "hotel.channels"
                    path.contains("/housekeeping") -> "hotel.housekeeping"
                    path.contains("/folio") || path.contains("/payments") -> "hotel.billing"
                    method == HttpMethod.Get -> "hotel.view"
                    path.contains("/check-in") || path.contains("/check-out") -> "hotel.frontdesk"
                    path.contains("/reservations") -> "hotel.reservations"
                    else -> "hotel.manage"
                }
                if (!call.hasPermission(permission)) { call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Permission required: $permission")); finish() }
            }
        }
        get("/status") { call.respond(ApiResponse(true, data = mapOf("enabled" to service.enabled(call.businessId())))) }
        put("/enabled") {
            if (!call.hasRole("ADMIN", "SUPERADMIN")) return@put call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Admin access required"))
            val enabled = call.receive<Map<String, Boolean>>()["enabled"] ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = "enabled required"))
            call.hotelResult { service.setEnabled(call.businessId(), call.callerUserId(), enabled) }
        }
        fun ApplicationCall.dates(): Pair<String, String> {
            val today = Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
            return (request.queryParameters["arrival"] ?: today.toString()) to (request.queryParameters["departure"] ?: today.plus(30, DateTimeUnit.DAY).toString())
        }
        get { val (a,d) = call.dates(); call.hotelResult { service.dashboard(call.businessId(), call.callerUserId(), a, d) } }
        get("/report") { val (a,d) = call.dates(); call.hotelResult { service.report(call.businessId(), call.callerUserId(), a, d) } }
        post("/room-types") { val r = call.receive<HotelTypeRequest>(); call.hotelResult(HttpStatusCode.Created) { service.saveType(call.businessId(), call.callerUserId(), null, r) } }
        put("/room-types/{id}") { val r = call.receive<HotelTypeRequest>(); call.hotelResult { service.saveType(call.businessId(), call.callerUserId(), call.parameters["id"], r) } }
        post("/rooms") { val r = call.receive<HotelRoomRequest>(); call.hotelResult(HttpStatusCode.Created) { service.saveRoom(call.businessId(), call.callerUserId(), null, r) } }
        put("/rooms/{id}") { val r = call.receive<HotelRoomRequest>(); call.hotelResult { service.saveRoom(call.businessId(), call.callerUserId(), call.parameters["id"], r) } }
        put("/rooms/{id}/housekeeping") { val r = call.receive<HotelHousekeepingRequest>(); call.hotelResult { service.housekeeping(call.businessId(), call.callerUserId(), call.parameters["id"].orEmpty(), r) } }
        post("/blocks") { val r = call.receive<HotelBlockRequest>(); call.hotelResult(HttpStatusCode.Created) { service.block(call.businessId(), call.callerUserId(), r) } }
        delete("/blocks/{id}") { call.hotelResult { service.removeBlock(call.businessId(), call.callerUserId(), call.parameters["id"].orEmpty()) } }
        post("/reservations") { val r = call.receive<HotelReservationRequest>(); call.hotelResult(HttpStatusCode.Created) { service.saveReservation(call.businessId(), call.callerUserId(), null, r) } }
        put("/reservations/{id}") { val r = call.receive<HotelReservationRequest>(); call.hotelResult { service.saveReservation(call.businessId(), call.callerUserId(), call.parameters["id"], r) } }
        for ((path, action) in listOf("check-in" to "CHECK_IN", "check-out" to "CHECK_OUT", "cancel" to "CANCEL", "no-show" to "NO_SHOW")) post("/reservations/{id}/$path") { call.hotelResult { service.transition(call.businessId(), call.callerUserId(), call.parameters["id"].orEmpty(), action) } }
        get("/reservations/{id}/folio") { call.hotelResult { service.folio(call.businessId(), call.callerUserId(), call.parameters["id"].orEmpty()) } }
        post("/reservations/{id}/folio") {
            val r = call.receive<HotelFolioRequest>()
            if (r.kind in setOf("CREDIT", "CASH_REFUND") && !call.hasPermission("hotel.refunds")) return@post call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, message = "Permission required: hotel.refunds"))
            call.hotelResult { service.postEntry(call.businessId(), call.callerUserId(), call.parameters["id"].orEmpty(), r) }
        }
        get("/reservations/{id}/payments") { call.hotelResult { service.paymentRequests(call.businessId(), call.parameters["id"].orEmpty()) } }
        post("/reservations/{id}/payments") { val r = call.receive<HotelPaymentRequest>(); call.hotelResult { service.payment(call.businessId(), call.callerUserId(), call.parameters["id"].orEmpty(), r) } }
        delete("/reservations/{id}/payments/{orderId}") { call.hotelResult { service.cancelPayment(call.businessId(), call.callerUserId(), call.parameters["id"].orEmpty(), call.parameters["orderId"].orEmpty()) } }
        get("/rooms/{id}/calendar") {
            runCatching { service.exportCalendar(call.businessId(), call.parameters["id"].orEmpty()) }.fold({ call.respondText(it, ContentType.parse("text/calendar")) }, { call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = it.message ?: "Calendar request failed")) })
        }
        post("/rooms/{id}/calendar") { val r = call.receive<HotelCalendarImportRequest>(); call.hotelResult { service.importCalendar(call.businessId(), call.callerUserId(), call.parameters["id"].orEmpty(), r) } }
    }
}
private suspend inline fun <reified T> ApplicationCall.hotelResult(status: HttpStatusCode = HttpStatusCode.OK, block: () -> T) {
    try { respond(status, ApiResponse(true, data = block())) }
    catch (e: IllegalArgumentException) { respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = e.message ?: "Hotel request failed")) }
    catch (e: IllegalStateException) { respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, message = e.message ?: "Hotel request failed")) }
}
