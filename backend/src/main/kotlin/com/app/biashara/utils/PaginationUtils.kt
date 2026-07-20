package com.app.biashara.utils

import com.app.biashara.constants.Constants
import com.app.biashara.models.PagedResponse
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder

/**
 * Pagination utilities for consistent pagination across all list endpoints.
 */

data class PaginationParams(
    val page: Int,
    val pageSize: Int,
    val offset: Long,
    val limit: Int
) {
    companion object {
        fun from(page: Int?, pageSize: Int?): PaginationParams {
            val validPage = (page ?: 1).coerceAtLeast(1)
            val validPageSize = (pageSize ?: Constants.Business.DEFAULT_PAGE_SIZE)
                .coerceIn(1, Constants.Business.MAX_PAGE_SIZE)
            val offset = ((validPage - 1) * validPageSize).toLong()
            
            return PaginationParams(
                page = validPage,
                pageSize = validPageSize,
                offset = offset,
                limit = validPageSize
            )
        }
    }
}

/**
 * Extension function to paginate Exposed queries.
 */
fun Query.paginate(params: PaginationParams): Query {
    return this.limit(params.limit, params.offset)
}

/**
 * Extension function to create PagedResponse from query results.
 */
fun <T> Query.toPagedResponse(
    params: PaginationParams,
    mapper: (ResultRow) -> T
): PagedResponse<T> {
    val total = this.count().toInt()
    val data = this
        .limit(params.limit, params.offset)
        .map(mapper)
    
    return PagedResponse(
        data = data,
        total = total,
        page = params.page,
        pageSize = params.pageSize,
        hasMore = (params.page * params.pageSize) < total
    )
}

/**
 * Create PagedResponse from list with in-memory pagination.
 * Use only when database pagination is not feasible.
 */
fun <T> List<T>.toPagedResponse(
    page: Int = 1,
    pageSize: Int = Constants.Business.DEFAULT_PAGE_SIZE
): PagedResponse<T> {
    val params = PaginationParams.from(page, pageSize)
    val total = this.size
    val startIndex = params.offset.toInt().coerceIn(0, total)
    val endIndex = (startIndex + params.pageSize).coerceIn(0, total)
    val data = this.subList(startIndex, endIndex)
    
    return PagedResponse(
        data = data,
        total = total,
        page = params.page,
        pageSize = params.pageSize,
        hasMore = endIndex < total
    )
}

/**
 * Pagination metadata for API responses.
 */
data class PaginationMeta(
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun from(page: Int, pageSize: Int, total: Int): PaginationMeta {
            val totalPages = if (pageSize > 0) (total + pageSize - 1) / pageSize else 0
            return PaginationMeta(
                page = page,
                pageSize = pageSize,
                total = total,
                totalPages = totalPages,
                hasNext = page < totalPages,
                hasPrevious = page > 1
            )
        }
    }
}

/**
 * Enhanced paged response with metadata.
 */
data class PagedResponseWithMeta<T>(
    val data: List<T>,
    val meta: PaginationMeta
)

/**
 * Sort parameter parser.
 */
data class SortParams(
    val field: String,
    val order: SortOrder
) {
    companion object {
        fun from(sortBy: String?, sortOrder: String?): SortParams {
            val order = when (sortOrder?.lowercase()) {
                "desc" -> SortOrder.DESC
                else -> SortOrder.ASC
            }
            return SortParams(sortBy ?: "createdAt", order)
        }
    }
}
