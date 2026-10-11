package com.app.biashara.data

import com.app.biashara.data.repository.nativeUserRole
import com.app.biashara.domain.model.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals

class AdministratorRoleTest {
    @Test fun `administrator aliases retain administrator restrictions on native clients`() {
        for (role in listOf("ADMIN", "BUSINESS_ADMIN", "ADMINISTRATOR", "administrator")) assertEquals(UserRole.ADMIN, nativeUserRole(role))
        assertEquals(UserRole.SUPERADMIN, nativeUserRole("SUPERADMIN"))
        assertEquals(UserRole.STAFF, nativeUserRole("STAFF"))
    }
}
