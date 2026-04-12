package com.app.biashara.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: ApplicationConfig) {
        // Use optional properties with defaults - they'll work if config has them
        val dbUrl = try {
            config.property("database.url").getString()
        } catch (e: Exception) {
            System.getenv("DATABASE_URL") ?: "jdbc:postgresql://postgres:5432/biashara360"
        }
        
        val dbUser = try {
            config.property("database.user").getString()
        } catch (e: Exception) {
            System.getenv("DB_USER") ?: "biashara360"
        }
        
        val dbPassword = try {
            config.property("database.password").getString()
        } catch (e: Exception) {
            System.getenv("DB_PASSWORD") ?: "password"
        }
        
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
            connectionTimeout = 60_000
            maxLifetime = 1_800_000
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            initializationFailTimeout = 60_000
            validate()
        }

        Database.connect(HikariDataSource(hikariConfig))
        createTables()
        seedSuperuser()
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
                CyberSourceConfigsTable
            )
        }
    }
}
