package com.app.biashara.services

import com.app.biashara.db.BusinessesTable
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction

class BusinessProfileService {

    fun getProfile(businessId: String): BusinessProfileResponse? = transaction {
        BusinessesTable
            .select { BusinessesTable.id eq businessId }
            .firstOrNull()
            ?.let {
                BusinessProfileResponse(
                    id              = it[BusinessesTable.id],
                    name            = it[BusinessesTable.name],
                    owner           = it[BusinessesTable.ownerName] ?: "",
                    phone           = it[BusinessesTable.ownerPhone],
                    email           = it[BusinessesTable.ownerEmail],
                    type            = it[BusinessesTable.type],
                    county          = it[BusinessesTable.county] ?: "",
                    address         = it[BusinessesTable.address] ?: "",
                    kraPin          = it[BusinessesTable.kraPin] ?: "",
                    paybillNumber   = it[BusinessesTable.paybillNumber] ?: "",
                    accountNumber   = it[BusinessesTable.accountNumber] ?: "",
                    subscriptionTier = it[BusinessesTable.subscriptionTier]
                )
            }
    }

    fun updateProfile(businessId: String, req: BusinessProfileRequest): ApiResponse<BusinessProfileResponse> = transaction {
        if (req.name.isBlank()) {
            return@transaction ApiResponse(false, message = "Business name is required")
        }

        val exists = BusinessesTable.select { BusinessesTable.id eq businessId }.count() > 0
        if (!exists) return@transaction ApiResponse(false, message = "Business not found")

        val now = Clock.System.now()
        BusinessesTable.update({ BusinessesTable.id eq businessId }) {
            it[name]          = req.name
            it[ownerName]     = req.owner.takeIf { v -> v.isNotBlank() }
            it[ownerPhone]    = req.phone
            it[ownerEmail]    = req.email
            it[type]          = req.type
            it[county]        = req.county.takeIf { v -> v.isNotBlank() }
            it[address]       = req.address.takeIf { v -> v.isNotBlank() }
            it[kraPin]        = req.kraPin.takeIf { v -> v.isNotBlank() }
            it[paybillNumber] = req.paybillNumber.takeIf { v -> v.isNotBlank() }
            it[accountNumber] = req.accountNumber.takeIf { v -> v.isNotBlank() }
            it[updatedAt]     = now
        }

        val updated = getProfile(businessId)
            ?: return@transaction ApiResponse(false, message = "Business not found after update")
        ApiResponse(success = true, data = updated, message = "Business profile updated")
    }
}
