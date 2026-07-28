package com.app.biashara.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MpesaConfig(
    val businessId: String,
    val shortCode: String,
    val callbackUrl: String,
    val environment: String = "sandbox",
    val accountType: String = "paybill",
    val passkeyConfigured: Boolean = false,
    val updatedAt: String = ""
)

@Serializable
data class CyberSourceConfig(
    val businessId: String = "",
    val merchantId: String = "",
    val merchantKeyId: String = "",
    val profileId: String = "",
    val accessKey: String = "",
    val environment: String = "sandbox",
    val secretConfigured: Boolean = false,
    val updatedAt: String = ""
)

@Deprecated("Payment settings are managed in the web application")
@Serializable
data class MpesaConfigRequest(
    val shortCode: String,
    val callbackUrl: String,
    val environment: String = "sandbox",
    val accountType: String = "paybill"
)
