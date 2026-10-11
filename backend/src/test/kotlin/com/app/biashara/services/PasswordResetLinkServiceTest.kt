package com.app.biashara.services

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.db.*
import io.ktor.server.config.MapApplicationConfig
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.*

class PasswordResetLinkServiceTest {
    private data class Delivery(val email: String, val phone: String, val name: String, val link: String, val channel: String)
    private val deliveries = mutableListOf<Delivery>()
    private var deliveryFails = false
    private val service = PasswordResetLinkService("https://biashara360.co.ke/login") { email, phone, name, link, channel ->
        deliveries += Delivery(email, phone, name, link, channel)
        if (deliveryFails) Result.failure(IllegalStateException("provider unavailable")) else Result.success(Unit)
    }
    @BeforeTest fun setup() {
        TransactionManager.defaultDatabase = Database.connect("jdbc:h2:mem:reset-${UUID.randomUUID()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(BusinessesTable, BranchesTable, UsersTable, OtpTable, RefreshTokensTable, PasswordResetLinksTable)
            exec("""INSERT INTO users(id,name,email,phone,password_hash,created_at,updated_at)
                VALUES ('a','Alice','alice@example.test','0712345678','old-hash',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),
                ('b','Bob','bob@example.test','0798765432','bob-hash',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)""")
            exec("INSERT INTO refresh_tokens(id,user_id,token,expires_at,created_at) VALUES ('r','a','refresh-secret',TIMESTAMP '2099-01-01 00:00:00',CURRENT_TIMESTAMP)")
        }
    }
    private fun token() = deliveries.last().link.substringAfter("#token=")
    @Test fun `email gets a strong hashed secret and link never appears in response`() {
        val response = service.request(" ALICE@EXAMPLE.TEST ", "EMAIL")
        assertTrue(response.success)
        assertEquals("alice@example.test", deliveries.single().email)
        assertEquals("EMAIL", deliveries.single().channel)
        assertTrue(token().matches(Regex("[a-f0-9]{64}")))
        assertFalse(response.message.contains(token()))
        assertNotEquals(token(), transaction { PasswordResetLinksTable.selectAll().single()[PasswordResetLinksTable.tokenHash] })
    }
    @Test fun `whatsapp goes to registered phone and works without logged in session`() {
        assertTrue(service.request("alice@example.test", "WHATSAPP").success)
        assertEquals("0712345678", deliveries.single().phone)
        assertEquals("WHATSAPP", deliveries.single().channel)
        assertTrue(service.confirm(token(), "S3cure!Pass").success)
        transaction {
            val user = UsersTable.select { UsersTable.id eq "a" }.single()
            assertTrue(PasswordUtils.verify("S3cure!Pass", user[UsersTable.passwordHash]))
            assertNotNull(user[UsersTable.tokenValidAfter])
            assertEquals("bob-hash", UsersTable.select { UsersTable.id eq "b" }.single()[UsersTable.passwordHash])
            assertTrue(RefreshTokensTable.selectAll().empty())
            assertTrue(PasswordResetLinksTable.selectAll().empty())
        }
        assertFalse(service.confirm(token(), "Another!123").success)
    }
    @Test fun `expired unknown and inactive links cannot change password`() {
        service.request("alice@example.test", "EMAIL")
        assertFalse(service.confirm("0".repeat(64), "S3cure!Pass").success)
        transaction { exec("UPDATE password_reset_links SET expires_at=TIMESTAMP '2000-01-01 00:00:00'") }
        assertFalse(service.confirm(token(), "S3cure!Pass").success)
        transaction {
            exec("UPDATE password_reset_links SET expires_at=TIMESTAMP '2099-01-01 00:00:00'")
            exec("UPDATE users SET is_active=false WHERE id='a'")
        }
        assertFalse(service.confirm(token(), "S3cure!Pass").success)
    }
    @Test fun `resend is throttled and replaces old link after cooldown`() {
        service.request("alice@example.test", "EMAIL")
        val old = token()
        service.request("alice@example.test", "WHATSAPP")
        assertEquals(1, deliveries.size)
        transaction { exec("UPDATE password_reset_links SET created_at=TIMESTAMP '2000-01-01 00:00:00'") }
        service.request("alice@example.test", "WHATSAPP")
        assertEquals(2, deliveries.size)
        assertFalse(service.confirm(old, "S3cure!Pass").success)
        assertTrue(service.confirm(token(), "S3cure!Pass").success)
    }
    @Test fun `absent inactive or missing phone never leak account information`() {
        val absent = service.request("absent@example.test", "WHATSAPP")
        transaction { exec("UPDATE users SET phone='' WHERE id='a'"); exec("UPDATE users SET is_active=false WHERE id='b'") }
        assertEquals(absent, service.request("alice@example.test", "WHATSAPP"))
        assertEquals(absent, service.request("bob@example.test", "WHATSAPP"))
        assertTrue(deliveries.isEmpty())
        assertTrue(transaction { PasswordResetLinksTable.selectAll().empty() })
    }
    @Test fun `failed delivery revokes link and weak password does not consume it`() {
        deliveryFails = true
        val failed = service.request("alice@example.test", "EMAIL")
        assertTrue(failed.success)
        assertFalse(service.confirm(token(), "S3cure!Pass").success)
        deliveryFails = false
        assertEquals(failed, service.request("alice@example.test", "EMAIL"))
        assertFalse(service.confirm(token(), "123").success)
        assertFalse(service.confirm(token(), "lettersOnly").success)
        assertTrue(service.confirm(token(), "S3cure!Pass").success)
    }
    @Test fun `reset email contains clickable link and escapes display name`() {
        val html = EmailService(MapApplicationConfig()).buildPasswordResetEmailHtml("https://example.test/login#token=abc", "<Alice>")
        assertContains(html, "href=\"https://example.test/login#token=abc\"")
        assertContains(html, "&lt;Alice&gt;")
        assertContains(html, "10 minutes")
        assertFalse(html.contains("<Alice>"))
    }
}
