package com.app.biashara.data

import com.app.biashara.data.remote.NativeAccess
import com.app.biashara.domain.model.UserRole
import com.app.biashara.ui.*
import kotlin.test.*

class DesktopMenuParityTest {
    @Test fun staffHospitalityMenusMatchWebNavigation() {
        val access = NativeAccess(enabledMenus = listOf(
            "CARD_PAYMENTS", "CUSTOMERS", "DASHBOARD", "EXPENSES", "HOSPITALITY",
            "OPEN_TABS", "ORDERS", "PAYMENTS", "POS", "REPORTS", "SOCIAL"
        ))
        val screens = appScreens.filter { access.allows(desktopMenu(it), UserRole.STAFF) }.toSet()
        assertEquals(setOf(
            AppScreen.Dashboard, AppScreen.Pos, AppScreen.Hospitality, AppScreen.OpenTabs,
            AppScreen.KitchenDisplay, AppScreen.Orders, AppScreen.Customers, AppScreen.Expenses,
            AppScreen.Payments, AppScreen.Reports, AppScreen.Social
        ), screens)
        assertFalse(AppScreen.HospitalityOperations in screens)
        assertFalse(AppScreen.Bookings in screens)
    }
}
