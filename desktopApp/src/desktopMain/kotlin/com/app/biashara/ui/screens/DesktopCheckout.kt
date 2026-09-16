package com.app.biashara.ui.screens

import com.app.biashara.data.remote.ApiResponse
import com.app.biashara.data.remote.BASE_URL
import com.app.biashara.domain.model.MpesaStkPushResponse
import com.app.biashara.domain.usecase.normalizeKenyanMobile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun requireDesktopTable(hospitality: Boolean, serviceType: String, tableId: String?) {
    require(!hospitality || serviceType != "DINE_IN" || !tableId.isNullOrBlank()) {
        "Select a table before placing a dine-in order."
    }
}

internal fun desktopMpesaPhone(phone: String): String {
    return requireNotNull(phone.normalizeKenyanMobile()) {
        "Enter a valid M-Pesa phone number, such as 0712345678."
    }
}

@Serializable
private data class DesktopCloseTabRequest(val paymentMethod: String)

@Serializable
private data class DesktopStkRequest(val orderId: String, val phoneNumber: String, val accountType: String?)

internal data class DesktopSettlementResult(val paid: Boolean, val message: String)

internal suspend fun settleDesktopTab(
    client: HttpClient,
    orderId: String,
    method: String,
    phone: String,
    accountType: String? = null,
    baseUrl: String = BASE_URL
): DesktopSettlementResult {
    val normalizedPhone = if (method == "MPESA") desktopMpesaPhone(phone) else phone
    val close = client.post("$baseUrl/hospitality/tabs/$orderId/close") {
        contentType(ContentType.Application.Json)
        setBody(DesktopCloseTabRequest(method))
    }.body<ApiResponse<JsonObject>>()
    check(close.success && close.data != null) { close.message.ifBlank { "Could not settle tab" } }
    if (method == "MPESA") {
        val stk = client.post("$baseUrl/payments/initiate") {
            contentType(ContentType.Application.Json)
            setBody(DesktopStkRequest(orderId, normalizedPhone, accountType))
        }.body<ApiResponse<MpesaStkPushResponse>>()
        check(stk.success && stk.data?.responseCode == "0") {
            stk.message.ifBlank { "M-Pesa prompt could not be sent. The tab remains unpaid; retry settlement." }
        }
        return DesktopSettlementResult(false, "M-Pesa prompt sent to $normalizedPhone. Waiting for payment confirmation.")
    }
    val paid = close.data?.get("paymentStatus")?.jsonPrimitive?.content == "PAID"
    return DesktopSettlementResult(paid, if (paid) "Tab settled and closed successfully." else "Tab is awaiting payment confirmation.")
}
