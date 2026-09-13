package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class ServiceCatalogResponse(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val durationMinutes: Int,
    val price: Double,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ServiceCatalogRequest(
    val name: String,
    val description: String = "",
    val category: String = "",
    val durationMinutes: Int = 60,
    val price: Double = 0.0,
    val isActive: Boolean = true,
)

@Serializable
data class ServiceResourceResponse(
    val id: String,
    val name: String,
    val type: String,
    val isActive: Boolean,
)

@Serializable
data class ServiceResourceRequest(
    val name: String,
    val type: String = "RESOURCE",
    val isActive: Boolean = true,
)

@Serializable
data class ServiceAppointmentResponse(
    val id: String,
    val serviceId: String,
    val serviceName: String,
    val resourceId: String?,
    val resourceName: String?,
    val customerId: String?,
    val customerName: String,
    val customerPhone: String,
    val staffUserId: String?,
    val startsAt: String,
    val durationMinutes: Int,
    val status: String,
    val notes: String,
    val orderId: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ServiceAppointmentRequest(
    val serviceId: String,
    val resourceId: String? = null,
    val customerId: String? = null,
    val customerName: String,
    val customerPhone: String = "",
    val staffUserId: String? = null,
    val startsAt: String,
    val durationMinutes: Int? = null,
    val notes: String = "",
)

@Serializable
data class ServiceAppointmentStatusRequest(val status: String)

@Serializable
data class ServiceScheduleResponse(
    val services: List<ServiceCatalogResponse>,
    val resources: List<ServiceResourceResponse>,
    val appointments: List<ServiceAppointmentResponse>,
)
