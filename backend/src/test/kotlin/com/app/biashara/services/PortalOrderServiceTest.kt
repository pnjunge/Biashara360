package com.app.biashara.services

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

class PortalOrderServiceTest {
    private val service = PortalOrderService()

    @BeforeTest
    fun setup() {
        val db = Database.connect("jdbc:h2:mem:portal-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", driver = "org.h2.Driver")
        TransactionManager.defaultDatabase = db
        transaction(db) {
            exec("CREATE TABLE users(id VARCHAR(36) PRIMARY KEY, business_id VARCHAR(36), is_active BOOLEAN)")
            exec("INSERT INTO users VALUES ('u1','a',true),('u2','a',true),('u3','b',true),('inactive','a',false)")
            exec("""CREATE TABLE orders(id VARCHAR(36) PRIMARY KEY, order_number VARCHAR(20), business_id VARCHAR(36),
                customer_name VARCHAR(255), delivery_location TEXT, subtotal DOUBLE PRECISION,
                payment_status VARCHAR(20), delivery_status VARCHAR(20), sales_channel VARCHAR(30),
                server_user_id VARCHAR(36), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)""")
            exec("CREATE TABLE order_items(order_id VARCHAR(36), product_name VARCHAR(255), quantity INT)")
            exec("""INSERT INTO orders(id,order_number,business_id,customer_name,delivery_location,subtotal,payment_status,delivery_status,sales_channel)
                VALUES ('o1','B360-ECOM-1','a','Guest','Table 1',500,'PAID','PROCESSING','ECOMMERCE'),
                ('o2','B360-ECOM-2','b','Other tenant','Pickup',200,'COD','PENDING','ECOMMERCE'),
                ('o3','B360-DESK-3','a','POS','Table 2',200,'COD','PROCESSING','DESKTOP'),
                ('o4','B360-ECOM-4','a','Cancelled','Table 1',200,'CANCELLED','CANCELLED','ECOMMERCE')""")
            exec("INSERT INTO order_items VALUES ('o1','Meal',2),('o1','Soda',1)")
        }
    }

    @Test
    fun `queue shows only eligible portal orders from this business including paid orders`() {
        val queue = service.queue("a", "u1")
        assertEquals(listOf("o1"), queue.waiting.map { it.id })
        assertEquals(2, queue.waiting.single().items.size)
        assertEquals("PAID", queue.waiting.single().paymentStatus)
        assertTrue(queue.mine.isEmpty())
    }

    @Test
    fun `claim moves order to my queue and preserves payment`() {
        val claimed = service.claim("a", "u1", "o1")!!
        assertEquals("u1", claimed.claimedBy)
        assertEquals("PAID", claimed.paymentStatus)
        assertTrue(service.queue("a", "u2").waiting.isEmpty())
        assertEquals(listOf("o1"), service.queue("a", "u1").mine.map { it.id })
        assertNotNull(service.claim("a", "u1", "o1"))
        assertNull(service.claim("a", "u2", "o1"))
    }

    @Test
    fun `two simultaneous staff claims have exactly one winner`() {
        val pool = Executors.newFixedThreadPool(2)
        val gate = CyclicBarrier(2)
        try {
            val attempts = listOf("u1", "u2").map { user -> pool.submit<PortalOrderSummary?> {
                gate.await(5, TimeUnit.SECONDS)
                service.claim("a", user, "o1")
            } }
            assertEquals(1, attempts.map { it.get(15, TimeUnit.SECONDS) }.count { it != null })
        } finally { pool.shutdownNow() }
    }

    @Test
    fun `tenant spoofing inactive staff and non portal orders cannot be claimed`() {
        assertFailsWith<IllegalArgumentException> { service.claim("a", "u3", "o1") }
        assertFailsWith<IllegalArgumentException> { service.queue("a", "inactive") }
        assertNull(service.claim("b", "u3", "o1"))
        assertNull(service.claim("a", "u1", "o3"))
        assertNull(service.claim("a", "u1", "o4"))
        assertNull(service.claim("a", "u1", "missing"))
        assertEquals(listOf("o1"), service.queue("a", "u1").waiting.map { it.id })
    }
}
