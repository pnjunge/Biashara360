package com.app.biashara.plugins

import com.app.biashara.cache.DistributedRateLimiter
import com.app.biashara.cache.RateLimitStore
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.app.biashara.auth.JwtUtils
import com.app.biashara.db.BusinessesTable
import com.app.biashara.models.ApiResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.ratelimit.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

fun Application.configureSecurity() {
    JwtUtils.init(environment.config)

    install(Authentication) {
        jwt("jwt-auth") {
            realm = JwtUtils.getRealm()
            verifier(
                JWT.require(Algorithm.HMAC256(JwtUtils.getSecret()))
                    .withIssuer(JwtUtils.getIssuer())
                    .withAudience(JwtUtils.getAudience())
                    .build()
            )
            validate { credential ->
                val type = credential.payload.getClaim("type").asString()
                val businessId = credential.payload.getClaim("businessId").asString()
                val role = credential.payload.getClaim("role").asString()
                val businessCanAccess = role == "SUPERADMIN" || (!businessId.isNullOrBlank() && transaction {
                    BusinessesTable
                        .slice(BusinessesTable.isActive, BusinessesTable.subscriptionEnabled)
                        .select { BusinessesTable.id eq businessId }
                        .firstOrNull()
                        ?.let {
                            it[BusinessesTable.isActive] &&
                                it[BusinessesTable.subscriptionEnabled]
                        } == true
                })
                if (type == "access" && businessCanAccess) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(false, message = "Token is not valid or has expired")
                )
            }
        }
    }
}

fun Application.configureCors() {
    install(CORS) {
        // 🔒 SECURITY FIX: Restrict CORS to specific domains instead of anyHost()
        // Update these domains to match your actual frontend domains
        val allowedDomains = this@configureCors.environment.config.propertyOrNull("cors.allowedDomains")
            ?.getList()
            ?: listOf(
                // Vite dev server — tries 3000 → 3001 → 3002 etc. if ports are taken
                "localhost:3000",
                "localhost:3001",
                "localhost:3002",
                "localhost:3003",
                "localhost:3004",
                "localhost:5173", // Vite default
                "localhost:8080",
                // Production
                "biashara360.co.ke",
                "app.biashara360.co.ke",
                "admin.biashara360.co.ke",
                "enw9p7mvty.us-east-1.awsapprunner.com"
            )
        
        allowedDomains.forEach { domain ->
            allowHost(domain)
        }
        
        // In production, use:
        // allowHost("app.biashara360.com")
        // allowHost("admin.biashara360.com")
        
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Client-Platform")
        allowHeader("X-Tenant-ID")
        allowCredentials = true
        maxAgeInSeconds = 86400 // 24 hours
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(false, message = cause.message ?: "Bad request")
                )
                is IllegalStateException -> call.respond(
                    HttpStatusCode.Conflict,
                    ApiResponse<Unit>(false, message = cause.message ?: "Conflict")
                )
                else -> {
                    call.application.log.error("Unhandled error", cause)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Unit>(false, message = "An internal error occurred")
                    )
                }
            }
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, message = "Resource not found"))
        }
        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(HttpStatusCode.Unauthorized, ApiResponse<Unit>(false, message = "Unauthorized"))
        }
    }
}

fun Application.configureDefaultHeaders() {
    install(DefaultHeaders) {
        // 🔒 SECURITY: Comprehensive security headers
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
        header("Content-Security-Policy", "default-src 'self'")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
    }
}

fun Application.configureRateLimiting() {
    val rateLimitStore = get<RateLimitStore>()
    install(RateLimit) {
        register(RateLimitName("auth-limiter")) {
            rateLimiter { _, key ->
                DistributedRateLimiter(rateLimitStore, "auth", key.toString(), limit = 10, windowSeconds = 60)
            }
            // Use X-Forwarded-For so per-client limiting works behind AWS/Nginx proxies
            requestKey { call ->
                call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
                    ?: call.request.origin.remoteHost
            }
        }
        register(RateLimitName("public-payment-limiter")) {
            rateLimiter { _, key ->
                DistributedRateLimiter(rateLimitStore, "public-payment", key.toString(), limit = 10, windowSeconds = 60)
            }
            requestKey { call -> call.request.origin.remoteHost }
        }
    }
}
