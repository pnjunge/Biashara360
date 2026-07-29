package com.app.biashara.services

import io.ktor.server.config.*
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

/**
 * Sends OTP verification emails via SMTP (Jakarta Mail).
 *
 * Configure credentials in application.conf or via environment variables:
 *   SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, SMTP_FROM_EMAIL
 */
class EmailService(config: ApplicationConfig) {

    private val host: String = config.propertyOrNull("smtp.host")?.getString() ?: ""
    private val port: Int = config.propertyOrNull("smtp.port")?.getString()?.toIntOrNull() ?: 0
    private val username: String = config.propertyOrNull("smtp.username")?.getString() ?: ""
    private val password: String = config.propertyOrNull("smtp.password")?.getString() ?: ""
    private val fromEmail: String = config.propertyOrNull("smtp.fromEmail")?.getString() ?: ""
    private val fromName: String = config.propertyOrNull("smtp.fromName")?.getString() ?: "Biashara360"

    /**
     * Returns true if the service is properly configured with SMTP credentials.
     */
    fun isConfigured(): Boolean = username.isNotBlank() && password.isNotBlank()

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

    private fun sendHtmlEmail(to: String, emailSubject: String, html: String): Result<Unit> {
        if (!isConfigured()) {
            println("[EmailService] SMTP credentials not configured; email was not sent")
            return Result.failure(IllegalStateException("SMTP credentials not configured"))
        }

        return try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", host)
                put("mail.smtp.port", port.toString())
                put("mail.smtp.ssl.trust", host)
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(username, password)
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromEmail, fromName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                subject = emailSubject
                setContent(html, "text/html; charset=utf-8")
            }

            Transport.send(message)
            println("[EmailService] OTP email sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            println("[EmailService] ✗ Failed to send email: ${e.message}")
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
