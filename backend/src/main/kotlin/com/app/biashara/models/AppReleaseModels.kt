package com.app.biashara.models

import kotlinx.serialization.Serializable

@Serializable
data class AppReleaseResponse(
    val id: String,
    val platform: String,
    val version: String,
    val buildNumber: Int,
    val fileName: String,
    val fileSizeBytes: Long,
    val sha256: String?,
    val downloadUrl: String,
    val releaseNotes: String?,
    val minOsVersion: String?,
    val isSigned: Boolean,
    val isActive: Boolean,
    val downloadCount: Long,
    val uploadedBy: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateAppReleaseRequest(
    val platform: String,
    val version: String,
    val buildNumber: Int = 1,
    val fileName: String? = null,
    val downloadUrl: String,
    val releaseNotes: String? = null,
    val minOsVersion: String? = null,
    val isSigned: Boolean = true,
    val isActive: Boolean = true
)

@Serializable
data class UpdateAppReleaseStatusRequest(
    val isActive: Boolean
)
