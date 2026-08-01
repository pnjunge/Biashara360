package com.app.biashara.services

import com.app.biashara.db.BusinessesTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select

internal fun storefrontSlugBase(name: String): String = name
    .trim()
    .lowercase()
    .replace(Regex("['’]"), "")
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
    .take(100)
    .ifBlank { "business" }

internal fun allocateStorefrontSlug(name: String, businessId: String): String {
    val base = storefrontSlugBase(name)
    val exists = BusinessesTable.select { BusinessesTable.storefrontSlug eq base }.any()
    return if (exists) "$base-${businessId.replace("-", "").take(8)}" else base
}
