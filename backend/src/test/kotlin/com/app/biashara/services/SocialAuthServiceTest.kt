package com.app.biashara.services

import com.app.biashara.models.SocialCredential
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SocialAuthServiceTest {
    private fun service(appId: String = "app", expires: Long = System.currentTimeMillis() / 1000 + 3600,
                        valid: Boolean = true, profileId: String = "person", email: String = "owner@example.com"): SocialAuthService {
        val client = HttpClient(MockEngine { request ->
            val body = if (request.url.encodedPath.endsWith("debug_token")) {
                """{"data":{"is_valid":$valid,"app_id":"$appId","user_id":"person","expires_at":$expires,"data_access_expires_at":$expires}}"""
            } else {
                assertEquals("Bearer provider-token", request.headers[HttpHeaders.Authorization])
                assertNotNull(request.url.parameters["appsecret_proof"])
                """{"id":"$profileId","name":"Owner","email":"$email"}"""
            }
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        return SocialAuthService(MapApplicationConfig(
            "socialAuth.facebookAppId" to "app", "socialAuth.facebookAppSecret" to "secret",
            "socialAuth.googleClientId" to "client.apps.googleusercontent.com"
        ), client)
    }
    private val credential = SocialCredential("facebook", "provider-token")

    @Test fun `verified Facebook identity comes from provider`() = runBlocking<Unit> {
        val profile = service().verify(credential)
        assertEquals("person", profile.subject)
        assertEquals("owner@example.com", profile.email)
    }
    @Test fun `rejects tokens issued to another app`() = runBlocking<Unit> {
        assertFailsWith<IllegalArgumentException> { service(appId = "other").verify(credential) }
    }
    @Test fun `rejects expired and revoked tokens`() = runBlocking<Unit> {
        assertFailsWith<IllegalArgumentException> { service(expires = 1).verify(credential) }
        assertFailsWith<IllegalArgumentException> { service(valid = false).verify(credential) }
    }
    @Test fun `rejects mismatched profile and missing email`() = runBlocking<Unit> {
        assertFailsWith<IllegalArgumentException> { service(profileId = "other").verify(credential) }
        assertFailsWith<IllegalArgumentException> { service(email = "").verify(credential) }
    }
    @Test fun `rejects unsupported providers and empty credentials`() = runBlocking<Unit> {
        assertFailsWith<IllegalArgumentException> { service().verify(SocialCredential("other", "token")) }
        assertFailsWith<IllegalArgumentException> { service().verify(SocialCredential("google", "")) }
    }
    @Test fun `rejects Google identity issued to another client`() = runBlocking<Unit> {
        val forged = com.auth0.jwt.JWT.create()
            .withIssuer("https://accounts.google.com")
            .withAudience("other.apps.googleusercontent.com")
            .withSubject("attacker")
            .withIssuedAt(java.util.Date())
            .withClaim("email", "owner@example.com")
            .withClaim("email_verified", true)
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 60000))
            .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("attacker-secret"))
        assertFailsWith<IllegalArgumentException> { service().verify(SocialCredential("google", forged)) }
    }
    @Test fun `does not advertise unconfigured Facebook login`() {
        val config = SocialAuthService(MapApplicationConfig(), HttpClient(MockEngine { error("No network expected") })).configuration()
        assertEquals("", config.facebookAppId)
    }
}
