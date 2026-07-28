package com.app.biashara.services

import com.app.biashara.models.CsPaymentLinkRequest
import io.mockk.mockk
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals

class CyberSourcePaymentLinkTest {

    @Test
    fun `generated link contains encoded checkout details and amount`() {
        val service = CyberSourcePaymentService(mockk(relaxed = true))

        val response = service.generatePaymentLink(
            businessId = "business/one",
            req = CsPaymentLinkRequest(
                orderId = "ORDER 42",
                amount = 4500.0,
                customerName = "Jane & John",
                customerEmail = "jane+payments@example.com",
                customerPhone = "+254 700 000 000"
            ),
            baseUrl = "http://localhost:5173/"
        )

        val uri = URI(response.linkUrl)
        val params = uri.rawQuery.split("&").associate { part ->
            val (key, value) = part.split("=", limit = 2)
            decode(key) to decode(value)
        }

        assertEquals("/pay/card", uri.path)
        assertEquals("ORDER 42", params["orderId"])
        assertEquals("business/one", params["businessId"])
        assertEquals("4500.0", params["amount"])
        assertEquals("Jane & John", params["name"])
        assertEquals("jane+payments@example.com", params["email"])
        assertEquals("+254 700 000 000", params["phone"])
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
}
