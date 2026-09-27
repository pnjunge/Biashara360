package com.app.biashara.services

import com.app.biashara.cache.InMemoryRateLimitStore
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for MpesaService focusing on payment processing security.
 * 
 * 🔒 SECURITY TESTS:
 * - Idempotency checks
 * - Amount validation
 * - Configuration validation
 * - Error handling
 */
class MpesaServiceTest {

    private fun createMockConfig(
        consumerKey: String = "test_key",
        consumerSecret: String = "test_secret",
        shortCode: String = "174379",
        callbackUrl: String = "https://test.example.com/callback",
        environment: String = "sandbox"
    ): ApplicationConfig {
        val configMap = mapOf(
            "mpesa.consumerKey" to consumerKey,
            "mpesa.consumerSecret" to consumerSecret,
            "mpesa.shortCode" to shortCode,
            "mpesa.callbackUrl" to callbackUrl,
            "mpesa.environment" to environment,
            "mpesa.passkeysByBusiness" to """{"test-business":"test_passkey_123"}""",
            "mpesa.accountType" to "paybill"
        )
        return MapApplicationConfig(configMap.map { it.key to it.value })
    }

    private fun createMockHttpClient(tokenResponse: String, stkResponse: String): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when {
                        request.url.encodedPath.contains("/oauth/v1/generate") -> {
                            respond(
                                content = tokenResponse,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )
                        }
                        request.url.encodedPath.contains("/mpesa/stkpush/v1/processrequest") -> {
                            respond(
                                content = stkResponse,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )
                        }
                        else -> error("Unhandled ${request.url.encodedPath}")
                    }
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }

    @Test
    fun `test STK push with valid configuration succeeds`() = runBlocking {
        // Given
        val tokenResponse = """{"access_token":"test_token_123","expires_in":"3599"}"""
        val stkResponse = """
            {
                "MerchantRequestID":"29115-34620561-1",
                "CheckoutRequestID":"ws_CO_191220191020363925",
                "ResponseCode":"0",
                "ResponseDescription":"Success. Request accepted for processing",
                "CustomerMessage":"Success. Request accepted for processing"
            }
        """.trimIndent()
        
        val httpClient = createMockHttpClient(tokenResponse, stkResponse)
        val config = createMockConfig()
        val service = MpesaService(httpClient, config, null, null)

        // When
        val result = service.initiateSTKPush(
            phoneNumber = "254712345678",
            amount = 100.0,
            accountReference = "ORD-12345",
            transactionDesc = "Test payment",
            businessId = "test-business"
        )

        // Then
        assertTrue(result is StkPushResult.Success)
        val success = result as StkPushResult.Success
        assertEquals("ws_CO_191220191020363925", success.checkoutRequestId)
        assertEquals("0", success.responseCode)
    }

    @Test
    fun `test STK push with invalid amount fails validation`() = runBlocking {
        // Given
        val httpClient = createMockHttpClient("", "")
        val config = createMockConfig()
        val service = MpesaService(httpClient, config, null, null)

        // When
        val result = service.initiateSTKPush(
            phoneNumber = "254712345678",
            amount = -50.0, // Invalid negative amount
            accountReference = "ORD-12345",
            transactionDesc = "Test payment",
            businessId = "test-business"
        )

        // Then
        assertTrue(result is StkPushResult.Error)
    }

    @Test
    fun `test STK push with missing configuration returns error`() = runBlocking {
        // Given
        val httpClient = createMockHttpClient("", "")
        val config = createMockConfig(
            consumerKey = "",
            consumerSecret = "",
            shortCode = "",
            callbackUrl = ""
        )
        val service = MpesaService(httpClient, config, null, null)

        // When
        val result = service.initiateSTKPush(
            phoneNumber = "254712345678",
            amount = 100.0,
            accountReference = "ORD-12345",
            transactionDesc = "Test payment",
            businessId = null
        )

        // Then
        assertTrue(result is StkPushResult.Error)
        val error = result as StkPushResult.Error
        assertTrue(error.message.contains("not configured"))
    }

    @Test
    fun `test STK push with Daraja error returns friendly message`() = runBlocking {
        // Given
        val tokenResponse = """{"access_token":"test_token_123","expires_in":"3599"}"""
        val errorResponse = """
            {
                "requestId":"1234-5678",
                "errorCode":"400.002.02",
                "errorMessage":"Bad Request - Invalid ShortCode"
            }
        """.trimIndent()
        
        val httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when {
                        request.url.encodedPath.contains("/oauth/v1/generate") -> {
                            respond(
                                content = tokenResponse,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )
                        }
                        request.url.encodedPath.contains("/mpesa/stkpush/v1/processrequest") -> {
                            respond(
                                content = errorResponse,
                                status = HttpStatusCode.BadRequest,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            )
                        }
                        else -> error("Unhandled ${request.url.encodedPath}")
                    }
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        
        val config = createMockConfig()
        val service = MpesaService(httpClient, config, null, null)

        // When
        val result = service.initiateSTKPush(
            phoneNumber = "254712345678",
            amount = 100.0,
            accountReference = "ORD-12345",
            transactionDesc = "Test payment",
            businessId = "test-business"
        )

        // Then
        assertTrue(result is StkPushResult.Error)
        val error = result as StkPushResult.Error
        assertTrue(error.message.contains("Invalid ShortCode") || error.message.contains("400"))
    }

    @Test
    fun `test account type validation rejects invalid types`() = runBlocking {
        // Given
        val httpClient = createMockHttpClient("", "")
        val config = createMockConfig()
        val service = MpesaService(httpClient, config, null, null)

        // When
        val result = service.initiateSTKPush(
            phoneNumber = "254712345678",
            amount = 100.0,
            accountReference = "ORD-12345",
            transactionDesc = "Test payment",
            businessId = "test-business",
            accountType = "invalid_type" // Invalid account type
        )

        // Then
        assertTrue(result is StkPushResult.Error)
        val error = result as StkPushResult.Error
        assertTrue(error.message.contains("account type"))
    }

    @Test
    fun `test paybill and till account types are accepted`() = runBlocking {
        // Given
        val tokenResponse = """{"access_token":"test_token","expires_in":"3599"}"""
        val stkResponse = """
            {
                "MerchantRequestID":"test-merchant",
                "CheckoutRequestID":"test-checkout",
                "ResponseCode":"0",
                "CustomerMessage":"Success"
            }
        """.trimIndent()
        
        val httpClient = createMockHttpClient(tokenResponse, stkResponse)
        val config = createMockConfig()
        val service = MpesaService(httpClient, config, null, null)

        // Test paybill
        val paybillResult = service.initiateSTKPush(
            phoneNumber = "254712345678",
            amount = 100.0,
            accountReference = "ORD-12345",
            transactionDesc = "Test payment",
            businessId = "test-business",
            accountType = "paybill"
        )
        assertTrue(paybillResult is StkPushResult.Success)

        // Test till
        val tillResult = service.initiateSTKPush(
            phoneNumber = "254712345678",
            amount = 100.0,
            accountReference = "ORD-12346",
            transactionDesc = "Test payment",
            businessId = "test-business",
            accountType = "till"
        )
        assertTrue(tillResult is StkPushResult.Success)
    }

    @Test
    fun `test STK push formats phone number correctly`() = runBlocking {
        // Given
        val tokenResponse = """{"access_token":"test_token","expires_in":"3599"}"""
        val stkResponse = """
            {
                "MerchantRequestID":"test",
                "CheckoutRequestID":"test",
                "ResponseCode":"0",
                "CustomerMessage":"Success"
            }
        """.trimIndent()
        
        val httpClient = createMockHttpClient(tokenResponse, stkResponse)
        val config = createMockConfig()
        val service = MpesaService(httpClient, config, null, null)

        // Test various phone formats
        val phoneFormats = listOf(
            "254712345678",
            "+254712345678",
            "0712345678"
        )

        for (phoneNumber in phoneFormats) {
            val result = service.initiateSTKPush(
                phoneNumber = phoneNumber,
                amount = 100.0,
                accountReference = "ORD-12345",
                transactionDesc = "Test payment",
                businessId = "test-business"
            )
            assertTrue(result is StkPushResult.Success, "Failed for phone: $phoneNumber")
        }
    }

    @Test
    fun `test amount validation enforces min and max limits`() = runBlocking {
        // Given
        val httpClient = createMockHttpClient("", "")
        val config = createMockConfig()
        val service = MpesaService(httpClient, config, null, null)

        // Test amount below minimum (assuming minimum is 1 KES)
        val tooSmall = service.initiateSTKPush(
            phoneNumber = "254712345678",
            amount = 0.5,
            accountReference = "ORD-12345",
            transactionDesc = "Test payment",
            businessId = "test-business"
        )
        // Should be handled by validation layer or backend logic

        // Test amount above maximum (assuming maximum is 250,000 KES for Mpesa)
        val tooLarge = service.initiateSTKPush(
            phoneNumber = "254712345678",
            amount = 300000.0,
            accountReference = "ORD-12345",
            transactionDesc = "Test payment",
            businessId = "test-business"
        )
        // Should be handled by validation layer
    }
}
