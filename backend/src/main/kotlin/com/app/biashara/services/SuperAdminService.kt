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
        if (req.businessName.isBlank() || req.businessType.isBlank()) {
            return@transaction ApiResponse(false, message = "Business name and type are required")
        }
        if (req.adminName.isBlank() || req.adminEmail.isBlank() || req.adminPhone.isBlank()) {
            return@transaction ApiResponse(false, message = "Admin name, email, and phone are required")
        }
        if (req.adminPassword.length < 6) {
            return@transaction ApiResponse(false, message = "Admin password must be at least 6 characters")
        }

        val emailExists = UsersTable.select { UsersTable.email eq req.adminEmail }.count() > 0
        if (emailExists) return@transaction ApiResponse(false, message = "Email already registered")

        val phoneExists = UsersTable.select { UsersTable.phone eq req.adminPhone }.count() > 0
        if (phoneExists) return@transaction ApiResponse(false, message = "Phone number already registered")

        val now = Clock.System.now()
        val businessId = generateId()
        val adminId = generateId()

        BusinessesTable.insert {
            it[id]               = businessId
            it[name]             = req.businessName
            it[type]             = req.businessType
            it[ownerPhone]       = req.adminPhone
            it[ownerEmail]       = req.adminEmail
            it[currency]         = "KES"
            it[subscriptionTier] = "FREEMIUM"
            it[enabledModules]   = "INVENTORY,SALES,CRM,EXPENSES,PAYMENTS,REPORTS"
            it[createdAt]        = now
            it[updatedAt]        = now
        }

        UsersTable.insert {
            it[id]               = adminId
            it[UsersTable.businessId] = businessId
            it[name]             = req.adminName
            it[email]            = req.adminEmail
            it[phone]            = req.adminPhone
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
            name             = req.businessName,
            type             = req.businessType,
            ownerPhone       = req.adminPhone,
            ownerEmail       = req.adminEmail,
            subscriptionTier = "FREEMIUM",
            createdAt        = now.toString()
        )
        val adminResp = UserResponse(
            id               = adminId,
            name             = req.adminName,
            email            = req.adminEmail,
            phone            = req.adminPhone,
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
