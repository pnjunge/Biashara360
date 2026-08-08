package com.app.biashara.services

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.auth.generateId
import com.app.biashara.db.UsersTable
import com.app.biashara.db.RefreshTokensTable
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

private val ASSIGNABLE_ROLES = setOf("ADMIN", "MANAGER", "STAFF")

class UserManagementService(
    private val authService: AuthService,
    private val auditLogService: AuditLogService
) {

    fun listUsers(businessId: String): List<UserResponse> = transaction {
        UsersTable.select { UsersTable.businessId eq businessId }
            .orderBy(UsersTable.createdAt, SortOrder.ASC)
            .map { it.toUserResponse() }
    }

    fun setStaffPin(
        userId: String,
        businessId: String,
        callerUserId: String,
        req: AdminSetStaffPinRequest,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val pin = req.pin.trim()
        if (pin.length !in 4..6 || !pin.all { it.isDigit() }) {
            return@transaction ApiResponse(false, message = "PIN must be a 4 to 6 digit numeric code")
        }

        val exists = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.count() > 0
        if (!exists) return@transaction ApiResponse(false, message = "User not found")

        val now = Clock.System.now()
        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[loginPinHash] = PasswordUtils.hash(pin)
            it[pinFailedAttempts] = 0
            it[pinLockedUntil] = null
            it[tokenValidAfter] = now
            it[updatedAt] = now
        }

        auditLogService.logEvent(businessId, callerUserId, userId, "SET_STAFF_PIN", ipAddress, "Assigned new POS quick-switch PIN")

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(success = true, data = updated.toUserResponse(), message = "Staff PIN assigned successfully")
    }

    fun removeStaffPin(
        userId: String,
        businessId: String,
        callerUserId: String,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val exists = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.count() > 0
        if (!exists) return@transaction ApiResponse(false, message = "User not found")

        val now = Clock.System.now()
        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[loginPinHash] = null
            it[pinFailedAttempts] = 0
            it[pinLockedUntil] = null
            it[tokenValidAfter] = now
            it[updatedAt] = now
        }

        auditLogService.logEvent(businessId, callerUserId, userId, "REMOVE_STAFF_PIN", ipAddress, "Cleared POS quick-switch PIN")

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(success = true, data = updated.toUserResponse(), message = "Staff PIN removed")
    }

    fun inviteUser(
        businessId: String,
        req: InviteUserRequest,
        callerUserId: String? = null,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> {
        val result = transaction {
            if (req.name.isBlank() || req.email.isBlank() || req.phone.isBlank()) {
                return@transaction ApiResponse(false, message = "Name, email, and phone are required")
            }
            val normalizedRole = req.role.trim().uppercase()
            if (normalizedRole !in ASSIGNABLE_ROLES) {
                return@transaction ApiResponse(false, message = "Role must be one of: ${ASSIGNABLE_ROLES.joinToString()}")
            }

            val email = req.email.trim().lowercase()
            val phone = normalizeUserPhone(req.phone)
            val emailExists = UsersTable.select { UsersTable.email.lowerCase() eq email }.count() > 0
            if (emailExists) return@transaction ApiResponse(false, message = "Email already registered")

            val phoneExists = UsersTable.select { UsersTable.phone eq phone }.count() > 0
            if (phoneExists) return@transaction ApiResponse(false, message = "Phone number already registered")

            val now = Clock.System.now()
            val userId = generateId()
            val unguessablePassword = buildString(48) {
                val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#%&*"
                val random = java.security.SecureRandom()
                repeat(48) { append(alphabet[random.nextInt(alphabet.length)]) }
            }

            UsersTable.insert {
                it[id] = userId
                it[UsersTable.businessId] = businessId
                it[name] = req.name.trim()
                it[UsersTable.email] = email
                it[UsersTable.phone] = phone
                it[passwordHash] = PasswordUtils.hash(unguessablePassword)
                it[role] = normalizedRole
                it[twoFactorEnabled] = false
                it[preferredLanguage] = "ENGLISH"
                it[isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }

            auditLogService.logEvent(businessId, callerUserId, userId, "INVITE_USER", ipAddress, "Invited user with role $normalizedRole")

            val user = UserResponse(userId, req.name.trim(), email, phone, normalizedRole, businessId, "ENGLISH", isActive = true)
            ApiResponse(success = true, data = user)
        }

        if (!result.success) return result
        val invitation = authService.requestPasswordReset(result.data!!.email, invitation = true)
        if (!invitation.success) {
            result.data.id.let { userId ->
                transaction {
                    com.app.biashara.db.OtpTable.deleteWhere {
                        com.app.biashara.db.OtpTable.userId eq userId
                    }
                    UsersTable.deleteWhere { UsersTable.id eq userId }
                }
            }
            return ApiResponse(false, message = invitation.message)
        }
        return result.copy(message = "Invitation sent. The reset code expires in 10 minutes.")
    }

    fun updateRole(
        userId: String,
        businessId: String,
        callerUserId: String,
        req: UpdateUserRoleRequest,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val normalizedRole = req.role.uppercase()
        if (normalizedRole !in ASSIGNABLE_ROLES) {
            return@transaction ApiResponse(false, message = "Role must be one of: ${ASSIGNABLE_ROLES.joinToString()}")
        }

        val row = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "User not found")

        if (userId == callerUserId) return@transaction ApiResponse(false, message = "You cannot change your own role")
        if (row[UsersTable.role] == "SUPERADMIN") {
            return@transaction ApiResponse(false, message = "Cannot modify a SUPERADMIN account")
        }
        if (row[UsersTable.role] == "ADMIN" && normalizedRole != "ADMIN" && activeAdminCount(businessId) <= 1) {
            return@transaction ApiResponse(false, message = "The business must retain at least one active administrator")
        }

        val now = Clock.System.now()
        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[role] = normalizedRole
            it[tokenValidAfter] = now
            it[updatedAt] = now
        }
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }

        auditLogService.logEvent(businessId, callerUserId, userId, "UPDATE_ROLE", ipAddress, "Role updated to $normalizedRole")

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(success = true, data = updated.toUserResponse(), message = "Role updated")
    }

    fun setActiveStatus(
        userId: String,
        businessId: String,
        callerUserId: String,
        req: UpdateUserStatusRequest,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val row = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "User not found")

        if (userId == callerUserId) return@transaction ApiResponse(false, message = "You cannot change your own account status")
        if (row[UsersTable.role] == "SUPERADMIN") {
            return@transaction ApiResponse(false, message = "Cannot modify a SUPERADMIN account")
        }
        if (!req.isActive && row[UsersTable.role] == "ADMIN" && activeAdminCount(businessId) <= 1) {
            return@transaction ApiResponse(false, message = "The business must retain at least one active administrator")
        }

        val now = Clock.System.now()
        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[isActive] = req.isActive
            it[tokenValidAfter] = now
            it[updatedAt] = now
        }
        if (!req.isActive) RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }

        auditLogService.logEvent(businessId, callerUserId, userId, if (req.isActive) "USER_ACTIVATED" else "USER_DEACTIVATED", ipAddress)

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(success = true, data = updated.toUserResponse(), message = if (req.isActive) "User activated" else "User deactivated")
    }

    private fun ResultRow.toUserResponse() = UserResponse(
        id = this[UsersTable.id],
        name = this[UsersTable.name],
        email = this[UsersTable.email],
        phone = this[UsersTable.phone],
        role = this[UsersTable.role],
        businessId = this[UsersTable.businessId],
        preferredLanguage = this[UsersTable.preferredLanguage],
        isActive = this[UsersTable.isActive],
        hasPinSet = this[UsersTable.loginPinHash] != null
    )

    private fun activeAdminCount(businessId: String) = UsersTable.select {
        (UsersTable.businessId eq businessId) and (UsersTable.role eq "ADMIN") and (UsersTable.isActive eq true)
    }.count()

    private fun normalizeUserPhone(value: String): String {
        val phone = value.trim().replace(Regex("[\\s()-]"), "")
        return when {
            phone.startsWith("+254") -> phone.drop(1)
            phone.startsWith("07") || phone.startsWith("01") -> "254${phone.drop(1)}"
            else -> phone
        }
    }
}
