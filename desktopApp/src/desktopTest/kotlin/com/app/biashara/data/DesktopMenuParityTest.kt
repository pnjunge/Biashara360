package com.app.biashara.data

import com.app.biashara.data.remote.NativeAccess
import com.app.biashara.domain.model.UserRole
import com.app.biashara.ui.*
import kotlin.test.*

class DesktopMenuParityTest {
    @Test fun staffHospitalityMenusUseOneTabsAndKitchenEntry() {
        val access = NativeAccess(enabledMenus = listOf(
            "CARD_PAYMENTS", "CUSTOMERS", "DASHBOARD", "EXPENSES", "HOSPITALITY",
            "OPEN_TABS", "ORDERS", "PAYMENTS", "POS", "REPORTS", "SOCIAL"
        ))
        val screens = appScreens.filter { desktopAllowsMenu(access, it) }.toSet()
        assertEquals(setOf(
            AppScreen.Dashboard, AppScreen.Pos, AppScreen.Hospitality,
            AppScreen.Orders, AppScreen.Customers, AppScreen.Expenses,
            AppScreen.Payments, AppScreen.Reports, AppScreen.Social
        ), screens)
        assertFalse(AppScreen.HospitalityOperations in screens)
        assertFalse(AppScreen.Bookings in screens)
    }
    @Test fun mergedMenuRetainsBothAccessPaths() {
        assertTrue(desktopAllowsMenu(NativeAccess(enabledMenus = listOf("OPEN_TABS")), AppScreen.Hospitality))
        assertTrue(desktopAllowsMenu(NativeAccess(enabledMenus = listOf("HOSPITALITY")), AppScreen.OpenTabs))
        assertFalse(desktopAllowsMenu(NativeAccess(), AppScreen.OpenTabs))
        assertFalse(AppScreen.KitchenDisplay in appScreens)
        assertFalse(AppScreen.OpenTabs in appScreens)
        assertFalse(AppScreen.HospitalityOperations in appScreens)
        assertEquals(listOf(1), desktopHospitalityTabs(NativeAccess(enabledMenus = listOf("OPEN_TABS"))))
        assertEquals(listOf(0, 2, 3), desktopHospitalityTabs(NativeAccess(enabledMenus = listOf("HOSPITALITY"))))
        assertEquals(listOf(3), desktopHospitalityTabs(NativeAccess(enabledMenus = listOf("HOSPITALITY_OPS"))))
    }
}
