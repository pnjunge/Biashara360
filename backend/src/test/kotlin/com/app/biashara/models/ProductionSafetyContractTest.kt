package com.app.biashara.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProductionSafetyContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `walk-in order accepts an omitted phone`() {
        val request = json.decodeFromString<CreateOrderRequest>(
            """
            {
              "customerName": "Walk-In Customer",
              "items": [{"productId": "product-1", "quantity": 1, "unitPrice": 100.0}]
            }
            """.trimIndent()
        )

        assertNull(request.customerPhone)
    }

    @Test
    fun `invitation contract does not require or consume a client password`() {
        val request = json.decodeFromString<InviteUserRequest>(
            """
            {
              "name": "Jane",
              "email": "jane@example.com",
              "phone": "254700000001",
              "role": "STAFF",
              "password": "client-must-not-control-this"
            }
            """.trimIndent()
        )

        assertEquals("jane@example.com", request.email)
        assertEquals("STAFF", request.role)
    }
}
