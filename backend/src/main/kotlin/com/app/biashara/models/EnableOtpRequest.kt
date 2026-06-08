package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class EnableOtpRequest(
    val userId: String,
    val enable: Boolean
)
