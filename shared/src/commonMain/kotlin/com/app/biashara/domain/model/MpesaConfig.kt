package com.app.biashara.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MpesaConfig(
    val businessId: String,
    val shortCode: String,
    val callbackUrl: String,
    val environment: String = "sandbox",
    val accountType: String = "paybill",
    val updatedAt: String = ""
)

@Serializable
data class MpesaConfigRequest(
    val shortCode: String,
    val callbackUrl: String,
    val environment: String = "sandbox",
    val accountType: String = "paybill"
)
