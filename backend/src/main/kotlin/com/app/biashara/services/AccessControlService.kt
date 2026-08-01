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
    MenuDefinition("INVENTORY", "Inventory"), MenuDefinition("ORDERS", "Orders"),
    MenuDefinition("CUSTOMERS", "Customers"), MenuDefinition("EXPENSES", "Expenses"),
    MenuDefinition("PAYMENTS", "M-Pesa Payments"), MenuDefinition("CARD_PAYMENTS", "Card Payments"),
    MenuDefinition("TAX", "Tax"), MenuDefinition("KRA", "KRA iTax"),
    MenuDefinition("SOCIAL", "Social Inbox"), MenuDefinition("SOCIAL_SETUP", "Social Setup"),
    MenuDefinition("USERS", "Users, Roles & Groups"), MenuDefinition("REPORTS", "Reports"),
    MenuDefinition("DOWNLOADS", "Download Apps"), MenuDefinition("SETTINGS", "Settings")
)
private val MENU_KEYS = BUSINESS_MENUS.map { it.key }.toSet()
private val DEFAULT_STAFF_MENUS = MENU_KEYS - setOf("USERS", "SETTINGS")

class AccessControlService {
    fun config(businessId: String): AccessConfigResponse = transaction {
        AccessConfigResponse(BUSINESS_MENUS, businessMenus(businessId), roles(businessId), groups(businessId))
    }

    fun myMenus(businessId: String, userId: String, builtInRole: String): MyMenuAccessResponse = transaction {
        val enabled = businessMenus(businessId).toSet()
        if (builtInRole == "ADMIN") return@transaction MyMenuAccessResponse(enabled.toList())
        val assignedRoleIds = (UserAccessGroupsTable innerJoin AccessGroupRolesTable innerJoin AccessGroupsTable)
            .slice(AccessGroupRolesTable.roleId)
            .select {
                (UserAccessGroupsTable.userId eq userId) and
                    (AccessGroupsTable.businessId eq businessId) and
                    (AccessGroupsTable.isActive eq true)
            }.map { it[AccessGroupRolesTable.roleId] }
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
        config(businessId)
    }

    fun createRole(businessId: String, request: SaveAccessRoleRequest): AccessRoleResponse = transaction {
        require(request.name.trim().length in 2..80) { "Role name must be between 2 and 80 characters" }
        require(AccessRolesTable.select { AccessRolesTable.businessId eq businessId }.none { it[AccessRolesTable.name].equals(request.name.trim(), ignoreCase = true) }) { "A role with this name already exists" }
        val id = generateId(); val now = Clock.System.now(); val menus = validateMenus(request.allowedMenus)
        AccessRolesTable.insert { row -> row[AccessRolesTable.id]=id; row[AccessRolesTable.businessId]=businessId; row[name]=request.name.trim(); row[description]=request.description.trim().take(255); row[allowedMenus]=menus.joinToString(","); row[isActive]=request.isActive; row[createdAt]=now; row[updatedAt]=now }
        AccessRoleResponse(id, request.name.trim(), request.description.trim().take(255), menus, request.isActive)
    }

    fun createGroup(businessId: String, request: SaveAccessGroupRequest): AccessGroupResponse = transaction {
        require(request.name.trim().length in 2..80) { "Group name must be between 2 and 80 characters" }
        val roleIds = request.roleIds.distinct()
        require(roleIds.isEmpty() || AccessRolesTable.select { (AccessRolesTable.businessId eq businessId) and (AccessRolesTable.id inList roleIds) }.count().toInt() == roleIds.size) { "One or more roles are invalid" }
        val id=generateId(); val now=Clock.System.now()
        AccessGroupsTable.insert { row -> row[AccessGroupsTable.id]=id; row[AccessGroupsTable.businessId]=businessId; row[name]=request.name.trim(); row[description]=request.description.trim().take(255); row[isActive]=request.isActive; row[createdAt]=now; row[updatedAt]=now }
        roleIds.forEach { roleId -> AccessGroupRolesTable.insert { it[groupId]=id; it[AccessGroupRolesTable.roleId]=roleId } }
        AccessGroupResponse(id, request.name.trim(), request.description.trim().take(255), roleIds, emptyList(), request.isActive)
    }

    fun assignUsers(businessId: String, groupId: String, request: AssignGroupUsersRequest): AccessGroupResponse = transaction {
        require(AccessGroupsTable.select { (AccessGroupsTable.id eq groupId) and (AccessGroupsTable.businessId eq businessId) }.any()) { "Group not found" }
        val userIds=request.userIds.distinct()
        require(userIds.isEmpty() || UsersTable.select { (UsersTable.businessId eq businessId) and (UsersTable.id inList userIds) }.count().toInt() == userIds.size) { "One or more users are invalid" }
        UserAccessGroupsTable.deleteWhere { UserAccessGroupsTable.groupId eq groupId }
        userIds.forEach { userId -> UserAccessGroupsTable.insert { it[UserAccessGroupsTable.userId]=userId; it[UserAccessGroupsTable.groupId]=groupId } }
        groups(businessId).first { it.id == groupId }
    }

    private fun businessMenus(businessId: String) = BusinessesTable.select { BusinessesTable.id eq businessId }.first()[BusinessesTable.enabledMenus].let(::csv).filter { it in MENU_KEYS }
    private fun roles(businessId: String) = AccessRolesTable.select { AccessRolesTable.businessId eq businessId }.orderBy(AccessRolesTable.name).map { AccessRoleResponse(it[AccessRolesTable.id],it[AccessRolesTable.name],it[AccessRolesTable.description],csv(it[AccessRolesTable.allowedMenus]),it[AccessRolesTable.isActive]) }
    private fun groups(businessId: String): List<AccessGroupResponse> {
        val roleMap=AccessGroupRolesTable.selectAll().groupBy({it[AccessGroupRolesTable.groupId]},{it[AccessGroupRolesTable.roleId]})
        val userMap=UserAccessGroupsTable.selectAll().groupBy({it[UserAccessGroupsTable.groupId]},{it[UserAccessGroupsTable.userId]})
        return AccessGroupsTable.select { AccessGroupsTable.businessId eq businessId }.orderBy(AccessGroupsTable.name).map { AccessGroupResponse(it[AccessGroupsTable.id],it[AccessGroupsTable.name],it[AccessGroupsTable.description],roleMap[it[AccessGroupsTable.id]].orEmpty(),userMap[it[AccessGroupsTable.id]].orEmpty(),it[AccessGroupsTable.isActive]) }
    }
    private fun validateMenus(values: List<String>): List<String> { val normalized=values.map { it.trim().uppercase() }.distinct(); require(normalized.all { it in MENU_KEYS }) { "Unknown menu selection" }; return normalized }
    private fun csv(value: String)=value.split(',').map { it.trim().uppercase() }.filter { it.isNotEmpty() }
}
