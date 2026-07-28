package com.app.biashara.services

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.auth.generateId
import com.app.biashara.db.BusinessesTable
import com.app.biashara.db.UsersTable
import com.app.biashara.models.*
import com.app.biashara.utils.ValidationUtils
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class SuperAdminService {

    fun createBusinessWithAdmin(req: CreateBusinessWithAdminRequest): ApiResponse<BusinessWithAdminResponse> = transaction {
        // 🔒 SECURITY FIX: Enhanced input validation
        
        // Validate business data
        if (req.businessName.isBlank() || req.businessType.isBlank()) {
            return@transaction ApiResponse(false, message = "Business name and type are required")
        }
        if (!ValidationUtils.isValidBusinessName(req.businessName)) {
            return@transaction ApiResponse(false, message = "Invalid business name format")
        }

        // Validate admin data
        if (req.adminName.isBlank() || req.adminEmail.isBlank() || req.adminPhone.isBlank()) {
            return@transaction ApiResponse(false, message = "Admin name, email, and phone are required")
        }
        
        // 🔒 SECURITY FIX: Validate name format
        if (!ValidationUtils.isValidPersonName(req.adminName)) {
            return@transaction ApiResponse(false, message = "Invalid admin name format")
        }

        // 🔒 SECURITY FIX: Validate email format
        if (!ValidationUtils.isValidEmail(req.adminEmail)) {
            return@transaction ApiResponse(false, message = "Invalid email format")
        }

        // 🔒 SECURITY FIX: Validate phone format
        if (!ValidationUtils.isValidPhoneKE(req.adminPhone)) {
            return@transaction ApiResponse(false, message = "Invalid phone number format (must be Kenyan: +254XXX...)")
        }

        // 🔒 SECURITY FIX: Enforce strong password policy (min 12 chars, complexity)
        if (!ValidationUtils.isValidPassword(req.adminPassword)) {
            return@transaction ApiResponse(
                false, 
                message = ValidationUtils.getPasswordRequirements()
            )
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
            it[ownerPhone]       = ValidationUtils.normalizePhoneKE(req.adminPhone)
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
            it[phone]            = ValidationUtils.normalizePhoneKE(req.adminPhone)
            it[passwordHash]     = PasswordUtils.hash(req.adminPassword)
            it[role]             = "ADMIN"
            it[twoFactorEnabled] = true // 🔒 SECURITY: Enable 2FA by default
            it[preferredLanguage] = "ENGLISH"
            it[isActive]         = true
            it[createdAt]        = now
            it[updatedAt]        = now
        }

        val businessResp = BusinessResponse(
            id               = businessId,
            name             = req.businessName,
            type             = req.businessType,
            ownerPhone       = ValidationUtils.normalizePhoneKE(req.adminPhone),
            ownerEmail       = req.adminEmail,
            subscriptionTier = "FREEMIUM",
            subscriptionEnabled = true,
            isActive         = true,
            createdAt        = now.toString()
        )
        val adminResp = UserResponse(
            id               = adminId,
            name             = req.adminName,
            email            = req.adminEmail,
            phone            = ValidationUtils.normalizePhoneKE(req.adminPhone),
            role             = "ADMIN",
            businessId       = businessId,
            preferredLanguage = "ENGLISH"
        )
        ApiResponse(success = true, data = BusinessWithAdminResponse(businessResp, adminResp), message = "Business and admin created successfully")
    }

    fun createBusinessOnly(req: CreateBusinessOnlyRequest): ApiResponse<BusinessResponse> = transaction {
        if (req.businessName.isBlank() || req.businessType.isBlank()) {
            return@transaction ApiResponse(false, message = "Business name and type are required")
        }
        if (!ValidationUtils.isValidBusinessName(req.businessName)) {
            return@transaction ApiResponse(false, message = "Invalid business name format")
        }

        val now = Clock.System.now()
        val businessId = generateId()

        BusinessesTable.insert {
            it[id]               = businessId
            it[name]             = req.businessName
            it[type]             = req.businessType.uppercase()
            it[ownerPhone]       = ""
            it[ownerEmail]       = ""
            it[currency]         = "KES"
            it[subscriptionTier] = "FREEMIUM"
            it[enabledModules]   = "INVENTORY,SALES,CRM,EXPENSES,PAYMENTS,REPORTS"
            it[createdAt]        = now
            it[updatedAt]        = now
        }

        ApiResponse(
            success = true,
            data = BusinessResponse(
                id               = businessId,
                name             = req.businessName,
                type             = req.businessType.uppercase(),
                ownerPhone       = "",
                ownerEmail       = "",
                subscriptionTier = "FREEMIUM",
                subscriptionEnabled = true,
                isActive         = true,
                createdAt        = now.toString()
            ),
            message = "Business created successfully"
        )
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
                    subscriptionEnabled = it[BusinessesTable.subscriptionEnabled],
                    isActive         = it[BusinessesTable.isActive],
                    createdAt        = it[BusinessesTable.createdAt].toString()
                )
            }
    }

    fun setBusinessActiveStatus(businessId: String, req: UpdateBusinessStatusRequest): ApiResponse<BusinessResponse> = transaction {
        // 🔒 SECURITY: Validate UUID format
        if (!ValidationUtils.isValidUUID(businessId)) {
            return@transaction ApiResponse(false, message = "Invalid business ID format")
        }

        BusinessesTable.select {
            (BusinessesTable.id eq businessId) and (BusinessesTable.type neq "SYSTEM")
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Business not found")

        BusinessesTable.update({ BusinessesTable.id eq businessId }) {
            it[isActive] = req.isActive
            it[updatedAt] = Clock.System.now()
        }

        val updated = BusinessesTable.select { BusinessesTable.id eq businessId }.first()
        ApiResponse(
            success = true,
            data = BusinessResponse(
                id = updated[BusinessesTable.id],
                name = updated[BusinessesTable.name],
                type = updated[BusinessesTable.type],
                ownerPhone = updated[BusinessesTable.ownerPhone],
                ownerEmail = updated[BusinessesTable.ownerEmail],
                subscriptionTier = updated[BusinessesTable.subscriptionTier],
                subscriptionEnabled = updated[BusinessesTable.subscriptionEnabled],
                isActive = updated[BusinessesTable.isActive],
                createdAt = updated[BusinessesTable.createdAt].toString()
            ),
            message = if (req.isActive) "Business activated" else "Business deactivated"
        )
    }

    fun updateSubscription(
        businessId: String,
        req: UpdateSubscriptionRequest
    ): ApiResponse<BusinessResponse> = transaction {
        if (!ValidationUtils.isValidUUID(businessId)) {
            return@transaction ApiResponse(false, message = "Invalid business ID format")
        }
        val normalizedTier = req.tier?.trim()?.uppercase()
        if (normalizedTier != null && normalizedTier !in setOf("FREEMIUM", "PREMIUM")) {
            return@transaction ApiResponse(false, message = "Subscription tier must be FREEMIUM or PREMIUM")
        }
        BusinessesTable.select {
            (BusinessesTable.id eq businessId) and (BusinessesTable.type neq "SYSTEM")
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Business not found")

        BusinessesTable.update({ BusinessesTable.id eq businessId }) {
            it[subscriptionEnabled] = req.enabled
            if (normalizedTier != null) it[subscriptionTier] = normalizedTier
            it[updatedAt] = Clock.System.now()
        }
        val updated = BusinessesTable.select { BusinessesTable.id eq businessId }.first()
        ApiResponse(
            success = true,
            data = BusinessResponse(
                id = updated[BusinessesTable.id],
                name = updated[BusinessesTable.name],
                type = updated[BusinessesTable.type],
                ownerPhone = updated[BusinessesTable.ownerPhone],
                ownerEmail = updated[BusinessesTable.ownerEmail],
                subscriptionTier = updated[BusinessesTable.subscriptionTier],
                subscriptionEnabled = updated[BusinessesTable.subscriptionEnabled],
                isActive = updated[BusinessesTable.isActive],
                createdAt = updated[BusinessesTable.createdAt].toString()
            ),
            message = if (req.enabled) "Subscription enabled" else "Subscription disabled"
        )
    }

    fun linkUserToBusiness(userId: String, req: LinkUserToBusinessRequest): ApiResponse<UserResponse> = transaction {
        // 🔒 SECURITY: Validate UUIDs
        if (!ValidationUtils.isValidUUID(userId)) {
            return@transaction ApiResponse(false, message = "Invalid user ID format")
        }
        if (!ValidationUtils.isValidUUID(req.businessId)) {
            return@transaction ApiResponse(false, message = "Invalid business ID format")
        }

        val businessExists = BusinessesTable.select { BusinessesTable.id eq req.businessId }.count() > 0
        if (!businessExists) {
            return@transaction ApiResponse(false, message = "Business not found")
        }

        val user = UsersTable.select { UsersTable.id eq userId }.firstOrNull()
            ?: return@transaction ApiResponse(false, message = "User not found")

        if (user[UsersTable.role] == "SUPERADMIN") {
            return@transaction ApiResponse(false, message = "Cannot assign SUPERADMIN to a business")
        }

        val normalizedRole = req.role?.uppercase()
        if (normalizedRole != null && normalizedRole !in setOf("ADMIN", "STAFF")) {
            return@transaction ApiResponse(false, message = "Role must be ADMIN or STAFF")
        }

        UsersTable.update({ UsersTable.id eq userId }) {
            it[businessId] = req.businessId
            if (normalizedRole != null) {
                it[role] = normalizedRole
            }
            it[updatedAt] = Clock.System.now()
        }

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(
            success = true,
            data = UserResponse(
                id = updated[UsersTable.id],
                name = updated[UsersTable.name],
                email = updated[UsersTable.email],
                phone = updated[UsersTable.phone],
                role = updated[UsersTable.role],
                businessId = updated[UsersTable.businessId],
                preferredLanguage = updated[UsersTable.preferredLanguage]
            ),
            message = "User linked to business successfully"
        )
    }
}
