package com.app.biashara.services

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthPinPolicyTest {
    @Test fun `accepts a nontrivial six digit PIN`() = assertTrue(AuthService.isAcceptableLoginPin("294817"))
    @Test fun `rejects predictable PINs`() {
        assertFalse(AuthService.isAcceptableLoginPin("123456"))
        assertFalse(AuthService.isAcceptableLoginPin("000000"))
        assertFalse(AuthService.isAcceptableLoginPin("12345"))
    }
}
