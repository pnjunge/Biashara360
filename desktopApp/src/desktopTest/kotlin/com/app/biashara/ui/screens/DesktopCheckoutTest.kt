package com.app.biashara.ui.screens

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class DesktopCheckoutTest {
    @Test
    fun `dine in requires table before placement`() {
        assertFailsWith<IllegalArgumentException> { requireDesktopTable(true, "DINE_IN", null) }
        assertFailsWith<IllegalArgumentException> { requireDesktopTable(true, "DINE_IN", "") }
        requireDesktopTable(true, "DINE_IN", "table-1")
        requireDesktopTable(true, "TAKEAWAY", null)
        requireDesktopTable(false, "DINE_IN", null)
    }

    @Test
    fun `mpesa settlement requests STK after close and remains unpaid`() = runBlocking<Unit> {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val response = if (paths.size == 1) {
                assertEquals("MPESA", body["paymentMethod"]?.jsonPrimitive?.content)
                """{"success":true,"data":{"paymentStatus":"PENDING","tabStatus":"AWAITING_PAYMENT"}}"""
            } else {
                assertEquals("tab-1", body["orderId"]?.jsonPrimitive?.content)
                assertEquals("254712345678", body["phoneNumber"]?.jsonPrimitive?.content)
                assertEquals("PAYBILL", body["accountType"]?.jsonPrimitive?.content)
                """{"success":true,"data":{"merchantRequestId":"m","checkoutRequestId":"c","responseCode":"0","responseDescription":"Success","customerMessage":"Accepted"}}"""
            }
            respond(response, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        try {
            val result = settleDesktopTab(client, "tab-1", "MPESA", "0712345678", "PAYBILL", "https://test/v1")
            assertFalse(result.paid)
            assertTrue(result.message.contains("Waiting for payment confirmation"))
            assertEquals(listOf("/v1/hospitality/tabs/tab-1/close", "/v1/payments/initiate"), paths)
        } finally { client.close() }
    }

    @Test
    fun `invalid phone causes no settlement request`() = runBlocking<Unit> {
        val client = HttpClient(MockEngine { error("No HTTP request should be made") })
        try {
            assertFailsWith<IllegalArgumentException> {
                settleDesktopTab(client, "tab-1", "MPESA", "", baseUrl = "https://test/v1")
            }
        } finally { client.close() }
    }

    @Test
    fun `failed STK is not reported as settled`() = runBlocking<Unit> {
        var count = 0
        val client = HttpClient(MockEngine {
            count++
            respond(if (count == 1) """{"success":true,"data":{"paymentStatus":"PENDING"}}"""
                else """{"success":false,"message":"Provider unavailable"}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            val error = assertFailsWith<IllegalStateException> {
                settleDesktopTab(client, "tab-1", "MPESA", "0712345678", baseUrl = "https://test/v1")
            }
            assertEquals("Provider unavailable", error.message)
            assertEquals(2, count)
        } finally { client.close() }
    }

    @Test
    fun `cash settlement does not request STK`() = runBlocking<Unit> {
        var count = 0
        val client = HttpClient(MockEngine {
            count++
            respond("""{"success":true,"data":{"paymentStatus":"PAID","tabStatus":"CLOSED"}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            assertTrue(settleDesktopTab(client, "tab-1", "CASH", "", baseUrl = "https://test/v1").paid)
            assertEquals(1, count)
        } finally { client.close() }
    }
}
