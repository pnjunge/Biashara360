package com.app.biashara.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MpesaConfig(
    val businessId: String,
    val consumerKey: String,
    val shortCode: String,
    val callbackUrl: String,
    val environment: String = "sandbox",
    val accountType: String = "paybill",
    val updatedAt: String = ""
)

@Serializable
data class MpesaConfigRequest(
    val consumerKey: String,
    val consumerSecret: String,
    val shortCode: String,
    val passKey: String,
    val callbackUrl: String,
    val environment: String = "sandbox",
    val accountType: String = "paybill"
)
