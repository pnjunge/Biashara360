package com.app.biashara.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import kotlinx.serialization.json.*

/**
 * Sends SMS messages via the Africa's Talking REST API.
 *
 * Supports both **sandbox** (free testing) and **production** environments.
 * Configure credentials in application.conf or via environment variables:
 *   AT_USERNAME, AT_API_KEY, AT_SENDER_ID, AT_ENVIRONMENT
 */
class SmsService(
    config: ApplicationConfig,
    private val httpClient: HttpClient
) {
    private val username: String = config.propertyOrNull("africastalking.username")?.getString() ?: "dummyUser"
    private val apiKey: String = config.propertyOrNull("africastalking.apiKey")?.getString() ?: "dummyKey"
    private val senderId: String = config.propertyOrNull("africastalking.senderId")?.getString() ?: ""
    private val environment: String = config.propertyOrNull("africastalking.environment")?.getString() ?: "sandbox"

    private val baseUrl: String
        get() = if (environment == "production")
            "https://api.africastalking.com"
        else
            "https://api.sandbox.africastalking.com"

    /**
     * Returns true if the service is properly configured with an API key.
     */
    fun isConfigured(): Boolean = apiKey.isNotBlank()

    /**
     * Send a raw SMS message to a phone number.
     * @param to Recipient phone number in international format (e.g. +2547XXXXXXXX)
     * @param message The SMS body text
     */
    suspend fun sendSms(to: String, message: String): Result<String> {
        if (!isConfigured()) {
            println("[SmsService] API key not configured; SMS was not sent")
            return Result.failure(IllegalStateException("Africa's Talking API key not configured"))
        }

        return try {
            val response = httpClient.submitForm(
                url = "$baseUrl/version1/messaging",
                formParameters = parameters {
                    append("username", username)
                    append("to", normalizePhone(to))
                    append("message", message)
                    if (senderId.isNotBlank()) {
                        append("from", senderId)
                    }
                }
            ) {
                header("apiKey", apiKey)
                header("Accept", "application/json")
            }

            val body = response.bodyAsText()
            println("[SmsService] AT response (${response.status}): $body")

            if (response.status.isSuccess()) {
                // Parse the AT response to check for failures
                val json = Json.parseToJsonElement(body).jsonObject
                val recipients = json["SMSMessageData"]
                    ?.jsonObject?.get("Recipients")
                    ?.jsonArray

                if (recipients != null && recipients.isNotEmpty()) {
                    val status = recipients[0].jsonObject["status"]?.jsonPrimitive?.content
                    if (status == "Success") {
                        println("[SmsService] SMS sent successfully")
                        Result.success("SMS sent successfully")
                    } else {
                        println("[SmsService] ✗ AT reported status: $status")
                        Result.failure(RuntimeException("SMS delivery failed: $status"))
                    }
                } else {
                    Result.success("SMS submitted")
                }
            } else {
                Result.failure(RuntimeException("AT API error: ${response.status} — $body"))
            }
        } catch (e: Exception) {
            println("[SmsService] ✗ Exception sending SMS: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Send an OTP code via SMS with a branded message.
     */
    suspend fun sendOtp(to: String, otp: String): Result<String> {
        val message = "Your Biashara360 verification code is: $otp. It expires in 10 minutes. Do not share this code."
        return sendSms(to, message)
    }

    /**
     * Normalize Kenyan phone numbers to international E.164 format.
     */
    private fun normalizePhone(phone: String): String {
        val cleaned = phone.replace(Regex("[\\s\\-()]"), "")
        return when {
            cleaned.startsWith("07") -> "+254${cleaned.substring(1)}"
            cleaned.startsWith("01") -> "+254${cleaned.substring(1)}"
            cleaned.startsWith("254") -> "+$cleaned"
            cleaned.startsWith("+254") -> cleaned
            else -> cleaned
        }
    }
}
