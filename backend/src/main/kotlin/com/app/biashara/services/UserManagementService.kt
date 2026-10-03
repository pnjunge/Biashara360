package com.app.biashara.services

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
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
    ): ApiResponse<UserResponse> = transaction {
        val business = BusinessesTable.select { BusinessesTable.id eq businessId }.singleOrNull()
            ?: return@transaction ApiResponse(false, message = "Business not found")
        val activeUsers = UsersTable.select { (UsersTable.businessId eq businessId) and (UsersTable.isActive eq true) }.count()
        if (activeUsers >= business[BusinessesTable.maxUsers]) {
            return@transaction ApiResponse(false, message = "User limit reached (${business[BusinessesTable.maxUsers]}). Upgrade your subscription to add more users.")
        }
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

        val rawPassword = req.password?.trim()?.takeIf { it.isNotBlank() } ?: "123456"
        if (rawPassword.length < 6) {
            return@transaction ApiResponse(false, message = "Password must be at least 6 characters")
        }

        val branchId = req.branchId?.trim()?.takeIf { it.isNotBlank() }
        if (branchId != null) {
            val branchExists = BranchesTable.select { (BranchesTable.id eq branchId) and (BranchesTable.businessId eq businessId) }.any()
            if (!branchExists) return@transaction ApiResponse(false, message = "Selected branch not found")
        }

        val now = Clock.System.now()
        val userId = generateId()

        UsersTable.insert {
            it[id] = userId
            it[UsersTable.businessId] = businessId
            it[UsersTable.branchId] = branchId
            it[name] = req.name.trim()
            it[UsersTable.email] = email
            it[UsersTable.phone] = phone
            it[passwordHash] = PasswordUtils.hash(rawPassword)
            it[role] = normalizedRole
            it[status] = "ACTIVE"
            it[createdByUserId] = callerUserId
            it[twoFactorEnabled] = false
            it[preferredLanguage] = "ENGLISH"
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }

        val allBranches = (req.branchIds + listOfNotNull(branchId)).distinct()
        for (bId in allBranches) {
            UserBranchesTable.insert {
                it[UserBranchesTable.userId] = userId
                it[UserBranchesTable.branchId] = bId
                it[isPrimary] = (bId == branchId)
            }
        }

        // Assign groups: use provided group(s) or default to Front
        val targetGroupIds = when {
            req.groupIds.isNotEmpty() -> req.groupIds
            !req.groupId.isNullOrBlank() -> listOf(req.groupId)
            else -> emptyList()
        }

        val assignedGroupNames = mutableListOf<String>()
        val assignedGroupIdsList = mutableListOf<String>()

        if (targetGroupIds.isNotEmpty()) {
            val validGroups = AccessGroupsTable.select {
                (AccessGroupsTable.businessId eq businessId) and (AccessGroupsTable.id inList targetGroupIds)
            }.map { it[AccessGroupsTable.id] to it[AccessGroupsTable.name] }

            for ((gId, gName) in validGroups) {
                UserAccessGroupsTable.insert {
                    it[UserAccessGroupsTable.userId] = userId
                    it[UserAccessGroupsTable.groupId] = gId
                }
                assignedGroupNames.add(gName)
                assignedGroupIdsList.add(gId)
            }
        }
        
        if (assignedGroupNames.isEmpty()) {
            val defaultGroup = AccessGroupsTable.select {
                (AccessGroupsTable.businessId eq businessId) and (AccessGroupsTable.isActive eq true)
            }.orderBy(AccessGroupsTable.createdAt, SortOrder.ASC).firstOrNull()

            val defaultGroupId = if (defaultGroup != null) {
                assignedGroupNames.add(defaultGroup[AccessGroupsTable.name])
                defaultGroup[AccessGroupsTable.id]
            } else {
                val newGroupId = generateId()
                AccessGroupsTable.insert {
                    it[id] = newGroupId
                    it[AccessGroupsTable.businessId] = businessId
                    it[name] = "Cashier"
                    it[description] = "Point of sale, open tabs, collections and customer sales"
                    it[allowedMenus] = "POS,OPEN_TABS,PAYMENTS,CARD_PAYMENTS,ORDERS,CUSTOMERS"
                    it[isActive] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }
                assignedGroupNames.add("Cashier")
                newGroupId
            }

            UserAccessGroupsTable.deleteWhere {
                (UserAccessGroupsTable.userId eq userId) and (UserAccessGroupsTable.groupId eq defaultGroupId)
            }
            UserAccessGroupsTable.insert {
                it[UserAccessGroupsTable.userId] = userId
                it[UserAccessGroupsTable.groupId] = defaultGroupId
            }
            assignedGroupIdsList.add(defaultGroupId)
        }

        val assignedRoleNames = mutableListOf<String>()
        val assignedRoleIdsList = mutableListOf<String>()

        if (req.roleIds.isNotEmpty()) {
            val validRoles = AccessRolesTable.select {
                (AccessRolesTable.businessId eq businessId) and (AccessRolesTable.id inList req.roleIds)
            }.map { it[AccessRolesTable.id] to it[AccessRolesTable.name] }

            for ((rId, rName) in validRoles) {
                UserAccessRolesTable.insert {
                    it[UserAccessRolesTable.userId] = userId
                    it[UserAccessRolesTable.roleId] = rId
                }
                assignedRoleNames.add(rName)
                assignedRoleIdsList.add(rId)
            }
        }

        val logDetails = buildString {
            append("Created user with role $normalizedRole in groups ${assignedGroupNames.joinToString()}")
            if (assignedRoleNames.isNotEmpty()) {
                append(" with roles ${assignedRoleNames.joinToString()}")
            }
        }
        auditLogService.logEvent(businessId, callerUserId, userId, "CREATE_USER", ipAddress, logDetails)

        val user = UserResponse(
            id = userId,
            name = req.name.trim(),
            email = email,
            phone = phone,
            role = normalizedRole,
            businessId = businessId,
            preferredLanguage = "ENGLISH",
            isActive = true,
            hasPinSet = false,
            assignedGroups = assignedGroupNames,
            assignedGroupIds = assignedGroupIdsList,
            assignedRoles = assignedRoleNames,
            assignedRoleIds = assignedRoleIdsList
        )
        ApiResponse(success = true, data = user, message = "User created successfully")
    }

    fun updateUserGroups(
        userId: String,
        businessId: String,
        groupIds: List<String>,
        callerUserId: String? = null,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val exists = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.any()
        if (!exists) return@transaction ApiResponse(false, message = "User not found")

        val validGroupIds = if (groupIds.isNotEmpty()) {
            AccessGroupsTable.select {
                (AccessGroupsTable.businessId eq businessId) and (AccessGroupsTable.id inList groupIds)
            }.map { it[AccessGroupsTable.id] }
        } else {
            emptyList()
        }

        UserAccessGroupsTable.deleteWhere {
            (UserAccessGroupsTable.userId eq userId)
        }
        for (gId in validGroupIds) {
            UserAccessGroupsTable.insert {
                it[UserAccessGroupsTable.userId] = userId
                it[UserAccessGroupsTable.groupId] = gId
            }
        }

        auditLogService.logEvent(businessId, callerUserId, userId, "UPDATE_USER_GROUPS", ipAddress, "Updated access groups for user")

        val updatedUser = UsersTable.select { UsersTable.id eq userId }.first().toUserResponse()
        ApiResponse(true, data = updatedUser, message = "User access groups updated")
    }

    fun updateUserRoles(
        userId: String,
        businessId: String,
        roleIds: List<String>,
        callerUserId: String? = null,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val exists = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.any()
        if (!exists) return@transaction ApiResponse(false, message = "User not found")

        val validRoleIds = if (roleIds.isNotEmpty()) {
            AccessRolesTable.select {
                (AccessRolesTable.businessId eq businessId) and (AccessRolesTable.id inList roleIds)
            }.map { it[AccessRolesTable.id] }
        } else {
            emptyList()
        }

        UserAccessRolesTable.deleteWhere {
            (UserAccessRolesTable.userId eq userId)
        }
        for (rId in validRoleIds) {
            UserAccessRolesTable.insert {
                it[UserAccessRolesTable.userId] = userId
                it[UserAccessRolesTable.roleId] = rId
            }
        }

        auditLogService.logEvent(businessId, callerUserId, userId, "UPDATE_USER_ROLES", ipAddress, "Updated direct access roles for user")

        val updatedUser = UsersTable.select { UsersTable.id eq userId }.first().toUserResponse()
        ApiResponse(true, data = updatedUser, message = "User access roles updated")
    }

    fun reassignUser(
        userId: String,
        businessId: String,
        callerUserId: String,
        callerRole: String,
        req: ReassignUserRequest,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val row = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "User not found")

        if (row[UsersTable.role] == "SUPERADMIN") {
            return@transaction ApiResponse(false, message = "Cannot modify a SUPERADMIN account")
        }

        val now = Clock.System.now()
        var roleChanged = false
        var targetRole = row[UsersTable.role]

        // 1. Account Role change if provided
        if (!req.role.isNullOrBlank()) {
            val normalizedRole = req.role.trim().uppercase()
            if (normalizedRole !in ASSIGNABLE_ROLES) {
                return@transaction ApiResponse(false, message = "Role must be one of: ${ASSIGNABLE_ROLES.joinToString()}")
            }
            if (normalizedRole != targetRole) {
                if (userId == callerUserId) {
                    return@transaction ApiResponse(false, message = "You cannot change your own role")
                }
                if (targetRole == "ADMIN" && normalizedRole != "ADMIN" && activeAdminCount(businessId) <= 1) {
                    return@transaction ApiResponse(false, message = "The business must retain at least one active administrator")
                }
                targetRole = normalizedRole
                roleChanged = true
            }
        }

        // 2. Business reassign if provided (Only SUPERADMIN can move across businesses)
        var newBusinessId = businessId
        if (!req.businessId.isNullOrBlank() && req.businessId != businessId) {
            if (callerRole != "SUPERADMIN") {
                return@transaction ApiResponse(false, message = "Only Superadmins can move users across businesses")
            }
            val targetBizExists = BusinessesTable.select { BusinessesTable.id eq req.businessId }.count() > 0
            if (!targetBizExists) {
                return@transaction ApiResponse(false, message = "Target business not found")
            }
            newBusinessId = req.businessId
        }

        // 3. Update Groups if provided
        val groupsUpdated = req.groupIds != null
        if (req.groupIds != null) {
            val validGroupIds = if (req.groupIds.isNotEmpty()) {
                AccessGroupsTable.select {
                    (AccessGroupsTable.businessId eq newBusinessId) and (AccessGroupsTable.id inList req.groupIds)
                }.map { it[AccessGroupsTable.id] }
            } else {
                emptyList()
            }
            UserAccessGroupsTable.deleteWhere { UserAccessGroupsTable.userId eq userId }
            for (gId in validGroupIds) {
                UserAccessGroupsTable.insert {
                    it[UserAccessGroupsTable.userId] = userId
                    it[UserAccessGroupsTable.groupId] = gId
                }
            }
        }

        // 4. Update Direct Access Roles if provided
        val rolesUpdated = req.roleIds != null
        if (req.roleIds != null) {
            val validRoleIds = if (req.roleIds.isNotEmpty()) {
                AccessRolesTable.select {
                    (AccessRolesTable.businessId eq newBusinessId) and (AccessRolesTable.id inList req.roleIds)
                }.map { it[AccessRolesTable.id] }
            } else {
                emptyList()
            }
            UserAccessRolesTable.deleteWhere { UserAccessRolesTable.userId eq userId }
            for (rId in validRoleIds) {
                UserAccessRolesTable.insert {
                    it[UserAccessRolesTable.userId] = userId
                    it[UserAccessRolesTable.roleId] = rId
                }
            }
        }

        // 5. Update user row if role, business, or branch changed
        val branchUpdated = req.branchId != null
        val targetBranchId = req.branchId?.trim()?.takeIf { it.isNotBlank() }
        if (branchUpdated && targetBranchId != null) {
            val branchExists = BranchesTable.select { (BranchesTable.id eq targetBranchId) and (BranchesTable.businessId eq newBusinessId) }.any()
            if (!branchExists) return@transaction ApiResponse(false, message = "Selected branch not found")
        }

        if (roleChanged || newBusinessId != businessId || branchUpdated) {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[role] = targetRole
                it[UsersTable.businessId] = newBusinessId
                if (branchUpdated) {
                    it[UsersTable.branchId] = targetBranchId
                }
                it[tokenValidAfter] = now
                it[updatedAt] = now
            }
            if (roleChanged || newBusinessId != businessId) {
                RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
            }
        }

        val logDetails = buildString {
            append("Reassigned user: ")
            if (roleChanged) append("Role -> $targetRole. ")
            if (groupsUpdated) append("Groups updated. ")
            if (rolesUpdated) append("Direct access roles updated. ")
            if (newBusinessId != businessId) append("Business -> $newBusinessId. ")
        }
        auditLogService.logEvent(businessId, callerUserId, userId, "REASSIGN_USER", ipAddress, logDetails.trim())

        val updated = UsersTable.select { UsersTable.id eq userId }.first().toUserResponse()
        ApiResponse(success = true, data = updated, message = "User reassigned successfully")
    }

    fun updateRole(
        userId: String,
        businessId: String,
        callerUserId: String,
        req: UpdateUserRoleRequest,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val normalizedRole = req.role.trim().uppercase()
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
        val newStatus = if (req.isActive) "ACTIVE" else "DISABLED"
        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[isActive] = req.isActive
            it[status] = newStatus
            it[tokenValidAfter] = now
            it[updatedAt] = now
        }
        if (!req.isActive) RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }

        auditLogService.logEvent(businessId, callerUserId, userId, if (req.isActive) "USER_ACTIVATED" else "USER_DEACTIVATED", ipAddress, null, "USER", userId)

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(success = true, data = updated.toUserResponse(), message = if (req.isActive) "User activated" else "User deactivated")
    }

    fun editUser(
        userId: String,
        businessId: String,
        callerUserId: String,
        req: EditUserRequest,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val row = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "User not found")

        val name = req.name.trim()
        val email = req.email.trim().lowercase()
        val phone = normalizeUserPhone(req.phone)

        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            return@transaction ApiResponse(false, message = "Name, email, and phone are required")
        }

        val emailInUse = UsersTable.select {
            (UsersTable.email.lowerCase() eq email) and (UsersTable.id neq userId)
        }.any()
        if (emailInUse) return@transaction ApiResponse(false, message = "Email already in use by another user")

        val phoneInUse = UsersTable.select {
            (UsersTable.phone eq phone) and (UsersTable.id neq userId)
        }.any()
        if (phoneInUse) return@transaction ApiResponse(false, message = "Phone number already in use by another user")

        val primaryBranchId = req.branchId?.trim()?.takeIf { it.isNotBlank() }
        if (primaryBranchId != null) {
            val branchExists = BranchesTable.select { (BranchesTable.id eq primaryBranchId) and (BranchesTable.businessId eq businessId) }.any()
            if (!branchExists) return@transaction ApiResponse(false, message = "Selected branch not found")
        }

        val now = Clock.System.now()
        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[UsersTable.name] = name
            it[UsersTable.email] = email
            it[UsersTable.phone] = phone
            it[UsersTable.branchId] = primaryBranchId
            if (!req.role.isNullOrBlank() && req.role.trim().uppercase() in ASSIGNABLE_ROLES) {
                it[role] = req.role.trim().uppercase()
            }
            it[updatedAt] = now
        }

        if (req.branchIds.isNotEmpty() || primaryBranchId != null) {
            val allBranches = (req.branchIds + listOfNotNull(primaryBranchId)).distinct()
            UserBranchesTable.deleteWhere { UserBranchesTable.userId eq userId }
            for (bId in allBranches) {
                UserBranchesTable.insert {
                    it[UserBranchesTable.userId] = userId
                    it[branchId] = bId
                    it[isPrimary] = (bId == primaryBranchId)
                }
            }
        }

        if (req.roleIds.isNotEmpty()) {
            val validRoles = AccessRolesTable.select {
                (AccessRolesTable.businessId eq businessId) and (AccessRolesTable.id inList req.roleIds)
            }.map { it[AccessRolesTable.id] }
            UserAccessRolesTable.deleteWhere { UserAccessRolesTable.userId eq userId }
            for (rId in validRoles) {
                UserAccessRolesTable.insert {
                    it[UserAccessRolesTable.userId] = userId
                    it[roleId] = rId
                }
            }
        }

        auditLogService.logEvent(businessId, callerUserId, userId, "USER_UPDATE", ipAddress, "Edited profile for $name", "USER", userId)

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(true, data = updated.toUserResponse(), message = "User updated successfully")
    }

    fun unlockAccount(
        userId: String,
        businessId: String,
        callerUserId: String,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val row = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "User not found")

        val now = Clock.System.now()
        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[pinFailedAttempts] = 0
            it[pinLockedUntil] = null
            it[status] = "ACTIVE"
            it[updatedAt] = now
        }

        auditLogService.logEvent(businessId, callerUserId, userId, "USER_UNLOCK", ipAddress, "Unlocked user account from locked state", "USER", userId)

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(true, data = updated.toUserResponse(), message = "User account unlocked successfully")
    }

    fun adminResetPassword(
        userId: String,
        businessId: String,
        callerUserId: String,
        req: AdminResetPasswordRequest,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val newPassword = req.newPassword.trim()
        if (newPassword.length < 6) {
            return@transaction ApiResponse(false, message = "Password must be at least 6 characters")
        }

        val row = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "User not found")

        val now = Clock.System.now()
        UsersTable.update({ (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }) {
            it[passwordHash] = PasswordUtils.hash(newPassword)
            it[tokenValidAfter] = now
            it[updatedAt] = now
        }
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }

        auditLogService.logEvent(businessId, callerUserId, userId, "PASSWORD_RESET", ipAddress, "Administrator reset user password", "USER", userId)

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(true, data = updated.toUserResponse(), message = "Password reset successfully")
    }

    fun assignUserBranches(
        userId: String,
        businessId: String,
        callerUserId: String,
        req: AssignUserBranchesRequest,
        ipAddress: String? = null
    ): ApiResponse<UserResponse> = transaction {
        val row = UsersTable.select {
            (UsersTable.id eq userId) and (UsersTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "User not found")

        val validBranches = BranchesTable.select {
            (BranchesTable.businessId eq businessId) and (BranchesTable.id inList req.branchIds)
        }.map { it[BranchesTable.id] }

        val primary = req.primaryBranchId?.takeIf { it in validBranches } ?: validBranches.firstOrNull()

        UserBranchesTable.deleteWhere { UserBranchesTable.userId eq userId }
        for (bId in validBranches) {
            UserBranchesTable.insert {
                it[UserBranchesTable.userId] = userId
                it[branchId] = bId
                it[isPrimary] = (bId == primary)
            }
        }

        val now = Clock.System.now()
        UsersTable.update({ UsersTable.id eq userId }) {
            it[branchId] = primary
            it[updatedAt] = now
        }

        auditLogService.logEvent(businessId, callerUserId, userId, "ASSIGN_USER_BRANCHES", ipAddress, "Assigned ${validBranches.size} branches", "USER", userId)

        val updated = UsersTable.select { UsersTable.id eq userId }.first()
        ApiResponse(true, data = updated.toUserResponse(), message = "Branches assigned successfully")
    }

    fun getUserActivity(userId: String, businessId: String): List<AuditLogResponse> {
        return auditLogService.getUserActivity(businessId, userId)
    }

    private fun ResultRow.toUserResponse(): UserResponse {
        val userId = this[UsersTable.id]
        val businessId = this[UsersTable.businessId] ?: ""
        val now = Clock.System.now()
        val groups = (UserAccessGroupsTable innerJoin AccessGroupsTable)
            .slice(AccessGroupsTable.id, AccessGroupsTable.name)
            .select {
                (UserAccessGroupsTable.userId eq userId) and
                    (AccessGroupsTable.businessId eq businessId) and
                    (AccessGroupsTable.isActive eq true)
            }
        val roles = (UserAccessRolesTable innerJoin AccessRolesTable)
            .slice(AccessRolesTable.id, AccessRolesTable.name)
            .select {
                (UserAccessRolesTable.userId eq userId) and
                    (AccessRolesTable.businessId eq businessId) and
                    (AccessRolesTable.isActive eq true)
            }
        val permissions = (UserAccessRolesTable innerJoin AccessRolesTable innerJoin RolePermissionsTable innerJoin PermissionsTable)
            .slice(PermissionsTable.code)
            .select {
                (UserAccessRolesTable.userId eq userId) and
                    (AccessRolesTable.businessId eq businessId) and
                    (AccessRolesTable.isActive eq true)
            }.map { it[PermissionsTable.code] }.distinct()

        val branchId = this[UsersTable.branchId]
        val branchName = branchId?.let { bId ->
            BranchesTable.slice(BranchesTable.name)
                .select { (BranchesTable.id eq bId) and (BranchesTable.businessId eq businessId) }
                .firstOrNull()?.get(BranchesTable.name)
        }

        val userBranchRows = (UserBranchesTable innerJoin BranchesTable)
            .slice(BranchesTable.id, BranchesTable.name)
            .select { (UserBranchesTable.userId eq userId) and (BranchesTable.businessId eq businessId) }
            .toList()

        val assignedBranchIds = userBranchRows.map { it[BranchesTable.id] }
        val assignedBranchNames = userBranchRows.map { it[BranchesTable.name] }

        val pinLockedUntil = this[UsersTable.pinLockedUntil]
        val isPinLocked = pinLockedUntil != null && pinLockedUntil > now
        val rawStatus = this[UsersTable.status]
        val resolvedStatus = when {
            isPinLocked -> "LOCKED"
            !this[UsersTable.isActive] || rawStatus.equals("DISABLED", ignoreCase = true) -> "DISABLED"
            else -> "ACTIVE"
        }

        return UserResponse(
            id = userId,
            name = this[UsersTable.name],
            email = this[UsersTable.email],
            phone = this[UsersTable.phone],
            role = this[UsersTable.role],
            businessId = this[UsersTable.businessId],
            preferredLanguage = this[UsersTable.preferredLanguage],
            isActive = this[UsersTable.isActive],
            status = resolvedStatus,
            lastLoginAt = this[UsersTable.lastLoginAt]?.toString(),
            hasPinSet = this[UsersTable.loginPinHash] != null,
            isPinLocked = isPinLocked,
            pinLockedUntil = pinLockedUntil?.toString(),
            assignedGroups = groups.map { it[AccessGroupsTable.name] },
            assignedGroupIds = groups.map { it[AccessGroupsTable.id] },
            assignedRoles = roles.map { it[AccessRolesTable.name] },
            assignedRoleIds = roles.map { it[AccessRolesTable.id] },
            permissions = permissions,
            branchId = branchId,
            branchName = branchName,
            assignedBranchIds = assignedBranchIds,
            assignedBranchNames = assignedBranchNames,
            createdAt = this[UsersTable.createdAt].toString(),
            updatedAt = this[UsersTable.updatedAt].toString()
        )
    }

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
