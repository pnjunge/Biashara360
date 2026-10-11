package com.app.biashara.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.app.biashara.db.Biashara360Database
import io.ktor.client.request.HttpRequestData
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.*

class OrderRepositoryRegressionTest {
    private val orderJson = """{
        "id":"order-1","orderNumber":"SALE-1","businessId":"business-1",
        "customerId":null,"customerName":"Customer","customerPhone":"",
        "items":[{"productId":"product-1","productName":"Product","quantity":1,"unitPrice":1000.0,"buyingPrice":500.0}],
        "paymentStatus":"PAID","deliveryStatus":"DELIVERED","paymentMethod":"CASH",
        "taxIncluded":true,"taxRate":0.16,"taxAmount":160.0,
        "createdAt":"2026-09-12T10:00:00Z","updatedAt":"2026-09-12T10:00:00Z"
    }"""
    private fun page(data: String, page: Int = 1, hasMore: Boolean = false) =
        """{"success":true,"data":{"data":[$data],"total":1,"page":$page,"pageSize":100,"hasMore":$hasMore}}"""

    private fun fixture(inspect: (HttpRequestData) -> Unit = {}, block: suspend (OrderRepositoryImpl, (String) -> Unit) -> Unit) = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Biashara360Database.Schema.create(driver)
        var response = page(orderJson)
        val client = HttpClient(MockEngine { request -> inspect(request); respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        try {
            val repo = OrderRepositoryImpl(Biashara360Database(driver), client)
            assertTrue(repo.syncOrdersFromApi("business-1").isSuccess)
            block(repo) { response = it }
        } finally { client.close(); driver.close() }
    }

    @Test fun cachedOrderPreservesTaxAndTotal() = fixture { repo, _ ->
        val order = assertNotNull(repo.getOrder("order-1"))
        assertTrue(order.includeTax)
        assertEquals(0.16, order.taxRate)
        assertEquals(1160.0, order.total)
    }

    @Test fun apiFailureDoesNotDeleteCachedOrders() = fixture { repo, respond ->
        respond("""{"success":false,"data":null,"message":"Unauthorized"}""")
        assertTrue(repo.syncOrdersFromApi("business-1").isFailure)
        assertNotNull(repo.getOrder("order-1"))
    }

    @Test fun missingDataDoesNotDeleteCachedOrders() = fixture { repo, respond ->
        respond("""{"success":true,"data":null}""")
        assertTrue(repo.syncOrdersFromApi("business-1").isFailure)
        assertNotNull(repo.getOrder("order-1"))
    }

    @Test fun invalidPaginationDoesNotDeleteCachedOrders() = fixture { repo, respond ->
        respond(page("", page = 2))
        assertTrue(repo.syncOrdersFromApi("business-1").isFailure)
        assertNotNull(repo.getOrder("order-1"))
    }

    @Test fun successfulEmptySyncRemovesStaleOrders() = fixture { repo, respond ->
        respond(page(""))
        assertTrue(repo.syncOrdersFromApi("business-1").isSuccess)
        assertNull(repo.getOrder("order-1"))
    }

    @Test fun dineInCheckoutSendsSelectedTableAndGuests() {
        var sent = false
        fixture(inspect = { request ->
            if (request.method == HttpMethod.Post) {
                val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                assertEquals("table-7", body.getValue("hospitalityTableId").jsonPrimitive.content)
                assertEquals("DINE_IN", body.getValue("serviceType").jsonPrimitive.content)
                assertEquals("4", body.getValue("guestCount").jsonPrimitive.content)
                sent = true
            }
        }) { repo, respond ->
            val order = assertNotNull(repo.getOrder("order-1"))
            respond("""{"success":true,"data":$orderJson}""")
            assertTrue(repo.createOrder(order.copy(hospitalityTableId = "table-7", serviceType = "DINE_IN", guestCount = 4)).isSuccess)
        }
        assertTrue(sent)
    }

    @Test fun migrationPreservesExistingOrders() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(null, """CREATE TABLE OrderEntity (
                id TEXT NOT NULL PRIMARY KEY, order_number TEXT NOT NULL, business_id TEXT NOT NULL,
                customer_id TEXT, customer_name TEXT NOT NULL, customer_phone TEXT NOT NULL,
                delivery_location TEXT NOT NULL DEFAULT '', payment_status TEXT NOT NULL,
                delivery_status TEXT NOT NULL, payment_method TEXT NOT NULL DEFAULT 'MPESA',
                mpesa_transaction_code TEXT, notes TEXT NOT NULL DEFAULT '', subtotal REAL NOT NULL,
                created_at TEXT NOT NULL, updated_at TEXT NOT NULL
            )""", 0)
            driver.execute(null, """INSERT INTO OrderEntity VALUES ('old','OLD','business-1',NULL,'Customer','','','PAID','DELIVERED','CASH',NULL,'',1000,'2026-09-12T10:00:00Z','2026-09-12T10:00:00Z')""", 0)
            Biashara360Database.Schema.migrate(driver, 2, 3)
            val order = Biashara360Database(driver).biashara360DatabaseQueries.selectOrderById("old").executeAsOne()
            assertEquals(1000.0, order.subtotal)
            assertEquals(0L, order.tax_included)
            assertEquals(0.0, order.tax_rate)
        } finally { driver.close() }
    }
}
