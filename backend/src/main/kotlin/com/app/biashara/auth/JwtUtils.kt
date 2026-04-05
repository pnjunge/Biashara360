package com.app.biashara.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.mindrot.jbcrypt.BCrypt
import java.util.*

object JwtUtils {
    private lateinit var secret: String
    private lateinit var issuer: String
    private lateinit var audience: String
    private var accessTokenExpiry: Long = 3600
    private var refreshTokenExpiry: Long = 2592000

    /**
     * Resolves a config property with a fallback chain:
     *   1. ApplicationConfig property (e.g. from application-docker.conf)
     *   2. System environment variable [envKey]
     *   3. [default] value (null means the property is required)
     *
     * Throws [IllegalStateException] when no value is found and [default] is null.
     */
    private fun resolve(config: ApplicationConfig, key: String, envKey: String, default: String? = null): String {
        return try {
            config.property(key).getString().takeIf { it.isNotBlank() }
                ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }
                ?: default
                ?: error("Required configuration '$key' not found in config or environment variable '$envKey'")
        } catch (e: Exception) {
            // config.property() throws when the key is absent — fall through to env / default
            if (e is IllegalStateException && e.message?.startsWith("Required configuration") == true) throw e
            System.getenv(envKey)?.takeIf { it.isNotBlank() }
                ?: default
                ?: error("Required configuration '$key' not found in config or environment variable '$envKey'")
        }
    }

    fun init(config: ApplicationConfig) {
        secret = resolve(config, "jwt.secret", "JWT_SECRET")
        issuer = resolve(config, "jwt.issuer", "JWT_ISSUER", "biashara360.co.ke")
        audience = resolve(config, "jwt.audience", "JWT_AUDIENCE", "biashara360-users")
        accessTokenExpiry = resolve(config, "jwt.accessTokenExpiry", "JWT_ACCESS_TOKEN_EXPIRY", "3600").toLong()
        refreshTokenExpiry = resolve(config, "jwt.refreshTokenExpiry", "JWT_REFRESH_TOKEN_EXPIRY", "2592000").toLong()
    }

    fun generateAccessToken(userId: String, businessId: String, role: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withClaim("businessId", businessId)
            .withClaim("role", role)
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + accessTokenExpiry * 1000))
            .sign(Algorithm.HMAC256(secret))
    }

    fun generateRefreshToken(userId: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenExpiry * 1000))
            .sign(Algorithm.HMAC256(secret))
    }

    fun verifyToken(token: String) = JWT.require(Algorithm.HMAC256(secret))
        .withIssuer(issuer)
        .withAudience(audience)
        .build()
        .verify(token)

    fun getSecret() = secret
    fun getIssuer() = issuer
    fun getAudience() = audience
}

object PasswordUtils {
    fun hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(12))
    fun verify(password: String, hash: String): Boolean = BCrypt.checkpw(password, hash)
}

object OtpUtils {
    fun generate(length: Int = 6): String {
        val digits = "0123456789"
        return (1..length).map { digits.random() }.joinToString("")
    }
}

fun generateId(): String = UUID.randomUUID().toString()
