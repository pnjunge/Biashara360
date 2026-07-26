package com.app.biashara.services

import io.ktor.client.HttpClient
import io.ktor.server.config.MapApplicationConfig
import io.mockk.mockk
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SocialServiceSecurityTest {
    private val appSecret = "test-app-secret"
    private val encryptionKey = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    private val service = SocialService(
        httpClient = mockk<HttpClient>(),
        mpesaService = mockk(),
        orderService = mockk(),
        productService = mockk(),
        config = MapApplicationConfig(
            "facebook.appId" to "123456789",
            "facebook.appSecret" to appSecret,
            "facebook.embeddedSignupConfigurationId" to "987654321",
            "facebook.webhookVerifyToken" to "verify-token",
            "social.tokenEncryptionKey" to encryptionKey
        )
    )

    @Test
    fun `validates webhook token and signed payload`() {
        val body = """{"object":"whatsapp_business_account","entry":[]}"""
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(appSecret.toByteArray(), "HmacSHA256"))
        val signature = mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }

        assertTrue(service.verifyMetaWebhookToken("verify-token"))
        assertFalse(service.verifyMetaWebhookToken("wrong-token"))
        assertTrue(service.verifyMetaWebhookSignature(body, "sha256=$signature"))
        assertFalse(service.verifyMetaWebhookSignature("$body ", "sha256=$signature"))
        assertFalse(service.verifyMetaWebhookSignature(body, null))
    }

    @Test
    fun `reports complete merchant onboarding configuration`() {
        val configuration = service.getMetaOnboardingConfiguration()
        assertTrue(configuration.configured)
        assertTrue(configuration.missing.isEmpty())
    }
}
