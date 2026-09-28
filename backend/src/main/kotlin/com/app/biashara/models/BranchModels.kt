package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class BranchRequest(
    val name: String,
    val code: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val county: String? = null,
    val isHeadOffice: Boolean = false,
    val receiptHeader: String? = null,
    val receiptFooter: String? = null
)

@Serializable
data class BranchResponse(
    val id: String,
    val businessId: String,
    val name: String,
    val code: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val county: String? = null,
    val isHeadOffice: Boolean = false,
    val isActive: Boolean = true,
    val receiptHeader: String? = null,
    val receiptFooter: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class UpdateBranchStatusRequest(
    val isActive: Boolean
)
