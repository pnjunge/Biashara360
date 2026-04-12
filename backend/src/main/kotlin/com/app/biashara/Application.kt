package com.app.biashara

import com.app.biashara.db.DatabaseFactory
import com.app.biashara.di.configureKoin
import com.app.biashara.plugins.*
import com.app.biashara.routes.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Database
    DatabaseFactory.init(environment.config)

    // DI
    configureKoin()

    // Plugins
    configureSerialization()
    configureSecurity()
    configureCors()
    configureStatusPages()
    configureDefaultHeaders()

    // Routes
    routing {
        // Public routes (no auth)
        route("/v1") {
            authRoutes()

            // Mpesa callback is public (called by Safaricom)
            post("/payments/mpesa/callback") {
                // Handled in paymentRoutes — mounted separately below
            }
        }

        // Protected routes (JWT required)
        authenticate("jwt-auth") {
            route("/v1") {
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
                settingsRoutes()
                superAdminRoutes()
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
