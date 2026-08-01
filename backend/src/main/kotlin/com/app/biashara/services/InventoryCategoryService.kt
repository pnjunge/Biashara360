package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.InventoryCategoriesTable
import com.app.biashara.db.ProductsTable
import com.app.biashara.models.ApiResponse
import com.app.biashara.models.InventoryCategoryResponse
import com.app.biashara.models.UpdateInventoryCategoryRequest
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class InventoryCategoryService {
    private val defaults = listOf("Electronics", "Clothing", "Food & Beverage", "Health & Beauty", "Home & Garden", "Stationery", "Other")

    fun getAll(businessId: String): List<InventoryCategoryResponse> = transaction {
        ensureInitialCategories(businessId)
        InventoryCategoriesTable.select { InventoryCategoriesTable.businessId eq businessId }
            .orderBy(InventoryCategoriesTable.isActive to SortOrder.DESC, InventoryCategoriesTable.name to SortOrder.ASC)
            .map { row -> row.toResponse(businessId) }
    }

    fun create(businessId: String, rawName: String, rawImageUrl: String? = null): ApiResponse<InventoryCategoryResponse> = transaction {
        val name = normalizeName(rawName)
        val imageUrl = validateImageUrl(rawImageUrl) ?: if (rawImageUrl.isNullOrBlank()) null else return@transaction ApiResponse(false, message = "Category image must use an HTTP(S) URL")
        if (name.length !in 2..100) return@transaction ApiResponse(false, message = "Category name must be between 2 and 100 characters")
        if (findByName(businessId, name) != null) return@transaction ApiResponse(false, message = "Category already exists")
        val id = generateId()
        val now = Clock.System.now()
        InventoryCategoriesTable.insert {
            it[InventoryCategoriesTable.id] = id
            it[InventoryCategoriesTable.businessId] = businessId
            it[InventoryCategoriesTable.name] = name
            it[InventoryCategoriesTable.imageUrl] = imageUrl
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }
        val row = InventoryCategoriesTable.select { InventoryCategoriesTable.id eq id }.first()
        ApiResponse(true, row.toResponse(businessId), "Category created")
    }

    fun update(id: String, businessId: String, req: UpdateInventoryCategoryRequest): ApiResponse<InventoryCategoryResponse> = transaction {
        val current = InventoryCategoriesTable.select {
            (InventoryCategoriesTable.id eq id) and (InventoryCategoriesTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Category not found")
        val oldName = current[InventoryCategoriesTable.name]
        val newName = req.name?.let(::normalizeName) ?: oldName
        val imageUrl = when {
            req.imageUrl == null || req.imageUrl.isBlank() -> null
            else -> validateImageUrl(req.imageUrl) ?: return@transaction ApiResponse(false, message = "Category image must use an HTTP(S) URL")
        }
        if (newName.length !in 2..100) return@transaction ApiResponse(false, message = "Category name must be between 2 and 100 characters")
        val duplicate = findByName(businessId, newName)
        if (duplicate != null && duplicate[InventoryCategoriesTable.id] != id) {
            return@transaction ApiResponse(false, message = "Category already exists")
        }
        val now = Clock.System.now()
        InventoryCategoriesTable.update({ (InventoryCategoriesTable.id eq id) and (InventoryCategoriesTable.businessId eq businessId) }) {
            it[name] = newName
            req.isActive?.let { active -> it[isActive] = active }
            if (req.imageUrl != null) it[InventoryCategoriesTable.imageUrl] = imageUrl
            it[updatedAt] = now
        }
        if (newName != oldName) {
            ProductsTable.update({
                (ProductsTable.businessId eq businessId) and (ProductsTable.category.lowerCase() eq oldName.lowercase())
            }) {
                it[category] = newName
                it[updatedAt] = now
            }
        }
        val updated = InventoryCategoriesTable.select { InventoryCategoriesTable.id eq id }.first()
        ApiResponse(true, updated.toResponse(businessId), "Category updated")
    }

    private fun ensureInitialCategories(businessId: String) {
        if (InventoryCategoriesTable.select { InventoryCategoriesTable.businessId eq businessId }.any()) return
        val existing = ProductsTable.slice(ProductsTable.category)
            .select { (ProductsTable.businessId eq businessId) and (ProductsTable.category neq "") }
            .withDistinct().map { normalizeName(it[ProductsTable.category]) }.filter { it.isNotBlank() }
        val names = (existing.ifEmpty { defaults }).distinctBy { it.lowercase() }
        val now = Clock.System.now()
        names.forEach { categoryName ->
            InventoryCategoriesTable.insert {
                it[id] = generateId(); it[InventoryCategoriesTable.businessId] = businessId
                it[name] = categoryName; it[isActive] = true; it[createdAt] = now; it[updatedAt] = now
            }
        }
    }

    private fun findByName(businessId: String, name: String) = InventoryCategoriesTable.select {
        (InventoryCategoriesTable.businessId eq businessId) and (InventoryCategoriesTable.name.lowerCase() eq name.lowercase())
    }.firstOrNull()

    private fun ResultRow.toResponse(businessId: String): InventoryCategoryResponse {
        val categoryName = this[InventoryCategoriesTable.name]
        val count = ProductsTable.select {
            (ProductsTable.businessId eq businessId) and (ProductsTable.isActive eq true) and
                (ProductsTable.category.lowerCase() eq categoryName.lowercase())
        }.count().toInt()
        return InventoryCategoryResponse(this[InventoryCategoriesTable.id], categoryName, this[InventoryCategoriesTable.isActive], count, this[InventoryCategoriesTable.imageUrl])
    }

    companion object {
        fun normalizeName(value: String): String = value.trim().replace(Regex("\\s+"), " ")
        private fun validateImageUrl(value: String?): String? {
            val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return normalized.takeIf { it.length <= 500 && (it.startsWith("https://") || it.startsWith("http://")) }
        }
    }
}
