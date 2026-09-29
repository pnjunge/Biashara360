package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateReportScheduleRequest(
    val branchId: String? = null,
    val name: String,
    val reportType: String, // SALES_SUMMARY | PAYMENTS | PROFIT_LOSS | LOW_STOCK
    val frequency: String,  // DAILY | WEEKLY | MONTHLY
    val timeOfDay: String = "20:00", // HH:mm (24-hour, EAT Africa/Nairobi)
    val dayOfWeek: Int? = null,      // 1 (Mon) .. 7 (Sun)
    val dayOfMonth: Int? = null,     // 1 .. 28
    val channels: String = "EMAIL",  // EMAIL | WHATSAPP | EMAIL,WHATSAPP
    val emailRecipients: String? = null,
    val whatsappRecipients: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class UpdateReportScheduleRequest(
    val branchId: String? = null,
    val name: String? = null,
    val reportType: String? = null,
    val frequency: String? = null,
    val timeOfDay: String? = null,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val channels: String? = null,
    val emailRecipients: String? = null,
    val whatsappRecipients: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class ReportScheduleResponse(
    val id: String,
    val businessId: String,
    val branchId: String? = null,
    val branchName: String? = null,
    val name: String,
    val reportType: String,
    val frequency: String,
    val timeOfDay: String,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val channels: String,
    val emailRecipients: String? = null,
    val whatsappRecipients: String? = null,
    val isActive: Boolean,
    val lastRunAt: String? = null,
    val lastStatus: String? = null,
    val lastError: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ReportScheduleLogResponse(
    val id: String,
    val scheduleId: String,
    val businessId: String,
    val reportType: String,
    val period: String,
    val channels: String,
    val status: String,
    val summaryText: String? = null,
    val recipientsCount: Int,
    val errorMessage: String? = null,
    val createdAt: String
)

@Serializable
data class SendReportNowResult(
    val success: Boolean,
    val message: String,
    val logId: String? = null,
    val summaryText: String? = null,
    val whatsappUrls: List<String> = emptyList()
)
