package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class BranchService(
    private val auditLogService: AuditLogService? = null
) {

    fun listBranches(businessId: String, includeInactive: Boolean = false): List<BranchResponse> = transaction {
        var query = BranchesTable.select { BranchesTable.businessId eq businessId }
        if (!includeInactive) {
            query = query.andWhere { BranchesTable.isActive eq true }
        }
        val branches = query
            .orderBy(BranchesTable.isHeadOffice, SortOrder.DESC)
            .orderBy(BranchesTable.createdAt, SortOrder.ASC)
            .map { it.toBranchResponse() }

        if (branches.isEmpty()) {
            listOf(ensureDefaultBranch(businessId))
        } else {
            branches
        }
    }

    fun getBranch(businessId: String, branchId: String): BranchResponse? = transaction {
        BranchesTable.select {
            (BranchesTable.id eq branchId) and (BranchesTable.businessId eq businessId)
        }.firstOrNull()?.toBranchResponse()
    }

    fun createBranch(
        businessId: String,
        req: BranchRequest,
        actorUserId: String? = null,
        ipAddress: String? = null
    ): ApiResponse<BranchResponse> = transaction {
        val name = req.name.trim()
        if (name.isBlank()) {
            return@transaction ApiResponse(false, message = "Branch name is required")
        }

        val code = (req.code?.trim()?.takeIf { it.isNotBlank() } ?: generateBranchCode(businessId, name)).uppercase()
        if (!code.matches(Regex("^[A-Z0-9_-]{2,20}$"))) {
            return@transaction ApiResponse(false, message = "Branch code must be 2 to 20 uppercase alphanumeric characters or dashes")
        }

        val codeExists = BranchesTable.select {
            (BranchesTable.businessId eq businessId) and (BranchesTable.code eq code)
        }.any()
        if (codeExists) {
            return@transaction ApiResponse(false, message = "Branch code '$code' already exists for this business")
        }

        val now = Clock.System.now()
        val branchId = generateId()

        val isHeadOffice = req.isHeadOffice || !BranchesTable.select { (BranchesTable.businessId eq businessId) and (BranchesTable.isHeadOffice eq true) }.any()

        if (isHeadOffice) {
            BranchesTable.update({ (BranchesTable.businessId eq businessId) and (BranchesTable.isHeadOffice eq true) }) {
                it[BranchesTable.isHeadOffice] = false
                it[updatedAt] = now
            }
        }

        BranchesTable.insert {
            it[id] = branchId
            it[BranchesTable.businessId] = businessId
            it[BranchesTable.name] = name
            it[BranchesTable.code] = code
            it[phone] = req.phone?.trim()?.takeIf { p -> p.isNotBlank() }
            it[email] = req.email?.trim()?.takeIf { e -> e.isNotBlank() }
            it[address] = req.address?.trim()?.takeIf { a -> a.isNotBlank() }
            it[city] = req.city?.trim()?.takeIf { c -> c.isNotBlank() }
            it[county] = req.county?.trim()?.takeIf { c -> c.isNotBlank() }
            it[BranchesTable.isHeadOffice] = isHeadOffice
            it[isActive] = true
            it[receiptHeader] = req.receiptHeader?.trim()?.takeIf { h -> h.isNotBlank() }
            it[receiptFooter] = req.receiptFooter?.trim()?.takeIf { f -> f.isNotBlank() }
            it[createdAt] = now
            it[updatedAt] = now
        }

        auditLogService?.logEvent(
            businessId = businessId,
            actorUserId = actorUserId,
            targetUserId = null,
            action = "CREATE_BRANCH",
            ipAddress = ipAddress,
            details = "Created branch '$name' ($code), head office: $isHeadOffice"
        )

        val created = BranchesTable.select { BranchesTable.id eq branchId }.first().toBranchResponse()
        ApiResponse(true, data = created, message = "Branch created successfully")
    }

    fun updateBranch(
        businessId: String,
        branchId: String,
        req: BranchRequest,
        actorUserId: String? = null,
        ipAddress: String? = null
    ): ApiResponse<BranchResponse> = transaction {
        val existing = BranchesTable.select {
            (BranchesTable.id eq branchId) and (BranchesTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Branch not found")

        val name = req.name.trim()
        if (name.isBlank()) {
            return@transaction ApiResponse(false, message = "Branch name is required")
        }

        val code = (req.code?.trim()?.takeIf { it.isNotBlank() } ?: existing[BranchesTable.code]).uppercase()
        if (!code.matches(Regex("^[A-Z0-9_-]{2,20}$"))) {
            return@transaction ApiResponse(false, message = "Branch code must be 2 to 20 uppercase alphanumeric characters or dashes")
        }

        val codeDuplicate = BranchesTable.select {
            (BranchesTable.businessId eq businessId) and
                (BranchesTable.code eq code) and
                (BranchesTable.id neq branchId)
        }.any()
        if (codeDuplicate) {
            return@transaction ApiResponse(false, message = "Branch code '$code' is already in use by another branch")
        }

        val now = Clock.System.now()
        val willBeHeadOffice = req.isHeadOffice

        if (willBeHeadOffice && !existing[BranchesTable.isHeadOffice]) {
            BranchesTable.update({ (BranchesTable.businessId eq businessId) and (BranchesTable.isHeadOffice eq true) }) {
                it[BranchesTable.isHeadOffice] = false
                it[updatedAt] = now
            }
        }

        // If unsetting head office, ensure at least one head office remains
        val finalIsHeadOffice = if (!willBeHeadOffice && existing[BranchesTable.isHeadOffice]) {
            val otherHeadOffice = BranchesTable.select {
                (BranchesTable.businessId eq businessId) and
                    (BranchesTable.id neq branchId) and
                    (BranchesTable.isHeadOffice eq true)
            }.any()
            if (!otherHeadOffice) true else false
        } else {
            willBeHeadOffice
        }

        BranchesTable.update({ (BranchesTable.id eq branchId) and (BranchesTable.businessId eq businessId) }) {
            it[BranchesTable.name] = name
            it[BranchesTable.code] = code
            it[phone] = req.phone?.trim()?.takeIf { p -> p.isNotBlank() }
            it[email] = req.email?.trim()?.takeIf { e -> e.isNotBlank() }
            it[address] = req.address?.trim()?.takeIf { a -> a.isNotBlank() }
            it[city] = req.city?.trim()?.takeIf { c -> c.isNotBlank() }
            it[county] = req.county?.trim()?.takeIf { c -> c.isNotBlank() }
            it[BranchesTable.isHeadOffice] = finalIsHeadOffice
            it[receiptHeader] = req.receiptHeader?.trim()?.takeIf { h -> h.isNotBlank() }
            it[receiptFooter] = req.receiptFooter?.trim()?.takeIf { f -> f.isNotBlank() }
            it[updatedAt] = now
        }

        auditLogService?.logEvent(
            businessId = businessId,
            actorUserId = actorUserId,
            targetUserId = null,
            action = "UPDATE_BRANCH",
            ipAddress = ipAddress,
            details = "Updated branch '$name' ($code)"
        )

        val updated = BranchesTable.select { BranchesTable.id eq branchId }.first().toBranchResponse()
        ApiResponse(true, data = updated, message = "Branch updated successfully")
    }

    fun setHeadOffice(
        businessId: String,
        branchId: String,
        actorUserId: String? = null,
        ipAddress: String? = null
    ): ApiResponse<BranchResponse> = transaction {
        val target = BranchesTable.select {
            (BranchesTable.id eq branchId) and (BranchesTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Branch not found")

        val now = Clock.System.now()
        BranchesTable.update({ (BranchesTable.businessId eq businessId) and (BranchesTable.isHeadOffice eq true) }) {
            it[isHeadOffice] = false
            it[updatedAt] = now
        }
        BranchesTable.update({ (BranchesTable.id eq branchId) and (BranchesTable.businessId eq businessId) }) {
            it[isHeadOffice] = true
            it[isActive] = true
            it[updatedAt] = now
        }

        auditLogService?.logEvent(
            businessId = businessId,
            actorUserId = actorUserId,
            targetUserId = null,
            action = "SET_HEAD_OFFICE_BRANCH",
            ipAddress = ipAddress,
            details = "Set branch '${target[BranchesTable.name]}' as head office"
        )

        val updated = BranchesTable.select { BranchesTable.id eq branchId }.first().toBranchResponse()
        ApiResponse(true, data = updated, message = "'${updated.name}' is now the primary head office")
    }

    fun deleteBranch(
        businessId: String,
        branchId: String,
        actorUserId: String? = null,
        ipAddress: String? = null
    ): ApiResponse<Unit> = transaction {
        val branch = BranchesTable.select {
            (BranchesTable.id eq branchId) and (BranchesTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Branch not found")

        if (branch[BranchesTable.isHeadOffice]) {
            return@transaction ApiResponse(false, message = "Cannot delete the primary head office branch. Reassign head office first.")
        }

        val totalBranches = BranchesTable.select { (BranchesTable.businessId eq businessId) and (BranchesTable.isActive eq true) }.count()
        if (totalBranches <= 1) {
            return@transaction ApiResponse(false, message = "Merchant must have at least one active branch")
        }

        // Check for dependencies
        val hasOrders = OrdersTable.select { OrdersTable.branchId eq branchId }.any()
        val hasUsers = UsersTable.select { UsersTable.branchId eq branchId }.any()
        val hasExpenses = ExpensesTable.select { ExpensesTable.branchId eq branchId }.any()
        val hasStockMovements = StockMovementsTable.select { StockMovementsTable.branchId eq branchId }.any()

        val now = Clock.System.now()
        if (hasOrders || hasUsers || hasExpenses || hasStockMovements) {
            // Soft delete / deactivate
            BranchesTable.update({ (BranchesTable.id eq branchId) and (BranchesTable.businessId eq businessId) }) {
                it[isActive] = false
                it[updatedAt] = now
            }
            auditLogService?.logEvent(
                businessId = businessId,
                actorUserId = actorUserId,
                targetUserId = null,
                action = "DEACTIVATE_BRANCH",
                ipAddress = ipAddress,
                details = "Deactivated branch '${branch[BranchesTable.name]}' (${branch[BranchesTable.code]})"
            )
            ApiResponse(true, message = "Branch has transaction history and was deactivated instead of deleted")
        } else {
            // Hard delete
            BranchesTable.deleteWhere { (BranchesTable.id eq branchId) and (BranchesTable.businessId eq businessId) }
            auditLogService?.logEvent(
                businessId = businessId,
                actorUserId = actorUserId,
                targetUserId = null,
                action = "DELETE_BRANCH",
                ipAddress = ipAddress,
                details = "Deleted branch '${branch[BranchesTable.name]}' (${branch[BranchesTable.code]})"
            )
            ApiResponse(true, message = "Branch deleted successfully")
        }
    }

    fun ensureDefaultBranch(businessId: String): BranchResponse = transaction {
        val existing = BranchesTable.select { BranchesTable.businessId eq businessId }
            .orderBy(BranchesTable.isHeadOffice, SortOrder.DESC)
            .firstOrNull()
        if (existing != null) return@transaction existing.toBranchResponse()

        val business = BusinessesTable.select { BusinessesTable.id eq businessId }.firstOrNull()
        val now = Clock.System.now()
        val branchId = generateId()
        val name = business?.get(BusinessesTable.name)?.takeIf { it.isNotBlank() }?.let { "$it - Head Office" } ?: "Head Office"

        BranchesTable.insert {
            it[id] = branchId
            it[BranchesTable.businessId] = businessId
            it[BranchesTable.name] = name
            it[code] = "MAIN"
            it[phone] = business?.get(BusinessesTable.ownerPhone)
            it[email] = business?.get(BusinessesTable.ownerEmail)
            it[address] = business?.get(BusinessesTable.address)
            it[county] = business?.get(BusinessesTable.county)
            it[isHeadOffice] = true
            it[isActive] = true
            it[receiptHeader] = business?.get(BusinessesTable.receiptHeader)
            it[receiptFooter] = business?.get(BusinessesTable.receiptFooter)
            it[createdAt] = now
            it[updatedAt] = now
        }

        BranchesTable.select { BranchesTable.id eq branchId }.first().toBranchResponse()
    }

    private fun generateBranchCode(businessId: String, name: String): String {
        val base = name.trim().uppercase()
            .replace(Regex("[^A-Z0-9]"), "")
            .take(4)
            .ifBlank { "BR" }
        val count = BranchesTable.select { BranchesTable.businessId eq businessId }.count()
        val candidate = "$base${count + 1}"
        return candidate.take(10)
    }

    private fun ResultRow.toBranchResponse() = BranchResponse(
        id = this[BranchesTable.id],
        businessId = this[BranchesTable.businessId],
        name = this[BranchesTable.name],
        code = this[BranchesTable.code],
        phone = this[BranchesTable.phone],
        email = this[BranchesTable.email],
        address = this[BranchesTable.address],
        city = this[BranchesTable.city],
        county = this[BranchesTable.county],
        isHeadOffice = this[BranchesTable.isHeadOffice],
        isActive = this[BranchesTable.isActive],
        receiptHeader = this[BranchesTable.receiptHeader],
        receiptFooter = this[BranchesTable.receiptFooter],
        createdAt = this[BranchesTable.createdAt].toString(),
        updatedAt = this[BranchesTable.updatedAt].toString()
    )
}
