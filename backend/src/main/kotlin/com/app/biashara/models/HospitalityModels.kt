package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable data class CreateHospitalityTableRequest(val name: String, val area: String = "Main Floor", val capacity: Int = 4)
@Serializable data class HospitalityTableResponse(val id: String, val name: String, val area: String, val capacity: Int, val status: String, val openOrderId: String? = null, val openAmount: Double = 0.0)
@Serializable data class HospitalityOrderRequest(val tableId: String? = null, val serviceType: String = "DINE_IN", val guestCount: Int = 1, val customerName: String = "Walk-in Guest", val customerPhone: String = "", val notes: String = "", val items: List<OrderItemRequest>)
@Serializable data class KitchenTicketResponse(val id: String, val orderId: String, val orderNumber: String, val tableName: String?, val station: String, val status: String, val notes: String, val items: List<OrderItemResponse>, val createdAt: String)
@Serializable data class UpdateTicketStatusRequest(val status: String)
@Serializable data class CloseHospitalityTabRequest(val paymentMethod: String)
@Serializable data class HospitalityDashboardResponse(val enabled: Boolean, val tables: List<HospitalityTableResponse>, val openTabs: List<OrderResponse>, val tickets: List<KitchenTicketResponse>)
