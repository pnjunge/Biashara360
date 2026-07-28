package com.app.biashara.services

import com.app.biashara.auth.*
import com.app.biashara.db.*
import com.app.biashara.models.*
import com.app.biashara.models.EnableOtpRequest
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.days
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction

@kotlinx.serialization.Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

class AuthService(
    private val smsService: SmsService,
    private val emailService: EmailService,
    private val whatsappOtpService: WhatsAppOtpService
) {
    /**
     * Enable or disable OTP (two‑factor) for a user.
     * Returns the updated user information.
     */
    fun setOtpEnabled(req: EnableOtpRequest): ApiResponse<UserResponse> = transaction {
        UsersTable.update({ UsersTable.id eq req.userId }) {
            it[twoFactorEnabled] = req.enable
        }
        val userRow = UsersTable.select { UsersTable.id eq req.userId }.firstOrNull()
            ?: return@transaction ApiResponse(false, message = "User not found")
        // Single JOIN to get business name — avoids nested transaction
        val businessName = userRow[UsersTable.businessId]?.let { bizId ->
            BusinessesTable.slice(BusinessesTable.name)
                .select { BusinessesTable.id eq bizId }
                .firstOrNull()?.get(BusinessesTable.name)
        }
        val userResp = UserResponse(
            userRow[UsersTable.id],
            userRow[UsersTable.name],
            userRow[UsersTable.email],
            userRow[UsersTable.phone],
            userRow[UsersTable.role],
            userRow[UsersTable.businessId],
            userRow[UsersTable.preferredLanguage],
            businessName
        )
        ApiResponse(success = true, data = userResp, message = "OTP ${if (req.enable) "enabled" else "disabled"}")
    }

    fun refreshToken(req: RefreshTokenRequest): ApiResponse<AuthResponse> = transaction {
        val now = Clock.System.now()
        val decoded = try {
            JwtUtils.verifyToken(req.refreshToken)
        } catch (_: Exception) {
            return@transaction ApiResponse(false, message = "Invalid or expired refresh token")
        }
        if (decoded.getClaim("type").asString() != "refresh" || decoded.subject.isNullOrBlank()) {
            return@transaction ApiResponse(false, message = "Invalid or expired refresh token")
        }
        val stored = RefreshTokensTable.select {
            (RefreshTokensTable.token eq hashRefreshToken(req.refreshToken)) and
            (RefreshTokensTable.expiresAt greaterEq now)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Invalid or expired refresh token")
        val userId = stored[RefreshTokensTable.userId]
        if (decoded.subject != userId) {
            return@transaction ApiResponse(false, message = "Invalid or expired refresh token")
        }
        // Opportunistic cleanup: delete all expired tokens for this user
        RefreshTokensTable.deleteWhere {
            (RefreshTokensTable.userId eq userId) and
            RefreshTokensTable.expiresAt.less(now)
        }
        val user = UsersTable.select { UsersTable.id eq userId }.firstOrNull()
            ?: return@transaction ApiResponse(false, message = "User not found")
        if (!user[UsersTable.isActive]) {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.id eq stored[RefreshTokensTable.id] }
            return@transaction ApiResponse(false, message = "Account is deactivated")
        }
        businessAccessError(user[UsersTable.businessId])?.let { message ->
            RefreshTokensTable.deleteWhere { RefreshTokensTable.id eq stored[RefreshTokensTable.id] }
            return@transaction ApiResponse(false, message = message)
        }
        // Rotate atomically: a refresh token is single-use.
        RefreshTokensTable.deleteWhere { RefreshTokensTable.id eq stored[RefreshTokensTable.id] }
        val auth = issueTokens(userId, user[UsersTable.businessId], user[UsersTable.role])
        ApiResponse(success = true, data = auth, message = "Token refreshed")
    }

    fun register(req: RegisterRequest): ApiResponse<UserResponse> = transaction {
        val emailExists = UsersTable.select { UsersTable.email eq req.email }.count() > 0
        if (emailExists) return@transaction ApiResponse(false, message = "Email already registered")

        val phoneExists = UsersTable.select { UsersTable.phone eq req.phone }.count() > 0
        if (phoneExists) return@transaction ApiResponse(false, message = "Phone number already registered")

        val now = Clock.System.now()
        val businessId = generateId()
        val userId = generateId()

        BusinessesTable.insert {
            it[id] = businessId
            it[name] = req.businessName
            it[type] = req.businessType
            it[ownerPhone] = req.phone
            it[ownerEmail] = req.email
            it[createdAt] = now
            it[updatedAt] = now
        }

        UsersTable.insert {
            it[id] = userId
            it[UsersTable.businessId] = businessId
            it[name] = req.name
            it[email] = req.email
            it[phone] = req.phone
            it[passwordHash] = PasswordUtils.hash(req.password)
            it[role] = "ADMIN"
            it[twoFactorEnabled] = false
            it[createdAt] = now
            it[updatedAt] = now
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
        businessAccessError(user[UsersTable.businessId])?.let { message ->
            return@transaction ApiResponse(false, message = message)
        }
        val userId = user[UsersTable.id]
        if (user[UsersTable.twoFactorEnabled]) {
            val otp = OtpUtils.generate()
            val now = Clock.System.now()
            OtpTable.deleteWhere { OtpTable.userId eq userId }
            OtpTable.insert {
                it[id] = generateId()
                it[OtpTable.userId] = userId
                it[code] = otp
                it[channel] = "SMS"
                it[used] = false
                it[expiresAt] = now + 600.seconds
                it[createdAt] = now
            }
            val phone = user[UsersTable.phone]
            val email = user[UsersTable.email]
            val name = user[UsersTable.name]
            dispatchOtp(phone, email, name, otp)
            ApiResponse(
                success = true,
                data = LoginResponse(userId, requiresOtp = true, otpChannels = buildList {
                    if (whatsappOtpService.isConfigured()) add("WHATSAPP")
                    add("SMS"); add("EMAIL")
                }),
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
        if (!user[UsersTable.isActive]) {
            return@transaction ApiResponse(false, message = "Account is deactivated")
        }
        businessAccessError(user[UsersTable.businessId])?.let { message ->
            return@transaction ApiResponse(false, message = message)
        }
        val auth = issueTokens(req.userId, user[UsersTable.businessId], user[UsersTable.role])
        ApiResponse(success = true, data = auth, message = "Login successful")
    }

    fun resendOtp(req: ResendOtpRequest): ApiResponse<Unit> {
        data class OtpDispatch(val phone: String, val email: String, val name: String, val otp: String)

        val dispatch: OtpDispatch = transaction {
            val user = UsersTable.select { UsersTable.id eq req.userId }.firstOrNull()
                ?: return@transaction null
            val otp = OtpUtils.generate()
            val now = Clock.System.now()
            OtpTable.deleteWhere { OtpTable.userId eq req.userId }
            OtpTable.insert {
                it[id] = generateId()
                it[OtpTable.userId] = req.userId
                it[code] = otp
                it[channel] = req.channel.uppercase()
                it[used] = false
                it[expiresAt] = now + 600.seconds
                it[createdAt] = now
            }
            OtpDispatch(user[UsersTable.phone], user[UsersTable.email], user[UsersTable.name], otp)
        } ?: return ApiResponse(false, message = "User not found")

        // Dispatch OTP outside the transaction — avoids runBlocking on a DB thread
        when (req.channel.uppercase()) {
            "WHATSAPP" -> {
                runBlocking { whatsappOtpService.sendOtp(dispatch.phone, dispatch.otp) }
                println("[AuthService] OTP resent via WhatsApp")
            }
            "SMS" -> {
                runBlocking { smsService.sendOtp(dispatch.phone, dispatch.otp) }
                println("[AuthService] OTP resent via SMS to ${dispatch.phone}")
            }
            "EMAIL" -> {
                emailService.sendOtpEmail(dispatch.email, dispatch.otp, dispatch.name)
                println("[AuthService] OTP resent via EMAIL to ${dispatch.email}")
            }
            else -> dispatchOtp(dispatch.phone, dispatch.email, dispatch.name, dispatch.otp)
        }
        return ApiResponse(success = true, message = "OTP resent via ${req.channel}")
    }

    fun requestPasswordReset(email: String): ApiResponse<Unit> {
        data class ResetDispatch(val email: String, val name: String, val code: String)
        val dispatch = transaction {
            val user = UsersTable.select { UsersTable.email eq email.trim() }.firstOrNull()
                ?: return@transaction null
            val code = OtpUtils.generate()
            val now = Clock.System.now()
            OtpTable.deleteWhere {
                (OtpTable.userId eq user[UsersTable.id]) and (OtpTable.channel eq "PASSWORD_RESET")
            }
            OtpTable.insert {
                it[id] = generateId()
                it[OtpTable.userId] = user[UsersTable.id]
                it[OtpTable.code] = code
                it[channel] = "PASSWORD_RESET"
                it[used] = false
                it[expiresAt] = now + 600.seconds
                it[createdAt] = now
            }
            ResetDispatch(user[UsersTable.email], user[UsersTable.name], code)
        }
        dispatch?.let {
            emailService.sendOtpEmail(it.email, it.code, it.name)
        }
        return ApiResponse(
            success = true,
            message = "If an account exists for that email, a reset code has been sent."
        )
    }

    fun confirmPasswordReset(token: String, newPassword: String): ApiResponse<Unit> = transaction {
        val now = Clock.System.now()
        val reset = OtpTable.select {
            (OtpTable.code eq token) and
                (OtpTable.channel eq "PASSWORD_RESET") and
                (OtpTable.used eq false) and
                (OtpTable.expiresAt greaterEq now)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Invalid or expired reset code")
        val userId = reset[OtpTable.userId]
        UsersTable.update({ UsersTable.id eq userId }) {
            it[passwordHash] = PasswordUtils.hash(newPassword)
            it[updatedAt] = now
        }
        OtpTable.update({ OtpTable.id eq reset[OtpTable.id] }) { it[used] = true }
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        ApiResponse(success = true, message = "Password reset successfully. Sign in with your new password.")
    }

    fun changePassword(userId: String, req: ChangePasswordRequest): ApiResponse<Unit> = transaction {
        if (req.newPassword.length < 8) {
            return@transaction ApiResponse(false, message = "New password must be at least 8 characters")
        }
        val user = UsersTable.select { UsersTable.id eq userId }.firstOrNull()
            ?: return@transaction ApiResponse(false, message = "User not found")
        if (!PasswordUtils.verify(req.currentPassword, user[UsersTable.passwordHash])) {
            return@transaction ApiResponse(false, message = "Current password is incorrect")
        }
        if (PasswordUtils.verify(req.newPassword, user[UsersTable.passwordHash])) {
            return@transaction ApiResponse(false, message = "New password must be different from the current password")
        }
        UsersTable.update({ UsersTable.id eq userId }) {
            it[passwordHash] = PasswordUtils.hash(req.newPassword)
            it[updatedAt] = Clock.System.now()
        }
        // Password changes invalidate every remembered device/session.
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        ApiResponse(success = true, message = "Password changed successfully")
    }

    private fun dispatchOtp(phone: String, email: String, name: String, otp: String) {
        // Do NOT log OTP values — keep auth codes out of log aggregators
        if (phone.isNotBlank()) {
            try {
                runBlocking { whatsappOtpService.sendOtp(phone, otp) }
            } catch (e: Exception) {
                println("[AuthService] WhatsApp OTP dispatch failed: ${e.message}")
            }
        }
        if (phone.isNotBlank()) {
            try {
                runBlocking { smsService.sendOtp(phone, otp) }
            } catch (e: Exception) {
                println("[AuthService] SMS dispatch failed")
            }
        }
        if (email.isNotBlank()) {
            try {
                emailService.sendOtpEmail(email, otp, name)
            } catch (e: Exception) {
                println("[AuthService] Email dispatch failed")
            }
        }
    }

    private fun businessAccessError(businessId: String?): String? {
        if (businessId == null) return null
        val business = BusinessesTable.select { BusinessesTable.id eq businessId }.firstOrNull()
            ?: return "Business account was not found"
        return when {
            !business[BusinessesTable.isActive] -> "Business account is disabled"
            !business[BusinessesTable.subscriptionEnabled] -> "Subscription is disabled. Contact Biashara360 support."
            else -> null
        }
    }

    private fun issueTokens(userId: String, businessId: String?, role: String): AuthResponse {
        val accessToken = JwtUtils.generateAccessToken(userId, businessId, role)
        val refreshToken = JwtUtils.generateRefreshToken(userId)
        val now = Clock.System.now()
        transaction {
            RefreshTokensTable.insert {
                it[id] = generateId()
                it[RefreshTokensTable.userId] = userId
                it[token] = hashRefreshToken(refreshToken)
                it[expiresAt] = now + 30.days
                it[createdAt] = now
            }
        }
        val userRow = transaction { UsersTable.select { UsersTable.id eq userId }.first() }
        val businessName = businessId?.let { bizId ->
            transaction {
                BusinessesTable.slice(BusinessesTable.name).select { BusinessesTable.id eq bizId }
                    .firstOrNull()?.get(BusinessesTable.name)
            }
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
                userRow[UsersTable.preferredLanguage],
                businessName
            )
        )
    }
}
