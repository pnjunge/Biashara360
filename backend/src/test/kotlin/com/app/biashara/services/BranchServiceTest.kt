package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.BranchRequest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.*

class BranchServiceTest {
    private val branchService = BranchService()

    @BeforeTest
    fun setup() {
        val db = Database.connect(
            "jdbc:h2:mem:branches-${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver"
        )
        TransactionManager.defaultDatabase = db
        transaction(db) {
            org.jetbrains.exposed.sql.SchemaUtils.create(
                com.app.biashara.db.BusinessesTable,
                com.app.biashara.db.BranchesTable,
                com.app.biashara.db.UsersTable,
                com.app.biashara.db.OrdersTable,
                com.app.biashara.db.ExpensesTable,
                com.app.biashara.db.StockMovementsTable
            )
            com.app.biashara.db.BusinessesTable.insert {
                it[id] = "biz-1"
                it[name] = "Nairobi Traders"
                it[storefrontSlug] = "nairobi-traders"
                it[type] = "RETAIL"
                it[ownerPhone] = "254712345678"
                it[ownerEmail] = "owner@nairobi.co.ke"
                it[address] = "Kenyatta Ave"
                it[county] = "Nairobi"
                it[receiptHeader] = "Welcome"
                it[receiptFooter] = "Thank you"
                it[createdAt] = kotlinx.datetime.Clock.System.now()
                it[updatedAt] = kotlinx.datetime.Clock.System.now()
            }
        }
    }

    @Test
    fun `listBranches automatically creates default head office if business has none`() {
        val branches = branchService.listBranches("biz-1")
        assertEquals(1, branches.size)
        val hq = branches.first()
        assertTrue(hq.isHeadOffice)
        assertTrue(hq.isActive)
        assertEquals("MAIN", hq.code)
        assertEquals("Nairobi Traders - Head Office", hq.name)
    }

    @Test
    fun `merchant can add a second branch`() {
        // First list ensures head office
        branchService.listBranches("biz-1")

        val result = branchService.createBranch(
            businessId = "biz-1",
            req = BranchRequest(
                name = "Westlands Mall Branch",
                code = "WTL",
                phone = "254799000111",
                address = "Westgate Mall 2nd Floor",
                city = "Nairobi",
                county = "Nairobi",
                isHeadOffice = false
            )
        )

        assertTrue(result.success)
        assertNotNull(result.data)
        assertEquals("Westlands Mall Branch", result.data!!.name)
        assertEquals("WTL", result.data!!.code)
        assertFalse(result.data!!.isHeadOffice)

        val allBranches = branchService.listBranches("biz-1")
        assertEquals(2, allBranches.size)
        assertEquals("MAIN", allBranches[0].code) // Head office first
        assertEquals("WTL", allBranches[1].code)
    }

    @Test
    fun `cannot create duplicate branch code within same business`() {
        branchService.listBranches("biz-1") // Creates MAIN

        val result = branchService.createBranch(
            businessId = "biz-1",
            req = BranchRequest(name = "Another Main", code = "MAIN")
        )

        assertFalse(result.success)
        assertTrue(result.message.contains("already exists"))
    }

    @Test
    fun `switching head office marks previous head office as false`() {
        branchService.listBranches("biz-1")

        val branch2 = branchService.createBranch(
            businessId = "biz-1",
            req = BranchRequest(name = "Mombasa Branch", code = "MSA", isHeadOffice = false)
        ).data!!

        // Set Mombasa as head office
        val setHqResult = branchService.setHeadOffice("biz-1", branch2.id)
        assertTrue(setHqResult.success)
        assertTrue(setHqResult.data!!.isHeadOffice)

        val branches = branchService.listBranches("biz-1")
        assertEquals("MSA", branches[0].code)
        assertTrue(branches[0].isHeadOffice)
        assertEquals("MAIN", branches[1].code)
        assertFalse(branches[1].isHeadOffice)
    }

    @Test
    fun `cannot delete primary head office branch`() {
        val hq = branchService.listBranches("biz-1").first()

        val deleteResult = branchService.deleteBranch("biz-1", hq.id)
        assertFalse(deleteResult.success)
        assertTrue(deleteResult.message.contains("Cannot delete the primary head office"))
    }

    @Test
    fun `deleting branch with order history deactivates it instead`() {
        branchService.listBranches("biz-1")
        val branch2 = branchService.createBranch(
            businessId = "biz-1",
            req = BranchRequest(name = "CBD Branch", code = "CBD")
        ).data!!

        // Attach an order to CBD branch
        transaction {
            OrdersTable.insert {
                it[id] = "ord-1"
                it[orderNumber] = "ORD-1"
                it[OrdersTable.businessId] = "biz-1"
                it[OrdersTable.branchId] = branch2.id
                it[customerName] = "Test"
                it[customerPhone] = "254700000000"
                it[baseAmount] = 100.0
                it[subtotal] = 100.0
                it[createdAt] = kotlinx.datetime.Clock.System.now()
                it[updatedAt] = kotlinx.datetime.Clock.System.now()
            }
        }

        val deleteResult = branchService.deleteBranch("biz-1", branch2.id)
        assertTrue(deleteResult.success)
        assertTrue(deleteResult.message.contains("deactivated"))

        // When listing active branches, only 1 remains
        val activeBranches = branchService.listBranches("biz-1", includeInactive = false)
        assertEquals(1, activeBranches.size)

        // When listing all branches, 2 exist with one inactive
        val allBranches = branchService.listBranches("biz-1", includeInactive = true)
        assertEquals(2, allBranches.size)
        val cbd = allBranches.first { it.code == "CBD" }
        assertFalse(cbd.isActive)
    }
}
