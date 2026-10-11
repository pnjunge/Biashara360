package com.app.biashara.data

import com.app.biashara.data.remote.*
import com.app.biashara.ui.screens.desktopHotelTabs
import com.app.biashara.ui.screens.hotelAmountCents
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class NativeHotelClientTest {
    private fun client(handler: MockRequestHandler) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    @Test fun `hotel staff receives all assigned desktop hotel tabs without room management rights`() {
        val access = NativeAccess(listOf("HOTEL"), listOf("hotel.view", "hotel.reservations", "hotel.frontdesk", "hotel.billing", "hotel.refunds", "hotel.housekeeping", "hotel.reports", "hotel.channels"))
        assertEquals(listOf("Reservations", "Calendar", "Rooms", "Housekeeping", "Reports", "Channels"), desktopHotelTabs(access))
        assertFalse(access.permits("hotel.manage"))
        assertTrue(desktopHotelTabs(access.copy(enabledMenus = listOf("HOSPITALITY"))).isEmpty())
        assertTrue(desktopHotelTabs(access.copy(permissions = listOf("hospitality.view"))).isEmpty())
        assertEquals(listOf("Reservations", "Calendar", "Rooms"), desktopHotelTabs(access.copy(permissions = listOf("hotel.view"))))
    }
    @Test fun `hotel collection converts cents exactly without rounding uncollected fractions`() {
        assertEquals(1L, hotelAmountCents("0.01"))
        assertEquals(123_456L, hotelAmountCents("1234.56"))
        assertEquals(10_000_000_000L, hotelAmountCents("100000000.00"))
        for (input in listOf("", "bad", "-1", "0", "1.001", "999999999999999999999")) assertFails { hotelAmountCents(input) }
    }
    @Test fun `native hotel dashboard loads server room notes and encoded stay range`() = runBlocking {
        val client = client { request ->
            assertEquals("/v1/hotel", request.url.encodedPath)
            assertEquals("2026-10-11", request.url.parameters["arrival"])
            assertEquals("2026-10-15", request.url.parameters["departure"])
            respond("""{"success":true,"data":{"roomTypes":[],"reservations":[],"blocks":[],"rooms":[{"id":"room","typeId":"type","number":"101","floor":"1","housekeepingStatus":"CLEAN","notes":"Inspected","isActive":true,"available":true}]}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        try {
            val room = NativeHotelClient(client).dashboard("2026-10-11", "2026-10-15").rooms.single()
            assertEquals("Inspected", room.notes)
            assertTrue(room.available)
        } finally { client.close() }
    }
    @Test fun `payment retries retain request identity and server cash settlement status`() = runBlocking {
        val ids = mutableListOf<String>()
        val client = client { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/v1/hotel/reservations/stay/payments", request.url.encodedPath)
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            assertEquals(123456L, body["amountCents"]!!.jsonPrimitive.long)
            assertEquals("CASH", body["method"]!!.jsonPrimitive.content)
            ids += body["requestId"]!!.jsonPrimitive.content
            respond("""{"success":true,"data":{"orderId":"order","paymentMethod":"CASH","paymentStatus":"PAID","amountCents":123456}}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        try {
            val api = NativeHotelClient(client)
            val request = NativeHotelPaymentRequest(123456, "CASH", "request-id")
            assertEquals(api.payment("stay", request), api.payment("stay", request))
            assertEquals(listOf("request-id", "request-id"), ids)
        } finally { client.close() }
    }
    @Test fun `native hotel surfaces denied access instead of empty fake data`() = runBlocking {
        val client = client { respond("""{"success":false,"message":"Permission required: hotel.billing"}""", HttpStatusCode.Forbidden, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
        try {
            val failure = assertFails { NativeHotelClient(client).folio("stay") }
            assertEquals("Permission required: hotel.billing", failure.message)
        } finally { client.close() }
    }
    @Test fun `method change cancel targets only selected hotel request`() = runBlocking {
        val client = client { request ->
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("/v1/hotel/reservations/stay/payments/order", request.url.encodedPath)
            respond("""{"success":true,"data":true}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        try { assertTrue(NativeHotelClient(client).cancelPayment("stay", "order")) } finally { client.close() }
    }
    @Test fun `calendar export preserves plain calendar format and enforces response status`() = runBlocking {
        val calendar = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR\r\n"
        val client = client { request ->
            assertEquals("/v1/hotel/rooms/room/calendar", request.url.encodedPath)
            respond(calendar, headers = headersOf(HttpHeaders.ContentType, "text/calendar"))
        }
        try { assertEquals(calendar, NativeHotelClient(client).exportCalendar("room")) } finally { client.close() }
    }
}
