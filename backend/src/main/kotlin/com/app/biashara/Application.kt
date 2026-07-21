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
    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
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
        // CyberSource capture-context is public (called before user auth to init widget)
        route("/v1") {
            cyberSourcePublicRoutes()
        }
    }
}
