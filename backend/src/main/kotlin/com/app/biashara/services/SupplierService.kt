package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.SuppliersTable
import com.app.biashara.models.ApiResponse
import com.app.biashara.models.SupplierRequest
import com.app.biashara.models.SupplierResponse
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class SupplierService {

    fun list(businessId: String): List<SupplierResponse> = transaction {
        SuppliersTable.select { (SuppliersTable.businessId eq businessId) and (SuppliersTable.isActive eq true) }
            .orderBy(SuppliersTable.name, SortOrder.ASC)
            .map {
                SupplierResponse(
                    id = it[SuppliersTable.id],
                    name = it[SuppliersTable.name],
                    phone = it[SuppliersTable.phone],
                    email = it[SuppliersTable.email],
                    address = it[SuppliersTable.address],
                    isActive = it[SuppliersTable.isActive]
                )
            }
    }

    fun create(businessId: String, req: SupplierRequest): ApiResponse<SupplierResponse> = transaction {
        val name = req.name.trim()
        if (name.isBlank()) {
            return@transaction ApiResponse(false, message = "Supplier name is required")
        }

        val existing = SuppliersTable.select {
            (SuppliersTable.businessId eq businessId) and (SuppliersTable.name.lowerCase() eq name.lowercase())
        }.firstOrNull()

        if (existing != null) {
            // If exists but inactive, reactivate it
            if (!existing[SuppliersTable.isActive]) {
                SuppliersTable.update({ SuppliersTable.id eq existing[SuppliersTable.id] }) {
                    it[isActive] = true
                    it[phone] = req.phone.trim().ifBlank { existing[SuppliersTable.phone] }
                    it[email] = req.email?.trim() ?: existing[SuppliersTable.email]
                    it[address] = req.address?.trim() ?: existing[SuppliersTable.address]
                    it[updatedAt] = Clock.System.now()
                }
            }
            return@transaction ApiResponse(
                true,
                data = SupplierResponse(
                    id = existing[SuppliersTable.id],
                    name = existing[SuppliersTable.name],
                    phone = req.phone.trim().ifBlank { existing[SuppliersTable.phone] },
                    email = req.email?.trim() ?: existing[SuppliersTable.email],
                    address = req.address?.trim() ?: existing[SuppliersTable.address],
                    isActive = true
                ),
                message = "Supplier already exists"
            )
        }

        val id = generateId()
        val now = Clock.System.now()
        SuppliersTable.insert {
            it[SuppliersTable.id] = id
            it[SuppliersTable.businessId] = businessId
            it[SuppliersTable.name] = name
            it[phone] = req.phone.trim()
            it[email] = req.email?.trim()?.takeIf { e -> e.isNotBlank() }
            it[address] = req.address?.trim()?.takeIf { a -> a.isNotBlank() }
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }

        val created = SuppliersTable.select { SuppliersTable.id eq id }.first()
        ApiResponse(
            true,
            data = SupplierResponse(
                id = created[SuppliersTable.id],
                name = created[SuppliersTable.name],
                phone = created[SuppliersTable.phone],
                email = created[SuppliersTable.email],
                address = created[SuppliersTable.address],
                isActive = created[SuppliersTable.isActive]
            ),
            message = "Supplier created successfully"
        )
    }

    fun update(businessId: String, id: String, req: SupplierRequest): ApiResponse<SupplierResponse> = transaction {
        val name = req.name.trim()
        if (name.isBlank()) {
            return@transaction ApiResponse(false, message = "Supplier name is required")
        }

        val updated = SuppliersTable.update({ (SuppliersTable.id eq id) and (SuppliersTable.businessId eq businessId) }) {
            it[SuppliersTable.name] = name
            it[phone] = req.phone.trim()
            it[email] = req.email?.trim()?.takeIf { e -> e.isNotBlank() }
            it[address] = req.address?.trim()?.takeIf { a -> a.isNotBlank() }
            it[updatedAt] = Clock.System.now()
        }

        if (updated == 0) return@transaction ApiResponse(false, message = "Supplier not found")

        val row = SuppliersTable.select { SuppliersTable.id eq id }.first()
        ApiResponse(
            true,
            data = SupplierResponse(
                id = row[SuppliersTable.id],
                name = row[SuppliersTable.name],
                phone = row[SuppliersTable.phone],
                email = row[SuppliersTable.email],
                address = row[SuppliersTable.address],
                isActive = row[SuppliersTable.isActive]
            ),
            message = "Supplier updated successfully"
        )
    }

    fun delete(businessId: String, id: String): ApiResponse<Boolean> = transaction {
        val count = SuppliersTable.update({ (SuppliersTable.id eq id) and (SuppliersTable.businessId eq businessId) }) {
            it[isActive] = false
            it[updatedAt] = Clock.System.now()
        }
        ApiResponse(count > 0, data = count > 0, message = if (count > 0) "Supplier deactivated" else "Supplier not found")
    }
}
