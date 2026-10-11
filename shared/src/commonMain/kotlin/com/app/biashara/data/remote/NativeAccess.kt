package com.app.biashara.data.remote

import com.app.biashara.UserSession
import com.app.biashara.domain.model.UserRole
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class NativeAccess(val enabledMenus: List<String> = emptyList(), val permissions: List<String> = emptyList()) {
    fun permits(permission: String) = permission in permissions
    fun allows(menu: String, role: UserRole? = UserSession.currentUser.value?.role): Boolean {
        if (menu == "SETTINGS" && role == UserRole.STAFF) return false
        return menu in enabledMenus || when (menu) {
            "PAYMENTS" -> "CARD_PAYMENTS" in enabledMenus
            "SOCIAL" -> "SOCIAL_SETUP" in enabledMenus
            else -> false
        }
    }
}

object NativeAccessSession {
    private val mutable = MutableStateFlow<NativeAccess?>(null)
    val access = mutable.asStateFlow()
    fun reset() { mutable.value = null }
    fun set(value: NativeAccess) { mutable.value = value }
    fun permits(permission: String) = mutable.value?.permits(permission) == true
}

@Serializable
data class NativeHospitalityStatus(val enabled: Boolean = false, val shiftOpen: Boolean = false)
@Serializable
private data class PersonalShift(val id: String)
@Serializable
private data class DutyStatus(val shift: PersonalShift? = null)
@Serializable
private data class OpenDayRequest(val openingFloat: Double, val notes: String = "Opened after signing in")
@Serializable
private data class StartDutyRequest(val notes: String = "Started after signing in")

data class LoginShiftStatus(val dayOpen: Boolean, val personalOpen: Boolean, val canOpenDay: Boolean, val canStartPersonal: Boolean) {
    val needsPrompt get() = !dayOpen || !personalOpen
}

class NativeShiftClient(private val client: HttpClient) {
    suspend fun check(access: NativeAccess): LoginShiftStatus? {
        if (UserSession.currentUser.value?.role in setOf(UserRole.ADMIN, UserRole.SUPERADMIN)) return null
        if (!access.permits("hospitality.view") || access.enabledMenus.none {
                it in setOf("HOSPITALITY", "HOSPITALITY_OPS", "OPEN_TABS") }) return null
        val status = client.get("$BASE_URL/hospitality/status").body<ApiResponse<NativeHospitalityStatus>>()
        check(status.success && status.data != null) { status.message.ifBlank { "Unable to check trading day" } }
        if (!status.data.enabled) return null
        val canStart = access.permits("hospitality.orders") || access.permits("hospitality.billing")
        val personalOpen = if (canStart) {
            val duty = client.get("$BASE_URL/hospitality/duty").body<ApiResponse<DutyStatus>>()
            check(duty.success && duty.data != null) { duty.message.ifBlank { "Unable to check your shift" } }
            duty.data.shift != null
        } else true
        return LoginShiftStatus(status.data.shiftOpen, personalOpen, access.permits("hospitality.shifts"), canStart)
    }
    suspend fun openDay(openingFloat: Double) {
        require(openingFloat.isFinite() && openingFloat >= 0) { "Enter a valid opening float" }
        val response = client.post("$BASE_URL/hospitality/operations/shifts/open") { setBody(OpenDayRequest(openingFloat)) }.body<ApiResponse<kotlinx.serialization.json.JsonElement>>()
        check(response.success) { response.message.ifBlank { "Unable to open trading day" } }
    }
    suspend fun startPersonal() {
        val response = client.post("$BASE_URL/hospitality/duty/start") { setBody(StartDutyRequest()) }.body<ApiResponse<kotlinx.serialization.json.JsonElement>>()
        check(response.success) { response.message.ifBlank { "Unable to start your shift" } }
    }
}
