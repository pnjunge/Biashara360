package com.app.biashara.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.app.biashara.auth.JwtUtils
import com.app.biashara.models.ApiResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json

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

    val env = environment
    install(Authentication) {
        jwt("jwt-auth") {
            realm = env.config.property("jwt.realm").getString()
            verifier(
                JWT.require(Algorithm.HMAC256(JwtUtils.getSecret()))
                    .withIssuer(JwtUtils.getIssuer())
                    .withAudience(JwtUtils.getAudience())
                    .build()
            )
            validate { credential ->
                val type = credential.payload.getClaim("type").asString()
                val businessId = credential.payload.getClaim("businessId").asString()
                if (type == "access" && !businessId.isNullOrBlank()) {
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
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Tenant-ID")
        // In production, restrict to your domains
        anyHost()
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
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
    }
}
