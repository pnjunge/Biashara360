package com.app.biashara.services

import com.app.biashara.auth.generateId
import com.app.biashara.db.*
import com.app.biashara.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
// KRA eTIMS Service
//
// eTIMS (Electronic Tax Invoice Management System) is KRA's real-time invoice
// signing system, mandatory for VAT-registered businesses from Jan 2024.
//
// API Base URLs:
//   Sandbox:    https://etims-sbx.kra.go.ke/etims-api
//   Production: https://etims.kra.go.ke/etims-api
//
// Auth: Basic auth with KRA PIN + device serial (per eTIMS spec v1.0)
// Docs: https://www.kra.go.ke/en/individual/filing-paying/types-of-taxes/etims
// ─────────────────────────────────────────────────────────────────────────────

class EtimsService(private val httpClient: HttpClient) {

    private fun baseUrl(env: String) = if (env == "production")
        "https://etims.kra.go.ke/etims-api"
    else
        "https://etims-sbx.kra.go.ke/etims-api"

    // ── Device Initialisation ─────────────────────────────────────────────────
    // Called once when registering a new virtual eTIMS device (SDC)
    // POST /selectInitOsdcInfo

    suspend fun initDevice(profile: KraProfileResponse, req: EtimsDeviceInitRequest): ApiResponse<EtimsDeviceInitResponse> {
        return try {
            val url  = "${baseUrl(profile.etimsEnvironment)}/selectInitOsdcInfo"
            val body = buildJsonObject {
                put("tin",      profile.pin)
                put("bhfId",    "00")               // branch ID (00 = head office)
                put("dvcSrlNo", req.serialNo)
                put("initOsdc", buildJsonObject {
                    put("sdcId",  req.sdcId)
                    put("mrcNo",  req.mrcNo)
                })
            }
            val resp = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
                header("tin",    profile.pin)
                header("bhfId",  "00")
                header("cmcKey", profile.etimsSdcId ?: "")
            }
            val json     = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
            val resultCd = json["resultCd"]?.jsonPrimitive?.content ?: "999"

            if (resultCd == "000") {
                val data = json["data"]?.jsonObject
                ApiResponse(true, data = EtimsDeviceInitResponse(
                    deviceSdcId      = data?.get("sdcId")?.jsonPrimitive?.content ?: req.sdcId,
                    serverDate       = data?.get("dtTm")?.jsonPrimitive?.content ?: "",
                    standardCodeList = emptyList(),
                    status           = "INITIALISED"
                ))
            } else {
                ApiResponse(false, message = "eTIMS init failed: ${json["resultMsg"]?.jsonPrimitive?.content}")
            }
        } catch (e: Exception) {
            ApiResponse(false, message = "eTIMS device init error: ${e.message}")
        }
    }

    // ── Transmit Invoice ──────────────────────────────────────────────────────
    // POST /saveTrnsSalesSvcReq  — saves a sale transaction in KRA system
    // Returns KRA invoice number + QR code content for receipt printing

    suspend fun transmitInvoice(
        profile: KraProfileResponse,
        internalId: String,
        order: OrderResponse,
        taxLines: List<TaxLineResponse>,
        req: EtimsInvoiceRequest
    ): ApiResponse<EtimsInvoiceResponse> {
        val env = profile.etimsEnvironment
        val sdcId = profile.etimsSdcId ?: return ApiResponse(false, message = "eTIMS SDC ID not configured. Register your virtual device first.")

        return try {
            val url = "${baseUrl(env)}/saveTrnsSalesSvcReq"
            val nairobi = kotlinx.datetime.TimeZone.of("Africa/Nairobi")
            val nowNairobi = Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.of("Africa/Nairobi"))
            val dtTm = "%04d%02d%02d%02d%02d%02d".format(nowNairobi.year, nowNairobi.monthNumber, nowNairobi.dayOfMonth, nowNairobi.hour, nowNairobi.minute, nowNairobi.second)

            // Build item list from order items
            val itemList = buildJsonArray {
                order.items.forEachIndexed { idx, item ->
                    addJsonObject {
                        put("itemSeq",   idx + 1)
                        put("itemCd",    item.productId.take(20))
                        put("itemClsCd", "5020230602")    // KRA item classification code (general merchandise)
                        put("itemNm",    item.productName)
                        put("qty",       item.quantity)
                        put("prc",       item.unitPrice)
                        put("splyAmt",   item.lineTotal)
                        put("totAmt",    item.lineTotal)
                        put("taxTyCd",   "B")             // B=16% VAT, C=0% VAT, D=Exempt
                        put("taxAmt",    Math.round(item.lineTotal * 0.16 * 100.0) / 100.0)
                        put("taxblAmt",  item.lineTotal)
                    }
                }
            }

            val totalTaxable = order.items.sumOf { it.lineTotal }
            val totalTax     = taxLines.sumOf { it.taxAmount }
            val totalAmount  = totalTaxable + totalTax

            val payload = buildJsonObject {
                put("tin",         profile.pin)
                put("bhfId",       "00")
                put("invcNo",      req.invoiceNumber)
                put("orgInvcNo",   0)
                put("rcptTyCd",    req.receiptType)        // NS, CR, CS
                put("pmtTyCd",     req.paymentType)        // 01=Cash, 03=Mpesa, 04=Card
                put("salesTyCd",   "N")                    // N=Normal, C=Copy
                put("cfmDt",       dtTm)                   // confirmation date-time
                put("salesDt",     dtTm.take(8))           // sale date YYYYMMDD
                put("custTin",     "")                     // customer TIN (blank = general public)
                put("custNm",      order.customerName)
                put("rcptNo",      0)
                put("totItemCnt",  order.items.size)
                put("taxblAmtA",   totalTaxable)           // A=16% VAT taxable
                put("taxblAmtB",   0.0)
                put("taxblAmtC",   0.0)
                put("taxblAmtD",   0.0)
                put("taxRtA",      16.0)
                put("taxAmtA",     totalTax)
                put("taxAmtB",     0.0)
                put("taxAmtC",     0.0)
                put("taxAmtD",     0.0)
                put("totTaxblAmt", totalTaxable)
                put("totTaxAmt",   totalTax)
                put("totAmt",      totalAmount)
                put("itemList",    itemList)
            }

            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
                header("tin",    profile.pin)
                header("bhfId",  "00")
                header("cmcKey", sdcId)
            }

            val rawText  = response.bodyAsText()
            val json     = Json.parseToJsonElement(rawText).jsonObject
            val resultCd = json["resultCd"]?.jsonPrimitive?.content ?: "999"
            val data     = json["data"]?.jsonObject

            if (resultCd == "000") {
                val etimsInvNo   = data?.get("rcptNo")?.jsonPrimitive?.content ?: req.invoiceNumber
                val sdcDateTime  = data?.get("sdcDateTime")?.jsonPrimitive?.content
                val intrlData    = data?.get("intrlData")?.jsonPrimitive?.content
                val rcptSign     = data?.get("rcptSign")?.jsonPrimitive?.content
                val qrUrl        = buildQrUrl(profile.pin, sdcId, etimsInvNo, dtTm.take(8), totalAmount.toString(), rcptSign)

                // Persist to DB
                transaction {
                    EtimsInvoicesTable.update({ EtimsInvoicesTable.id eq internalId }) {
                        it[etimsInvoiceNumber] = etimsInvNo
                        it[EtimsInvoicesTable.sdcId]       = sdcId
                        it[EtimsInvoicesTable.sdcDateTime]  = sdcDateTime
                        it[EtimsInvoicesTable.qrCodeContent] = qrUrl
                        it[EtimsInvoicesTable.intrlData]    = intrlData
                        it[EtimsInvoicesTable.rcptSign]     = rcptSign
                        it[status]             = "TRANSMITTED"
                        it[rawResponse]        = rawText
                        it[submittedAt]        = Clock.System.now()
                    }
                }

                ApiResponse(true, data = EtimsInvoiceResponse(
                    internalId          = internalId,
                    orderId             = req.orderId,
                    invoiceNumber       = req.invoiceNumber,
                    etimsInvoiceNumber  = etimsInvNo,
                    qrCode              = null,
                    qrCodeUrl           = qrUrl,
                    sdcId               = sdcId,
                    sdcDateTime         = sdcDateTime,
                    status              = "TRANSMITTED",
                    errorMessage        = null,
                    submittedAt         = Clock.System.now().toString(),
                    createdAt           = Clock.System.now().toString()
                ))
            } else {
                val errMsg = json["resultMsg"]?.jsonPrimitive?.content ?: "Unknown eTIMS error"
                transaction {
                    EtimsInvoicesTable.update({ EtimsInvoicesTable.id eq internalId }) {
                        it[status]       = "REJECTED"
                        it[errorMessage] = errMsg
                        it[rawResponse]  = rawText
                        it[retryCount]   = 1  // incremented via separate read
                    }
                }
                ApiResponse(false, message = "eTIMS rejection (code $resultCd: $errMsg)")
            }
        } catch (e: Exception) {
            transaction {
                EtimsInvoicesTable.update({ EtimsInvoicesTable.id eq internalId }) {
                    it[status]       = "ERROR"
                    it[errorMessage] = e.message
                    it[retryCount]   = 1  // incremented via separate read
                }
            }
            ApiResponse(false, message = "eTIMS transmission error: ${e.message}")
        }
    }

    // ── Build QR Code URL ─────────────────────────────────────────────────────
    // KRA QR format for receipt verification at iserveafrica.com/etims/v

    private fun buildQrUrl(pin: String, sdcId: String, invoiceNo: String, date: String, amount: String, signature: String?): String {
        val base = "https://etims.kra.go.ke/etims/v"
        return "$base?tin=$pin&sdc=$sdcId&rcpt=$invoiceNo&dt=$date&amt=$amount&sig=${signature ?: ""}"
    }

    // ── Get pending retries ───────────────────────────────────────────────────

    fun getPendingTransmissions(businessId: String): List<EtimsInvoiceResponse> = transaction {
        EtimsInvoicesTable.select {
            (EtimsInvoicesTable.businessId eq businessId) and
            (EtimsInvoicesTable.status inList listOf("PENDING", "ERROR")) and
            (EtimsInvoicesTable.retryCount less 5)
        }.map { it.toEtimsResponse() }
    }

    fun getTransmissionHistory(businessId: String, limit: Int = 50): List<EtimsInvoiceResponse> = transaction {
        EtimsInvoicesTable.select { EtimsInvoicesTable.businessId eq businessId }
            .orderBy(EtimsInvoicesTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toEtimsResponse() }
    }

    private fun ResultRow.toEtimsResponse() = EtimsInvoiceResponse(
        internalId         = this[EtimsInvoicesTable.id],
        orderId            = this[EtimsInvoicesTable.orderId] ?: "",
        invoiceNumber      = this[EtimsInvoicesTable.invoiceNumber],
        etimsInvoiceNumber = this[EtimsInvoicesTable.etimsInvoiceNumber],
        qrCode             = this[EtimsInvoicesTable.qrCodeBase64],
        qrCodeUrl          = this[EtimsInvoicesTable.qrCodeContent],
        sdcId              = this[EtimsInvoicesTable.sdcId],
        sdcDateTime        = this[EtimsInvoicesTable.sdcDateTime],
        status             = this[EtimsInvoicesTable.status],
        errorMessage       = this[EtimsInvoicesTable.errorMessage],
        submittedAt        = this[EtimsInvoicesTable.submittedAt]?.toString(),
        createdAt          = this[EtimsInvoicesTable.createdAt].toString()
    )
}

    // ── Create pending DB record before transmission ──────────────────────────

    fun createPendingRecord(
        businessId: String,
        req: EtimsInvoiceRequest,
        order: OrderResponse
    ): String = transaction {
        val id            = generateId()
        val now           = Clock.System.now()
        val taxableAmount = order.items.sumOf { it.lineTotal }
        val taxAmt        = Math.round(taxableAmount * 0.16 * 100.0) / 100.0
        EtimsInvoicesTable.insert {
            it[EtimsInvoicesTable.id]            = id
            it[EtimsInvoicesTable.businessId]    = businessId
            it[EtimsInvoicesTable.orderId]       = req.orderId
            it[invoiceNumber]                    = req.invoiceNumber
            it[receiptType]                      = req.receiptType
            it[paymentType]                      = req.paymentType
            it[EtimsInvoicesTable.taxableAmount] = taxableAmount
            it[EtimsInvoicesTable.taxAmount]     = taxAmt
            it[totalAmount]                      = taxableAmount + taxAmt
            it[status]                           = "PENDING"
            it[createdAt]                        = now
        }
        id
    }
