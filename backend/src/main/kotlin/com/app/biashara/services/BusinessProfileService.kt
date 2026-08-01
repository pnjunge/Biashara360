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
                    storefrontSlug  = it[BusinessesTable.storefrontSlug],
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
                    subscriptionTier = it[BusinessesTable.subscriptionTier],
                    subscriptionEnabled = it[BusinessesTable.subscriptionEnabled],
                    hospitalityEnabled = it[BusinessesTable.hospitalityEnabled],
                    receiptHeader   = it[BusinessesTable.receiptHeader],
                    receiptFooter   = it[BusinessesTable.receiptFooter],
                    receiptLogo     = it[BusinessesTable.receiptLogo],
                    receiptShowTax  = it[BusinessesTable.receiptShowTax],
                    receiptShowCustomer = it[BusinessesTable.receiptShowCustomer],
                    storefrontThemeColor = it[BusinessesTable.storefrontThemeColor],
                    storefrontHeadline = it[BusinessesTable.storefrontHeadline],
                    storefrontDescription = it[BusinessesTable.storefrontDescription],
                    storefrontBannerUrl = it[BusinessesTable.storefrontBannerUrl],
                    storefrontLayout = it[BusinessesTable.storefrontLayout],
                    dayStartTime = it[BusinessesTable.dayStartTime],
                    dayCloseTime = it[BusinessesTable.dayCloseTime]
                )
            }
    }

    fun updateProfile(businessId: String, req: BusinessProfileRequest): ApiResponse<BusinessProfileResponse> = transaction {
        if (req.name.isBlank()) {
            return@transaction ApiResponse(false, message = "Business name is required")
        }
        val logo = req.receiptLogo?.trim()?.takeIf { it.isNotEmpty() }
        if (logo != null && logo.length > 750_000) {
            return@transaction ApiResponse(false, message = "Receipt logo must be smaller than 500 KB")
        }
        if (logo != null && !logo.startsWith("data:image/png;base64,") &&
            !logo.startsWith("data:image/jpeg;base64,") &&
            !logo.startsWith("data:image/webp;base64,") &&
            !logo.startsWith("https://")) {
            return@transaction ApiResponse(false, message = "Receipt logo must be a PNG, JPEG, WebP, or HTTPS image")
        }
        val themeColor = req.storefrontThemeColor.trim().uppercase()
        if (!themeColor.matches(Regex("^#[0-9A-F]{6}$"))) return@transaction ApiResponse(false, message = "Storefront theme color must be a valid hex color")
        val bannerUrl = req.storefrontBannerUrl?.trim()?.takeIf { it.isNotEmpty() }
        if (bannerUrl != null && !bannerUrl.startsWith("https://")) return@transaction ApiResponse(false, message = "Storefront banner must use an HTTPS image URL")
        val layout = req.storefrontLayout.trim().uppercase()
        if (layout !in setOf("GRID", "LIST")) return@transaction ApiResponse(false, message = "Storefront layout must be GRID or LIST")
        val timePattern = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
        if (!req.dayStartTime.matches(timePattern) || !req.dayCloseTime.matches(timePattern)) {
            return@transaction ApiResponse(false, message = "Start and close times must use 24-hour HH:mm format")
        }
        if (req.dayStartTime == req.dayCloseTime) return@transaction ApiResponse(false, message = "Start and close times must be different")

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
            it[receiptHeader] = req.receiptHeader
            it[receiptFooter] = req.receiptFooter
            it[receiptLogo] = logo
            it[receiptShowTax] = req.receiptShowTax
            it[receiptShowCustomer] = req.receiptShowCustomer
            it[storefrontThemeColor] = themeColor
            it[storefrontHeadline] = req.storefrontHeadline.trim().ifBlank { "Shop with us online" }.take(120)
            it[storefrontDescription] = req.storefrontDescription.trim().take(500)
            it[storefrontBannerUrl] = bannerUrl
            it[storefrontLayout] = layout
            it[dayStartTime] = req.dayStartTime
            it[dayCloseTime] = req.dayCloseTime
            it[updatedAt]     = now
        }

        val updated = getProfile(businessId)
            ?: return@transaction ApiResponse(false, message = "Business not found after update")
        ApiResponse(success = true, data = updated, message = "Business profile updated")
    }
}
