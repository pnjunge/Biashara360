package com.app.biashara.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class WhatsAppPasswordResetTest {
    private fun config() = MapApplicationConfig(
        "whatsapp.otpToken" to "test-token",
        "whatsapp.otpPhoneNumberId" to "123",
        "whatsapp.resetTemplateName" to "password_reset_link",
        "whatsapp.resetTemplateLanguage" to "en_US"
    )
    @Test fun `sends reset URL as one approved template body parameter`() = runBlocking {
        val link = "https://biashara360.co.ke/login#token=" + "a".repeat(64)
        val client = HttpClient(MockEngine { request ->
            assertEquals("Bearer test-token", request.headers[HttpHeaders.Authorization])
            val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
            assertEquals("254712345678", body["to"]!!.jsonPrimitive.content)
            val template = body["template"]!!.jsonObject
            assertEquals("password_reset_link", template["name"]!!.jsonPrimitive.content)
            val component = template["components"]!!.jsonArray.single().jsonObject
            assertEquals(link, component["parameters"]!!.jsonArray.single().jsonObject["text"]!!.jsonPrimitive.content)
            respond("{}", HttpStatusCode.OK)
        })
        try { assertTrue(WhatsAppOtpService(config(), client).sendPasswordResetLink("+254 712 345678", link).isSuccess) }
        finally { client.close() }
    }
    @Test fun `missing approved reset template does not fall back to login OTP`() = runBlocking {
        val client = HttpClient(MockEngine { error("Must not call Meta without a reset template") })
        try {
            val service = WhatsAppOtpService(MapApplicationConfig(
                "whatsapp.otpToken" to "test", "whatsapp.otpPhoneNumberId" to "123", "whatsapp.otpTemplateName" to "login_otp"
            ), client)
            assertTrue(service.isConfigured())
            assertFalse(service.isPasswordResetConfigured())
            assertTrue(service.sendPasswordResetLink("0712345678", "https://example.test").isFailure)
        } finally { client.close() }
    }
    @Test fun `provider failure is reported without exposing secret URL`() = runBlocking {
        val client = HttpClient(MockEngine { respond("sensitive provider response", HttpStatusCode.BadRequest) })
        try {
            val result = WhatsAppOtpService(config(), client).sendPasswordResetLink("0712345678", "https://example.test#token=secret")
            assertTrue(result.isFailure)
            assertFalse(result.exceptionOrNull()!!.message.orEmpty().contains("secret"))
        } finally { client.close() }
    }
}
