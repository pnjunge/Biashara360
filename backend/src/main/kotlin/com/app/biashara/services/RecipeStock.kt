package com.app.biashara.services

import com.app.biashara.db.*
import org.jetbrains.exposed.sql.*
import kotlin.math.floor

internal data class RecipeStock(val lines: Map<String, Double>, val available: Int, val cost: Double)

// Used by all sales channels and their catalogues; recipe items never own a second stock balance.
internal fun recipeStock(businessId: String, productIds: List<String>): Map<String, RecipeStock> {
    if (productIds.isEmpty()) return emptyMap()
    val recipes = ProductRecipesTable.select { ProductRecipesTable.productId inList productIds }.toList()
    if (recipes.isEmpty()) return emptyMap()
    val ingredients = InventoryIngredientsTable.select {
        (InventoryIngredientsTable.businessId eq businessId) and
            (InventoryIngredientsTable.id inList recipes.map { it[ProductRecipesTable.ingredientId] }.distinct()) and
            (InventoryIngredientsTable.isActive eq true)
    }.associateBy { it[InventoryIngredientsTable.id] }
    return recipes.groupBy { it[ProductRecipesTable.productId] }.mapValues { (_, lines) ->
        val quantities = lines.associate { it[ProductRecipesTable.ingredientId] to it[ProductRecipesTable.quantity] }
        val available = quantities.minOf { (id, needed) ->
            val stock = ingredients[id]?.get(InventoryIngredientsTable.quantity) ?: 0.0
            if (!needed.isFinite() || needed <= 0 || !stock.isFinite()) 0
            else floor(stock / needed + 1e-9).coerceIn(0.0, Int.MAX_VALUE.toDouble()).toInt()
        }
        val cost = quantities.entries.sumOf { (id, needed) -> needed * (ingredients[id]?.get(InventoryIngredientsTable.unitCost) ?: 0.0) }
        RecipeStock(quantities, available, cost)
    }
}

internal fun purchaseConversion(stockUnit: String, purchaseUnit: String, configuredUnit: String?, configuredSize: Double): Double {
    val base = stockUnit.trim().uppercase()
    val unit = purchaseUnit.trim().uppercase()
    if (unit == base) return 1.0
    val units = mapOf("G" to ("MASS" to 1.0), "KG" to ("MASS" to 1000.0), "ML" to ("VOLUME" to 1.0), "L" to ("VOLUME" to 1000.0))
    val from = units[unit]; val to = units[base]
    if (from != null && to != null && from.first == to.first) return from.second / to.second
    require(unit in setOf("BOTTLE", "PACK", "CASE") && unit == configuredUnit?.trim()?.uppercase()) { "Purchase unit is incompatible with the ingredient stock unit; configure its pack size first" }
    require(configuredSize.isFinite() && configuredSize > 0) { "Pack size must be positive" }
    return configuredSize
}
