package com.app.biashara.services

import com.app.biashara.auth.*
import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.days
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class AuthService {

    fun register(req: RegisterRequest): ApiResponse<UserResponse> = transaction {
        val now = Clock.System.now()
        val businessId = generateId()
        val userId = generateId()

        BusinessesTable.insert {
            it[id]         = businessId
            it[name]       = req.businessName
            it[type]       = req.businessType
            it[ownerPhone] = req.phone
            it[ownerEmail] = req.email
            it[createdAt]  = now
            it[updatedAt]  = now
        }

        UsersTable.insert {
            it[id]           = userId
            it[UsersTable.businessId] = businessId
            it[name]         = req.name
            it[email]        = req.email
            it[phone]        = req.phone
            it[passwordHash] = PasswordUtils.hash(req.password)
            it[role]         = "ADMIN"
            it[createdAt]    = now
            it[updatedAt]    = now
        }

        val userResp = UserResponse(userId, req.name, req.email, req.phone, "ADMIN", businessId, "ENGLISH")
        ApiResponse(success = true, data = userResp, message = "Registration successful")
    }

    fun login(req: LoginRequest): ApiResponse<LoginResponse> = transaction {
        val user = UsersTable.select { UsersTable.email eq req.email }.firstOrNull()
            ?: return@transaction ApiResponse(false, message = "Invalid credentials")

        if (!PasswordUtils.verify(req.password, user[UsersTable.passwordHash]))
            return@transaction ApiResponse(false, message = "Invalid credentials")

        if (!user[UsersTable.isActive])
            return@transaction ApiResponse(false, message = "Account is deactivated")

        val userId = user[UsersTable.id]

        if (user[UsersTable.twoFactorEnabled]) {
            val otp = OtpUtils.generate()
            val now = Clock.System.now()

            OtpTable.deleteWhere { OtpTable.userId eq userId }
            OtpTable.insert {
                it[id]               = generateId()
                it[OtpTable.userId]  = userId
                it[code]             = otp
                it[channel]          = "SMS"
                it[used]             = false
                it[expiresAt]        = now + 600.seconds
                it[createdAt]        = now
            }
            println("OTP for $userId: $otp")
            ApiResponse(
                success = true,
                data = LoginResponse(userId, requiresOtp = true, otpChannels = listOf("SMS", "EMAIL")),
                message = "OTP sent"
            )
        } else {
            val auth = issueTokens(userId, user[UsersTable.businessId], user[UsersTable.role])
            ApiResponse(
                success = true,
                data = LoginResponse(
                    userId = userId,
                    requiresOtp = false,
                    otpChannels = emptyList(),
                    accessToken = auth.accessToken,
                    refreshToken = auth.refreshToken,
                    user = auth.user
                ),
                message = "Login successful"
            )
        }
    }

    fun verifyOtp(req: OtpVerifyRequest): ApiResponse<AuthResponse> = transaction {
        val now = Clock.System.now()
        val otpRow = OtpTable.select {
            (OtpTable.userId eq req.userId) and
            (OtpTable.code eq req.otp) and
            (OtpTable.used eq false) and
            (OtpTable.expiresAt greaterEq now)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Invalid or expired OTP")

        OtpTable.update({ OtpTable.id eq otpRow[OtpTable.id] }) { it[used] = true }

        val user = UsersTable.select { UsersTable.id eq req.userId }.firstOrNull()
            ?: return@transaction ApiResponse(false, message = "User not found")

        val auth = issueTokens(req.userId, user[UsersTable.businessId], user[UsersTable.role])
        ApiResponse(success = true, data = auth, message = "Login successful")
    }

    fun refreshToken(req: RefreshTokenRequest): ApiResponse<AuthResponse> = transaction {
        val now = Clock.System.now()
        val stored = RefreshTokensTable.select {
            (RefreshTokensTable.token eq req.refreshToken) and
            (RefreshTokensTable.expiresAt greaterEq now)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Invalid or expired refresh token")

        val userId = stored[RefreshTokensTable.userId]
        val user = UsersTable.select { UsersTable.id eq userId }.firstOrNull()
            ?: return@transaction ApiResponse(false, message = "User not found")

        val auth = issueTokens(userId, user[UsersTable.businessId], user[UsersTable.role])
        ApiResponse(success = true, data = auth, message = "Token refreshed")
    }

    private fun issueTokens(userId: String, businessId: String, role: String): AuthResponse {
        val accessToken  = JwtUtils.generateAccessToken(userId, businessId, role)
        val refreshToken = JwtUtils.generateRefreshToken(userId)
        val now = Clock.System.now()

        transaction {
            RefreshTokensTable.insert {
                it[id]                        = generateId()
                it[RefreshTokensTable.userId] = userId
                it[token]                     = refreshToken
                it[expiresAt]                 = now + 30.days
                it[createdAt]                 = now
            }
        }

        val userRow = transaction {
            UsersTable.select { UsersTable.id eq userId }.first()
        }
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = UserResponse(
                userId,
                userRow[UsersTable.name],
                userRow[UsersTable.email],
                userRow[UsersTable.phone],
                userRow[UsersTable.role],
                userRow[UsersTable.businessId],
                userRow[UsersTable.preferredLanguage]
            )
        )
    }
}
