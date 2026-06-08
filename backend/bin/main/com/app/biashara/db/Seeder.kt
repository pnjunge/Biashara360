package com.app.biashara.db

import com.app.biashara.auth.PasswordUtils
import com.app.biashara.auth.generateId
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Seeds the database with the initial system superuser account.
 *
 * This function is idempotent — it checks whether the superuser already exists
 * (by email) before inserting, so it is safe to call on every application start.
 */
fun seedSuperuser() {
    val superuserEmail = "admin@biashara360.co.ke"
    val superuserPhone = "+254700000000"

    transaction {
        // Guard: skip if the superuser already exists
        val exists = UsersTable
            .select { UsersTable.email eq superuserEmail }
            .count() > 0

        if (exists) {
            println("[Seeder] Superuser already exists — skipping seed.")
            return@transaction
        }

        val now = Clock.System.now()
        val userId = generateId()

        // Create the superuser account without a business (SUPERADMIN is system-level)
        UsersTable.insert {
            it[id]               = userId
            it[businessId]       = null
            it[name]             = "System Administrator"
            it[email]            = superuserEmail
            it[phone]            = superuserPhone
            it[passwordHash]     = PasswordUtils.hash("admin123")
            it[role]             = "SUPERADMIN"
            it[twoFactorEnabled] = false
            it[preferredLanguage] = "ENGLISH"
            it[isActive]         = true
            it[createdAt]        = now
            it[updatedAt]        = now
        }

        println("[Seeder] Superuser seeded successfully (id=$userId, email=$superuserEmail).")
    }
}
