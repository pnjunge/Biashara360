package com.app.biashara.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.app.biashara.UserSession
import com.app.biashara.data.offline.*
import com.app.biashara.data.remote.*
import com.app.biashara.db.Biashara360Database
import com.app.biashara.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import kotlin.test.*

class OfflineWorkflowTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var store: OfflineStore
    private var available = true
    private lateinit var client: HttpClient
    private val json = Json
    @BeforeTest fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Biashara360Database.Schema.create(driver)
        store = OfflineStore(Biashara360Database(driver), driver)
        NativeOffline.store = store
        UserSession.setUser(User("staff", "user@test", "", "Staff", UserRole.STAFF, "business", Clock.System.now()))
        NativeAccessSession.set(NativeAccess(listOf("POS", "HOSPITALITY", "OPEN_TABS"), listOf("orders.create", "hospitality.orders", "hospitality.kitchen", "hospitality.billing")))
        available = true
        client = HttpClient(OfflineEngine(MockEngine { request ->
            if (!available) throw java.io.IOException("offline")
            val text = when (request.url.encodedPath.substringAfter("/v1")) {
                "/products" -> """{"success":true,"data":[{"id":"meal","name":"Meal","buyingPrice":10,"sellingPrice":100,"category":"Meals","currentStock":100}]}"""
                "/hospitality/status" -> """{"success":true,"data":{"enabled":true,"shiftOpen":true}}"""
                "/hospitality" -> """{"success":true,"data":{"tables":[],"openTabs":[],"tickets":[]}}"""
                else -> """{"success":true,"data":{"id":"remote-order"}}"""
            }
            respond(text, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }, store))
    }
    @AfterTest fun cleanup() { client.close(); driver.close(); UserSession.clearUser(); NativeOffline.store = null }
    private suspend fun prime() {
        for (path in listOf("products", "hospitality/status", "hospitality")) client.get("$BASE_URL/$path").bodyAsText()
    }
    private suspend fun create(path: String = "hospitality/orders") = client.post("$BASE_URL/$path") {
        setBody(TextContent("""{"clientReference":"local-order","serviceType":"TAKEAWAY","paymentMethod":"TAB","customerName":"Guest","items":[{"productId":"meal","quantity":1,"unitPrice":100}]}""", ContentType.Application.Json))
    }.bodyAsText()
    @Test fun offlineTabAndKitchenSurviveReopeningStore() = runBlocking<Unit> {
        prime(); available = false
        val response = json.parseToJsonElement(create()).jsonObject
        assertTrue(response["success"]!!.jsonPrimitive.boolean)
        assertEquals("local-order", response["data"]!!.jsonObject["id"]!!.jsonPrimitive.content)
        val reopened = OfflineStore(Biashara360Database(driver), driver)
        assertEquals(1, reopened.queued(NativeOffline.owner()!!).size)
        val dashboard = json.parseToJsonElement(client.get("$BASE_URL/hospitality").bodyAsText()).jsonObject["data"]!!.jsonObject
        assertEquals(1, dashboard["openTabs"]!!.jsonArray.size)
        assertEquals("PREPARING", dashboard["tickets"]!!.jsonArray.single().jsonObject["status"]!!.jsonPrimitive.content)
    }
    @Test fun cashSettlementRequiresServedKitchenAndQueuesInOrder() = runBlocking<Unit> {
        prime(); available = false; create()
        client.patch("$BASE_URL/hospitality/tickets/offline-ticket-local-order") {
            setBody(TextContent("""{"status":"SERVED"}""", ContentType.Application.Json))
        }.bodyAsText()
        val result = client.post("$BASE_URL/hospitality/tabs/local-order/close") {
            setBody(TextContent("""{"paymentMethod":"CASH"}""", ContentType.Application.Json))
        }.bodyAsText()
        assertEquals("PAID", json.parseToJsonElement(result).jsonObject["data"]!!.jsonObject["paymentStatus"]!!.jsonPrimitive.content)
        assertEquals(3, store.queued(NativeOffline.owner()!!).size)
        val dashboard = json.parseToJsonElement(client.get("$BASE_URL/hospitality").bodyAsText()).jsonObject["data"]!!.jsonObject
        assertTrue(dashboard["openTabs"]!!.jsonArray.isEmpty())
    }
    @Test fun settlementBeforeServingIsRejectedWithoutAddingAnotherAction() = runBlocking<Unit> {
        prime(); available = false; create()
        assertFailsWith<IllegalStateException> {
            client.post("$BASE_URL/hospitality/tabs/local-order/close") { setBody(TextContent("""{"paymentMethod":"CASH"}""", ContentType.Application.Json)) }.bodyAsText()
        }
        assertEquals(1, store.queued(NativeOffline.owner()!!).size)
    }
    @Test fun reconnectReplaysAndRemovesOnlyAcknowledgedActions() = runBlocking<Unit> {
        prime(); available = false; create(); available = true
        replayOfflineOnce(client, store)
        assertTrue(store.queued(NativeOffline.owner()!!).isEmpty())
        assertEquals("remote-order", store.get("${NativeOffline.owner()}:map:local-order"))
    }
    @Test fun cachedDataCannotBeReadUnderAnotherAccount() = runBlocking<Unit> {
        prime(); available = false
        UserSession.setUser(User("other", "other@test", "", "Other", UserRole.STAFF, "business", Clock.System.now()))
        assertFailsWith<java.io.IOException> { client.get("$BASE_URL/products").bodyAsText() }
        assertTrue(store.queued(NativeOffline.owner()!!).isEmpty())
    }
    @Test fun externalPaymentsAreNeverQueuedOrPretendedSuccessful() = runBlocking<Unit> {
        prime(); available = false
        assertFailsWith<java.io.IOException> {
            client.post("$BASE_URL/orders") { setBody(TextContent("""{"paymentMethod":"MPESA","items":[]}""", ContentType.Application.Json)) }.bodyAsText()
        }
        assertTrue(store.queued(NativeOffline.owner()!!).isEmpty())
    }
}
