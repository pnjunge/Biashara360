package com.app.biashara.services

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.auth.generateId
import com.app.biashara.db.UsersTable
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

// Roles that a business ADMIN is allowed to assign
private val ASSIGNABLE_ROLES = setOf("ADMIN", "STAFF")

class UserManagementService {

    fun listUsers(businessId: String): List<UserResponse> = transaction {
        UsersTable.select { UsersTable.businessId eq businessId }
            .orderBy(UsersTable.createdAt, SortOrder.ASC)
            .map { it.toUserResponse() }
    }

    fun inviteUser(businessId: String, req: InviteUserRequest): ApiResponse<UserResponse> = transaction {
        if (req.name.isBlank() || req.email.isBlank() || req.phone.isBlank()) {
            return@transaction ApiResponse(false, message = "Name, email, and phone are required")
        }
        if (req.password.length < 6) {
            return@transaction ApiResponse(false, message = "Password must be at least 6 characters")
        }
        val normalizedRole = req.role.uppercase()
        if (normalizedRole !in ASSIGNABLE_ROLES) {
            return@transaction ApiResponse(false, message = "Role must be one of: ${ASSIGNABLE_ROLES.joinToString()}")
        }

        // Reject if email or phone already taken (globally)
        val emailExists = UsersTable.select { UsersTable.email eq req.email }.count() > 0
        if (emailExists) return@transaction ApiResponse(false, message = "Email already registered")

        val phoneExists = UsersTable.select { UsersTable.phone eq req.phone }.count() > 0
        if (phoneExists) return@transaction ApiResponse(false, message = "Phone number already registered")

        val now = Clock.System.now()
        val userId = generateId()

        UsersTable.insert {
            it[id]                       = userId
            it[UsersTable.businessId]    = businessId
            it[name]                     = req.name
            it[email]                    = req.email
            it[phone]                    = req.phone
            it[passwordHash]             = PasswordUtils.hash(req.password)
            it[role]                     = normalizedRole
            it[twoFactorEnabled]         = false
            it[preferredLanguage]        = "ENGLISH"
            it[isActive]                 = true
            it[createdAt]                = now
            it[updatedAt]                = now
        }

        val user = UserResponse(userId, req.name, req.email, req.phone, normalizedRole, businessId, "ENGLISH")
        ApiResponse(success = true, data = user, message = "User created successfully")
    }

    fun updateRole(userId: String, businessId: String, req: UpdateUserRoleRequest): ApiResponse<UserResponse> = transaction {
        val normalizedRole = req.role.uppercase()
        if (normalizedRole !in ASSIGNABLE_ROLES) {
            return@transaction ApiResponse(false, message = "Role must be one of: ${ASSIGNABLE_ROLES.joinToString()}")
        }

        val row = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "User not found")

        // Prevent changing your own role or the SUPERADMIN role
        if (row[UsersTable.role] == "SUPERADMIN") {
            return@transaction ApiResponse(false, message = "Cannot modify a SUPERADMIN account")
        }

        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[role]      = normalizedRole
            it[updatedAt] = Clock.System.now()
        }

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(success = true, data = updated.toUserResponse(), message = "Role updated")
    }

    fun setActiveStatus(userId: String, businessId: String, req: UpdateUserStatusRequest): ApiResponse<UserResponse> = transaction {
        val row = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "User not found")

        if (row[UsersTable.role] == "SUPERADMIN") {
            return@transaction ApiResponse(false, message = "Cannot modify a SUPERADMIN account")
        }

        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[isActive]  = req.isActive
            it[updatedAt] = Clock.System.now()
        }

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(success = true, data = updated.toUserResponse(), message = if (req.isActive) "User activated" else "User deactivated")
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun ResultRow.toUserResponse() = UserResponse(
        id = this[UsersTable.id],
        name = this[UsersTable.name],
        email = this[UsersTable.email],
        phone = this[UsersTable.phone],
        role = this[UsersTable.role],
        businessId = this[UsersTable.businessId],
        preferredLanguage = this[UsersTable.preferredLanguage]
    )
}
