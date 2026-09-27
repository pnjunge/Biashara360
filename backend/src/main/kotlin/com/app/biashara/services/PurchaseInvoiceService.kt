package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PurchaseInvoiceService {
    private val json = Json { ignoreUnknownKeys = true }

    fun list(businessId: String, query: String? = null): ApiResponse<List<PurchaseInvoice>> = transaction {
        var q = PurchaseInvoicesTable.select { PurchaseInvoicesTable.businessId eq businessId }
        if (!query.isNullOrBlank()) {
            val term = "%${query.trim().lowercase()}%"
            q = q.andWhere {
                (PurchaseInvoicesTable.invoiceNumber.lowerCase() like term) or
                (PurchaseInvoicesTable.supplierName.lowerCase() like term)
            }
        }
        val invoices = q.orderBy(PurchaseInvoicesTable.createdAt, SortOrder.DESC).map { row ->
            val itemsList: List<PurchaseLineItem> = try {
                json.decodeFromString(row[PurchaseInvoicesTable.itemsJson])
            } catch (_: Exception) {
                emptyList()
            }
            PurchaseInvoice(
                id = row[PurchaseInvoicesTable.id],
                businessId = row[PurchaseInvoicesTable.businessId],
                invoiceNumber = row[PurchaseInvoicesTable.invoiceNumber],
                supplierName = row[PurchaseInvoicesTable.supplierName],
                supplierPhone = row[PurchaseInvoicesTable.supplierPhone],
                invoiceDate = row[PurchaseInvoicesTable.invoiceDate].toString(),
                totalAmount = row[PurchaseInvoicesTable.totalAmount],
                paymentStatus = row[PurchaseInvoicesTable.paymentStatus],
                paymentMethod = row[PurchaseInvoicesTable.paymentMethod],
                notes = row[PurchaseInvoicesTable.notes],
                items = itemsList,
                createdAt = row[PurchaseInvoicesTable.createdAt].toString(),
                updatedAt = row[PurchaseInvoicesTable.updatedAt].toString()
            )
        }
        ApiResponse(true, data = invoices)
    }

    fun getById(id: String, businessId: String): ApiResponse<PurchaseInvoice> = transaction {
        val row = PurchaseInvoicesTable.select {
            (PurchaseInvoicesTable.id eq id) and (PurchaseInvoicesTable.businessId eq businessId)
        }.firstOrNull() ?: return@transaction ApiResponse(false, message = "Purchase invoice not found")

        val itemsList: List<PurchaseLineItem> = try {
            json.decodeFromString(row[PurchaseInvoicesTable.itemsJson])
        } catch (_: Exception) {
            emptyList()
        }
        val invoice = PurchaseInvoice(
            id = row[PurchaseInvoicesTable.id],
            businessId = row[PurchaseInvoicesTable.businessId],
            invoiceNumber = row[PurchaseInvoicesTable.invoiceNumber],
            supplierName = row[PurchaseInvoicesTable.supplierName],
            supplierPhone = row[PurchaseInvoicesTable.supplierPhone],
            invoiceDate = row[PurchaseInvoicesTable.invoiceDate].toString(),
            totalAmount = row[PurchaseInvoicesTable.totalAmount],
            paymentStatus = row[PurchaseInvoicesTable.paymentStatus],
            paymentMethod = row[PurchaseInvoicesTable.paymentMethod],
            notes = row[PurchaseInvoicesTable.notes],
            items = itemsList,
            createdAt = row[PurchaseInvoicesTable.createdAt].toString(),
            updatedAt = row[PurchaseInvoicesTable.updatedAt].toString()
        )
        ApiResponse(true, data = invoice)
    }

    fun create(businessId: String, req: CreatePurchaseInvoiceRequest): ApiResponse<PurchaseInvoice> = transaction {
        val invoiceNum = req.invoiceNumber.trim()
        if (invoiceNum.isBlank()) {
            return@transaction ApiResponse(false, message = "Invoice Number is required")
        }
        if (req.supplierName.trim().isBlank()) {
            return@transaction ApiResponse(false, message = "Supplier Name is required")
        }
        if (req.items.isEmpty()) {
            return@transaction ApiResponse(false, message = "At least one item must be included in the purchase invoice")
        }

        val now = Clock.System.now()
        val parsedDate = try {
            req.invoiceDate?.let { Instant.parse(it) } ?: now
        } catch (_: Exception) {
            now
        }

        val total = req.items.sumOf { it.quantity * it.unitCost }
        val id = UUID.randomUUID().toString()
        val itemsJsonStr = json.encodeToString(req.items)

        // 1. Insert Purchase Invoice record
        PurchaseInvoicesTable.insert {
            it[PurchaseInvoicesTable.id] = id
            it[PurchaseInvoicesTable.businessId] = businessId
            it[PurchaseInvoicesTable.invoiceNumber] = invoiceNum
            it[PurchaseInvoicesTable.supplierName] = req.supplierName.trim()
            it[PurchaseInvoicesTable.supplierPhone] = req.supplierPhone?.trim()?.takeIf { it.isNotBlank() }
            it[PurchaseInvoicesTable.invoiceDate] = parsedDate
            it[PurchaseInvoicesTable.totalAmount] = total
            it[PurchaseInvoicesTable.paymentStatus] = req.paymentStatus?.ifBlank { "PAID" } ?: "PAID"
            it[PurchaseInvoicesTable.paymentMethod] = req.paymentMethod?.ifBlank { "CASH" } ?: "CASH"
            it[PurchaseInvoicesTable.notes] = req.notes?.trim() ?: ""
            it[PurchaseInvoicesTable.itemsJson] = itemsJsonStr
            it[PurchaseInvoicesTable.createdAt] = now
            it[PurchaseInvoicesTable.updatedAt] = now
        }

        // 2. Augment Inventory: increase product current_stock and record STOCK_IN movement
        for (item in req.items) {
            if (item.productId.isNotBlank()) {
                val existing = ProductsTable.select {
                    (ProductsTable.id eq item.productId) and (ProductsTable.businessId eq businessId)
                }.firstOrNull()

                if (existing != null) {
                    val currentStock = existing[ProductsTable.currentStock]
                    val newStock = currentStock + item.quantity
                    ProductsTable.update({ (ProductsTable.id eq item.productId) and (ProductsTable.businessId eq businessId) }) {
                        it[ProductsTable.currentStock] = newStock
                        if (item.unitCost > 0.0) {
                            it[ProductsTable.buyingPrice] = item.unitCost
                        }
                        it[ProductsTable.updatedAt] = now
                    }

                    // Record StockMovement entry
                    StockMovementsTable.insert {
                        it[StockMovementsTable.id] = UUID.randomUUID().toString()
                        it[StockMovementsTable.productId] = item.productId
                        it[StockMovementsTable.businessId] = businessId
                        it[StockMovementsTable.type] = "STOCK_IN"
                        it[StockMovementsTable.quantity] = item.quantity
                        it[StockMovementsTable.note] = "Purchase Invoice #${invoiceNum} from ${req.supplierName.trim()}"
                        it[StockMovementsTable.orderId] = id
                        it[StockMovementsTable.recordedAt] = now
                    }
                }
            }
        }

        // 3. Record stock purchase in ExpensesTable so it appears under Expenses
        try {
            val expDate = try {
                parsedDate.toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
            } catch (_: Exception) {
                Clock.System.now().toLocalDateTime(TimeZone.of("Africa/Nairobi")).date
            }
            ExpensesTable.insert {
                it[ExpensesTable.id] = id
                it[ExpensesTable.businessId] = businessId
                it[category] = "STOCK_PURCHASE"
                it[amount] = total
                it[description] = "Stock Purchase: #${invoiceNum} - ${req.supplierName.trim()}"
                it[expenseDate] = expDate
                it[receiptUrl] = null
                it[recordedAt] = now
            }
        } catch (_: Exception) {
            // Already present or ignore
        }

        val invoice = PurchaseInvoice(
            id = id,
            businessId = businessId,
            invoiceNumber = invoiceNum,
            supplierName = req.supplierName.trim(),
            supplierPhone = req.supplierPhone?.trim(),
            invoiceDate = parsedDate.toString(),
            totalAmount = total,
            paymentStatus = req.paymentStatus?.ifBlank { "PAID" } ?: "PAID",
            paymentMethod = req.paymentMethod?.ifBlank { "CASH" } ?: "CASH",
            notes = req.notes?.trim() ?: "",
            items = req.items,
            createdAt = now.toString(),
            updatedAt = now.toString()
        )
        ApiResponse(true, data = invoice, message = "Purchase invoice recorded and inventory augmented successfully")
    }
}
