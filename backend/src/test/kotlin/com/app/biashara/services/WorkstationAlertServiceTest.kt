package com.app.biashara.services

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.*

class WorkstationAlertServiceTest {
    private val service = WorkstationAlertService()
    @BeforeTest fun setup() {
        val db=Database.connect("jdbc:h2:mem:workstation-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",driver="org.h2.Driver")
        TransactionManager.defaultDatabase=db
        transaction {
            exec("CREATE TABLE users(id VARCHAR(36),business_id VARCHAR(36),role VARCHAR(30),is_active BOOLEAN)")
            exec("INSERT INTO users VALUES ('admin-a','a','ADMIN',true),('staff-a','a','STAFF',true),('inactive-a','a','ADMIN',false),('super','platform','SUPERADMIN',true)")
            exec("CREATE TABLE businesses(id VARCHAR(36) PRIMARY KEY, name VARCHAR(255))")
            exec("INSERT INTO businesses VALUES ('a','Merchant A'),('b','Merchant B')")
            exec("CREATE TABLE workstation_alerts(id VARCHAR(36) PRIMARY KEY,business_id VARCHAR(36) REFERENCES businesses(id),name VARCHAR(100),token_hash VARCHAR(64) UNIQUE,expires_at TIMESTAMP,revoked BOOLEAN DEFAULT FALSE)")
            exec("CREATE TABLE orders(id VARCHAR(36),business_id VARCHAR(36),order_number VARCHAR(50),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,sales_channel VARCHAR(30),server_user_id VARCHAR(36),responsible_user_id VARCHAR(36),payment_status VARCHAR(30),delivery_status VARCHAR(30))")
            exec("""INSERT INTO orders(id,business_id,order_number,sales_channel,payment_status,delivery_status) VALUES
                ('o1','a','ECOM-A','ECOMMERCE','PAID','PENDING'),
                ('o2','b','ECOM-B','ECOMMERCE','PENDING','PENDING'),
                ('o3','a','POS-A','DESKTOP','PAID','PENDING'),
                ('o4','a','CANCEL-A','ECOMMERCE','CANCELLED','PENDING'),
                ('o5','a','DONE-A','ECOMMERCE','PAID','DELIVERED')""")
            exec("INSERT INTO orders(id,business_id,order_number,sales_channel,payment_status,delivery_status,responsible_user_id) VALUES ('o6','a','CLAIM-A','ECOMMERCE','PAID','PROCESSING','u1')")
        }
    }
    @Test fun `only active merchant administrators can create or revoke links`() {
        assertTrue(service.isAdministrator("a","admin-a"))
        assertFalse(service.isAdministrator("b","admin-a"))
        assertFalse(service.isAdministrator("a","staff-a"))
        assertFalse(service.isAdministrator("a","inactive-a"))
        assertFalse(service.isAdministrator("a","missing"))
        assertTrue(service.isAdministrator("b","super"))
    }
    @Test fun `linked workstation receives only unclaimed eligible merchant order references`() {
        val linked=service.link("a","Till 1")
        val feed=assertNotNull(service.feed(linked.token))
        assertEquals("Merchant A",feed.merchantName)
        assertEquals(listOf("o1"),feed.orders.map { it.id })
        assertEquals("ECOM-A",feed.orders.single().orderNumber)
        assertNull(service.feed("not-a-key"))
        assertEquals(listOf("o2"),service.feed(service.link("b","Till B").token)!!.orders.map { it.id })
    }
    @Test fun `device secrets are hashed and revocation cannot cross merchants`() {
        val linked=service.link("a","Till")
        val hash=transaction { WorkstationAlertsTable.selectAll().single()[WorkstationAlertsTable.tokenHash] }
        assertEquals(64,hash.length);assertNotEquals(linked.token,hash)
        assertFalse(service.revoke("b",linked.id));assertNotNull(service.feed(linked.token))
        assertTrue(service.revoke("a",linked.id));assertNull(service.feed(linked.token))
        assertTrue(service.list("a").single().revoked)
        assertTrue(service.list("b").isEmpty())
    }
    @Test fun `expired device cannot read alerts`() {
        val linked=service.link("a","Till")
        transaction { exec("UPDATE workstation_alerts SET expires_at=TIMESTAMP '2000-01-01 00:00:00'") }
        assertNull(service.feed(linked.token))
    }
    @Test fun `invalid name is rejected before storing a credential`() {
        assertFailsWith<IllegalArgumentException>{service.link("a", " ")}
        assertTrue(service.list("a").isEmpty())
    }
}
