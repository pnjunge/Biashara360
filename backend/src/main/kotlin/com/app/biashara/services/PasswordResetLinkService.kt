package com.app.biashara.services

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.db.*
import com.app.biashara.models.ApiResponse
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object PasswordResetLinksTable : Table("password_reset_links") {
    val tokenHash = varchar("token_hash", 64)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(tokenHash)
}

/** Secrets are delivered once, stored as hashes, and never returned by the public API. */
class PasswordResetLinkService(
    private val baseUrl: String,
    private val deliver: (email: String, phone: String, name: String, link: String, channel: String) -> Result<Unit>
) {
    private fun hash(token: String) = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray()).joinToString("") { "%02x".format(it) }

    fun request(email: String, channel: String): ApiResponse<Unit> {
        data class Dispatch(val email: String, val phone: String, val name: String, val token: String)
        val dispatch = transaction {
            val user = UsersTable.select { (UsersTable.email.lowerCase() eq email.trim().lowercase()) and
                (UsersTable.isActive eq true) }.forUpdate().firstOrNull() ?: return@transaction null
            val userId = user[UsersTable.id]
            val now = Clock.System.now()
            if (PasswordResetLinksTable.select { (PasswordResetLinksTable.userId eq userId) and
                    (PasswordResetLinksTable.createdAt greater now - 60.seconds) }.any()) return@transaction null
            if (channel == "WHATSAPP" && user[UsersTable.phone].isBlank()) return@transaction null
            val token = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
            PasswordResetLinksTable.deleteWhere { PasswordResetLinksTable.userId eq userId }
            OtpTable.deleteWhere { (OtpTable.userId eq userId) and (OtpTable.channel eq "PASSWORD_RESET") }
            PasswordResetLinksTable.insert {
                it[tokenHash] = hash(token)
                it[PasswordResetLinksTable.userId] = userId
                it[expiresAt] = now + 10.minutes
                it[createdAt] = now
            }
            Dispatch(user[UsersTable.email], user[UsersTable.phone], user[UsersTable.name], token)
        }
        dispatch?.let { dispatch ->
            // Fragment prevents the token being sent in HTTP URLs, access logs or Referer headers.
            val result = runCatching { deliver(dispatch.email, dispatch.phone, dispatch.name, "$baseUrl#token=${dispatch.token}", channel) }.getOrElse { Result.failure(it) }
            if (result.isFailure) {
                transaction { PasswordResetLinksTable.deleteWhere { tokenHash eq hash(dispatch.token) } }
                println("[PasswordReset] $channel delivery failed; reset link revoked")
            }
        }
        // Identical for absent/inactive accounts and delivery failures: no account enumeration.
        return ApiResponse(true, message = "If the account is eligible, a reset link will be sent via ${if (channel == "WHATSAPP") "WhatsApp to its registered phone" else "email"}. The link expires in 10 minutes.")
    }

    fun confirm(token: String, newPassword: String): ApiResponse<Unit> {
        if (!token.matches(Regex("[a-f0-9]{64}")) || newPassword.length < 8 || !com.app.biashara.utils.ValidationUtils.isValidPassword(newPassword))
            return ApiResponse(false, message = "Invalid reset link or password")
        return transaction {
            val digest = hash(token)
            val candidate = PasswordResetLinksTable.select { PasswordResetLinksTable.tokenHash eq digest }.firstOrNull()
                ?: return@transaction ApiResponse(false, message = "Invalid or expired reset link")
            val userId = candidate[PasswordResetLinksTable.userId]
            val user = UsersTable.select { (UsersTable.id eq userId) and (UsersTable.isActive eq true) }.forUpdate().firstOrNull()
                ?: return@transaction ApiResponse(false, message = "Invalid or expired reset link")
            val link = PasswordResetLinksTable.select { (PasswordResetLinksTable.tokenHash eq digest) and
                (PasswordResetLinksTable.expiresAt greater Clock.System.now()) }.forUpdate().firstOrNull()
                ?: return@transaction ApiResponse(false, message = "Invalid or expired reset link")
            UsersTable.update({ UsersTable.id eq user[UsersTable.id] }) {
                it[passwordHash] = PasswordUtils.hash(newPassword)
                it[updatedAt] = Clock.System.now()
                it[tokenValidAfter] = Clock.System.now()
            }
            PasswordResetLinksTable.deleteWhere { PasswordResetLinksTable.userId eq link[PasswordResetLinksTable.userId] }
            OtpTable.deleteWhere { (OtpTable.userId eq userId) and (OtpTable.channel eq "PASSWORD_RESET") }
            RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
            ApiResponse(true, message = "Password reset successfully. Sign in with your new password.")
        }
    }
}
