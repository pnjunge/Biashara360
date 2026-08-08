package com.app.biashara.di

import com.app.biashara.cache.RateLimitStore
import com.app.biashara.cache.RedisRateLimitStore
import com.app.biashara.cache.CacheStore
import com.app.biashara.services.*
import com.app.biashara.services.CyberSourceConfig
import com.app.biashara.services.CyberSourceService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin(appConfig: ApplicationConfig) {
    install(Koin) {
        slf4jLogger()
        modules(appModule(appConfig))
    }
}

fun appModule(config: ApplicationConfig) = module {
    single { RedisRateLimitStore(config) }
    single<RateLimitStore> { get<RedisRateLimitStore>() }
    single<CacheStore> { get<RedisRateLimitStore>() }

    // HTTP client for outbound calls (Mpesa Daraja API)
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    // SMS & Email services for OTP delivery
    single { SmsService(config, get()) }
    single { EmailService(config) }
    single { WhatsAppOtpService(config, get()) }

    // Services
    single { AuthService(get(), get(), get()) }
    single { ProductService() }
    single { InventoryCategoryService() }
    single { OrderService() }
    single { CustomerService() }
    single { ExpenseService() }
    single { PaymentService() }
    single { BusinessSettingsService() }
    single { SystemSettingsService() }
    single { MpesaService(get(), config, get(), get()) }
    single { UserManagementService(get(), get()) }
    single { AuditLogService() }
    single { SuperAdminService() }
    single { BusinessProfileService() }
    single { DashboardService(get(), get(), get()) }
    single { StorefrontService(get(), get()) }
    single { AccessControlService() }
    single { HospitalityService(get()) }
    single { AdvancedHospitalityService() }
    single { ReportService() }

    // CyberSource card payment services
    single {
        CyberSourceConfig(
            merchantId        = config.propertyOrNull("cybersource.merchantId")?.getString() ?: "",
            merchantKeyId     = config.propertyOrNull("cybersource.merchantKeyId")?.getString() ?: "",
            merchantSecretKey = config.propertyOrNull("cybersource.merchantSecretKey")?.getString() ?: "",
            environment       = config.propertyOrNull("cybersource.environment")?.getString() ?: "sandbox"
        )
    }
    single { CyberSourceService(get(), get()) }
    single { CyberSourcePaymentService(get(), get()) }
    single { SecureAcceptanceService(get()) }
    single { TaxService() }
    single { KraService() }
    single { EtimsService(get()) }
    single { SocialService(get(), get(), get(), get(), config) }
}
