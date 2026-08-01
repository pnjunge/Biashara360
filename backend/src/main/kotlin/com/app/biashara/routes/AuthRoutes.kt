package com.app.biashara.routes

import com.app.biashara.constants.Constants
import com.app.biashara.exceptions.UnauthorizedException
import com.app.biashara.models.*
import com.app.biashara.dto.ResetPasswordRequestDTO
import com.app.biashara.dto.ResetPasswordConfirmDTO
import com.app.biashara.services.AuthService
import com.app.biashara.validation.Validator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val authLogger = LoggerFactory.getLogger("AuthenticationAudit")

/**
 * Authentication routes with comprehensive validation.
 * All inputs are validated before processing.
 */
fun Route.authRoutesValidated() {
    val authService: AuthService by inject()

    route("/auth") {
        /**
         * Register new user and business
         * POST /auth/register
         */
        post("/register") {
            val req = call.receive<RegisterRequest>()
            
            // Validate all registration fields
            Validator.validate {
                field("name", req.name) {
                    required()
                    length(2, 100)
                }
                field("email", req.email) {
                    required()
                    email()
                    maxLength(255)
                }
                field("phone", req.phone) {
                    required()
                    phone()
                }
                field("password", req.password) {
                    required()
                    password()
                }
                field("businessName", req.businessName) {
                    required()
                    length(2, 255)
                }
                field("businessType", req.businessType) {
                    required()
                    oneOf("RETAIL", "WHOLESALE", "RESTAURANT", "ECOMMERCE", "SERVICE", "MANUFACTURING", "OTHER")
                }
            }
            
            val result = authService.register(req)
            authLogger.info("""{"event":"registration_attempt","success":${result.success},"request_id":"${call.callId ?: "unknown"}"}""")
            call.respond(
                if (result.success) HttpStatusCode.Created else HttpStatusCode.BadRequest,
                result
            )
        }

        rateLimit(RateLimitName("auth-limiter")) {
            /**
             * Login user
             * POST /auth/login
             */
            post("/login") {
                val req = call.receive<LoginRequest>()
                
                // Validate login credentials
                Validator.validate {
                    field("email", req.email) {
                        required()
                        email()
                    }
                    field("password", req.password) {
                        required()
                        minLength(6)  // Basic check, actual validation done by auth service
                    }
                }
                
                val result = authService.login(req)
                authLogger.info("""{"event":"login_attempt","success":${result.success},"request_id":"${call.callId ?: "unknown"}"}""")
                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.Unauthorized,
                    result
                )
            }

            /**
             * Verify OTP for 2FA
             * POST /auth/verify-otp
             */
            post("/verify-otp") {
                val req = call.receive<OtpVerifyRequest>()
                
                // Validate OTP verification request
                Validator.validate {
                    field("userId", req.userId) {
                        required()
                        uuid()
                    }
                    field("otp", req.otp) {
                        required()
                        custom("OTP must be ${Constants.Auth.OTP_LENGTH} digits", "INVALID_OTP_FORMAT") {
                            it is String && it.matches(Regex("^\\d{${Constants.Auth.OTP_LENGTH}}\$"))
                        }
                    }
                    field("channel", req.channel) {
                        required()
                        oneOf("SMS", "EMAIL", "WHATSAPP")
                    }
                }
                
                val result = authService.verifyOtp(req)
                authLogger.info("""{"event":"otp_verification","success":${result.success},"user_id":"${req.userId}","channel":"${req.channel}","request_id":"${call.callId ?: "unknown"}"}""")
                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.Unauthorized,
                    result
                )
            }
        }

        /**
         * Refresh access token
         * POST /auth/refresh
         */
        rateLimit(RateLimitName("auth-limiter")) {
            post("/refresh") {
                val req = call.receive<RefreshTokenRequest>()
            
            // Validate refresh token
            Validator.validate {
                field("refreshToken", req.refreshToken) {
                    required()
                    minLength(20)  // JWT tokens are long
                }
            }
            
                val result = authService.refreshToken(req)
                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.Unauthorized,
                    result
                )
            }
        }

        /**
         * Resend OTP
         * POST /auth/resend-otp
         */
        rateLimit(RateLimitName("auth-limiter")) {
            post("/resend-otp") {
                val req = call.receive<ResendOtpRequest>()
            
            // Validate resend OTP request
            Validator.validate {
                field("userId", req.userId) {
                    required()
                    uuid()
                }
                field("channel", req.channel) {
                    oneOf("SMS", "EMAIL", "WHATSAPP")
                }
            }
            
                val result = authService.resendOtp(req)
                call.respond(
                    if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                    result
                )
            }

            post("/forgot-password") {
                val req = call.receive<ResetPasswordRequestDTO>()
                Validator.validate {
                    field("email", req.email) { required(); email() }
                }
                call.respond(HttpStatusCode.OK, authService.requestPasswordReset(req.email))
            }

            post("/reset-password") {
                val req = call.receive<ResetPasswordConfirmDTO>()
                Validator.validate {
                    field("token", req.token) {
                        required()
                        custom("Reset code must be 6 digits", "INVALID_RESET_CODE") {
                            it is String && it.matches(Regex("^\\d{6}\$"))
                        }
                    }
                    field("newPassword", req.newPassword) { required(); password() }
                }
                val result = authService.confirmPasswordReset(req.token, req.newPassword)
                call.respond(if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest, result)
            }
        }
    }
}

/**
 * Authenticated account management routes (require JWT).
 * Must be placed inside authenticate("jwt-auth") block.
 */
fun Route.accountRoutesValidated() {
    val authService: AuthService by inject()

    route("/auth") {
        /**
         * Enable/disable OTP 2FA
         * POST /auth/set-otp
         */
        post("/set-otp") {
            val callerUserId = call.principal<JWTPrincipal>()?.payload?.subject
                ?: throw UnauthorizedException("Authentication required")
            
            val req = call.receive<EnableOtpRequest>()
            
            if (req.userId != callerUserId && !call.hasRole("SUPERADMIN")) {
                throw UnauthorizedException("Cannot modify OTP for another user")
            }
            
            // Validate OTP settings request
            Validator.validate {
                field("enable", req.enable) {
                    required()
                }
            }
            
            val result = authService.setOtpEnabled(req)
            call.respond(
                if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                result
            )
        }

        /**
         * Change password
         * POST /auth/change-password
         */
        post("/change-password") {
            val callerUserId = call.principal<JWTPrincipal>()?.payload?.subject
                ?: throw UnauthorizedException("Authentication required")
            
            val req = call.receive<com.app.biashara.services.ChangePasswordRequest>()
            
            // Validate password change request
            Validator.validate {
                field("currentPassword", req.currentPassword) {
                    required()
                    minLength(6)
                }
                field("newPassword", req.newPassword) {
                    required()
                    password()
                    custom("New password must be different from current password", "SAME_PASSWORD") {
                        it as String != req.currentPassword
                    }
                }
            }
            
            val result = authService.changePassword(callerUserId, req)
            call.respond(
                if (result.success) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                result
            )
        }
    }
}
