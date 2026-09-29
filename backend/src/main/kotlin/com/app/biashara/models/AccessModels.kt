package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable data class MenuDefinition(val key: String, val label: String)
@Serializable data class AccessRoleResponse(val id: String, val name: String, val description: String, val allowedMenus: List<String>, val isActive: Boolean)
@Serializable data class AccessGroupResponse(
    val id: String,
    val name: String,
    val description: String,
    val allowedMenus: List<String> = emptyList(),
    val roleIds: List<String> = emptyList(),
    val userIds: List<String> = emptyList(),
    val isActive: Boolean = true
)
@Serializable data class AccessConfigResponse(
    val menus: List<MenuDefinition>,
    val enabledMenus: List<String>,
    val roles: List<AccessRoleResponse> = emptyList(),
    val groups: List<AccessGroupResponse>
)
@Serializable data class MyMenuAccessResponse(val enabledMenus: List<String>)
@Serializable data class UpdateMenusRequest(val enabledMenus: List<String>)
@Serializable data class SaveAccessRoleRequest(val name: String, val description: String = "", val allowedMenus: List<String> = emptyList(), val isActive: Boolean = true)
@Serializable data class SaveAccessGroupRequest(
    val name: String,
    val description: String = "",
    val allowedMenus: List<String> = emptyList(),
    val roleIds: List<String> = emptyList(),
    val isActive: Boolean = true
)
@Serializable data class AssignGroupUsersRequest(val userIds: List<String>)
@Serializable data class UpdateAccessStatusRequest(val isActive: Boolean)
