package com.app.biashara.services

import com.app.biashara.db.SystemSettingsTable
import com.app.biashara.models.ApiResponse
import com.app.biashara.models.SystemSettingResponse
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

const val KEY_MPESA_CALLBACK_URL = "mpesa_callback_url"

class SystemSettingsService {

    fun getSetting(key: String): String? = transaction {
        SystemSettingsTable
            .select { SystemSettingsTable.key eq key }
            .firstOrNull()
            ?.get(SystemSettingsTable.value)
    }

    fun saveSetting(key: String, value: String) {
        transaction {
            val now = Clock.System.now()
            val exists = SystemSettingsTable.select { SystemSettingsTable.key eq key }.count() > 0
            if (exists) {
                SystemSettingsTable.update({ SystemSettingsTable.key eq key }) {
                    it[SystemSettingsTable.value] = value
                    it[updatedAt] = now
                }
            } else {
                SystemSettingsTable.insert {
                    it[SystemSettingsTable.key] = key
                    it[SystemSettingsTable.value] = value
                    it[updatedAt] = now
                }
            }
        }
    }

    fun getMpesaCallbackUrl(): String? = getSetting(KEY_MPESA_CALLBACK_URL)

    fun saveMpesaCallbackUrl(url: String): ApiResponse<SystemSettingResponse> {
        if (url.isBlank()) {
            return ApiResponse(false, message = "Callback URL must not be blank")
        }
        if (!url.startsWith("https://")) {
            return ApiResponse(false, message = "Callback URL must start with https://")
        }
        try {
            val parsed = java.net.URL(url)
            if (parsed.host.isNullOrBlank()) {
                return ApiResponse(false, message = "Callback URL must contain a valid host")
            }
        } catch (e: java.net.MalformedURLException) {
            return ApiResponse(false, message = "Callback URL is not a valid URL: ${e.message}")
        }
        saveSetting(KEY_MPESA_CALLBACK_URL, url)
        return ApiResponse(true, data = SystemSettingResponse(KEY_MPESA_CALLBACK_URL, url), message = "Mpesa callback URL updated")
    }
}
