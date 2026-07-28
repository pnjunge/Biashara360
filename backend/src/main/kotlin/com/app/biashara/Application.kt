package com.app.biashara
import com.app.biashara.db.seedSuperuser
import com.app.biashara.db.DatabaseFactory
import com.app.biashara.di.configureKoin
import com.app.biashara.plugins.*
import com.app.biashara.routes.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import com.app.biashara.models.HealthResponse
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import java.util.UUID
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val appConfig = HoconApplicationConfig(ConfigFactory.load())

    DatabaseFactory.init(appConfig)
    seedSuperuser()

    // DI
    configureKoin(appConfig)

    // Plugins
    configureSerialization()
    configureSecurity()
    configureCors()
    configureExceptionHandling()  // Global exception handler (single StatusPages installation)
    configureDefaultHeaders()
    configureRateLimiting()
    install(CallId) {
        retrieveFromHeader("X-Request-ID")
        generate { UUID.randomUUID().toString() }
        verify { it.length in 8..128 && it.all { char -> char.isLetterOrDigit() || char in "-_." } }
        replyToHeader("X-Request-ID")
    }
    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
        callIdMdc("requestId")
        format { call ->
            val requestId = call.callId ?: "unknown"
            val status = call.response.status()?.value ?: 0
            """{"event":"http_request","request_id":"$requestId","method":"${call.request.httpMethod.value}","path":"${call.request.path()}","status":$status}"""
        }
    }

    // Routes
    routing {
        // Public routes (no auth)
        route("/v1") {
            healthRoutes()  // Comprehensive health checks
            authRoutesValidated()
            // Mpesa Daraja callback — called by Safaricom, no JWT required
            mpesaCallbackRoute()
            publicBusinessRoutes()
            cyberSourcePublicRoutes()
        }

        // Protected routes (JWT required)
        authenticate("jwt-auth") {
            route("/v1") {
                accountRoutesValidated()
                dashboardRoute()
                productRoutes()
                orderRoutes()
                customerRoutes()
                expenseRoutes()
                paymentRoutes()
                reportRoutes()
                userRoutes()
                cyberSourceRoutes()
                taxRoutes()
                kraRoutes()
                socialRoutes()
                superAdminRoutes()
                businessSettingsRoutes()
                businessProfileRoutes()
            }
        }
        // Public webhook routes — no auth
        route("/v1") {
            socialWebhookRoutes()
        }
    }
}
