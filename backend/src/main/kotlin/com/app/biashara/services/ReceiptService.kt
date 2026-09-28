package com.app.biashara.services

import com.app.biashara.db.*
import com.app.biashara.models.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ReceiptService(
    private val emailService: EmailService,
    private val smsService: SmsService,
    private val config: ApplicationConfig
) {
    private val publicBaseUrl: String
        get() = config.propertyOrNull("app.publicUrl")?.getString()?.trimEnd('/')
            ?: System.getenv("APP_PUBLIC_URL")?.trimEnd('/')
            ?: "https://biashara360.co.ke"

    /**
     * Retrieve public receipt details for a customer given orderId or orderNumber.
     * Accessible publicly without authentication.
     */
    fun getPublicReceipt(orderIdOrNumber: String): PublicReceiptResponse? = transaction {
        val orderRow = OrdersTable.select {
            (OrdersTable.id eq orderIdOrNumber) or (OrdersTable.orderNumber eq orderIdOrNumber)
        }.firstOrNull() ?: return@transaction null

        val orderId = orderRow[OrdersTable.id]
        val businessId = orderRow[OrdersTable.businessId]
        val branchId = orderRow[OrdersTable.branchId]

        val bizRow = BusinessesTable.select {
            BusinessesTable.id eq businessId
        }.firstOrNull() ?: return@transaction null

        val branchRow = branchId?.let { bId ->
            BranchesTable.select {
                (BranchesTable.id eq bId) and (BranchesTable.businessId eq businessId)
            }.firstOrNull()
        }

        val items = OrderItemsTable.select { OrderItemsTable.orderId eq orderId }.map { item ->
            val qty = item[OrderItemsTable.quantity]
            val price = item[OrderItemsTable.unitPrice]
            val buying = item[OrderItemsTable.buyingPrice]
            val modifiers = runCatching {
                Json.decodeFromString<List<MenuOption>>(item[OrderItemsTable.modifiersJson])
            }.getOrDefault(emptyList())
            val gross = qty * (price + modifiers.sumOf { it.priceDelta })
            val lineTotal = if (item[OrderItemsTable.complimentary]) 0.0 else (gross - item[OrderItemsTable.discountAmount]).coerceAtLeast(0.0)

            OrderItemResponse(
                id = item[OrderItemsTable.id],
                productId = item[OrderItemsTable.productId],
                productName = item[OrderItemsTable.productName],
                quantity = qty,
                unitPrice = price,
                buyingPrice = buying,
                lineTotal = lineTotal,
                lineProfit = lineTotal - (qty * buying),
                modifiers = modifiers,
                itemNote = item[OrderItemsTable.itemNote],
                discountAmount = item[OrderItemsTable.discountAmount],
                complimentary = item[OrderItemsTable.complimentary]
            )
        }

        val orderResponse = OrderResponse(
            id = orderId,
            orderNumber = orderRow[OrdersTable.orderNumber],
            businessId = businessId,
            customerId = orderRow[OrdersTable.customerId],
            customerName = orderRow[OrdersTable.customerName],
            customerPhone = orderRow[OrdersTable.customerPhone],
            deliveryLocation = orderRow[OrdersTable.deliveryLocation],
            items = items,
            paymentStatus = orderRow[OrdersTable.paymentStatus],
            deliveryStatus = orderRow[OrdersTable.deliveryStatus],
            paymentMethod = orderRow[OrdersTable.paymentMethod],
            salesChannel = orderRow[OrdersTable.salesChannel],
            serviceType = orderRow[OrdersTable.serviceType],
            hospitalityTableId = orderRow[OrdersTable.hospitalityTableId],
            serverUserId = orderRow[OrdersTable.serverUserId],
            guestCount = orderRow[OrdersTable.guestCount],
            tabStatus = orderRow[OrdersTable.tabStatus],
            mpesaTransactionCode = orderRow[OrdersTable.mpesaTransactionCode],
            baseAmount = orderRow[OrdersTable.baseAmount],
            taxIncluded = orderRow[OrdersTable.taxIncluded],
            taxRate = orderRow[OrdersTable.taxRate],
            taxAmount = orderRow[OrdersTable.taxAmount],
            subtotal = orderRow[OrdersTable.subtotal],
            notes = orderRow[OrdersTable.notes],
            branchId = branchId,
            branchName = branchRow?.get(BranchesTable.name),
            createdAt = orderRow[OrdersTable.createdAt].toString(),
            updatedAt = orderRow[OrdersTable.updatedAt].toString()
        )

        val businessProfile = PublicBusinessProfile(
            id = bizRow[BusinessesTable.id],
            name = bizRow[BusinessesTable.name],
            phone = bizRow[BusinessesTable.ownerPhone],
            email = bizRow[BusinessesTable.ownerEmail],
            address = bizRow[BusinessesTable.address] ?: "",
            county = bizRow[BusinessesTable.county] ?: "",
            kraPin = bizRow[BusinessesTable.kraPin] ?: "",
            receiptHeader = bizRow[BusinessesTable.receiptHeader],
            receiptFooter = bizRow[BusinessesTable.receiptFooter],
            receiptLogo = bizRow[BusinessesTable.receiptLogo],
            receiptLogoWidthMm = bizRow[BusinessesTable.receiptLogoWidthMm],
            receiptLogoHeightMm = bizRow[BusinessesTable.receiptLogoHeightMm],
            receiptShowTax = bizRow[BusinessesTable.receiptShowTax],
            receiptShowCustomer = bizRow[BusinessesTable.receiptShowCustomer]
        )

        val branchInfo = branchRow?.let { b ->
            PublicBranchInfo(
                id = b[BranchesTable.id],
                name = b[BranchesTable.name],
                code = b[BranchesTable.code],
                phone = b[BranchesTable.phone],
                address = b[BranchesTable.address],
                receiptHeader = b[BranchesTable.receiptHeader],
                receiptFooter = b[BranchesTable.receiptFooter]
            )
        }

        val kraRow = EtimsInvoicesTable.select { EtimsInvoicesTable.orderId eq orderId }
            .orderBy(EtimsInvoicesTable.createdAt, SortOrder.DESC)
            .firstOrNull()

        val kraFiscal = kraRow?.let { k ->
            PublicKraFiscal(
                invoiceNumber = k[EtimsInvoicesTable.etimsInvoiceNumber] ?: k[EtimsInvoicesTable.invoiceNumber],
                qrCodeContent = k[EtimsInvoicesTable.qrCodeContent],
                qrCodeBase64 = k[EtimsInvoicesTable.qrCodeBase64],
                sdcId = k[EtimsInvoicesTable.sdcId],
                rcptSign = k[EtimsInvoicesTable.rcptSign]
            )
        }

        val receiptUrl = "$publicBaseUrl/receipt/$orderId"

        PublicReceiptResponse(
            order = orderResponse,
            business = businessProfile,
            branch = branchInfo,
            receiptUrl = receiptUrl,
            kraFiscal = kraFiscal
        )
    }

    /**
     * Send e-receipt via SMS, Email, or generate WhatsApp share payload.
     * Authenticated endpoint called by merchant/cashier.
     */
    suspend fun sendEReceipt(
        orderId: String,
        businessId: String,
        req: SendEReceiptRequest
    ): ApiResponse<SendEReceiptResponse> {
        val receiptData = getPublicReceipt(orderId)
            ?: return ApiResponse(false, message = "Order not found")

        if (receiptData.order.businessId != businessId) {
            return ApiResponse(false, message = "Unauthorized access to order")
        }

        val order = receiptData.order
        val business = receiptData.business
        val receiptUrl = receiptData.receiptUrl

        return when (req.channel.trim().uppercase()) {
            "SMS" -> {
                val targetPhone = (req.recipient?.trim()?.takeIf { it.isNotEmpty() } ?: order.customerPhone).trim()
                if (targetPhone.isBlank()) {
                    return ApiResponse(false, message = "A recipient phone number is required for SMS delivery")
                }
                val smsMessage = "Receipt from ${business.name} for Order #${order.orderNumber}. Amount: KES ${String.format("%,.2f", order.subtotal)}. View official e-receipt: $receiptUrl"
                val sendResult = smsService.sendSms(targetPhone, smsMessage)
                if (sendResult.isSuccess) {
                    ApiResponse(
                        true,
                        message = "e-Receipt SMS sent to $targetPhone",
                        data = SendEReceiptResponse(
                            channel = "SMS",
                            recipient = targetPhone,
                            status = "SENT",
                            messageText = smsMessage
                        )
                    )
                } else {
                    ApiResponse(
                        false,
                        message = "Failed to send SMS: ${sendResult.exceptionOrNull()?.message ?: "Provider error"}"
                    )
                }
            }

            "EMAIL" -> {
                val targetEmail = req.recipient?.trim()?.takeIf { it.isNotEmpty() }
                if (targetEmail.isNullOrBlank() || !targetEmail.contains("@")) {
                    return ApiResponse(false, message = "A valid recipient email address is required")
                }
                val itemsSummary = order.items.map { item ->
                    "${item.quantity}x ${item.productName}" to item.lineTotal
                }
                val emailResult = emailService.sendReceiptEmail(
                    to = targetEmail,
                    businessName = business.name,
                    orderNumber = order.orderNumber,
                    receiptUrl = receiptUrl,
                    totalFormatted = "KES ${String.format("%,.2f", order.subtotal)}",
                    items = itemsSummary
                )
                if (emailResult.isSuccess) {
                    ApiResponse(
                        true,
                        message = "e-Receipt sent to $targetEmail",
                        data = SendEReceiptResponse(
                            channel = "EMAIL",
                            recipient = targetEmail,
                            status = "SENT"
                        )
                    )
                } else {
                    ApiResponse(
                        false,
                        message = "Failed to send email: ${emailResult.exceptionOrNull()?.message ?: "SMTP error"}"
                    )
                }
            }

            "WHATSAPP" -> {
                val targetPhone = (req.recipient?.trim()?.takeIf { it.isNotEmpty() } ?: order.customerPhone).trim()
                val phoneDigits = normalizeForWhatsApp(targetPhone)
                val waMessage = buildString {
                    appendLine("🧾 *Official Electronic Receipt*")
                    appendLine("*${business.name}*")
                    if (receiptData.branch != null) {
                        appendLine("Branch: ${receiptData.branch.name}")
                    }
                    appendLine("Order: #${order.orderNumber}")
                    appendLine("Total: KES ${String.format("%,.2f", order.subtotal)}")
                    appendLine("Payment: ${order.paymentMethod} (${order.paymentStatus})")
                    appendLine()
                    appendLine("View, download & verify your receipt:")
                    appendLine(receiptUrl)
                    appendLine()
                    appendLine("Thank you for shopping with us!")
                }.trim()

                val encodedText = URLEncoder.encode(waMessage, StandardCharsets.UTF_8.toString()).replace("+", "%20")
                val waUrl = if (phoneDigits.isNotBlank()) {
                    "https://wa.me/$phoneDigits?text=$encodedText"
                } else {
                    "https://api.whatsapp.com/send?text=$encodedText"
                }

                ApiResponse(
                    true,
                    message = "WhatsApp e-receipt link ready",
                    data = SendEReceiptResponse(
                        channel = "WHATSAPP",
                        recipient = targetPhone,
                        status = "READY",
                        whatsappUrl = waUrl,
                        messageText = waMessage
                    )
                )
            }

            else -> ApiResponse(false, message = "Unsupported receipt channel '${req.channel}'. Use SMS, EMAIL, or WHATSAPP")
        }
    }

    private fun normalizeForWhatsApp(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("07") && digits.length == 10 -> "254${digits.substring(1)}"
            digits.startsWith("01") && digits.length == 10 -> "254${digits.substring(1)}"
            digits.startsWith("254") && digits.length == 12 -> digits
            else -> digits
        }
    }
}
