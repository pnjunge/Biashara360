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
        AccessConfigResponse(BUSINESS_MENUS, businessMenus(businessId), roles(businessId), groups(businessId))
    }

    private fun ensureDefaults(businessId: String) {
        val now = Clock.System.now()
        val existingRoles = AccessRolesTable.select { AccessRolesTable.businessId eq businessId }.count()
        val defaultRoleId = if (existingRoles == 0L) {
            val roleId = generateId()
            AccessRolesTable.insert {
                it[id] = roleId
                it[AccessRolesTable.businessId] = businessId
                it[name] = "Staff Access"
                it[description] = "Standard operational menus for staff"
                it[allowedMenus] = DEFAULT_STAFF_MENUS.joinToString(",")
                it[isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }
            roleId
        } else {
            AccessRolesTable.select { AccessRolesTable.businessId eq businessId }.first()[AccessRolesTable.id]
        }

        val existingGroups = AccessGroupsTable.select { AccessGroupsTable.businessId eq businessId }.count()
        if (existingGroups == 0L) {
            val groupId = generateId()
            AccessGroupsTable.insert {
                it[id] = groupId
                it[AccessGroupsTable.businessId] = businessId
                it[name] = "Front"
                it[description] = "Front operations team"
                it[isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }
            AccessGroupRolesTable.insert {
                it[AccessGroupRolesTable.groupId] = groupId
                it[AccessGroupRolesTable.roleId] = defaultRoleId
            }
        } else {
            val frontGroup = AccessGroupsTable.select {
                (AccessGroupsTable.businessId eq businessId) and (AccessGroupsTable.name.lowerCase() eq "front")
            }.firstOrNull()
            if (frontGroup != null) {
                val fId = frontGroup[AccessGroupsTable.id]
                val hasRoles = AccessGroupRolesTable.select { AccessGroupRolesTable.groupId eq fId }.any()
                if (!hasRoles) {
                    AccessGroupRolesTable.insert {
                        it[AccessGroupRolesTable.groupId] = fId
                        it[AccessGroupRolesTable.roleId] = defaultRoleId
                    }
                }
            }
        }
    }

    fun myMenus(businessId: String, userId: String, builtInRole: String): MyMenuAccessResponse = transaction {
        val enabled = businessMenus(businessId).toSet()
        if (builtInRole == "ADMIN") return@transaction MyMenuAccessResponse(enabled.toList())
        val groupRoleIds = (UserAccessGroupsTable innerJoin AccessGroupRolesTable innerJoin AccessGroupsTable)
            .slice(AccessGroupRolesTable.roleId)
            .select {
                (UserAccessGroupsTable.userId eq userId) and
                    (AccessGroupsTable.businessId eq businessId) and
                    (AccessGroupsTable.isActive eq true)
            }.map { it[AccessGroupRolesTable.roleId] }

        val directRoleIds = (UserAccessRolesTable innerJoin AccessRolesTable)
            .slice(UserAccessRolesTable.roleId)
            .select {
                (UserAccessRolesTable.userId eq userId) and
                    (AccessRolesTable.businessId eq businessId) and
                    (AccessRolesTable.isActive eq true)
            }.map { it[UserAccessRolesTable.roleId] }

        val assignedRoleIds = (groupRoleIds + directRoleIds).distinct()
        val allowed = if (assignedRoleIds.isEmpty()) DEFAULT_STAFF_MENUS else AccessRolesTable
            .select {
                (AccessRolesTable.id inList assignedRoleIds) and
                    (AccessRolesTable.businessId eq businessId) and
                    (AccessRolesTable.isActive eq true)
            }.flatMap { csv(it[AccessRolesTable.allowedMenus]) }.toSet()
        MyMenuAccessResponse((enabled intersect allowed).sorted())
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
        AccessRolesTable.insert { row -> row[AccessRolesTable.id]=id; row[AccessRolesTable.businessId]=businessId; row[name]=request.name.trim(); row[description]=request.description.trim().take(255); row[allowedMenus]=menus.joinToString(","); row[isActive]=request.isActive; row[createdAt]=now; row[updatedAt]=now }
        auditLogService?.logEvent(businessId, null, null, "CREATE_ACCESS_ROLE", null, "Created access role ${request.name.trim()} with ${menus.size} menus")
        AccessRoleResponse(id, request.name.trim(), request.description.trim().take(255), menus, request.isActive)
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
        auditLogService?.logEvent(businessId, null, null, "UPDATE_ACCESS_ROLE", null, "Updated access role $name (active: ${request.isActive})")
        AccessRoleResponse(roleId, name, request.description.trim().take(255), menus, request.isActive)
    }

    fun createGroup(businessId: String, request: SaveAccessGroupRequest): AccessGroupResponse = transaction {
        require(request.name.trim().length in 2..80) { "Group name must be between 2 and 80 characters" }
        require(AccessGroupsTable.select { AccessGroupsTable.businessId eq businessId }.none { it[AccessGroupsTable.name].equals(request.name.trim(), ignoreCase = true) }) { "A group with this name already exists" }
        val roleIds = request.roleIds.distinct()
        require(roleIds.isEmpty() || AccessRolesTable.select { (AccessRolesTable.businessId eq businessId) and (AccessRolesTable.id inList roleIds) }.count().toInt() == roleIds.size) { "One or more roles are invalid" }
        val id=generateId(); val now=Clock.System.now()
        AccessGroupsTable.insert { row -> row[AccessGroupsTable.id]=id; row[AccessGroupsTable.businessId]=businessId; row[name]=request.name.trim(); row[description]=request.description.trim().take(255); row[isActive]=request.isActive; row[createdAt]=now; row[updatedAt]=now }
        roleIds.forEach { roleId -> AccessGroupRolesTable.insert { it[groupId]=id; it[AccessGroupRolesTable.roleId]=roleId } }
        AccessGroupResponse(id, request.name.trim(), request.description.trim().take(255), roleIds, emptyList(), request.isActive)
    }

    fun updateGroup(businessId: String, groupId: String, request: SaveAccessGroupRequest): AccessGroupResponse = transaction {
        require(AccessGroupsTable.select { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) }.any()) { "Group not found" }
        val name = request.name.trim()
        require(name.length in 2..80) { "Group name must be between 2 and 80 characters" }
        require(AccessGroupsTable.select { AccessGroupsTable.businessId eq businessId }.none {
            it[AccessGroupsTable.id] != groupId && it[AccessGroupsTable.name].equals(name, ignoreCase = true)
        }) { "A group with this name already exists" }
        val roleIds = request.roleIds.distinct()
        require(roleIds.isEmpty() || AccessRolesTable.select {
            (AccessRolesTable.businessId eq businessId) and (AccessRolesTable.id inList roleIds)
        }.count().toInt() == roleIds.size) { "One or more roles are invalid" }
        AccessGroupsTable.update({ AccessGroupsTable.id eq groupId }) {
            it[AccessGroupsTable.name] = name
            it[description] = request.description.trim().take(255)
            it[isActive] = request.isActive
            it[updatedAt] = Clock.System.now()
        }
        AccessGroupRolesTable.deleteWhere { AccessGroupRolesTable.groupId eq groupId }
        roleIds.forEach { roleId -> AccessGroupRolesTable.insert { it[AccessGroupRolesTable.groupId] = groupId; it[AccessGroupRolesTable.roleId] = roleId } }
        groups(businessId).first { it.id == groupId }
    }

    fun assignUsers(businessId: String, groupId: String, request: AssignGroupUsersRequest): AccessGroupResponse = transaction {
        require(AccessGroupsTable.select { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) }.any()) { "Group not found" }
        val userIds=request.userIds.distinct()
        require(userIds.isEmpty() || UsersTable.select { (UsersTable.businessId eq businessId) and (UsersTable.id inList userIds) }.count().toInt() == userIds.size) { "One or more users are invalid" }
        UserAccessGroupsTable.deleteWhere { UserAccessGroupsTable.groupId eq groupId }
        userIds.forEach { userId -> UserAccessGroupsTable.insert { it[UserAccessGroupsTable.userId]=userId; it[UserAccessGroupsTable.groupId]=groupId } }
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
        AccessGroupsTable.deleteWhere { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) } > 0
    }

    fun toggleGroupStatus(businessId: String, groupId: String, isActive: Boolean): AccessGroupResponse = transaction {
        val exists = AccessGroupsTable.select { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) }.any()
        require(exists) { "Group not found" }
        AccessGroupsTable.update({ AccessGroupsTable.id eq groupId }) {
            it[AccessGroupsTable.isActive] = isActive
            it[updatedAt] = Clock.System.now()
        }
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
    private fun roles(businessId: String) = AccessRolesTable.select { AccessRolesTable.businessId eq businessId }.orderBy(AccessRolesTable.name).map { AccessRoleResponse(it[AccessRolesTable.id],it[AccessRolesTable.name],it[AccessRolesTable.description],csv(it[AccessRolesTable.allowedMenus]),it[AccessRolesTable.isActive]) }
    private fun groups(businessId: String): List<AccessGroupResponse> {
        val roleMap=AccessGroupRolesTable.selectAll().groupBy({it[AccessGroupRolesTable.groupId]},{it[AccessGroupRolesTable.roleId]})
        val userMap=UserAccessGroupsTable.selectAll().groupBy({it[UserAccessGroupsTable.groupId]},{it[UserAccessGroupsTable.userId]})
        return AccessGroupsTable.select { AccessGroupsTable.businessId eq businessId }.orderBy(AccessGroupsTable.name).map { AccessGroupResponse(it[AccessGroupsTable.id],it[AccessGroupsTable.name],it[AccessGroupsTable.description],roleMap[it[AccessGroupsTable.id]].orEmpty(),userMap[it[AccessGroupsTable.id]].orEmpty(),it[AccessGroupsTable.isActive]) }
    }
    fun hasMenuAccess(userId: String, businessId: String, menuKey: String): Boolean = transaction {
        val user = UsersTable.select { (UsersTable.id eq userId) and (UsersTable.businessId eq businessId) }.firstOrNull() ?: return@transaction false
        if (user[UsersTable.role] == "ADMIN" || user[UsersTable.role] == "SUPERADMIN") return@transaction true
        val access = myMenus(businessId, userId, user[UsersTable.role])
        access.enabledMenus.contains(menuKey.uppercase())
    }

    private fun validateMenus(values: List<String>): List<String> { val normalized=values.map { it.trim().uppercase() }.distinct(); require(normalized.all { it in MENU_KEYS }) { "Unknown menu selection" }; return normalized }
    private fun csv(value: String)=value.split(',').map { it.trim().uppercase() }.filter { it.isNotEmpty() }
}

