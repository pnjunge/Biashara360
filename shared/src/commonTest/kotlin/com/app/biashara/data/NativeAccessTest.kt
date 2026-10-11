package com.app.biashara.data

import com.app.biashara.data.remote.*
import com.app.biashara.domain.model.UserRole
import kotlinx.serialization.json.Json
import kotlin.test.*

class NativeAccessTest {
    @Test fun decodesPermissionsFromTheSharedApiContract() {
        val access = Json.decodeFromString<NativeAccess>("""{"enabledMenus":["ORDERS"],"permissions":["orders.view"]}""")
        assertTrue(access.allows("ORDERS", UserRole.STAFF))
        assertTrue(access.permits("orders.view"))
        assertFalse(access.permits("orders.cancel"))
        assertFalse(access.permits("orders.refund"))
        assertFalse(access.allows("PURCHASES", UserRole.ADMIN))
    }
    @Test fun settingsAndAliasesMatchWebRules() {
        val access = NativeAccess(listOf("SETTINGS", "CARD_PAYMENTS", "SOCIAL_SETUP", "HOSPITALITY"))
        assertFalse(access.allows("SETTINGS", UserRole.STAFF))
        assertTrue(access.allows("SETTINGS", UserRole.ADMIN))
        assertTrue(access.allows("PAYMENTS", UserRole.STAFF))
        assertTrue(access.allows("SOCIAL", UserRole.STAFF))
        assertFalse(access.allows("HOSPITALITY_OPS", UserRole.STAFF))
        assertFalse(access.allows("HOTEL", UserRole.ADMIN))
    }
    @Test fun missingOrResetRightsNeverAllowMutations() {
        NativeAccessSession.set(NativeAccess(permissions = listOf("orders.cancel")))
        assertTrue(NativeAccessSession.permits("orders.cancel"))
        NativeAccessSession.reset()
        assertFalse(NativeAccessSession.permits("orders.cancel"))
    }
    @Test fun promptRemainsUntilBothRequiredShiftsAreOpen() {
        assertTrue(LoginShiftStatus(false, true, true, false).needsPrompt)
        assertTrue(LoginShiftStatus(true, false, false, true).needsPrompt)
        assertFalse(LoginShiftStatus(true, true, true, true).needsPrompt)
    }
}
