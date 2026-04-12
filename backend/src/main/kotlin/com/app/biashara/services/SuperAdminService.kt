package com.app.biashara.services

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.auth.generateId
import com.app.biashara.db.BusinessesTable
import com.app.biashara.db.UsersTable
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class SuperAdminService {

    fun listBusinesses(): ApiResponse<List<BusinessResponse>> = transaction {
        val businesses = BusinessesTable
            .selectAll()
            .orderBy(BusinessesTable.createdAt, SortOrder.DESC)
            .map { it.toBusinessResponse() }
        ApiResponse(success = true, data = businesses)
    }

    fun getBusiness(id: String): BusinessResponse? = transaction {
        BusinessesTable.select { BusinessesTable.id eq id }
            .firstOrNull()
            ?.toBusinessResponse()
    }

    fun createBusinessWithAdmin(req: CreateBusinessRequest): ApiResponse<BusinessResponse> = transaction {
        if (req.businessName.isBlank() || req.businessType.isBlank()) {
            return@transaction ApiResponse(false, message = "Business name and type are required")
        }
        if (req.adminName.isBlank() || req.adminEmail.isBlank() || req.adminPhone.isBlank()) {
            return@transaction ApiResponse(false, message = "Admin name, email, and phone are required")
        }
        if (req.adminPassword.length < 6) {
            return@transaction ApiResponse(false, message = "Admin password must be at least 6 characters")
        }

        // Ensure email/phone are unique
        val emailExists = UsersTable.select { UsersTable.email eq req.adminEmail }.count() > 0
        if (emailExists) return@transaction ApiResponse(false, message = "Email already registered")

        val phoneExists = UsersTable.select { UsersTable.phone eq req.adminPhone }.count() > 0
        if (phoneExists) return@transaction ApiResponse(false, message = "Phone number already registered")

        val now        = Clock.System.now()
        val businessId = generateId()
        val userId     = generateId()

        BusinessesTable.insert {
            it[id]               = businessId
            it[name]             = req.businessName
            it[type]             = req.businessType
            it[ownerPhone]       = req.adminPhone
            it[ownerEmail]       = req.adminEmail
            it[currency]         = req.currency.uppercase()
            it[subscriptionTier] = req.subscriptionTier.uppercase()
            it[createdAt]        = now
            it[updatedAt]        = now
        }

        UsersTable.insert {
            it[id]                    = userId
            it[UsersTable.businessId] = businessId
            it[name]                  = req.adminName
            it[email]                 = req.adminEmail
            it[phone]                 = req.adminPhone
            it[passwordHash]          = PasswordUtils.hash(req.adminPassword)
            it[role]                  = "ADMIN"
            it[twoFactorEnabled]      = false
            it[preferredLanguage]     = "ENGLISH"
            it[isActive]              = true
            it[createdAt]             = now
            it[updatedAt]             = now
        }

        val resp = BusinessesTable.select { BusinessesTable.id eq businessId }
            .first()
            .toBusinessResponse()
        ApiResponse(success = true, data = resp, message = "Business and admin user created successfully")
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun ResultRow.toBusinessResponse() = BusinessResponse(
        id               = this[BusinessesTable.id],
        name             = this[BusinessesTable.name],
        type             = this[BusinessesTable.type],
        ownerEmail       = this[BusinessesTable.ownerEmail],
        ownerPhone       = this[BusinessesTable.ownerPhone],
        currency         = this[BusinessesTable.currency],
        subscriptionTier = this[BusinessesTable.subscriptionTier],
        createdAt        = this[BusinessesTable.createdAt].toString()
    )
}
