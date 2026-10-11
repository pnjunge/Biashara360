package com.app.biashara.services

import com.app.biashara.db.ProductsTable
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable data class StorefrontCatalogItem(
    val id: String, val name: String, val sku: String, val category: String,
    val onlineVisible: Boolean, val onlineGroup: String, val isActive: Boolean
)
@Serializable data class StorefrontCatalogUpdate(val onlineVisible: Boolean, val onlineGroup: String = "")

/** Portal presentation does not change inventory status or POS categories. */
class StorefrontCatalogService {
    fun list(businessId: String): List<StorefrontCatalogItem> = transaction {
        ProductsTable.select { ProductsTable.businessId eq businessId }.orderBy(ProductsTable.name).map { item(it) }
    }
    fun update(businessId: String, id: String, request: StorefrontCatalogUpdate): StorefrontCatalogItem = transaction {
        val group = request.onlineGroup.trim()
        require(group.length <= 100 && group.none { it.isISOControl() }) { "Group must be at most 100 characters" }
        val updated = ProductsTable.update({ (ProductsTable.businessId eq businessId) and (ProductsTable.id eq id) }) {
            it[onlineVisible] = request.onlineVisible
            it[onlineGroup] = group
            it[updatedAt] = Clock.System.now()
        }
        require(updated == 1) { "Product not found" }
        item(ProductsTable.select { (ProductsTable.businessId eq businessId) and (ProductsTable.id eq id) }.single())
    }
    private fun item(row: ResultRow) = StorefrontCatalogItem(row[ProductsTable.id], row[ProductsTable.name], row[ProductsTable.sku],
        row[ProductsTable.category], row[ProductsTable.onlineVisible], row[ProductsTable.onlineGroup], row[ProductsTable.isActive])
}
