package com.app.biashara.services

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.auth.generateId
import com.app.biashara.db.BusinessesTable
import com.app.biashara.db.UsersTable
import com.app.biashara.models.*
import com.app.biashara.utils.ValidationUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class SuperAdminService(private val auditLogService: AuditLogService? = null) {

    private fun parseInstantOrDate(value: String): Instant? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        return runCatching { Instant.parse(trimmed) }.getOrNull()
            ?: runCatching { Instant.parse("${trimmed.take(10)}T23:59:59Z") }.getOrNull()
    }

    private fun rowToBusinessResponse(row: ResultRow, now: Instant = Clock.System.now()): BusinessResponse {
        val validUntil = row[BusinessesTable.subscriptionValidUntil]
        val diffSeconds = validUntil?.let { v -> v.epochSeconds - now.epochSeconds }
        val daysRemaining = if (diffSeconds != null) {
            if (diffSeconds <= 0) 0L else (diffSeconds + 86399) / 86400
        } else null
        val isExpired = validUntil != null && now > validUntil

        return BusinessResponse(
            id               = row[BusinessesTable.id],
            name             = row[BusinessesTable.name],
            type             = row[BusinessesTable.type],
            ownerPhone       = row[BusinessesTable.ownerPhone],
            ownerEmail       = row[BusinessesTable.ownerEmail],
            subscriptionTier = row[BusinessesTable.subscriptionTier],
            subscriptionEnabled = row[BusinessesTable.subscriptionEnabled],
            isActive         = row[BusinessesTable.isActive],
            createdAt        = row[BusinessesTable.createdAt].toString(),
            isTrial          = row[BusinessesTable.isTrial],
            subscriptionValidUntil = validUntil?.toString(),
            daysRemaining    = daysRemaining,
            isExpired        = isExpired
        )
    }

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

        val isTrial = req.isTrial
        val trialDays = req.trialDays.coerceIn(1, 365)
        val validUntil = if (isTrial) now + trialDays.days else null
        val tier = if (isTrial) "TRIAL" else "FREEMIUM"

        BusinessesTable.insert {
            it[id]               = businessId
            it[name]             = req.businessName
            it[storefrontSlug]   = allocateStorefrontSlug(req.businessName, businessId)
            it[type]             = req.businessType
            it[ownerPhone]       = ValidationUtils.normalizePhoneKE(req.adminPhone)
            it[ownerEmail]       = req.adminEmail
            it[currency]         = "KES"
            it[subscriptionTier] = tier
            it[subscriptionEnabled] = true
            it[BusinessesTable.isTrial] = isTrial
            it[subscriptionValidUntil] = validUntil
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

        val inserted = BusinessesTable.select { BusinessesTable.id eq businessId }.first()
        val businessResp = rowToBusinessResponse(inserted, now)
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

        val isTrial = req.isTrial
        val trialDays = req.trialDays.coerceIn(1, 365)
        val validUntil = if (isTrial) now + trialDays.days else null
        val tier = if (isTrial) "TRIAL" else "FREEMIUM"

        BusinessesTable.insert {
            it[id]               = businessId
            it[name]             = req.businessName
            it[storefrontSlug]   = allocateStorefrontSlug(req.businessName, businessId)
            it[type]             = req.businessType.uppercase()
            it[ownerPhone]       = ""
            it[ownerEmail]       = ""
            it[currency]         = "KES"
            it[subscriptionTier] = tier
            it[subscriptionEnabled] = true
            it[BusinessesTable.isTrial] = isTrial
            it[subscriptionValidUntil] = validUntil
            it[enabledModules]   = "INVENTORY,SALES,CRM,EXPENSES,PAYMENTS,REPORTS"
            it[createdAt]        = now
            it[updatedAt]        = now
        }

        val inserted = BusinessesTable.select { BusinessesTable.id eq businessId }.first()
        ApiResponse(
            success = true,
            data = rowToBusinessResponse(inserted, now),
            message = "Business created successfully"
        )
    }

    fun listBusinesses(): List<BusinessResponse> = transaction {
        val now = Clock.System.now()
        BusinessesTable
            .select { BusinessesTable.type neq "SYSTEM" }
            .orderBy(BusinessesTable.createdAt, SortOrder.DESC)
            .map { rowToBusinessResponse(it, now) }
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
            data = rowToBusinessResponse(updated),
            message = if (req.isActive) "Business activated" else "Business deactivated"
        )
    }

    fun updateSubscription(
        businessId: String,
        req: UpdateSubscriptionRequest,
        actorUserId: String? = null
    ): ApiResponse<BusinessResponse> = transaction {
        if (!ValidationUtils.isValidUUID(businessId)) {
            return@transaction ApiResponse(false, message = "Invalid business ID format")
        }
        val normalizedTier = req.tier?.trim()?.uppercase()
        if (normalizedTier != null && normalizedTier !in setOf("FREEMIUM", "TRIAL", "PREMIUM")) {
            return@transaction ApiResponse(false, message = "Subscription tier must be FREEMIUM, TRIAL, or PREMIUM")
        }
        val current = BusinessesTable.select {
            (BusinessesTable.id eq businessId) and (BusinessesTable.type neq "SYSTEM")
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Business not found")

        val now = Clock.System.now()
        val currentUntil = current[BusinessesTable.subscriptionValidUntil]
        var targetUntil = currentUntil

        if (req.extendDays != null && req.extendDays > 0) {
            val base = currentUntil?.takeIf { it > now } ?: now
            targetUntil = base + req.extendDays.days
        } else if (req.validUntil != null) {
            targetUntil = parseInstantOrDate(req.validUntil)
        } else if ((normalizedTier == "TRIAL" || req.isTrial == true) && currentUntil == null) {
            targetUntil = now + 14.days
        }

        val targetIsTrial = when {
            req.isTrial != null -> req.isTrial
            normalizedTier == "TRIAL" -> true
            normalizedTier in setOf("FREEMIUM", "PREMIUM") -> false
            else -> current[BusinessesTable.isTrial]
        }

        BusinessesTable.update({ BusinessesTable.id eq businessId }) {
            it[subscriptionEnabled] = req.enabled
            if (normalizedTier != null) it[subscriptionTier] = normalizedTier
            it[BusinessesTable.isTrial] = targetIsTrial
            it[subscriptionValidUntil] = targetUntil
            if (req.maxUsers != null && req.maxUsers > 0) it[maxUsers] = req.maxUsers
            it[updatedAt] = now
        }

        val updated = BusinessesTable.select { BusinessesTable.id eq businessId }.first()
        val resp = rowToBusinessResponse(updated, now)

        auditLogService?.logEvent(
            businessId = businessId,
            actorUserId = actorUserId,
            action = "SUBSCRIPTION_UPDATED",
            details = "Tier: ${resp.subscriptionTier}, Enabled: ${resp.subscriptionEnabled}, Trial: ${resp.isTrial}, ValidUntil: ${resp.subscriptionValidUntil ?: "None"}, DaysRemaining: ${resp.daysRemaining ?: "N/A"}. Note: ${req.note ?: ""}"
        )

        ApiResponse(
            success = true,
            data = resp,
            message = if (req.extendDays != null) "Subscription extended by ${req.extendDays} days" else "Subscription updated"
        )
    }

    fun extendSubscription(
        businessId: String,
        req: ExtendSubscriptionRequest,
        actorUserId: String? = null
    ): ApiResponse<BusinessResponse> = transaction {
        if (!ValidationUtils.isValidUUID(businessId)) {
            return@transaction ApiResponse(false, message = "Invalid business ID format")
        }
        if (req.extendDays <= 0 || req.extendDays > 3650) {
            return@transaction ApiResponse(false, message = "Extension period must be between 1 and 3650 days")
        }
        val normalizedTier = req.tier?.trim()?.uppercase()
        if (normalizedTier != null && normalizedTier !in setOf("FREEMIUM", "TRIAL", "PREMIUM")) {
            return@transaction ApiResponse(false, message = "Subscription tier must be FREEMIUM, TRIAL, or PREMIUM")
        }

        val current = BusinessesTable.select {
            (BusinessesTable.id eq businessId) and (BusinessesTable.type neq "SYSTEM")
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Business not found")

        val now = Clock.System.now()
        val currentUntil = current[BusinessesTable.subscriptionValidUntil]
        val base = currentUntil?.takeIf { it > now } ?: now
        val newValidUntil = base + req.extendDays.days

        val targetIsTrial = req.isTrial ?: (if (normalizedTier == "TRIAL") true else current[BusinessesTable.isTrial])

        BusinessesTable.update({ BusinessesTable.id eq businessId }) {
            it[subscriptionEnabled] = true
            it[subscriptionValidUntil] = newValidUntil
            it[BusinessesTable.isTrial] = targetIsTrial
            if (normalizedTier != null) it[subscriptionTier] = normalizedTier
            it[updatedAt] = now
        }

        val updated = BusinessesTable.select { BusinessesTable.id eq businessId }.first()
        val resp = rowToBusinessResponse(updated, now)

        auditLogService?.logEvent(
            businessId = businessId,
            actorUserId = actorUserId,
            action = "SUBSCRIPTION_EXTENDED",
            details = "Extended by ${req.extendDays} days. Valid until: $newValidUntil. Trial: $targetIsTrial. Note: ${req.note ?: ""}"
        )

        val periodType = if (targetIsTrial) "Trial period" else "Subscription"
        ApiResponse(
            success = true,
            data = resp,
            message = "$periodType extended by ${req.extendDays} days until ${newValidUntil.toString().substringBefore('T')}"
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
