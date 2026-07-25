package com.app.biashara.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: ApplicationConfig) {
        // Use optional properties with defaults - they'll work if config has them
        val dbUrl = try {
            config.property("database.url").getString()
        } catch (e: Exception) {
            System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/biashara360"
        }
        
        val dbUser = try {
            config.property("database.user").getString()
        } catch (e: Exception) {
            System.getenv("DB_USER") ?: "biashara360"
        }
        
        val dbPassword = try {
            config.property("database.password").getString().takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
            ?: System.getenv("DB_PASSWORD")?.takeIf { it.isNotBlank() }
            ?: error("[DatabaseFactory] DB_PASSWORD is required but not set. Set the environment variable before starting.")
        
        val maxPoolSize = try {
            config.property("database.maxPoolSize").getString().toInt()
        } catch (e: Exception) {
            10
        }

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbUrl
            username = dbUser
            password = dbPassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = maxPoolSize
            minimumIdle = 2
            idleTimeout = 300_000
            connectionTimeout = 15_000  // 15s — avoids long Netty thread stalls on DB outage
            maxLifetime = 1_800_000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            initializationFailTimeout = 60_000
            validate()
        }

        Database.connect(HikariDataSource(hikariConfig))
        createTables()
        // Note: seedSuperuser() is called by Application.module() after init()
    }

    private fun createTables() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                BusinessesTable,
                UsersTable,
                OtpTable,
                RefreshTokensTable,
                ProductsTable,
                StockMovementsTable,
                CustomersTable,
                OrdersTable,
                OrderItemsTable,
                ExpensesTable,
                PaymentsTable,
                CyberSourceTransactionsTable,
                CsCustomerTokensTable,
                TaxRatesTable,
                OrderTaxLinesTable,
                TaxRemittancesTable,
                KraProfilesTable,
                EtimsInvoicesTable,
                TaxReturnsTable,
                SocialChannelsTable,
                SocialConversationsTable,
                SocialMessagesTable,
                SocialOrdersTable,
                MpesaConfigsTable,
                CyberSourceConfigsTable,
                BusinessSessionSettingsTable,
                SystemSettingsTable
            )
            // WhatsApp uses the single platform system-user token injected from
            // the deployment vault. Remove any legacy merchant tokens from DB.
            SocialChannelsTable.update({ SocialChannelsTable.platform eq "WHATSAPP" }) {
                it[SocialChannelsTable.accessToken] = ""
                it[SocialChannelsTable.refreshToken] = null
            }
        }
    }
}
