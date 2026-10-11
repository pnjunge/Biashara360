package com.app.biashara.services

import com.app.biashara.db.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

object WorkstationAlertsTable : Table("workstation_alerts") {
    val id = varchar("id", 36)
    val businessId = varchar("business_id", 36).references(BusinessesTable.id)
    val name = varchar("name", 100)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val revoked = bool("revoked").default(false)
    override val primaryKey = PrimaryKey(id)
}
@Serializable data class WorkstationLinkRequest(val name: String)
@Serializable data class WorkstationLink(val id: String, val name: String, val merchantName: String, val token: String, val expiresAt: String)
@Serializable data class WorkstationRegistration(val id: String, val name: String, val expiresAt: String, val revoked: Boolean)
@Serializable data class WorkstationOrderAlert(val id: String, val orderNumber: String, val createdAt: String)
@Serializable data class WorkstationAlertFeed(val merchantName: String, val orders: List<WorkstationOrderAlert>)

class WorkstationAlertService {
    private fun hash(token: String) = MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    fun isAdministrator(businessId: String, userId: String): Boolean = transaction {
        val user = UsersTable.slice(UsersTable.businessId, UsersTable.role, UsersTable.isActive).select { UsersTable.id eq userId }.firstOrNull() ?: return@transaction false
        user[UsersTable.isActive] && (user[UsersTable.role] == "SUPERADMIN" || (user[UsersTable.businessId] == businessId && user[UsersTable.role] in setOf("ADMIN", "BUSINESS_ADMIN")))
    }
    fun link(businessId: String, name: String): WorkstationLink = transaction {
        require(name.trim().length in 1..100) { "Enter a workstation name (up to 100 characters)" }
        val merchant = BusinessesTable.slice(BusinessesTable.name).select { BusinessesTable.id eq businessId }.single()[BusinessesTable.name]
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
        val id = UUID.randomUUID().toString()
        val expiry = Clock.System.now() + kotlin.time.Duration.parse("365d")
        WorkstationAlertsTable.insert { it[this.id]=id; it[this.businessId]=businessId; it[this.name]=name.trim(); it[tokenHash]=hash(token); it[expiresAt]=expiry }
        WorkstationLink(id, name.trim(), merchant, token, expiry.toString())
    }
    fun list(businessId: String): List<WorkstationRegistration> = transaction {
        WorkstationAlertsTable.select { WorkstationAlertsTable.businessId eq businessId }.map {
            WorkstationRegistration(it[WorkstationAlertsTable.id], it[WorkstationAlertsTable.name], it[WorkstationAlertsTable.expiresAt].toString(), it[WorkstationAlertsTable.revoked])
        }
    }
    fun revoke(businessId: String, id: String): Boolean = transaction {
        WorkstationAlertsTable.update({ (WorkstationAlertsTable.id eq id) and (WorkstationAlertsTable.businessId eq businessId) }) { it[revoked]=true } == 1
    }
    fun feed(token: String): WorkstationAlertFeed? = transaction {
        if (token.length != 43) return@transaction null
        val device = WorkstationAlertsTable.select { (WorkstationAlertsTable.tokenHash eq hash(token)) and (WorkstationAlertsTable.revoked eq false) and (WorkstationAlertsTable.expiresAt greater Clock.System.now()) }.firstOrNull() ?: return@transaction null
        val merchantId = device[WorkstationAlertsTable.businessId]
        val merchant = BusinessesTable.slice(BusinessesTable.name).select { BusinessesTable.id eq merchantId }.single()[BusinessesTable.name]
        val orders = OrdersTable.slice(OrdersTable.id, OrdersTable.orderNumber, OrdersTable.createdAt).select {
            (OrdersTable.businessId eq merchantId) and (OrdersTable.salesChannel eq "ECOMMERCE") and OrdersTable.serverUserId.isNull() and OrdersTable.responsibleUserId.isNull() and
                (OrdersTable.paymentStatus notInList listOf("CANCELLED", "VOIDED", "REFUNDED")) and (OrdersTable.deliveryStatus notInList listOf("CANCELLED", "DELIVERED", "RETURNED"))
        }.orderBy(OrdersTable.createdAt, SortOrder.DESC).limit(100).map { WorkstationOrderAlert(it[OrdersTable.id],it[OrdersTable.orderNumber],it[OrdersTable.createdAt].toString()) }
        WorkstationAlertFeed(merchant, orders)
    }
}
