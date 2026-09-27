package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.AuditLogsTable
import com.app.biashara.db.UsersTable
import com.app.biashara.models.AuditLogResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class AuditLogService {

    fun logEvent(
        businessId: String?,
        actorUserId: String?,
        targetUserId: String? = null,
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

    fun listAuditLogs(
        businessId: String,
        limit: Int = 200,
        action: String? = null,
        search: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): List<AuditLogResponse> = transaction {
        val userNames = UsersTable.slice(UsersTable.id, UsersTable.name)
            .select { UsersTable.businessId eq businessId }
            .associate { it[UsersTable.id] to it[UsersTable.name] }

        var query = AuditLogsTable.select { AuditLogsTable.businessId eq businessId }

        if (!action.isNullOrBlank() && action.trim().uppercase() != "ALL") {
            val act = action.trim().uppercase()
            query = query.andWhere { AuditLogsTable.action eq act }
        }

        if (!startDate.isNullOrBlank()) {
            runCatching { Instant.parse(startDate) }.getOrNull()?.let { startInstant ->
                query = query.andWhere { AuditLogsTable.createdAt greaterEq startInstant }
            }
        }

        if (!endDate.isNullOrBlank()) {
            runCatching { Instant.parse(endDate) }.getOrNull()?.let { endInstant ->
                query = query.andWhere { AuditLogsTable.createdAt lessEq endInstant }
            }
        }

        val rows = query
            .orderBy(AuditLogsTable.createdAt, SortOrder.DESC)
            .limit(limit.coerceIn(1, 1000))
            .toList()

        val searchTerms = search?.trim()?.lowercase()?.split(" ")?.filter { it.isNotEmpty() }.orEmpty()

        rows.mapNotNull { row ->
            val actorId = row[AuditLogsTable.actorUserId]
            val targetId = row[AuditLogsTable.targetUserId]
            val actorName = actorId?.let { userNames[it] }
            val targetName = targetId?.let { userNames[it] }
            val actionText = row[AuditLogsTable.action]
            val detailsText = row[AuditLogsTable.details].orEmpty()
            val ipText = row[AuditLogsTable.ipAddress].orEmpty()

            if (searchTerms.isNotEmpty()) {
                val combined = "$actionText $actorName $targetName $detailsText $ipText".lowercase()
                if (!searchTerms.all { term -> combined.contains(term) }) {
                    return@mapNotNull null
                }
            }

            AuditLogResponse(
                id = row[AuditLogsTable.id],
                businessId = row[AuditLogsTable.businessId],
                actorUserId = actorId,
                actorName = actorName,
                targetUserId = targetId,
                targetName = targetName,
                action = actionText,
                ipAddress = ipText.ifBlank { null },
                details = detailsText.ifBlank { null },
                createdAt = row[AuditLogsTable.createdAt].toString()
            )
        }
    }
}
