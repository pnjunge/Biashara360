package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.AuditLogsTable
import com.app.biashara.db.UsersTable
import com.app.biashara.models.AuditLogResponse
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class AuditLogService {

    fun logEvent(
        businessId: String?,
        actorUserId: String?,
        targetUserId: String?,
        action: String,
        ipAddress: String? = null,
        details: String? = null
    ) {
        runCatching {
            transaction {
                AuditLogsTable.insert { row ->
                    row[id] = generateId()
                    row[AuditLogsTable.businessId] = businessId
                    row[AuditLogsTable.actorUserId] = actorUserId
                    row[AuditLogsTable.targetUserId] = targetUserId
                    row[AuditLogsTable.action] = action.trim().uppercase()
                    row[AuditLogsTable.ipAddress] = ipAddress?.take(45)
                    row[AuditLogsTable.details] = details?.take(1000)
                    row[createdAt] = Clock.System.now()
                }
            }
        }
    }

    fun listAuditLogs(businessId: String, limit: Int = 100): List<AuditLogResponse> = transaction {
        val userNames = UsersTable.slice(UsersTable.id, UsersTable.name)
            .select { UsersTable.businessId eq businessId }
            .associate { it[UsersTable.id] to it[UsersTable.name] }

        AuditLogsTable.select { AuditLogsTable.businessId eq businessId }
            .orderBy(AuditLogsTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { row ->
                val actorId = row[AuditLogsTable.actorUserId]
                val targetId = row[AuditLogsTable.targetUserId]
                AuditLogResponse(
                    id = row[AuditLogsTable.id],
                    businessId = row[AuditLogsTable.businessId],
                    actorUserId = actorId,
                    actorName = actorId?.let { userNames[it] },
                    targetUserId = targetId,
                    targetName = targetId?.let { userNames[it] },
                    action = row[AuditLogsTable.action],
                    ipAddress = row[AuditLogsTable.ipAddress],
                    details = row[AuditLogsTable.details],
                    createdAt = row[AuditLogsTable.createdAt].toString()
                )
            }
    }
}
