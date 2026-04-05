package com.app.biashara.di

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

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule(environment.config))
    }
}

fun appModule(config: ApplicationConfig) = module {
    // HTTP client for outbound calls (Mpesa Daraja API)
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    // Services
    single { AuthService() }
    single { ProductService() }
    single { OrderService() }
    single { CustomerService() }
    single { ExpenseService() }
    single { PaymentService() }
    single { MpesaService(get(), config) }

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
    single { CyberSourcePaymentService(get()) }
    single { TaxService() }
    single { KraService() }
    single { EtimsService(get()) }
    single { SocialService(get(), get(), get(), get()) }
}
