package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

val BUSINESS_MENUS = listOf(
    MenuDefinition("DASHBOARD", "Dashboard"), MenuDefinition("POS", "Point of Sale"),
    MenuDefinition("HOSPITALITY", "Bar & Restaurant"),
    MenuDefinition("HOSPITALITY_OPS", "Hospitality Operations"),
    MenuDefinition("SERVICES", "Appointments & Services"),
    MenuDefinition("OPEN_TABS", "Open Tabs"),
    MenuDefinition("INVENTORY", "Inventory"), MenuDefinition("PURCHASES", "Purchases"),
    MenuDefinition("ORDERS", "Orders"),
    MenuDefinition("CUSTOMERS", "Customers"), MenuDefinition("EXPENSES", "Expenses"),
    MenuDefinition("PAYMENTS", "M-Pesa Payments"), MenuDefinition("CARD_PAYMENTS", "Card Payments"),
    MenuDefinition("TAX", "Tax"), MenuDefinition("KRA", "KRA iTax"),
    MenuDefinition("SOCIAL", "Social Inbox"), MenuDefinition("SOCIAL_SETUP", "Social Setup"),
    MenuDefinition("USERS", "Users, Roles & Groups"),
    MenuDefinition("AUDIT_LOG", "Audit Log"),
    MenuDefinition("REPORTS", "Reports"),
    MenuDefinition("DOWNLOADS", "Download Apps"), MenuDefinition("SETTINGS", "Settings")
)
private val MENU_KEYS = BUSINESS_MENUS.map { it.key }.toSet()
private val DEFAULT_STAFF_MENUS = MENU_KEYS - setOf("USERS", "AUDIT_LOG", "SETTINGS")

class AccessControlService(
    private val auditLogService: AuditLogService? = null
) {
    fun config(businessId: String): AccessConfigResponse = transaction {
        ensureDefaults(businessId)
        AccessConfigResponse(
            menus = BUSINESS_MENUS,
            enabledMenus = businessMenus(businessId),
            roles = roles(businessId),
            groups = groups(businessId),
            permissions = listPermissions()
        )
    }

    private fun ensureDefaults(businessId: String) {
        val now = Clock.System.now()
        val existingGroups = AccessGroupsTable.select { AccessGroupsTable.businessId eq businessId }.count()
        if (existingGroups == 0L) {
            val defaults = listOf(
                Triple("Cashier", "Point of sale, open tabs, collections and customer sales", listOf("POS", "OPEN_TABS", "PAYMENTS", "CARD_PAYMENTS", "ORDERS", "CUSTOMERS")),
                Triple("Supervisor", "Floor operations, bar/restaurant supervision, orders and reports", listOf("HOSPITALITY", "OPEN_TABS", "HOSPITALITY_OPS", "ORDERS", "REPORTS")),
                Triple("Storekeeper", "Inventory stock tracking, purchases, and operating expenses", listOf("INVENTORY", "PURCHASES", "EXPENSES")),
                Triple("Manager", "Full business operations, reports, accounting and store settings", listOf("DASHBOARD", "POS", "HOSPITALITY", "HOSPITALITY_OPS", "SERVICES", "OPEN_TABS", "INVENTORY", "PURCHASES", "ORDERS", "CUSTOMERS", "EXPENSES", "PAYMENTS", "CARD_PAYMENTS", "TAX", "KRA", "REPORTS", "DOWNLOADS", "SETTINGS"))
            )
            for ((gName, gDesc, gMenus) in defaults) {
                val groupId = generateId()
                AccessGroupsTable.insert {
                    it[id] = groupId
                    it[AccessGroupsTable.businessId] = businessId
                    it[name] = gName
                    it[description] = gDesc
                    it[allowedMenus] = gMenus.joinToString(",")
                    it[isActive] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
    }

    fun myMenus(businessId: String, userId: String, builtInRole: String): MyMenuAccessResponse = transaction {
        val enabled = businessMenus(businessId).toSet()
        val roleIds = effectiveRoleIds(businessId, userId)
        val userPerms = if (builtInRole in setOf("ADMIN", "BUSINESS_ADMIN", "SUPERADMIN")) {
            PermissionsTable.selectAll().map { it[PermissionsTable.code] }
        } else permissionsForRoles(roleIds)

        if (builtInRole in setOf("ADMIN", "BUSINESS_ADMIN", "SUPERADMIN")) {
            return@transaction MyMenuAccessResponse(enabled.toList(), userPerms)
        }

        // 1. Direct rights assigned to the user's groups
        val directGroupMenus = (UserAccessGroupsTable innerJoin AccessGroupsTable)
            .slice(AccessGroupsTable.allowedMenus)
            .select {
                (UserAccessGroupsTable.userId eq userId) and
                    (AccessGroupsTable.businessId eq businessId) and
                    (AccessGroupsTable.isActive eq true)
            }.flatMap { csv(it[AccessGroupsTable.allowedMenus]) }

        val legacyRoleMenus = AccessRolesTable.select { AccessRolesTable.id inList roleIds }
            .flatMap { csv(it[AccessRolesTable.allowedMenus]) }
        val hasAssignments = UserAccessGroupsTable.select { UserAccessGroupsTable.userId eq userId }.any() ||
            UserAccessRolesTable.select { UserAccessRolesTable.userId eq userId }.any()

        val allAssigned = (directGroupMenus + legacyRoleMenus).distinct()
        val allowed = if (!hasAssignments) DEFAULT_STAFF_MENUS else allAssigned.toSet()
        MyMenuAccessResponse((enabled intersect allowed).sorted(), userPerms)
    }

    fun updateMenus(businessId: String, request: UpdateMenusRequest): AccessConfigResponse = transaction {
        val menus = validateMenus(request.enabledMenus)
        BusinessesTable.update({ BusinessesTable.id eq businessId }) { it[enabledMenus] = menus.joinToString(",") }
        auditLogService?.logEvent(businessId, null, null, "UPDATE_BUSINESS_MENUS", null, "Updated enabled menus (${menus.size} menus active)")
        config(businessId)
    }

    fun createRole(businessId: String, request: SaveAccessRoleRequest): AccessRoleResponse = transaction {
        require(request.name.trim().length in 2..80) { "Role name must be between 2 and 80 characters" }
        require(AccessRolesTable.select { AccessRolesTable.businessId eq businessId }.none { it[AccessRolesTable.name].equals(request.name.trim(), ignoreCase = true) }) { "A role with this name already exists" }
        val id = generateId(); val now = Clock.System.now(); val menus = validateMenus(request.allowedMenus)
        AccessRolesTable.insert { row ->
            row[AccessRolesTable.id] = id
            row[AccessRolesTable.businessId] = businessId
            row[name] = request.name.trim()
            row[description] = request.description.trim().take(255)
            row[allowedMenus] = menus.joinToString(",")
            row[isActive] = request.isActive
            row[createdAt] = now
            row[updatedAt] = now
        }
        val validPerms = if (request.permissions.isNotEmpty()) {
            PermissionsTable.select { PermissionsTable.code inList request.permissions }
                .map { it[PermissionsTable.id] to it[PermissionsTable.code] }
        } else emptyList()

        for ((pId, _) in validPerms) {
            RolePermissionsTable.insert {
                it[roleId] = id
                it[permissionId] = pId
            }
        }
        auditLogService?.logEvent(businessId, null, null, "CREATE_ACCESS_ROLE", null, "Created access role ${request.name.trim()} with ${validPerms.size} permissions", "ROLE", id)
        AccessRoleResponse(
            id = id,
            name = request.name.trim(),
            description = request.description.trim().take(255),
            allowedMenus = menus,
            permissions = validPerms.map { it.second },
            isActive = request.isActive
        )
    }

    fun updateRole(businessId: String, roleId: String, request: SaveAccessRoleRequest): AccessRoleResponse = transaction {
        val current = AccessRolesTable.select { (AccessRolesTable.id eq roleId) and (AccessRolesTable.businessId eq businessId) }
            .firstOrNull() ?: error("Role not found")
        val name = request.name.trim()
        require(name.length in 2..80) { "Role name must be between 2 and 80 characters" }
        require(AccessRolesTable.select { AccessRolesTable.businessId eq businessId }.none {
            it[AccessRolesTable.id] != roleId && it[AccessRolesTable.name].equals(name, ignoreCase = true)
        }) { "A role with this name already exists" }
        val menus = validateMenus(request.allowedMenus)
        AccessRolesTable.update({ AccessRolesTable.id eq current[AccessRolesTable.id] }) {
            it[AccessRolesTable.name] = name
            it[description] = request.description.trim().take(255)
            it[allowedMenus] = menus.joinToString(",")
            it[isActive] = request.isActive
            it[updatedAt] = Clock.System.now()
        }

        val validPerms = if (request.permissions.isNotEmpty()) {
            PermissionsTable.select { PermissionsTable.code inList request.permissions }
                .map { it[PermissionsTable.id] to it[PermissionsTable.code] }
        } else emptyList()

        RolePermissionsTable.deleteWhere { RolePermissionsTable.roleId eq roleId }
        for ((pId, _) in validPerms) {
            RolePermissionsTable.insert {
                it[RolePermissionsTable.roleId] = roleId
                it[permissionId] = pId
            }
        }

        auditLogService?.logEvent(businessId, null, null, "UPDATE_ACCESS_ROLE", null, "Updated access role $name (active: ${request.isActive})", "ROLE", roleId)
        AccessRoleResponse(
            id = roleId,
            name = name,
            description = request.description.trim().take(255),
            allowedMenus = menus,
            permissions = validPerms.map { it.second },
            isActive = request.isActive
        )
    }

    fun createGroup(businessId: String, request: SaveAccessGroupRequest): AccessGroupResponse = transaction {
        require(request.name.trim().length in 2..80) { "Group name must be between 2 and 80 characters" }
        require(AccessGroupsTable.select { AccessGroupsTable.businessId eq businessId }.none { it[AccessGroupsTable.name].equals(request.name.trim(), ignoreCase = true) }) { "A group with this name already exists" }
        
        val roleIds = validateRoleIds(businessId, request.roleIds)
        val menus = validateMenus(request.allowedMenus)

        val id = generateId()
        val now = Clock.System.now()
        AccessGroupsTable.insert { row ->
            row[AccessGroupsTable.id] = id
            row[AccessGroupsTable.businessId] = businessId
            row[name] = request.name.trim()
            row[description] = request.description.trim().take(255)
            row[allowedMenus] = menus.joinToString(",")
            row[isActive] = request.isActive
            row[createdAt] = now
            row[updatedAt] = now
        }
        roleIds.forEach { roleId -> AccessGroupRolesTable.insert { it[groupId] = id; it[AccessGroupRolesTable.roleId] = roleId } }
        auditLogService?.logEvent(businessId, null, null, "CREATE_ACCESS_GROUP", null, "Created access group ${request.name.trim()} with ${menus.size} rights")
        AccessGroupResponse(id, request.name.trim(), request.description.trim().take(255), menus, roleIds, emptyList(), request.isActive)
    }

    fun updateGroup(businessId: String, groupId: String, request: SaveAccessGroupRequest): AccessGroupResponse = transaction {
        require(AccessGroupsTable.select { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) }.any()) { "Group not found" }
        val name = request.name.trim()
        require(name.length in 2..80) { "Group name must be between 2 and 80 characters" }
        require(AccessGroupsTable.select { AccessGroupsTable.businessId eq businessId }.none {
            it[AccessGroupsTable.id] != groupId && it[AccessGroupsTable.name].equals(name, ignoreCase = true)
        }) { "A group with this name already exists" }

        val roleIds = validateRoleIds(businessId, request.roleIds)
        val menus = validateMenus(request.allowedMenus)

        AccessGroupsTable.update({ AccessGroupsTable.id eq groupId }) {
            it[AccessGroupsTable.name] = name
            it[description] = request.description.trim().take(255)
            it[allowedMenus] = menus.joinToString(",")
            it[isActive] = request.isActive
            it[updatedAt] = Clock.System.now()
        }
        AccessGroupRolesTable.deleteWhere { AccessGroupRolesTable.groupId eq groupId }
        roleIds.forEach { roleId -> AccessGroupRolesTable.insert { it[AccessGroupRolesTable.groupId] = groupId; it[AccessGroupRolesTable.roleId] = roleId } }
        auditLogService?.logEvent(businessId, null, null, "UPDATE_ACCESS_GROUP", null, "Updated access group $name with ${menus.size} rights")
        groups(businessId).first { it.id == groupId }
    }

    fun assignUsers(businessId: String, groupId: String, request: AssignGroupUsersRequest): AccessGroupResponse = transaction {
        require(AccessGroupsTable.select { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) }.any()) { "Group not found" }
        val userIds = request.userIds.distinct()
        require(userIds.isEmpty() || UsersTable.select { (UsersTable.businessId eq businessId) and (UsersTable.id inList userIds) }.count().toInt() == userIds.size) { "One or more users are invalid" }
        UserAccessGroupsTable.deleteWhere { UserAccessGroupsTable.groupId eq groupId }
        userIds.forEach { userId -> UserAccessGroupsTable.insert { it[UserAccessGroupsTable.userId] = userId; it[UserAccessGroupsTable.groupId] = groupId } }
        auditLogService?.logEvent(businessId, null, null, "ASSIGN_GROUP_USERS", null, "Assigned ${userIds.size} users to group $groupId")
        groups(businessId).first { it.id == groupId }
    }

    fun deleteRole(businessId: String, roleId: String): Boolean = transaction {
        val exists = AccessRolesTable.select { (AccessRolesTable.id eq roleId) and (AccessRolesTable.businessId eq businessId) }.any()
        require(exists) { "Role not found" }
        val assignedGroups = (AccessGroupRolesTable innerJoin AccessGroupsTable)
            .select {
                (AccessGroupRolesTable.roleId eq roleId) and
                    (AccessGroupsTable.businessId eq businessId)
            }
            .count()
        require(assignedGroups == 0L) { "Role is assigned to one or more groups. Disable it or remove it from those groups first." }
        AccessRolesTable.deleteWhere { (AccessRolesTable.id eq roleId) and (AccessRolesTable.businessId eq businessId) } > 0
    }

    fun toggleRoleStatus(businessId: String, roleId: String, isActive: Boolean): AccessRoleResponse = transaction {
        val exists = AccessRolesTable.select { (AccessRolesTable.id eq roleId) and (AccessRolesTable.businessId eq businessId) }.any()
        require(exists) { "Role not found" }
        AccessRolesTable.update({ AccessRolesTable.id eq roleId }) {
            it[AccessRolesTable.isActive] = isActive
            it[updatedAt] = Clock.System.now()
        }
        roles(businessId).first { it.id == roleId }
    }

    fun deleteGroup(businessId: String, groupId: String): Boolean = transaction {
        val exists = AccessGroupsTable.select { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) }.any()
        require(exists) { "Group not found" }
        AccessGroupRolesTable.deleteWhere { AccessGroupRolesTable.groupId eq groupId }
        UserAccessGroupsTable.deleteWhere { UserAccessGroupsTable.groupId eq groupId }
        val deleted = AccessGroupsTable.deleteWhere { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) } > 0
        if (deleted) auditLogService?.logEvent(businessId, null, null, "DELETE_ACCESS_GROUP", null, "Deleted access group $groupId")
        deleted
    }

    fun toggleGroupStatus(businessId: String, groupId: String, isActive: Boolean): AccessGroupResponse = transaction {
        val exists = AccessGroupsTable.select { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) }.any()
        require(exists) { "Group not found" }
        AccessGroupsTable.update({ AccessGroupsTable.id eq groupId }) {
            it[AccessGroupsTable.isActive] = isActive
            it[updatedAt] = Clock.System.now()
        }
        auditLogService?.logEvent(businessId, null, null, "TOGGLE_GROUP_STATUS", null, "Toggled group $groupId active=$isActive")
        groups(businessId).first { it.id == groupId }
    }

    private fun businessMenus(businessId: String): List<String> {
        val business = BusinessesTable.select { BusinessesTable.id eq businessId }.first()
        val menus = csv(business[BusinessesTable.enabledMenus]).filter { it in MENU_KEYS }.toMutableSet()
        val isHospitalityActive = business[BusinessesTable.hospitalityEnabled] || business[BusinessesTable.type].equals("HOSPITALITY", ignoreCase = true)
        if (isHospitalityActive) {
            menus += setOf("HOSPITALITY", "HOSPITALITY_OPS", "OPEN_TABS")
        } else {
            menus -= setOf("HOSPITALITY", "HOSPITALITY_OPS", "OPEN_TABS")
        }
        if (business[BusinessesTable.servicesEnabled]) menus += "SERVICES" else menus -= "SERVICES"
        return menus.toList()
    }

    private fun roles(businessId: String): List<AccessRoleResponse> {
        val permMap = (RolePermissionsTable innerJoin PermissionsTable)
            .slice(RolePermissionsTable.roleId, PermissionsTable.code)
            .selectAll()
            .groupBy({ it[RolePermissionsTable.roleId] }, { it[PermissionsTable.code] })

        return AccessRolesTable.select { AccessRolesTable.businessId eq businessId }
            .orderBy(AccessRolesTable.name)
            .map {
                val rId = it[AccessRolesTable.id]
                AccessRoleResponse(
                    id = rId,
                    name = it[AccessRolesTable.name],
                    description = it[AccessRolesTable.description],
                    allowedMenus = csv(it[AccessRolesTable.allowedMenus]),
                    permissions = permMap[rId].orEmpty(),
                    isActive = it[AccessRolesTable.isActive]
                )
            }
    }

    fun listPermissions(): List<PermissionDefinition> = transaction {
        PermissionsTable.selectAll()
            .orderBy(PermissionsTable.module)
            .orderBy(PermissionsTable.code)
            .map {
                PermissionDefinition(
                    id = it[PermissionsTable.id],
                    code = it[PermissionsTable.code],
                    module = it[PermissionsTable.module],
                    action = it[PermissionsTable.action],
                    name = it[PermissionsTable.name],
                    description = it[PermissionsTable.description]
                )
            }
    }

    fun hasPermission(userId: String, businessId: String, permissionCode: String): Boolean = transaction {
        val user = UsersTable.select { (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }.firstOrNull() ?: return@transaction false
        if (user[UsersTable.role] in setOf("ADMIN", "BUSINESS_ADMIN", "SUPERADMIN")) return@transaction true

        permissionCode in permissionsForRoles(effectiveRoleIds(businessId, userId))
    }

    private fun validateRoleIds(businessId: String, values: List<String>): List<String> {
        val ids = values.distinct()
        require(AccessRolesTable.select {
            (AccessRolesTable.businessId eq businessId) and (AccessRolesTable.id inList ids)
        }.count().toInt() == ids.size) { "One or more roles are invalid for this business" }
        return ids
    }

    private fun effectiveRoleIds(businessId: String, userId: String): List<String> {
        val direct = UserAccessRolesTable.select { UserAccessRolesTable.userId eq userId }
            .map { it[UserAccessRolesTable.roleId] }
        val inherited = (UserAccessGroupsTable innerJoin AccessGroupsTable innerJoin AccessGroupRolesTable)
            .select {
                (UserAccessGroupsTable.userId eq userId) and
                    (AccessGroupsTable.businessId eq businessId) and (AccessGroupsTable.isActive eq true)
            }.map { it[AccessGroupRolesTable.roleId] }
        return AccessRolesTable.select {
            (AccessRolesTable.id inList (direct + inherited).distinct()) and
                (AccessRolesTable.businessId eq businessId) and (AccessRolesTable.isActive eq true)
        }.map { it[AccessRolesTable.id] }
    }

    private fun permissionsForRoles(roleIds: List<String>): List<String> =
        (RolePermissionsTable innerJoin PermissionsTable)
            .select { RolePermissionsTable.roleId inList roleIds }
            .map { it[PermissionsTable.code] }.distinct()

    private fun groups(businessId: String): List<AccessGroupResponse> {
        val roleMap = AccessGroupRolesTable.selectAll().groupBy({ it[AccessGroupRolesTable.groupId] }, { it[AccessGroupRolesTable.roleId] })
        val userMap = UserAccessGroupsTable.selectAll().groupBy({ it[UserAccessGroupsTable.groupId] }, { it[UserAccessGroupsTable.userId] })
        return AccessGroupsTable.select { AccessGroupsTable.businessId eq businessId }
            .orderBy(AccessGroupsTable.name)
            .map {
                AccessGroupResponse(
                    id = it[AccessGroupsTable.id],
                    name = it[AccessGroupsTable.name],
                    description = it[AccessGroupsTable.description],
                    allowedMenus = csv(it[AccessGroupsTable.allowedMenus]),
                    roleIds = roleMap[it[AccessGroupsTable.id]].orEmpty(),
                    userIds = userMap[it[AccessGroupsTable.id]].orEmpty(),
                    isActive = it[AccessGroupsTable.isActive]
                )
            }
    }

    fun hasMenuAccess(userId: String, businessId: String, menuKey: String): Boolean = transaction {
        val user = UsersTable.select { (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }.firstOrNull() ?: return@transaction false
        if (user[UsersTable.role] == "ADMIN" || user[UsersTable.role] == "SUPERADMIN") return@transaction true
        val access = myMenus(businessId, userId, user[UsersTable.role])
        access.enabledMenus.contains(menuKey.uppercase())
    }

    private fun validateMenus(values: List<String>): List<String> {
        val normalized = values.map { it.trim().uppercase() }.distinct()
        require(normalized.all { it in MENU_KEYS }) { "Unknown menu selection" }
        return normalized
    }

    private fun csv(value: String) = value.split(',').map { it.trim().uppercase() }.filter { it.isNotEmpty() }
}
