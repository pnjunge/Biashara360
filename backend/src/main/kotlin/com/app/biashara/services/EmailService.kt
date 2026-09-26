package com.app.biashara.services

import com.app.biashara.models.EmailConfigStatusResponse
import com.app.biashara.models.SmtpSettingsRequest
import io.ktor.server.config.*
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

private const val KEY_SMTP_HOST = "smtp_host"
private const val KEY_SMTP_PORT = "smtp_port"
private const val KEY_SMTP_USERNAME = "smtp_username"
private const val KEY_SMTP_PASSWORD = "smtp_password"
private const val KEY_SMTP_FROM_EMAIL = "smtp_from_email"
private const val KEY_SMTP_FROM_NAME = "smtp_from_name"

/**
 * Sends OTP verification emails and notifications via SMTP (Jakarta Mail).
 * Supports Oracle Cloud Infrastructure (OCI) Email Delivery (smtp.email.<region>.oci.oraclecloud.com:587),
 * Gmail (smtp.gmail.com:587), Outlook 365 (smtp.office365.com:587), and custom SMTP servers.
 *
 * Configure credentials in system settings, application.conf, or via environment variables:
 *   SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, SMTP_FROM_EMAIL, SMTP_FROM_NAME
 */
class EmailService(
    private val config: ApplicationConfig,
    private val settingsService: SystemSettingsService? = null
) {

    fun getHost(): String =
        settingsService?.getSetting(KEY_SMTP_HOST)?.takeIf { it.isNotBlank() }
            ?: config.propertyOrNull("smtp.host")?.getString()?.takeIf { it.isNotBlank() }
            ?: System.getenv("SMTP_HOST")?.takeIf { it.isNotBlank() }
            ?: "smtp.email.af-johannesburg-1.oci.oraclecloud.com"

    fun getPort(): Int =
        settingsService?.getSetting(KEY_SMTP_PORT)?.toIntOrNull()
            ?: config.propertyOrNull("smtp.port")?.getString()?.toIntOrNull()
            ?: System.getenv("SMTP_PORT")?.toIntOrNull()
            ?: 587

    fun getUsername(): String =
        settingsService?.getSetting(KEY_SMTP_USERNAME)?.takeIf { it.isNotBlank() }
            ?: config.propertyOrNull("smtp.username")?.getString()?.takeIf { it.isNotBlank() }
            ?: System.getenv("SMTP_USERNAME")?.takeIf { it.isNotBlank() }
            ?: ""

    fun getPassword(): String =
        settingsService?.getSetting(KEY_SMTP_PASSWORD)?.takeIf { it.isNotBlank() }
            ?: config.propertyOrNull("smtp.password")?.getString()?.takeIf { it.isNotBlank() }
            ?: System.getenv("SMTP_PASSWORD")?.takeIf { it.isNotBlank() }
            ?: ""

    fun getFromEmail(): String =
        settingsService?.getSetting(KEY_SMTP_FROM_EMAIL)?.takeIf { it.isNotBlank() }
            ?: config.propertyOrNull("smtp.fromEmail")?.getString()?.takeIf { it.isNotBlank() }
            ?: System.getenv("SMTP_FROM_EMAIL")?.takeIf { it.isNotBlank() }
            ?: getUsername().takeIf { it.contains("@") && !it.startsWith("ocid1.") }
            ?: "noreply@biashara360.co.ke"

    fun getFromName(): String =
        settingsService?.getSetting(KEY_SMTP_FROM_NAME)?.takeIf { it.isNotBlank() }
            ?: config.propertyOrNull("smtp.fromName")?.getString()?.takeIf { it.isNotBlank() }
            ?: System.getenv("SMTP_FROM_NAME")?.takeIf { it.isNotBlank() }
            ?: "Biashara360"

    /**
     * Returns true if the service is properly configured with SMTP credentials.
     */
    fun isConfigured(): Boolean = getUsername().isNotBlank() && getPassword().isNotBlank()

    fun getConfigStatus(): EmailConfigStatusResponse {
        return EmailConfigStatusResponse(
            configured = isConfigured(),
            host = getHost(),
            port = getPort(),
            username = getUsername(),
            fromEmail = getFromEmail(),
            fromName = getFromName()
        )
    }

    fun updateSettings(req: SmtpSettingsRequest) {
        settingsService?.let { svc ->
            req.host?.let { svc.saveSetting(KEY_SMTP_HOST, it.trim()) }
            req.port?.let { svc.saveSetting(KEY_SMTP_PORT, it.toString()) }
            req.username?.let { svc.saveSetting(KEY_SMTP_USERNAME, it.trim()) }
            req.password?.takeIf { it.isNotBlank() }?.let { svc.saveSetting(KEY_SMTP_PASSWORD, it) }
            req.fromEmail?.let { svc.saveSetting(KEY_SMTP_FROM_EMAIL, it.trim()) }
            req.fromName?.let { svc.saveSetting(KEY_SMTP_FROM_NAME, it.trim()) }
        }
    }

    /**
     * Send an OTP verification email to the user.
     */
    fun sendOtpEmail(to: String, otp: String, userName: String): Result<Unit> {
        return sendHtmlEmail(
            to = to,
            emailSubject = "Biashara360 — Verification Code",
            html = buildOtpEmailHtml(otp, userName)
        )
    }

    fun sendInvitationEmail(to: String, code: String, userName: String): Result<Unit> {
        return sendHtmlEmail(
            to = to,
            emailSubject = "Set up your Biashara360 account",
            html = buildInvitationEmailHtml(code, userName)
        )
    }

    /**
     * Sends a test email to verify Oracle Cloud (OCI) Email Delivery / SMTP connectivity.
     */
    fun sendTestEmail(to: String): Result<Unit> {
        val currentHost = getHost()
        val currentPort = getPort()
        val currentFrom = getFromEmail()
        val currentName = getFromName()
        val providerName = when {
            currentHost.contains("oraclecloud.com") || currentHost.contains("oracle") -> "Oracle Cloud (OCI) Email Delivery"
            currentHost.contains("office365.com") || currentHost.contains("outlook") -> "Microsoft Outlook 365"
            currentHost.contains("gmail.com") -> "Google Gmail"
            else -> "SMTP Server"
        }
        return sendHtmlEmail(
            to = to,
            emailSubject = "Biashara360 — Email Connection Test",
            html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:24px;font-family:Arial,sans-serif;background:#f4f7fa;color:#334155;">
                    <div style="max-width:480px;margin:auto;background:#ffffff;border-radius:12px;padding:32px;box-shadow:0 4px 12px rgba(0,0,0,0.05);">
                        <h2 style="color:#16a34a;margin-top:0;">Biashara360 Email Test</h2>
                        <p>This is a verification email from Biashara360.</p>
                        <p style="color:#16a34a;font-weight:bold;">Connection to $providerName ($currentHost:$currentPort) was established successfully!</p>
                        <ul style="color:#64748b;font-size:14px;line-height:1.8;">
                            <li><strong>Provider:</strong> $providerName</li>
                            <li><strong>Host:</strong> $currentHost:$currentPort</li>
                            <li><strong>From:</strong> $currentFrom ($currentName)</li>
                        </ul>
                    </div>
                </body>
                </html>
            """.trimIndent()
        )
    }

    private fun sendHtmlEmail(to: String, emailSubject: String, html: String): Result<Unit> {
        val currentUsername = getUsername()
        val currentPassword = getPassword()
        val currentHost = getHost()
        val currentPort = getPort()
        val currentFromEmail = getFromEmail()
        val currentFromName = getFromName()

        if (!isConfigured()) {
            println("[EmailService] SMTP credentials not configured; email was not sent")
            return Result.failure(IllegalStateException("SMTP credentials not configured (username or password missing)"))
        }

        return try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
                put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
                put("mail.smtp.host", currentHost)
                put("mail.smtp.port", currentPort.toString())
                put("mail.smtp.ssl.trust", currentHost)
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.timeout", "10000")
                put("mail.smtp.writetimeout", "10000")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(currentUsername, currentPassword)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(currentFromEmail, currentFromName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = emailSubject
                setContent(html, "text/html; charset=utf-8")
            }

            Transport.send(message)
            println("[EmailService] Email sent successfully to $to")
            Result.success(Unit)
        } catch (e: Exception) {
            println("[EmailService] ✗ Failed to send email to $to: ${e.message}")
            Result.failure(e)
        }
    }

    private fun buildInvitationEmailHtml(code: String, userName: String): String = """
        <!DOCTYPE html>
        <html>
        <body style="margin:0;padding:24px;font-family:Arial,sans-serif;background:#f4f7fa;color:#334155;">
            <div style="max-width:480px;margin:auto;background:#ffffff;border-radius:12px;padding:32px;">
                <h1 style="color:#16a34a;font-size:24px;">Welcome to Biashara360</h1>
                <p>Hello <strong>$userName</strong>,</p>
                <p>An administrator invited you to Biashara360. Use the one-time code below on the
                   <strong>Forgot password</strong> screen to choose your own password.</p>
                <div style="margin:24px 0;padding:16px;text-align:center;background:#f0fdf4;border-radius:10px;">
                    <span style="font-size:32px;font-weight:800;letter-spacing:8px;color:#16a34a;">$code</span>
                </div>
                <p>This code expires in <strong>10 minutes</strong>. If you were not expecting this invitation,
                   do not share the code and contact your administrator.</p>
            </div>
        </body>
        </html>
    """.trimIndent()

    /**
     * Build a branded HTML email body for OTP delivery.
     */
    private fun buildOtpEmailHtml(otp: String, userName: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin:0;padding:0;font-family:'Segoe UI',Roboto,Arial,sans-serif;background-color:#f4f7fa;">
            <div style="max-width:480px;margin:40px auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                <!-- Header -->
                <div style="background:linear-gradient(135deg,#16a34a,#15803d);padding:32px 24px;text-align:center;">
                    <h1 style="color:#ffffff;margin:0;font-size:24px;font-weight:700;">Biashara360</h1>
                    <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:14px;">Business Management Platform</p>
                </div>
                <!-- Body -->
                <div style="padding:32px 24px;">
                    <p style="color:#334155;font-size:16px;margin:0 0 16px;">Hello <strong>${userName}</strong>,</p>
                    <p style="color:#64748b;font-size:14px;line-height:1.6;margin:0 0 24px;">
                        Use the verification code below to complete your sign-in. This code expires in <strong>10 minutes</strong>.
                    </p>
                    <!-- OTP Code -->
                    <div style="text-align:center;margin:24px 0;">
                        <div style="display:inline-block;background:#f0fdf4;border:2px dashed #16a34a;border-radius:12px;padding:16px 40px;">
                            <span style="font-size:36px;font-weight:800;letter-spacing:8px;color:#16a34a;font-family:monospace;">$otp</span>
                        </div>
                    </div>
                    <p style="color:#94a3b8;font-size:13px;text-align:center;margin:24px 0 0;">
                        If you didn't request this code, please ignore this email.
                    </p>
                </div>
                <!-- Footer -->
                <div style="background:#f8fafc;padding:16px 24px;text-align:center;border-top:1px solid #e2e8f0;">
                    <p style="color:#94a3b8;font-size:12px;margin:0;">
                        &copy; 2026 Biashara360. All rights reserved.<br>
                        Nairobi, Kenya
                    </p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
