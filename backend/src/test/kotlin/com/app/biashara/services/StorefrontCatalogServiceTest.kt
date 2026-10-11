package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.*
import com.app.biashara.routes.storefrontCatalogRoutes
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.*

class StorefrontCatalogServiceTest {
    private val catalog = StorefrontCatalogService()
    private val orders = mockk<OrderService>(relaxed = true)
    private val hospitality = mockk<HospitalityService>(relaxed = true)
    private val storefront = StorefrontService(orders, mockk(relaxed = true), hospitality)

    @BeforeTest fun setup() {
        TransactionManager.defaultDatabase = Database.connect("jdbc:h2:mem:catalog-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(BusinessesTable, ProductsTable, ProductRecipesTable, InventoryIngredientsTable)
            for (key in listOf("shop", "other")) BusinessesTable.insert {
                it[id] = key; it[name] = key; it[storefrontSlug] = key; it[type] = "RETAIL"
                it[ownerEmail] = "$key@example.test"; it[ownerPhone] = "254712345678"
                it[createdAt] = Clock.System.now(); it[updatedAt] = Clock.System.now()
            }
            for ((key, business) in listOf("meal" to "shop", "drink" to "shop", "foreign" to "other")) ProductsTable.insert {
                it[id] = key; it[businessId] = business; it[sku] = key; it[name] = key
                it[buyingPrice] = 50.0; it[sellingPrice] = 100.0; it[currentStock] = 10; it[category] = "Food"
                it[createdAt] = Clock.System.now(); it[updatedAt] = Clock.System.now()
            }
        }
    }
    @Test fun `portal management routes require administrator access`() = testApplication {
        environment { config = io.ktor.server.config.MapApplicationConfig() }
        val algorithm = Algorithm.HMAC256("catalog-test-only")
        fun token(role: String) = JWT.create().withSubject("operator").withClaim("businessId", "shop").withClaim("role", role).sign(algorithm)
        application {
            install(ContentNegotiation) { json() }
            install(Authentication) { jwt { verifier(JWT.require(algorithm).build()); validate { JWTPrincipal(it.payload) } } }
            routing { authenticate { storefrontCatalogRoutes() } }
        }
        assertEquals(HttpStatusCode.Forbidden, client.get("/storefront/items").status)
        assertEquals(HttpStatusCode.Forbidden, client.get("/storefront/items") { bearerAuth(token("STAFF")) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.put("/storefront/items/meal") { bearerAuth(token("STAFF")); contentType(ContentType.Application.Json); setBody("""{"onlineVisible":false}""") }.status)
        assertEquals(HttpStatusCode.OK, client.get("/storefront/items") { bearerAuth(token("ADMIN")) }.status)
        assertEquals(HttpStatusCode.OK, client.put("/storefront/items/meal") { bearerAuth(token("ADMIN")); contentType(ContentType.Application.Json); setBody("""{"onlineVisible":false,"onlineGroup":"Meals"}""") }.status)
        assertFalse(catalog.list("shop").first { it.id == "meal" }.onlineVisible)
    }
    @Test fun `portal removal preserves POS inventory and can be reversed`() {
        catalog.update("shop", "meal", StorefrontCatalogUpdate(false, "Meals"))
        assertEquals(listOf("drink"), storefront.getStorefront("shop")!!.products.map { it.id })
        transaction {
            val row = ProductsTable.select { ProductsTable.id eq "meal" }.single()
            assertTrue(row[ProductsTable.isActive]); assertEquals(10, row[ProductsTable.currentStock])
            assertEquals("Food", row[ProductsTable.category])
        }
        assertEquals(2, catalog.list("shop").size)
        catalog.update("shop", "meal", StorefrontCatalogUpdate(true, "Meals"))
        assertEquals(2, storefront.getStorefront("shop")!!.products.size)
    }
    @Test fun `portal groups override inventory categories and blank groups fall back`() {
        catalog.update("shop", "drink", StorefrontCatalogUpdate(true, "  Drinks  "))
        val products = storefront.getStorefront("shop")!!.products.associateBy { it.id }
        assertEquals("Drinks", products.getValue("drink").group)
        assertEquals("Food", products.getValue("meal").group)
        assertEquals("Food", products.getValue("drink").category)
        catalog.update("shop", "drink", StorefrontCatalogUpdate(true, ""))
        assertEquals("Food", storefront.getStorefront("shop")!!.products.first { it.id == "drink" }.group)
    }
    @Test fun `portal management cannot alter another merchant and rejects invalid groups`() {
        assertFailsWith<IllegalArgumentException> { catalog.update("shop", "foreign", StorefrontCatalogUpdate(false)) }
        assertTrue(catalog.list("other").single().onlineVisible)
        assertFailsWith<IllegalArgumentException> { catalog.update("shop", "meal", StorefrontCatalogUpdate(true, "x".repeat(101))) }
        assertFailsWith<IllegalArgumentException> { catalog.update("shop", "meal", StorefrontCatalogUpdate(true, "bad\ngroup")) }
    }
    @Test fun `a removed item cannot be ordered from a stale or crafted cart`() = runBlocking {
        catalog.update("shop", "meal", StorefrontCatalogUpdate(false))
        val result = storefront.checkout("shop", StorefrontCheckoutRequest("customer-reference", "Customer", "0712345678", "Pickup", "COD", listOf(StorefrontCheckoutItemRequest("meal", 1))))
        assertFalse(result.success); assertEquals("One or more products are unavailable", result.message)
    }
}
