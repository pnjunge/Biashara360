package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val success: Boolean = true,
    val message: String = "OK"
)
