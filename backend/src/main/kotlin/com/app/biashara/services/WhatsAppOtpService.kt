package com.app.biashara.services

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.ApplicationConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Global Meta WhatsApp Cloud API sender for authentication OTPs. */
class WhatsAppOtpService(
    config: ApplicationConfig,
    private val httpClient: HttpClient
) {
    private val token = config.propertyOrNull("whatsapp.otpToken")?.getString()?.trim().orEmpty()
    private val phoneNumberId = config.propertyOrNull("whatsapp.otpPhoneNumberId")?.getString()?.trim().orEmpty()
    private val templateName = config.propertyOrNull("whatsapp.otpTemplateName")?.getString()?.trim().orEmpty()
    private val templateLanguage = config.propertyOrNull("whatsapp.otpTemplateLanguage")?.getString()?.trim().takeUnless { it.isNullOrBlank() } ?: "en_US"

    fun isConfigured() = token.isNotBlank() && phoneNumberId.isNotBlank() && templateName.isNotBlank()

    suspend fun sendOtp(to: String, otp: String): Result<String> {
        if (!isConfigured()) return Result.failure(IllegalStateException("WhatsApp OTP is not configured"))
        return try {
            val payload = buildJsonObject {
                put("messaging_product", "whatsapp")
                put("to", normalizePhone(to))
                put("type", "template")
                put("template", buildJsonObject {
                    put("name", templateName)
                    put("language", buildJsonObject { put("code", templateLanguage) })
                    put("components", Json.parseToJsonElement("""[{"type":"body","parameters":[{"type":"text","text":"$otp"}]}]"""))
                })
            }
            val response = httpClient.post("https://graph.facebook.com/v20.0/$phoneNumberId/messages") {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            if (response.status.isSuccess()) Result.success("WhatsApp OTP submitted")
            else Result.failure(IllegalStateException("WhatsApp API error (${response.status.value})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizePhone(phone: String): String {
        val cleaned = phone.replace(Regex("[\\s\\-()+]"), "")
        return when {
            cleaned.startsWith("0") -> "254${cleaned.drop(1)}"
            cleaned.startsWith("254") -> cleaned
            else -> cleaned
        }
    }
}
