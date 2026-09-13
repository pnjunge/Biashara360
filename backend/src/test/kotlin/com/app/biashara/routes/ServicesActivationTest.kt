package com.app.biashara.routes

import com.app.biashara.models.ServiceScheduleResponse
import com.app.biashara.services.ServiceManagementService
import com.app.biashara.services.StorefrontService
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes

class ServicesActivationTest {
    @Test
    fun `activation is admin only and gates private and public booking`() = testApplication {
        environment { config = MapApplicationConfig() }
        val service = mockk<ServiceManagementService>()
        val storefront = mockk<StorefrontService>()
        var enabled = false
        every { service.isEnabled("business") } answers { enabled }
        every { service.setEnabled("business", any()) } answers {
            enabled = secondArg()
            mapOf("enabled" to enabled)
        }
        every { service.schedule("business", null, null) } returns ServiceScheduleResponse(emptyList(), emptyList(), emptyList())
        every { storefront.resolveActiveBusinessId("shop") } returns "business"
        val algorithm = Algorithm.HMAC256("activation-test-secret")
        fun token(role: String) = JWT.create().withSubject("user").withClaim("businessId", "business").withClaim("role", role).sign(algorithm)
        application {
            install(Koin) { modules(module { single { service }; single { storefront } }) }
            install(ContentNegotiation) { json() }
            install(Authentication) { jwt { verifier(JWT.require(algorithm).build()); validate { JWTPrincipal(it.payload) } } }
            install(RateLimit) { register(RateLimitName("public-payment-limiter")) { rateLimiter(limit = 30, refillPeriod = 1.minutes) } }
            routing { authenticate { serviceRoutes() }; storefrontRoutes() }
        }
        assertEquals(HttpStatusCode.OK, client.get("/services/status") { bearerAuth(token("STAFF")) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.get("/services") { bearerAuth(token("SUPERADMIN")) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/public/store/shop/appointments").status)
        assertEquals(HttpStatusCode.Forbidden, client.put("/services/enabled") {
            bearerAuth(token("STAFF")); contentType(ContentType.Application.Json); setBody("{\"enabled\":true}")
        }.status)
        verify(exactly = 0) { service.setEnabled(any(), any()) }
        assertEquals(HttpStatusCode.OK, client.put("/services/enabled") {
            bearerAuth(token("ADMIN")); contentType(ContentType.Application.Json); setBody("{\"enabled\":true}")
        }.status)
        assertTrue(enabled)
        assertEquals(HttpStatusCode.OK, client.get("/services") { bearerAuth(token("SUPERADMIN")) }.status)
        assertEquals(HttpStatusCode.BadRequest, client.put("/services/enabled") {
            bearerAuth(token("ADMIN")); contentType(ContentType.Application.Json); setBody("{}")
        }.status)
        assertTrue(enabled)
        assertEquals(HttpStatusCode.OK, client.put("/services/enabled") {
            bearerAuth(token("ADMIN")); contentType(ContentType.Application.Json); setBody("{\"enabled\":false}")
        }.status)
        assertFalse(enabled)
        assertEquals(HttpStatusCode.Forbidden, client.post("/services/appointments") { bearerAuth(token("ADMIN")) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/public/store/shop/appointments").status)
        verify(exactly = 0) { service.createAppointment(any(), any(), any()) }
    }
}
