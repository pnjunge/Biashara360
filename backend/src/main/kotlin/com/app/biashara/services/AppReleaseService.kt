package com.app.biashara.services

import com.app.biashara.db.AppReleasesTable
import com.app.biashara.models.ApiResponse
import com.app.biashara.models.AppReleaseResponse
import com.app.biashara.models.CreateAppReleaseRequest
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.util.UUID

class AppReleaseService {
    private val logger = LoggerFactory.getLogger(AppReleaseService::class.java)

    private val storageDir: File = run {
        val path = System.getenv("APP_RELEASES_DIR") ?: "uploads/releases"
        File(path).apply { if (!exists()) mkdirs() }
    }

    fun getStorageDirectory(): File = storageDir

    private fun ResultRow.toAppReleaseResponse(): AppReleaseResponse {
        return AppReleaseResponse(
            id = this[AppReleasesTable.id],
            platform = this[AppReleasesTable.platform],
            version = this[AppReleasesTable.version],
            buildNumber = this[AppReleasesTable.buildNumber],
            fileName = this[AppReleasesTable.fileName],
            fileSizeBytes = this[AppReleasesTable.fileSizeBytes],
            sha256 = this[AppReleasesTable.sha256],
            downloadUrl = this[AppReleasesTable.downloadUrl],
            releaseNotes = this[AppReleasesTable.releaseNotes],
            minOsVersion = this[AppReleasesTable.minOsVersion],
            isSigned = this[AppReleasesTable.isSigned],
            isActive = this[AppReleasesTable.isActive],
            downloadCount = this[AppReleasesTable.downloadCount],
            uploadedBy = this[AppReleasesTable.uploadedBy],
            createdAt = this[AppReleasesTable.createdAt].toString(),
            updatedAt = this[AppReleasesTable.updatedAt].toString()
        )
    }

    fun listReleases(platform: String? = null, activeOnly: Boolean = false): List<AppReleaseResponse> = transaction {
        var query = AppReleasesTable.selectAll()
        if (!platform.isNullOrBlank()) {
            query = query.andWhere { AppReleasesTable.platform eq platform.uppercase().trim() }
        }
        if (activeOnly) {
            query = query.andWhere { AppReleasesTable.isActive eq true }
        }
        query.orderBy(AppReleasesTable.createdAt to SortOrder.DESC)
            .map { it.toAppReleaseResponse() }
    }

    fun getActiveReleasesByPlatform(): Map<String, AppReleaseResponse> = transaction {
        val allActive = AppReleasesTable.select { AppReleasesTable.isActive eq true }
            .orderBy(AppReleasesTable.createdAt to SortOrder.DESC)
            .map { it.toAppReleaseResponse() }

        // Take the latest active release per platform
        allActive.groupBy { it.platform.uppercase() }
            .mapValues { (_, releases) -> releases.first() }
    }

    fun getLatestActiveRelease(platform: String): AppReleaseResponse? = transaction {
        AppReleasesTable.select {
            (AppReleasesTable.platform eq platform.uppercase().trim()) and (AppReleasesTable.isActive eq true)
        }
            .orderBy(AppReleasesTable.createdAt to SortOrder.DESC)
            .firstOrNull()
            ?.toAppReleaseResponse()
    }

    fun getReleaseById(id: String): AppReleaseResponse? = transaction {
        AppReleasesTable.select { AppReleasesTable.id eq id }
            .firstOrNull()
            ?.toAppReleaseResponse()
    }

    fun saveUploadedRelease(
        releaseId: String,
        platform: String,
        version: String,
        buildNumber: Int,
        originalFileName: String,
        fileSizeBytes: Long,
        sha256Hex: String,
        minOsVersion: String?,
        releaseNotes: String?,
        isSigned: Boolean,
        makeActive: Boolean,
        uploadedBy: String?
    ): AppReleaseResponse = transaction {
        val now = Clock.System.now()
        val normalizedPlatform = platform.uppercase().trim()

        if (makeActive) {
            // Deactivate previous active releases for the same platform
            AppReleasesTable.update({ AppReleasesTable.platform eq normalizedPlatform }) {
                it[isActive] = false
                it[updatedAt] = now
            }
        }

        val relativeDownloadUrl = "/v1/downloads/files/$releaseId"

        AppReleasesTable.insert {
            it[id] = releaseId
            it[AppReleasesTable.platform] = normalizedPlatform
            it[AppReleasesTable.version] = version.trim()
            it[AppReleasesTable.buildNumber] = buildNumber
            it[fileName] = originalFileName
            it[AppReleasesTable.fileSizeBytes] = fileSizeBytes
            it[sha256] = sha256Hex
            it[downloadUrl] = relativeDownloadUrl
            it[AppReleasesTable.releaseNotes] = releaseNotes?.trim()
            it[AppReleasesTable.minOsVersion] = minOsVersion?.trim()
            it[AppReleasesTable.isSigned] = isSigned
            it[isActive] = makeActive
            it[downloadCount] = 0L
            it[AppReleasesTable.uploadedBy] = uploadedBy
            it[createdAt] = now
            it[updatedAt] = now
        }

        logger.info("Successfully registered app release $releaseId for platform $normalizedPlatform ($version)")

        AppReleasesTable.select { AppReleasesTable.id eq releaseId }
            .first()
            .toAppReleaseResponse()
    }

    fun saveExternalRelease(req: CreateAppReleaseRequest, uploadedBy: String?): ApiResponse<AppReleaseResponse> = transaction {
        val now = Clock.System.now()
        val normalizedPlatform = req.platform.uppercase().trim()
        val releaseId = UUID.randomUUID().toString()

        if (req.isActive) {
            AppReleasesTable.update({ AppReleasesTable.platform eq normalizedPlatform }) {
                it[isActive] = false
                it[updatedAt] = now
            }
        }

        AppReleasesTable.insert {
            it[id] = releaseId
            it[platform] = normalizedPlatform
            it[version] = req.version.trim()
            it[buildNumber] = req.buildNumber
            it[fileName] = req.fileName?.trim()?.ifBlank { "$normalizedPlatform-v${req.version}" } ?: "$normalizedPlatform-v${req.version}"
            it[fileSizeBytes] = 0L
            it[sha256] = null
            it[downloadUrl] = req.downloadUrl.trim()
            it[releaseNotes] = req.releaseNotes?.trim()
            it[minOsVersion] = req.minOsVersion?.trim()
            it[isSigned] = req.isSigned
            it[isActive] = req.isActive
            it[downloadCount] = 0L
            it[AppReleasesTable.uploadedBy] = uploadedBy
            it[createdAt] = now
            it[updatedAt] = now
        }

        val saved = AppReleasesTable.select { AppReleasesTable.id eq releaseId }
            .first()
            .toAppReleaseResponse()

        ApiResponse(true, data = saved, message = "External release registered successfully")
    }

    fun updateStatus(id: String, isActive: Boolean): ApiResponse<AppReleaseResponse> = transaction {
        val exists = AppReleasesTable.select { AppReleasesTable.id eq id }.firstOrNull()
            ?: return@transaction ApiResponse(false, message = "App release not found")

        val now = Clock.System.now()
        val platform = exists[AppReleasesTable.platform]

        if (isActive) {
            // If making active, deactivate others on same platform
            AppReleasesTable.update({ (AppReleasesTable.platform eq platform) and (AppReleasesTable.id neq id) }) {
                it[AppReleasesTable.isActive] = false
                it[updatedAt] = now
            }
        }

        AppReleasesTable.update({ AppReleasesTable.id eq id }) {
            it[AppReleasesTable.isActive] = isActive
            it[updatedAt] = now
        }

        val updated = AppReleasesTable.select { AppReleasesTable.id eq id }
            .first()
            .toAppReleaseResponse()

        ApiResponse(true, data = updated, message = if (isActive) "Release marked as active" else "Release deactivated")
    }

    fun deleteRelease(id: String): ApiResponse<Unit> = transaction {
        val row = AppReleasesTable.select { AppReleasesTable.id eq id }.firstOrNull()
            ?: return@transaction ApiResponse(false, message = "App release not found")

        // Try to delete file from disk if it was stored locally
        val downloadUrl = row[AppReleasesTable.downloadUrl]
        if (downloadUrl.startsWith("/v1/downloads/files/")) {
            val files = storageDir.listFiles { _, name -> name.startsWith("${id}_") }
            files?.forEach { file ->
                try {
                    file.delete()
                    logger.info("Deleted physical release file: ${file.name}")
                } catch (e: Exception) {
                    logger.warn("Could not delete physical file ${file.name}: ${e.message}")
                }
            }
        }

        AppReleasesTable.deleteWhere { AppReleasesTable.id eq id }
        ApiResponse(true, message = "App release deleted successfully")
    }

    fun getReleaseFile(id: String): Pair<File, AppReleaseResponse>? {
        val release = getReleaseById(id) ?: return null
        val files = storageDir.listFiles { _, name -> name.startsWith("${id}_") }
        val matchingFile = files?.firstOrNull() ?: return null
        if (!matchingFile.exists() || !matchingFile.isFile) return null
        return Pair(matchingFile, release)
    }

    fun incrementDownloadCount(id: String) {
        transaction {
            AppReleasesTable.update({ AppReleasesTable.id eq id }) {
                with(SqlExpressionBuilder) {
                    it.update(downloadCount, downloadCount + 1)
                }
            }
        }
    }
}
