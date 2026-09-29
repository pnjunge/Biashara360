package com.app.biashara.services

import com.app.biashara.db.BusinessesTable
import com.app.biashara.db.UsersTable
import com.app.biashara.models.ExtendSubscriptionRequest
import com.app.biashara.models.UpdateSubscriptionRequest
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.*
import kotlin.time.Duration.Companion.days

class SubscriptionExtensionTest {
    private val superAdminService = SuperAdminService(auditLogService = null)
    private val bizId = "11111111-1111-1111-1111-111111111111"

    @BeforeTest
    fun setup() {
        val db = Database.connect(
            "jdbc:h2:mem:sub-${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver"
        )
        TransactionManager.defaultDatabase = db
        transaction(db) {
            SchemaUtils.create(
                BusinessesTable,
                UsersTable
            )
            val now = Clock.System.now()
            BusinessesTable.insert {
                it[id] = bizId
                it[name] = "Trial Merchant"
                it[storefrontSlug] = "trial-merchant"
                it[type] = "RETAIL"
                it[ownerPhone] = "254711223344"
                it[ownerEmail] = "trial@example.com"
                it[subscriptionTier] = "TRIAL"
                it[subscriptionEnabled] = true
                it[isTrial] = true
                it[subscriptionValidUntil] = now.plus(14.days)
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    @Test
    fun `extendSubscription adds days to current subscription and preserves trial status if specified`() {
        val beforeList = superAdminService.listBusinesses()
        val before = beforeList.find { it.id == bizId }
        assertNotNull(before)
        assertTrue(before.isTrial)
        assertEquals("TRIAL", before.subscriptionTier)
        assertFalse(before.isExpired)
        assertTrue((before.daysRemaining ?: 0) in 13..14)

        // Extend trial by 14 days
        val res = superAdminService.extendSubscription(
            businessId = bizId,
            req = ExtendSubscriptionRequest(
                extendDays = 14,
                isTrial = true,
                tier = "TRIAL",
                note = "Extended trial for merchant onboard evaluation"
            ),
            actorUserId = "superadmin-1"
        )

        assertTrue(res.success)
        val updated = res.data
        assertNotNull(updated)
        assertTrue(updated.isTrial)
        assertEquals("TRIAL", updated.subscriptionTier)
        assertTrue((updated.daysRemaining ?: 0) >= 27)
        assertFalse(updated.isExpired)
    }

    @Test
    fun `extendSubscription can convert trial to paid tier with extension`() {
        val res = superAdminService.extendSubscription(
            businessId = bizId,
            req = ExtendSubscriptionRequest(
                extendDays = 30,
                isTrial = false,
                tier = "PREMIUM",
                note = "Upgraded merchant to premium with 30 days credit"
            ),
            actorUserId = "superadmin-1"
        )

        assertTrue(res.success)
        val updated = res.data
        assertNotNull(updated)
        assertFalse(updated.isTrial)
        assertEquals("PREMIUM", updated.subscriptionTier)
        assertTrue((updated.daysRemaining ?: 0) >= 43)
    }

    @Test
    fun `updateSubscription can set explicit validUntil date and detect expiration`() {
        val pastDate = Clock.System.now().plus((-2).days)
        val res = superAdminService.updateSubscription(
            businessId = bizId,
            req = UpdateSubscriptionRequest(
                enabled = true,
                tier = "TRIAL",
                isTrial = true,
                validUntil = pastDate.toString(),
                note = "Set expired trial"
            ),
            actorUserId = "superadmin-1"
        )

        assertTrue(res.success)
        val updated = res.data
        assertNotNull(updated)
        assertTrue(updated.isTrial)
        assertTrue(updated.isExpired)
        assertEquals(0, updated.daysRemaining)
    }

    @Test
    fun `extendSubscription from expired state starts extension from current time`() {
        // First set subscription to expired 5 days ago
        val pastDate = Clock.System.now().plus((-5).days)
        superAdminService.updateSubscription(
            businessId = bizId,
            req = UpdateSubscriptionRequest(
                enabled = true,
                tier = "TRIAL",
                isTrial = true,
                validUntil = pastDate.toString()
            )
        )

        // Now extend by 7 days
        val res = superAdminService.extendSubscription(
            businessId = bizId,
            req = ExtendSubscriptionRequest(
                extendDays = 7,
                isTrial = true,
                note = "Granted extension after trial expired"
            ),
            actorUserId = "superadmin-1"
        )

        assertTrue(res.success)
        val updated = res.data
        assertNotNull(updated)
        assertFalse(updated.isExpired)
        assertTrue((updated.daysRemaining ?: 0) in 6..7)
    }

    @Test
    fun `extendSubscription rejects zero or negative days`() {
        val res = superAdminService.extendSubscription(
            businessId = bizId,
            req = ExtendSubscriptionRequest(extendDays = 0)
        )
        assertFalse(res.success)
        assertTrue(res.message.contains("between 1 and 3650 days"))
    }
}
