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

    fun createBusinessWithAdmin(req: CreateBusinessWithAdminRequest): ApiResponse<BusinessWithAdminResponse> = transaction {
        val businessName = req.businessName.trim()
        val businessType = req.businessType.trim()
        val adminName    = req.adminName.trim()
        val adminEmail   = req.adminEmail.trim()
        val adminPhone   = req.adminPhone.trim()

        if (businessName.isBlank() || businessType.isBlank()) {
            return@transaction ApiResponse(false, message = "Business name and type are required")
        }
        if (adminName.isBlank() || adminEmail.isBlank() || adminPhone.isBlank()) {
            return@transaction ApiResponse(false, message = "Admin name, email, and phone are required")
        }
        if (req.adminPassword.length < 6) {
            return@transaction ApiResponse(false, message = "Admin password must be at least 6 characters")
        }

        val emailExists = UsersTable.select { UsersTable.email eq adminEmail }.count() > 0
        if (emailExists) return@transaction ApiResponse(false, message = "Email already registered")

        val phoneExists = UsersTable.select { UsersTable.phone eq adminPhone }.count() > 0
        if (phoneExists) return@transaction ApiResponse(false, message = "Phone number already registered")

        val now = Clock.System.now()
        val businessId = generateId()
        val adminId = generateId()

        BusinessesTable.insert {
            it[id]               = businessId
            it[name]             = businessName
            it[type]             = businessType
            it[ownerPhone]       = adminPhone
            it[ownerEmail]       = adminEmail
            it[currency]         = "KES"
            it[subscriptionTier] = "FREEMIUM"
            it[enabledModules]   = "INVENTORY,SALES,CRM,EXPENSES,PAYMENTS,REPORTS"
            it[createdAt]        = now
            it[updatedAt]        = now
        }

        UsersTable.insert {
            it[id]               = adminId
            it[UsersTable.businessId] = businessId
            it[name]             = adminName
            it[email]            = adminEmail
            it[phone]            = adminPhone
            it[passwordHash]     = PasswordUtils.hash(req.adminPassword)
            it[role]             = "ADMIN"
            it[twoFactorEnabled] = false
            it[preferredLanguage] = "ENGLISH"
            it[isActive]         = true
            it[createdAt]        = now
            it[updatedAt]        = now
        }

        val businessResp = BusinessResponse(
            id               = businessId,
            name             = businessName,
            type             = businessType,
            ownerPhone       = adminPhone,
            ownerEmail       = adminEmail,
            subscriptionTier = "FREEMIUM",
            createdAt        = now.toString()
        )
        val adminResp = UserResponse(
            id               = adminId,
            name             = adminName,
            email            = adminEmail,
            phone            = adminPhone,
            role             = "ADMIN",
            businessId       = businessId,
            preferredLanguage = "ENGLISH"
        )
        ApiResponse(success = true, data = BusinessWithAdminResponse(businessResp, adminResp), message = "Business and admin created successfully")
    }

    fun listBusinesses(): List<BusinessResponse> = transaction {
        BusinessesTable
            .select { BusinessesTable.type neq "SYSTEM" }
            .orderBy(BusinessesTable.createdAt, SortOrder.DESC)
            .map {
                BusinessResponse(
                    id               = it[BusinessesTable.id],
                    name             = it[BusinessesTable.name],
                    type             = it[BusinessesTable.type],
                    ownerPhone       = it[BusinessesTable.ownerPhone],
                    ownerEmail       = it[BusinessesTable.ownerEmail],
                    subscriptionTier = it[BusinessesTable.subscriptionTier],
                    createdAt        = it[BusinessesTable.createdAt].toString()
                )
            }
    }
}
