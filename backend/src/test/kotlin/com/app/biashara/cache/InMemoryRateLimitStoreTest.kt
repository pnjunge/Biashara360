package com.app.biashara.cache

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryRateLimitStoreTest {
    @Test
    fun `limits requests sharing the same key`() = runBlocking {
        val store = InMemoryRateLimitStore()

        assertTrue(store.consume("login:client", 2, 60).allowed)
        val second = store.consume("login:client", 2, 60)
        val third = store.consume("login:client", 2, 60)

        assertTrue(second.allowed)
        assertEquals(0, second.remaining)
        assertFalse(third.allowed)
    }

    @Test
    fun `keeps tenant and endpoint keys independent`() = runBlocking {
        val store = InMemoryRateLimitStore()

        assertTrue(store.consume("auth:business-a", 1, 60).allowed)
        assertFalse(store.consume("auth:business-a", 1, 60).allowed)
        assertTrue(store.consume("auth:business-b", 1, 60).allowed)
    }
}
