package com.app.biashara.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
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

    fun init(config: ApplicationConfig) {
        secret = config.property("jwt.secret").getString()
        issuer = config.property("jwt.issuer").getString()
        audience = config.property("jwt.audience").getString()
        accessTokenExpiry = config.property("jwt.accessTokenExpiry").getString().toLong()
        refreshTokenExpiry = config.property("jwt.refreshTokenExpiry").getString().toLong()
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
